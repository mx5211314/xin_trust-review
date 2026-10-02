import { REGIONS, TENCENT_LBS_KEY } from './config'

/**
 * 定位当前城市（供 feed 城市选择弹层 / publish 发布地区共用）：
 * getLocation 拿经纬度 → 腾讯位置服务逆地理编码出城市名 → 匹配 REGIONS。
 *
 * resolve: 命中的城市 { code, name }
 * reject:  Error（message 可直接 toast 展示给用户）
 *          - TENCENT_LBS_KEY 未配置（config.js 留空，lbs.qq.com 免费申请）
 *          - 用户拒绝授权定位 / 定位失败 / 定位城市不在覆盖范围
 */
export function locateCity() {
  return new Promise((resolve, reject) => {
    if (!TENCENT_LBS_KEY) {
      return reject(new Error('未配置地图Key，请手动选择'))
    }
    uni.getLocation({
      type: 'wgs84',
      success: (res) => {
        uni.request({
          url: 'https://apis.map.qq.com/ws/geocoder/v1/',
          data: {
            location: res.latitude + ',' + res.longitude,
            key: TENCENT_LBS_KEY,
            get_poi: 0
          },
          success: (r) => {
            const city = r.data && r.data.status === 0 && r.data.result
              ? r.data.result.address_component.city : ''
            const hit = REGIONS.find(x => city && (city.indexOf(x.name) === 0 || x.name.indexOf(city.replace('市', '')) === 0))
            if (hit) return resolve(hit)
            reject(new Error('定位到「' + (city || '未知城市') + '」暂未覆盖，请手动选择'))
          },
          fail: () => reject(new Error('定位服务异常，请手动选择'))
        })
      },
      fail: () => reject(new Error('未授权定位，请手动选择'))
    })
  })
}
