<template>
  <view class="page">
    <view v-for="u in list" :key="u.userId" class="user-item">
      <view class="avatar" @tap="goHome(u)">
        <text class="avatar-text">{{ (u.nickname || '客').slice(0, 1) }}</text>
      </view>
      <view class="info" @tap="goHome(u)">
        <view class="name-row">
          <text class="nickname">{{ u.nickname }}</text>
          <text v-if="u.role === 'REVIEWER'" class="tag tag-reviewer">点评人</text>
          <text v-else-if="u.role === 'ADMIN'" class="tag tag-admin">管理员</text>
        </view>
      </view>
      <view class="unfollow" @tap="unfollow(u)">
        <text class="unfollow-text">已关注</text>
      </view>
    </view>

    <view v-if="!loading && !list.length" class="empty">
      <text class="empty-emoji">🔍</text>
      <text class="muted">还没有关注任何人</text>
      <text class="muted small">去首页发现值得信赖的点评人吧</text>
      <button class="btn-primary go-btn" @tap="goFeed">去逛逛</button>
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
        const data = await request({ url: '/user/following?page=1&pageSize=50' })
        this.list = data.list || []
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    goHome(u) {
      uni.navigateTo({ url: '/pages/user/user?userId=' + u.userId })
    },
    unfollow(u) {
      uni.showModal({
        title: '取消关注',
        content: '不再关注「' + u.nickname + '」？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({ url: `/user/${u.userId}/follow`, method: 'DELETE', silent: true })
            this.list = this.list.filter(x => x.userId !== u.userId)
            uni.showToast({ title: '已取消关注', icon: 'none' })
          } catch (e) { /* ignore */ }
        }
      })
    },
    goFeed() {
      uni.switchTab({ url: '/pages/feed/feed' })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #ffffff;
}
.user-item {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid #f6f7f9;
}
.avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: #ffe8ea;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 22rpx;
  flex-shrink: 0;
}
.avatar-text {
  font-size: 34rpx;
  color: #ff2442;
}
.info {
  flex: 1;
  min-width: 0;
}
.name-row {
  display: flex;
  align-items: center;
}
.nickname {
  font-size: 30rpx;
  font-weight: 500;
  margin-right: 12rpx;
}
.unfollow {
  border: 1.5rpx solid #d8dbe0;
  border-radius: 999rpx;
  padding: 10rpx 32rpx;
  flex-shrink: 0;
}
.unfollow-text {
  font-size: 24rpx;
  color: #999999;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}
.empty-emoji {
  font-size: 80rpx;
  margin-bottom: 24rpx;
}
.small {
  font-size: 22rpx;
  color: #c2c8d0;
  margin-top: 10rpx;
}
.go-btn {
  margin-top: 50rpx;
  height: 76rpx;
  line-height: 76rpx;
  padding: 0 70rpx;
  font-size: 26rpx;
}
</style>
