import request from '@/utils/request'

/** 分类与标签（04 A3.6 #22 / #23，公开 GET） */

export function getCategories() {
  return request.get('/categories')
}

export function getTags() {
  return request.get('/tags')
}
