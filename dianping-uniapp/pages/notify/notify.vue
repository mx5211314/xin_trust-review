<template>
  <view class="page">
    <view class="topbar">
      <text class="title">消息</text>
      <text class="readall" @tap="readAll">全部已读</text>
    </view>

    <view v-for="n in list" :key="n.notifyId" class="item" :class="{ unread: !n.isRead }" @tap="tapItem(n)">
      <view class="icon" :class="'icon-' + n.type">
        <text class="icon-text">{{ iconOf(n.type) }}</text>
      </view>
      <view class="body">
        <view class="line1">
          <text class="actor">{{ n.actorNickname }}</text>
          <text class="action">{{ actionOf(n) }}</text>
        </view>
        <text v-if="n.text" class="ntext">{{ n.text }}</text>
        <text class="ntime">{{ n.createTime }}</text>
      </view>
      <view v-if="!n.isRead" class="dot"></view>
    </view>

    <view v-if="!list.length" class="empty">
      <text class="muted">还没有消息</text>
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
      page: 1,
      pageSize: 20,
      hasMore: true,
      loading: false
    }
  },
  onShow() {
    if (!getUser()) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.page = 1
    this.fetch()
  },
  onReachBottom() {
    if (this.hasMore && !this.loading) {
      this.page += 1
      this.fetch(true)
    }
  },
  methods: {
    iconOf(type) {
      const map = { LIKE: '♥', COMMENT: '💬', REPLY: '💬', FOLLOW: '＋', FAV: '★', AUDIT_PASS: '✓', AUDIT_REJECT: '✕' }
      return map[type] || '🔔'
    },
    actionOf(n) {
      const map = {
        LIKE: '赞了你的笔记',
        COMMENT: '评论了你的笔记',
        REPLY: '回复了你的评论',
        FOLLOW: '关注了你',
        FAV: '收藏了你的笔记',
        AUDIT_PASS: '你的笔记已通过审核',
        AUDIT_REJECT: '你的笔记未通过审核'
      }
      return map[n.type] || '互动了你的内容'
    },
    async fetch(append) {
      this.loading = true
      try {
        const data = await request({
          url: `/user/notify?page=${this.page}&pageSize=${this.pageSize}`
        })
        this.list = append ? this.list.concat(data.list) : data.list
        this.hasMore = this.list.length < data.total
      } catch (e) {
        if (!append) this.list = []
      } finally {
        this.loading = false
      }
    },
    async readAll() {
      try {
        await request({ url: '/user/notify/read-all', method: 'POST' })
        this.list.forEach(n => { n.isRead = 1 })
        uni.showToast({ title: '已全部标记为已读', icon: 'none' })
      } catch (e) { /* ignore */ }
    },
    tapItem(n) {
      n.isRead = 1
      if (n.contentId) {
        uni.navigateTo({ url: '/pages/detail/detail?id=' + n.contentId })
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #ffffff;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 32rpx 20rpx;
  border-bottom: 1rpx solid #f1f3f5;
}
.title {
  font-size: 34rpx;
  font-weight: 500;
}
.readall {
  font-size: 24rpx;
  color: #ff2442;
}
.item {
  display: flex;
  align-items: flex-start;
  padding: 26rpx 32rpx;
  border-bottom: 1rpx solid #f6f7f9;
  position: relative;
}
.item.unread {
  background: #fff8f9;
}
.icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #ffe8ea;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 22rpx;
  flex-shrink: 0;
}
.icon-FOLLOW {
  background: #e6f4ef;
}
.icon-AUDIT_PASS, .icon-AUDIT_REJECT {
  background: #eeedfe;
}
.icon-text {
  font-size: 30rpx;
  color: #ff2442;
}
.icon-FOLLOW .icon-text {
  color: #0e7c66;
}
.icon-AUDIT_PASS .icon-text, .icon-AUDIT_REJECT .icon-text {
  color: #3c3489;
}
.body {
  flex: 1;
  min-width: 0;
}
.line1 {
  display: flex;
  align-items: baseline;
}
.actor {
  font-size: 28rpx;
  font-weight: 500;
  color: #1f2430;
  margin-right: 8rpx;
}
.action {
  font-size: 26rpx;
  color: #444444;
}
.ntext {
  display: block;
  font-size: 25rpx;
  color: #666666;
  margin-top: 6rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ntime {
  display: block;
  font-size: 21rpx;
  color: #b9c0c9;
  margin-top: 6rpx;
}
.dot {
  position: absolute;
  right: 32rpx;
  top: 34rpx;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #ff2442;
}
.empty {
  text-align: center;
  padding: 120rpx 0;
}
</style>
