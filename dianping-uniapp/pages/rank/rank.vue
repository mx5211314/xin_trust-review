<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <view class="hero">
      <text class="hero-title">口碑榜</text>
      <text class="hero-sub">已上架点评按店铺聚合 · 按总赞数排序</text>
    </view>

    <view class="list" v-if="list.length">
      <view
        class="row"
        v-for="(r, i) in list"
        :key="r.shop"
        hover-class="row-hover"
        @tap="goShop(r.shop)"
      >
        <view class="no" :class="'no' + ((i < 3) ? (i + 1) : 0)">
          <text class="no-text">{{ i + 1 }}</text>
        </view>
        <view class="mid">
          <text class="shop">{{ r.shop }}</text>
          <text class="meta">{{ r.contentCount }} 篇点评 · {{ r.likeCount }} 赞</text>
        </view>
        <view class="bar">
          <view class="bar-fill" :style="{ width: pct(r.likeCount) + '%' }"></view>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 引导式空状态（原则 09：说清楚 + 给下一步） -->
    <view v-if="!loading && !list.length" class="empty">
      <view class="empty-ic">
        <image class="empty-ic-img" src="/static/icons/shop.png" mode="aspectFit" />
      </view>
      <text class="empty-t">榜单还没生成</text>
      <text class="empty-d">带店铺的点评积累到一定数量后，榜单会自动出现</text>
      <view class="empty-btn" @tap="goWrite">去写点评</view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'

export default {
  data() {
    return {
      list: [],
      loading: true,
      maxLike: 0
    }
  },
  onLoad() {
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        this.list = await request({ url: '/content/rank-shops?limit=50' }) || []
        this.maxLike = this.list.reduce((m, r) => Math.max(m, r.likeCount || 0), 0)
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    pct(n) {
      if (!this.maxLike) return 0
      return Math.max(6, Math.round((n / this.maxLike) * 100))
    },
    goShop(shop) {
      uni.navigateTo({
        url: '/pages/collection/collection?mode=shop&q=' + encodeURIComponent(shop)
      })
    },
    /** 空状态引导：去发布页写点评（发布是 tabBar 页，须用 switchTab） */
    goWrite() {
      uni.switchTab({ url: '/pages/publish/publish' })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--dp-bg);
}
.hero {
  background: linear-gradient(135deg, #ff5468, var(--dp-brand) 55%, #e01836);
  padding: 60rpx 40rpx 48rpx;
}
.hero-title {
  display: block;
  color: #fff;
  font-size: 48rpx;
  font-weight: 700;
  font-family: Georgia, "Songti SC", serif;
}
.hero-sub {
  display: block;
  color: rgba(255, 255, 255, 0.9);
  font-size: 22rpx;
  margin-top: 10rpx;
}
.list {
  padding: 16rpx 24rpx 40rpx;
}
.row {
  display: flex;
  align-items: center;
  background: var(--dp-card);
  border-radius: 16rpx;
  padding: 26rpx 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.04);
}
.row-hover {
  transform: scale(0.98);
  opacity: 0.9;
}
.no {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: var(--dp-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 20rpx;
}
.no-text {
  font-family: Georgia, serif;
  font-style: italic;
  font-weight: 700;
  font-size: 30rpx;
  color: var(--dp-text3);
}
.no1 {
  background: linear-gradient(135deg, var(--dp-brand), var(--dp-brand-deep));
}
.no1 .no-text { color: #fff; }
.no2 {
  background: linear-gradient(135deg, var(--dp-orange), #ff7a45);
}
.no2 .no-text { color: #fff; }
.no3 {
  background: linear-gradient(135deg, #ffd28a, #ffb03a);
}
.no3 .no-text { color: #fff; }
.mid {
  flex: 1;
  min-width: 0;
}
.shop {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: var(--dp-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.meta {
  display: block;
  font-size: 22rpx;
  color: var(--dp-text3);
  margin-top: 6rpx;
}
.bar {
  width: 120rpx;
  height: 10rpx;
  border-radius: 6rpx;
  background: var(--dp-soft);
  overflow: hidden;
  margin: 0 18rpx;
  flex-shrink: 0;
}
.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--dp-brand), var(--dp-brand-deep));
  border-radius: 6rpx;
}
.arrow {
  color: var(--dp-text4);
  font-size: 36rpx;
  flex-shrink: 0;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx var(--sp-5) 80rpx;
}
.empty-ic {
  width: 128rpx;
  height: 128rpx;
  border-radius: var(--r-pill);
  background: var(--dp-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--sp-4);
}
.empty-ic-img {
  width: 56rpx;
  height: 56rpx;
  opacity: .45;
}
.empty-t {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--dp-text);
}
.empty-d {
  font-size: var(--fs-sm);
  color: var(--dp-text3);
  line-height: 1.7;
  text-align: center;
  margin-top: var(--sp-2);
  max-width: 440rpx;
}
.empty-btn {
  margin-top: var(--sp-5);
  padding: 18rpx 56rpx;
  border-radius: var(--r-pill);
  background: var(--dp-brand-deep);
  color: #ffffff;
  font-size: var(--fs-base);
  font-weight: 600;
  box-shadow: 0 8rpx 24rpx -6rpx rgba(216, 18, 40, .45);
}
.muted {
  font-size: 24rpx;
  color: var(--dp-text4);
  line-height: 1.6;
}
</style>
