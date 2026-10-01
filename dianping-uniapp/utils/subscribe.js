/**
 * 微信订阅消息授权引导。
 * 必须在用户点击手势内调用（微信限制）；模板 ID 未配置时静默跳过；
 * 每个业务键每次启动只弹一次，授权结果上报后端记额度（一次性订阅：授一次发一条）。
 */
import { request } from './request'
import { SUBSCRIBE_TEMPLATES } from './config'

const askedInSession = {}

export function askSubscribe(keys) {
  const tmplIds = (keys || [])
    .map(k => SUBSCRIBE_TEMPLATES[k])
    .filter(id => !!id)
  // 未配置模板：本地开发/未接入，静默跳过
  if (!tmplIds.length) return Promise.resolve()

  return new Promise(resolve => {
    uni.requestSubscribeMessage({
      tmplIds,
      success: (res) => {
        // res[tmplId] === 'accept' 表示用户同意该条
        const accepted = tmplIds.filter(id => res[id] === 'accept')
        if (accepted.length) {
          // 上报额度（键名换回业务键）
          const keyById = {}
          keys.forEach(k => { if (SUBSCRIBE_TEMPLATES[k]) keyById[SUBSCRIBE_TEMPLATES[k]] = k })
          const bizKeys = accepted.map(id => keyById[id]).filter(Boolean)
          request({
            url: '/wechat/subscribe/report',
            method: 'POST',
            data: { keys: bizKeys },
            silent: true
          }).catch(() => {})
        }
        resolve()
      },
      fail: () => resolve() // 用户取消/环境不支持（App/H5）都静默
    })
  })
}

/** 会话内同键只引导一次 */
export function askSubscribeOnce(key) {
  if (askedInSession[key]) return Promise.resolve()
  askedInSession[key] = true
  return askSubscribe([key])
}
