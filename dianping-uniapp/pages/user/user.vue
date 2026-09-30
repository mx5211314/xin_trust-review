<template>
  <view class="page">
    <!-- 居中头部 -->
    <view class="head">
      <view class="avatar">
        <text class="avatar-text">{{ shortName }}</text>
      </view>
      <text class="nickname">{{ profile ? profile.nickname : '-' }}</text>
      <view class="badge-row">
        <text v-if="isReviewerRole" class="tag tag-reviewer">点评人</text>
        <text v-if="isBanned" class="tag tag-banned">已封禁</text>
      </view>
      <text class="muted bio">{{ profile && profile.role ? regionText : '' }}</text>
    </view>

    <!-- 数据栏 -->
    <view class="stats">
      <view class="stat">
        <text class="stat-num">{{ total }}</text>
        <text class="stat-label">笔记</text>
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
      page: 1,
      pageSize: 10,
      hasMore: true,
      loading: false
    }
  },
  computed: {
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
    async fetch(append) {
      this.loading = true
      try {
        const data = await request({
          url: `/user/${this.userId}?page=${this.page}&pageSize=${this.pageSize}`
        })
        this.profile = data.profile
        this.total = data.contents.total
        this.totalLikes = (data.contents.list || []).reduce((s, c) => s + (c.likeCount || 0), this.totalLikes)
        this.list = append ? this.list.concat(data.contents.list) : data.contents.list
        this.hasMore = this.list.length < this.total
      } catch (e) {
        uni.showToast({ title: '用户不存在', icon: 'none' })
      } finally {
        this.loading = false
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
  background: #ffffff;
}
.head {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 50rpx 0 30rpx;
}
.avatar {
  width: 150rpx;
  height: 150rpx;
  border-radius: 50%;
  background: #ffe8ea;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}
.avatar-text {
  font-size: 60rpx;
  color: #ff2442;
  font-weight: 500;
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
.stats {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f1f3f5;
}
.stat {
  text-align: center;
  padding: 0 70rpx;
}
.stat-num {
  display: block;
  font-size: 34rpx;
  font-weight: 500;
}
.stat-label {
  font-size: 22rpx;
  color: #999999;
}
.stat-div {
  width: 1rpx;
  height: 50rpx;
  background: #f1f3f5;
}
.tabs {
  display: flex;
  justify-content: center;
  border-bottom: 1rpx solid #f1f3f5;
}
.tab {
  padding: 22rpx 40rpx;
  position: relative;
}
.tab-text {
  font-size: 28rpx;
  color: #999999;
}
.tab-text.on {
  color: #1f2430;
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
  background: #f7f8fa;
  min-height: 300rpx;
}
.col {
  flex: 1;
}
.col + .col {
  margin-left: 16rpx;
}
.wcard {
  background: #ffffff;
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
  background: #eceef1;
}
.cover-empty-text {
  font-size: 22rpx;
  color: #b9c0c9;
}
.wtitle {
  display: block;
  padding: 12rpx 14rpx 0;
  font-size: 24rpx;
  font-weight: 500;
  color: #1f2430;
  line-height: 1.4;
}
.wfoot {
  padding: 8rpx 14rpx 14rpx;
}
.wlike-num {
  font-size: 20rpx;
  color: #999999;
}
.empty {
  text-align: center;
  padding: 80rpx 0;
}
</style>
