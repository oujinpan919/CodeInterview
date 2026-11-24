import { Marked } from 'marked'
import markedShiki from 'marked-shiki'
import DOMPurify from 'dompurify'

/**
 * 全站唯一的 Markdown 渲染链（02 §4.4 / 06 T06）：
 *   marked（异步）→ marked-shiki（Shiki 高亮）→ DOMPurify 清洗 → 由 MarkdownView 输出
 *
 * - Shiki 用 createHighlighterCore + 显式 10 种语言 + github-light，单例懒加载；
 *   不引 shiki 全量 bundle 入口。
 * - Shiki 失败时该代码块退化为无高亮的 <pre><code>，Markdown 其余部分照常渲染。
 * - 任何输出都必须经过 DOMPurify.sanitize(..., { USE_PROFILES: { html: true } })。
 */

const THEME = 'github-light'
const SUPPORTED_LANGUAGES = [
  'javascript',
  'typescript',
  'java',
  'sql',
  'bash',
  'json',
  'xml',
  'yaml',
  'python',
  'go'
]

const LANGUAGE_ALIASES = {
  js: 'javascript',
  jsx: 'javascript',
  mjs: 'javascript',
  cjs: 'javascript',
  ts: 'typescript',
  tsx: 'typescript',
  sh: 'bash',
  shell: 'bash',
  zsh: 'bash',
  yml: 'yaml',
  html: 'xml',
  htm: 'xml',
  vue: 'xml'
}

const LANGUAGE_LOADERS = {
  javascript: () => import('shiki/langs/javascript.mjs'),
  typescript: () => import('shiki/langs/typescript.mjs'),
  java: () => import('shiki/langs/java.mjs'),
  sql: () => import('shiki/langs/sql.mjs'),
  bash: () => import('shiki/langs/bash.mjs'),
  json: () => import('shiki/langs/json.mjs'),
  xml: () => import('shiki/langs/xml.mjs'),
  yaml: () => import('shiki/langs/yaml.mjs'),
  python: () => import('shiki/langs/python.mjs'),
  go: () => import('shiki/langs/go.mjs')
}

let highlighterPromise = null
let markedInstance = null

/** 单例懒加载：第一次渲染代码块时才创建 highlighter */
function loadHighlighter() {
  if (!highlighterPromise) {
    highlighterPromise = (async () => {
      const { createHighlighterCore } = await import('shiki/core')
      const { createOnigurumaEngine } = await import('shiki/engine/oniguruma')
      return createHighlighterCore({
        themes: [import('shiki/themes/github-light.mjs')],
        langs: Object.values(LANGUAGE_LOADERS).map((load) => load()),
        engine: createOnigurumaEngine(() => import('shiki/wasm'))
      })
    })()
  }
  return highlighterPromise
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function normalizeLanguage(lang) {
  const raw = String(lang || '')
    .trim()
    .toLowerCase()
  const mapped = LANGUAGE_ALIASES[raw] || raw
  return SUPPORTED_LANGUAGES.includes(mapped) ? mapped : ''
}

/**
 * 给代码块 <pre> 打上 data-lang，供 MarkdownView 渲染语言标签。
 *
 * 为什么不用 <code class="language-x">：Shiki 输出的 <code> 不带 language 类
 * （高亮已把内容拆成大量 <span style="color:...">，class 在 <pre class="shiki"> 上）。
 * 这里只改第一个 <pre 开始标签，不碰内部结构，DOMPurify 默认允许 data-* 属性。
 */
function withDataLang(html, lang) {
  const label = String(lang || '')
    .trim()
    .toLowerCase()
  if (!label) return html
  return html.replace(/<pre\b([^>]*)>/, (match, attrs) => `<pre${attrs} data-lang="${escapeHtml(label)}">`)
}

function fallbackCode(code, lang) {
  const language = String(lang || '').trim().toLowerCase()
  const className = language ? ` class="language-${escapeHtml(language)}"` : ''
  const dataLang = language ? ` data-lang="${escapeHtml(language)}"` : ''
  return `<pre class="md-code-fallback"${dataLang}><code${className}>${escapeHtml(code)}</code></pre>`
}

async function highlightCode(code, lang) {
  const language = normalizeLanguage(lang)
  if (!language) return fallbackCode(code, lang)
  try {
    const highlighter = await loadHighlighter()
    // 用规范化后的语言名做标签：```js 与 ```javascript 都显示 javascript
    return withDataLang(highlighter.codeToHtml(code, { lang: language, theme: THEME }), language)
  } catch {
    // Shiki 初始化/高亮失败时退化，DOMPurify 仍会在后面执行
    return fallbackCode(code, lang)
  }
}

function getMarked() {
  if (!markedInstance) {
    markedInstance = new Marked()
    markedInstance.use(markedShiki({ highlight: highlightCode }))
  }
  return markedInstance
}

// 外链统一 target=_blank + rel=noopener noreferrer（在清洗后由 hook 补属性）
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (!node || node.tagName !== 'A') return
  const href = node.getAttribute('href') || ''
  if (/^(https?:)?\/\//i.test(href)) {
    node.setAttribute('target', '_blank')
    node.setAttribute('rel', 'noopener noreferrer')
  }
})

/**
 * v4.12：Markdown 里的图片统一加懒加载与尺寸约束。
 *
 * DOMPurify 默认就允许 <img>：http/https/相对路径的 src 走 IS_ALLOWED_URI 正则放行，
 * javascript: 被该正则排除，所以 OSS 的 https 图片天然通过，不需要放宽任何白名单。
 *
 * 注意 data: —— DOMPurify 3.x 的 DEFAULT_DATA_URI_TAGS 包含 img，
 * 因此 `![](data:image/png;base64,...)` 是能渲染的。对 img 这不构成 XSS，
 * 但它绕过了上传接口、把大段 base64 塞进 content_md，正文会膨胀。
 * 目前不拦（Markdown 由管理员/本人书写）；若要收紧，在这里判断并移除即可。
 *
 * 只补渲染属性：不加 loading="lazy" 时，长题干里十几张图会一起下载。
 */
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (!node || node.tagName !== 'IMG') return
  node.setAttribute('loading', 'lazy')
  node.setAttribute('decoding', 'async')
  // 防布局抖动：图片加载前先占位（CSS 里也有 max-width 兜底）
  if (!node.getAttribute('alt')) node.setAttribute('alt', '')
})

function sanitize(html) {
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
}

/** 流式中一旦出现代码围栏就提前加载 highlighter，避免收尾渲染 Markdown 时卡一下。 */
export function preloadMarkdown() {
  loadHighlighter().catch(() => {
    // 预加载失败不影响正常渲染：MarkdownView 会走 fallback
  })
}

/** Markdown → 已清洗 HTML；任何失败都退回纯文本（保留换行） */
export async function renderMarkdown(markdown) {
  const source = typeof markdown === 'string' ? markdown : ''
  if (!source.trim()) return ''
  try {
    const html = await getMarked().parse(source)
    return sanitize(html)
  } catch {
    return renderPlainText(source)
  }
}

/** 纯文本兜底（转义后放进 <pre>，再经 DOMPurify 清洗；任何路径都不得跳过 sanitize） */
export function renderPlainText(text) {
  const source = typeof text === 'string' ? text : ''
  return sanitize(`<pre class="md-plain-fallback">${escapeHtml(source)}</pre>`)
}
