import request from '@/utils/request'

/** 自评与刷题记录（04 A3.4 #14 / #15）；错题本接口在 T10 追加 */

/** 三档自评：追加一条 practice_record，同时联动错题本与 practice_count */
export function submitAssessment(questionId, payload) {
  return request.post(`/questions/${questionId}/assessment`, payload)
}

/** 刷题记录：可按 selfAssess 筛选，按时间倒序 */
export function getPracticeRecords(params) {
  return request.get('/practice-records', { params })
}
