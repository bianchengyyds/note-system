import { getToken } from '@/utils/auth'

/**
 * 上传文件
 * @param {File} file 文件对象
 * @returns {Promise<{code: number, msg: string, data: {url: string}}>}
 */
export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return fetch('/api/file/upload', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${getToken()}`
    },
    body: formData
  }).then(res => res.json())
}