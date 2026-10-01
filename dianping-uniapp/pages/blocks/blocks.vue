<template>
  <view class="page">
    <view class="head">
      <text class="title">黑名单</text>
      <text class="sub">拉黑后，双方都看不到对方的内容与私信</text>
    </view>

    <view class="list" v-if="list.length">
      <view class="row" v-for="u in list" :key="u.userId" @tap="goUser(u)">
        <view class="avatar">
          <image v-if="u.avatar" class="avatar-img" :src="u.avatar" mode="aspectFill" />
          <text v-else class="avatar-text">{{ (u.nickname || '客').slice(0, 1) }}</text>
        </view>
        <text class="nickname">{{ u.nickname }}</text>
        <view class="unblock" @tap.stop="unblock(u)">
          <text class="unblock-text">解除</text>
        </view>
      </view>
    </view>

    <view v-if="!loading && !list.length" class="empty">
      <text class="empty-emoji">🚫</text>
      <text class="muted">还没有拉黑任何人</text>
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
      loading: false
    }
  },
  onShow() {
    if (!getUser()) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.fetch()
  },
  methods: {
    async fetch() {
      this.loading = true
      try {
        this.list = await request({ url: '/user/blocks', silent: true }) || []
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    async unblock(u) {
      uni.showModal({
        title: '解除拉黑',
        content: '将重新看到 ' + u.nickname + ' 的内容',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({ url: `/user/${u.userId}/block`, method: 'DELETE', silent: true })
            this.list = this.list.filter(x => x.userId !== u.userId)
            uni.showToast({ title: '已解除拉黑', icon: 'none' })
          } catch (e) { /* ignore */ }
        }
      })
    },
    goUser(u) {
      uni.navigateTo({ url: '/pages/user/user?userId=' + u.userId })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
}
.head {
  background: #ffffff;
  padding: 36rpx 32rpx;
  border-bottom: 1rpx solid #f1f3f5;
}
.title {
  display: block;
  font-size: 36rpx;
  font-weight: 500;
  color: #1f2430;
}
.sub {
  display: block;
  font-size: 22rpx;
  color: #999999;
  margin-top: 8rpx;
}
.list {
  background: #ffffff;
  margin-top: 20rpx;
}
.row {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid #f6f7f9;
}
.avatar {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  background: #ffe8ea;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 22rpx;
  overflow: hidden;
}
.avatar-img {
  width: 100%;
  height: 100%;
}
.avatar-text {
  font-size: 30rpx;
  color: #ff2442;
}
.nickname {
  flex: 1;
  font-size: 30rpx;
  color: #1f2430;
}
.unblock {
  background: #f6f7f9;
  border-radius: 999rpx;
  padding: 10rpx 28rpx;
}
.unblock-text {
  font-size: 26rpx;
  color: #666666;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}
.empty-emoji {
  font-size: 72rpx;
  margin-bottom: 20rpx;
}
</style>
