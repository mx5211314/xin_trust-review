<template>
  <view class="page">
    <view class="search-row">
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜手机号 / 昵称"
        placeholder-class="ph"
        confirm-type="search"
        @confirm="search"
      />
      <button class="search-btn" @tap="search">搜索</button>
    </view>

    <view v-for="u in list" :key="u.id" class="card user-item">
      <view class="avatar avatar-ph">
        <text class="avatar-ph-text">{{ (u.nickname || '客').slice(0, 1) }}</text>
      </view>
      <view class="user-info">
        <view class="name-row">
          <text class="nickname">{{ u.nickname }}</text>
          <text v-if="u.role === 'REVIEWER'" class="tag tag-reviewer">点评人</text>
          <text v-else-if="u.role === 'ADMIN'" class="tag tag-admin">管理员</text>
          <text v-else class="tag muted-tag">普通</text>
          <text v-if="u.status === 'BANNED'" class="tag tag-banned">已封禁</text>
        </view>
        <text class="muted">{{ u.phone }}</text>
      </view>
      <view class="ops">
        <button
          v-if="u.role === 'USER'"
          class="op op-green"
          @tap="grant(u)"
        >授权点评人</button>
        <button
          v-if="u.role === 'REVIEWER'"
          class="op op-plain"
          @tap="revoke(u)"
        >收回授权</button>
        <button
          v-if="u.status !== 'BANNED' && u.role !== 'ADMIN'"
          class="op op-red"
          @tap="ban(u)"
        >封禁</button>
        <button
          v-if="u.status === 'BANNED'"
          class="op op-green"
          @tap="unban(u)"
        >解除封禁</button>
      </view>
    </view>

    <view v-if="!loading && list.length === 0" class="empty">
      <text class="muted">没有匹配的用户</text>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { isAdmin } from '@/utils/auth'

export default {
  data() {
    return {
      keyword: '',
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
    async fetch() {
      this.loading = true
      try {
        const page = await request({
          url: `/admin/user/list?page=1&pageSize=50` +
            (this.keyword.trim() ? `&keyword=${encodeURIComponent(this.keyword.trim())}` : '')
        })
        this.list = (page.records || []).map(u => ({
          id: String(u.id),
          nickname: u.nickname,
          phone: u.phone,
          role: u.role,
          status: u.status
        }))
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    search() {
      this.fetch()
    },
    confirm(title, action, tip) {
      return new Promise((resolve) => {
        uni.showModal({
          title,
          success: (res) => resolve(!!res.confirm)
        })
      })
    },
    async grant(u) {
      if (!(await this.confirm(`把「${u.nickname}」设为点评人？`))) return
      try {
        await request({ url: `/admin/reviewer/grant?userId=${u.id}`, method: 'POST' })
        uni.showToast({ title: '已授权', icon: 'success' })
        this.fetch()
      } catch (e) { /* toast 已提示 */ }
    },
    async revoke(u) {
      if (!(await this.confirm(`收回「${u.nickname}」的点评人资格？`))) return
      try {
        await request({ url: `/admin/reviewer/revoke?userId=${u.id}`, method: 'POST' })
        uni.showToast({ title: '已收回', icon: 'success' })
        this.fetch()
      } catch (e) { /* toast 已提示 */ }
    },
    async ban(u) {
      if (!(await this.confirm(`封禁「${u.nickname}」？封禁后无法登录`))) return
      try {
        await request({ url: `/admin/user/${u.id}/ban`, method: 'POST' })
        uni.showToast({ title: '已封禁', icon: 'success' })
        this.fetch()
      } catch (e) { /* toast 已提示 */ }
    },
    async unban(u) {
      if (!(await this.confirm(`解除「${u.nickname}」的封禁？`))) return
      try {
        await request({ url: `/admin/user/${u.id}/unban`, method: 'POST' })
        uni.showToast({ title: '已解除', icon: 'success' })
        this.fetch()
      } catch (e) { /* toast 已提示 */ }
    }
  }
}
</script>

<style scoped>
.page {
  padding: 24rpx;
}
.search-row {
  display: flex;
  margin-bottom: 20rpx;
}
.search-input {
  flex: 1;
  background: #ffffff;
  border-radius: 16rpx;
  padding: 20rpx 28rpx;
  font-size: 28rpx;
  margin-right: 16rpx;
}
.ph {
  color: #b4b2a9;
}
.search-btn {
  background: #0f6e56;
  color: #ffffff;
  font-size: 26rpx;
  border-radius: 16rpx;
  padding: 0 36rpx;
  height: 76rpx;
  line-height: 76rpx;
  margin: 0;
}
.user-item {
  margin-bottom: 20rpx;
}
.user-item::after {
  content: '';
  display: block;
  clear: both;
}
.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: #cecbf6;
  margin-right: 20rpx;
  float: left;
}
.avatar-ph {
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-ph-text {
  font-size: 32rpx;
  color: #3c3489;
}
.user-info {
  float: left;
  padding-top: 6rpx;
}
.name-row {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}
.nickname {
  font-size: 28rpx;
  font-weight: 500;
  margin-right: 12rpx;
}
.muted-tag {
  background: #f1efe8;
  color: #888780;
}
.ops {
  float: right;
  width: 100%;
  margin-top: 16rpx;
  display: flex;
  justify-content: flex-end;
}
.op {
  font-size: 22rpx;
  border-radius: 999rpx;
  padding: 0 28rpx;
  height: 52rpx;
  line-height: 52rpx;
  margin: 0 0 0 16rpx;
}
.op-green {
  background: #e1f5ee;
  color: #0f6e56;
}
.op-red {
  background: var(--dp-danger-soft);
  color: var(--dp-danger);
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
