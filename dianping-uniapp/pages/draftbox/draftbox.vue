<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <view class="topbar">
      <text class="title">草稿箱</text>
      <text v-if="list.length" class="clear" @tap="clearAll">清空</text>
    </view>

    <view v-if="list.length" class="list">
      <view
        v-for="d in list"
        :key="d.id"
        class="card"
        hover-class="card-hover"
        @tap="editDraft(d)"
      >
        <view class="card-body">
          <text class="card-title">{{ d.title || '无标题草稿' }}</text>
          <text class="card-snippet">{{ snippet(d.text) }}</text>
          <view class="card-meta">
            <text class="tag-type" :class="{ video: d.type === 'video' }">{{ d.type === 'video' ? '视频' : '图文' }}</text>
            <text v-if="d.poiName" class="meta-poi">{{ d.poiName }}</text>
            <text v-for="t in (d.tags || [])" :key="t" class="meta-tag">#{{ t }}</text>
          </view>
          <text class="card-time">{{ d.savedAt }}</text>
        </view>
        <view class="card-actions" @tap.stop>
          <text class="act" @tap.stop="editDraft(d)">继续编辑</text>
          <text class="act danger" @tap.stop="delDraft(d)">删除</text>
        </view>
      </view>
    </view>

    <view v-else class="empty">
      <text class="muted">草稿箱是空的</text>
      <text class="muted small">写点评时点「存草稿」或离开时自动保存，都会到这里</text>
    </view>
  </view>
</template>

<script>
import { getUser } from '@/utils/auth'

export default {
  data() {
    return {
      list: []
    }
  },
  onShow() {
    if (!getUser()) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.load()
  },
  methods: {
    load() {
      let arr = []
      try { arr = uni.getStorageSync('dp_drafts') || [] } catch (e) { arr = [] }
      // 倒序：最近保存在前
      this.list = arr.slice().sort((a, b) => (b.id || 0) - (a.id || 0))
    },
    snippet(text) {
      if (!text) return '（没有正文）'
      return text.length > 40 ? text.slice(0, 40) + '…' : text
    },
    /** 继续编辑：tab 页用 storage 标记跳转 publish */
    editDraft(d) {
      try { uni.setStorageSync('dp_edit_draft', String(d.id)) } catch (e) { /* ignore */ }
      uni.switchTab({ url: '/pages/publish/publish' })
    },
    delDraft(d) {
      uni.showModal({
        title: '删除草稿',
        content: '「' + (d.title || '无标题草稿') + '」删除后不可恢复',
        success: (res) => {
          if (!res.confirm) return
          let arr = []
          try { arr = uni.getStorageSync('dp_drafts') || [] } catch (e) { arr = [] }
          const next = arr.filter(x => x.id !== d.id)
          try { uni.setStorageSync('dp_drafts', next) } catch (e) { /* ignore */ }
          this.load()
        }
      })
    },
    clearAll() {
      if (!this.list.length) return
      uni.showModal({
        title: '清空草稿箱',
        content: `确定删除全部 ${this.list.length} 条草稿？此操作不可恢复`,
        success: (res) => {
          if (!res.confirm) return
          try { uni.removeStorageSync('dp_drafts') } catch (e) { /* ignore */ }
          this.load()
          uni.showToast({ title: '已清空', icon: 'none' })
        }
      })
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
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 88rpx 32rpx 20rpx;
  background: var(--dp-card);
}
.title {
  font-size: 36rpx;
  font-weight: 600;
  color: var(--dp-text);
}
.clear {
  font-size: 26rpx;
  color: var(--dp-text3);
}
.list {
  padding: 20rpx 24rpx;
}
.card {
  background: var(--dp-card);
  border-radius: 16rpx;
  padding: 24rpx 26rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.04);
}
.card-hover {
  transform: scale(0.99);
  opacity: 0.92;
}
.card-body {
  min-width: 0;
}
.card-title {
  display: block;
  font-size: 30rpx;
  font-weight: 500;
  color: var(--dp-text);
}
.card-snippet {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--dp-text3);
  line-height: 1.5;
}
.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 14rpx;
}
.tag-type {
  font-size: 20rpx;
  color: var(--dp-brand-deep);
  background: var(--dp-accent-soft);
  border-radius: 6rpx;
  padding: 4rpx 12rpx;
  margin-right: 12rpx;
}
.tag-type.video {
  color: #2b6cff;
  background: rgba(43, 108, 255, 0.1);
}
.meta-poi {
  font-size: 22rpx;
  color: var(--dp-text3);
  margin-right: 12rpx;
}
.meta-tag {
  font-size: 22rpx;
  color: var(--dp-text3);
  margin-right: 12rpx;
}
.card-time {
  display: block;
  margin-top: 14rpx;
  font-size: 20rpx;
  color: var(--dp-text4);
}
.card-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16rpx;
  border-top: 1rpx solid var(--dp-soft);
  padding-top: 16rpx;
}
.act {
  font-size: 26rpx;
  color: var(--dp-brand-deep);
  padding: 8rpx 20rpx;
}
.act.danger {
  color: var(--dp-text3);
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 140rpx 0;
}
.muted {
  font-size: 26rpx;
  color: var(--dp-text3);
}
.small {
  font-size: 22rpx;
  color: var(--dp-text4);
  margin-top: 10rpx;
  padding: 0 40rpx;
  text-align: center;
}
</style>
