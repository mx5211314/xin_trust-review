/**
 * 登录态存取（本地存储）
 */
const KEY_TOKEN = 'dp_token'
const KEY_USER = 'dp_user'

export function getToken() {
  return uni.getStorageSync(KEY_TOKEN) || ''
}

export function setLogin(token, user) {
  uni.setStorageSync(KEY_TOKEN, token)
  uni.setStorageSync(KEY_USER, user)
}

export function getUser() {
  return uni.getStorageSync(KEY_USER) || null
}

export function setUserInfo(user) {
  uni.setStorageSync(KEY_USER, user)
}

export function logout() {
  uni.removeStorageSync(KEY_TOKEN)
  uni.removeStorageSync(KEY_USER)
  uni.reLaunch({ url: '/pages/login/login' })
}

/** 是否已登录 */
export function isLogin() {
  return !!getToken()
}

/** 角色判断 */
export function isReviewer() {
  const u = getUser()
  return !!u && (u.role === 'REVIEWER' || u.role === 'ADMIN')
}

export function isAdmin() {
  const u = getUser()
  return !!u && u.role === 'ADMIN'
}
