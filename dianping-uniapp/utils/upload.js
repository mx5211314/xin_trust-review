import { request } from './request'
import { getToken } from './auth'
import { BASE_URL } from './config'

/**
 * 文件上传（双模式，后端 upload-params 返回的 mode 决定）：
 * - local  模式：uni.uploadFile POST 到后端 /oss/upload-local，
 *   响应是统一返回体 JSON —— 必须校验业务 code=0，
 *   否则后端报错（HTTP 200 + code!=0）会被当成上传成功（已踩过的坑）
 * - aliyun 模式：PostObject 直传 OSS，HTTP 200 即成功
 *
 * 坑点记录：
 * 1. uni.uploadFile 不会自动带登录态，必须手动加 Authorization 头，
 *    否则后端拦截器返回 1002（HTTP 仍是 200）。
 * 2. H5 的 chooseImage/chooseVideo 返回的是 blob:http://host/uuid 形式的临时地址，
 *    **没有文件扩展名**。原先用 path.split('.') 猜后缀，会得到 "1:18080/uuid"
 *    这种垃圾值 → 后端 buildKey 拼进 key → saveLocal 校验扩展名失败 →
 *    「不支持的文件类型」。（头像上传报这个错就是踩了这一条）
 *    现在优先从 MIME 判断，且只采用后端白名单里的扩展名。
 *
 * resolve 值：{ key, url } —— key 用于提交给后端存库，url 用于本地预览/直接展示
 */

/** 后端 OssService 的白名单：IMAGE_EXT / VIDEO_EXT（不一致会被拒） */
const IMAGE_EXT = ['jpg', 'jpeg', 'png', 'webp', 'gif']
const VIDEO_EXT = ['mp4', 'mov', 'm4v']

/** MIME → 扩展名，只映射白名单内的值，避免出了白名单又被后端打回 */
const MIME_EXT = {
  'image/jpeg': 'jpg',
  'image/jpg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
  'image/gif': 'gif',
  'video/mp4': 'mp4',
  'video/quicktime': 'mov',
  'video/x-m4v': 'm4v'
}

function mimeToExt(mime) {
  if (!mime) return ''
  const key = String(mime).toLowerCase().split(';')[0].trim()
  return MIME_EXT[key] || ''
}

/** 从路径取后缀：只认「最后一段是 2~5 位字母数字」且在白名单内，否则返回空 */
function pathToExt(filePath, allowed) {
  const clean = String(filePath || '').split('?')[0].split('#')[0]
  const seg = clean.split('.')
  if (seg.length < 2) return ''
  const last = seg[seg.length - 1].toLowerCase()
  if (!/^[a-z0-9]{2,5}$/.test(last)) return ''
  return allowed.indexOf(last) >= 0 ? last : ''
}

/**
 * 解析真实扩展名。优先级：调用方传的 MIME > blob/data 的实际 MIME > 路径后缀 > 按类型的默认值
 * blob/data 分支只在浏览器里跑，小程序里 filePath 是本地真实路径，不会进这里。
 */
function resolveExt(filePath, type, mime) {
  const allowed = type === 'video' ? VIDEO_EXT : IMAGE_EXT
  let ext = mimeToExt(mime)

  const p = String(filePath || '')
  if (!ext && /^(blob:|data:)/.test(p) && typeof fetch === 'function') {
    return fetch(p)
      .then((res) => res.blob())
      .then((blob) => mimeToExt(blob && blob.type) || pathToExt(p, allowed) || allowed[0])
      .catch(() => pathToExt(p, allowed) || allowed[0])
  }

  if (!ext) ext = pathToExt(p, allowed)
  return Promise.resolve(ext || allowed[0])
}

export function uploadFile(filePath, type = 'image', mime = '') {
  return new Promise((resolve, reject) => {
    resolveExt(filePath, type, mime)
      .then((ext) => {
        request({
          url: `/oss/upload-params?type=${type}&ext=${ext}`
        }).then((params) => {
          const isLocal = params.mode === 'local'
          uni.uploadFile({
            url: params.host,
            filePath: filePath,
            name: 'file',
            header: { Authorization: 'Bearer ' + getToken() },
            formData: {
              key: params.key,
              policy: params.policy || '',
              OSSAccessKeyId: params.ossAccessKeyId || '',
              signature: params.signature || '',
              success_action_status: '200'
            },
            success: (res) => {
              if (isLocal) {
                let body = null
                try { body = JSON.parse(res.data) } catch (e) { /* 非 JSON */ }
                if (res.statusCode === 200 && body && body.code === 0) {
                  return resolve({ key: params.key, url: BASE_URL + '/' + params.key })
                }
                const msg = (body && body.msg) ? body.msg : 'HTTP ' + res.statusCode
                uni.showToast({ title: '上传失败：' + msg, icon: 'none', duration: 3000 })
                return reject(new Error('upload fail: ' + msg))
              }
              // aliyun 直传：HTTP 200 即成功（url 留空，展示由后端 publicUrl 拼）
              if (res.statusCode === 200) {
                return resolve({ key: params.key, url: '' })
              }
              uni.showToast({ title: '上传失败(' + res.statusCode + ')', icon: 'none' })
              reject(new Error('upload fail ' + res.statusCode))
            },
            fail: (err) => {
              uni.showToast({ title: '上传失败，请检查网络/后端', icon: 'none' })
              reject(err)
            }
          })
        }).catch(reject)
      })
      .catch(reject)
  })
}
