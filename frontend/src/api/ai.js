import request from '@/utils/request'

/** AI 助手（04 A3.7 #24 / #25）：额度查询；对话流走 utils/sse.js 的 fetch，不走 axios。 */
export function getQuota() {
  return request.get('/ai/quota')
}
