<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <!-- 头部：新建入口放右上角，20 个夹子也不用滚到底 -->
    <view class="head">
      <view class="head-main">
        <text class="title">收藏夹</text>
        <text class="sub">管理你的收藏分类，删除后内容回落到未分类</text>
      </view>
      <view class="head-add" @tap="create">
        <text class="head-add-text">＋ 新建</text>
      </view>
    </view>

    <!-- 收藏夹列表 -->
    <view class="list">
      <view v-for="f in folders" :key="f.folderId" class="row">
        <view class="row-main" @tap="openFolder(f)">
          <view class="ficon">
            <text class="ficon-text">{{ f.name.slice(0, 1) }}</text>
          </view>
          <view class="row-body">
            <text class="name">{{ f.name }}</text>
            <text class="count">{{ f.count }} 篇</text>
          </view>
        </view>
        <view v-if="f.folderId !== '0'" class="row-actions">
          <text class="act" @tap="rename(f)">改名</text>
          <text class="act danger" @tap="remove(f)">删除</text>
        </view>
        <text v-else class="locked">默认</text>
      </view>
    </view>

    <!-- 引导式空状态（原则 09） -->
    <view v-if="!loading && !folders.length" class="empty">
      <view class="empty-ic">
        <image class="empty-ic-img" src="/static/icons/star.png" mode="aspectFit" />
      </view>
      <text class="empty-t">还没有收藏夹</text>
      <text class="empty-d">点右上角「新建」，把收藏的内容分门别类</text>
      <view class="empty-btn" @tap="create">新建收藏夹</view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { getUser } from '@/utils/auth'

export default {
  data() {
    return {
      folders: [],
      loading: false
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
    async load() {
      this.loading = true
      try {
        this.folders = (await request({ url: '/user/favorite/folders', silent: true })) || []
      } catch (e) {
        this.folders = []
      } finally {
        this.loading = false
      }
    },
    /** 点击进入该收藏夹内容（跳 mine 页收藏 tab 并预选 folderId，通过 storage 标记） */
    openFolder(f) {
      uni.setStorageSync('dp_open_folder', String(f.folderId))
      uni.switchTab({ url: '/pages/mine/mine' })
    },
    /** 新建收藏夹 */
    create() {
      uni.showModal({
        title: '新建收藏夹',
        editable: true,
        placeholderText: '如：美食清单',
        success: async (res) => {
          if (!res.confirm) return
          const name = (res.content || '').trim()
          if (!name) return uni.showToast({ title: '名称不能为空', icon: 'none' })
          try {
            await request({ url: '/user/favorite/folder', method: 'POST', data: { name } })
            uni.showToast({ title: '已创建', icon: 'success' })
            await this.load()
          } catch (e) { /* toast 已提示 */ }
        }
      })
    },
    /** 重命名（PATCH /favorite/folder/{id}） */
    rename(f) {
      uni.showModal({
        title: '重命名收藏夹',
        editable: true,
        content: f.name,
        placeholderText: '输入新名称',
        success: async (res) => {
          if (!res.confirm) return
          const name = (res.content || '').trim()
          if (!name || name === f.name) return
          try {
            await request({
              url: `/user/favorite/folder/${f.folderId}`,
              method: 'PATCH',
              data: { name }
            })
            uni.showToast({ title: '已改名', icon: 'success' })
            await this.load()
          } catch (e) { /* toast 已提示 */ }
        }
      })
    },
    /** 删除（DELETE /favorite/folder/{id}，夹内收藏回落未分类） */
    remove(f) {
      uni.showModal({
        title: '删除收藏夹',
        content: `「${f.name}」删除后，里面的 ${f.count} 篇收藏会移到"未分类"，内容不会丢失`,
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({ url: `/user/favorite/folder/${f.folderId}`, method: 'DELETE', silent: true })
            uni.showToast({ title: '已删除', icon: 'none' })
            await this.load()
          } catch (e) { /* toast 已提示 */ }
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
.head {
  display: flex;
  align-items: flex-start;
  gap: var(--sp-3);
  background: linear-gradient(165deg, var(--dp-accent-soft), var(--dp-card) 58%);
  padding: 40rpx 32rpx 36rpx;
}
.head-main {
  flex: 1;
  min-width: 0;
}
/* 新建入口：固定在页头，不随列表变长而下沉 */
.head-add {
  flex-shrink: 0;
  margin-top: 8rpx;
  padding: 12rpx 26rpx;
  border-radius: var(--r-pill);
  background: var(--dp-brand-deep);
  box-shadow: var(--sh-float);
}
.head-add-text {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: #ffffff;
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
.list {
  background: var(--dp-card);
  margin: 20rpx 24rpx;
  border-radius: 20rpx;
  overflow: hidden;
}
.row {
  display: flex;
  align-items: center;
  padding: 26rpx 28rpx;
  border-bottom: 1rpx solid var(--dp-soft);
}
.row:last-child {
  border-bottom: none;
}
.row-main {
  flex: 1;
  display: flex;
  align-items: center;
  min-width: 0;
}
.ficon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, var(--dp-orange), var(--dp-brand));
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 22rpx;
  flex-shrink: 0;
}
.ficon-text {
  font-size: 34rpx;
  color: #ffffff;
  font-weight: 600;
}
.row-body {
  flex: 1;
  min-width: 0;
}
.name {
  display: block;
  font-size: 30rpx;
  font-weight: 500;
  color: var(--dp-text);
}
.count {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--dp-text3);
}
.row-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.act {
  font-size: 26rpx;
  color: var(--dp-brand-deep);
  padding: 8rpx 16rpx;
}
.act.danger {
  color: var(--dp-text3);
}
.locked {
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
/* 原底部「＋新建收藏夹」已上移到页头（见 .head-add），此处样式移除 */
</style>
