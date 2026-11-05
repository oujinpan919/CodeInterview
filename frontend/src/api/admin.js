import request from '@/utils/request'

/** 管理端（04 A3.9 #27–#41）；分类/标签管理与批量导入在 T13 追加 */

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
