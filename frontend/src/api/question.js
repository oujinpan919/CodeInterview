import request from '@/utils/request'

/** 题目（04 A3.3）：列表接口；详情 / 上下题在 T06 追加 */

export function getQuestions(params) {
  return request.get('/questions', { params })
}
