<template>
  <view class="page">
    <scroll-view
      class="msg-scroll"
      scroll-y
      :scroll-into-view="scrollTo"
      :scroll-with-animation="true"
    >
      <view class="scroll-pad"></view>
      <view
        v-for="m in list"
        :key="m.messageId"
        :id="'m' + m.messageId"
        class="msg-row"
        :class="{ mine: m.mine }"
      >
        <view v-if="!m.mine" class="msg-avatar">
          <text class="msg-avatar-text">{{ (peer.nickname || '客').slice(0, 1) }}</text>
        </view>
        <view class="bubble" :class="{ mine: m.mine }">
          <text class="bubble-text" :class="{ mine: m.mine }">{{ m.text }}</text>
        </view>
        <view v-if="m.mine" class="msg-avatar mine-avatar">
          <text class="msg-avatar-text">{{ (myName || '我').slice(0, 1) }}</text>
        </view>
      </view>
      <view class="hint">
        <text class="hint-text">— 友善交流，文明点评 —</text>
      </view>
    </scroll-view>

    <view class="input-bar">
      <input
        v-model="draft"
        class="chat-input"
        placeholder="发消息…"
        placeholder-class="ph"
        confirm-type="send"
        :adjust-position="true"
        @confirm="send"
      />
      <view class="send" :class="{ on: draft.trim() }" @tap="send">
        <text class="send-text">发送</text>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { getUser } from '@/utils/auth'

export default {
  data() {
    return {
      peerId: '',
      peer: {},
      myName: '',
      list: [],
      draft: '',
      scrollTo: ''
    }
  },
  onLoad(query) {
    this.peerId = query.userId
    if (query.nickname) {
      this.peer = { nickname: decodeURIComponent(query.nickname) }
      uni.setNavigationBarTitle({ title: this.peer.nickname })
    }
    const me = getUser()
    this.myName = me && me.nickname ? me.nickname : '我'
  },
  onShow() {
    this.fetch()
  },
  methods: {
    async fetch() {
      try {
        const data = await request({
          url: `/chat/messages?userId=${this.peerId}&page=1&pageSize=50`
        })
        this.peer = data.peer || this.peer
        if (this.peer.nickname) {
          uni.setNavigationBarTitle({ title: this.peer.nickname })
        }
        this.list = data.list || []
        this.scrollToBottom()
      } catch (e) { /* ignore */ }
    },
    scrollToBottom() {
      if (!this.list.length) return
      const last = this.list[this.list.length - 1]
      this.scrollTo = ''
      this.$nextTick(() => {
        this.scrollTo = 'm' + last.messageId
      })
    },
    async send() {
      const text = this.draft.trim()
      if (!text) return
      try {
        const res = await request({
          url: '/chat/send',
          method: 'POST',
          data: { toUserId: this.peerId, text }
        })
        this.draft = ''
        this.list.push({
          messageId: res.messageId,
          mine: true,
          text,
          createTime: ''
        })
        this.scrollToBottom()
      } catch (e) { /* toast 已提示 */ }
    }
  }
}
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
}
.msg-scroll {
  flex: 1;
  overflow: hidden;
}
.scroll-pad {
  height: 20rpx;
}
.msg-row {
  display: flex;
  align-items: flex-start;
  padding: 14rpx 24rpx;
}
.msg-row.mine {
  justify-content: flex-end;
}
.msg-avatar {
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  background: #ffffff;
  border: 1rpx solid #ffe0e4;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 16rpx;
}
.mine-avatar {
  background: #ffe8ea;
  border: none;
  margin-right: 0;
  margin-left: 16rpx;
}
.msg-avatar-text {
  font-size: 26rpx;
  color: #ff2442;
}
.bubble {
  max-width: 62%;
  background: #ffffff;
  border-radius: 6rpx 20rpx 20rpx 20rpx;
  padding: 20rpx 24rpx;
}
.bubble.mine {
  background: #ff2442;
  border-radius: 20rpx 6rpx 20rpx 20rpx;
}
.bubble-text {
  font-size: 28rpx;
  color: #1f2430;
  line-height: 1.5;
  word-break: break-all;
}
.bubble-text.mine {
  color: #ffffff;
}
.hint {
  text-align: center;
  padding: 30rpx 0 40rpx;
}
.hint-text {
  font-size: 22rpx;
  color: #c2c8d0;
}
.input-bar {
  display: flex;
  align-items: center;
  background: #ffffff;
  border-top: 1rpx solid #f1f3f5;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
}
.chat-input {
  flex: 1;
  background: #f6f7f9;
  border-radius: 32rpx;
  padding: 16rpx 28rpx;
  font-size: 28rpx;
  height: 44rpx;
}
.ph {
  color: #b9c0c9;
}
.send {
  margin-left: 18rpx;
  padding: 12rpx 26rpx;
  border-radius: 28rpx;
}
.send.on {
  background: #ffe8ea;
}
.send-text {
  font-size: 28rpx;
  color: #ff2442;
  font-weight: 500;
}
</style>
