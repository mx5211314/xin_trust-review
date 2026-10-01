/**
 * 主题（暗黑模式）：storage 持久化 light/dark/auto，auto 跟随系统。
 * 页面通过全局 mixin 拿到 isDark，根节点绑 :class="{'theme-dark': isDark}"。
 */
const KEY = 'dp_theme'

export function getMode() {
  try {
    const v = uni.getStorageSync(KEY)
    return v === 'dark' || v === 'light' ? v : 'auto'
  } catch (e) {
    return 'auto'
  }
}

/** 系统当前是否深色：微信小程序 darkmode 后 si.theme；App 用 osTheme；H5 用 matchMedia */
export function sysDark() {
  try {
    const si = uni.getSystemInfoSync()
    const t = (si && (si.theme || si.osTheme)) || ''
    if (t) return t === 'dark'
  } catch (e) { /* ignore */ }
  // #ifdef H5
  try {
    return !!(typeof window !== 'undefined' && window.matchMedia
      && window.matchMedia('(prefers-color-scheme: dark)').matches)
  } catch (e) { /* ignore */ }
  // #endif
  return false
}

/** 页面当前是否应显示深色 */
export function isDarkNow() {
  const m = getMode()
  return m === 'dark' || (m === 'auto' && sysDark())
}

/** 切换模式：light / dark / auto */
export function setMode(m) {
  try {
    uni.setStorageSync(KEY, m === 'dark' || m === 'light' ? m : 'auto')
  } catch (e) { /* ignore */ }
}
