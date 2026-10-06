<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <!-- 账号信息卡 -->
    <view class="card">
      <view class="row">
        <text class="label">手机号</text>
        <text class="value">{{ profile.phoneMasked || '—' }}</text>
      </view>
      <view class="row">
        <text class="label">身份</text>
        <text class="value">{{ roleText }}</text>
      </view>
      <view class="row">
        <text class="label">点评号</text>
        <text class="value">{{ dianpingNo }}</text>
      </view>
    </view>

    <!-- 账号与安全 -->
    <view class="card">
      <view class="row tap" @tap="showAccountSecurity">
        <text class="label">账号与安全</text>
        <text class="arrow">›</text>
      </view>
      <view class="row tap" @tap="showPrivacy">
        <text class="label">隐私设置</text>
        <text class="arrow">›</text>
      </view>
      <view class="row">
        <text class="label">隐藏我的关注列表</text>
        <switch :checked="hideFollowing" color="#ff2442" style="transform:scale(0.8)" @change="toggleHideFollowing" />
      </view>
      <view class="row">
        <text class="label">允许他人评论我的笔记</text>
        <switch :checked="allowComment" color="#ff2442" style="transform:scale(0.8)" @change="toggleAllowComment" />
      </view>
    </view>

    <!-- 通用设置 -->
    <view class="card">
      <view class="row tap" @tap="goEditProfile">
        <text class="label">编辑资料</text>
        <text class="arrow">›</text>
      </view>
      <view class="row">
        <text class="label">接收互动通知</text>
        <switch :checked="notifyOn" color="#ff2442" style="transform:scale(0.8)" @change="toggleNotify" />
      </view>
      <view class="row tap" @tap="clearCache">
        <text class="label">清理缓存</text>
        <text class="value">{{ cacheSize }}</text>
      </view>
      <view class="row tap" @tap="pickTheme">
        <text class="label">深色模式</text>
        <text class="value">{{ themeText }}</text>
      </view>
      <view class="row tap" @tap="goBlocks">
        <text class="label">黑名单管理</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 关于 -->
    <view class="card">
      <view class="row tap" @tap="showAbout">
        <text class="label">关于本地点评</text>
        <text class="value">v0.1.0</text>
      </view>
      <view class="row tap" @tap="showPolicy">
        <text class="label">用户协议与隐私政策</text>
        <text class="arrow">›</text>
      </view>
      <view class="row tap" @tap="showHelp">
        <text class="label">帮助与客服</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="logout-wrap">
      <text class="logout" @tap="doLogout">退出登录</text>
    </view>

    <text class="footer-tip">本地点评 · 发现身边的真实好店</text>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { getUser, logout, setUserInfo } from '@/utils/auth'
import { getMode, setMode, sysDark } from '@/utils/theme'

export default {
  data() {
    return {
      profile: {},
      notifyOn: true,
      cacheSize: '0 KB',
      hideFollowing: false,
      allowComment: true,
      themeMode: 'auto'
    }
  },
  computed: {
    themeText() {
      return this.themeMode === 'dark' ? '深色' : this.themeMode === 'light' ? '浅色' : '跟随系统'
    },
    roleText() {
      const r = this.profile.role
      if (r === 'ADMIN') return '管理员'
      if (r === 'REVIEWER') return '点评人'
      return '普通用户'
    },
    dianpingNo() {
      const id = this.profile.userId ? String(this.profile.userId) : ''
      return id.length > 8 ? id.slice(-8) : (id || '—')
    }
  },
  onShow() {
    if (!getUser()) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.loadProfile()
    this.calcCache()
    this.themeMode = getMode()
    try {
      const v = uni.getStorageSync('dp_notify_on')
      this.notifyOn = v === '' ? true : !!v
      const hf = uni.getStorageSync('dp_hide_following')
      this.hideFollowing = hf === '' ? false : !!hf
      const ac = uni.getStorageSync('dp_allow_comment')
      this.allowComment = ac === '' ? true : !!ac
    } catch (e) { /* ignore */ }
  },
  methods: {
    async loadProfile() {
      try {
        this.profile = await request({ url: '/user/me' })
      } catch (e) { /* ignore */ }
    },
    calcCache() {
      try {
        const info = uni.getStorageInfoSync()
        const kb = info.currentSize || 0
        this.cacheSize = kb > 1024 ? (kb / 1024).toFixed(1) + ' MB' : kb + ' KB'
      } catch (e) {
        this.cacheSize = '0 KB'
      }
    },
    goEditProfile() {
      // mine 是 tabBar 页，navigateTo 不能跳 tab 页且 switchTab 不支持参数 → 用 storage 传标记
      try {
        uni.setStorageSync('dp_open_edit', '1')
      } catch (e) { /* ignore */ }
      uni.switchTab({ url: '/pages/mine/mine' })
    },
    goBlocks() {
      uni.navigateTo({ url: '/pages/blocks/blocks' })
    },
    /** 深色模式：浅色/深色/跟随系统，storage 持久化，返回各页 onShow 自动生效 */
    pickTheme() {
      const modes = ['light', 'dark', 'auto']
      const labels = ['浅色', '深色', '跟随系统']
      uni.showActionSheet({
        itemList: labels,
        success: (res) => {
          const m = modes[res.tapIndex]
          setMode(m)
          this.themeMode = m
          this.isDark = m === 'dark' || (m === 'auto' && sysDark())
          uni.showToast({ title: '已设为' + labels[res.tapIndex], icon: 'none' })
        }
      })
    },
    toggleNotify(e) {
      this.notifyOn = e.detail.value
      try {
        uni.setStorageSync('dp_notify_on', this.notifyOn)
      } catch (err) { /* ignore */ }
      uni.showToast({ title: this.notifyOn ? '已开启通知' : '已关闭通知', icon: 'none' })
    },
    clearCache() {
      uni.showModal({
        title: '清理缓存',
        content: '将清理本地缓存（不会退出登录）',
        success: (res) => {
          if (!res.confirm) return
          const token = uni.getStorageSync('dp_token')
          const user = uni.getStorageSync('dp_user')
          const notifyOn = uni.getStorageSync('dp_notify_on')
          uni.clearStorageSync()
          if (token) uni.setStorageSync('dp_token', token)
          if (user) uni.setStorageSync('dp_user', user)
          if (notifyOn !== '') uni.setStorageSync('dp_notify_on', notifyOn)
          this.calcCache()
          uni.showToast({ title: '缓存已清理', icon: 'success' })
        }
      })
    },
    showAccountSecurity() {
      uni.showModal({
        title: '账号与安全',
        content: `手机号：${this.profile.phoneMasked || '—'}\n登录方式：手机号 + 验证码\n账号状态：${this.profile.status === 'BANNED' ? '已封禁' : '正常'}\n\n（演示版本：换绑手机号、注销账号等功能上线前提供）`,
        showCancel: false,
        confirmText: '知道了'
      })
    },
    showPrivacy() {
      uni.showModal({
        title: '隐私设置说明',
        content: '可控制关注列表可见性、是否允许他人评论。演示版本开关仅保存在本机，正式版将生效于服务端。',
        showCancel: false,
        confirmText: '知道了'
      })
    },
    toggleHideFollowing(e) {
      this.hideFollowing = e.detail.value
      try {
        uni.setStorageSync('dp_hide_following', this.hideFollowing)
      } catch (err) { /* ignore */ }
      // 提示必须诚实：这两个开关目前只写本机 storage，服务端没有对应约束。
      // 原来只说「已隐藏关注列表」，用户会以为别人真的看不到了。
      uni.showToast({
        title: this.hideFollowing ? '已隐藏关注列表（仅本机生效）' : '已公开关注列表',
        icon: 'none'
      })
    },
    toggleAllowComment(e) {
      this.allowComment = e.detail.value
      try {
        uni.setStorageSync('dp_allow_comment', this.allowComment)
      } catch (err) { /* ignore */ }
      uni.showToast({
        title: this.allowComment ? '已允许评论' : '已关闭评论（仅本机生效）',
        icon: 'none'
      })
    },
    showHelp() {
      uni.showModal({
        title: '帮助与客服',
        content: '遇到问题？\n\n1. 检查网络与后端是否启动\n2. 账号问题联系管理员：13800000000\n3. 意见反馈：在问题详情点右上角···→举报',
        showCancel: false,
        confirmText: '知道了'
      })
    },
    showAbout() {
      uni.showModal({
        title: '本地点评 v0.1.0',
        content: '一个专注本地生活的地方生活点评平台：点评人分享真实体验，用户发现身边好店。\n\n技术栈：Spring Boot 3 + MyBatis-Plus + MySQL + Redis\n客户端：uni-app（小程序 / App / H5）',
        showCancel: false,
        confirmText: '知道了'
      })
    },
    showPolicy() {
      uni.showModal({
        title: '用户协议与隐私政策',
        content: '我们仅收集必要信息（手机号用于登录、发布内容用于展示）。不会向第三方出售你的个人信息。演示版本，完整协议上线前提供。',
        showCancel: false,
        confirmText: '知道了'
      })
    },
    doLogout() {
      uni.showModal({
        title: '退出登录',
        content: '确定要退出吗？',
        success: (res) => {
          if (res.confirm) logout()
        }
      })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--dp-bg);
  padding: 24rpx 0 60rpx;
}
.card {
  background: var(--dp-card);
  border-radius: 20rpx;
  margin: 0 24rpx 24rpx;
  padding: 0 28rpx;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 0;
  border-bottom: 1rpx solid var(--dp-soft);
}
.row:last-child {
  border-bottom: none;
}
.label {
  font-size: 28rpx;
  color: var(--dp-text);
}
.value {
  font-size: 26rpx;
  color: var(--dp-text3);
}
.arrow {
  font-size: 32rpx;
  color: var(--dp-text4);
}
.logout-wrap {
  margin: 40rpx 24rpx 0;
  background: var(--dp-card);
  border-radius: 20rpx;
  padding: 32rpx 0;
  text-align: center;
}
.logout {
  font-size: 28rpx;
  color: var(--dp-brand-deep);
}
.footer-tip {
  display: block;
  text-align: center;
  margin-top: 40rpx;
  font-size: 22rpx;
  color: var(--dp-text4);
}
</style>
