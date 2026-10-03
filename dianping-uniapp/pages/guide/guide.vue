<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <!-- 顶部品牌区 -->
    <view class="head">
      <view class="head-deco"></view>
      <view class="face">
        <view class="face-eye face-eye-l"></view>
        <text class="face-star">★</text>
        <view class="face-mouth"></view>
      </view>
      <text class="head-title">如何成为点评人</text>
      <text class="head-sub">发布权仅对认证点评人开放，保证内容可信</text>
    </view>

    <!-- 三步流程 -->
    <view class="steps">
      <view class="step">
        <view class="step-num" style="background: #ff2442"><text>1</text></view>
        <view class="step-body">
          <text class="step-title">联系管理员</text>
          <text class="step-desc">说明你想点评的领域和地区</text>
        </view>
      </view>
      <view class="step">
        <view class="step-num" style="background: #ff8a3d"><text>2</text></view>
        <view class="step-body">
          <text class="step-title">平台确认身份</text>
          <text class="step-desc">管理员审核通过后为你开通权限</text>
        </view>
      </view>
      <view class="step">
        <view class="step-num" style="background: var(--dp-text)"><text>3</text></view>
        <view class="step-body">
          <text class="step-title">开始发布点评</text>
          <text class="step-desc">重新登录后发布入口即点亮</text>
        </view>
      </view>
    </view>

    <button class="btn-primary contact-btn" @tap="copyAdminPhone">复制管理员联系方式</button>

    <text class="foot-tip">普通用户可浏览全部点评 · 点赞互动</text>
  </view>
</template>

<script>
export default {
  methods: {
    copyAdminPhone() {
      uni.setClipboardData({
        data: '13800000000',
        success: () => uni.showToast({ title: '已复制管理员手机号', icon: 'none' })
      })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--dp-card);
}
.head {
  background: var(--dp-accent-soft);
  padding: 70rpx 0 50rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow: hidden;
}
.head-deco {
  position: absolute;
  right: -50rpx;
  top: -70rpx;
  width: 240rpx;
  height: 240rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.45);
}
.face {
  width: 150rpx;
  height: 150rpx;
  border-radius: 50%;
  background: var(--dp-brand-deep);
  position: relative;
  margin-bottom: 30rpx;
}
.face-eye {
  position: absolute;
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  background: var(--dp-card);
  top: 48rpx;
}
.face-eye-l {
  left: 38rpx;
}
.face-star {
  position: absolute;
  right: 28rpx;
  top: 36rpx;
  color: #ffffff;
  font-size: 36rpx;
  line-height: 36rpx;
}
.face-mouth {
  position: absolute;
  left: 38rpx;
  right: 38rpx;
  bottom: 30rpx;
  height: 30rpx;
  border-bottom: 9rpx solid #ffffff;
  border-radius: 0 0 64rpx 64rpx;
}
.head-title {
  font-size: 38rpx;
  font-weight: 500;
  color: var(--dp-text);
  position: relative;
}
.head-sub {
  margin-top: 14rpx;
  font-size: 24rpx;
  color: var(--dp-text3);
  position: relative;
}
.steps {
  padding: 40rpx 40rpx 0;
}
.step {
  display: flex;
  align-items: center;
  background: var(--dp-soft);
  border-radius: 20rpx;
  padding: 30rpx 28rpx;
  margin-bottom: 20rpx;
}
.step-num {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
  flex-shrink: 0;
}
.step-num text {
  color: #ffffff;
  font-size: 26rpx;
}
.step-body {
  display: flex;
  flex-direction: column;
}
.step-title {
  font-size: 28rpx;
  color: var(--dp-text);
  margin-bottom: 6rpx;
}
.step-desc {
  font-size: 22rpx;
  color: var(--dp-text3);
}
.contact-btn {
  margin: 30rpx 40rpx 0;
  height: 92rpx;
  line-height: 92rpx;
}
.foot-tip {
  display: block;
  text-align: center;
  margin-top: 36rpx;
  font-size: 22rpx;
  color: var(--dp-text4);
}
</style>
