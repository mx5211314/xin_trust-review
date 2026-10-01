# -*- coding: utf-8 -*-
"""生成演示用户 + 真实点赞关系（让 like_count 是真实且好看的数字）"""
import random, io
random.seed(42)

XING = ['老', '小', '阿', '大', '陈', '李', '王', '刘', '赵', '周', '孙', '吴', '郑', '冯', '陈']
MING = ['张', '明', '强', '磊', '洋', '静', '敏', '丽', '杰', '涛', '峰', '宁', '航', '宇', 'xin']
SUFFIX = ['爱吃', '探店', '在路上', '有点馋', '觅食记', '本地通', '溜达', '随手拍', '干饭人', 'citywalk', '爱遛弯', '常客']

lines = ["-- 自动生成：演示用户 + 点赞关系（让点赞数是真实值）", "USE dianping;", ""]
lines.append("DELETE FROM `user` WHERE id BETWEEN 3001 AND 3060;")
lines.append("DELETE FROM user_action WHERE id BETWEEN 900001 AND 999999;")
lines.append("")
lines.append("-- 60 个演示用户")
vals = []
for i in range(60):
    uid = 3001 + i
    phone = '1390222%04d' % (1001 + i)
    nick = random.choice(SUFFIX) + str(random.randint(1, 99))
    role = 'REVIEWER' if i % 12 == 0 else 'USER'
    vals.append("(%d, '%s', '%s', '%s', 'NORMAL')" % (uid, phone, nick, role))
lines.append("INSERT INTO `user` (id, phone, nickname, role, status) VALUES\n" + ",\n".join(vals) + ";")
lines.append("")

# 每条内容随机 18~52 个用户点赞
lines.append("-- 真实点赞关系（内容 2001-2011）")
action_rows = []
aid = 900001
for cid in list(range(2001, 2012)):
    k = random.randint(18, 52)
    likers = random.sample(range(3001, 3061), k)
    for uid in likers:
        action_rows.append("(%d, %d, %d, 1)" % (aid, uid, cid))
        aid += 1
# 追加原有演示用户的点赞也保留（跳过，已存在）
lines.append("INSERT IGNORE INTO user_action (id, user_id, content_id, type) VALUES\n" + ",\n".join(action_rows) + ";")
lines.append("")
lines.append("-- 按真实关系回填 like_count")
for cid in list(range(2001, 2012)):
    lines.append("UPDATE content SET like_count = (SELECT COUNT(*) FROM user_action ua WHERE ua.content_id = %d AND ua.type = 1) WHERE id = %d;" % (cid, cid))
lines.append("")
lines.append("-- 评论点赞补一些（让热评有赞）")
cl_rows = []
clid = 950001
for cmid in [3001, 3002, 3005, 3012, 3016]:
    for uid in random.sample(range(3001, 3061), random.randint(3, 12)):
        cl_rows.append("(%d, %d, %d)" % (clid, cmid, uid))
        clid += 1
lines.append("INSERT IGNORE INTO comment_like (id, comment_id, user_id) VALUES\n" + ",\n".join(cl_rows) + ";")
lines.append("UPDATE comment SET like_count = (SELECT COUNT(*) FROM comment_like cl WHERE cl.comment_id = comment.id);")

io.open('demo-users-likes.sql', 'w', encoding='utf-8').write("\n".join(lines))
print('generated demo-users-likes.sql, users=60, likes=%d, commentLikes=%d' % (len(action_rows), len(cl_rows)))
