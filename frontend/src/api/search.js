import request from '@/utils/request'

export function search(params) {
  return request.get('/search', { params })
}