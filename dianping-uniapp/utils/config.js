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
export const REGIONS = [
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

export function regionName(code) {
  const r = REGIONS.find(item => item.code === code)
  return r ? r.name : code
}
