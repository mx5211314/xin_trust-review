<template>
  <view class="page" :class="{'theme-dark': isDark}">
    <!-- 自定义导航：发现/同城 + 城市定位 + 显性搜索框 -->
    <view class="topbar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="seg">
        <text class="seg-item" :class="{ on: tab === 'follow' }" @tap="switchTab('follow')">关注</text>
        <text class="seg-item" :class="{ on: tab === 'rec' }" @tap="switchTab('rec')">推荐</text>
        <text class="seg-item" :class="{ on: tab === 'find' }" @tap="switchTab('find')">发现</text>
        <text class="seg-item" :class="{ on: tab === 'city' }" @tap="switchTab('city')">同城</text>
      </view>
      <view class="sbar">
        <view v-if="tab === 'city'" class="sbar-loc" @tap="openCitySheet">
          <image class="sbar-loc-ic" src="/static/icons/location.png" mode="aspectFit" />
          <text class="sbar-loc-tx">{{ cityText }}</text>
          <text class="sbar-loc-arr">▾</text>
        </view>
        <view class="sbar-box" @tap="goSearch">
          <view class="sbar-mag"><view class="sbar-ring"></view><view class="sbar-handle"></view></view>
          <text class="sbar-ph">搜内容 / 话题 / 店铺</text>
        </view>
      </view>
    </view>
    <view :style="{ height: statusBarHeight + 96 + 'px' }"></view>

    <!-- 城市选择弹层（公共组件：定位 + 城市网格） -->
    <city-sheet
      :show="citySheet"
      :current="cityCode"
      @select="onCityPicked"
      @pick-province="onCityPicked"
      @locate="onLocated"
      @close="citySheet = false"
    />

    <!-- 口碑榜常驻卡（首页信任感核心资产） -->
    <view class="rankbar" v-if="rank.length" @tap="goRank">
      <view class="rank-head">
        <text class="rank-title">本地口碑榜</text>
        <text class="rank-week">本周</text>
        <text class="rank-more">完整榜 ›</text>
      </view>
      <view class="rank-item" v-for="(r, i) in rank" :key="r.shop">
        <text class="rank-no" :class="{ top: i === 0 }">{{ i + 1 }}</text>
        <view class="rank-mid">
          <text class="rank-name">{{ r.shop }}</text>
          <text class="rank-sub">{{ r.likeCount }} 赞 · {{ r.contentCount }} 篇</text>
        </view>
      </view>
    </view>

    <!-- 双列瀑布流 -->
    <view class="waterfall" v-if="list.length">
      <view class="col">
        <view v-for="item in leftList" :key="item.contentId" class="wcard" hover-class="card-hover" @tap="goDetail(item)">
          <view class="cover-wrap" :style="{ height: coverH(item) + 'rpx' }">
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
            <view v-if="item.author && (item.author.role === 'REVIEWER' || item.author.role === 'ADMIN')" class="trust-badge">
              <image class="trust-star" src="/static/icons/star-red.png" mode="aspectFit" />
              <text class="trust-text">认证</text>
            </view>
            <view v-if="item.videoUrl" class="video-badge">
              <text class="video-badge-text">视频</text>
            </view>
            <view v-else-if="item.images && item.images.length > 1" class="img-badge">
              <text class="img-badge-text">⧉ {{ item.images.length }}</text>
            </view>
          </view>
          <view class="wbody">
            <text class="wtitle">{{ item.title }}</text>
            <view v-if="item.poiName" class="wpoi">
              <image class="wpoi-ic" src="/static/icons/location.png" mode="aspectFit" />
              <text class="wpoi-tx">{{ item.poiName }}</text>
            </view>
            <view v-if="item.tags && item.tags.length" class="wtag">
              <text class="wtag-tx"># {{ item.tags.join(' # ') }}</text>
            </view>
            <view class="wfoot">
              <view class="wauthor">
                <view class="avatar-ph wavatar">
                  <text class="wavatar-text">{{ shortName(item.author) }}</text>
                </view>
                <text class="wnick">{{ item.author ? item.author.nickname : '匿名' }}</text>
              </view>
              <view class="wlike" @tap.stop="doLike(item)">
                <image class="wlike-img" :src="item.liked ? '/static/icons/heart-on.png' : '/static/icons/heart.png'" />
                <text class="wlike-num">{{ item.likeCount }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
      <view class="col">
        <view v-for="item in rightList" :key="item.contentId" class="wcard" hover-class="card-hover" @tap="goDetail(item)">
          <view class="cover-wrap" :style="{ height: coverH(item) + 'rpx' }">
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
            <view v-if="item.author && (item.author.role === 'REVIEWER' || item.author.role === 'ADMIN')" class="trust-badge">
              <image class="trust-star" src="/static/icons/star-red.png" mode="aspectFit" />
              <text class="trust-text">认证</text>
            </view>
            <view v-if="item.videoUrl" class="video-badge">
              <text class="video-badge-text">视频</text>
            </view>
            <view v-else-if="item.images && item.images.length > 1" class="img-badge">
              <text class="img-badge-text">⧉ {{ item.images.length }}</text>
            </view>
          </view>
          <view class="wbody">
            <text class="wtitle">{{ item.title }}</text>
            <view v-if="item.poiName" class="wpoi">
              <image class="wpoi-ic" src="/static/icons/location.png" mode="aspectFit" />
              <text class="wpoi-tx">{{ item.poiName }}</text>
            </view>
            <view v-if="item.tags && item.tags.length" class="wtag">
              <text class="wtag-tx"># {{ item.tags.join(' # ') }}</text>
            </view>
            <view class="wfoot">
              <view class="wauthor">
                <view class="avatar-ph wavatar">
                  <text class="wavatar-text">{{ shortName(item.author) }}</text>
                </view>
                <text class="wnick">{{ item.author ? item.author.nickname : '匿名' }}</text>
              </view>
              <view class="wlike" @tap.stop="doLike(item)">
                <image class="wlike-img" :src="item.liked ? '/static/icons/heart-on.png' : '/static/icons/heart.png'" />
                <text class="wlike-num">{{ item.likeCount }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 骨架屏 -->
    <view class="waterfall" v-if="loading && !list.length">
      <view class="col">
        <view class="wcard"><view class="skeleton sk-img"></view><view class="wbody"><view class="skeleton sk-line"></view></view></view>
        <view class="wcard"><view class="skeleton sk-img short"></view><view class="wbody"><view class="skeleton sk-line"></view></view></view>
      </view>
      <view class="col">
        <view class="wcard"><view class="skeleton sk-img short"></view><view class="wbody"><view class="skeleton sk-line"></view></view></view>
        <view class="wcard"><view class="skeleton sk-img"></view><view class="wbody"><view class="skeleton sk-line"></view></view></view>
      </view>
    </view>

    <!-- 引导式空状态：说清楚 + 给下一步（原则 09） -->
    <view v-if="!loading && !list.length" class="empty">
      <view class="empty-ic">
        <image class="empty-ic-img" :src="emptyInfo.icon" mode="aspectFit" />
      </view>
      <text class="empty-t">{{ emptyInfo.title }}</text>
      <text class="empty-d">{{ emptyInfo.desc }}</text>
      <view class="empty-btn" @tap="goEmptyAction">{{ emptyInfo.btn }}</view>
    </view>
    <view v-if="loading && list.length" class="load-more">
      <text class="load-more-text">加载中…</text>
    </view>
    <view v-if="list.length && !hasMore" class="load-end">
      <text class="muted">— 到底啦 —</text>
    </view>
    <view style="height: 30rpx"></view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { regionName } from '@/utils/config'
import { isLogin } from '@/utils/auth'
import CitySheet from '@/components/city-sheet/city-sheet.vue'

export default {
  components: { CitySheet },
  data() {
    return {
      tab: 'find',
      cityCode: uni.getStorageSync('dp_city') || '130100',
      cityLabel: '',
      citySheet: false,
      followedIds: [],
      list: [],
      page: 1,
      pageSize: 10,
      total: 0,
      hasMore: true,
      loading: false,
      unread: 0,
      rank: [],
      statusBarHeight: 20
    }
  },
  computed: {
    leftList() {
      return this.list.filter((_, i) => i % 2 === 0)
    },
    rightList() {
      return this.list.filter((_, i) => i % 2 === 1)
    },
    /** 搜索框前的城市名：来自本地选择或定位结果（storage 持久化） */
    cityText() {
      return this.cityLabel || regionName(this.cityCode)
    },
    /** 空状态文案：按 tab 给「说清楚 + 下一步」（原则 09 引导式空状态） */
    emptyInfo() {
      if (this.tab === 'follow') {
        return {
          icon: '/static/icons/star.png',
          title: '这里还没有点评',
          desc: '关注一些人，就能在这里看到他们的探店足迹',
          btn: '去发现'
        }
      }
      if (this.tab === 'city') {
        return {
          icon: '/static/icons/location.png',
          title: `「${this.cityText}」还没有点评`,
          desc: '换个城市，或成为这里的第一位点评人',
          btn: '换个城市'
        }
      }
      return {
        icon: '/static/icons/topic.png',
        title: '这里还没有点评',
        desc: '成为第一位点评人，让更多人看到好店',
        btn: '写第一条点评'
      }
    }
  },
  onLoad() {
    try {
      this.statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 20
    } catch (e) { /* 默认值兜底 */ }
  },
  onShow() {
    if (!isLogin()) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.refresh()
    this.loadUnread()
    this.loadRank()
  },
  onPullDownRefresh() {
    this.refresh().finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    if (this.hasMore && !this.loading) {
      this.page += 1
      this.fetch(false)
    }
  },
  methods: {
    shortName(author) {
      return author && author.nickname ? author.nickname.slice(0, 1) : '客'
    },
    /** 封面高度交替，模拟瀑布流错落 */
    coverH(item) {
      if (item.videoUrl) return 200
      const idx = this.list.indexOf(item)
      return idx % 4 < 2 ? 340 : 260
    },
    switchTab(t) {
      if (this.tab === t) return
      this.tab = t
      this.refresh()
    },
    /** 空状态的引导动作：按当前 tab 给对应的下一步 */
    goEmptyAction() {
      if (this.tab === 'follow') {
        this.switchTab('find')
      } else if (this.tab === 'city') {
        this.openCitySheet()
      } else {
        // 发布是 tabBar 页，必须用 switchTab
        uni.switchTab({ url: '/pages/publish/publish' })
      }
    },
    /** 打开城市选择弹层（点左上角位置区域，同城 tab 专属） */
    openCitySheet() {
      this.citySheet = true
    },
    /** 城市弹层回调（手动选市/选全省）：持久化 → 自动切同城 tab 按前缀过滤 */
    onCityPicked(hit) {
      this.cityCode = hit.code
      this.cityLabel = hit.isProvince ? hit.name : ''
      try { uni.setStorageSync('dp_city', hit.code) } catch (e) { /* 存储失败不影响本次会话 */ }
      this.citySheet = false
      if (this.tab !== 'city') this.tab = 'city'
      this.refresh()
    },
    /** 定位回调：精确到区县——过滤码用 adcode，展示用"城市·区县" */
    onLocated(loc) {
      this.cityCode = loc.adcode || ''
      this.cityLabel = loc.districtName && loc.districtName !== loc.cityName
        ? loc.cityName + '·' + loc.districtName
        : loc.cityName
      try { uni.setStorageSync('dp_city', this.cityCode) } catch (e) { /* ignore */ }
      this.citySheet = false
      if (this.tab !== 'city') this.tab = 'city'
      this.refresh()
    },
    goSearch() {
      uni.navigateTo({ url: '/pages/search/search' })
    },
    loadUnread() {
      Promise.all([
        request({ url: '/user/notify/unread' }).catch(() => ({ unread: 0 })),
        request({ url: '/chat/unread' }).catch(() => ({ unread: 0 }))
      ]).then(([a, b]) => {
        const total = Number(a.unread || 0) + Number(b.unread || 0)
        this.unread = total
        // tabBar "消息"tab 徽标（index=2）
        if (total > 0) {
          uni.setTabBarBadge({ index: 2, text: total > 99 ? '99+' : String(total) })
        } else {
          uni.removeTabBarBadge({ index: 2 })
        }
      })
    },
    goDetail(item) {
      uni.navigateTo({ url: '/pages/detail/detail?id=' + item.contentId })
    },
    /** 口碑榜：已上架按店铺聚合（篇数+总赞），首页常驻 Top3 */
    async loadRank() {
      try {
        this.rank = await request({ url: '/content/rank-shops?limit=3', silent: true }) || []
      } catch (e) {
        this.rank = []
      }
    },
    goRank() {
      uni.navigateTo({ url: '/pages/rank/rank' })
    },
    async refresh() {
      this.page = 1
      this.hasMore = true
      await this.fetch(true)
    },
    async fetch(reset) {
      this.loading = true
      try {
        let data
        if (this.tab === 'follow') {
          data = await request({
            url: `/content/follow-feed?page=${this.page}&pageSize=${this.pageSize}`
          })
        } else if (this.tab === 'rec') {
          data = await request({
            url: `/content/recommend?page=${this.page}&pageSize=${this.pageSize}`
          })
        } else {
          const region = this.tab === 'city' ? this.cityCode : ''
          data = await request({
            url: `/content/feed?page=${this.page}&pageSize=${this.pageSize}` +
              (region ? `&regionCode=${region}` : '')
          })
        }
        this.total = data.total
        this.list = reset ? data.list : this.list.concat(data.list)
        this.hasMore = this.list.length < data.total
      } catch (e) {
        if (reset) this.list = []
      } finally {
        this.loading = false
      }
    },
    async doLike(item) {
      if (item.liked) {
        try {
          await request({ url: `/content/${item.contentId}/like`, method: 'DELETE', silent: true })
          item.liked = false
          item.likeCount = Math.max(0, item.likeCount - 1)
          uni.showToast({ title: '已取消点赞', icon: 'none', duration: 800 })
        } catch (e) { /* ignore */ }
      } else {
        try {
          await request({ url: `/content/${item.contentId}/like`, method: 'POST', silent: true })
          item.liked = true
          item.likeCount += 1
          uni.showToast({ title: '已点赞', icon: 'none', duration: 800 })
        } catch (e) {
          if (e.code === 2003) item.liked = true
        }
      }
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
  padding-bottom: 10rpx;
  box-shadow: var(--sh-card);
}
.seg {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 46px;
}
.seg-item {
  font-size: 30rpx;
  color: var(--dp-text3);
  margin: 0 24rpx;
  position: relative;
}
.seg-item.on {
  color: var(--dp-text);
  font-weight: 500;
}
.seg-item.on::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: -10rpx;
  width: 40rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: var(--dp-brand-deep);
}
/* 显性搜索框：城市定位 + 胶囊搜索框（点击整体跳搜索页） */
.sbar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 4rpx 24rpx 8rpx;
}
.sbar-loc {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.sbar-loc-ic {
  width: 28rpx;
  height: 28rpx;
}
.sbar-loc-tx {
  font-size: 26rpx;
  font-weight: 600;
  color: var(--dp-text);
  margin: 0 4rpx 0 8rpx;
}
.sbar-loc-arr {
  font-size: 18rpx;
  color: var(--dp-text4);
}
.sbar-box {
  flex: 1;
  background: var(--dp-soft);
  border-radius: 999rpx;
  height: 62rpx;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  min-width: 0;
}
.sbar-mag {
  width: 26rpx;
  height: 26rpx;
  position: relative;
  flex-shrink: 0;
}
.sbar-ring {
  width: 18rpx;
  height: 18rpx;
  border: 3rpx solid var(--dp-text4);
  border-radius: 50%;
}
.sbar-handle {
  width: 10rpx;
  height: 3rpx;
  background: var(--dp-text4);
  border-radius: 2rpx;
  transform: rotate(45deg);
  margin-top: -3rpx;
  margin-left: 15rpx;
}
.sbar-ph {
  font-size: 24rpx;
  color: var(--dp-text4);
  margin-left: 12rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.wpoi {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
  min-width: 0;
}
.wpoi-ic {
  width: 22rpx;
  height: 22rpx;
  flex-shrink: 0;
}
.wpoi-tx {
  font-size: 21rpx;
  color: var(--dp-brand-deep);
  margin-left: 6rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.wtag {
  margin-top: 6rpx;
  min-width: 0;
}
.wtag-tx {
  font-size: 20rpx;
  color: var(--dp-text4);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}
.seg-item.on {
  color: var(--dp-text);
  font-weight: 500;
}
.seg-item.on::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: -10rpx;
  width: 40rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: var(--dp-brand-deep);
}
.waterfall {
  display: flex;
  padding: 0 16rpx;
}
.col {
  flex: 1;
}
.col + .col {
  margin-left: 16rpx;
}
.wcard {
  background: var(--dp-card);
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
  box-shadow: var(--sh-card);
}
.cover-wrap {
  position: relative;
  width: 100%;
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
  background: var(--dp-soft);
}
.cover-empty-text {
  font-size: 24rpx;
  color: var(--dp-text4);
}
.video-badge {
  position: absolute;
  top: 12rpx;
  left: 12rpx;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
}
.video-badge-text {
  color: #ffffff;
  font-size: 18rpx;
}
.wbody {
  padding: 14rpx 16rpx 16rpx;
}
.wtitle {
  display: block;
  font-size: 26rpx;
  font-weight: 500;
  color: var(--dp-text);
  line-height: 1.45;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.wfoot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14rpx;
}
.wauthor {
  display: flex;
  align-items: center;
  min-width: 0;
}
.wavatar {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--dp-orange), var(--dp-brand-deep));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.wavatar-text {
  font-size: 20rpx;
  color: #ffffff;
}
.wnick {
  margin-left: 10rpx;
  font-size: 22rpx;
  color: var(--dp-text3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.wlike {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.wlike-img {
  width: 30rpx;
  height: 30rpx;
}
.wfoot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 14rpx 14rpx;
}
.wuser {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}
.wavatar {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--dp-orange), var(--dp-brand-deep));
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10rpx;
  flex-shrink: 0;
}
.wavatar-text {
  font-size: 20rpx;
  color: #ffffff;
}
.wnick {
  font-size: 21rpx;
  color: var(--dp-text3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 140rpx;
}
.wlike {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.wlike-num {
  margin-left: 6rpx;
  font-size: 22rpx;
  color: var(--dp-text3);
}
.sk-img {
  height: 300rpx;
  border-radius: 0;
}
/* 口碑榜卡：浅粉卡 + 衬线大数字（替代原饱和红底白字） */
.rankbar {
  margin: var(--sp-2) var(--sp-3) 0;
  border-radius: var(--r-xl);
  background: linear-gradient(135deg, #fff5f5, #ffe9ec 55%, #ffdfe4);
  padding: var(--sp-4);
  position: relative;
  overflow: hidden;
  box-shadow: var(--sh-card);
}
.theme-dark .rankbar {
  background: linear-gradient(135deg, #2b1614, #3a1a1c 55%, #43201f);
}
.rankbar::after {
  content: "";
  position: absolute;
  right: -60rpx;
  top: -60rpx;
  width: 240rpx;
  height: 240rpx;
  border-radius: 50%;
  background: rgba(255, 36, 66, .07);
}
.rank-head {
  display: flex;
  align-items: baseline;
  gap: var(--sp-2);
  margin-bottom: var(--sp-3);
  position: relative;
}
.rank-title {
  font-size: var(--fs-md);
  font-weight: 700;
  color: var(--dp-text);
}
.rank-week {
  font-size: var(--fs-caption);
  font-weight: 600;
  color: var(--dp-brand-deep);
  background: rgba(255, 255, 255, .8);
  padding: 2rpx 12rpx;
  border-radius: var(--r-pill);
}
.theme-dark .rank-week {
  background: rgba(255, 255, 255, .1);
}
.rank-more {
  margin-left: auto;
  font-size: var(--fs-sm);
  color: var(--dp-brand-deep);
  font-weight: 500;
}
.rank-item {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  padding: 10rpx 0;
  position: relative;
}
.rank-no {
  width: 52rpx;
  flex-shrink: 0;
  font-size: var(--fs-lg);
  font-weight: 800;
  line-height: 1;
  color: var(--dp-text4);
}
.rank-no.top {
  color: var(--dp-brand-deep);
}
.rank-mid {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.rank-name {
  font-size: var(--fs-base);
  font-weight: 600;
  color: var(--dp-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.rank-sub {
  font-size: var(--fs-caption);
  color: var(--dp-text3);
  margin-top: 4rpx;
}
/* 卡片右上认证标（作者为点评人/管理员时） */
.trust-badge {
  position: absolute;
  top: 10rpx;
  right: 10rpx;
  z-index: 6;
  display: flex;
  align-items: center;
  gap: 4rpx;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 999rpx;
  padding: 3rpx 10rpx;
}
.trust-star {
  width: 18rpx;
  height: 18rpx;
}
.trust-text {
  font-size: 18rpx;
  color: var(--dp-brand-deep);
  font-weight: 700;
}
.sk-img.short {
  height: 220rpx;
}
.sk-line {
  height: 28rpx;
  margin: 14rpx 16rpx;
  border-radius: 6rpx;
}
/* 引导式空状态：图标 + 标题 + 说明 + 下一步按钮（原则 09） */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx var(--sp-5) 80rpx;
}
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
.load-end {
  text-align: center;
  padding: 26rpx 0 46rpx;
}
.load-more {
  text-align: center;
  padding: 26rpx 0 46rpx;
}
.load-more-text {
  font-size: 24rpx;
  color: var(--dp-text4);
}
.card-hover {
  transform: scale(0.97);
  opacity: 0.85;
}
.wcard {
  transition: transform 0.15s ease, opacity 0.15s ease;
}
.img-badge {
  position: absolute;
  right: 12rpx;
  top: 12rpx;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
}
.img-badge-text {
  color: #ffffff;
  font-size: 18rpx;
}
.wlike-icon.liked {
  animation: pop 0.3s ease;
}
@keyframes pop {
  0% { transform: scale(1); }
  50% { transform: scale(1.4); }
  100% { transform: scale(1); }
}
</style>
