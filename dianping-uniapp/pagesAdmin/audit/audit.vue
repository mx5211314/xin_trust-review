<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <!-- 统计头 -->
    <view class="stats">
      <view
        v-for="s in statCards"
        :key="s.label"
        class="stat"
        :class="'c' + s.i"
      >
        <text class="stat-num">{{ s.value }}</text>
        <text class="stat-label">{{ s.label }}</text>
      </view>
    </view>

    <!-- 筛选 tab -->
    <view class="tabs">
      <view
        v-for="t in tabs"
        :key="t.value"
        class="tab"
        :class="{ active: status === t.value }"
        @tap="switchTab(t.value)"
      >
        <text class="tab-text" :class="{ active: status === t.value }">{{ t.label }}</text>
      </view>
    </view>

    <!-- 待审：连续审核工作台（一次一条，处理完自动下一条） -->
    <view v-if="status === 'PENDING'" class="bench">
      <view v-if="current" class="bench-card" :class="{'theme-dark': isDark}">
        <view class="bench-cover-wrap" v-if="coverOf(current)">
          <image class="bench-cover" :src="coverOf(current)" mode="aspectFill" />
        </view>
        <view v-else class="bench-cover-wrap bench-cover-empty">
          <text class="muted">视频 / 无封面</text>
        </view>
        <view class="bench-body">
          <text class="bench-title">{{ current.title || '无标题' }}</text>
          <view class="bench-meta">
            <text v-if="current.poiName" class="meta-poi">{{ current.poiName }}</text>
            <text v-if="current.author && current.author.nickname" class="meta-author">{{ current.author.nickname }}</text>
            <text class="meta-time">{{ current.createTime }}</text>
          </view>
          <scroll-view scroll-y class="bench-text">
            <text class="bench-text-inner">{{ current.text || '（作者没有写正文）' }}</text>
          </scroll-view>
        </view>
        <view class="bench-ops">
          <button class="op op-reject" @tap="reject(current)">驳回</button>
          <button class="op op-pass" @tap="approve(current)">通过并公开</button>
        </view>
        <text class="bench-count">剩余 {{ queue.length }} 条待审</text>
      </view>

      <view v-else class="bench-done">
        <view class="done-badge"><text class="done-badge-text">完成</text></view>
        <text class="done-title">全部审完啦</text>
        <text class="muted small">待审队列已清空，新提交的会实时出现</text>
        <button class="done-btn" @tap="fetchPending">刷新一下</button>
      </view>
    </view>

    <!-- 其他状态：列表 -->
    <view v-else>
      <view v-for="item in list" :key="item.contentId" class="card item">
        <view class="item-main" @tap="goDetail(item)">
          <image
            v-if="item.coverUrl || (item.images && item.images.length)"
            class="thumb"
            :src="item.coverUrl || item.images[0]"
            mode="aspectFill"
          />
          <view v-else class="thumb thumb-empty">
            <text class="muted">视频</text>
          </view>
          <view class="item-right">
            <text class="item-title">{{ item.title }}</text>
            <text class="muted">{{ statusLabel(item.status) }} · {{ item.createTime }}</text>
            <text v-if="item.status === 'REJECTED' && item.rejectReason" class="muted reason">
              原因：{{ item.rejectReason }}
            </text>
          </view>
        </view>
        <view class="ops">
          <button
            v-if="item.status === 'APPROVED'"
            class="op op-reject"
            @tap="takedown(item)"
          >下架</button>
          <button
            v-if="item.status !== 'PENDING'"
            class="op op-plain"
            @tap="goDetail(item)"
          >查看</button>
        </view>
      </view>

      <view v-if="!loading && list.length === 0" class="empty">
        <view class="empty-ic">
          <image class="empty-ic-img" src="/static/icons/eye.png" mode="aspectFit" />
        </view>
        <text class="empty-t">该状态下暂无内容</text>
        <text class="empty-d">切换上面的状态筛选，或等作者提交新内容</text>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { isAdmin } from '@/utils/auth'

export default {
  data() {
    return {
      tabs: [
        { value: 'PENDING', label: '待审' },
        { value: 'APPROVED', label: '已上架' },
        { value: 'REJECTED', label: '已驳回' },
        { value: 'TAKEN_DOWN', label: '已下架' }
      ],
      status: 'PENDING',
      stats: { pending: 0, todayPassed: 0, todayRejected: 0, rejectRate: 0 },
      queue: [],
      list: [],
      loading: false,
      page: 1,
      hasMore: true
    }
  },
  computed: {
    statCards() {
      const s = this.stats || {}
      return [
        { i: 0, label: '待审', value: s.pending ?? 0 },
        { i: 1, label: '今日通过', value: s.todayPassed ?? 0 },
        { i: 2, label: '今日驳回', value: s.todayRejected ?? 0 },
        { i: 3, label: '驳回率', value: (s.rejectRate ?? 0) + '%' }
      ]
    },
    /** 连续审核当前条（队列头） */
    current() {
      return this.queue && this.queue.length ? this.queue[0] : null
    }
  },
  onShow() {
    if (!isAdmin()) {
      uni.showToast({ title: '仅管理员可访问', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 800)
      return
    }
    this.loadStats()
    if (this.status === 'PENDING') this.fetchPending()
    else this.fetchList()
  },
  onReachBottom() {
    if (this.status !== 'PENDING' && this.hasMore && !this.loading) {
      this.page += 1
      this.fetchList(true)
    }
  },
  methods: {
    statusLabel(s) {
      const map = { PENDING: '待审', APPROVED: '已上架', REJECTED: '已驳回', TAKEN_DOWN: '已下架' }
      return map[s] || s
    },
    coverOf(item) {
      return item && (item.coverUrl || (item.images && item.images[0]))
        ? (item.coverUrl || item.images[0])
        : ''
    },
    async loadStats() {
      try {
        this.stats = await request({ url: '/admin/stats', silent: true }) || this.stats
      } catch (e) { /* ignore */ }
    },
    /** 待审队列：一次拉一批，处理完自动下一条；清空后再拉下一批 */
    async fetchPending() {
      try {
        const data = await request({
          url: `/admin/content/list?status=PENDING&page=1&pageSize=50`
        })
        this.queue = (data.list || []).map((x) => Object.assign({}, x))
      } catch (e) {
        this.queue = []
      }
    },
    async fetchList(append) {
      this.loading = true
      try {
        const data = await request({
          url: `/admin/content/list?status=${this.status}&page=${this.page}&pageSize=20`
        })
        const items = data.list || []
        this.list = append ? this.list.concat(items) : items
        this.hasMore = this.list.length < (data.total || 0)
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    switchTab(v) {
      if (this.status === v) return
      this.status = v
      this.page = 1
      this.hasMore = true
      this.list = []
      if (v === 'PENDING') this.fetchPending()
      else this.fetchList()
    },
    /** 处理完一条后：出队 + 刷新统计；队列空了自动拉下一批 */
    afterAction() {
      if (this.queue.length) this.queue.shift()
      this.stats.pending = Math.max(0, (this.stats.pending || 0) - 1)
      if (!this.queue.length) this.fetchPending()
      else this.loadStats()
    },
    approve(item) {
      uni.showModal({
        title: '通过该内容？',
        content: '通过后将公开展示',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({ url: `/admin/content/${item.contentId}/approve`, method: 'POST' })
            uni.showToast({ title: '已通过', icon: 'success' })
            this.afterAction()
          } catch (e) { /* toast 已提示 */ }
        }
      })
    },
    reject(item) {
      uni.showModal({
        title: '驳回该内容',
        editable: true,
        placeholderText: '请输入驳回原因',
        success: async (res) => {
          if (!res.confirm) return
          const reason = (res.content || '').trim()
          if (!reason) return uni.showToast({ title: '驳回原因不能为空', icon: 'none' })
          try {
            await request({
              url: `/admin/content/${item.contentId}/reject`,
              method: 'POST',
              data: { reason }
            })
            uni.showToast({ title: '已驳回', icon: 'success' })
            this.afterAction()
          } catch (e) { /* toast 已提示 */ }
        }
      })
    },
    takedown(item) {
      uni.showModal({
        title: '下架该内容？',
        content: '下架后将从信息流移除',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({ url: `/admin/content/${item.contentId}/takedown`, method: 'POST' })
            uni.showToast({ title: '已下架', icon: 'success' })
            this.fetchList()
          } catch (e) { /* toast 已提示 */ }
        }
      })
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
/* 统计头 */
.stats {
  display: flex;
  padding: 24rpx 16rpx 8rpx;
  gap: 14rpx;
}
.stat {
  flex: 1;
  background: var(--dp-card);
  border-radius: 16rpx;
  padding: 22rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}
.stat-num {
  font-family: Georgia, serif;
  font-style: italic;
  font-size: 40rpx;
  font-weight: 700;
  color: var(--dp-text);
}
.stat-label {
  font-size: 20rpx;
  color: var(--dp-text3);
  margin-top: 4rpx;
}
.stat.c0 .stat-num { color: var(--dp-brand-deep); }
.stat.c1 .stat-num { color: #0a9d6e; }
.stat.c2 .stat-num { color: #e08a00; }
.stat.c3 .stat-num { color: #3a7afe; }

/* tabs */
.tabs {
  display: flex;
  padding: 16rpx 16rpx 8rpx;
}
.tab {
  padding: 12rpx 36rpx;
  border-radius: 999rpx;
  background: var(--dp-soft);
  margin-right: 16rpx;
}
.tab.active {
  background: var(--dp-brand-deep);
}
.tab-text {
  font-size: 26rpx;
  color: var(--dp-text3);
}
.tab-text.active {
  color: #ffffff;
}

/* 连续审核工作台 */
.bench {
  padding: 16rpx;
}
.bench-card {
  background: var(--dp-card);
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
}
.bench-cover-wrap {
  width: 100%;
  height: 360rpx;
  background: var(--dp-soft);
}
.bench-cover {
  width: 100%;
  height: 100%;
  display: block;
}
.bench-cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}
.bench-body {
  padding: 24rpx 28rpx 8rpx;
}
.bench-title {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: var(--dp-text);
  line-height: 1.4;
}
.bench-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 12rpx;
}
.meta-poi {
  font-size: 24rpx;
  color: var(--dp-brand-deep);
  margin-right: 16rpx;
}
.meta-author {
  font-size: 24rpx;
  color: var(--dp-text2);
  margin-right: 16rpx;
}
.meta-time {
  font-size: 22rpx;
  color: var(--dp-text4);
}
.bench-text {
  max-height: 320rpx;
  margin-top: 18rpx;
  background: var(--dp-soft);
  border-radius: 14rpx;
  padding: 20rpx;
}
.bench-text-inner {
  font-size: 28rpx;
  line-height: 1.7;
  color: var(--dp-text);
  white-space: pre-wrap;
}
.bench-ops {
  display: flex;
  padding: 20rpx 28rpx 8rpx;
  gap: 20rpx;
}
.op {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  font-size: 28rpx;
  border-radius: 42rpx;
  padding: 0;
}
.op-pass {
  background: var(--dp-brand-deep);
  color: #ffffff;
}
.op-reject {
  background: var(--dp-soft);
  color: var(--dp-text2);
}
.op-plain {
  background: var(--dp-soft);
  color: var(--dp-text2);
  margin: 0;
}
.bench-count {
  display: block;
  text-align: center;
  font-size: 22rpx;
  color: var(--dp-text4);
  padding: 8rpx 0 24rpx;
}

/* 完成态 */
.bench-done {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 40rpx;
}
.done-badge {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: rgba(10, 157, 110, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}
.done-badge-text {
  font-size: 30rpx;
  font-weight: 600;
  color: #0a9d6e;
}
.done-title {
  font-size: 32rpx;
  font-weight: 600;
  color: var(--dp-text);
}
.done-btn {
  margin-top: 32rpx;
  height: 76rpx;
  line-height: 76rpx;
  font-size: 26rpx;
  border-radius: 38rpx;
  background: var(--dp-brand-deep);
  color: #ffffff;
  padding: 0 60rpx;
}

/* 列表（非待审） */
.card {
  background: var(--dp-card);
  border-radius: 16rpx;
  margin: 16rpx;
  padding: 20rpx;
}
.item-main {
  display: flex;
}
.thumb {
  width: 120rpx;
  height: 120rpx;
  border-radius: 12rpx;
  background: var(--dp-soft);
  margin-right: 20rpx;
  flex-shrink: 0;
}
.thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}
.item-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 120rpx;
}
.item-title {
  font-size: 28rpx;
  font-weight: 500;
  line-height: 1.4;
  color: var(--dp-text);
}
.reason {
  color: var(--dp-danger);
  font-size: 22rpx;
}
.ops {
  display: flex;
  justify-content: flex-end;
  margin-top: 16rpx;
}
.ops .op {
  margin: 0 0 0 16rpx;
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 36rpx;
}
/* 引导式空状态容器（原则 09） */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx var(--sp-5) 60rpx;
}
.muted {
  font-size: 24rpx;
  color: var(--dp-text3);
}
.small {
  font-size: 22rpx;
  color: var(--dp-text4);
  margin-top: 10rpx;
}
</style>
