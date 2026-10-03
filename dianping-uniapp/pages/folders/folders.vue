<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <!-- 头部 -->
    <view class="head">
      <text class="title">收藏夹</text>
      <text class="sub">管理你的收藏分类，删除后内容回落到未分类</text>
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

    <view v-if="!loading && !folders.length" class="empty">
      <text class="muted">还没有收藏夹</text>
      <text class="muted small">点下面按钮新建一个吧</text>
    </view>

    <!-- 新建 -->
    <view class="add-bar" @tap="create">
      <text class="add-text">＋ 新建收藏夹</text>
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
.add-bar {
  margin: 24rpx 24rpx 0;
  background: var(--dp-card);
  border: 1rpx dashed var(--dp-text4);
  border-radius: 20rpx;
  padding: 32rpx 0;
  text-align: center;
}
.add-text {
  font-size: 28rpx;
  color: var(--dp-brand-deep);
  font-weight: 500;
}
</style>
