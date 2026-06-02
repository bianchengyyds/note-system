import request from '@/utils/request'

// 后台管理员统计数据（按照接口文档）
export function getAdminStatisticsOverview() {
  return request.get('/admin/statistics/overview')
}

export function getAdminStatisticsYearly(year) {
  return request.get('/admin/statistics/yearly', { params: { year } })
}

export function getAdminStatisticsMonthly(year, month) {
  return request.get('/admin/statistics/monthly', { params: { year, month } })
}

export function refreshAdminStatistics(statDate) {
  return request.post('/admin/statistics/refresh', { statDate })
}

export function refreshAdminUserStatistics() {
  return request.post('/admin/statistics/user/refresh')
}
