import request from '@/utils/request'

/**
 * AI 助手 API（08 §4 / 04 A3.7 #24/#25）。
 * 对话流走 utils/sse.js 的 fetch，不走 axios。
 */

/** 对话页初始化：可用模型 + 生效模型 + 模板 + 开关（D2/D13/D11）。 */
export function getChatMeta() {
  return request.get('/ai/meta')
}

/** Prompt 模板列表（仅启用项，不含正文）。 */
export function listTemplates() {
  return request.get('/ai/templates')
}

/** 额度查询（04 A3.7 #24）。 */
export function getQuota() {
  return request.get('/ai/quota')
}

// ---- 用户级模型配置（HC-3/4）----

export function listMyModels() {
  return request.get('/ai/config/models')
}

export function getMyModel(id) {
  return request.get(`/ai/config/models/${id}`)
}

export function createMyModel(data) {
  return request.post('/ai/config/models', data)
}

export function updateMyModel(id, data) {
  return request.put(`/ai/config/models/${id}`, data)
}

export function deleteMyModel(id) {
  return request.delete(`/ai/config/models/${id}`)
}

export function setDefaultModel(id) {
  return request.put(`/ai/config/models/${id}/default`)
}

export function toggleModel(id, enabled) {
  return request.put(`/ai/config/models/${id}/enabled`, null, { params: { enabled } })
}

/** 连接测试（D4：先 /models 探活，不可用降级 chat）。 */
export function testConnection(data) {
  return request.post('/ai/config/test', data, { timeout: 30000 })
}

// ---- 会话历史（D6）----

export function listConversations(params) {
  return request.get('/ai/conversations', { params })
}

export function getConversation(id) {
  return request.get(`/ai/conversations/${id}`)
}

export function createConversation(data) {
  return request.post('/ai/conversations', data || {})
}

export function renameConversation(id, title) {
  return request.patch(`/ai/conversations/${id}`, { title })
}

export function deleteConversation(id) {
  return request.delete(`/ai/conversations/${id}`)
}

// ---- 管理端（D2/D11）----

export function listSiteModels() {
  return request.get('/admin/ai/models')
}

export function createSiteModel(data) {
  return request.post('/admin/ai/models', data)
}

export function updateSiteModel(id, data) {
  return request.put(`/admin/ai/models/${id}`, data)
}

export function deleteSiteModel(id) {
  return request.delete(`/admin/ai/models/${id}`)
}

export function setDefaultSiteModel(id) {
  return request.put(`/admin/ai/models/${id}/default`)
}

export function testSiteConnection(data) {
  return request.post('/admin/ai/test', data, { timeout: 30000 })
}

export function getSiteSettings() {
  return request.get('/admin/ai/settings')
}

export function updateSiteSettings(allowUserOwnKey) {
  return request.put('/admin/ai/settings', null, { params: { allowUserOwnKey } })
}

export function listPromptTemplates() {
  return request.get('/admin/ai/templates')
}

export function createPromptTemplate(data) {
  return request.post('/admin/ai/templates', data)
}

export function updatePromptTemplate(id, data) {
  return request.put(`/admin/ai/templates/${id}`, data)
}

export function deletePromptTemplate(id) {
  return request.delete(`/admin/ai/templates/${id}`)
}

/** 主密钥导出（D3-b，仅管理员）。 */
export function exportMasterKey() {
  return request.get('/ai/master-key')
}
