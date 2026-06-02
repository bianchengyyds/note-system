import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/login',  // ✅ 会自动拼接 baseURL
    method: 'post',
    data
  })
}


/**
 * 用户注册
 * @param {Object} data - 注册表单数据
 * @param {string} data.name - 用户名
 * @param {string} data.password - 密码
 * @param {string} data.email - 邮箱（可选）
 * @param {string} data.phone - 手机号（可选）
 * @param {number} data.gender - 性别（0未知 1男 2女）
 */
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}
