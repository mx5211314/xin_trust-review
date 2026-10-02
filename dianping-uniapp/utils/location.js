import { PROVINCES, TENCENT_LBS_KEY } from './config'

/**
 * 腾讯位置服务封装（lbs.qq.com，WebService API）：
 * - locateDetail()：精确定位——省/市/区/行政区划码(adcode)/坐标/推荐地址
 * - nearbyPois()：附近店铺 POI（发布页自动填充店铺名用）
 *
 * TENCENT_LBS_KEY 未配置（config.js 留空）时全部降级 reject，提示手动选择。
 * 小程序正式包还需在微信公众平台把 apis.map.qq.com 加入 request 合法域名。
 */

function requestLbs(path, data) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: 'https://apis.map.qq.com/ws/' + path,
      data: Object.assign({ key: TENCENT_LBS_KEY }, data),
      success: (r) => {
        if (r.data && r.data.status === 0) return resolve(r.data)
        reject(new Error((r.data && r.data.message) || '地图服务异常，请手动选择'))
      },
      fail: () => reject(new Error('网络异常，请手动选择'))
    })
  })
}

/**
 * 精确定位。
 * resolve: { provinceName, cityName, districtName, adcode, lat, lng, recommend }
 *   - adcode 为区县级 6 位码（发布笔记存这个，市级前缀过滤可召回）
 * reject: Error（可直接 toast）
 */
export function locateDetail() {
  return new Promise((resolve, reject) => {
    if (!TENCENT_LBS_KEY) {
      return reject(new Error('未配置地图Key，请手动选择'))
    }
    uni.getLocation({
      type: 'wgs84',
      success: (res) => {
        requestLbs('geocoder/v1/', {
          location: res.latitude + ',' + res.longitude,
          get_poi: 0
        })
          .then((data) => {
            const r = data.result || {}
            const ac = r.address_component || {}
            const ad = r.ad_info || {}
            if (!ac.city) return reject(new Error('定位结果异常，请手动选择'))
            resolve({
              provinceName: ac.province || '',
              cityName: ac.city || '',
              districtName: ac.district || '',
              adcode: ad.adcode || '',
              lat: res.latitude,
              lng: res.longitude,
              recommend: (r.formatted_addresses && r.formatted_addresses.recommend) || ''
            })
          })
          .catch(reject)
      },
      fail: () => reject(new Error('未授权定位，请手动选择'))
    })
  })
}

/**
 * 附近店铺 POI（按距离排序）。resolve: [{ title, address, adcode, lat, lng }]
 * keyword 传空 = 附近全部类型；发布页传"美食"聚焦餐饮店铺。
 */
export function nearbyPois(lat, lng, radius = 1000, keyword = '') {
  if (!TENCENT_LBS_KEY) {
    return Promise.reject(new Error('未配置地图Key'))
  }
  return requestLbs('place/v1/search/', {
    boundary: `nearby(${lat},${lng},${radius},1)`,
    keyword: keyword,
    page_size: 10,
    orderby: '_distance'
  }).then((data) => {
    const list = (data.data || []).map(p => ({
      title: p.title || '',
      address: (p.address || ''),
      adcode: (p.ad_info && p.ad_info.adcode) || '',
      lat: p.location && p.location.lat,
      lng: p.location && p.location.lng
    }))
    if (!list.length) return Promise.reject(new Error('附近没搜到店铺，请手动填写'))
    return list
  })
}

/** 兼容旧调用：只取城市（feed 城市弹层） */
export function locateCity() {
  return locateDetail().then((loc) => {
    const prov = PROVINCES.find(p => loc.provinceName.indexOf(p.name.replace(/省|市$/, '')) === 0 || p.name.indexOf(loc.provinceName.replace(/省|市$/, '')) === 0)
    if (!prov) throw new Error('定位到「' + loc.cityName + '」暂未覆盖，请手动选择')
    const city = prov.cities.find(c => loc.cityName.indexOf(c.name) === 0 || c.name.indexOf(loc.cityName.replace('市', '')) === 0)
    if (!city) throw new Error('定位到「' + loc.cityName + '」暂未覆盖，请手动选择')
    return city
  })
}
