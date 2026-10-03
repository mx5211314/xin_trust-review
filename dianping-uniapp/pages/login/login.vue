<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <!-- 品牌区：红渐变 + 波点纹理 + 白脸好评脸 -->
    <view class="hero">
      <view class="hero-dots"></view>
      <view class="logo-wrap">
        <view class="logo">
          <view class="eye eye-l"></view>
          <text class="star">★</text>
          <view class="mouth"></view>
        </view>
        <text class="app-name">本地点评</text>
        <text class="slogan">每一条点评，都来自可信的人</text>
      </view>
    </view>

    <!-- 表单浮卡：压在品牌区上形成层次 -->
    <view class="form-card">
      <view class="form">
        <input
          v-model="phone"
          class="input"
          type="number"
          maxlength="11"
          placeholder="手机号"
          placeholder-class="ph"
        />
        <view class="code-row">
          <input
            v-model="code"
            class="input code-input"
            type="number"
            maxlength="6"
            placeholder="验证码"
            placeholder-class="ph"
          />
          <view class="code-btn" @tap="getSms">
            <text class="code-btn-text">{{ smsText }}</text>
          </view>
        </view>
        <button class="btn-primary login-btn" :class="{ disabled: !canSubmit }" @tap="doLogin">
          登 录
        </button>
        <text class="tip">新用户验证通过后自动注册</text>
        <text class="tip">普通用户可浏览 · 点评人可发布</text>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { setLogin } from '@/utils/auth'

export default {
  data() {
    return {
      phone: '',
      code: '',
      smsText: '获取验证码',
      counting: false,
      submitting: false
    }
  },
  computed: {
    canSubmit() {
      return /^1\d{10}$/.test(this.phone) && this.code.length >= 4
    }
  },
  methods: {
    getSms() {
      if (this.counting) return
      if (!/^1\d{10}$/.test(this.phone)) {
        return uni.showToast({ title: '手机号格式不对', icon: 'none' })
      }
      // TODO 接后端短信接口后，验证码写入 Redis 并校验
      this.counting = true
      let left = 60
      this.smsText = left + 's'
      const timer = setInterval(() => {
        left--
        if (left <= 0) {
          clearInterval(timer)
          this.counting = false
          this.smsText = '获取验证码'
        } else {
          this.smsText = left + 's'
        }
      }, 1000)
      uni.showToast({ title: '开发环境验证码：8888', icon: 'none' })
    },
    async doLogin() {
      if (!this.canSubmit || this.submitting) return
      this.submitting = true
      try {
        const data = await request({
          url: '/auth/login',
          method: 'POST',
          data: { phone: this.phone, code: this.code }
        })
        setLogin(data.token, { userId: data.userId, role: data.role })
        uni.showToast({ title: data.isNew ? '注册成功' : '登录成功', icon: 'success' })
        setTimeout(() => uni.reLaunch({ url: '/pages/feed/feed' }), 600)
      } catch (e) {
        // toast 已在 request 里统一处理
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--dp-card);
  position: relative;
  overflow: hidden;
}
/* 品牌区：红渐变 + 波点纹理，白脸 logo 反转聚焦 */
.hero {
  background: linear-gradient(160deg, #ff4d64, var(--dp-brand) 45%, #d91c38);
  padding: 150rpx 0 130rpx;
  position: relative;
  overflow: hidden;
}
.hero-dots {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255, 255, 255, 0.16) 3rpx, transparent 3rpx);
  background-size: 36rpx 36rpx;
}
.logo-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}
/* 好评脸：白圆反转 + 红眼（右眼是星）+ 红微笑弧 */
.logo {
  width: 170rpx;
  height: 170rpx;
  border-radius: 50%;
  background: #ffffff;
  position: relative;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.18);
}
.eye {
  position: absolute;
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background: var(--dp-brand-deep);
  top: 56rpx;
}
.eye-l {
  left: 44rpx;
}
.star {
  position: absolute;
  right: 34rpx;
  top: 44rpx;
  color: var(--dp-brand-deep);
  font-size: 40rpx;
  line-height: 40rpx;
}
.mouth {
  position: absolute;
  left: 45rpx;
  right: 45rpx;
  bottom: 34rpx;
  height: 34rpx;
  border-bottom: 10rpx solid var(--dp-brand-deep);
  border-radius: 0 0 70rpx 70rpx;
}
.app-name {
  margin-top: 36rpx;
  font-size: 42rpx;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 8rpx;
}
.slogan {
  margin-top: 14rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 3rpx;
}
/* 表单浮卡 */
.form-card {
  margin: -80rpx 44rpx 0;
  background: var(--dp-card);
  border-radius: 28rpx;
  padding: 44rpx 40rpx 36rpx;
  box-shadow: 0 16rpx 48rpx rgba(31, 36, 48, 0.1);
  position: relative;
}
.form {
  position: relative;
}
.input {
  background: var(--dp-soft);
  border-radius: 24rpx;
  padding: 26rpx 30rpx;
  font-size: 28rpx;
  margin-bottom: 24rpx;
}
.ph {
  color: var(--dp-text4);
}
.code-row {
  display: flex;
  justify-content: space-between;
}
.code-input {
  width: 380rpx;
}
.code-btn {
  width: 190rpx;
  height: 90rpx;
  border-radius: 24rpx;
  background: var(--dp-accent-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}
.code-btn-text {
  color: var(--dp-brand-deep);
  font-size: 26rpx;
}
.login-btn {
  margin-top: 44rpx;
  height: 96rpx;
  line-height: 96rpx;
  border-radius: 999rpx;
  box-shadow: 0 10rpx 28rpx rgba(255, 36, 66, 0.32);
}
.tip {
  display: block;
  text-align: center;
  margin-top: 18rpx;
  font-size: 22rpx;
  color: var(--dp-text4);
}
.tip + .tip {
  margin-top: 8rpx;
}
</style>
