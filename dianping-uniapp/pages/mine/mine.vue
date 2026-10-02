<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <!-- 头部品牌区 -->
    <view class="head">
      <view class="deco"></view>
      <view class="head-row">
        <view class="avatar-wrap" @tap="changeAvatar">
          <view class="avatar">
            <image v-if="user && user.avatar" class="avatar-img" :src="user.avatar" mode="aspectFill" />
            <text v-else class="avatar-text">{{ shortName }}</text>
          </view>
          <view class="cam-badge">
            <image class="cam-img" src="/static/icons/camera.png" mode="aspectFit" />
          </view>
        </view>
        <view class="head-info">
          <view class="name-row">
            <text class="nickname">{{ user ? user.nickname : '-' }}</text>
            <view v-if="user && user.role === 'REVIEWER'" class="tag tag-reviewer">
              <image class="tag-star" src="/static/icons/star-red.png" mode="aspectFit" />
              <text>点评人</text>
            </view>
            <text v-else-if="user && user.role === 'ADMIN'" class="tag tag-admin">管理员</text>
          </view>
          <text class="muted head-muted">点评号：{{ dianpingNo }}</text>
          <text v-if="user && user.bio" class="muted head-muted">{{ user.bio }}</text>
        </view>
        <view class="head-icons" @tap="goSettings">
          <image class="gear-img" src="/static/icons/gear.png" mode="aspectFit" />
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
      <view class="stat" @tap="goFans">
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

    <!-- 收藏夹筛选条（仅收藏 tab） -->
    <scroll-view v-if="tab === 'fav'" class="folder-bar" scroll-x="true" :show-scrollbar="false">
      <view
        v-for="f in folders"
        :key="f.folderId"
        class="folder-chip"
        :class="{ on: curFolder === f.folderId }"
        @tap="pickFolder(f.folderId)"
      >
        <text class="folder-chip-text">{{ f.name }}</text>
        <text class="folder-chip-count">{{ f.count }}</text>
      </view>
      <view class="folder-chip folder-add" @tap="newFolder">
        <text class="folder-add-text">＋ 新建</text>
      </view>
    </scroll-view>

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
      <view class="menu-item" @tap="goFolders">
        <text class="menu-text">收藏夹管理</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goBrowse">
        <text class="menu-text">浏览记录</text>
        <text class="menu-arrow">›</text>
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
import { getUser, setUserInfo, logout, isAdmin } from '@/utils/auth'
import { uploadFile } from '@/utils/upload'

export default {
  data() {
    return {
      user: null,
      stats: { posts: 0, likes: 0, views: 0, following: 0, followers: 0 },
      tab: 'note',
      folders: [],
      curFolder: '0', // 后端统一返回字符串，'0'=未分类
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
    // 设置页"编辑资料"跳来（tab 页不能用 navigateTo 带参 → storage 标记）
    try {
      if (uni.getStorageSync('dp_open_edit') === '1') {
        uni.removeStorageSync('dp_open_edit')
        setTimeout(() => this.openEdit(), 300)
      }
      // 收藏夹管理页点某夹 → 进收藏 tab 并预选该夹
      const openFolder = uni.getStorageSync('dp_open_folder')
      if (openFolder) {
        uni.removeStorageSync('dp_open_folder')
        this.tab = 'fav'
        this.curFolder = String(openFolder)
        this.page = 1
        this.hasMore = true
        this.list = []
        this.loadFolders()
        this.fetch()
      }
    } catch (e) { /* ignore */ }
  },
  onReachBottom() {
    if (this.hasMore && !this.loading) {
      this.page += 1
      this.fetch(true)
    }
  },
  methods: {
    /** 点击头像：更换头像（选图 → 传 OSS → PUT /user/profile → 同步本地 → 刷新） */
    changeAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: async (res) => {
          const temp = res.tempFilePaths && res.tempFilePaths[0]
          if (!temp) return
          uni.showLoading({ title: '上传中…' })
          try {
            const up = await uploadFile(temp, 'image')
            await request({
              url: '/user/profile',
              method: 'PUT',
              data: {
                nickname: (this.user && this.user.nickname) || '',
                avatar: up.key,
                bio: (this.user && this.user.bio) || ''
              }
            })
            // 同步本地登录态，下次进入头像不丢
            const u = getUser() || {}
            setUserInfo(Object.assign({}, u, { avatar: up.url || u.avatar }))
            uni.hideLoading()
            uni.showToast({ title: '头像已更新', icon: 'success' })
            this.page = 1
            this.fetch()
          } catch (e) {
            uni.hideLoading()
          }
        }
      })
    },
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
            url: `/user/favorites?page=${this.page}&pageSize=${this.pageSize}&folderId=${this.curFolder}`
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
      if (t === 'fav') {
        this.curFolder = '0'
        this.loadFolders()
      }
      this.fetch()
    },
    /** 收藏夹列表（含各夹收藏数）。注意：接口挂在 /user 前缀下 */
    async loadFolders() {
      try {
        this.folders = await request({ url: '/user/favorite/folders', silent: true }) || []
      } catch (e) {
        this.folders = []
      }
    },
    /** 切换收藏夹：重新拉对应夹内容 */
    pickFolder(id) {
      if (this.curFolder === id) return
      this.curFolder = id
      this.page = 1
      this.hasMore = true
      this.list = []
      this.fetch()
    },
    /** 新建收藏夹（弹窗输入名称） */
    newFolder() {
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
            await this.loadFolders()
          } catch (e) { /* toast 已提示 */ }
        }
      })
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
    goSettings() {
      uni.navigateTo({ url: '/pages/settings/settings' })
    },
    goFollowing() {
      uni.navigateTo({ url: '/pages/following/following?tab=follow' })
    },
    goFans() {
      uni.navigateTo({ url: '/pages/following/following?tab=fans' })
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
    goFolders() {
      uni.navigateTo({ url: '/pages/folders/folders' })
    },
    goBrowse() {
      uni.navigateTo({ url: '/pages/browse/browse' })
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
  background: linear-gradient(165deg, #ffe4e9, #fff1f3 55%, #ffffff);
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
  background: rgba(255, 36, 66, 0.07);
}
.head-row {
  display: flex;
  align-items: center;
  position: relative;
}
.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}
.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffb199, #ff2442);
  margin-right: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: visible;
}
.avatar-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  display: block;
}
/* 相机角标：提示可点击更换头像 */
.cam-badge {
  position: absolute;
  right: 20rpx;
  bottom: 0;
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: rgba(31, 36, 48, 0.72);
  border: 3rpx solid #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}
.cam-img {
  width: 24rpx;
  height: 24rpx;
}
.avatar-text {
  font-size: 52rpx;
  color: #ffffff;
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
  font-weight: 600;
  color: var(--dp-text);
  margin-right: 16rpx;
}
.head-muted {
  display: block;
  color: var(--dp-text3) !important;
  font-size: 22rpx;
}
.head-icons {
  padding: 10rpx;
}
.gear-img {
  width: 40rpx;
  height: 40rpx;
}
.stats {
  background: var(--dp-card);
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
  font-family: Georgia, serif;
  font-style: italic;
  font-size: 40rpx;
  font-weight: 700;
  color: var(--dp-text);
}
.stat-label {
  font-size: 22rpx;
  color: var(--dp-text3);
}
.stat-div {
  width: 1rpx;
  height: 50rpx;
  background: var(--dp-line);
}
.tabs {
  display: flex;
  justify-content: center;
  background: var(--dp-card);
  margin-top: 20rpx;
  border-bottom: 1rpx solid var(--dp-line);
}
.tab {
  padding: 22rpx 40rpx;
  position: relative;
}
.tab-text {
  font-size: 28rpx;
  color: var(--dp-text3);
}
.tab-text.on {
  color: var(--dp-text);
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
  background: var(--dp-bg);
  min-height: 200rpx;
}
.folder-bar {
  white-space: nowrap;
  background: var(--dp-card);
  padding: 18rpx 20rpx;
  border-bottom: 1rpx solid var(--dp-line);
}
.folder-chip {
  display: inline-flex;
  align-items: center;
  padding: 12rpx 24rpx;
  margin-right: 16rpx;
  background: var(--dp-soft);
  border-radius: 999rpx;
}
.folder-chip.on {
  background: var(--dp-accent-soft);
}
.folder-chip-text {
  font-size: 26rpx;
  color: #555555;
}
.folder-chip.on .folder-chip-text {
  color: #ff2442;
  font-weight: 500;
}
.folder-chip-count {
  font-size: 20rpx;
  color: var(--dp-text4);
  margin-left: 10rpx;
}
.folder-chip.on .folder-chip-count {
  color: #ff7a8e;
}
.folder-add {
  background: transparent;
  border: 1rpx dashed var(--dp-text4);
}
.folder-add-text {
  font-size: 26rpx;
  color: var(--dp-text3);
}
.col {
  flex: 1;
}
.col + .col {
  margin-left: 16rpx;
}
.wcard {
  background: var(--dp-card);
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
  background: var(--dp-soft);
}
.cover-empty-text {
  font-size: 22rpx;
  color: var(--dp-text4);
}
.wtitle {
  display: block;
  padding: 12rpx 14rpx 0;
  font-size: 25rpx;
  font-weight: 500;
  color: var(--dp-text);
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
  color: var(--dp-text3);
}
.menu {
  background: var(--dp-card);
  border-radius: 20rpx;
  margin: 20rpx 24rpx;
  padding: 0 28rpx;
}
.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 0;
  border-bottom: 1rpx solid var(--dp-soft);
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
  color: var(--dp-text);
}
.menu-text.logout {
  color: #ff2442;
}
.menu-arrow {
  color: var(--dp-text4);
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
  background: var(--dp-bg);
}
.empty-emoji {
  font-size: 70rpx;
  margin-bottom: 20rpx;
}
.small {
  font-size: 22rpx;
  color: var(--dp-text4);
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
  background: var(--dp-card);
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
  background: var(--dp-soft);
  border-radius: 16rpx;
  padding: 22rpx 26rpx;
  font-size: 28rpx;
  margin-bottom: 20rpx;
}
.ph {
  color: var(--dp-text4);
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
  background: var(--dp-soft);
  color: var(--dp-text2);
  margin-right: 20rpx;
}
.edit-btn.primary {
  background: #ff2442;
  color: #ffffff;
}
</style>
