<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <!-- 居中头部 -->
    <view class="head">
      <view class="avatar">
        <image v-if="profile && profile.avatar" class="avatar-img" :src="profile.avatar" mode="aspectFill" />
        <text v-else class="avatar-text">{{ shortName }}</text>
      </view>
      <text class="nickname">{{ profile ? profile.nickname : '-' }}</text>
      <view class="badge-row">
        <view v-if="isReviewerRole" class="tag tag-reviewer">
          <image class="tag-star" src="/static/icons/star-red.png" mode="aspectFit" />
          <text>{{ profile.role === 'ADMIN' ? '管理员' : '点评人' }}</text>
        </view>
        <text v-if="isBanned" class="tag tag-banned">已封禁</text>
      </view>
      <text class="muted bio">{{ profile ? (profile.bio || regionText) : '' }}</text>
      <view v-if="!isMine" class="btn-row">
        <view class="follow-btn" :class="{ on: isFollowing }" @tap="toggleFollow">
          <text class="btn-text" :class="{ on: isFollowing }">{{ isFollowing ? '已关注' : '+ 关注' }}</text>
        </view>
        <view class="chat-btn" @tap="goChat">
          <text class="chat-btn-text">私信</text>
        </view>
        <view class="more-btn" @tap="openMore">
          <text class="more-btn-text">···</text>
        </view>
      </view>
    </view>

    <!-- 数据栏 -->
    <view class="stats">
      <view class="stat">
        <text class="stat-num">{{ total }}</text>
        <text class="stat-label">笔记</text>
      </view>
      <view class="stat-div"></view>
      <view class="stat">
        <text class="stat-num">{{ followers }}</text>
        <text class="stat-label">粉丝</text>
      </view>
      <view class="stat-div"></view>
      <view class="stat">
        <text class="stat-num">{{ totalLikes }}</text>
        <text class="stat-label">获赞</text>
      </view>
    </view>

    <view class="tabs">
      <view class="tab" @tap="noop">
        <text class="tab-text on">笔记</text>
        <view class="tab-line"></view>
      </view>
    </view>

    <!-- 双列瀑布流 -->
    <view class="waterfall" v-if="list.length">
      <view class="col">
        <view v-for="item in leftList" :key="item.contentId" class="wcard" @tap="goDetail(item)">
          <view class="cover-wrap">
            <image
              v-if="item.coverUrl || (item.images && item.images.length)"
              class="cover"
              :src="item.coverUrl || item.images[0]"
              mode="aspectFill"
            />
            <view v-else class="cover cover-empty">
              <text class="cover-empty-text">视频</text>
            </view>
          </view>
          <text class="wtitle">{{ item.title }}</text>
          <view class="wfoot">
            <text class="wlike-num">{{ item.likeCount }} 赞</text>
          </view>
        </view>
      </view>
      <view class="col">
        <view v-for="item in rightList" :key="item.contentId" class="wcard" @tap="goDetail(item)">
          <view class="cover-wrap">
            <image
              v-if="item.coverUrl || (item.images && item.images.length)"
              class="cover"
              :src="item.coverUrl || item.images[0]"
              mode="aspectFill"
            />
            <view v-else class="cover cover-empty">
              <text class="cover-empty-text">视频</text>
            </view>
          </view>
          <text class="wtitle">{{ item.title }}</text>
          <view class="wfoot">
            <text class="wlike-num">{{ item.likeCount }} 赞</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="!loading && !list.length" class="empty">
      <text class="muted">还没有发布过笔记</text>
    </view>
    <view style="height: 40rpx"></view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { getUser } from '@/utils/auth'

export default {
  data() {
    return {
      userId: '',
      isMine: false,
      profile: null,
      list: [],
      total: 0,
      totalLikes: 0,
      followers: 0,
      isFollowing: false,
      isBlocked: false,
      page: 1,
      pageSize: 10,
      hasMore: true,
      loading: false
    }
  },
  computed: {
    /** 瀑布流左右列（建页时漏了这个 computed，导致他人主页列表永远空白） */
    leftList() {
      return this.list.filter((_, i) => i % 2 === 0)
    },
    rightList() {
      return this.list.filter((_, i) => i % 2 === 1)
    },
    isReviewerRole() {
      return this.profile && (this.profile.role === 'REVIEWER' || this.profile.role === 'ADMIN')
    },
    isBanned() {
      return this.profile && this.profile.status === 'BANNED'
    },
    shortName() {
      return this.profile && this.profile.nickname ? this.profile.nickname.slice(0, 1) : '客'
    },
    regionText() {
      return this.isMine ? '这是你的高端主页' : 'TA 的每一条点评都值得看'
    }
  },
  onLoad(query) {
    this.userId = query.userId
    const me = getUser()
    this.isMine = !!me && String(me.userId) === String(this.userId)
  },
  onShow() {
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
    noop() { /* 占位 */ },
    goDetail(item) {
      uni.navigateTo({ url: '/pages/detail/detail?id=' + item.contentId })
    },
    async fetch(append) {
      this.loading = true
      try {
        const data = await request({
          url: `/user/${this.userId}?page=${this.page}&pageSize=${this.pageSize}`
        })
        this.profile = data.profile
        this.followers = data.profile.followers || 0
        this.isFollowing = !!data.profile.following
        this.total = data.contents.total
        if (!this.isMine) this.loadBlockState()
        this.totalLikes = (data.contents.list || []).reduce((s, c) => s + (c.likeCount || 0), this.totalLikes)
        this.list = append ? this.list.concat(data.contents.list) : data.contents.list
        this.hasMore = this.list.length < this.total
      } catch (e) {
        uni.showToast({ title: '用户不存在', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    async toggleFollow() {
      const uid = this.userId
      if (this.isFollowing) {
        try {
          await request({ url: `/user/${uid}/follow`, method: 'DELETE', silent: true })
          this.isFollowing = false
          this.followers = Math.max(0, this.followers - 1)
        } catch (e) { /* ignore */ }
      } else {
        try {
          await request({ url: `/user/${uid}/follow`, method: 'POST', silent: true })
          this.isFollowing = true
          this.followers += 1
          uni.showToast({ title: '关注成功，可在首页"关注"tab 看到TA的更新', icon: 'none', duration: 2500 })
        } catch (e) {
          if (e.code === 2003) this.isFollowing = true
        }
      }
    },
    goChat() {
      const name = this.profile ? this.profile.nickname : ''
      uni.navigateTo({
        url: `/pages/chat/chat?userId=${this.userId}&nickname=${encodeURIComponent(name || '')}`
      })
    },
    /** 拉黑/举报 菜单 */
    openMore() {
      const items = [this.isBlocked ? '取消拉黑' : '拉黑TA', '举报TA']
      uni.showActionSheet({
        itemList: items,
        success: (res) => {
          if (items[res.tapIndex] === '拉黑TA') this.toggleBlock(true)
          else if (items[res.tapIndex] === '取消拉黑') this.toggleBlock(false)
          else this.reportUser()
        }
      })
    },
    async toggleBlock(block) {
      try {
        if (block) {
          await request({ url: `/user/${this.userId}/block`, method: 'POST', silent: true })
          this.isBlocked = true
          uni.showToast({ title: '已拉黑，不再看到TA的内容', icon: 'none' })
        } else {
          await request({ url: `/user/${this.userId}/block`, method: 'DELETE', silent: true })
          this.isBlocked = false
          uni.showToast({ title: '已解除拉黑', icon: 'none' })
        }
      } catch (e) { /* ignore */ }
    },
    reportUser() {
      const reasons = ['色情低俗', '广告诈骗', '违法违规', '不实信息', '其他']
      uni.showActionSheet({
        itemList: reasons,
        success: async (res) => {
          try {
            await request({
              url: '/report',
              method: 'POST',
              data: { targetType: 'USER', targetId: this.userId, reason: reasons[res.tapIndex] },
              silent: true
            })
            uni.showToast({ title: '举报已提交，感谢反馈', icon: 'none' })
          } catch (e) {
            if (e && e.code === 2003) uni.showToast({ title: '已举报，等待处理', icon: 'none' })
          }
        }
      })
    },
    async loadBlockState() {
      try {
        const list = await request({ url: '/user/blocks', silent: true }) || []
        this.isBlocked = list.some(b => String(b.userId) === String(this.userId))
      } catch (e) {
        this.isBlocked = false
      }
    },
    goDetail(item) {
      uni.navigateTo({ url: '/pages/detail/detail?id=' + item.contentId })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--dp-card);
}
.head {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 50rpx 0 30rpx;
  background: linear-gradient(180deg, #ffe4e9, #fff1f3 60%, rgba(255, 255, 255, 0));
}
.avatar {
  width: 150rpx;
  height: 150rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffb199, #ff2442);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
  border: 4rpx solid #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(255, 36, 66, 0.18);
}
.avatar-text {
  font-size: 60rpx;
  color: #ffffff;
  font-weight: 500;
}
.avatar-img {
  width: 150rpx;
  height: 150rpx;
  border-radius: 50%;
  display: block;
}
.nickname {
  font-size: 36rpx;
  font-weight: 500;
  margin-bottom: 14rpx;
}
.badge-row {
  margin-bottom: 6rpx;
}
.bio {
  margin-top: 10rpx;
}
.btn-row {
  display: flex;
  align-items: center;
  margin-top: 24rpx;
}
.chat-btn {
  margin-left: 20rpx;
  background: var(--dp-soft);
  border-radius: 40rpx;
  padding: 0 58rpx;
  height: 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-btn-text {
  color: var(--dp-text);
  font-size: 28rpx;
}
.more-btn {
  margin-left: 20rpx;
  background: var(--dp-soft);
  border-radius: 40rpx;
  width: 70rpx;
  height: 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.more-btn-text {
  color: var(--dp-text);
  font-size: 36rpx;
  letter-spacing: 2rpx;
  margin-top: -6rpx;
}
.follow-btn {
  background: #ff2442;
  border-radius: 40rpx;
  padding: 0 66rpx;
  height: 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.follow-btn.on {
  background: var(--dp-soft);
}
.btn-text {
  color: #ffffff;
  font-size: 28rpx;
}
.btn-text.on {
  color: var(--dp-text2);
}
.stats {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid var(--dp-line);
}
.stat {
  text-align: center;
  padding: 0 70rpx;
}
.stat-num {
  display: block;
  font-family: Georgia, serif;
  font-style: italic;
  font-size: 38rpx;
  font-weight: 700;
}
.stat-label {
  font-size: 22rpx;
  color: var(--dp-text3);
}
.stat-div {
  width: 1rpx;
  height: 50rpx;
  background: var(--dp-line);
}
.tabs {
  display: flex;
  justify-content: center;
  border-bottom: 1rpx solid var(--dp-line);
}
.tab {
  padding: 22rpx 40rpx;
  position: relative;
}
.tab-text {
  font-size: 28rpx;
  color: var(--dp-text3);
}
.tab-text.on {
  color: var(--dp-text);
  font-weight: 500;
}
.tab-line {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48rpx;
  height: 5rpx;
  border-radius: 3rpx;
  background: #ff2442;
}
.waterfall {
  display: flex;
  padding: 20rpx 16rpx;
  background: var(--dp-bg);
  min-height: 300rpx;
}
.col {
  flex: 1;
}
.col + .col {
  margin-left: 16rpx;
}
.wcard {
  background: var(--dp-card);
  border-radius: 14rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.04);
}
.cover-wrap {
  width: 100%;
  height: 240rpx;
}
.cover {
  width: 100%;
  height: 100%;
  display: block;
}
.cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: var(--dp-soft);
}
.cover-empty-text {
  font-size: 22rpx;
  color: var(--dp-text4);
}
.wtitle {
  display: block;
  padding: 12rpx 14rpx 0;
  font-size: 24rpx;
  font-weight: 500;
  color: var(--dp-text);
  line-height: 1.4;
}
.wfoot {
  padding: 8rpx 14rpx 14rpx;
}
.wlike-num {
  font-size: 20rpx;
  color: var(--dp-text3);
}
.empty {
  text-align: center;
  padding: 80rpx 0;
}
</style>
