import request from '@/utils/request'

/**
 * 图片上传（v4.12）。
 *
 * 单独一份 api 而不是塞进 auth.js/question.js：上传是跨业务的通用能力，
 * 头像、题目配图、笔记配图都走它，避免同一份 FormData 逻辑复制三遍。
 *
 * @param {File} file   浏览器 File 对象
 * @param {string} folder 业务子目录：avatar / question / note
 * @returns {Promise<{ key: string, url: string, size: number }>}
 */
export function uploadImage(file, folder = 'misc') {
  const form = new FormData()
  form.append('file', file)
  form.append('folder', folder)
  // 不手写 Content-Type：必须让浏览器自己带 multipart boundary，
  // 手动指定会丢掉 boundary 导致后端解析失败（40000）。
  return request.post('/uploads', form, {
    headers: { 'Content-Type': undefined },
    timeout: 60000
  })
}
