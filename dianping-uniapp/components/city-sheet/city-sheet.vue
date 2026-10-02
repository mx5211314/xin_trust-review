<template>
  <view>
    <view v-if="show" class="mask" @tap="$emit('close')">
      <view class="sheet" @tap.stop>
        <view class="sheet-head">
          <text class="sheet-title">选择地区</text>
          <text class="sheet-close" @tap="$emit('close')">×</text>
        </view>
        <view class="loc-row" @tap="onLocate">
          <image class="loc-ic" src="/static/icons/location.png" mode="aspectFit" />
          <text class="loc-tx">{{ locating ? '定位中…' : '定位当前城市' }}</text>
          <text class="loc-arr">›</text>
        </view>

        <!-- 左省右市两级；点省名整行=选全省（前缀过滤） -->
        <view class="body-row">
          <scroll-view class="prov-col" scroll-y :show-scrollbar="false">
            <view
              v-for="p in provinces"
              :key="p.code"
              class="prov-item"
              :class="{ on: curProvCode === p.code }"
              @tap="curProvCode = p.code"
            >
              <text class="prov-tx" :class="{ on: curProvCode === p.code }">{{ p.name }}</text>
            </view>
          </scroll-view>
          <scroll-view class="city-col" scroll-y :show-scrollbar="false">
            <view class="prov-all" @tap="pickProvince">
              <text class="prov-all-tx">{{ curProv.name }}（全省）</text>
            </view>
            <view class="city-grid">
              <view
                v-for="c in curProv.cities"
                :key="c.code"
                class="city-item"
                :class="{ on: current === c.code }"
                @tap="$emit('select', { code: c.code, name: c.name })"
              >
                <text class="city-tx" :class="{ on: current === c.code }">{{ c.name }}</text>
              </view>
            </view>
            <view style="height: 30rpx"></view>
          </scroll-view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { PROVINCES } from '@/utils/config'
import { locateDetail } from '@/utils/location'

/**
 * 地区选择弹层（feed 同城 / publish 发布地区共用）：
 * - 左省右市两级联动；点"全省"选整个省（code=省级码，过滤走前缀匹配）
 * - 第一行"定位当前城市"：locateDetail() 成功后 emit('locate', 定位结果)，
 *   失败组件内 toast（key 未配置等降级提示）
 * 事件：@select(city {code,name}) @pick-province(prov {code,name}) @locate(loc) @close
 */
export default {
  name: 'CitySheet',
  props: {
    show: { type: Boolean, default: false },
    /** 当前选中地区 code（省码或市码，高亮用） */
    current: { type: String, default: '' }
  },
  emits: ['select', 'pick-province', 'locate', 'close'],
  data() {
    return {
      provinces: PROVINCES,
      curProvCode: PROVINCES[0].code,
      locating: false
    }
  },
  computed: {
    curProv() {
      return this.provinces.find(p => p.code === this.curProvCode) || this.provinces[0]
    }
  },
  watch: {
    show(v) {
      if (v && this.current) {
        // 高亮定位到 current 所属省
        const prov = this.provinces.find(p => this.current.indexOf(p.code) === 0)
        if (prov) this.curProvCode = prov.code
      }
    }
  },
  methods: {
    pickProvince() {
      this.$emit('pick-province', { code: this.curProv.code, name: this.curProv.name, isProvince: true })
    },
    onLocate() {
      if (this.locating) return
      this.locating = true
      locateDetail()
        .then((loc) => {
          this.locating = false
          this.$emit('locate', loc)
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
  color: var(--dp-brand-deep);
}
.loc-arr {
  font-size: 26rpx;
  color: var(--dp-brand-deep);
  opacity: 0.6;
}
.body-row {
  display: flex;
  margin-top: 22rpx;
  height: 440rpx;
}
.prov-col {
  width: 176rpx;
  background: var(--dp-soft);
  border-radius: 14rpx;
  flex-shrink: 0;
  height: 100%;
}
.prov-item {
  padding: 24rpx 20rpx;
}
.prov-tx {
  font-size: 26rpx;
  color: var(--dp-text3);
}
.prov-tx.on {
  color: #ff2442;
  font-weight: 700;
}
.city-col {
  flex: 1;
  height: 100%;
  margin-left: 18rpx;
  box-sizing: border-box;
}
.prov-all {
  background: var(--dp-accent-soft);
  border-radius: 12rpx;
  padding: 18rpx 20rpx;
  margin-bottom: 18rpx;
}
.prov-all-tx {
  font-size: 25rpx;
  font-weight: 600;
  color: var(--dp-brand-deep);
}
.city-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.city-item {
  width: calc((100% - 32rpx) / 3);
  text-align: center;
  padding: 16rpx 0;
  background: var(--dp-soft);
  border-radius: 12rpx;
  box-sizing: border-box;
}
.city-tx {
  font-size: 25rpx;
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
