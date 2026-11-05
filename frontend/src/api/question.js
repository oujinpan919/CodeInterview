import request from '@/utils/request'

/** 题目（04 A3.3）：列表 / 详情 / 上下题 / 收藏 / 我的收藏 */

export function getQuestions(params) {
  return request.get('/questions', { params })
}

/** 详情（04 A3.3 #9）：一次返回 question / myFavorite / myAssessment / neighbors */
export function getQuestionDetail(id) {
  return request.get(`/questions/${id}`)
}

/** 上/下一题（04 A3.3 #10） */
export function getNeighbors(id) {
  return request.get(`/questions/${id}/neighbors`)
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
