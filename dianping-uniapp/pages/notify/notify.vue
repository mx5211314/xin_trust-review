<template>
  <view class="page">
    <view class="topbar">
      <view class="tabs">
        <text class="tab" :class="{ on: tab === 'notify' }" @tap="switchTab('notify')">通知</text>
        <text class="tab" :class="{ on: tab === 'chat' }" @tap="switchTab('chat')">私信</text>
      </view>
      <text v-if="tab === 'notify'" class="readall" @tap="readAll">全部已读</text>
    </view>

    <!-- 私信：会话列表 -->
    <view v-if="tab === 'chat'">
      <view v-for="c in conversations" :key="c.peerId" class="item" @tap="goChat(c)">
        <view class="icon chat-avatar">
          <text class="icon-text">{{ (c.nickname || '客').slice(0, 1) }}</text>
        </view>
        <view class="body">
          <view class="line1">
            <text class="actor">{{ c.nickname }}</text>
          </view>
          <text class="ntext">{{ c.lastMine ? '我: ' : '' }}{{ c.lastText }}</text>
        </view>
        <view class="chat-right">
          <text class="ntime-right">{{ c.lastTime }}</text>
          <view v-if="c.unread > 0" class="chat-badge">
            <text class="chat-badge-text">{{ c.unread }}</text>
          </view>
        </view>
      </view>
      <view v-if="!loadingChat && !conversations.length" class="empty">
        <text class="muted">还没有私信</text>
        <text class="muted small">在用户主页点"私信"可以给对方发消息</text>
      </view>
    </view>


    <view v-if="tab === 'notify'" v-for="n in list" :key="n.notifyId" class="item" :class="{ unread: !n.isRead }" @tap="tapItem(n)">
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

    <view v-if="tab === 'notify' && !list.length" class="empty">
      <text class="muted">还没有通知</text>
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
      loading: false,
      tab: 'notify',
      conversations: [],
      loadingChat: false
    }
  },
  onShow() {
    if (!getUser()) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.page = 1
    this.fetch()
    this.loadConversations()
    // 清除消息 tab 徽标（进入即视为查看）
    setTimeout(() => {
      uni.removeTabBarBadge({ index: 2 })
    }, 1500)
  },
  onReachBottom() {
    if (this.hasMore && !this.loading) {
      this.page += 1
      this.fetch(true)
    }
  },
  methods: {
    switchTab(t) {
      this.tab = t
      if (t === 'chat') {
        this.loadConversations()
      }
    },
    async loadConversations() {
      this.loadingChat = true
      try {
        const data = await request({ url: '/chat/conversations?limit=30' })
        this.conversations = data.list || []
      } catch (e) {
        this.conversations = []
      } finally {
        this.loadingChat = false
      }
    },
    goChat(c) {
      uni.navigateTo({
        url: `/pages/chat/chat?userId=${c.peerId}&nickname=${encodeURIComponent(c.nickname || '')}`
      })
    },
    iconOf(type) {
      const map = { LIKE: '♥', COMMENT: '💬', REPLY: '💬', FOLLOW: '＋', FAV: '★', MESSAGE: '✉', AUDIT_PASS: '✓', AUDIT_REJECT: '✕' }
      return map[type] || '🔔'
    },
    actionOf(n) {
      const map = {
        MESSAGE: '给你发了私信',
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
.tabs {
  display: flex;
}
.tab {
  font-size: 32rpx;
  color: #999999;
  margin-right: 40rpx;
  position: relative;
}
.tab.on {
  color: #1f2430;
  font-weight: 500;
}
.tab.on::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: -12rpx;
  width: 36rpx;
  height: 5rpx;
  border-radius: 3rpx;
  background: #ff2442;
}
.chat-avatar {
  background: #ffe8ea;
}
.chat-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}
.ntime-right {
  font-size: 20rpx;
  color: #c2c8d0;
}
.chat-badge {
  margin-top: 8rpx;
  background: #ff2442;
  border-radius: 999rpx;
  padding: 2rpx 14rpx;
}
.chat-badge-text {
  color: #ffffff;
  font-size: 20rpx;
}
.small {
  font-size: 22rpx;
  color: #c2c8d0;
  margin-top: 10rpx;
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
