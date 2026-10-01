<template>
  <view class="page">
    <!-- 自定义导航：取消 | 发笔记 | 发布 -->
    <view class="topbar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <text class="nav-cancel" @tap="goBack">取消</text>
      <text class="nav-title">{{ editId ? '编辑笔记' : '发笔记' }}</text>
      <view class="nav-publish" :class="{ dim: submitting }" @tap="submit">
        <text class="nav-publish-text">{{ submitting ? '…' : '发布' }}</text>
      </view>
    </view>
    <view :style="{ height: statusBarHeight + 54 + 'px' }"></view>

    <!-- 普通用户：引导 -->
    <view v-if="role === 'USER'" class="guide">
      <view class="guide-logo">
        <text class="guide-logo-text">★</text>
      </view>
      <text class="guide-title">发布功能仅对点评人开放</text>
      <text class="guide-text">本平台点评内容由各地区的认证点评人发布，保证信息真实可信。如你想成为点评人，走下面三步：</text>
      <view class="step"><text class="step-num">1</text><text class="step-text">联系管理员，说明你想点评的领域</text></view>
      <view class="step"><text class="step-num">2</text><text class="step-text">管理员确认后为你开通权限</text></view>
      <view class="step"><text class="step-num">3</text><text class="step-text">重新登录，开始发布你的第一条点评</text></view>
      <button class="btn-primary contact-btn" @tap="copyAdminPhone">复制管理员联系方式</button>
    </view>

    <!-- 点评人 / 管理员：发布表单 -->
    <view v-else class="form">
      <!-- 1. 图片区在最上（小红书式） -->
      <view v-if="type === 'image' && !editId" class="grid grid-top">
        <view v-for="(img, i) in localImages" :key="i" class="grid-item">
          <image class="grid-img" :src="img.path" mode="aspectFill" />
          <view class="grid-del" @tap="removeImage(i)">
            <text class="grid-del-text">×</text>
          </view>
          <view v-if="img.uploading" class="grid-mask">
            <text class="grid-mask-text">上传中</text>
          </view>
        </view>
        <view v-if="localImages.length < 9" class="grid-item add" @tap="chooseImages">
          <text class="add-plus">＋</text>
          <text class="add-count">{{ localImages.length }}/9</text>
        </view>
      </view>
      <view v-else-if="type === 'video' && !editId" class="grid grid-top">
        <view v-if="localVideo" class="grid-item video-item">
          <text class="video-name">{{ localVideo.name }}</text>
          <view class="grid-del" @tap="removeVideo">
            <text class="grid-del-text">×</text>
          </view>
          <view v-if="localVideo.uploading" class="grid-mask">
            <text class="grid-mask-text">上传中</text>
          </view>
        </view>
        <view v-else class="grid-item add" @tap="chooseVideo">
          <text class="add-plus">＋</text>
          <text class="add-count">视频</text>
        </view>
      </view>

      <!-- 2. 编辑模式：极简提示 -->
      <view v-if="editId" class="edit-tip">
        <text class="edit-tip-text">修改后需重新审核 · 图片暂不支持修改</text>
      </view>

      <!-- 3. 标题（大字无边框） -->
      <view class="count-wrap">
        <input
          v-model="title"
          class="input title-input"
          maxlength="30"
          placeholder="填写标题会有更多赞哦～"
          placeholder-class="ph"
        />
        <text class="count-text">{{ title.length }}/30</text>
      </view>
      <view class="divider"></view>

      <!-- 4. 正文（无边框） -->
      <view class="count-wrap">
        <textarea
          v-model="text"
          class="textarea textarea-clean"
          maxlength="2000"
          placeholder="分享你的真实体验，帮助大家避坑～"
          placeholder-class="ph"
        />
        <text class="count-text count-textarea">{{ text.length }}/2000</text>
      </view>
      <view class="divider"></view>

      <!-- 5. 类型切换（轻量小标签） -->
      <view class="type-row">
        <text class="type-tab" :class="{ active: type === 'image' }" @tap="type = 'image'">图文</text>
        <text class="type-sep">|</text>
        <text class="type-tab" :class="{ active: type === 'video' }" @tap="type = 'video'">视频</text>
      </view>
      <view class="divider"></view>

      <!-- 6. 话题（默认折叠成一行，点击展开） -->
      <view class="line-row" @tap="topicOpen = !topicOpen">
        <view class="row-left">
          <image class="ficon-img" src="/static/icons/topic.png" />
          <text class="line-topic" :class="{ picked: tags.length }">
          # {{ tags.length ? tags.join('  # ') : '添加话题' }}
          </text>
        </view>
        <text class="arrow">{{ topicOpen ? '▴' : '▾' }}</text>
      </view>
      <view v-if="topicOpen" class="topic-chips">
        <view
          v-for="t in allTags"
          :key="t"
          class="tchip"
          :class="{ on: tags.includes(t) }"
          @tap="toggleTag(t)"
        >
          <text class="tchip-text" :class="{ on: tags.includes(t) }"># {{ t }}</text>
        </view>
      </view>
      <view class="divider"></view>

      <!-- 7. 地点 -->
      <picker :range="regionNames" @change="onRegionChange">
        <view class="line-row">
          <view class="row-left">
            <image class="ficon-img" src="/static/icons/location.png" />
            <text class="line-topic" :class="{ picked: regionIndex >= 0 }">
              {{ regionIndex >= 0 ? regionNames[regionIndex] : '添加地区' }}
            </text>
          </view>
          <text class="arrow">▾</text>
        </view>
      </picker>
      <view class="poi-row-wrap">
        <image class="ficon-img" src="/static/icons/shop.png" />
        <input
          v-model="poiName"
          class="input poi-input"
          placeholder="店铺名（选填）"
          placeholder-class="ph"
        />
      </view>
      <view style="height: 60rpx"></view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { uploadFile } from '@/utils/upload'
import { REGIONS } from '@/utils/config'
import { getUser } from '@/utils/auth'

export default {
  data() {
    return {
      role: 'USER',
      type: 'image',
      title: '',
      text: '',
      localImages: [],
      localVideo: null,
      regionIndex: -1,
      regionNames: REGIONS.map(r => r.name),
      allTags: ['唐山美食', '探店', '咖啡', '遛娃', '拍照', '老店'],
      tags: [],
      topicOpen: false,
      poiName: '',
      submitting: false,
      editId: '',
      statusBarHeight: 20
    }
  },
  onLoad(query) {
    try {
      this.statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 20
    } catch (e) { /* 默认值兜底 */ }
    if (query && query.id) {
      this.editId = query.id
      this.loadForEdit()
    }
  },
  onShow() {
    const u = getUser()
    this.role = u ? u.role : 'USER'
  },
  methods: {
    toggleTag(t) {
      const i = this.tags.indexOf(t)
      if (i >= 0) {
        this.tags.splice(i, 1)
      } else if (this.tags.length < 5) {
        this.tags.push(t)
      } else {
        uni.showToast({ title: "话题最多 5 个", icon: "none" })
      }
    },
    chooseImages() {
      const left = 9 - this.localImages.length
      uni.chooseImage({
        count: left,
        sizeType: ['compressed'],
        success: (res) => {
          res.tempFilePaths.forEach(p => {
            this.localImages.push({ path: p, uploading: false, key: '' })
          })
        }
      })
    },
    removeImage(i) {
      this.localImages.splice(i, 1)
    },
    chooseVideo() {
      uni.chooseVideo({
        maxDuration: 60,
        compressed: true,
        success: (res) => {
          if (res.duration > 60) {
            return uni.showToast({ title: '视频最长 60 秒', icon: 'none' })
          }
          if (res.size > 100 * 1024 * 1024) {
            return uni.showToast({ title: '视频最大 100MB', icon: 'none' })
          }
          this.localVideo = {
            path: res.tempFilePath,
            thumb: res.thumbTempFilePath || '',
            name: '视频 ' + Math.round(res.duration) + 's',
            duration: Math.round(res.duration),
            uploading: false,
            key: ''
          }
        }
      })
    },
    removeVideo() {
      this.localVideo = null
    },
    onRegionChange(e) {
      this.regionIndex = Number(e.detail.value)
    },
    copyAdminPhone() {
      uni.setClipboardData({
        data: '13800000000',
        success: () => uni.showToast({ title: '已复制管理员手机号', icon: 'none' })
      })
    },
    goBack() {
      uni.switchTab({ url: '/pages/feed/feed' })
    },
    validate() {
      if (!this.title.trim()) return '标题不能为空'
      if (!this.text.trim()) return '正文不能为空'
      if (this.regionIndex < 0) return '请选择地区'
      if (!this.editId && this.type === 'image' && this.localImages.length === 0) return '至少选一张图片'
      if (!this.editId && this.type === 'video' && !this.localVideo) return '请选择视频'
      return ''
    },
    async submit() {
      const err = this.validate()
      if (err) return uni.showToast({ title: err, icon: 'none' })
      if (this.submitting) return
      this.submitting = true
      try {
        const images = []
        for (const img of this.localImages) {
          img.uploading = true
          const up = await uploadFile(img.path, 'image')
          img.key = up.key
          img.uploading = false
          images.push(img.key)
        }
        let videoKey = ''
        let coverKey = ''
        let duration = 0
        if (this.type === 'video') {
          this.localVideo.uploading = true
          const up2 = await uploadFile(this.localVideo.path, 'video')
          videoKey = up2.key
          this.localVideo.uploading = false
          duration = this.localVideo.duration
          // 用 chooseVideo 返回的首帧缩略图作封面，消除信息流视频灰块（后端 coverKey 已支持）
          if (this.localVideo.thumb) {
            const upc = await uploadFile(this.localVideo.thumb, 'image')
            coverKey = upc.key
          }
        }
        const payload = {
          title: this.title.trim(),
          text: this.text.trim(),
          images: images.length ? images : undefined,
          videoKey: videoKey || undefined,
          coverKey: coverKey || undefined,
          duration: duration || undefined,
          regionCode: REGIONS[this.regionIndex].code,
          tags: this.tags.length ? this.tags : undefined,
          poiName: this.poiName.trim() || undefined
        }
        if (this.editId) {
          await request({ url: `/content/${this.editId}`, method: 'PUT', data: payload })
        } else {
          await request({ url: '/content', method: 'POST', data: payload })
        }
        uni.showToast({ title: this.editId ? '已保存，审核通过后公开' : '已提交，审核通过后公开', icon: 'success' })
        setTimeout(() => {
          this.reset()
          uni.switchTab({ url: '/pages/feed/feed' })
        }, 900)
      } catch (e) {
        // 上传/提交失败，toast 已提示
      } finally {
        this.submitting = false
      }
    },
    reset() {
      this.title = ''
      this.text = ''
      this.localImages = []
      this.localVideo = null
      this.regionIndex = -1
      this.poiName = ''
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #ffffff;
}
.topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
  background: #ffffff;
  display: flex;
  align-items: center;
  height: 54px;
  padding: 0 24rpx;
  box-sizing: content-box;
  border-bottom: 1rpx solid #f1f3f5;
}
.nav-cancel {
  font-size: 28rpx;
  color: #666666;
}
.nav-title {
  flex: 1;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
  color: #1f2430;
}
.nav-publish {
  background: #ff2442;
  border-radius: 30rpx;
  padding: 10rpx 34rpx;
}
.nav-publish.dim {
  opacity: 0.5;
}
.nav-publish-text {
  color: #ffffff;
  font-size: 26rpx;
}
.guide {
  margin-top: 120rpx;
  padding: 0 60rpx;
  text-align: center;
}
.guide-logo {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: #ff2442;
  margin: 0 auto 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.guide-logo-text {
  color: #ffffff;
  font-size: 70rpx;
}
.guide-title {
  display: block;
  font-size: 34rpx;
  font-weight: 500;
  margin-bottom: 24rpx;
}
.guide-text {
  display: block;
  font-size: 26rpx;
  color: #999999;
  line-height: 1.7;
  margin-bottom: 40rpx;
  text-align: left;
}
.step {
  display: flex;
  align-items: center;
  background: #f6f7f9;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 16rpx;
}
.step-num {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: #ff2442;
  color: #ffffff;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}
.step-text {
  font-size: 26rpx;
  color: #1f2430;
}
.contact-btn {
  margin-top: 30rpx;
  height: 88rpx;
  line-height: 88rpx;
}
.edit-tip {
  background: #fff7e8;
  border-radius: 12rpx;
  padding: 14rpx 22rpx;
  margin-bottom: 10rpx;
}
.edit-tip-text {
  font-size: 22rpx;
  color: #b8860b;
}
.divider {
  height: 1rpx;
  background: #f1f3f5;
  margin: 6rpx 0;
}

.input {
  background: #ffffff;
  padding: 26rpx 4rpx;
  font-size: 30rpx;
  border-radius: 0;
  margin-bottom: 0;
}
.title-input {
  font-size: 36rpx;
  font-weight: 500;
}
.type-row {
  display: flex;
  align-items: center;
  padding: 22rpx 4rpx;
}
.type-tab {
  font-size: 28rpx;
  color: #999999;
}
.type-tab.active {
  color: #ff2442;
  font-weight: 500;
}
.type-sep {
  font-size: 24rpx;
  color: #e5e7eb;
  margin: 0 20rpx;
}
.textarea {
  background: #ffffff;
  padding: 24rpx 4rpx;
  font-size: 30rpx;
  width: auto;
  height: 260rpx;
}
.ph {
  color: #b9c0c9;
}
.count-wrap {
  position: relative;
}
.count-input {
  padding-right: 90rpx;
}
.count-text {
  position: absolute;
  right: 4rpx;
  top: 30rpx;
  font-size: 22rpx;
  color: #c2c8d0;
}
.count-textarea {
  top: auto;
  bottom: 20rpx;
}
.grid {
  display: flex;
  flex-wrap: wrap;
  padding: 24rpx 0 10rpx;
}
.grid-top {
  padding-top: 6rpx;
}
.grid-item {
  width: 200rpx;
  height: 200rpx;
  border-radius: 12rpx;
  background: #f6f7f9;
  margin: 0 16rpx 16rpx 0;
  position: relative;
  overflow: hidden;
}
.grid-img {
  width: 100%;
  height: 100%;
}
.grid-del {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}
.grid-del-text {
  color: #ffffff;
  font-size: 28rpx;
  line-height: 40rpx;
}
.grid-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}
.grid-mask-text {
  color: #ffffff;
  font-size: 22rpx;
}
.add {
  border: 2rpx dashed #e5e7eb;
  background: #fafbfc;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.add-plus {
  color: #c2c8d0;
  font-size: 48rpx;
  line-height: 1;
}
.add-count {
  font-size: 20rpx;
  color: #c2c8d0;
  margin-top: 8rpx;
}
.video-item {
  display: flex;
  align-items: center;
  justify-content: center;
}
.video-name {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 24rpx;
  color: #5f5e5a;
  padding: 0 50rpx;
  text-align: center;
}
.line-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 4rpx;
}
.row-left {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}
.ficon-img {
  width: 40rpx;
  height: 40rpx;
  margin-right: 22rpx;
  flex-shrink: 0;
}
.poi-row-wrap {
  display: flex;
  align-items: center;
  padding: 10rpx 4rpx;
}
.line-topic {
  font-size: 28rpx;
  color: #999999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 560rpx;
}
.line-topic.picked {
  color: #ff2442;
}
.arrow {
  color: #b9c0c9;
}
.poi-input {
  padding-left: 4rpx;
  padding-top: 0;
  padding-bottom: 24rpx;
}
.topic-chips {
  display: flex;
  flex-wrap: wrap;
  padding: 8rpx 0 20rpx;
}
.tchip {
  background: #f6f7f9;
  border-radius: 999rpx;
  padding: 12rpx 28rpx;
  margin: 0 16rpx 16rpx 0;
  border: 1.5rpx solid #f6f7f9;
}
.tchip.on {
  background: #ffe8ea;
  border-color: #ff2442;
}
.tchip-text {
  font-size: 24rpx;
  color: #6b7280;
}
.tchip-text.on {
  color: #ff2442;
  font-weight: 500;
}
</style>
