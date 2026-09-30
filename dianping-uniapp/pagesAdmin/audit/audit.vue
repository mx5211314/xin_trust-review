<template>
  <view class="page">
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
          v-if="item.status === 'PENDING'"
          class="op op-pass"
          @tap="approve(item)"
        >通过</button>
        <button
          v-if="item.status === 'PENDING'"
          class="op op-reject"
          @tap="reject(item)"
        >驳回</button>
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
      <text class="muted">该状态下暂无内容</text>
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
        { value: 'REJECTED', label: '已驳回' }
      ],
      status: 'PENDING',
      list: [],
      loading: false
    }
  },
  onShow() {
    if (!isAdmin()) {
      uni.showToast({ title: '仅管理员可访问', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 800)
      return
    }
    this.fetch()
  },
  methods: {
    statusLabel(s) {
      const map = { PENDING: '待审', APPROVED: '已上架', REJECTED: '已驳回', TAKEN_DOWN: '已下架' }
      return map[s] || s
    },
    async fetch() {
      this.loading = true
      try {
        const data = await request({
          url: `/admin/content/list?status=${this.status}&page=1&pageSize=20`
        })
        this.list = data.list || []
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    switchTab(v) {
      if (this.status === v) return
      this.status = v
      this.fetch()
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
            this.fetch()
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
            this.fetch()
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
            this.fetch()
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
  padding: 24rpx;
}
.tabs {
  display: flex;
  margin-bottom: 20rpx;
}
.tab {
  padding: 12rpx 36rpx;
  border-radius: 999rpx;
  background: #f1efe8;
  margin-right: 20rpx;
}
.tab.active {
  background: #0f6e56;
}
.tab-text {
  font-size: 26rpx;
  color: #5f5e5a;
}
.tab-text.active {
  color: #ffffff;
}
.item {
  padding: 20rpx;
}
.item-main {
  display: flex;
}
.thumb {
  width: 120rpx;
  height: 120rpx;
  border-radius: 12rpx;
  background: #f1efe8;
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
}
.reason {
  color: #a32d2d;
  font-size: 22rpx;
}
.ops {
  display: flex;
  justify-content: flex-end;
  margin-top: 16rpx;
}
.op {
  font-size: 24rpx;
  border-radius: 999rpx;
  padding: 0 36rpx;
  height: 56rpx;
  line-height: 56rpx;
  margin: 0 0 0 16rpx;
}
.op-pass {
  background: #0f6e56;
  color: #ffffff;
}
.op-reject {
  background: #fcebeb;
  color: #a32d2d;
}
.op-plain {
  background: #f1efe8;
  color: #5f5e5a;
}
.empty {
  text-align: center;
  padding: 80rpx 0;
}
</style>
