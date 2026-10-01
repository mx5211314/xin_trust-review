# -*- coding: utf-8 -*-
"""后端按业务模块重构：移动文件 + 重写 package/import/全限定名"""
import io, os, re, shutil

ROOT = r"D:\桌面文件\111\dianping-backend\src\main\java\com\dianping"

# 类名 -> 新模块包
MAP = {
    # auth
    'AuthController': 'auth', 'AuthService': 'auth', 'LoginReq': 'auth',
    # user
    'UserController': 'user', 'User': 'user', 'UserMapper': 'user', 'UpdateProfileReq': 'user',
    # content
    'ContentController': 'content', 'ContentService': 'content', 'Content': 'content',
    'ContentMapper': 'content', 'ContentCreateReq': 'content', 'ContentVO': 'content',
    'AuditService': 'content', 'LikeService': 'content', 'LikeSyncTask': 'content',
    # comment
    'Comment': 'comment', 'CommentMapper': 'comment', 'CommentLike': 'comment',
    'CommentLikeMapper': 'comment', 'CommentReq': 'comment', 'CommentService': 'comment',
    # interaction
    'UserAction': 'interaction', 'UserActionMapper': 'interaction', 'Follow': 'interaction',
    'FollowMapper': 'interaction', 'FollowService': 'interaction', 'FavoriteService': 'interaction',
    # chat
    'ChatController': 'chat', 'ChatService': 'chat', 'Message': 'chat', 'MessageMapper': 'chat',
    'SendMsgReq': 'chat',
    # notify
    'NotifyController': 'notify', 'NotifyService': 'notify', 'Notify': 'notify', 'NotifyMapper': 'notify',
    # admin
    'AdminController': 'admin', 'AdminService': 'admin', 'AnnounceReq': 'admin', 'RejectReq': 'admin',
    # oss
    'OssController': 'oss', 'OssService': 'oss',
}

# 明确不移动的（common 基础设施）—— WebConfig/MybatisPlusConfig 保留在 common
KEEP = {'R', 'ResultCode', 'BizException', 'GlobalExceptionHandler', 'JwtUtil',
        'AuthInterceptor', 'RequireRole', 'UserContext', 'WebConfig', 'MybatisPlusConfig',
        'DianpingApplication'}

LAYERS = ['entity', 'mapper', 'service', 'controller', 'dto', 'task']
OLD = r"D:\桌面文件\111\dianping-backend\src\main\java\com\dianping"

# 1. 收集待移动文件
moves = []  # (src, dst, cls, module)
for layer in LAYERS:
    d = os.path.join(OLD, layer)
    if not os.path.isdir(d):
        continue
    for f in os.listdir(d):
        if not f.endswith('.java'):
            continue
        cls = f[:-5]
        if cls in KEEP:
            continue
        mod = MAP.get(cls)
        if not mod:
            print('!! 未映射，跳过：', cls)
            continue
        moves.append((os.path.join(d, f), os.path.join(OLD, 'module', mod, f), cls, mod))

# 2. 执行移动
for src, dst, cls, mod in moves:
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.move(src, dst)
print('moved', len(moves), 'files')

# 3. 构建替换表（全限定名）
repl = []
for cls, mod in MAP.items():
    for layer in LAYERS:
        old_fqn = 'com.dianping.%s.%s' % (layer, cls)
        new_fqn = 'com.dianping.module.%s.%s' % (mod, cls)
        repl.append((old_fqn, new_fqn))

# 4. 重写所有 java 文件
def rewrite(path):
    with io.open(path, encoding='utf-8') as fp:
        s = fp.read()
    orig = s
    # package 声明
    m = re.search(r'^package com\.dianping\.(\w+);', s, re.M)
    if m:
        layer = m.group(1)
        cls = os.path.basename(path)[:-5]
        if layer in LAYERS and cls in MAP:
            s = s.replace('package com.dianping.%s;' % layer,
                          'package com.dianping.module.%s;' % MAP[cls], 1)
    # 全限定名替换（长的先替换，避免前缀误伤）
    for old_fqn, new_fqn in sorted(repl, key=lambda x: -len(x[0])):
        s = re.sub(re.escape(old_fqn) + r'(?![\w])', new_fqn, s)
    if s != orig:
        with io.open(path, 'w', encoding='utf-8') as fp:
            fp.write(s)
        return True
    return False

changed = 0
for base, _, files in os.walk(ROOT):
    for f in files:
        if f.endswith('.java'):
            if rewrite(os.path.join(base, f)):
                changed += 1
print('rewritten', changed, 'files')
