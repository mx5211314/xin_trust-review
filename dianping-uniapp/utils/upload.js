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
 * 坑点记录：uni.uploadFile 不会自动带登录态，必须手动加
 * Authorization 头，否则后端拦截器返回 1002（HTTP 仍是 200）。
 *
 * resolve 值：{ key, url } —— key 用于提交给后端存库，url 用于本地预览/直接展示
 */
export function uploadFile(filePath, type = 'image') {
  return new Promise((resolve, reject) => {
    const seg = filePath.split('.')
    const ext = seg.length > 1 ? seg[seg.length - 1].toLowerCase() : ''
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
}
