import request from '@/utils/request'

/** 今日计划（04 A2 #22，v3.6）：待复习 + 新题 + 估算分钟 */
export function getReviewSummary() {
  return request.get('/review/summary')
}
