<template>
  <view class="page" :class="{'theme-dark': isDark}">
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
        <view class="bubble" :class="{ mine: m.mine, img: m.imageUrl }">
          <image
            v-if="m.imageUrl"
            class="bubble-img"
            :src="m.imageUrl"
            mode="widthFix"
            @tap="previewImg(m.imageUrl)"
          />
          <text v-else class="bubble-text" :class="{ mine: m.mine }">{{ m.text }}</text>
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
      <!-- 同一按钮位：未输入显示 ➕（发图），一输入就变成「发送」（原型 v4） -->
      <view v-if="!draft.trim() && !sending" class="plus-btn" @tap="pickImage">
        <view class="plus-v"></view>
        <view class="plus-h"></view>
      </view>
      <view v-else class="send" @tap="send">
        <text class="send-text">{{ sending ? '…' : '发送' }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { getUser } from '@/utils/auth'
import { uploadFile } from '@/utils/upload'

export default {
  data() {
    return {
      peerId: '',
      peer: {},
      myName: '',
      list: [],
      draft: '',
      sending: false,
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
      if (!text || this.sending) return
      this.sending = true
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
          imageUrl: res.imageUrl || '',
          createTime: ''
        })
        this.scrollToBottom()
      } catch (e) { /* toast 已提示 */ }
      this.sending = false
    },
    /** ➕ 发图：选图 → 上传 OSS → 以 imageKey 发送（纯图片消息 text 为空） */
    pickImage() {
      if (this.sending) return
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: async (r) => {
          const p = r.tempFilePaths && r.tempFilePaths[0]
          if (!p) return
          this.sending = true
          try {
            const up = await uploadFile(p, 'image')
            const res = await request({
              url: '/chat/send',
              method: 'POST',
              data: { toUserId: this.peerId, text: '', imageKey: up.key }
            })
            this.list.push({
              messageId: res.messageId,
              mine: true,
              text: '',
              imageUrl: res.imageUrl || '',
              createTime: ''
            })
            this.scrollToBottom()
          } catch (e) { /* toast 已提示 */ }
          this.sending = false
        }
      })
    },
    /** 点图片气泡看大图 */
    previewImg(url) {
      if (!url) return
      uni.previewImage({ urls: [url] })
    }
  }
}
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--dp-bg);
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
  background: var(--dp-card);
  border: 1rpx solid var(--dp-accent-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 16rpx;
}
.mine-avatar {
  background: var(--dp-accent-soft);
  border: none;
  margin-right: 0;
  margin-left: 16rpx;
}
.msg-avatar-text {
  font-size: 26rpx;
  color: var(--dp-brand-deep);
}
.bubble {
  max-width: 62%;
  background: var(--dp-card);
  border-radius: 6rpx 20rpx 20rpx 20rpx;
  padding: 20rpx 24rpx;
}
.bubble.mine {
  background: var(--dp-brand-deep);
  border-radius: 20rpx 6rpx 20rpx 20rpx;
}
.bubble-text {
  font-size: 28rpx;
  color: var(--dp-text);
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
  color: var(--dp-text4);
}
/* 毛玻璃输入栏（原型 v4） */
.input-bar {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  background: rgba(250, 248, 245, .88);
  backdrop-filter: blur(36rpx) saturate(1.6);
  border-top: 1rpx solid var(--dp-line);
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
}
.theme-dark .input-bar {
  background: rgba(20, 17, 14, .88);
}
.chat-input {
  flex: 1;
  background: var(--dp-soft);
  border-radius: 32rpx;
  padding: 16rpx 28rpx;
  font-size: 28rpx;
  height: 44rpx;
}
.ph {
  color: var(--dp-text4);
}
/* 发送：有输入时实心品牌红 */
.send {
  flex-shrink: 0;
  padding: 12rpx 28rpx;
  border-radius: var(--r-pill);
  background: var(--dp-brand-deep);
  box-shadow: var(--sh-float);
}
.send-text {
  font-size: var(--fs-sm);
  color: #ffffff;
  font-weight: 600;
}
/* ➕ 发图入口（与「发送」共用一个位置，纯 CSS 绘制，不占图标资源） */
.plus-btn {
  flex-shrink: 0;
  width: 68rpx;
  height: 68rpx;
  border-radius: var(--r-pill);
  background: var(--dp-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.plus-v,
.plus-h {
  position: absolute;
  background: var(--dp-text2);
  border-radius: 2rpx;
}
.plus-v { width: 4rpx; height: 30rpx; }
.plus-h { width: 30rpx; height: 4rpx; }
/* 图片气泡：去内边距，图随宽度自适应 */
.bubble.img {
  padding: 0;
  overflow: hidden;
  background: var(--dp-soft);
}
.bubble-img {
  display: block;
  width: 320rpx;
  border-radius: 16rpx;
}
</style>
