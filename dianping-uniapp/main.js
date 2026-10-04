import App from './App'
import { createSSRApp } from 'vue'
import { isDarkNow } from './utils/theme'

// #ifdef H5
/**
 * 宽屏（PC）适配用：把主题状态同步到 html/body 上。
 * 宽屏时应用会收成手机宽度居中，外侧画布的底色需要跟着明暗一起变。
 */
function syncBodyTheme() {
  try {
    const dark = isDarkNow()
    document.documentElement.classList.toggle('dp-dark', dark)
    document.body.classList.toggle('dp-dark', dark)
  } catch (e) { /* ignore */ }
}
syncBodyTheme()
// #endif

export function createApp() {
  const app = createSSRApp(App)
  // 全局主题 mixin：每个页面根节点绑 :class="{'theme-dark': isDark}" 即可生效暗黑模式
  app.mixin({
    data() {
      return { isDark: false }
    },
    onLoad() {
      this.isDark = isDarkNow()
      // #ifdef H5
      syncBodyTheme()
      // #endif
    },
    onShow() {
      this.isDark = isDarkNow()
      // #ifdef H5
      syncBodyTheme()
      // #endif
    }
  })
  return { app }
}
