<template>
  <view class="page">
    <!-- 搜索框 + 取消 -->
    <view class="search-row">
      <view class="search-box" :class="{ active: keyword }">
        <view class="s-ring"></view>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索店铺 / 内容"
          placeholder-class="ph"
          confirm-type="search"
          focus
          @confirm="doSearch"
        />
        <text v-if="keyword" class="clear" @tap="clearKw">×</text>
      </view>
      <text class="cancel" @tap="goBack">取消</text>
    </view>

    <!-- 未搜索：猜你想搜 -->
    <view v-if="!searched">
      <text class="sec-title">猜你想搜</text>
      <view class="hot-wrap">
        <view v-for="(w, i) in hotWords" :key="i" class="hot-item" @tap="tapHot(w)">
          <text class="hot-text">{{ w }}</text>
        </view>
      </view>
    </view>

    <!-- 搜索结果：双列瀑布流 -->
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
              <view v-else class="cover cover-empty">
                <text class="cover-empty-text">视频</text>
              </view>
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
              <view v-else class="cover cover-empty">
                <text class="cover-empty-text">视频</text>
              </view>
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
      <view v-if="list.length && !hasMore" class="empty">
        <text class="muted">— 到底啦 —</text>
      </view>
    </view>
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
      hotWords: ['唐山烧烤', '人均30', '咖啡店', '周末遛娃', '拍照圣地', '老店'],
      list: [],
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
  onReachBottom() {
    if (this.searched && this.hasMore && !this.loading) {
      this.page += 1
      this.fetch(true)
    }
  },
  methods: {
    tapHot(w) {
      this.keyword = w
      this.doSearch()
    },
    clearKw() {
      this.keyword = ''
      this.searched = false
      this.list = []
    },
    goBack() {
      uni.navigateBack()
    },
    doSearch() {
      const kw = this.keyword.trim()
      if (!kw) return
      this.lastKw = kw
      this.searched = true
      this.page = 1
      this.hasMore = true
      this.list = []
      this.fetch()
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
}
.hot-text {
  font-size: 26rpx;
  color: #1f2430;
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
.empty {
  text-align: center;
  padding: 100rpx 0;
}
</style>
