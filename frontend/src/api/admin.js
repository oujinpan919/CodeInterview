import request from '@/utils/request'

/** 管理端（04 A3.9 #27–#41） */

export function getAdminQuestions(params) {
  return request.get('/admin/questions', { params })
}

export function getAdminQuestion(id) {
  return request.get(`/admin/questions/${id}`)
}

export function createAdminQuestion(payload) {
  return request.post('/admin/questions', payload)
}

export function updateAdminQuestion(id, payload) {
  return request.put(`/admin/questions/${id}`, payload)
}

export function deleteAdminQuestion(id) {
  return request.delete(`/admin/questions/${id}`)
}

export function publishAdminQuestion(id) {
  return request.put(`/admin/questions/${id}/publish`)
}

export function offlineAdminQuestion(id) {
  return request.put(`/admin/questions/${id}/offline`)
}

/** 批量导入（04 A3.9 #34）：最多 50 题，单请求覆盖 60s 超时（02 §4.5） */
export function importAdminQuestions(json) {
  return request.post('/admin/questions/import', { json }, { timeout: 60000 })
}

/** 全部分类（含停用；04 A3.9 #35，管理端专用） */
export function getAdminCategories() {
  return request.get('/admin/categories')
}

export function createAdminCategory(payload) {
  return request.post('/admin/categories', payload)
}

export function updateAdminCategory(id, payload) {
  return request.put(`/admin/categories/${id}`, payload)
}

export function deleteAdminCategory(id) {
  return request.delete(`/admin/categories/${id}`)
}

export function createAdminTag(payload) {
  return request.post('/admin/tags', payload)
}

export function updateAdminTag(id, payload) {
  return request.put(`/admin/tags/${id}`, payload)
}

export function deleteAdminTag(id) {
  return request.delete(`/admin/tags/${id}`)
}
