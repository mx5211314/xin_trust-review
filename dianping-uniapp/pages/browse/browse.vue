<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <view class="head">
      <text class="title">浏览记录</text>
      <text class="sub">你最近看过的点评，按时间倒序</text>
    </view>

    <view v-if="groups.length" class="timeline">
      <view v-for="g in groups" :key="g.label" class="group">
        <view class="group-head">
          <text class="group-label">{{ g.label }}</text>
        </view>
        <view
          v-for="item in g.items"
          :key="item.contentId"
          class="card"
          hover-class="card-hover"
          @tap="goDetail(item)"
        >
          <view class="cover-wrap">
            <image
              v-if="!item.coverError && (item.coverUrl || (item.images && item.images.length))"
              class="cover"
              :src="item.coverUrl || item.images[0]"
              mode="aspectFill"
              @error="item.coverError = true"
            />
            <view v-else class="cover cover-empty">
              <text class="cover-empty-text">视频</text>
            </view>
          </view>
          <view class="body">
            <text class="title-text">{{ item.title }}</text>
            <view class="meta">
              <text v-if="item.poiName" class="poi">{{ item.poiName }}</text>
              <text class="time">{{ timeOf(item.viewTime) }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="!loading && !groups.length" class="empty">
      <text class="muted">还没有浏览记录</text>
      <text class="muted small">去首页逛逛，看过的点评会记在这里</text>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { getUser } from '@/utils/auth'

export default {
  data() {
    return {
      list: [],
      total: 0,
      page: 1,
      pageSize: 20,
      hasMore: true,
      loading: false
    }
  },
  computed: {
    /** 按日期分组（今天 / 昨天 / MM月DD日），保持后端倒序 */
    groups() {
      if (!this.list.length) return []
      const map = new Map()
      const order = []
      for (const it of this.list) {
        const date = (it.viewTime || '').slice(0, 10)
        const label = this.dateLabel(date)
        if (!map.has(label)) {
          map.set(label, [])
          order.push(label)
        }
        map.get(label).push(it)
      }
      return order.map((label) => ({ label, items: map.get(label) }))
    }
  },
  onShow() {
    if (!getUser()) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.page = 1
    this.fetch()
  },
  onPullDownRefresh() {
    this.page = 1
    this.fetch().then(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    if (this.hasMore && !this.loading) {
      this.page += 1
      this.fetch(true)
    }
  },
  methods: {
    async fetch(append) {
      this.loading = true
      try {
        const d = await request({
          url: `/user/browse?page=${this.page}&pageSize=${this.pageSize}`
        })
        const items = (d.list || []).map((x) => Object.assign({}, x, { coverError: false }))
        this.list = append ? this.list.concat(items) : items
        this.total = Number(d.total || 0)
        this.hasMore = this.list.length < this.total
      } catch (e) { /* toast 已提示 */ }
      finally {
        this.loading = false
      }
    },
    /** 日期分组标签 */
    dateLabel(date) {
      if (!date) return '更早'
      const now = new Date()
      const today = this.fmtDate(now)
      const y = new Date(now.getTime() - 86400000)
      const yest = this.fmtDate(y)
      if (date === today) return '今天'
      if (date === yest) return '昨天'
      const [yy, mm, dd] = date.split('-')
      return `${Number(mm)}月${Number(dd)}日`
    },
    fmtDate(d) {
      const p = (n) => (n < 10 ? '0' + n : '' + n)
      return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
    },
    /** 仅取时分部分展示 */
    timeOf(vt) {
      if (!vt) return ''
      return vt.slice(11, 16)
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
  background: var(--dp-bg);
  padding-bottom: 40rpx;
}
.head {
  background: linear-gradient(165deg, var(--dp-accent-soft), var(--dp-accent-soft) 55%, #ffffff);
  padding: 40rpx 32rpx 36rpx;
}
.title {
  display: block;
  font-size: 40rpx;
  font-weight: 600;
  color: var(--dp-text);
}
.sub {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--dp-text3);
}
.timeline {
  padding: 8rpx 24rpx;
}
.group {
  margin-top: 12rpx;
}
.group-head {
  padding: 16rpx 4rpx 8rpx;
}
.group-label {
  font-size: 24rpx;
  font-weight: 600;
  color: var(--dp-text3);
}
.card {
  display: flex;
  background: var(--dp-card);
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.04);
  transition: transform 0.15s ease, opacity 0.15s ease;
}
.card-hover {
  transform: scale(0.98);
  opacity: 0.9;
}
.cover-wrap {
  width: 180rpx;
  height: 180rpx;
  flex-shrink: 0;
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
  background: var(--dp-soft);
}
.cover-empty-text {
  font-size: 22rpx;
  color: var(--dp-text4);
}
.body {
  flex: 1;
  min-width: 0;
  padding: 20rpx 22rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.title-text {
  font-size: 28rpx;
  font-weight: 500;
  color: var(--dp-text);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
}
.poi {
  font-size: 22rpx;
  color: var(--dp-brand-deep);
  max-width: 380rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.time {
  font-size: 22rpx;
  color: var(--dp-text4);
  flex-shrink: 0;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}
.muted {
  font-size: 26rpx;
  color: var(--dp-text3);
}
.small {
  font-size: 22rpx;
  color: var(--dp-text4);
  margin-top: 10rpx;
}
</style>
