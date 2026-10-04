<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <!-- 头部品牌区 -->
    <view class="head">
      <view class="deco"></view>
      <view class="head-row">
        <!-- 点头像 = 打开编辑资料（内含「更换头像」）。
             原来直接跳图片选择器，而编辑资料藏在 设置 里，找起来费劲 -->
        <view class="avatar-wrap" @tap="openEdit">
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
        <!-- ☰ 侧边栏：次要功能统一收在这里（参考抖音/小红书） -->
        <view class="head-icons" @tap="drawerShow = true">
          <view class="drawer-btn">
            <view class="drawer-bar"></view>
            <view class="drawer-bar"></view>
            <view class="drawer-bar"></view>
          </view>
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
            <!-- 审核状态角标：非上架内容只有作者本人拿得到（后端按归属下发） -->
            <text
              v-if="item.status && item.status !== 'APPROVED'"
              class="wstate"
              :class="{ reject: item.status === 'REJECTED' }"
            >{{ item.status === 'PENDING' ? '审核中' : '未通过' }}</text>
          </view>
          <text class="wtitle">{{ item.title }}</text>
          <view class="wfoot">
            <text class="wlike-num">{{ item.likeCount }} 赞</text>
            <!-- 播放量属作者私有数据：后端只对作者本人下发，非作者为 null -->
            <text v-if="item.viewCount != null" class="wview-num">{{ item.viewCount }} 播放</text>
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
            <!-- 审核状态角标：非上架内容只有作者本人拿得到（后端按归属下发） -->
            <text
              v-if="item.status && item.status !== 'APPROVED'"
              class="wstate"
              :class="{ reject: item.status === 'REJECTED' }"
            >{{ item.status === 'PENDING' ? '审核中' : '未通过' }}</text>
          </view>
          <text class="wtitle">{{ item.title }}</text>
          <view class="wfoot">
            <text class="wlike-num">{{ item.likeCount }} 赞</text>
            <!-- 播放量属作者私有数据：后端只对作者本人下发，非作者为 null -->
            <text v-if="item.viewCount != null" class="wview-num">{{ item.viewCount }} 播放</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 引导式空状态：说清楚 + 给下一步（原则 09，去掉 emoji） -->
    <view v-if="!loading && !list.length" class="empty">
      <view class="empty-ic">
        <image class="empty-ic-img" :src="emptyInfo.icon" mode="aspectFit" />
      </view>
      <text class="empty-t">{{ emptyInfo.title }}</text>
      <text class="empty-d">{{ emptyInfo.desc }}</text>
      <view v-if="emptyInfo.btn" class="empty-btn" @tap="goEmptyAction">{{ emptyInfo.btn }}</view>
    </view>

    <!-- 侧边栏抽屉：功能入口不占主屏（抖音/小红书式） -->
    <view v-if="drawerShow" class="drawer-mask" @tap="drawerShow = false">
      <view class="drawer" @tap.stop>
        <view class="drawer-head">
          <view class="drawer-avatar">
            <image v-if="user && user.avatar" class="drawer-avatar-img" :src="user.avatar" mode="aspectFill" />
            <text v-else class="drawer-avatar-text">{{ shortName }}</text>
          </view>
          <view class="drawer-id">
            <text class="drawer-name">{{ user ? user.nickname : '-' }}</text>
            <text class="drawer-sub">点评号：{{ dianpingNo }}</text>
          </view>
        </view>
        <scroll-view class="drawer-list" scroll-y="true">
          <view class="drawer-item" @tap="drawerGo('goEditProfile')">
            <text class="drawer-text">编辑资料</text><text class="drawer-arrow">›</text>
          </view>
          <view class="drawer-item" v-if="user && user.role === 'USER'" @tap="drawerGo('goGuide')">
            <text class="drawer-text">如何成为点评人</text><text class="drawer-arrow">›</text>
          </view>
          <view class="drawer-item" @tap="drawerGo('goFolders')">
            <text class="drawer-text">收藏夹管理</text><text class="drawer-arrow">›</text>
          </view>
          <view class="drawer-item" @tap="drawerGo('goBrowse')">
            <text class="drawer-text">浏览记录</text><text class="drawer-arrow">›</text>
          </view>
          <view class="drawer-item" @tap="drawerGo('goDraftbox')">
            <text class="drawer-text">草稿箱</text><text class="drawer-arrow">›</text>
          </view>
          <view class="drawer-item" @tap="drawerGo('goTopics')">
            <text class="drawer-text">关注的话题</text><text class="drawer-arrow">›</text>
          </view>
          <view class="drawer-item" @tap="drawerGo('goSettings')">
            <text class="drawer-text">设置</text><text class="drawer-arrow">›</text>
          </view>
          <!-- 管理端入口 -->
          <view class="drawer-item" v-if="isAdminUser" @tap="drawerGo('goAdminAudit')">
            <text class="drawer-text">内容管理（后台）</text><text class="drawer-arrow">›</text>
          </view>
          <view class="drawer-item" v-if="isAdminUser" @tap="drawerGo('goAnnounce')">
            <text class="drawer-text">发布公告</text><text class="drawer-arrow">›</text>
          </view>
          <view class="drawer-item" v-if="isAdminUser" @tap="drawerGo('goAdminUsers')">
            <text class="drawer-text">用户管理（后台）</text><text class="drawer-arrow">›</text>
          </view>
        </scroll-view>
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
      drawerShow: false,
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
    /** 空状态文案：说清楚 + 给下一步（原则 09） */
    emptyInfo() {
      if (this.tab === 'note') {
        return {
          icon: '/static/icons/topic.png',
          title: '还没有发布过笔记',
          desc: this.isReviewer ? '发布后长按卡片可编辑或删除' : '成为点评人后即可发布',
          btn: this.isReviewer ? '写第一条点评' : ''
        }
      }
      if (this.tab === 'fav') {
        return {
          icon: '/static/icons/star.png',
          title: '还没有收藏内容',
          desc: '在详情页点收藏，就能在这里找到',
          btn: '去逛逛'
        }
      }
      return {
        icon: '/static/icons/heart.png',
        title: '还没有赞过内容',
        desc: '点赞后会出现在这里',
        btn: '去逛逛'
      }
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
        // 回写登录态：角色可能被后台改过（如刚被授予点评人），
        // 不回写的话 isReviewer() 读到的永远是"登录那一刻"的旧值，必须退出重登
        setUserInfo(Object.assign({}, getUser(), me))
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
    /** 抽屉入口：收抽屉后打开编辑资料面板 */
    goEditProfile() {
      this.drawerShow = false
      this.openEdit()
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
    /** 抽屉条目点击：先收起抽屉，再执行跳转 */
    drawerGo(fn) {
      this.drawerShow = false
      if (typeof this[fn] === 'function') this[fn]()
    },
    /** 空状态的引导动作（feed/publish 均为 tabBar 页，需用 switchTab） */
    goEmptyAction() {
      if (this.tab === 'note') {
        uni.switchTab({ url: '/pages/publish/publish' })
      } else {
        uni.switchTab({ url: '/pages/feed/feed' })
      }
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
    goDraftbox() {
      uni.navigateTo({ url: '/pages/draftbox/draftbox' })
    },
    goTopics() {
      uni.navigateTo({ url: '/pages/topics/topics' })
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
  background: linear-gradient(165deg, var(--dp-accent-soft), var(--dp-card) 58%);
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
  background: linear-gradient(135deg, var(--dp-orange), var(--dp-brand));
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
/* ☰ 三横线（纯 CSS，不需要图标资源） */
.drawer-btn {
  width: 44rpx;
  height: 44rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8rpx;
}
.drawer-bar {
  height: 4rpx;
  border-radius: 2rpx;
  background: var(--dp-text);
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
  background: var(--dp-brand-deep);
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
  color: var(--dp-brand-deep);
  font-weight: 500;
}
.folder-chip-count {
  font-size: 20rpx;
  color: var(--dp-text4);
  margin-left: 10rpx;
}
.folder-chip.on .folder-chip-count {
  color: var(--dp-brand);
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
  /* 给绝对定位的 .wstate 审核角标做定位参照 */
  position: relative;
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
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: 8rpx 14rpx 14rpx;
}
.wlike-num {
  font-size: var(--fs-caption);
  color: var(--dp-text3);
}
/* 播放量：只有作者本人拿得到（后端按归属下发，非作者为 null 不渲染） */
.wview-num {
  font-size: var(--fs-caption);
  color: var(--dp-brand-deep);
  margin-left: auto;
}
/* 审核状态角标：压在封面上，只有作者本人能看到自己的待审/驳回内容 */
.wstate {
  position: absolute;
  left: 12rpx;
  top: 12rpx;
  padding: 4rpx 14rpx;
  border-radius: var(--r-pill);
  background: rgba(28, 25, 23, .72);
  color: #ffffff;
  font-size: var(--fs-caption);
  font-weight: 600;
}
.wstate.reject {
  background: rgba(163, 45, 45, .9);
}
/* 侧边栏抽屉（抖音/小红书式：功能入口不占主屏） */
.drawer-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(28, 25, 23, .4);
  /* 同上：抽屉要盖住 tabBar(998)，否则抽屉底部一截被底栏压住 */
  z-index: 1000;
}
.drawer {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 560rpx;
  max-width: 78%;
  background: var(--dp-bg);
  box-shadow: -16rpx 0 48rpx rgba(28, 25, 23, .2);
  display: flex;
  flex-direction: column;
}
.drawer-head {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 96rpx 32rpx 32rpx;
  border-bottom: 1rpx solid var(--dp-line);
}
.drawer-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: var(--r-pill);
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(140deg, var(--dp-orange), var(--dp-brand-deep));
}
.drawer-avatar-img {
  width: 100%;
  height: 100%;
}
.drawer-avatar-text {
  color: #ffffff;
  font-size: var(--fs-md);
  font-weight: 700;
}
.drawer-id {
  flex: 1;
  min-width: 0;
}
.drawer-name {
  display: block;
  font-size: var(--fs-md);
  font-weight: 700;
  color: var(--dp-text);
}
.drawer-sub {
  display: block;
  font-size: var(--fs-sm);
  color: var(--dp-text3);
  margin-top: 4rpx;
}
.drawer-list {
  flex: 1;
  padding: var(--sp-2) 0;
}
.drawer-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--sp-3) var(--sp-4);
}
.drawer-text {
  font-size: var(--fs-base);
  color: var(--dp-text);
}
.drawer-arrow {
  font-size: var(--fs-md);
  color: var(--dp-text4);
}
.badge {
  background: var(--dp-brand-deep);
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
/* 引导式空状态（原则 09，去掉 emoji） */
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
  /* H5 的 tabBar 是 z-index:998，弹层必须高于它，否则底部按钮被压住 */
  z-index: 1000;
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
  color: var(--dp-brand-deep);
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
  background: var(--dp-brand-deep);
  color: #ffffff;
}
</style>
