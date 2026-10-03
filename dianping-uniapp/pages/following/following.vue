<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <!-- 关注 / 粉丝 双 tab -->
    <view class="tabs">
      <view class="tab" @tap="switchTab('follow')">
        <text class="tab-tx" :class="{ on: tab === 'follow' }">关注</text>
        <view v-if="tab === 'follow'" class="tab-line"></view>
      </view>
      <view class="tab" @tap="switchTab('fans')">
        <text class="tab-tx" :class="{ on: tab === 'fans' }">粉丝</text>
        <view v-if="tab === 'fans'" class="tab-line"></view>
      </view>
    </view>

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
      <!-- 关注 tab：取消关注；粉丝 tab：回关/互相关注 -->
      <view v-if="tab === 'follow'" class="pill" @tap="unfollow(u)">
        <text class="pill-tx">已关注</text>
      </view>
      <view v-else-if="u.mutual" class="pill" @tap="unfollow(u)">
        <text class="pill-tx">互相关注</text>
      </view>
      <view v-else class="pill pill-on" @tap="followBack(u)">
        <text class="pill-tx-on">回关</text>
      </view>
    </view>

    <!-- 引导式空状态（原则 09） -->
    <view v-if="!loading && !list.length" class="empty">
      <view class="empty-ic">
        <image class="empty-ic-img" src="/static/icons/star.png" mode="aspectFit" />
      </view>
      <text class="empty-t">{{ emptyText }}</text>
      <text class="empty-d">{{ emptySub }}</text>
      <view class="empty-btn" @tap="goFeed">去逛逛</view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { getUser } from '@/utils/auth'

export default {
  data() {
    return {
      tab: 'follow',
      list: [],
      loading: false
    }
  },
  computed: {
    emptyText() {
      return this.tab === 'follow' ? '还没有关注任何人' : '还没有粉丝'
    },
    emptySub() {
      return this.tab === 'follow' ? '去首页发现值得信赖的点评人吧' : '多发笔记、多互动，粉丝会多起来的'
    }
  },
  onLoad(query) {
    if (query && (query.tab === 'fans' || query.tab === 'follow')) {
      this.tab = query.tab
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
    switchTab(t) {
      if (this.tab === t) return
      this.tab = t
      this.list = []
      this.fetch()
    },
    async fetch() {
      this.loading = true
      try {
        const url = this.tab === 'follow'
          ? '/user/following?page=1&pageSize=50'
          : '/user/fans?page=1&pageSize=50'
        const data = await request({ url })
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
            this.fetch()
            uni.showToast({ title: '已取消关注', icon: 'none' })
          } catch (e) { /* ignore */ }
        }
      })
    },
    /** 回关：成功后刷新列表变"互相关注" */
    async followBack(u) {
      try {
        await request({ url: `/user/${u.userId}/follow`, method: 'POST', silent: true })
        u.mutual = true
        uni.showToast({ title: '已回关', icon: 'none', duration: 900 })
      } catch (e) {
        if (e.code === 2003) {
          u.mutual = true
          uni.showToast({ title: '已互相关注', icon: 'none', duration: 900 })
        }
      }
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
  background: var(--dp-card);
}
.tabs {
  display: flex;
  justify-content: center;
  gap: 60rpx;
  padding: 20rpx 0 16rpx;
  border-bottom: 1rpx solid var(--dp-soft);
}
.tab {
  position: relative;
  padding-bottom: 8rpx;
}
.tab-tx {
  font-size: 30rpx;
  color: var(--dp-text3);
}
.tab-tx.on {
  color: var(--dp-text);
  font-weight: 600;
}
.tab-line {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 0;
  width: 40rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: var(--dp-brand-deep);
}
.user-item {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid var(--dp-soft);
}
.avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--dp-orange), var(--dp-brand));
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 22rpx;
  flex-shrink: 0;
}
.avatar-text {
  font-size: 34rpx;
  color: #ffffff;
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
.pill {
  border: 1.5rpx solid var(--dp-text4);
  border-radius: 999rpx;
  padding: 10rpx 32rpx;
  flex-shrink: 0;
}
.pill-tx {
  font-size: 24rpx;
  color: var(--dp-text3);
}
.pill-on {
  background: var(--dp-brand-deep);
  border-color: var(--dp-brand-deep);
}
.pill-tx-on {
  font-size: 24rpx;
  color: #ffffff;
  font-weight: 500;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}
.muted {
  color: var(--dp-text3);
  font-size: 26rpx;
}
.small {
  font-size: 22rpx;
  color: var(--dp-text4);
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
