import request from '@/utils/request'

/** 自评、刷题记录与错题本（04 A3.4 #14–#18） */

/** 三档自评：追加一条 practice_record，同时联动错题本与 practice_count */
export function submitAssessment(questionId, payload) {
  return request.post(`/questions/${questionId}/assessment`, payload)
}

/** 刷题记录：可按 selfAssess 筛选，按时间倒序 */
export function getPracticeRecords(params) {
  return request.get('/practice-records', { params })
}

/** 错题本列表：resolved=0 未掌握 / 1 已掌握 */
export function getWrongBook(params) {
  return request.get('/wrong-book', { params })
}

/** 标记掌握（幂等） */
export function markWrongResolved(questionId) {
  return request.put(`/wrong-book/${questionId}/resolved`)
}

/** 彻底移除（物理删除） */
export function removeWrong(questionId) {
  return request.delete(`/wrong-book/${questionId}`)
}
