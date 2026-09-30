<template>
  <view class="page" v-if="content">
    <!-- 驳回提示（仅作者可见） -->
    <view v-if="isRejectedMine" class="reject-bar">
      <text class="reject-text">未通过审核：{{ content.rejectReason || '内容不符合规范' }}</text>
    </view>

    <!-- 沉浸式图区（黑底，custom 导航） -->
    <view class="media" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-ghost">
        <view class="nav-btn" @tap="goBack">
          <text class="nav-btn-text">‹</text>
        </view>
      </view>
      <swiper
        v-if="content.images && content.images.length"
        class="banner"
        indicator-dots
        indicator-color="rgba(255,255,255,0.4)"
        indicator-active-color="#FFFFFF"
        circular
      >
        <swiper-item v-for="(img, i) in content.images" :key="i">
          <image class="banner-img" :src="img" mode="aspectFill" @tap="preview(i)" />
        </swiper-item>
      </swiper>
      <video
        v-else-if="content.videoUrl"
        class="banner"
        :src="content.videoUrl"
        :poster="content.coverUrl"
        controls
        object-fit="contain"
      />
      <view v-else class="banner banner-empty">
        <text class="banner-empty-text">视频处理中</text>
      </view>
    </view>

    <!-- 内容区 -->
    <view class="body">
      <text class="title">{{ content.title }}</text>
      <view class="author-row" @tap="goAuthor">
        <view class="avatar-ph avatar">
          <text class="avatar-text">{{ shortName }}</text>
        </view>
        <text class="nickname">{{ content.author ? content.author.nickname : '匿名' }}</text>
        <text v-if="isReviewerAuthor" class="tag tag-reviewer">点评人</text>
      </view>
      <text class="text">{{ content.text }}</text>

      <!-- 话题标签 -->
      <view class="topic-row" v-if="content.tags && content.tags.length">
        <text v-for="(t, i) in content.tags" :key="i" class="topic">#{{ t }}</text>
      </view>

      <!-- 地点 -->
      <view class="poi-row" v-if="content.poiName">
        <text class="poi">📍 {{ content.poiName }}</text>
        <text class="region" v-if="content.regionCode">{{ regionName(content.regionCode) }}</text>
      </view>
      <text class="time">编辑于 {{ content.createTime }}</text>
    </view>

    <!-- 评论区 -->
    <view class="comments">
      <view class="comments-head">
        <text class="comments-title">评论 {{ commentTotal }}</text>
      </view>
      <view
        v-for="c in comments"
        :key="c.commentId"
        class="comment-item"
        :class="{ reply: c.parentId !== '0' }"
      >
        <view class="cavatar">
          <text class="cavatar-text">{{ c.user ? c.user.nickname.slice(0, 1) : '客' }}</text>
        </view>
        <view class="cbody">
          <view class="cinfo">
            <text class="cnick">{{ c.user ? c.user.nickname : '匿名' }}</text>
            <view class="cinfo-r">
              <text class="creply" @tap="tapReply(c)">回复</text>
              <text
                v-if="isMyComment(c)"
                class="cdel"
                @tap="delComment(c)"
              >删除</text>
              <text class="ctime">{{ c.createTime }}</text>
            </view>
          </view>
          <text class="ctext">
            <text v-if="c.parentId !== '0' && c.replyToNickname" class="reply-tag">回复 @{{ c.replyToNickname }}：</text>{{ c.text }}
          </text>
        </view>
      </view>
      <view v-if="!comments.length" class="empty">
        <text class="muted">还没有评论，来抢沙发～</text>
      </view>
    </view>

    <!-- 悬浮操作栏（评论输入实装，支持回复） -->
    <view class="footer">
      <view v-if="replyTarget" class="replying">
        <text class="replying-text">回复 @{{ replyTarget.nickname }}</text>
        <text class="replying-cancel" @tap="replyTarget = null">✕</text>
      </view>
      <input
        v-model="commentText"
        class="comment-input"
        :placeholder="replyTarget ? '回复 @' + replyTarget.nickname + '...' : '说点什么...'"
        placeholder-class="ph"
        confirm-type="send"
        @confirm="sendComment"
      />
      <view class="send-btn" :class="{ on: commentText.trim() }" @tap="sendComment">
        <text class="send-text">发送</text>
      </view>
      <view class="act" @tap="doLike">
        <text class="act-icon" :class="{ liked: content.liked }">{{ content.liked ? '♥' : '♡' }}</text>
        <text class="act-num">{{ content.likeCount }}</text>
      </view>
      <view class="act" @tap="doFav">
        <text class="act-icon star" :class="{ faved: favorited }">{{ favorited ? '★' : '☆' }}</text>
        <text class="act-num">{{ favoriteCount }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { regionName } from '@/utils/config'
import { getUser } from '@/utils/auth'

export default {
  data() {
    return {
      id: '',
      content: null,
      statusBarHeight: 20,
      comments: [],
      commentTotal: 0,
      commentText: '',
      replyTarget: null,
      favorited: false,
      favoriteCount: 0
    }
  },
  computed: {
    isRejectedMine() {
      const me = getUser()
      return (
        this.content &&
        this.content.status === 'REJECTED' &&
        me &&
        this.content.author &&
        String(me.userId) === String(this.content.author.userId)
      )
    },
    isReviewerAuthor() {
      return this.content && this.content.author
    },
    shortName() {
      const a = this.content && this.content.author
      return a && a.nickname ? a.nickname.slice(0, 1) : '客'
    }
  },
  onLoad(query) {
    this.id = query.id
    try {
      this.statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 20
    } catch (e) { /* 默认值兜底 */ }
  },
  onShow() {
    this.fetch()
  },
  methods: {
    regionName,
    async fetch() {
      try {
        this.content = await request({ url: `/content/${this.id}` })
        if (this.content) {
          this.favorited = !!this.content.favorited
          this.favoriteCount = this.content.favoriteCount || 0
          if (this.content.status === 'APPROVED') {
            // 浏览计数上报（静默，失败不影响展示）
            request({ url: `/content/${this.id}/view`, method: 'POST', silent: true }).catch(() => {})
          }
          this.loadComments()
        }
      } catch (e) {
        uni.showToast({ title: '内容不存在', icon: 'none' })
        setTimeout(() => uni.navigateBack(), 800)
      }
    },
    async loadComments() {
      try {
        const data = await request({
          url: `/content/${this.id}/comments?page=1&pageSize=50`
        })
        this.comments = data.list || []
        this.commentTotal = data.total || this.comments.length
      } catch (e) { /* ignore */ }
    },
    isMyComment(c) {
      const me = getUser()
      return !!(me && c.user && String(me.userId) === String(c.user.userId))
    },
    delComment(c) {
      uni.showModal({
        title: '删除评论',
        content: c.text,
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({
              url: `/content/${this.id}/comments/${c.commentId}`,
              method: 'DELETE',
              silent: true
            })
            this.commentTotal = Math.max(0, this.commentTotal - 1)
            this.comments = this.comments.filter(x => x.commentId !== c.commentId)
            uni.showToast({ title: '已删除', icon: 'none' })
          } catch (e) { /* toast 已提示 */ }
        }
      })
    },
    async sendComment() {
      const text = this.commentText.trim()
      if (!text) return
      try {
        await request({
          url: `/content/${this.id}/comments`,
          method: 'POST',
          data: {
            text,
            parentId: this.replyTarget ? this.replyTarget.commentId : undefined
          }
        })
        this.commentText = ''
        this.replyTarget = null
        this.commentTotal += 1
        this.loadComments()
        uni.showToast({ title: '评论成功', icon: 'none' })
      } catch (e) { /* toast 已提示 */ }
    },
    tapReply(c) {
      this.replyTarget = {
        commentId: c.commentId,
        nickname: c.user ? c.user.nickname : '匿名'
      }
    },
    async doFav() {
      if (this.favorited) {
        try {
          await request({ url: `/content/${this.id}/favorite`, method: 'DELETE', silent: true })
          this.favorited = false
          this.favoriteCount = Math.max(0, this.favoriteCount - 1)
        } catch (e) { /* ignore */ }
      } else {
        try {
          await request({ url: `/content/${this.id}/favorite`, method: 'POST', silent: true })
          this.favorited = true
          this.favoriteCount += 1
        } catch (e) {
          if (e.code === 2003) this.favorited = true
        }
      }
    },
    preview(index) {
      uni.previewImage({ urls: this.content.images, current: index })
    },
    goBack() {
      uni.navigateBack()
    },
    goAuthor() {
      if (this.content.author) {
        uni.navigateTo({
          url: '/pages/user/user?userId=' + this.content.author.userId
        })
      }
    },
    async doLike() {
      const c = this.content
      if (c.liked) {
        try {
          await request({ url: `/content/${c.contentId}/like`, method: 'DELETE', silent: true })
          c.liked = false
          c.likeCount = Math.max(0, c.likeCount - 1)
        } catch (e) { /* ignore */ }
      } else {
        try {
          await request({ url: `/content/${c.contentId}/like`, method: 'POST', silent: true })
          c.liked = true
          c.likeCount += 1
        } catch (e) {
          if (e.code === 2003) c.liked = true
        }
      }
    }
  },
  onShareAppMessage() {
    return { title: this.content ? this.content.title : '本地点评' }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #ffffff;
  padding-bottom: 150rpx;
}
.reject-bar {
  background: #fcebeb;
  padding: 20rpx 24rpx;
}
.reject-text {
  color: #a32d2d;
  font-size: 26rpx;
}
.media {
  background: #111111;
  position: relative;
}
.nav-ghost {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  padding: 10rpx 24rpx;
}
.nav-btn {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-btn-text {
  color: #ffffff;
  font-size: 40rpx;
  line-height: 40rpx;
  margin-top: -6rpx;
}
.banner {
  width: 100%;
  height: 560rpx;
}
.banner-img {
  width: 100%;
  height: 560rpx;
}
.banner-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}
.banner-empty-text {
  color: #666666;
  font-size: 26rpx;
}
.body {
  padding: 28rpx 28rpx 0;
}
.title {
  display: block;
  font-size: 36rpx;
  font-weight: 500;
  line-height: 1.45;
  margin-bottom: 24rpx;
}
.author-row {
  display: flex;
  align-items: center;
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid #f1f3f5;
}
.avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #ffe8ea;
  margin-right: 16rpx;
}
.avatar-ph {
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text {
  font-size: 28rpx;
  color: #ff2442;
}
.nickname {
  font-size: 28rpx;
  color: #1f2430;
  margin-right: 12rpx;
}
.text {
  display: block;
  margin-top: 26rpx;
  font-size: 30rpx;
  line-height: 1.75;
  color: #333333;
  white-space: pre-wrap;
}
.topic-row {
  margin-top: 26rpx;
  display: flex;
  flex-wrap: wrap;
}
.topic {
  font-size: 26rpx;
  color: #4a90d9;
  margin-right: 22rpx;
}
.poi-row {
  margin-top: 26rpx;
  display: flex;
  align-items: center;
}
.poi {
  font-size: 28rpx;
  color: #ff2442;
  font-weight: 500;
  margin-right: 20rpx;
}
.region {
  font-size: 24rpx;
  color: #999999;
}
.time {
  display: block;
  margin-top: 22rpx;
  font-size: 22rpx;
  color: #b9c0c9;
}
.comments {
  margin-top: 30rpx;
  border-top: 12rpx solid #f7f8fa;
  padding: 24rpx 28rpx;
}
.comments-head {
  margin-bottom: 10rpx;
}
.comments-title {
  font-size: 30rpx;
  font-weight: 500;
}
.comment-item {
  display: flex;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f6f7f9;
}
.cavatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: #ffe8ea;
  margin-right: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.cavatar-text {
  font-size: 26rpx;
  color: #ff2442;
}
.cbody {
  flex: 1;
}
.cinfo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}
.cnick {
  font-size: 24rpx;
  color: #666666;
}
.ctime {
  font-size: 20rpx;
  color: #c2c8d0;
}
.ctext {
  font-size: 28rpx;
  color: #1f2430;
  line-height: 1.5;
}
.empty {
  text-align: center;
  padding: 40rpx 0;
}
.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #ffffff;
  border-top: 1rpx solid #f1f3f5;
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
}
.comment-input {
  flex: 1;
  background: #f6f7f9;
  border-radius: 32rpx;
  padding: 14rpx 28rpx;
  font-size: 26rpx;
  margin-right: 16rpx;
}
.ph {
  color: #b9c0c9;
}
.send-btn {
  padding: 12rpx 24rpx;
  margin-right: 10rpx;
}
.send-btn.on {
  background: #ffe8ea;
  border-radius: 24rpx;
}
.send-text {
  color: #ff2442;
  font-size: 26rpx;
  font-weight: 500;
}
.act {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-left: 22rpx;
}
.act-icon {
  font-size: 40rpx;
  color: #999999;
}
.act-icon.liked {
  color: #ff2442;
}
.act-icon.star {
  color: #ff8a3d;
  font-size: 38rpx;
}
.act-icon.star.faved {
  color: #ff8a3d;
}
.act-num {
  font-size: 20rpx;
  color: #999999;
  margin-top: 2rpx;
}
.comment-item.reply {
  margin-left: 60rpx;
  background: #fafafa;
  border-radius: 12rpx;
  padding: 18rpx 20rpx;
}
.reply-tag {
  color: #4a90d9;
}
.replying {
  display: flex;
  align-items: center;
  background: #fff1e6;
  border-radius: 12rpx;
  padding: 8rpx 18rpx;
  margin-right: 14rpx;
}
.replying-text {
  font-size: 22rpx;
  color: #ff8a3d;
}
.replying-cancel {
  color: #999999;
  font-size: 22rpx;
  margin-left: 12rpx;
}
.creply {
  font-size: 22rpx;
  color: #4a90d9;
  margin-right: 16rpx;
}
.cinfo-r {
  display: flex;
  align-items: center;
}
.cdel {
  font-size: 22rpx;
  color: #ff2442;
  margin-right: 16rpx;
}
</style>
