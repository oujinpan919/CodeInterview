/**
 * 生成分类图标数据（v4.9 · 单色实心风格）。
 *
 * 为什么要有这个脚本：
 *   44 个分类的图标来源混杂（simple-icons 品牌 logo、phosphor 概念实心图标、
 *   devicon 纯图形），三者的 viewBox 和留白都不同。手写进 .vue 必然出现
 *   「有的撑满、有的缩成一团」。这里用程序统一处理：
 *
 *   1. 归一化 viewBox → 全部输出 0 0 24 24，用 <g transform> 缩放并居中；
 *   2. 统一转单色 —— 剥掉源文件里写死的品牌色，交给外层 fill="currentColor"；
 *   3. 颜色按技术家族分配（Java 橙 / Spring 绿 / 数据库青 / AI 紫…）；
 *   4. 生成紧凑的数据模块，只保留 svg 片段字符串。
 *
 * 数据源（全部为可商用的宽松许可）：
 *   - Simple Icons  CC0-1.0  https://simpleicons.org
 *   - Phosphor     MIT      https://phosphoricons.com
 *   - Devicon      MIT      https://devicon.dev
 *
 * 图标文件缓存在 scripts/_icons/ 下；缺失时会自动联网下载，
 * 所以这个目录不需要提交到仓库，脚本本身可复现。
 *
 * 用法：node scripts/_build-category-icons.mjs
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const here = path.dirname(fileURLToPath(import.meta.url))
const icons = path.join(here, '_icons')
const outFile = path.join(here, '..', 'frontend', 'src', 'data', 'categoryIcons.js')

/**
 * 精确的 SVG path 包围盒。
 *
 * 自己手写路径解析很容易出错 —— 实测踩过坑：圆弧命令 `a.956.956 0 01-.962.962`
 * 里的 `01` 是两个 flag（不是数字 1），手写解析器算错包围盒，导致图标被放大到
 * 2.5 倍、直接溢出画布。所以这里用成熟库 svg-path-bbox。
 *
 * 该库只用于本脚本（不进前端产物），装在 frontend/node_modules 下，
 * 因此这里显式按路径导入，而不是裸模块名（脚本目录解析不到前端依赖）。
 */
const bboxLibPath = path.join(here, '..', 'frontend', 'node_modules', 'svg-path-bbox', 'dist', 'index.js')
let svgPathBbox
try {
  const mod = await import(pathToFileURL(bboxLibPath).href)
  svgPathBbox = mod.svgPathBbox ?? mod.default?.svgPathBbox
} catch {
  svgPathBbox = undefined
}
if (typeof svgPathBbox !== 'function') {
  throw new Error(
    '缺少依赖 svg-path-bbox，请先在 frontend/ 下执行：npm install --no-save svg-path-bbox'
  )
}

/* ------------------------------------------------------------------ *
 * 读取与归一化
 * ------------------------------------------------------------------ */

/** 各图标库的 CDN 地址模板（用于本地缓存缺失时补下载） */
const CDN = {
  si: (n) => `https://cdn.jsdelivr.net/npm/simple-icons@latest/icons/${n}.svg`,
  ph: (n) => `https://cdn.jsdelivr.net/npm/@phosphor-icons/core@latest/assets/fill/${n}-fill.svg`,
  // devicon 的变体后缀（-plain / -original）是文件名的一部分：
  // icons/redis/redis-plain.svg，不是 icons/redis-plain/redis-plain.svg。
  dv: (n) => {
    const m = n.match(/^(.*)-(plain|original)$/)
    const base = m ? m[1] : n
    return `https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/${base}/${n}.svg`
  },
  lu: (n) => `https://cdn.jsdelivr.net/npm/lucide-static@latest/icons/${n}.svg`
}

async function download(url) {
  // jsDelivr 会拒绝没有 User-Agent 的请求（403），必须带上。
  const res = await fetch(url, {
    headers: { 'User-Agent': 'codeinterview-icon-builder/1.0' }
  })
  if (!res.ok) throw new Error(`HTTP ${res.status} ${url}`)
  return res.text()
}

/**
 * 读取图标源文件。本地缓存优先；没有就联网下载并写入缓存。
 * phosphor 的实心变体文件名统一带 -fill 后缀。
 */
async function readRaw(kind, name) {
  const fileName = kind === 'ph' ? `${name}-fill.svg` : `${name}.svg`
  const file = path.join(icons, kind, fileName)
  if (fs.existsSync(file)) return fs.readFileSync(file, 'utf8')

  const url = CDN[kind]?.(name)
  if (!url) throw new Error(`unknown icon source: ${kind}`)
  const svg = await download(url)
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, svg, 'utf8')
  return svg
}

/** 取 <svg> 内部的 <path> 元素，返回 { d, fill } 数组 */
function extractPaths(svg) {
  const body = svg
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/<title>[\s\S]*?<\/title>/g, '')
    .replace(/<defs>[\s\S]*?<\/defs>/g, '')
  const paths = []
  for (const m of body.matchAll(/<path\b([^>]*?)\/?>/g)) {
    const attrs = m[1]
    const d = attrs.match(/\bd="([^"]+)"/)
    if (!d) continue
    // 只保留 fill="none" 这个语义（用于描边型图标），其余写死颜色一律丢弃，
    // 否则 devicon 自带的品牌色会盖掉我们按家族分配的色相。
    const fill = attrs.match(/\bfill="none"/) ? 'none' : undefined
    paths.push(fill ? { d: d[1], fill } : { d: d[1] })
  }
  if (!paths.length) throw new Error('no <path> found in svg')
  return paths
}

/**
 * 精确求一组 path 的真实包围盒。
 *
 * 为什么不直接用 viewBox：源图标在自己的画布内留白量差异极大 —— 实测
 * Phosphor 的双箭头只占画布 63%，而 Simple Icons 的 Spring logo 占 92%。
 * 若按 viewBox 缩放，前者会明显比后者小一圈，看起来像没对齐。
 * 这里改用「真实图形」的包围盒，让每个图标都填满同样的目标尺寸。
 */
function pathBBox(paths) {
  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity
  for (const { d } of paths) {
    // 注意：svgPathBbox 返回的是 [minX, minY, maxX, maxY]，不是 [x, y, w, h]。
    // 之前误当宽高用，导致多 path 图标（collection / itops）被算成 0.05 倍缩、
    // 缩成一小团；单 path 图标则整体偏小。
    const [x1, y1, x2, y2] = svgPathBbox(d)
    if (!Number.isFinite(x1) || !Number.isFinite(x2)) continue
    minX = Math.min(minX, x1)
    minY = Math.min(minY, y1)
    maxX = Math.max(maxX, x2)
    maxY = Math.max(maxY, y2)
  }
  if (!Number.isFinite(minX)) return null
  return { x: minX, y: minY, w: maxX - minX, h: maxY - minY }
}

/**
 * 计算把源图形缩放到 24×24 画布所需的变换。
 *
 * 按「真实图形包围盒」缩放并居中，而不是按 viewBox —— 这样不同图标库里
 * 留白习惯不同的图标，最终视觉大小才一致（都填满 pad 之后的目标区域）。
 */
function fitTransform(paths, { pad = 1.2 } = {}) {
  const box = pathBBox(paths)
  if (!box || box.w <= 0 || box.h <= 0) {
    throw new Error('无法计算图形包围盒')
  }
  const target = 24 - pad * 2 // 可用区域
  const s = target / Math.max(box.w, box.h)
  const cx = box.x + box.w / 2
  const cy = box.y + box.h / 2
  return `translate(${round(12 - cx * s)} ${round(12 - cy * s)}) scale(${round(s, 4)})`
}

function round(n, digits = 3) {
  return Number(n.toFixed(digits))
}

/* ------------------------------------------------------------------ *
 * 图标清单
 *
 * key  →  { color, src, name, scale?, pad? }
 *   src: 'si' Simple Icons | 'ph' Phosphor fill | 'dv' Devicon plain | 'lu' Lucide
 *   scale: 视觉补偿系数。圆形/尖角 > 1，方形/满幅 < 1。
 * ------------------------------------------------------------------ */

const JAVA = '#C2410C'
const DB = '#0E7490'
const REDIS = '#B91C1C'
const SPRING = '#4D7C0F'
const MQ = '#B45309'
const NET = '#0F766E'
const SYS = '#475569'
const FE = '#1D4ED8'
const VUE = '#15803D'
const REACT = '#0369A1'
const ALGO = '#6D28D9'
const ARCH = '#4338CA'
const AI = '#6D28D9'
const GO = '#0E7490'
const CPP = '#1E40AF'
const CS = '#701A75'
const PHP = '#4338CA'
const PY = '#1D4ED8'
const GIT = '#C2410C'
const PATTERN = '#B45309'
const HR = '#BE185D'
const LIB = '#1D4ED8'

/**
 * 44 个分类的图标定义。
 *
 * 选择原则：
 *   - 有官方品牌图形的（Java/MySQL/Redis/Spring/...）一律用品牌图形，
 *     用户一眼就能认出来，比任何自绘的隐喻都准确；
 *   - 没有品牌图形的抽象分类（并发、集合、JVM、系统设计、场景题…）
 *     用 Phosphor 的实心概念图标，语义直白；
 *   - 同一家族的图标共用色相，靠造型区分。
 */
const SPEC = {
  /* ---- Java 家族：橙 ---- */
  java: { color: JAVA, src: 'si', name: 'java' }, // 咖啡杯
  collection: { color: JAVA, src: 'ph', name: 'stack' }, // 叠层 = 集合收纳
  concurrency: { color: JAVA, src: 'ph', name: 'arrows-left-right' }, // 并行流
  jvm: { color: JAVA, src: 'ph', name: 'cpu' }, // 虚拟机 = CPU 抽象层

  /* ---- 数据库 ---- */
  mysql: { color: DB, src: 'ph', name: 'database' }, // 官方是 wordmark，改用通用库图标
  mongodb: { color: DB, src: 'si', name: 'mongodb' }, // 官方叶片

  /* ---- Redis ---- */
  redis: { color: REDIS, src: 'dv', name: 'redis-plain' }, // 官方立方

  /* ---- Spring 家族 ---- */
  spring: { color: SPRING, src: 'si', name: 'spring' },
  springboot: { color: SPRING, src: 'si', name: 'springboot' },
  cloud: { color: SPRING, src: 'ph', name: 'share-network' }, // 分布式拓扑
  mybatis: { color: SPRING, src: 'ph', name: 'table' }, // SQL 映射 → 表格
  netty: { color: SPRING, src: 'ph', name: 'swap' }, // 编解码 → 双向数据交换

  /* ---- 消息队列 ---- */
  mq: { color: MQ, src: 'ph', name: 'queue' },
  rabbitmq: { color: MQ, src: 'si', name: 'rabbitmq' },

  /* ---- 网络 ---- */
  network: { color: NET, src: 'ph', name: 'globe-hemisphere-west' },
  nginx: { color: NET, src: 'si', name: 'nginx' },

  /* ---- 系统 ---- */
  os: { color: SYS, src: 'ph', name: 'desktop' },
  sysops: { color: SYS, src: 'ph', name: 'gear-six' },
  itops: { color: SYS, src: 'ph', name: 'hard-drives' },
  devops: { color: SYS, src: 'ph', name: 'arrows-clockwise' },

  /* ---- 前端 ---- */
  javascript: { color: FE, src: 'si', name: 'javascript' },
  vue: { color: VUE, src: 'si', name: 'vuedotjs' },
  react: { color: REACT, src: 'si', name: 'react' },
  handwritten: { color: FE, src: 'ph', name: 'pencil-simple' },
  codeanalysis: { color: FE, src: 'ph', name: 'magnifying-glass-plus' },

  /* ---- 算法 ---- */
  algorithm: { color: ALGO, src: 'ph', name: 'tree-structure' },
  datastructure: { color: ALGO, src: 'ph', name: 'graph' },

  /* ---- 架构 ---- */
  systemdesign: { color: ARCH, src: 'ph', name: 'cube' },
  scenario: { color: ARCH, src: 'ph', name: 'lightning' },

  /* ---- AI 家族 ----
   * 注意：claudecode 原本用 Claude 官方星芒，但它的放射状造型和 ai 的 sparkle
   * 在 40px 下几乎分辨不出，所以改成终端窗口 —— Claude Code 本身就是 CLI 工具，
   * 这样既分得开，语义也更准。
   */
  ai: { color: AI, src: 'ph', name: 'sparkle' }, // 泛 AI 应用
  aiagent: { color: AI, src: 'ph', name: 'robot' }, // 智能体
  aiproject: { color: AI, src: 'ph', name: 'target' }, // 项目实战：完整交付目标
  claudecode: { color: AI, src: 'ph', name: 'terminal-window' }, // CLI 编程工具
  openclaw: { color: AI, src: 'ph', name: 'plugs-connected' }, // 开源智能体框架

  /* ---- 通用语言 ----
   * 基础篇用语言官方 logo（用户认的是语言本身），
   * 进阶篇改用「进阶」概念图标（曲线上升 / 火箭），
   * 避免两个相邻卡片出现一模一样的图形、看着像渲染错误。
   */
  gobasic: { color: GO, src: 'dv', name: 'go-plain' }, // 地鼠
  go: { color: GO, src: 'ph', name: 'chart-line-up' }, // 进阶：性能/调度向上
  cppbasic: { color: CPP, src: 'dv', name: 'cplusplus-plain' },
  cpp: { color: CPP, src: 'ph', name: 'rocket-launch' }, // 进阶：模板/移动语义
  csharp: { color: CS, src: 'si', name: 'csharp' },
  // PHP 官方标志就是「php」字标（没有独立图形），
  // simple-icons 版字标占满椭圆，比 devicon 版更饱满、22px 下可辨。
  php: { color: PHP, src: 'si', name: 'php' },
  python: { color: PY, src: 'dv', name: 'python-plain' },

  /* ---- 工具与通用 ---- */
  git: { color: GIT, src: 'si', name: 'git' },
  pattern: { color: PATTERN, src: 'ph', name: 'squares-four' },
  hr: { color: HR, src: 'ph', name: 'users-three' },
  library: { color: LIB, src: 'ph', name: 'books' }
}

/* ------------------------------------------------------------------ *
 * 生成
 * ------------------------------------------------------------------ */

const entries = []
for (const [key, spec] of Object.entries(SPEC)) {
  const raw = await readRaw(spec.src, spec.name)
  const paths = extractPaths(raw)
  entries.push({
    key,
    color: spec.color,
    src: spec.src,
    name: spec.name,
    transform: fitTransform(paths, { pad: spec.pad ?? 1.2 }),
    paths
  })
}

// 检测重复：两个分类用同一图形会无法区分（视觉上像渲染错误）。
const byShape = new Map()
for (const e of entries) {
  const sig = e.paths.map((p) => p.d).join('|')
  if (!byShape.has(sig)) byShape.set(sig, [])
  byShape.get(sig).push(e.key)
}
const dups = [...byShape.values()].filter((keys) => keys.length > 1)
if (dups.length) {
  console.error('✗ 以下分类共用同一图形：')
  for (const d of dups) console.error('   ' + d.join(' / '))
  process.exitCode = 1
}

const banner = `/**
 * 题库分类图标集（v4.9 重设计 · 单色实心风格）。
 *
 * ⚠️ 本文件由 scripts/_build-category-icons.mjs 生成，请勿手改。
 *    改图标请改脚本里的 SPEC 再重新生成。
 *
 * 图标来源（均为可商用宽松许可）：
 *   Simple Icons  CC0-1.0  https://simpleicons.org
 *   Phosphor      MIT      https://phosphoricons.com
 *   Devicon       MIT      https://devicon.dev
 *
 * 设计约定：
 *   - 所有图形统一归一化到 24×24 画布，四周留 1px 安全边距，视觉重量接近；
 *   - 单色填充（fill=currentColor），颜色由每个分类的 color 控制；
 *     同一技术家族共用色相（Java 橙 / Spring 绿 / 数据库青 / AI 紫…）；
 *   - 品牌技术用官方 logo（辨识度最高），抽象概念用 Phosphor 实心图标；
 *   - 相邻分类不允许复用同一图形（会像渲染错误），由生成脚本强制校验。
 */

/**
 * 图标定义。
 *
 * transform —— 把原始 viewBox 缩放进 24×24 画布的变换（字符串，直接给 <g> 用）；
 * paths     —— 图形路径数组，组件用 <path v-for> 渲染，不拼 HTML 字符串。
 */
export const CATEGORY_ICONS = {
`

const body = entries
  .map(
    (e) =>
      `  ${e.key}: {\n` +
      `    color: '${e.color}',\n` +
      `    // ${e.src}/${e.name}\n` +
      `    transform: '${e.transform}',\n` +
      `    paths: [\n` +
      e.paths
        .map((p) => `      { d: '${p.d.replace(/'/g, "\\'")}'${p.fill ? `, fill: '${p.fill}'` : ''} }`)
        .join(',\n') +
      `\n    ]\n` +
      `  }`
  )
  .join(',\n')

const rules = `}

/**
 * 分类名 → 图标 key。
 *
 * 顺序即优先级，必须「具体 → 宽泛」：
 *   JavaScript 要在 Java 之前（否则被 Java 抢走）；
 *   Java 虚拟机 / 并发 / 集合 要在 Java 之前；
 *   C++ 基础 要在 C++ 之前；SpringCloud 要在 Spring 之前。
 */
const NAME_RULES = [
  [/^最全\\s*AI|AI 应用开发/i, 'ai'],
  [/AI\\s*Agent|智能体面试/i, 'aiagent'],
  [/超级智能体项目/, 'aiproject'],
  [/Claude\\s*Code/i, 'claudecode'],
  [/OpenClaw/i, 'openclaw'],
  [/^Go\\s*基础/, 'gobasic'],
  [/^Go/, 'go'],
  [/^C\\+\\+\\s*基础/, 'cppbasic'],
  [/^C\\+\\+/, 'cpp'],
  [/^C#/, 'csharp'],
  [/PHP/i, 'php'],
  [/Python/i, 'python'],
  [/MySQL/i, 'mysql'],
  [/MongoDB/i, 'mongodb'],
  [/Redis/i, 'redis'],
  [/RabbitMQ/i, 'rabbitmq'],
  [/消息队列|Kafka|RocketMQ/i, 'mq'],
  [/Spring\\s*Cloud/i, 'cloud'],
  [/Spring\\s*Boot/i, 'springboot'],
  [/Spring/i, 'spring'],
  [/MyBatis/i, 'mybatis'],
  [/Netty/i, 'netty'],
  [/Nginx/i, 'nginx'],
  [/DevOps/i, 'devops'],
  [/IT\\s*运维/, 'itops'],
  [/运维/, 'sysops'],
  [/操作系统|Linux/i, 'os'],
  [/计算机网络|网络|TCP|HTTP/i, 'network'],
  [/Java\\s*虚拟机|JVM/i, 'jvm'],
  [/Java\\s*并发|并发/i, 'concurrency'],
  [/Java\\s*集合|集合/, 'collection'],
  [/JavaScript/i, 'javascript'],
  [/Java/i, 'java'],
  [/Vue/i, 'vue'],
  [/React/i, 'react'],
  [/手写代码/, 'handwritten'],
  [/代码分析/, 'codeanalysis'],
  [/前端/i, 'javascript'],
  [/算法/, 'algorithm'],
  [/数据结构/, 'datastructure'],
  [/系统设计|后端系统/, 'systemdesign'],
  [/后端场景/, 'scenario'],
  [/设计模式/, 'pattern'],
  [/Git/i, 'git'],
  [/HR|人力|招聘/i, 'hr'],
  [/全部题库|全部题目|题库/, 'library']
]

/**
 * 解析分类图标。命中返回 { color, svg }；
 * 未命中（管理员新建的分类）返回 null，由组件走首字兜底。
 */
export function resolveCategoryIcon(name) {
  const text = String(name || '').trim()
  if (!text) return null
  for (const [pattern, key] of NAME_RULES) {
    if (pattern.test(text)) return CATEGORY_ICONS[key]
  }
  return null
}

export default CATEGORY_ICONS
`

fs.mkdirSync(path.dirname(outFile), { recursive: true })
fs.writeFileSync(outFile, banner + body + rules, 'utf8')
console.log(`✓ 写入 ${outFile}`)
console.log(`  ${entries.length} 个图标，重复检查：${dups.length ? '有冲突' : '通过'}`)
