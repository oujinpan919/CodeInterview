/**
 * v4.2：从题目答案 Markdown 里提取「回答重点 / 拓展知识点」目录。
 *
 * 为什么不用标题层级（# / ##）：
 *   本库 718 道题的答案**没有一篇**使用 `##` 标题，小节一律写成段落开头的
 *   `**小节名**：正文…`。所以目录只能从「行首加粗 + 紧跟冒号」这个模式里提取。
 *
 * 识别规则（都是为了把「小节标题」和「行内强调」区分开）：
 *   1. 必须以 `**` 开头 —— `1. **要点**：…` 这种列表项里的加粗不算小节；
 *   2. 加粗后必须紧跟 `：`/`:`（或允许先出现一个括号注，如 `**冲突解决三条规则**（按优先级）：`），
 *      或者整行只有这一个加粗（`**小结**`）；
 *   3. 反例：`**String** 不可变，每次拼接产生新对象` —— 加粗后是空格再接正文，
 *      这是行内强调而不是小节，必须排除；
 *   4. 标题长度上限 24 字，过滤掉整段都被加粗的长句。
 *
 * 返回结构：{ key, text }，key 用于 :key 与锚点匹配（同文本去重）。
 */

const MAX_HEADING_LENGTH = 24
const MAX_ITEMS = 16

/** 加粗后面允许出现的「小节标记」：可选一个括号注 + 冒号 */
const TAIL_PATTERN = /^\s*(?:[（(][^）)]{0,20}[）)])?\s*[：:]/

function isHeadingLine(line) {
  const trimmed = line.trim()
  if (!trimmed.startsWith('**')) return false

  const match = trimmed.match(/^\*\*(.+?)\*\*/)
  if (!match) return false

  const text = match[1].trim()
  if (!text || text.length > MAX_HEADING_LENGTH) return false
  // 加粗里套了反引号说明是代码术语（如 **`orElse`**），一般不是小节名
  if (text.includes('`')) return false

  const tail = trimmed.slice(match[0].length)
  // 整行只有加粗，或加粗后紧跟冒号（允许一个括号注）
  return tail.trim() === '' || TAIL_PATTERN.test(tail)
}

/**
 * @param {string} markdown 答案 Markdown 原文
 * @returns {Array<{ key: string, text: string }>}
 */
export function extractOutline(markdown) {
  const source = typeof markdown === 'string' ? markdown : ''
  if (!source.trim()) return []

  const items = []
  const seen = new Set()

  for (const line of source.split('\n')) {
    if (!isHeadingLine(line)) continue

    const match = line.trim().match(/^\*\*(.+?)\*\*/)
    const text = match[1].trim()
    if (seen.has(text)) continue

    seen.add(text)
    items.push({ key: `${items.length}-${text}`, text })
    if (items.length >= MAX_ITEMS) break
  }

  return items
}
