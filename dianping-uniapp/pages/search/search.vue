<template>
  <view class="page">
    <!-- 搜索框 + 取消 -->
    <view class="search-row">
      <view class="search-box" :class="{ active: keyword }">
        <view class="s-ring"></view>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索内容 / 用户 / 话题 / 店铺"
          placeholder-class="ph"
          confirm-type="search"
          focus
          @confirm="doSearch"
        />
        <text v-if="keyword" class="clear" @tap="clearKw">×</text>
      </view>
      <text class="cancel" @tap="goBack">取消</text>
    </view>

    <!-- Tab 切换 -->
    <view class="tabs">
      <text
        v-for="t in tabs"
        :key="t.key"
        class="tab"
        :class="{ on: tab === t.key }"
        @tap="switchTab(t.key)"
      >{{ t.label }}</text>
    </view>

    <!-- 内容 -->
    <block v-if="tab === 'content'">
      <view v-if="!searched">
        <view v-if="history.length" class="sec-head">
          <text class="sec-title">历史记录</text>
          <text class="sec-clear" @tap="clearHistory">清空</text>
        </view>
        <view v-if="history.length" class="hot-wrap">
          <view v-for="(w, i) in history" :key="'h' + i" class="hot-item" @tap="tapHot(w)">
            <text class="hot-text">{{ w }}</text>
          </view>
        </view>
        <text class="sec-title">猜你想搜</text>
        <view class="hot-wrap">
          <view v-for="(w, i) in hotWords" :key="i" class="hot-item" @tap="tapHot(w)">
            <text class="hot-text">{{ w }}</text>
          </view>
        </view>
      </view>
      <view v-else>
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
                <view v-else class="cover cover-empty"><text class="cover-empty-text">视频</text></view>
              </view>
              <text class="wtitle">{{ item.title }}</text>
              <view class="wfoot">
                <text class="wauthor">{{ item.author ? item.author.nickname : '匿名' }}</text>
                <text class="wlike">♥ {{ item.likeCount }}</text>
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
                <view v-else class="cover cover-empty"><text class="cover-empty-text">视频</text></view>
              </view>
              <text class="wtitle">{{ item.title }}</text>
              <view class="wfoot">
                <text class="wauthor">{{ item.author ? item.author.nickname : '匿名' }}</text>
                <text class="wlike">♥ {{ item.likeCount }}</text>
              </view>
            </view>
          </view>
        </view>
        <view v-if="!loading && !list.length" class="empty">
          <text class="muted">没找到"{{ lastKw }}"相关的内容</text>
        </view>
        <view v-if="list.length && !hasMore" class="empty"><text class="muted">— 到底啦 —</text></view>
      </view>
    </block>

    <!-- 用户 -->
    <block v-else-if="tab === 'user'">
      <view v-if="!searched" class="empty"><text class="muted">输入昵称，搜索用户</text></view>
      <view v-else>
        <view v-if="users.length" class="user-list">
          <view v-for="u in users" :key="u.userId" class="user-row" @tap="goUser(u)">
            <image class="u-avatar" :src="u.avatar" />
            <text class="u-name">{{ u.nickname }}</text>
            <text class="u-go">查看 ›</text>
          </view>
        </view>
        <view v-if="!loading && !users.length" class="empty">
          <text class="muted">没找到"{{ lastKw }}"相关的用户</text>
        </view>
      </view>
    </block>

    <!-- 话题 -->
    <block v-else-if="tab === 'topic'">
      <view class="cloud-sec">
        <text class="cloud-title">热门话题</text>
        <view class="hot-wrap">
          <view v-for="(t, i) in hotTags" :key="i" class="hot-item tag" @tap="tapTag(t.tag)">
            <text class="hot-text"># {{ t.tag }}</text>
            <text class="hot-sub">{{ t.count }} 篇</text>
          </view>
        </view>
      </view>
    </block>

    <!-- 店铺 -->
    <block v-else-if="tab === 'shop'">
      <view class="cloud-sec">
        <text class="cloud-title">热门店铺</text>
        <view class="hot-wrap">
          <view v-for="(s, i) in hotShops" :key="i" class="hot-item shop" @tap="tapShop(s.shop)">
            <text class="hot-text">{{ s.shop }}</text>
            <text class="hot-sub">{{ s.count }} 篇</text>
          </view>
        </view>
      </view>
    </block>
  </view>
</template>

<script>
import { request } from '@/utils/request'

export default {
  data() {
    return {
      keyword: '',
      lastKw: '',
      searched: false,
      tab: 'content',
      tabs: [
        { key: 'content', label: '内容' },
        { key: 'user', label: '用户' },
        { key: 'topic', label: '话题' },
        { key: 'shop', label: '店铺' }
      ],
      hotWords: ['唐山烧烤', '人均30', '咖啡店', '周末遛娃', '拍照圣地', '老店'],
      history: [],
      list: [],
      users: [],
      hotTags: [],
      hotShops: [],
      page: 1,
      pageSize: 10,
      total: 0,
      hasMore: true,
      loading: false
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
  onLoad(query) {
    try {
      this.history = uni.getStorageSync('dp_search_history') || []
    } catch (e) { /* ignore */ }
    this.loadHotTags()
    this.loadHotShops()
    if (query && query.keyword) {
      this.keyword = decodeURIComponent(query.keyword)
      this.$nextTick(() => this.doSearch())
    }
  },
  onReachBottom() {
    if (this.tab === 'content' && this.searched && this.hasMore && !this.loading) {
      this.page += 1
      this.fetch(true)
    }
  },
  methods: {
    tapHot(w) {
      this.keyword = w
      this.doSearch()
    },
    switchTab(t) {
      this.tab = t
      if (t === 'topic' && !this.hotTags.length) this.loadHotTags()
      if (t === 'shop' && !this.hotShops.length) this.loadHotShops()
    },
    clearKw() {
      this.keyword = ''
      this.searched = false
      this.list = []
      this.users = []
    },
    goBack() {
      uni.navigateBack()
    },
    saveHistory(kw) {
      const list = this.history.filter(x => x !== kw)
      list.unshift(kw)
      this.history = list.slice(0, 10)
      try {
        uni.setStorageSync('dp_search_history', this.history)
      } catch (e) { /* ignore */ }
    },
    clearHistory() {
      this.history = []
      try {
        uni.removeStorageSync('dp_search_history')
      } catch (e) { /* ignore */ }
    },
    doSearch() {
      const kw = this.keyword.trim()
      if (!kw) return
      this.saveHistory(kw)
      this.lastKw = kw
      if (this.tab === 'content') {
        this.searched = true
        this.page = 1
        this.hasMore = true
        this.list = []
        this.fetch()
      } else if (this.tab === 'user') {
        this.searched = true
        this.users = []
        this.fetchUsers()
      }
      // 话题/店铺为发现型词云，不按关键词检索
    },
    async fetch(append) {
      this.loading = true
      try {
        const data = await request({
          url: `/content/search?page=${this.page}&pageSize=${this.pageSize}` +
            `&keyword=${encodeURIComponent(this.lastKw)}`
        })
        this.total = data.total
        this.list = append ? this.list.concat(data.list) : data.list
        this.hasMore = this.list.length < data.total
      } catch (e) {
        if (!append) this.list = []
      } finally {
        this.loading = false
      }
    },
    async fetchUsers() {
      this.loading = true
      try {
        this.users = await request({
          url: `/user/search?keyword=${encodeURIComponent(this.lastKw)}&limit=20`
        })
      } catch (e) {
        this.users = []
      } finally {
        this.loading = false
      }
    },
    async loadHotTags() {
      try {
        this.hotTags = await request({ url: '/content/hot-tags?limit=20', silent: true })
      } catch (e) { /* ignore */ }
    },
    async loadHotShops() {
      try {
        this.hotShops = await request({ url: '/content/hot-shops?limit=20', silent: true })
      } catch (e) { /* ignore */ }
    },
    goDetail(item) {
      uni.navigateTo({ url: '/pages/detail/detail?id=' + item.contentId })
    },
    goUser(u) {
      uni.navigateTo({ url: '/pages/user/user?userId=' + u.userId })
    },
    tapTag(tag) {
      uni.navigateTo({ url: '/pages/collection/collection?mode=topic&q=' + encodeURIComponent(tag) })
    },
    tapShop(shop) {
      uni.navigateTo({ url: '/pages/collection/collection?mode=shop&q=' + encodeURIComponent(shop) })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #ffffff;
}
.search-row {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
}
.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f6f7f9;
  border-radius: 36rpx;
  padding: 16rpx 24rpx;
}
.search-box.active {
  border: 1.5rpx solid #ff2442;
  background: #ffffff;
}
.s-ring {
  width: 24rpx;
  height: 24rpx;
  border: 3rpx solid #999999;
  border-radius: 50%;
  margin-right: 14rpx;
  flex-shrink: 0;
}
.search-input {
  flex: 1;
  font-size: 28rpx;
}
.ph {
  color: #b9c0c9;
}
.clear {
  color: #b9c0c9;
  font-size: 36rpx;
  padding: 0 8rpx;
}
.cancel {
  margin-left: 20rpx;
  font-size: 28rpx;
  color: #ff2442;
}
.tabs {
  display: flex;
  align-items: center;
  padding: 6rpx 24rpx 14rpx;
  border-bottom: 1rpx solid #f1f3f5;
}
.tab {
  font-size: 28rpx;
  color: #8a9099;
  margin-right: 44rpx;
  padding: 8rpx 0;
  position: relative;
}
.tab.on {
  color: #1f2430;
  font-weight: 600;
}
.tab.on::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -2rpx;
  transform: translateX(-50%);
  width: 36rpx;
  height: 5rpx;
  border-radius: 3rpx;
  background: #ff2442;
}
.sec-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 24rpx 0;
}
.sec-head .sec-title {
  padding: 0;
}
.sec-clear {
  font-size: 24rpx;
  color: #999999;
}
.sec-title {
  display: block;
  padding: 30rpx 24rpx 16rpx;
  font-size: 28rpx;
  font-weight: 500;
}
.hot-wrap {
  display: flex;
  flex-wrap: wrap;
  padding: 0 24rpx;
}
.hot-item {
  background: #f6f7f9;
  border-radius: 999rpx;
  padding: 14rpx 30rpx;
  margin: 0 16rpx 16rpx 0;
  display: flex;
  align-items: center;
}
.hot-text {
  font-size: 26rpx;
  color: #1f2430;
}
.hot-sub {
  font-size: 20rpx;
  color: #b9c0c9;
  margin-left: 12rpx;
}
.waterfall {
  display: flex;
  padding: 20rpx 16rpx;
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
  font-size: 25rpx;
  font-weight: 500;
  color: #1f2430;
  line-height: 1.4;
}
.wfoot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 14rpx 14rpx;
}
.wauthor {
  font-size: 21rpx;
  color: #999999;
}
.wlike {
  font-size: 21rpx;
  color: #999999;
}
.user-list {
  padding: 8rpx 24rpx;
}
.user-row {
  display: flex;
  align-items: center;
  padding: 22rpx 8rpx;
  border-bottom: 1rpx solid #f1f3f5;
}
.u-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #ffd7df;
  flex-shrink: 0;
}
.u-name {
  flex: 1;
  margin-left: 22rpx;
  font-size: 30rpx;
  color: #1f2430;
}
.u-go {
  font-size: 24rpx;
  color: #ff2442;
}
.cloud-sec {
  padding: 16rpx 0;
}
.cloud-title {
  display: block;
  padding: 24rpx 24rpx 12rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #1f2430;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
.muted {
  color: #b9c0c9;
  font-size: 26rpx;
}
</style>
