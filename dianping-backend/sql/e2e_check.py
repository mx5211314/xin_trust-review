# -*- coding: utf-8 -*-
"""全流程端到端走查：登录→列表→详情→互动→消息→发布→编辑→删除→后台"""
import json, time, urllib.request, urllib.parse, socket

BASE = 'http://localhost:18080'
OK, FAIL = [], []


def call(method, path, token=None, body=None, form=False, raw=False):
    url = BASE + path
    data = None
    headers = {}
    if body is not None:
        data = json.dumps(body).encode()
        headers['Content-Type'] = 'application/json'
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            txt = r.read().decode('utf-8', errors='ignore')
            return r.status, (txt if raw else json.loads(txt))
    except urllib.error.HTTPError as e:
        return e.code, {'error': e.read().decode('utf-8', errors='ignore')[:120]}
    except Exception as e:
        return 0, {'error': str(e)[:120]}


def step(name, cond, detail=''):
    (OK if cond else FAIL).append(name)
    print(('  ✅ ' if cond else '  ❌ ') + name + (('  → ' + str(detail)[:110]) if detail else ''))


print('== 1. 认证 ==')
st, r = call('POST', '/auth/login', body={'phone': '13901110006', 'code': '8888'})
tok = r.get('data', {}).get('token') if st == 200 else None
step('手机号+验证码登录', bool(tok), r.get('msg', r.get('error')))
st2, r2 = call('POST', '/auth/login', body={'phone': '13800000000', 'code': '8888'})
atok = r2.get('data', {}).get('token') if st2 == 200 else None
step('管理员登录', bool(atok), r2.get('msg'))
st3, r3 = call('POST', '/auth/login', body={'phone': '123', 'code': '8888'})
step('非法手机号被拒（参数校验）', st3 == 200 and r3.get('code') != 0, r3.get('msg'))

print('== 2. 浏览链路 ==')
st, r = call('GET', '/content/feed?page=1&pageSize=10', tok)
lst = r.get('data', {}).get('list', [])
step('首页信息流', len(lst) > 0, '共 %s 条' % r.get('data', {}).get('total'))
if lst:
    img = (lst[0].get('images') or [''])[0]
    st_img, _ = call('GET', img.replace(BASE, ''), raw=True)
    step('列表首图 URL 可访问', st_img == 200, img)
    c0 = lst[0]['contentId']
    st, r = call('GET', '/content/%s' % c0, tok)
    d = r.get('data', {})
    step('详情返回完整字段', bool(d.get('title') and 'likeCount' in d and 'favorited' in d))
st, r = call('GET', '/content/follow-feed?page=1&pageSize=5', tok)
step('关注 tab（follow-feed）', st == 200, r.get('msg', ''))
st, r = call('GET', '/content/search?keyword=' + urllib.parse.quote('烧烤'), tok)
step('搜索', st == 200 and int(r.get('data', {}).get('total', 0) or 0) > 0)
st, r = call('GET', '/content/2011/comments?page=1&pageSize=10', tok)
step('评论树（父+子）', st == 200 and 'rootTotal' in r.get('data', {}))

print('== 3. 互动链路 ==')
st, r = call('GET', '/content/2003', tok)
liked = r.get('data', {}).get('liked')
st, r = call('POST' if not liked else 'DELETE', '/content/2003/like', tok)
step('点赞/取消', st == 200 and r.get('code') in (0, 2003), r.get('msg', ''))
st, r = call('GET', '/content/2003', tok)
step('点赞数即时更新', st == 200, 'likeCount=%s' % r.get('data', {}).get('likeCount'))
st, r = call('POST', '/content/2007/favorite', tok)
step('收藏', st == 200 and r.get('code') in (0, 2003), r.get('msg', ''))
st, r = call('POST', '/content/2008/comments', tok, body={'text': '走查评论 ' + str(int(time.time()) % 10000)})
step('发评论', st == 200 and r.get('code') == 0, r.get('msg', ''))
st, r = call('POST', '/content/comments/3012/like', tok)
step('评论点赞', st == 200 and r.get('code') in (0, 2003), r.get('msg', ''))
st, r = call('POST', '/user/1005/follow', tok)
step('关注（幂等）', st == 200 and r.get('code') in (0, 2003), r.get('msg', ''))
st, r = call('POST', '/chat/send', tok, body={'toUserId': 1005, 'text': '走查私信'})
step('发私信', st == 200 and r.get('code') == 0, r.get('msg', ''))

print('== 4. 消息链路 ==')
st, r = call('GET', '/user/notify?category=interact&page=1&pageSize=5', tok)
nid = (r.get('data', {}).get('list') or [{}])[0].get('notifyId')
step('互动消息列表', st == 200, 'total=%s' % r.get('data', {}).get('total'))
st, r = call('GET', '/user/notify?category=announce&page=1&pageSize=5', tok)
aid = (r.get('data', {}).get('list') or [{}])[0].get('notifyId')
step('公告列表', st == 200 and int(r.get('data', {}).get('total', 0) or 0) > 0)
st, r = call('GET', '/user/notify/unread', tok)
u1 = r.get('data', {}).get('unread')
step('未读数', st == 200 and u1 is not None, 'unread=%s' % u1)
if aid:
    st, r = call('POST', '/user/notify/%s/read' % aid, tok)
    st, r = call('GET', '/user/notify/unread', tok)
    step('单条已读后未读数下降', int(r.get('data', {}).get('unread', 0)) < int(u1 or 0), 'unread=%s→%s' % (u1, r.get('data', {}).get('unread')))
st, r = call('GET', '/chat/conversations', tok)
step('会话列表', st == 200 and 'list' in r.get('data', {}))

print('== 5. 发布链路（点评人） ==')
st, r = call('POST', '/auth/login', body={'phone': '13901110001', 'code': '8888'})
rtok = r.get('data', {}).get('token') if st == 200 else None
step('点评人登录', bool(rtok))
st, r = call('POST', '/content', rtok, body={'title': '走查笔记', 'text': '端到端走查内容', 'images': ['upload/demo/dp1.jpg'], 'regionCode': '130200', 'tags': ['探店']})
cid = r.get('data', {}).get('contentId') if st == 200 else None
step('发布笔记', bool(cid), r.get('msg', r.get('error')))
if cid:
    st, r = call('PUT', '/content/%s' % cid, rtok, body={'title': '走查笔记（已编辑）', 'text': '编辑后内容', 'regionCode': '130200'})
    step('编辑笔记', st == 200 and r.get('code') == 0, r.get('msg', ''))
    st, r = call('DELETE', '/content/%s' % cid, rtok)
    step('删除笔记（含级联清理）', st == 200 and r.get('code') == 0, r.get('msg', ''))
st, r = call('POST', '/auth/login', body={'phone': '13901110002', 'code': '8888'})
ztok = r.get('data', {}).get('token') if st == 200 else None
st, r = call('PUT', '/content/2001', ztok, body={'title': '越权编辑', 'text': 'x', 'regionCode': '130200'})
step('越权编辑他人笔记被拒（1003）', r.get('code') == 1003, r.get('msg', ''))
st, r = call('POST', '/auth/login', body={'phone': '13901110001', 'code': '8888'})
rtok = r.get('data', {}).get('token') or rtok

print('== 6. 个人中心 ==')
st, r = call('GET', '/user/me', tok); step('我的资料', st == 200 and r.get('data', {}).get('nickname'))
st, r = call('GET', '/user/stats', tok); d = r.get('data', {})
step('数据统计（含关注/粉丝）', all(k in d for k in ('following', 'followers', 'likes')), d)
st, r = call('GET', '/user/favorites', tok); step('我的收藏', st == 200 and 'total' in r.get('data', {}))
st, r = call('GET', '/user/liked', tok); step('赞过列表', st == 200 and 'total' in r.get('data', {}))
st, r = call('GET', '/user/following', tok); step('我关注的', st == 200 and 'list' in r.get('data', {}))
st, r = call('PUT', '/user/profile', tok, body={'nickname': '路过的小美', 'bio': '爱探店的小美'})
step('编辑资料', st == 200 and r.get('code') == 0, r.get('msg', ''))

print('== 7. 后台管理（管理员） ==')
st, r = call('GET', '/admin/content/list?page=1&pageSize=5', atok)
step('内容管理列表', st == 200 and 'list' in r.get('data', {}))
st, r = call('GET', '/admin/user/list?page=1&pageSize=5', atok)
step('用户管理列表', st == 200 and (r.get('data') is not None), str(r.get('data'))[:80])
st, r = call('POST', '/admin/announce', atok, body={'text': '【走查】公告链路测试'})
step('发布公告', st == 200 and r.get('code') == 0, r.get('msg', r.get('error')))
st, r = call('POST', '/admin/user/3002/ban', atok); step('封禁用户', st == 200 and r.get('code') == 0, r.get('msg', ''))
st, r = call('POST', '/admin/user/3002/unban', atok); step('解封用户', st == 200 and r.get('code') == 0, r.get('msg', ''))

print()
print('==== 结果：通过 %d / 失败 %d ====' % (len(OK), len(FAIL)))
if FAIL:
    print('失败项：')
    for f in FAIL:
        print('  - ' + f)
