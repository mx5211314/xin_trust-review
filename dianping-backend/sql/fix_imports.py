# -*- coding: utf-8 -*-
"""重构后自动补 import：跨模块引用短类名时补 import"""
import io, os, re

ROOT = r"D:/桌面文件/111/dianping-backend/src/main/java/com/dianping"
MAP = {
    'AuthController':'auth','AuthService':'auth','LoginReq':'auth',
    'UserController':'user','User':'user','UserMapper':'user','UpdateProfileReq':'user',
    'ContentController':'content','ContentService':'content','Content':'content',
    'ContentMapper':'content','ContentCreateReq':'content','ContentVO':'content',
    'AuditService':'content','LikeService':'content','LikeSyncTask':'content',
    'Comment':'comment','CommentMapper':'comment','CommentLike':'comment',
    'CommentLikeMapper':'comment','CommentReq':'comment','CommentService':'comment',
    'UserAction':'interaction','UserActionMapper':'interaction','Follow':'interaction',
    'FollowMapper':'interaction','FollowService':'interaction','FavoriteService':'interaction',
    'ChatController':'chat','ChatService':'chat','Message':'chat','MessageMapper':'chat','SendMsgReq':'chat',
    'NotifyController':'notify','NotifyService':'notify','Notify':'notify','NotifyMapper':'notify',
    'AdminController':'admin','AdminService':'admin','AnnounceReq':'admin','RejectReq':'admin',
    'OssController':'oss','OssService':'oss',
}
fixed = 0
for base, _, files in os.walk(ROOT):
    for f in files:
        if not f.endswith('.java'):
            continue
        path = os.path.join(base, f)
        with io.open(path, encoding='utf-8') as fp:
            s = fp.read()
        m = re.search(r'^package com\.dianping\.(?:module\.(\w+)|common|);', s, re.M)
        cur = m.group(1) if (m and m.group(1)) else 'common'
        self_cls = f[:-5]
        need = []
        for cls, mod in MAP.items():
            if cls == self_cls:
                continue
            if not re.search(r'(?<![\w.])' + cls + r'(?![\w])', s):
                continue
            imp = 'import com.dianping.module.%s.%s;' % (mod, cls)
            if imp in s:
                continue
            if mod == cur:
                continue
            need.append(imp)
        if not need:
            continue
        # 插入到最后一个 import 之后
        lines = s.split('\n')
        last = max(i for i, l in enumerate(lines) if l.startswith('import '))
        lines = lines[:last + 1] + sorted(need) + lines[last + 1:]
        with io.open(path, 'w', encoding='utf-8') as fp:
            fp.write('\n'.join(lines))
        fixed += 1
print('imports fixed in', fixed, 'files')
