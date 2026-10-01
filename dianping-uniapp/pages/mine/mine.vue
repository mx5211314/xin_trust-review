<template>
  <view class="page">
    <!-- 头部品牌区 -->
    <view class="head">
      <view class="deco"></view>
      <view class="head-row">
        <view class="avatar">
          <text class="avatar-text">{{ shortName }}</text>
        </view>
        <view class="head-info">
          <view class="name-row">
            <text class="nickname">{{ user ? user.nickname : '-' }}</text>
            <text v-if="user && user.role === 'REVIEWER'" class="tag tag-reviewer">点评人</text>
            <text v-else-if="user && user.role === 'ADMIN'" class="tag tag-admin">管理员</text>
          </view>
          <text class="muted head-muted">点评号：{{ dianpingNo }}</text>
          <text v-if="user && user.bio" class="muted head-muted">{{ user.bio }}</text>
        </view>
        <view class="head-icons" @tap="openEdit">
          <text class="icon-text">✎</text>
        </view>
      </view>
    </view>

    <!-- 数据行：关注 / 粉丝 / 获赞（小红书式） -->
    <view class="stats">
      <view class="stat" @tap="goFollowing">
        <text class="stat-num">{{ stats.following }}</text>
        <text class="stat-label">关注</text>
      </view>
      <view class="stat-div"></view>
      <view class="stat">
        <text class="stat-num">{{ stats.followers }}</text>
        <text class="stat-label">粉丝</text>
      </view>
      <view class="stat-div"></view>
      <view class="stat">
        <text class="stat-num">{{ stats.likes }}</text>
        <text class="stat-label">获赞</text>
      </view>
    </view>

    <!-- tab：笔记 / 收藏 / 赞过 -->
    <view class="tabs">
      <view class="tab" @tap="switchTab('note')">
        <text class="tab-text" :class="{ on: tab === 'note' }">笔记</text>
        <view v-if="tab === 'note'" class="tab-line"></view>
      </view>
      <view class="tab" @tap="switchTab('fav')">
        <text class="tab-text" :class="{ on: tab === 'fav' }">收藏</text>
        <view v-if="tab === 'fav'" class="tab-line"></view>
      </view>
      <view class="tab" @tap="switchTab('liked')">
        <text class="tab-text" :class="{ on: tab === 'liked' }">赞过</text>
        <view v-if="tab === 'liked'" class="tab-line"></view>
      </view>
    </view>

    <!-- 双列瀑布流 -->
    <view class="waterfall" v-if="list.length">
      <view class="col">
        <view
          v-for="item in leftList"
          :key="item.contentId"
          class="wcard"
          hover-class="card-hover"
          @tap="goDetail(item)"
          @longpress="onCardLong(item)"
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
              <text class="cover-empty-text">{{ item.coverError ? '图片加载失败' : '视频' }}</text>
            </view>
          </view>
          <text class="wtitle">{{ item.title }}</text>
          <view class="wfoot">
            <text class="wlike-num">{{ item.likeCount }} 赞</text>
          </view>
        </view>
      </view>
      <view class="col">
        <view
          v-for="item in rightList"
          :key="item.contentId"
          class="wcard"
          hover-class="card-hover"
          @tap="goDetail(item)"
          @longpress="onCardLong(item)"
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
              <text class="cover-empty-text">{{ item.coverError ? '图片加载失败' : '视频' }}</text>
            </view>
          </view>
          <text class="wtitle">{{ item.title }}</text>
          <view class="wfoot">
            <text class="wlike-num">{{ item.likeCount }} 赞</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="!loading && !list.length" class="empty">
      <text class="empty-emoji">📝</text>
      <text class="muted">{{ tab === 'note' ? '还没有发布过笔记' : tab === 'fav' ? '还没有收藏内容' : '还没有赞过内容' }}</text>
      <text v-if="tab === 'note' && isReviewer" class="muted small">长按卡片可编辑或删除</text>
    </view>

    <!-- 菜单 -->
    <view class="menu">
      <view class="menu-item" v-if="user && user.role === 'USER'" @tap="goGuide">
        <text class="menu-text">如何成为点评人</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" v-if="isAdminUser" @tap="goAdminAudit">
        <text class="menu-text">内容管理（后台）</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" v-if="isAdminUser" @tap="goAnnounce">
        <text class="menu-text">发布公告</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" v-if="isAdminUser" @tap="goAdminUsers">
        <text class="menu-text">用户管理（后台）</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="doLogout">
        <text class="menu-text logout">退出登录</text>
        <text class="menu-arrow"></text>
      </view>
    </view>

    <!-- 编辑资料弹层 -->
    <view v-if="editShow" class="edit-mask" @tap="editShow = false">
      <view class="edit-panel" @tap.stop>
        <text class="edit-title">编辑资料</text>
        <view class="edit-avatar-row">
          <image v-if="editAvatar" class="edit-avatar" :src="editAvatar" mode="aspectFill" />
          <view v-else class="edit-avatar edit-avatar-ph">
            <text class="muted">无头像</text>
          </view>
          <text class="edit-avatar-btn" @tap="chooseAvatar">更换头像</text>
        </view>
        <input v-model="editNickname" class="edit-input" maxlength="20" placeholder="昵称" placeholder-class="ph" />
        <input v-model="editBio" class="edit-input" maxlength="100" placeholder="一句话简介" placeholder-class="ph" />
        <view class="edit-btns">
          <button class="edit-btn ghost" @tap="editShow = false">取消</button>
          <button class="edit-btn primary" @tap="saveProfile">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { getUser, logout, isAdmin } from '@/utils/auth'
import { uploadFile } from '@/utils/upload'

export default {
  data() {
    return {
      user: null,
      stats: { posts: 0, likes: 0, views: 0, following: 0, followers: 0 },
      tab: 'note',
      list: [],
      page: 1,
      pageSize: 10,
      hasMore: true,
      loading: false,
      unread: 0,
      editShow: false,
      editNickname: '',
      editAvatar: '',
      editBio: ''
    }
  },
  computed: {
    isAdminUser() {
      return isAdmin()
    },
    isReviewer() {
      const u = this.user
      return !!u && (u.role === 'REVIEWER' || u.role === 'ADMIN')
    },
    shortName() {
      return this.user && this.user.nickname ? this.user.nickname.slice(0, 1) : '我'
    },
    dianpingNo() {
      const id = this.user && this.user.userId ? String(this.user.userId) : ''
      return id.length > 8 ? id.slice(-8) : id
    },
    leftList() {
      return this.list.filter((_, i) => i % 2 === 0)
    },
    rightList() {
      return this.list.filter((_, i) => i % 2 === 1)
    }
  },
  onShow() {
    if (!getUser()) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.user = getUser()
    this.page = 1
    this.fetch()
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
        const me = await request({ url: '/user/me' })
        this.user = Object.assign({}, getUser(), me)
        const stats = await request({ url: '/user/stats' })
        this.stats = Object.assign(this.stats, stats)
        let data
        if (this.tab === 'note') {
          data = await request({
            url: `/user/${this.user.userId}?page=${this.page}&pageSize=${this.pageSize}`
          })
          const items = data.contents ? data.contents.list : []
          this.list = append ? this.list.concat(items) : items
          this.hasMore = this.list.length < (data.contents ? data.contents.total : 0)
        } else if (this.tab === 'fav') {
          data = await request({
            url: `/user/favorites?page=${this.page}&pageSize=${this.pageSize}`
          })
          this.list = append ? this.list.concat(data.list) : data.list
          this.hasMore = this.list.length < data.total
        } else {
          data = await request({
            url: `/user/liked?page=${this.page}&pageSize=${this.pageSize}`
          })
          this.list = append ? this.list.concat(data.list) : data.list
          this.hasMore = this.list.length < data.total
        }
      } catch (e) {
        this.user = getUser()
      } finally {
        this.loading = false
      }
    },
    switchTab(t) {
      if (this.tab === t) return
      this.tab = t
      this.page = 1
      this.hasMore = true
      this.list = []
      this.fetch()
    },
    /** 长按卡片：编辑 / 删除（仅笔记 tab）——小红书式 */
    onCardLong(item) {
      if (this.tab !== 'note') return
      uni.showActionSheet({
        itemList: ['编辑', '删除'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.editNote(item)
          } else {
            this.delNote(item)
          }
        }
      })
    },
    goDetail(item) {
      uni.navigateTo({ url: '/pages/detail/detail?id=' + item.contentId })
    },
    editNote(item) {
      uni.navigateTo({ url: '/pages/publish/publish?id=' + item.contentId })
    },
    delNote(item) {
      uni.showModal({
        title: '删除笔记',
        content: '「' + item.title + '」删除后不可恢复',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({ url: `/content/${item.contentId}`, method: 'DELETE', silent: true })
            uni.showToast({ title: '已删除', icon: 'none' })
            this.page = 1
            this.fetch()
          } catch (e) { /* toast 已提示 */ }
        }
      })
    },
    openEdit() {
      this.editNickname = this.user ? this.user.nickname : ''
      this.editAvatar = this.user ? this.user.avatar : ''
      this.editBio = this.user && this.user.bio ? this.user.bio : ''
      this.editShow = true
    },
    chooseAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: async (res) => {
          try {
            uni.showLoading({ title: '上传中' })
            const up = await uploadFile(res.tempFilePaths[0], 'image')
            this.editAvatar = up.url
            uni.hideLoading()
          } catch (e) {
            uni.hideLoading()
          }
        }
      })
    },
    async saveProfile() {
      const nick = this.editNickname.trim()
      if (!nick) return uni.showToast({ title: '昵称不能为空', icon: 'none' })
      try {
        await request({
          url: '/user/profile',
          method: 'PUT',
          data: {
            nickname: nick,
            avatar: this.editAvatar || undefined,
            bio: this.editBio.trim() || undefined
          }
        })
        uni.showToast({ title: '已保存', icon: 'success' })
        this.editShow = false
        this.fetch()
      } catch (e) { /* toast 已提示 */ }
    },
    goNotify() {
      uni.navigateTo({ url: '/pages/notify/notify' })
    },
    goFollowing() {
      uni.navigateTo({ url: '/pages/following/following' })
    },
    goGuide() {
      uni.navigateTo({ url: '/pages/guide/guide' })
    },
    goAdminAudit() {
      uni.navigateTo({ url: '/pagesAdmin/audit/audit' })
    },
    goAdminUsers() {
      uni.navigateTo({ url: '/pagesAdmin/users/users' })
    },
    goAnnounce() {
      uni.navigateTo({ url: '/pagesAdmin/announce/announce' })
    },
    doLogout() {
      uni.showModal({
        title: '退出登录',
        content: '确定要退出吗？',
        success: (res) => {
          if (res.confirm) logout()
        }
      })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 40rpx;
}
.head {
  background: #ff2442;
  padding: 40rpx 32rpx 60rpx;
  position: relative;
  overflow: hidden;
}
.deco {
  position: absolute;
  right: -40rpx;
  top: -60rpx;
  width: 240rpx;
  height: 240rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}
.head-row {
  display: flex;
  align-items: center;
  position: relative;
}
.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: #ffffff;
  margin-right: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.avatar-text {
  font-size: 52rpx;
  color: #ff2442;
  font-weight: 500;
}
.head-info {
  flex: 1;
  min-width: 0;
}
.name-row {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}
.nickname {
  font-size: 36rpx;
  font-weight: 500;
  color: #ffffff;
  margin-right: 16rpx;
}
.head-muted {
  display: block;
  color: rgba(255, 255, 255, 0.75) !important;
  font-size: 22rpx;
}
.head-icons {
  padding: 10rpx;
}
.icon-text {
  font-size: 36rpx;
  color: rgba(255, 255, 255, 0.9);
}
.stats {
  background: #ffffff;
  border-radius: 20rpx;
  margin: -34rpx 24rpx 0;
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  position: relative;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
}
.stat {
  flex: 1;
  text-align: center;
}
.stat-num {
  display: block;
  font-size: 36rpx;
  font-weight: 500;
  color: #1f2430;
}
.stat-label {
  font-size: 22rpx;
  color: #999999;
}
.stat-div {
  width: 1rpx;
  height: 50rpx;
  background: #f1f3f5;
}
.tabs {
  display: flex;
  justify-content: center;
  background: #ffffff;
  margin-top: 20rpx;
  border-bottom: 1rpx solid #f1f3f5;
}
.tab {
  padding: 22rpx 40rpx;
  position: relative;
}
.tab-text {
  font-size: 28rpx;
  color: #999999;
}
.tab-text.on {
  color: #1f2430;
  font-weight: 500;
}
.tab-line {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48rpx;
  height: 5rpx;
  border-radius: 3rpx;
  background: #ff2442;
}
.waterfall {
  display: flex;
  padding: 20rpx 16rpx;
  background: #f7f8fa;
  min-height: 200rpx;
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
  transition: transform 0.15s ease, opacity 0.15s ease;
}
.card-hover {
  transform: scale(0.97);
  opacity: 0.85;
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
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.wfoot {
  padding: 8rpx 14rpx 14rpx;
}
.wlike-num {
  font-size: 20rpx;
  color: #999999;
}
.menu {
  background: #ffffff;
  border-radius: 20rpx;
  margin: 20rpx 24rpx;
  padding: 0 28rpx;
}
.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #f6f7f9;
}
.menu-item:last-child {
  border-bottom: none;
}
.menu-left {
  display: flex;
  align-items: center;
}
.menu-text {
  font-size: 28rpx;
  color: #1f2430;
}
.menu-text.logout {
  color: #ff2442;
}
.menu-arrow {
  color: #c2c8d0;
  font-size: 32rpx;
}
.badge {
  background: #ff2442;
  border-radius: 999rpx;
  padding: 2rpx 12rpx;
  margin-left: 12rpx;
}
.badge-text {
  color: #ffffff;
  font-size: 18rpx;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
  background: #f7f8fa;
}
.empty-emoji {
  font-size: 70rpx;
  margin-bottom: 20rpx;
}
.small {
  font-size: 22rpx;
  color: #c2c8d0;
  margin-top: 10rpx;
}
.edit-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 100;
  display: flex;
  align-items: flex-end;
}
.edit-panel {
  width: 100%;
  background: #ffffff;
  border-radius: 32rpx 32rpx 0 0;
  padding: 40rpx 40rpx calc(40rpx + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
}
.edit-title {
  font-size: 32rpx;
  font-weight: 500;
  text-align: center;
  margin-bottom: 32rpx;
}
.edit-avatar-row {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}
.edit-avatar {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background: #f1f2f4;
  margin-right: 24rpx;
}
.edit-avatar-ph {
  display: flex;
  align-items: center;
  justify-content: center;
}
.edit-avatar-btn {
  color: #ff2442;
  font-size: 26rpx;
}
.edit-input {
  background: #f6f7f9;
  border-radius: 16rpx;
  padding: 22rpx 26rpx;
  font-size: 28rpx;
  margin-bottom: 20rpx;
}
.ph {
  color: #b9c0c9;
}
.edit-btns {
  display: flex;
  margin-top: 10rpx;
}
.edit-btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  font-size: 28rpx;
  border-radius: 42rpx;
  padding: 0;
}
.edit-btn.ghost {
  background: #f6f7f9;
  color: #666666;
  margin-right: 20rpx;
}
.edit-btn.primary {
  background: #ff2442;
  color: #ffffff;
}
</style>
