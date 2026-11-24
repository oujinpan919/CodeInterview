import request from '@/utils/request'

/** 刷题路线（04 C8 #51–#52，v3.6 C8）：公开只读，登录时返回本人进度 */

export function getLearningPaths() {
  return request.get('/learning-paths')
}

export function getLearningPath(id) {
  return request.get(`/learning-paths/${id}`)
}
