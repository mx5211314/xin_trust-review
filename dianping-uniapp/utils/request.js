import { BASE_URL } from './config'
import { getToken, logout } from './auth'

/**
 * 统一请求封装：
 * - 自动带 Authorization: Bearer <token>
 * - code=0 返回 data
 * - code=1002 清登录态跳登录页
 * - 其他 code toast 提示并 reject
 */
export function request({ url, method = 'GET', data = {}, silent = false }) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      timeout: 15000,
      header: token ? { Authorization: 'Bearer ' + token } : {},
      success: (res) => {
        const body = res.data
        if (!body || typeof body.code === 'undefined') {
          if (!silent) uni.showToast({ title: '服务异常', icon: 'none' })
          return reject(new Error('bad response'))
        }
        if (body.code === 0) return resolve(body.data)
        if (body.code === 1002) {
          logout()
          return reject(new Error('unauthorized'))
        }
        if (!silent) uni.showToast({ title: body.msg || '请求失败', icon: 'none' })
        const err = new Error(body.msg || 'request failed')
        err.code = body.code
        return reject(err)
      },
      fail: (err) => {
        if (!silent) uni.showToast({ title: '网络异常，请检查后端是否启动', icon: 'none' })
        reject(err)
      }
    })
  })
}
