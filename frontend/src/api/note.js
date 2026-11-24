import request from '@/utils/request'

/** 题目私有笔记（04 C5 #48–#49，v3.6 C6/C7） */

/** 读我的笔记：未写过返回 { contentMd: '', updatedAt: null } */
export function getQuestionNote(questionId) {
  return request.get(`/questions/${questionId}/note`)
}

/** 保存我的笔记：空内容 = 删除；服务端限 20000 字符 */
export function saveQuestionNote(questionId, contentMd) {
  return request.put(`/questions/${questionId}/note`, { contentMd })
}

/**
 * 导出笔记为 Markdown 文件：返回 Blob（响应拦截器对非 Result 载荷原样返回）。
 * 文件名由后端 Content-Disposition 给出，前端只负责触发下载。
 */
export function exportNotes() {
  return request.get('/export/notes', { responseType: 'blob' })
}
