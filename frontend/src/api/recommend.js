import request from '@/utils/request'

export function getRecommend(type, limit = 5) {
  return request.get(`/recommend/${type}/${limit}`)
}