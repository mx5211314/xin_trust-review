<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <!-- 自定义导航：取消 | 发笔记 | 发布（非点评人不显示发布/存草稿） -->
    <view class="topbar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <text class="nav-cancel" @tap="goBack">取消</text>
      <text class="nav-title">{{ editId ? '编辑笔记' : '发笔记' }}</text>
      <view v-if="canPublish && !editId" class="nav-draft" @tap="saveDraftToList">
        <text class="nav-draft-text">存草稿</text>
      </view>
      <view v-if="canPublish" class="nav-publish" :class="{ dim: submitting }" @tap="submit">
        <text class="nav-publish-text">{{ submitting ? '…' : '发布' }}</text>
      </view>
    </view>
    <view :style="{ height: statusBarHeight + 54 + 'px' }"></view>

    <!-- 草稿箱入口条（新建模式、有草稿时） -->
    <view v-if="!editId && draftCount > 0" class="draft-banner" @tap="goDrafts">
      <text class="draft-banner-text">草稿箱有 {{ draftCount }} 条未发布，点击继续</text>
      <text class="draft-banner-arrow">›</text>
    </view>

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

      <!-- 3.5 @提及：点选插入 @某人（后端按昵称解析并通知） -->
      <view class="mention-row" @tap="openMention">
        <image class="mention-ic" src="/static/icons/at.png" mode="aspectFit" />
        <text class="mention-tx">@ 提及好友（选人插入昵称）</text>
      </view>

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

      <!-- 5. 类型切换（分段钮，原型05） -->
      <view class="type-row">
        <view class="type-tab" :class="{ active: type === 'image' }" @tap="type = 'image'">
          <text class="type-tab-tx">图文</text>
        </view>
        <view class="type-tab" :class="{ active: type === 'video' }" @tap="type = 'video'">
          <text class="type-tab-tx">视频</text>
        </view>
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
        <!-- 自定义话题：不限于预设标签 -->
        <view class="tchip tchip-new" @tap="addCustomTag">
          <text class="tchip-text tchip-new-tx">＋ 自定义</text>
        </view>
      </view>
      <view class="divider"></view>

      <!-- 7. 地区：精确定位（区县级，自动弹附近店铺）或手动选省/市 -->
      <view class="line-row" @tap="onRegionTap">
        <view class="row-left">
          <image class="ficon-img" src="/static/icons/location.png" />
          <text class="line-topic" :class="{ picked: regionLabel }">
            {{ regionLabel || '添加地区' }}
          </text>
        </view>
        <text class="arrow">▾</text>
      </view>
      <city-sheet
        :show="citySheetShow"
        :current="regionCode"
        @select="onCityPick"
        @pick-province="onProvincePick"
        @locate="onLocated"
        @close="citySheetShow = false"
      />

      <!-- 附近店铺弹层：定位后按距离列出真实 POI，点选自动填店铺名 -->
      <view v-if="poiSheetShow" class="mask" @tap="poiSheetShow = false">
        <view class="sheet" @tap.stop>
          <view class="sheet-head">
            <text class="sheet-title">附近的店铺</text>
            <text class="sheet-close" @tap="poiSheetShow = false">×</text>
          </view>
          <view v-for="p in nearPois" :key="p.title" class="poi-item" @tap="pickPoi(p)">
            <text class="poi-item-title">{{ p.title }}</text>
            <text class="poi-item-addr">{{ p.address }}</text>
          </view>
          <view v-if="!nearPois.length" class="poi-empty">
            <text class="muted">附近没搜到店铺，请手动填写</text>
          </view>
        </view>
      </view>

      <!-- @提及用户搜索弹层 -->
      <view v-if="mentionShow" class="mask" @tap="mentionShow = false">
        <view class="sheet" @tap.stop>
          <view class="sheet-head">
            <text class="sheet-title">@ 提及谁</text>
            <text class="sheet-close" @tap="mentionShow = false">×</text>
          </view>
          <view class="mention-search">
            <input
              v-model="mentionKw"
              class="mention-input"
              placeholder="输入昵称搜索"
              placeholder-class="ph"
              @input="searchMention"
            />
          </view>
          <scroll-view scroll-y class="mention-list">
            <view v-for="u in mentionUsers" :key="u.userId" class="mention-item" @tap="pickMention(u)">
              <image v-if="u.avatar" class="mention-avatar" :src="u.avatar" mode="aspectFill" />
              <view v-else class="mention-avatar ph"><text class="mention-avatar-text">{{ (u.nickname || '?').slice(0, 1) }}</text></view>
              <text class="mention-name">{{ u.nickname }}</text>
            </view>
            <view v-if="mentionKw && !mentionUsers.length" class="mention-empty">
              <text class="muted">没搜到用户</text>
            </view>
          </scroll-view>
        </view>
      </view>
      <view class="poi-row-wrap">
        <image class="ficon-img" src="/static/icons/shop.png" />
        <input
          v-model="poiName"
          class="input poi-input"
          :class="{ filled: poiName }"
          placeholder="店铺名（选填，可点右侧定位选店）"
          placeholder-class="ph"
        />
        <view class="poi-locate" @tap="openNearPois">
          <image class="poi-locate-ic" src="/static/icons/location.png" mode="aspectFit" />
          <text class="poi-locate-tx">附近</text>
        </view>
      </view>
      <view style="height: 60rpx"></view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { uploadFile } from '@/utils/upload'
import { REGIONS, regionName } from '@/utils/config'
import { getUser, setUserInfo, isLogin } from '@/utils/auth'
import { askSubscribeOnce } from '@/utils/subscribe'
import { locateDetail, nearbyPois } from '@/utils/location'
import CitySheet from '@/components/city-sheet/city-sheet.vue'

export default {
  components: { CitySheet },
  data() {
    return {
      role: 'USER',
      type: 'image',
      title: '',
      text: '',
      localImages: [],
      localVideo: null,
      // ---- 编辑模式下的「原媒体」----
      // 注意：详情接口只下发 URL、不下发存储 key（后端有意不暴露）。
      // 所以这里存的是 URL，仅用于判断笔记类型和给用户提示；
      // 提交时不重选媒体就不传对应字段，后端 updateContent 会保留原值。
      origImages: [],
      origVideoUrl: '',
      origDuration: 0,
      regionIndex: -1,
      citySheetShow: false,
      located: null, // 精确定位结果 { code(区县adcode), label, lat, lng }，设置后优先于手动选
      poiSheetShow: false,
      nearPois: [],
      regionNames: REGIONS.map(r => r.name),
      allTags: ['唐山美食', '探店', '咖啡', '遛娃', '拍照', '老店'],
      tags: [],
      topicOpen: false,
      poiName: '',
      submitting: false,
      editId: '',
      statusBarHeight: 20,
      draftId: null,
      draftCount: 0,
      mentionShow: false,
      mentionKw: '',
      mentionUsers: []
    }
  },
  onLoad(query) {
    try {
      this.statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 20
    } catch (e) { /* 默认值兜底 */ }
    // 合并本地自定义话题（上次自定义过的话题进候选，持久可用）
    try {
      const saved = uni.getStorageSync('dp_custom_tags') || []
      for (const t of saved) if (!this.allTags.includes(t)) this.allTags.push(t)
    } catch (e) { /* ignore */ }
    if (query && query.id) {
      this.editId = query.id
      this.loadForEdit()
    } else if (query && query.draftId) {
      this.loadDraft(Number(query.draftId))
    }
    // 新会话的 draftId 在 onShow 中分配（tab 页 onLoad 仅首次触发）
  },
  onHide() {
    // 离开页面自动存草稿（仅新建模式、且有内容时）
    if (!this.editId) this.saveDraft()
  },
  onShow() {
    const u = getUser()
    this.role = u ? u.role : 'USER'
    // 角色可能刚被后台改过（授予/取消点评人）→ 拉一次最新信息并回写登录态，
    // 否则用户必须退出重登才能发布（原先就是这个问题）
    this.refreshRole()
    // 「我的」长按编辑跳转：发布页是 tabBar 页，不能 navigateTo 带参
    //（会静默失败、停在原页面），所以改用 storage 标记传递
    let editTarget = null
    try {
      editTarget = uni.getStorageSync('dp_edit_content')
      if (editTarget) uni.removeStorageSync('dp_edit_content')
    } catch (e) { /* ignore */ }
    if (editTarget && String(editTarget) !== String(this.editId)) {
      this.editId = editTarget
      this.loadForEdit()
      return
    }
    if (!this.editId) {
      // 草稿箱「继续编辑」跳转（tab 页不能 navigateTo 带参 → storage 标记）
      let fromDraft = null
      try {
        fromDraft = uni.getStorageSync('dp_edit_draft')
        if (fromDraft) uni.removeStorageSync('dp_edit_draft')
      } catch (e) { /* ignore */ }
      if (fromDraft) {
        this.loadDraft(Number(fromDraft))
        return
      }
      // 本会话草稿 id（离开时自动 upsert 进草稿箱）
      if (!this.draftId) this.draftId = Date.now()
      try {
        this.draftCount = (uni.getStorageSync('dp_drafts') || []).length
      } catch (e) { this.draftCount = 0 }
    }
  },
  computed: {
    /** 只有点评人/管理员能发布；非点评人连顶部按钮都不该看到 */
    canPublish() {
      return this.role === 'REVIEWER' || this.role === 'ADMIN'
    },
    /** 提交用地区码：精确定位(区县adcode) > 手动选市/省 */
    regionCode() {
      if (this.located) return this.located.code
      return this.regionIndex >= 0 ? REGIONS[this.regionIndex].code : ''
    },
    /** 地区行显示文本 */
    regionLabel() {
      if (this.located) return this.located.label
      return this.regionIndex >= 0 ? this.regionNames[this.regionIndex] : ''
    }
  },
  methods: {
    /** 自定义话题：弹输入框（可输入任意话题），加入候选并选中，持久化到本地话题池 */
    addCustomTag() {
      uni.showModal({
        title: '自定义话题',
        editable: true,
        placeholderText: '输入话题名（不带#，最多12字）',
        success: (res) => {
          if (!res.confirm) return
          const t = (res.content || '').trim().replace(/^#+/, '').slice(0, 12)
          if (!t) return
          if (!this.allTags.includes(t)) {
            this.allTags.push(t)
            try {
              const saved = uni.getStorageSync('dp_custom_tags') || []
              if (!saved.includes(t)) uni.setStorageSync('dp_custom_tags', saved.concat(t))
            } catch (e) { /* 存储失败不影响本次使用 */ }
          }
          if (!this.tags.includes(t) && this.tags.length < 5) this.tags.push(t)
        }
      })
    },
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
    /**
     * 编辑模式：拉详情回填文本字段（编辑不支持改图/视频，见 edit-tip 提示）。
     * 注意：onLoad 调用了此方法但此前从未定义——编辑功能一直处于损坏状态，本次补齐。
     */
    async loadForEdit() {
      // 先清空上一次的编辑残留再加载。
      // 发布页是 tabBar 页、状态会保留：之前"先写新笔记并选好图 → 再去编辑另一篇"时，
      // localImages 里的残留图会被 submit 一起提交到**被编辑的那篇笔记**上，
      // 造成内容被污染（而且用户看不出来）。
      this.localImages = []
      this.localVideo = null
      this.type = 'image'
      this.tags = []
      this.origImages = []
      this.origVideoUrl = ''
      this.origDuration = 0
      try {
        const c = await request({ url: `/content/${this.editId}` })
        this.title = c.title || ''
        this.text = c.text || ''
        this.tags = Array.isArray(c.tags) ? c.tags : []
        if (c.poiName) this.poiName = c.poiName
        // 记录原媒体（只有 URL，见 data 里的说明），并判定笔记类型。
        // 视频判定同时看 videoUrl 和 duration：videoUrl 是后端拼的公开地址，
        // OSS 配置缺失时会是空串，只凭它会漏判。
        this.origImages = Array.isArray(c.images) ? c.images.slice() : []
        this.origVideoUrl = c.videoUrl || ''
        this.origDuration = c.duration || 0
        if (this.origVideoUrl || this.origDuration > 0) this.type = 'video'
        if (c.regionCode) {
          // 市级码直接匹配手动选择；区县级 adcode（定位发布）回显为 located
          const idx = REGIONS.findIndex(r => r.code === c.regionCode)
          if (idx >= 0) {
            this.regionIndex = idx
            this.located = null
          } else {
            const cityName = regionName(c.regionCode.slice(0, 4) + '00')
            this.located = { code: c.regionCode, label: cityName || c.regionCode, lat: 0, lng: 0 }
            this.regionIndex = -1
          }
        }
      } catch (e) { /* request 已统一 toast */ }
    },
    /** 地区行点击：优先精确定位（区县级+自动弹附近店铺），或打开地区选择弹层 */
    onRegionTap() {
      uni.showActionSheet({
        itemList: ['定位当前位置（精确）', '手动选择地区'],
        success: (res) => {
          if (res.tapIndex === 0) {
            uni.showLoading({ title: '定位中…' })
            locateDetail()
              .then((loc) => {
                uni.hideLoading()
                this.applyLocated(loc)
              })
              .catch((e) => {
                uni.hideLoading()
                uni.showToast({ title: e.message || '定位失败，请手动选择', icon: 'none', duration: 2200 })
              })
          } else {
            this.citySheetShow = true
          }
        }
      })
    },
    /**
     * 应用精确定位：地区填到区县（adcode），并自动弹出附近店铺列表供点选填店铺名。
     * 搜索不到店铺时静默，店铺名手动填。
     */
    applyLocated(loc) {
      const label = loc.districtName && loc.districtName !== loc.cityName
        ? loc.cityName + '·' + loc.districtName
        : loc.cityName
      this.located = { code: loc.adcode || '', label: label, lat: loc.lat, lng: loc.lng }
      this.regionIndex = -1
      uni.showToast({ title: '已定位 ' + label, icon: 'none', duration: 1000 })
      this.openNearPois()
    },
    /** 附近店铺：定位坐标 → 腾讯 place API 按距离搜"美食"类 POI → 弹层点选 */
    openNearPois() {
      if (!this.located || !this.located.lat) {
        uni.showToast({ title: '请先定位，再选择附近店铺', icon: 'none' })
        return
      }
      uni.showLoading({ title: '搜索附近店铺…' })
      nearbyPois(this.located.lat, this.located.lng, 1000, '美食')
        .then((pois) => {
          uni.hideLoading()
          this.nearPois = pois
          this.poiSheetShow = true
        })
        .catch((e) => {
          uni.hideLoading()
          uni.showToast({ title: e.message || '附近店铺搜索失败', icon: 'none', duration: 2000 })
        })
    },
    pickPoi(p) {
      this.poiName = p.title
      this.poiSheetShow = false
      // POI 的 adcode 比定位点更精确，顺手校准地区码
      if (p.adcode && this.located) this.located.code = p.adcode
    },
    onCityPick(city) {
      this.citySheetShow = false
      this.located = null
      this.regionIndex = REGIONS.findIndex(r => r.code === city.code)
    },
    onProvincePick(prov) {
      this.citySheetShow = false
      // 选省：提交省级码，同城/地区过滤走前缀匹配（可召回全省内容）
      this.located = { code: prov.code, label: prov.name + '（全省）', lat: 0, lng: 0 }
      this.regionIndex = -1
    },
    onLocated(loc) {
      this.citySheetShow = false
      this.applyLocated(loc)
    },
    copyAdminPhone() {
      uni.setClipboardData({
        data: '13800000000',
        success: () => uni.showToast({ title: '已复制管理员手机号', icon: 'none' })
      })
    },
    goBack() {
      // 编辑态下"取消"要**显式退出编辑**：否则回到发布页时 editId 还在，
      // 再点发布就会把刚取消的那篇旧笔记又改一遍。
      // 新建态不清 —— 交给 onHide 自动存草稿，现在清了会把草稿一起丢掉。
      if (this.editId) this.reset()
      uni.switchTab({ url: '/pages/feed/feed' })
    },
    /** 拉最新用户信息：刷新角色 + 回写登录态
     *  后台刚授予/取消点评人时，本地缓存的 role 是旧的 —— 不回写就必须退出重登 */
    async refreshRole() {
      if (!isLogin()) return
      try {
        const me = await request({ url: '/user/me' })
        setUserInfo(Object.assign({}, getUser(), me))
        if (me && me.role) this.role = me.role
      } catch (e) { /* 静默：网络异常时沿用缓存角色 */ }
    },
    validate() {
      if (!this.title.trim()) return '标题不能为空'
      if (!this.text.trim()) return '正文不能为空'
      if (this.regionIndex < 0 && !this.located) return '请选择地区'
      if (!this.editId && this.type === 'image' && this.localImages.length === 0) return '至少选一张图片'
      if (!this.editId && this.type === 'video' && !this.localVideo) return '请选择视频'
      return ''
    },
    async submit() {
      // 非点评人：先给明确原因，别让 validate() 报「标题不能为空」误导用户
      if (!this.canPublish) {
        return uni.showToast({ title: '发布功能仅对点评人开放', icon: 'none' })
      }
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
        // 只有「新选了视频」才上传。
        // 编辑既有笔记时 localVideo 为 null，原来这里是 if (this.type === 'video')，
        // 直接访问 this.localVideo.uploading 会抛 TypeError（点发布没反应）。
        // 不传 videoKey/coverKey/duration 时后端会保留原值
        // （ContentService.updateContent 不覆盖这三个字段）。
        if (this.type === 'video' && this.localVideo && this.localVideo.path) {
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
          regionCode: this.regionCode,
          tags: this.tags.length ? this.tags : undefined,
          poiName: this.poiName.trim() || undefined
        }
        let res = null
        if (this.editId) {
          res = await request({ url: `/content/${this.editId}`, method: 'PUT', data: payload })
        } else {
          res = await request({ url: '/content', method: 'POST', data: payload })
          // 发布成功 → 引导订阅"审核结果通知"（未配置模板时静默跳过；tap 手势内合法）
          askSubscribeOnce('audit')
        }
        // 机审放行 → APPROVED；否则停 PENDING 等后台人工审。提示必须跟真实状态一致，
        // 不能像之前那样无条件说「审核通过后公开」（实际是秒过，别人立刻就看到了）
        const approved = res && res.status === 'APPROVED'
        const tip = approved
          ? (this.editId ? '已保存' : '已发布')
          : '已提交，审核通过后公开'
        uni.showToast({ title: tip, icon: 'success' })
        setTimeout(() => {
          this.reset()
          this.removeDraft()
          uni.switchTab({ url: '/pages/feed/feed' })
        }, 900)
      } catch (e) {
        // 上传/提交失败，toast 已提示
      } finally {
        this.submitting = false
      }
    },
    hasContent() {
      return !!(this.title.trim() || this.text.trim() || this.localImages.length || this.localVideo || this.tags.length || this.poiName.trim())
    },
    /** 离开页面自动存草稿：upsert 进 dp_drafts 列表（多草稿） */
    saveDraft() {
      if (this.editId || !this.hasContent() || !this.draftId) return
      this.upsertDraft(this.draftId)
    },
    /** 顶部「存草稿」按钮：立即存入并提示 */
    saveDraftToList() {
      if (this.editId) return
      if (!this.hasContent()) return uni.showToast({ title: '还没有内容可存', icon: 'none' })
      if (!this.draftId) this.draftId = Date.now()
      this.upsertDraft(this.draftId)
      try { this.draftCount = (uni.getStorageSync('dp_drafts') || []).length } catch (e) { /* ignore */ }
      uni.showToast({ title: '已存入草稿箱', icon: 'success' })
    },
    /** 写入/更新一条草稿（key=id，新草稿置顶） */
    upsertDraft(id) {
      const entry = {
        id,
        title: this.title,
        text: this.text,
        regionIndex: this.regionIndex,
        tags: this.tags,
        poiName: this.poiName,
        type: this.type,
        savedAt: this.fmtNow()
      }
      let list = []
      try { list = uni.getStorageSync('dp_drafts') || [] } catch (e) { list = [] }
      const i = list.findIndex(d => d.id === id)
      if (i >= 0) list[i] = entry
      else list.unshift(entry)
      try { uni.setStorageSync('dp_drafts', list) } catch (e) { /* ignore */ }
    },
    /** 从草稿箱载入（图片/视频不存，需重选） */
    loadDraft(id) {
      let list = []
      try { list = uni.getStorageSync('dp_drafts') || [] } catch (e) { list = [] }
      const d = list.find(x => x.id === id)
      if (!d) { this.draftId = Date.now(); return }
      this.draftId = id
      this.title = d.title || ''
      this.text = d.text || ''
      this.regionIndex = typeof d.regionIndex === 'number' ? d.regionIndex : -1
      this.tags = Array.isArray(d.tags) ? d.tags : []
      this.poiName = d.poiName || ''
      if (d.type === 'video' || d.type === 'image') this.type = d.type
    },
    /** 发布/放弃后移除该草稿 */
    removeDraft() {
      if (!this.draftId) return
      let list = []
      try { list = uni.getStorageSync('dp_drafts') || [] } catch (e) { list = [] }
      const next = list.filter(d => d.id !== this.draftId)
      try { uni.setStorageSync('dp_drafts', next) } catch (e) { /* ignore */ }
      this.draftId = null
    },
    goDrafts() {
      uni.navigateTo({ url: '/pages/draftbox/draftbox' })
    },
    /** @提及：打开选人弹层 */
    openMention() {
      this.mentionKw = ''
      this.mentionUsers = []
      this.mentionShow = true
    },
    /** 昵称搜索（复用 /user/search） */
    async searchMention() {
      const kw = (this.mentionKw || '').trim()
      if (!kw) { this.mentionUsers = []; return }
      try {
        this.mentionUsers = (await request({
          url: `/user/search?keyword=${encodeURIComponent(kw)}&limit=20`,
          silent: true
        })) || []
      } catch (e) {
        this.mentionUsers = []
      }
    },
    /** 选中：在正文末尾插入 @昵称 + 空格 */
    pickMention(u) {
      const name = u.nickname || ''
      if (!name) return
      this.text = (this.text || '') + '@' + name + ' '
      this.mentionShow = false
    },
    fmtNow() {
      const p = (n) => (n < 10 ? '0' + n : '' + n)
      const d = new Date()
      return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
    },
    /**
     * 回到"全新发布"状态。
     *
     * **必须把编辑态一起清掉**：发布页是 tabBar 页、页面状态会一直留着，
     * 只清内容不清 editId 的话，下次点"发布"仍会带着 editId 走 PUT，
     * 把上一次编辑的旧笔记又改一遍 —— 这是会**误改用户数据**的问题，不只是显示不对。
     */
    reset() {
      this.title = ''
      this.text = ''
      this.tags = []
      this.type = 'image'
      this.localImages = []
      this.localVideo = null
      this.regionIndex = -1
      this.located = null
      this.poiName = ''
      this.nearPois = []
      // 编辑态 + 原媒体记录
      this.editId = ''
      this.origImages = []
      this.origVideoUrl = ''
      this.origDuration = 0
      // 开一个新的草稿会话
      this.draftId = Date.now()
      try {
        this.draftCount = (uni.getStorageSync('dp_drafts') || []).length
      } catch (e) { this.draftCount = 0 }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--dp-card);
}
.topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
  background: var(--dp-card);
  display: flex;
  align-items: center;
  height: 54px;
  padding: 0 24rpx;
  box-sizing: content-box;
  border-bottom: 1rpx solid var(--dp-line);
}
.nav-cancel {
  font-size: 28rpx;
  color: var(--dp-text2);
}
.nav-title {
  flex: 1;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
  color: var(--dp-text);
}
.nav-publish {
  background: var(--dp-brand-deep);
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
.nav-draft {
  padding: 10rpx 20rpx;
}
.nav-draft-text {
  color: var(--dp-text2);
  font-size: 26rpx;
}
.draft-banner {
  margin: 16rpx 24rpx 0;
  background: var(--dp-soft);
  border-radius: 14rpx;
  padding: 22rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.draft-banner-text {
  font-size: 26rpx;
  color: var(--dp-text2);
}
.draft-banner-arrow {
  font-size: 30rpx;
  color: var(--dp-text4);
}
.mention-row {
  display: flex;
  align-items: center;
  padding: 22rpx 4rpx 6rpx;
  margin: 0 24rpx;
}
.mention-ic {
  width: 32rpx;
  height: 32rpx;
  margin-right: 14rpx;
}
.mention-tx {
  font-size: 26rpx;
  color: var(--dp-brand-deep);
}
.mention-search {
  padding: 16rpx 24rpx;
}
.mention-input {
  background: var(--dp-soft);
  border-radius: 14rpx;
  padding: 18rpx 22rpx;
  font-size: 28rpx;
}
.mention-list {
  /* scroll-view 在 H5 里需要「确定高度」才能滚动；只给 max-height 会算不出高度，
     表现为列表显示不全 / 滚不动。这里给死高度，外层 .sheet 的 overflow 兜底。 */
  height: 520rpx;
}
.mention-item {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid var(--dp-soft);
}
.mention-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  margin-right: 18rpx;
  flex-shrink: 0;
}
.mention-avatar.ph {
  background: linear-gradient(135deg, var(--dp-orange), var(--dp-brand));
  display: flex;
  align-items: center;
  justify-content: center;
}
.mention-avatar-text {
  font-size: 28rpx;
  color: #ffffff;
}
.mention-name {
  font-size: 28rpx;
  color: var(--dp-text);
}
.mention-empty {
  text-align: center;
  padding: 40rpx 0;
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
  background: var(--dp-brand-deep);
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
  color: var(--dp-text3);
  line-height: 1.7;
  margin-bottom: 40rpx;
  text-align: left;
}
.step {
  display: flex;
  align-items: center;
  background: var(--dp-soft);
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 16rpx;
}
.step-num {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: var(--dp-brand-deep);
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
  color: var(--dp-text);
}
.contact-btn {
  margin-top: 30rpx;
  height: 88rpx;
  line-height: 88rpx;
}
.edit-tip {
  background: var(--dp-warn-soft);
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
  background: var(--dp-line);
  margin: 6rpx 0;
}

.input {
  background: var(--dp-card);
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
  gap: 18rpx;
  padding: 22rpx 4rpx;
}
.type-tab {
  flex: 1;
  text-align: center;
  border: 1rpx solid var(--dp-line);
  border-radius: 12rpx;
  padding: 14rpx 0;
}
.type-tab-tx {
  font-size: 26rpx;
  color: var(--dp-text3);
}
.type-tab.active {
  border-color: var(--dp-brand-deep);
  background: var(--dp-accent-soft);
}
.type-tab.active .type-tab-tx {
  color: var(--dp-brand-deep);
  font-weight: 600;
}
.textarea {
  background: var(--dp-card);
  padding: 24rpx 4rpx;
  font-size: 30rpx;
  width: auto;
  height: 260rpx;
}
.ph {
  color: var(--dp-text4);
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
  color: var(--dp-text4);
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
  background: var(--dp-soft);
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
  color: var(--dp-text4);
  font-size: 48rpx;
  line-height: 1;
}
.add-count {
  font-size: 20rpx;
  color: var(--dp-text4);
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
  color: var(--dp-text3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 560rpx;
}
.line-topic.picked {
  color: var(--dp-brand-deep);
}
.arrow {
  color: var(--dp-text4);
}
.poi-input {
  padding-left: 4rpx;
  padding-top: 0;
  padding-bottom: 24rpx;
}
/* 已填店铺名红色显示（原型05：已选 POI 强调） */
.poi-input.filled {
  color: var(--dp-brand-deep);
  font-weight: 500;
}
/* "附近"入口：定位选附近店铺 */
.poi-locate {
  display: flex;
  align-items: center;
  gap: 4rpx;
  flex-shrink: 0;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: var(--dp-accent-soft);
}
.poi-locate-ic {
  width: 24rpx;
  height: 24rpx;
}
.poi-locate-tx {
  font-size: 22rpx;
  color: var(--dp-brand-deep);
  font-weight: 500;
}
/* 自定义话题 chip（虚线） */
.tchip-new {
  border: 1.5rpx dashed var(--dp-text4);
  background: transparent;
}
.tchip-new-tx {
  color: var(--dp-text3);
}
/* 附近店铺 / @提及 弹层
   注意 z-index 必须高于 H5 的 tabBar(998)：本页是 tabBar 页，
   底栏常驻，弹层若低于它，面板底部（含按钮）会被压住、看着"显示不全" */
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 1000;
}
.sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1001;
  background: var(--dp-card);
  border-radius: 28rpx 28rpx 0 0;
  padding: 30rpx 28rpx calc(24rpx + env(safe-area-inset-bottom));
  /* 72vh：@提及 面板要装下 头部+搜索框+520rpx 列表，60vh 在矮屏上会把列表挤掉 */
  max-height: 72vh;
  overflow-y: auto;
}
.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10rpx;
}
.sheet-title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--dp-text);
}
.sheet-close {
  font-size: 44rpx;
  line-height: 44rpx;
  color: var(--dp-text4);
  padding: 0 10rpx;
}
.poi-item {
  display: flex;
  flex-direction: column;
  padding: 20rpx 4rpx;
  border-bottom: 1rpx solid var(--dp-soft);
}
.poi-item-title {
  font-size: 27rpx;
  font-weight: 600;
  color: var(--dp-text);
}
.poi-item-addr {
  font-size: 21rpx;
  color: var(--dp-text4);
  margin-top: 4rpx;
}
.poi-empty {
  padding: 50rpx 0;
  text-align: center;
}
.topic-chips {
  display: flex;
  flex-wrap: wrap;
  padding: 8rpx 0 20rpx;
}
.tchip {
  background: var(--dp-soft);
  border-radius: 999rpx;
  padding: 12rpx 28rpx;
  margin: 0 16rpx 16rpx 0;
  border: 1.5rpx solid var(--dp-soft);
}
.tchip.on {
  background: var(--dp-accent-soft);
  border-color: var(--dp-brand-deep);
}
.tchip-text {
  font-size: 24rpx;
  color: #6b7280;
}
.tchip-text.on {
  color: var(--dp-brand-deep);
  font-weight: 500;
}
</style>
