import request from '@/utils/request'

/** 题目（04 A3.3）：列表接口；详情 / 上下题在 T06 追加 */

export function getQuestions(params) {
  return request.get('/questions', { params })
}

/** 详情（04 A3.3 #9）：一次返回 question / myFavorite / myAssessment / neighbors */
export function getQuestionDetail(id) {
  return request.get(`/questions/${id}`)
}
