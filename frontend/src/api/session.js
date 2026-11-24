import request from '@/utils/request'

export function createSession(payload) {
  return request.post('/practice-sessions', payload)
}

export function getCurrentSession() {
  return request.get('/practice-sessions/current')
}

export function advanceSession(id, payload) {
  return request.post(`/practice-sessions/${id}/advance`, payload)
}
