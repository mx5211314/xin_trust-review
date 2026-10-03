<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <view class="head">
      <text class="title">关注的话题</text>
      <text class="sub">点话题看全部内容，可随时取消关注</text>
    </view>

    <view v-if="list.length" class="list">
      <view
        v-for="t in list"
        :key="t"
        class="row"
      >
        <view class="row-main" @tap="goTopic(t)">
          <view class="hash">#</view>
          <view class="row-body">
            <text class="name">{{ t }}</text>
            <text class="count">{{ countOf(t) }} 篇</text>
          </view>
        </view>
        <text class="unfollow" @tap="unfollow(t)">取消</text>
      </view>
    </view>

    <view v-else class="empty">
      <text class="muted">还没有关注的话题</text>
      <text class="muted small">在话题页点「关注话题」就能收集到这里</text>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { getUser } from '@/utils/auth'

export default {
  data() {
    return {
      list: [],
      counts: {}
    }
  },
  onShow() {
    if (!getUser()) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.load()
  },
  methods: {
    async load() {
      try {
        this.list = (await request({ url: '/user/followed-topics', silent: true })) || []
      } catch (e) {
        this.list = []
      }
    },
    countOf(t) {
      return this.counts[t] || 0
    },
    goTopic(t) {
      uni.navigateTo({ url: '/pages/collection/collection?mode=topic&q=' + encodeURIComponent(t) })
    },
    async unfollow(t) {
      try {
        await request({ url: `/user/follow-topic/${encodeURIComponent(t)}`, method: 'DELETE', silent: true })
        this.list = this.list.filter(x => x !== t)
        uni.showToast({ title: '已取消关注', icon: 'none' })
      } catch (e) { /* toast 已提示 */ }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--dp-bg);
}
.head {
  background: linear-gradient(165deg, var(--dp-accent-soft), var(--dp-accent-soft) 55%, #ffffff);
  padding: 40rpx 32rpx 36rpx;
}
.title {
  display: block;
  font-size: 40rpx;
  font-weight: 600;
  color: var(--dp-text);
}
.sub {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--dp-text3);
}
.list {
  background: var(--dp-card);
  margin: 20rpx 24rpx;
  border-radius: 20rpx;
  overflow: hidden;
}
.row {
  display: flex;
  align-items: center;
  padding: 26rpx 28rpx;
  border-bottom: 1rpx solid var(--dp-soft);
}
.row:last-child {
  border-bottom: none;
}
.row-main {
  flex: 1;
  display: flex;
  align-items: center;
  min-width: 0;
}
.hash {
  width: 60rpx;
  height: 60rpx;
  border-radius: 14rpx;
  background: var(--dp-soft);
  color: var(--dp-brand-deep);
  font-size: 34rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 22rpx;
  flex-shrink: 0;
}
.row-body {
  flex: 1;
  min-width: 0;
}
.name {
  display: block;
  font-size: 30rpx;
  font-weight: 500;
  color: var(--dp-text);
}
.count {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--dp-text3);
}
.unfollow {
  font-size: 26rpx;
  color: var(--dp-text3);
  padding: 8rpx 16rpx;
  flex-shrink: 0;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}
.muted {
  font-size: 26rpx;
  color: var(--dp-text3);
}
.small {
  font-size: 22rpx;
  color: var(--dp-text4);
  margin-top: 10rpx;
}
</style>
