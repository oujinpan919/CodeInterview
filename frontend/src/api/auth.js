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
