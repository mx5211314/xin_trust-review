import App from './App'
import { createSSRApp } from 'vue'
import { isDarkNow } from './utils/theme'

export function createApp() {
  const app = createSSRApp(App)
  // 全局主题 mixin：每个页面根节点绑 :class="{'theme-dark': isDark}" 即可生效暗黑模式
  app.mixin({
    data() {
      return { isDark: false }
    },
    onLoad() {
      this.isDark = isDarkNow()
    },
    onShow() {
      this.isDark = isDarkNow()
    }
  })
  return { app }
}
