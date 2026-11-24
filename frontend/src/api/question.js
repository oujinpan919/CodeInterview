import request from '@/utils/request'

/** 题目（04 A3.3）：列表 / 详情 / 上下题 / 收藏 / 我的收藏 */

export function getQuestions(params) {
  return request.get('/questions', { params })
}

/** 详情（04 A3.3 #9）：一次返回 question / myFavorite / myAssessment / neighbors。
 *  v3.6 修1：params 可带 ctx=list|favorite|wrong（+ 列表筛选/scope），后端据此按来源列表算上下题。 */
export function getQuestionDetail(id, params) {
  return request.get(`/questions/${id}`, { params })
}

/** 收藏（04 A3.3 #11）：幂等，重复收藏仍成功 */
export function favoriteQuestion(id) {
  return request.post(`/questions/${id}/favorite`)
}

/** 取消收藏（04 A3.3 #12）：物理删除，不存在也成功 */
export function unfavoriteQuestion(id) {
  return request.delete(`/questions/${id}/favorite`)
}

/** 我的收藏（04 A3.3 #13）：Page<QuestionBrief & { favoritedAt }> */
export function getFavorites(params) {
  return request.get('/favorites', { params })
}

export function getRandomQuestion(params) {
  return request.get('/questions/random', { params })
}

/** v3.6 C10：相似题（同分类优先 + 共享标签），默认 3 条 */
export function getSimilarQuestions(id, params) {
  return request.get(`/questions/${id}/similar`, { params })
}

/**
 * v4.2：最近浏览的题目（题目练习页左侧列表）。
 * 未登录时后端直接返回空数组，调用方按空态处理即可。
 * @param {{ limit?: number, categoryId?: number|string }} params
 */
export function getRecentViews(params) {
  return request.get('/questions/recent-views', { params })
}
