<template>
  <view class="page">
    <!-- 自定义导航：发现/同城 + 搜索入口 -->
    <view class="topbar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="seg">
        <text class="seg-item" :class="{ on: tab === 'follow' }" @tap="switchTab('follow')">关注</text>
        <text class="seg-item" :class="{ on: tab === 'find' }" @tap="switchTab('find')">发现</text>
        <text class="seg-item" :class="{ on: tab === 'city' }" @tap="switchTab('city')">同城</text>
      </view>
      <view class="sicon msg-entry" @tap="goNotify">
        <image class="msg-icon" src="/static/icons/bubble.png" />
        <view v-if="unread > 0" class="msg-dot"></view>
      </view>
      <view class="sicon search-icon" @tap="goSearch">
        <view class="sicon-ring"></view>
        <view class="sicon-handle"></view>
      </view>
    </view>
    <view :style="{ height: statusBarHeight + 50 + 'px' }"></view>

    <!-- 地区 chips（同城 tab 下显示） -->
    <scroll-view v-if="tab === 'city'" class="chips" scroll-x :show-scrollbar="false">
      <view
        v-for="r in regions"
        :key="r.code"
        class="chip"
        :class="{ active: region === r.code }"
        @tap="switchRegion(r.code)"
      >
        <text class="chip-text" :class="{ active: region === r.code }">{{ r.name }}</text>
      </view>
    </scroll-view>

    <!-- 双列瀑布流 -->
    <view class="waterfall" v-if="list.length">
      <view class="col">
        <view v-for="item in leftList" :key="item.contentId" class="wcard" hover-class="card-hover" @tap="goDetail(item)">
          <view class="cover-wrap" :style="{ height: coverH(item) + 'rpx' }">
            <image
              v-if="item.coverUrl || (item.images && item.images.length)"
              class="cover"
              :src="item.coverUrl || item.images[0]"
              mode="aspectFill"
              lazy-load
            />
            <view v-else class="cover cover-empty">
              <text class="cover-empty-text">视频</text>
            </view>
            <view v-if="item.videoUrl" class="video-badge">
              <text class="video-badge-text">视频</text>
            </view>
            <view v-else-if="item.images && item.images.length > 1" class="img-badge">
              <text class="img-badge-text">⧉ {{ item.images.length }}</text>
            </view>
          </view>
          <view class="wbody">
            <text class="wtitle">{{ item.title }}</text>
            <view class="wfoot">
              <view class="wauthor">
                <view class="avatar-ph wavatar">
                  <text class="wavatar-text">{{ shortName(item.author) }}</text>
                </view>
                <text class="wnick">{{ item.author ? item.author.nickname : '匿名' }}</text>
              </view>
              <view class="wlike" @tap.stop="doLike(item)">
                <image class="wlike-img" :src="item.liked ? '/static/icons/heart-on.png' : '/static/icons/heart.png'" />
                <text class="wlike-num">{{ item.likeCount }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
      <view class="col">
        <view v-for="item in rightList" :key="item.contentId" class="wcard" hover-class="card-hover" @tap="goDetail(item)">
          <view class="cover-wrap" :style="{ height: coverH(item) + 'rpx' }">
            <image
              v-if="item.coverUrl || (item.images && item.images.length)"
              class="cover"
              :src="item.coverUrl || item.images[0]"
              mode="aspectFill"
              lazy-load
            />
            <view v-else class="cover cover-empty">
              <text class="cover-empty-text">视频</text>
            </view>
            <view v-if="item.videoUrl" class="video-badge">
              <text class="video-badge-text">视频</text>
            </view>
            <view v-else-if="item.images && item.images.length > 1" class="img-badge">
              <text class="img-badge-text">⧉ {{ item.images.length }}</text>
            </view>
          </view>
          <view class="wbody">
            <text class="wtitle">{{ item.title }}</text>
            <view class="wfoot">
              <view class="wauthor">
                <view class="avatar-ph wavatar">
                  <text class="wavatar-text">{{ shortName(item.author) }}</text>
                </view>
                <text class="wnick">{{ item.author ? item.author.nickname : '匿名' }}</text>
              </view>
              <view class="wlike" @tap.stop="doLike(item)">
                <image class="wlike-img" :src="item.liked ? '/static/icons/heart-on.png' : '/static/icons/heart.png'" />
                <text class="wlike-num">{{ item.likeCount }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 骨架屏 -->
    <view class="waterfall" v-if="loading && !list.length">
      <view class="col">
        <view class="wcard"><view class="skeleton sk-img"></view><view class="wbody"><view class="skeleton sk-line"></view></view></view>
        <view class="wcard"><view class="skeleton sk-img short"></view><view class="wbody"><view class="skeleton sk-line"></view></view></view>
      </view>
      <view class="col">
        <view class="wcard"><view class="skeleton sk-img short"></view><view class="wbody"><view class="skeleton sk-line"></view></view></view>
        <view class="wcard"><view class="skeleton sk-img"></view><view class="wbody"><view class="skeleton sk-line"></view></view></view>
      </view>
    </view>

    <view v-if="!loading && !list.length" class="empty">
      <text v-if="tab === 'follow'" class="muted">关注点评人后，这里展示 TA 们的最新笔记</text>
      <text v-else class="muted">这里还没有点评，等第一位点评人吧</text>
    </view>
    <view v-if="loading && list.length" class="load-more">
      <text class="load-more-text">加载中…</text>
    </view>
    <view v-if="list.length && !hasMore" class="empty">
      <text class="muted">— 到底啦 —</text>
    </view>
    <view style="height: 30rpx"></view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { REGIONS } from '@/utils/config'
import { isLogin } from '@/utils/auth'

export default {
  data() {
    return {
      regions: REGIONS,
      tab: 'find',
      region: '130100',
      followedIds: [],
      list: [],
      page: 1,
      pageSize: 10,
      total: 0,
      hasMore: true,
      loading: false,
      unread: 0,
      statusBarHeight: 20
    }
  },
  computed: {
    leftList() {
      return this.list.filter((_, i) => i % 2 === 0)
    },
    rightList() {
      return this.list.filter((_, i) => i % 2 === 1)
    }
  },
  onLoad() {
    try {
      this.statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 20
    } catch (e) { /* 默认值兜底 */ }
  },
  onShow() {
    if (!isLogin()) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.refresh()
    this.loadUnread()
  },
  onPullDownRefresh() {
    this.refresh().finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    if (this.hasMore && !this.loading) {
      this.page += 1
      this.fetch(false)
    }
  },
  methods: {
    shortName(author) {
      return author && author.nickname ? author.nickname.slice(0, 1) : '客'
    },
    /** 封面高度交替，模拟瀑布流错落 */
    coverH(item) {
      if (item.videoUrl) return 200
      const idx = this.list.indexOf(item)
      return idx % 4 < 2 ? 340 : 260
    },
    switchTab(t) {
      if (this.tab === t) return
      this.tab = t
      if (t === 'city') {
        this.region = this.region || '130100'
      } else {
        this.region = ''
      }
      this.refresh()
    },
    switchRegion(code) {
      this.region = code
      this.refresh()
    },
    goSearch() {
      uni.navigateTo({ url: '/pages/search/search' })
    },
    goNotify() {
      uni.navigateTo({ url: '/pages/notify/notify' })
    },
    loadUnread() {
      Promise.all([
        request({ url: '/user/notify/unread' }).catch(() => ({ unread: 0 })),
        request({ url: '/chat/unread' }).catch(() => ({ unread: 0 }))
      ]).then(([a, b]) => {
        this.unread = Number(a.unread || 0) + Number(b.unread || 0)
      })
    },
    goDetail(item) {
      uni.navigateTo({ url: '/pages/detail/detail?id=' + item.contentId })
    },
    async refresh() {
      this.page = 1
      this.hasMore = true
      await this.fetch(true)
    },
    async fetch(reset) {
      this.loading = true
      try {
        let data
        if (this.tab === 'follow') {
          data = await request({
            url: `/content/follow-feed?page=${this.page}&pageSize=${this.pageSize}`
          })
        } else {
          const region = this.tab === 'city' ? this.region : ''
          data = await request({
            url: `/content/feed?page=${this.page}&pageSize=${this.pageSize}` +
              (region ? `&regionCode=${region}` : '')
          })
        }
        this.total = data.total
        this.list = reset ? data.list : this.list.concat(data.list)
        this.hasMore = this.list.length < data.total
      } catch (e) {
        if (reset) this.list = []
      } finally {
        this.loading = false
      }
    },
    async doLike(item) {
      if (item.liked) {
        try {
          await request({ url: `/content/${item.contentId}/like`, method: 'DELETE', silent: true })
          item.liked = false
          item.likeCount = Math.max(0, item.likeCount - 1)
          uni.showToast({ title: '已取消点赞', icon: 'none', duration: 800 })
        } catch (e) { /* ignore */ }
      } else {
        try {
          await request({ url: `/content/${item.contentId}/like`, method: 'POST', silent: true })
          item.liked = true
          item.likeCount += 1
          uni.showToast({ title: '已点赞 ♥', icon: 'none', duration: 800 })
        } catch (e) {
          if (e.code === 2003) item.liked = true
        }
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
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 50px;
  box-sizing: content-box;
}
.seg {
  display: flex;
  align-items: center;
}
.seg-item {
  font-size: 30rpx;
  color: #999999;
  margin: 0 24rpx;
  position: relative;
}
.seg-item.on {
  color: #1f2430;
  font-weight: 500;
}
.seg-item.on::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: -10rpx;
  width: 40rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: #ff2442;
}
.sicon {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36rpx;
  height: 36rpx;
}
.search-icon {
  right: 32rpx;
}
.msg-entry {
  right: 110rpx;
}
.msg-icon {
  width: 40rpx;
  height: 40rpx;
}
.msg-dot {
  position: absolute;
  right: -6rpx;
  top: -6rpx;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #ff2442;
  border: 2rpx solid #ffffff;
}
.sicon-ring {
  width: 24rpx;
  height: 24rpx;
  border: 4rpx solid #1f2430;
  border-radius: 50%;
}
.sicon-handle {
  width: 14rpx;
  height: 4rpx;
  background: #1f2430;
  border-radius: 2rpx;
  transform: rotate(45deg);
  margin-top: -4rpx;
  margin-left: 20rpx;
}
.chips {
  white-space: nowrap;
  padding: 12rpx 24rpx 4rpx;
  background: #ffffff;
}
.chip {
  display: inline-block;
  padding: 10rpx 28rpx;
  border-radius: 999rpx;
  background: #f6f7f9;
  margin-right: 16rpx;
}
.chip.active {
  background: #ffe8ea;
}
.chip-text {
  font-size: 24rpx;
  color: #6b7280;
}
.chip-text.active {
  color: #ff2442;
  font-weight: 500;
}
.waterfall {
  display: flex;
  padding: 0 16rpx;
}
.col {
  flex: 1;
}
.col + .col {
  margin-left: 16rpx;
}
.wcard {
  background: #ffffff;
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.04);
}
.cover-wrap {
  position: relative;
  width: 100%;
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
  background: #eceef1;
}
.cover-empty-text {
  font-size: 24rpx;
  color: #b9c0c9;
}
.video-badge {
  position: absolute;
  top: 12rpx;
  left: 12rpx;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
}
.video-badge-text {
  color: #ffffff;
  font-size: 18rpx;
}
.wbody {
  padding: 14rpx 16rpx 16rpx;
}
.wtitle {
  display: block;
  font-size: 26rpx;
  font-weight: 500;
  color: #1f2430;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.wfoot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14rpx;
}
.wauthor {
  display: flex;
  align-items: center;
  min-width: 0;
}
.wavatar {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #cecbf6;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.wavatar-text {
  font-size: 20rpx;
  color: #3c3489;
}
.wnick {
  margin-left: 10rpx;
  font-size: 22rpx;
  color: #999999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.wlike {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.wlike-img {
  width: 30rpx;
  height: 30rpx;
}
.wlike-num {
  margin-left: 6rpx;
  font-size: 22rpx;
  color: #999999;
}
.sk-img {
  height: 300rpx;
  border-radius: 0;
}
.sk-img.short {
  height: 220rpx;
}
.sk-line {
  height: 28rpx;
  margin: 14rpx 16rpx;
  border-radius: 6rpx;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
.load-more {
  text-align: center;
  padding: 26rpx 0 46rpx;
}
.load-more-text {
  font-size: 24rpx;
  color: #b9c0c9;
}
.card-hover {
  transform: scale(0.97);
  opacity: 0.85;
}
.wcard {
  transition: transform 0.15s ease, opacity 0.15s ease;
}
.img-badge {
  position: absolute;
  right: 12rpx;
  top: 12rpx;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
}
.img-badge-text {
  color: #ffffff;
  font-size: 18rpx;
}
.wlike-icon.liked {
  animation: pop 0.3s ease;
}
@keyframes pop {
  0% { transform: scale(1); }
  50% { transform: scale(1.4); }
  100% { transform: scale(1); }
}
</style>
