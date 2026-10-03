<template>
  <view class="page" :class="{'theme-dark': isDark}">
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

    <!-- 引导式空状态（原则 09，去掉 emoji） -->
    <view v-if="!loading && !list.length" class="empty">
      <view class="empty-ic">
        <image class="empty-ic-img" src="/static/icons/eye.png" mode="aspectFit" />
      </view>
      <text class="empty-t">还没有拉黑任何人</text>
      <text class="empty-d">拉黑后彼此看不到对方的内容与评论</text>
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
  background: var(--dp-bg);
}
.head {
  background: var(--dp-card);
  padding: 36rpx 32rpx;
  border-bottom: 1rpx solid var(--dp-line);
}
.title {
  display: block;
  font-size: 36rpx;
  font-weight: 500;
  color: var(--dp-text);
}
.sub {
  display: block;
  font-size: 22rpx;
  color: var(--dp-text3);
  margin-top: 8rpx;
}
.list {
  background: var(--dp-card);
  margin-top: 20rpx;
}
.row {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid var(--dp-soft);
}
.avatar {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  background: var(--dp-accent-soft);
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
  color: var(--dp-brand-deep);
}
.nickname {
  flex: 1;
  font-size: 30rpx;
  color: var(--dp-text);
}
.unblock {
  background: var(--dp-soft);
  border-radius: 999rpx;
  padding: 10rpx 28rpx;
}
.unblock-text {
  font-size: 26rpx;
  color: var(--dp-text2);
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}
.empty-ic {
  width: 128rpx;
  height: 128rpx;
  border-radius: var(--r-pill);
  background: var(--dp-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--sp-4);
}
.empty-ic-img {
  width: 56rpx;
  height: 56rpx;
  opacity: .45;
}
.empty-t {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--dp-text);
}
.empty-d {
  font-size: var(--fs-sm);
  color: var(--dp-text3);
  line-height: 1.7;
  text-align: center;
  margin-top: var(--sp-2);
  max-width: 440rpx;
}
</style>
