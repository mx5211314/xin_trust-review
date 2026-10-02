<template>
  <view>
    <view v-if="show" class="mask" @tap="$emit('close')">
      <view class="sheet" @tap.stop>
        <view class="sheet-head">
          <text class="sheet-title">选择城市</text>
          <text class="sheet-close" @tap="$emit('close')">×</text>
        </view>
        <view class="loc-row" @tap="onLocate">
          <image class="loc-ic" src="/static/icons/location.png" mode="aspectFit" />
          <text class="loc-tx">{{ locating ? '定位中…' : '定位当前城市' }}</text>
          <text class="loc-arr">›</text>
        </view>
        <view class="city-grid">
          <view
            v-for="r in regions"
            :key="r.code"
            class="city-item"
            :class="{ on: current === r.code }"
            @tap="$emit('select', r)"
          >
            <text class="city-tx" :class="{ on: current === r.code }">{{ r.name }}</text>
          </view>
        </view>
        <view style="height: env(safe-area-inset-bottom)"></view>
      </view>
    </view>
  </view>
</template>

<script>
import { REGIONS } from '@/utils/config'
import { locateCity } from '@/utils/location'

/**
 * 城市选择弹层（feed 同城 / publish 发布地区共用）：
 * - 第一行"定位当前城市"：locateCity() 成功后 emit('select', {code,name})，失败组件内 toast
 * - 城市网格：点击 emit('select', {code,name})，当前选中项红底高亮
 * 事件：@select(city) @close
 */
export default {
  name: 'CitySheet',
  props: {
    show: { type: Boolean, default: false },
    /** 当前选中城市 code（高亮用） */
    current: { type: String, default: '' }
  },
  emits: ['select', 'close'],
  data() {
    return {
      regions: REGIONS,
      locating: false
    }
  },
  methods: {
    onLocate() {
      if (this.locating) return
      this.locating = true
      locateCity()
        .then((hit) => {
          this.locating = false
          this.$emit('select', hit)
        })
        .catch((e) => {
          this.locating = false
          uni.showToast({ title: e.message || '定位失败，请手动选择', icon: 'none', duration: 2200 })
        })
    }
  }
}
</script>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 60;
}
.sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 61;
  background: var(--dp-card);
  border-radius: 28rpx 28rpx 0 0;
  padding: 30rpx 28rpx 24rpx;
}
.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
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
.loc-row {
  display: flex;
  align-items: center;
  background: var(--dp-accent-soft);
  border-radius: 16rpx;
  padding: 22rpx 24rpx;
}
.loc-ic {
  width: 30rpx;
  height: 30rpx;
}
.loc-tx {
  flex: 1;
  margin-left: 12rpx;
  font-size: 27rpx;
  font-weight: 600;
  color: #ff2442;
}
.loc-arr {
  font-size: 26rpx;
  color: #ff2442;
  opacity: 0.6;
}
.city-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
  margin-top: 26rpx;
}
.city-item {
  width: calc((100% - 36rpx) / 3);
  text-align: center;
  padding: 18rpx 0;
  background: var(--dp-soft);
  border-radius: 12rpx;
  box-sizing: border-box;
}
.city-tx {
  font-size: 26rpx;
  color: var(--dp-text);
}
.city-item.on {
  background: #ff2442;
}
.city-tx.on {
  color: #ffffff;
  font-weight: 600;
}
</style>
