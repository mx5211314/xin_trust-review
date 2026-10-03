<template>
  <view class="page">
    <view class="tip-card">
      <text class="tip-title">发布平台公告</text>
      <text class="tip-text">公告会以“通知”形式群发给所有正常用户，请谨慎发布。内容最多 200 字。</text>
    </view>
    <view class="count-wrap">
      <textarea
        v-model="text"
        class="ann-input"
        maxlength="200"
        placeholder="例：【平台公告】唐山地区点评人招募中，欢迎推荐懂本地生活的朋友～"
        placeholder-class="ph"
      />
      <text class="count-text">{{ text.length }}/200</text>
    </view>
    <button class="btn-primary pub-btn" :class="{ disabled: !text.trim() || submitting }" @tap="publish">
      {{ submitting ? '发布中…' : '发布公告' }}
    </button>

    <view class="history" v-if="recent.length">
      <text class="history-title">最近发布</text>
      <view v-for="(r, i) in recent" :key="i" class="history-item">
        <text class="history-text">{{ r.text }}</text>
        <text class="history-time">{{ r.createTime }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { isAdmin } from '@/utils/auth'

export default {
  data() {
    return {
      text: '',
      submitting: false,
      recent: []
    }
  },
  onShow() {
    if (!isAdmin()) {
      uni.showToast({ title: '仅管理员可访问', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 800)
      return
    }
    this.loadRecent()
  },
  methods: {
    async loadRecent() {
      try {
        const data = await request({ url: '/user/notify?category=announce&page=1&pageSize=5' })
        this.recent = data.list || []
      } catch (e) { /* ignore */ }
    },
    publish() {
      const t = this.text.trim()
      if (!t) return
      uni.showModal({
        title: '发布公告',
        content: '将群发给所有用户，确认发布？',
        success: async (res) => {
          if (!res.confirm) return
          this.submitting = true
          try {
            const r = await request({
              url: '/admin/announce',
              method: 'POST',
              data: { text: t }
            })
            uni.showToast({ title: `已发送给 ${r.sent} 位用户`, icon: 'none' })
            this.text = ''
            this.loadRecent()
          } catch (e) { /* toast 已提示 */ } finally {
            this.submitting = false
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.page {
  padding: 24rpx;
}
.tip-card {
  background: var(--dp-warn-soft);
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
}
.tip-title {
  display: block;
  font-size: 28rpx;
  font-weight: 500;
  color: #b8860b;
  margin-bottom: 10rpx;
}
.tip-text {
  font-size: 24rpx;
  color: #a07a20;
  line-height: 1.6;
}
.count-wrap {
  position: relative;
}
.ann-input {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 26rpx;
  font-size: 28rpx;
  width: auto;
  height: 260rpx;
}
.ph {
  color: #b9c0c9;
}
.count-text {
  position: absolute;
  right: 24rpx;
  bottom: 20rpx;
  font-size: 22rpx;
  color: #c2c8d0;
}
.pub-btn {
  margin-top: 30rpx;
  height: 92rpx;
  line-height: 92rpx;
}
.history {
  margin-top: 50rpx;
}
.history-title {
  display: block;
  font-size: 26rpx;
  color: #999999;
  margin-bottom: 16rpx;
}
.history-item {
  background: #ffffff;
  border-radius: 14rpx;
  padding: 22rpx 24rpx;
  margin-bottom: 16rpx;
}
.history-text {
  display: block;
  font-size: 26rpx;
  color: var(--dp-text);
  line-height: 1.5;
}
.history-time {
  display: block;
  margin-top: 10rpx;
  font-size: 20rpx;
  color: #c2c8d0;
}
</style>
