import request from '@/utils/request'

/** 统计（04 A3.5 #19–#21） */

/** 指标卡：todayCount/weekCount/masteredRate/streak/totalQuestionCount/totalRecordCount */
export function getStatsOverview() {
  return request.get('/stats/overview')
}

/** 近 N 天趋势：days 仅 7/30，返回连续日期 */
export function getStatsTrend(days) {
  return request.get('/stats/trend', { params: { days } })
}

/** 分类掌握分布：count 按自评条数聚合 */
export function getStatsCategory() {
  return request.get('/stats/category')
}
