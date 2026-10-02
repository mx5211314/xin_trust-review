/**
 * 全局配置
 * BASE_URL 说明：
 * - 浏览器(H5)调试：localhost 可用
 * - 真机调试：改成电脑的局域网 IP（cmd 里 ipconfig 看 IPv4），且手机和电脑连同一 WiFi
 * - 上线：改成服务器域名（需 ICP 备案 + HTTPS）
 */
// #ifdef H5
// H5 版本：空字符串 = 相对路径（与后端同源部署时自动跟随访问域名，换 IP/穿透域名都不用改）
export const BASE_URL = ''
// #endif

// #ifndef H5
// 小程序/App 真机调试：电脑局域网 IP（手机与电脑需同一 WiFi）
export const BASE_URL = 'http://192.168.1.113:18080'
// #endif

/** 地区列表（demo 用河北 11 市，按需增删） */
/**
 * 省市区数据（可扩展）：以后加省份往 PROVINCES 里加一项即可。
 * code 规则：省级=2位（如'13'），市级=6位；同城/地区过滤用前缀匹配（likeRight），
 * 因此选省=按省前缀召回全省内容，选市=按市前缀召回（含区县发布的笔记）。
 */
export const PROVINCES = [
  {
    code: '13',
    name: '河北省',
    cities: [
      { code: '130100', name: '石家庄' },
      { code: '130200', name: '唐山' },
      { code: '130300', name: '秦皇岛' },
      { code: '130400', name: '邯郸' },
      { code: '130500', name: '邢台' },
      { code: '130600', name: '保定' },
      { code: '130700', name: '张家口' },
      { code: '130800', name: '承德' },
      { code: '130900', name: '沧州' },
      { code: '131000', name: '廊坊' },
      { code: '131100', name: '衡水' }
    ]
  }
]

/** 平铺城市列表（兼容旧引用：publish 的手动选择等） */
export const REGIONS = PROVINCES.flatMap(p => p.cities)

export function regionName(code) {
  if (!code) return ''
  const p = PROVINCES.find(x => x.code === code)
  if (p) return p.name
  const r = REGIONS.find(item => item.code === code)
  return r ? r.name : code
}

/**
 * 腾讯位置服务 Key（lbs.qq.com 免费申请，WebService API 勾选 WebServiceAPI）。
 * 用于"定位当前城市"：getLocation 拿经纬度后逆地理编码出城市名。
 * 留空 = 定位功能降级，点"定位当前城市"会提示手动选择城市（选择器仍然可用）。
 * 注意：小程序正式包还需在微信公众平台把 apis.map.qq.com 加入 request 合法域名。
 */
export const TENCENT_LBS_KEY = ''

/**
 * 微信订阅消息模板 ID（mp.weixin.qq.com 订阅消息里选用后复制）。
 * 留空 = 不弹授权、不请求（本地开发/未配置时静默跳过）。
 * audit = 审核结果通知；interact = 互动通知（赞/评/关注/@）。
 */
export const SUBSCRIBE_TEMPLATES = {
  audit: '',
  interact: ''
}
