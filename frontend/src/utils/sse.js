/**
 * 助手 SSE 客户端（05 §6.2）：fetch + ReadableStream。
 * 禁用 EventSource（不能带 satoken 头）与 axios（不支持流）。
 *
 * 事件块以空行（\n\n）分隔；data 行做 JSON.parse，非法 JSON 直接跳过。
 */
export async function streamChat({ payload, token, sessionId, signal, onEvent }) {
  const base = import.meta.env.VITE_API_BASE || '/api/v1'
  const res = await fetch(`${base}/ai/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      satoken: token,
      ...(sessionId ? { 'X-Session-Id': sessionId } : {})
    },
    body: JSON.stringify(payload),
    signal
  })

  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw Object.assign(new Error(body.message || '请求失败'), {
      code: body.code,
      http: res.status
    })
  }
  if (!res.body) {
    throw Object.assign(new Error('当前浏览器不支持流式响应'), { code: 0 })
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      let idx
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const raw = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        const evt = parseBlock(raw)
        if (evt) onEvent(evt)
      }
    }
    // 末尾没有空行收尾时也要把最后一块交给调用方
    if (buffer.trim()) {
      const evt = parseBlock(buffer)
      if (evt) onEvent(evt)
    }
  } finally {
    try {
      await reader.cancel()
    } catch {
      // 已经结束/已 abort 时 cancel 会抛错，忽略
    }
  }
}

function parseBlock(raw) {
  let event = 'message'
  const dataLines = []
  for (const line of raw.split(/\r?\n/)) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
  }
  if (!dataLines.length) return null
  const text = dataLines.join('\n')
  try {
    return { event, data: JSON.parse(text) }
  } catch {
    return null // 非法 JSON 直接跳过
  }
}
