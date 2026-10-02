<template>
  <view class="hub" :class="{'theme-dark': isDark}">
    <view class="hub-head">
      <text class="hub-title">{{ mode === 'topic' ? '#' + q : q }}</text>
      <text class="hub-sub">{{ total }} 篇内容</text>
      <view v-if="mode === 'topic'" class="follow-btn" :class="{ on: isFollowingTopic }" @tap="toggleFollowTopic">
        <text class="follow-btn-text">{{ isFollowingTopic ? '已关注' : '关注话题' }}</text>
      </view>
    </view>

    <view v-if="loading && !list.length" class="waterfall">
      <view class="col">
        <view class="wcard"><view class="skeleton sk-img"></view><view class="wbody"><view class="skeleton sk-line"></view></view></view>
        <view class="wcard"><view class="skeleton sk-img short"></view><view class="wbody"><view class="skeleton sk-line"></view></view></view>
      </view>
      <view class="col">
        <view class="wcard"><view class="skeleton sk-img short"></view><view class="wbody"><view class="skeleton sk-line"></view></view></view>
        <view class="wcard"><view class="skeleton sk-img"></view><view class="wbody"><view class="skeleton sk-line"></view></view></view>
      </view>
    </view>

    <view v-else-if="!list.length" class="empty">
      <text class="empty-text">还没有相关内容</text>
    </view>

    <view v-else class="waterfall">
      <view class="col">
        <view v-for="item in leftList" :key="item.contentId" class="wcard" hover-class="card-hover" @tap="goDetail(item)">
          <view class="cover-wrap">
            <image
              v-if="!item.coverError && (item.coverUrl || (item.images && item.images.length))"
              class="cover"
              :src="item.coverUrl || item.images[0]"
              mode="aspectFill"
              @error="item.coverError = true"
            />
            <view v-else class="cover cover-empty"><text class="cover-empty-text">视频</text></view>
            <view v-if="item.videoUrl" class="video-badge"><text class="video-badge-text">视频</text></view>
            <view v-else-if="item.images && item.images.length > 1" class="img-badge"><text class="img-badge-text">⧉ {{ item.images.length }}</text></view>
          </view>
          <view class="wbody">
            <text class="wtitle">{{ item.title }}</text>
            <view v-if="item.poiName" class="wpoi">
              <image class="wpoi-ic" src="/static/icons/location.png" mode="aspectFit" />
              <text class="wpoi-tx">{{ item.poiName }}</text>
            </view>
            <view v-if="item.tags && item.tags.length" class="wtag">
              <text class="wtag-tx"># {{ item.tags.join(' # ') }}</text>
            </view>
            <view class="wfoot">
              <view class="wauthor">
                <view class="avatar-ph wavatar"><text class="wavatar-text">{{ shortName(item.author) }}</text></view>
                <text class="wnick">{{ item.author ? item.author.nickname : '匿名' }}</text>
              </view>
              <view class="wlike">
                <image class="wlike-img" :src="item.liked ? '/static/icons/heart-on.png' : '/static/icons/heart.png'" />
                <text class="wlike-num">{{ item.likeCount }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
      <view class="col">
        <view v-for="item in rightList" :key="item.contentId" class="wcard" hover-class="card-hover" @tap="goDetail(item)">
          <view class="cover-wrap">
            <image
              v-if="!item.coverError && (item.coverUrl || (item.images && item.images.length))"
              class="cover"
              :src="item.coverUrl || item.images[0]"
              mode="aspectFill"
              @error="item.coverError = true"
            />
            <view v-else class="cover cover-empty"><text class="cover-empty-text">视频</text></view>
            <view v-if="item.videoUrl" class="video-badge"><text class="video-badge-text">视频</text></view>
            <view v-else-if="item.images && item.images.length > 1" class="img-badge"><text class="img-badge-text">⧉ {{ item.images.length }}</text></view>
          </view>
          <view class="wbody">
            <text class="wtitle">{{ item.title }}</text>
            <view v-if="item.poiName" class="wpoi">
              <image class="wpoi-ic" src="/static/icons/location.png" mode="aspectFit" />
              <text class="wpoi-tx">{{ item.poiName }}</text>
            </view>
            <view v-if="item.tags && item.tags.length" class="wtag">
              <text class="wtag-tx"># {{ item.tags.join(' # ') }}</text>
            </view>
            <view class="wfoot">
              <view class="wauthor">
                <view class="avatar-ph wavatar"><text class="wavatar-text">{{ shortName(item.author) }}</text></view>
                <text class="wnick">{{ item.author ? item.author.nickname : '匿名' }}</text>
              </view>
              <view class="wlike">
                <image class="wlike-img" :src="item.liked ? '/static/icons/heart-on.png' : '/static/icons/heart.png'" />
                <text class="wlike-num">{{ item.likeCount }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="list.length && !finished" class="loading-more"><text>加载中…</text></view>
    <view v-if="list.length && finished && page > 1" class="loading-more"><text>没有更多了</text></view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { getUser } from '@/utils/auth'

export default {
  data() {
    return {
      mode: 'topic', // topic | shop
      q: '',
      list: [],
      leftList: [],
      rightList: [],
      colIndex: 0,
      page: 1,
      pageSize: 10,
      total: 0,
      loading: false,
      finished: false,
      isFollowingTopic: false
    }
  },
  onLoad(query) {
    this.mode = query.mode === 'shop' ? 'shop' : 'topic'
    this.q = decodeURIComponent(query.q || '')
    uni.setNavigationBarTitle({ title: this.mode === 'topic' ? '话题' : '店铺' })
    if (this.mode === 'topic' && getUser()) this.loadFollowState()
    this.load(true)
  },
  onReachBottom() {
    if (!this.finished && !this.loading) {
      this.page += 1
      this.load(false)
    }
  },
  methods: {
    async load(reset) {
      if (reset) {
        this.page = 1
        this.list = []
        this.leftList = []
        this.rightList = []
        this.colIndex = 0
        this.finished = false
      }
      this.loading = true
      try {
        const data = await request({
          url: `/content/search?keyword=${encodeURIComponent(this.q)}&page=${this.page}&pageSize=${this.pageSize}`,
          silent: true
        })
        const items = (data.list || []).map(it => ({ ...it, coverError: false }))
        items.forEach(it => {
          if (this.colIndex % 2 === 0) this.leftList.push(it)
          else this.rightList.push(it)
          this.colIndex += 1
        })
        this.list = this.list.concat(items)
        this.total = data.total || 0
        this.finished = this.list.length >= this.total
      } catch (e) {
        // 搜索失败静默
      } finally {
        this.loading = false
      }
    },
    goDetail(item) {
      uni.navigateTo({ url: '/pages/detail/detail?id=' + item.contentId })
    },
    /** 话题模式：是否关注了当前话题（后端 followed_topics） */
    async loadFollowState() {
      try {
        const list = await request({ url: '/user/followed-topics', silent: true }) || []
        this.isFollowingTopic = list.indexOf(this.q) >= 0
      } catch (e) {
        this.isFollowingTopic = false
      }
    },
    async toggleFollowTopic() {
      if (!getUser()) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      try {
        if (this.isFollowingTopic) {
          await request({ url: `/user/follow-topic/${encodeURIComponent(this.q)}`, method: 'DELETE' })
          this.isFollowingTopic = false
          uni.showToast({ title: '已取消关注', icon: 'none' })
        } else {
          await request({ url: '/user/follow-topic', method: 'POST', data: { topic: this.q } })
          this.isFollowingTopic = true
          uni.showToast({ title: '已关注', icon: 'none' })
        }
      } catch (e) { /* toast 已提示 */ }
    },
    shortName(author) {
      if (!author || !author.nickname) return '匿'
      return author.nickname.slice(0, 1)
    }
  }
}
</script>

<style scoped>
.hub {
  min-height: 100vh;
  background: var(--dp-bg);
}
.hub-head {
  padding: 34rpx 28rpx 30rpx;
  background: linear-gradient(160deg, #2c3e50, #4a6274 75%, #5e8c61 130%);
  position: relative;
  overflow: hidden;
}
/* 巨号 # 水印锚点（低成本高记忆，原型09） */
.hub-head::after {
  content: '#';
  position: absolute;
  right: 4rpx;
  bottom: -70rpx;
  font-family: Georgia, serif;
  font-style: italic;
  font-size: 240rpx;
  color: rgba(255, 255, 255, 0.12);
  line-height: 1;
}
.hub-title {
  font-size: 44rpx;
  font-weight: 700;
  color: #ffffff;
  position: relative;
  z-index: 1;
  word-break: break-all;
}
.hub-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.82);
  position: relative;
  z-index: 1;
}
.follow-btn {
  position: absolute;
  top: 34rpx;
  right: 28rpx;
  z-index: 2;
  padding: 10rpx 28rpx;
  border-radius: 999rpx;
  background: #ff2442;
  border: 1rpx solid #ff2442;
}
.follow-btn.on {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.6);
}
.follow-btn-text {
  font-size: 24rpx;
  color: #ffffff;
}
.waterfall {
  display: flex;
  padding: 16rpx;
  gap: 16rpx;
}
.col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  min-width: 0;
}
.wcard {
  background: var(--dp-card);
  border-radius: 16rpx;
  overflow: hidden;
}
.card-hover {
  opacity: 0.92;
}
.cover-wrap {
  height: 320rpx;
  background: #f2f3f5;
  position: relative;
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
}
.cover-empty-text {
  color: #bbb;
  font-size: 24rpx;
}
.video-badge {
  position: absolute;
  right: 12rpx;
  bottom: 12rpx;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 8rpx;
}
.img-badge {
  position: absolute;
  right: 12rpx;
  top: 12rpx;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 8rpx;
}
.wbody {
  padding: 14rpx 16rpx 16rpx;
}
.wtitle {
  font-size: 28rpx;
  color: #222;
  line-height: 38rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.wpoi {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
  min-width: 0;
}
.wpoi-ic {
  width: 22rpx;
  height: 22rpx;
  flex-shrink: 0;
}
.wpoi-tx {
  font-size: 21rpx;
  color: var(--dp-brand-deep);
  margin-left: 6rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.wtag {
  margin-top: 6rpx;
  min-width: 0;
}
.wtag-tx {
  font-size: 20rpx;
  color: var(--dp-text4);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}
.wfoot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
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
  background: #ffd7df;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.wavatar-text {
  font-size: 20rpx;
  color: var(--dp-brand-deep);
}
.wnick {
  font-size: 22rpx;
  color: var(--dp-text3);
  margin-left: 10rpx;
  max-width: 160rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.wlike {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.wlike-img {
  width: 28rpx;
  height: 28rpx;
}
.wlike-num {
  font-size: 22rpx;
  color: var(--dp-text3);
  margin-left: 6rpx;
}
.empty {
  text-align: center;
  padding: 160rpx 0;
}
.empty-text {
  color: #bbb;
  font-size: 28rpx;
}
.loading-more {
  text-align: center;
  color: #bbb;
  padding: 24rpx;
  font-size: 24rpx;
}
.skeleton {
  background: linear-gradient(90deg, #eee 25%, #f5f5f5 37%, #eee 63%);
  background-size: 400% 100%;
  animation: sk 1.3s ease infinite;
}
.sk-img {
  height: 320rpx;
}
.sk-img.short {
  height: 240rpx;
}
.sk-line {
  height: 28rpx;
  margin: 16rpx;
  border-radius: 6rpx;
}
@keyframes sk {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
</style>
