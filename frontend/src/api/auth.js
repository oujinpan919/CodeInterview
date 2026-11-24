import request from '@/utils/request'

/** 认证与账号接口（04 A3.1）。字段严格按契约：昵称即登录名，无 username。 */

export function register(data) {
  return request.post('/auth/register', data)
}

export function login(data) {
  return request.post('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}

export function getMe() {
  return request.get('/auth/me')
}

export function updateProfile(data) {
  return request.put('/auth/profile', data)
}

export function updatePassword(data) {
  return request.put('/auth/password', data)
}

/**
 * v4.12 设置头像。传空字符串表示清除（回退首字母色块）。
 * 与改名/改密不同，这个接口不会踢下线，返回更新后的用户信息。
 */
export function updateAvatar(avatar) {
  return request.put('/auth/avatar', { avatar })
}
