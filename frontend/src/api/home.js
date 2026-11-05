import request from '@/utils/request'

/** 首页聚合（04 A3.2 #7，公开 GET；登录时 continueLearning 返回最近 3 条） */
export function getHome() {
  return request.get('/home')
}
