-- 演示数据（可重复执行）
-- 用户：点评人 1001~1005、普通用户 1006；管理员 1 已由 init.sql 预置
USE dianping;

-- 1. comment 表
CREATE TABLE IF NOT EXISTS `comment` (
    `id`          BIGINT       NOT NULL COMMENT '雪花ID/演示自定ID',
    `content_id`  BIGINT       NOT NULL,
    `user_id`     BIGINT       NOT NULL,
    `text`        VARCHAR(500) NOT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_content` (`content_id`)
) COMMENT '评论';

-- 2. content 加 tags 列（幂等）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA='dianping' AND TABLE_NAME='content' AND COLUMN_NAME='tags');
SET @ddl := IF(@exist=0,
    'ALTER TABLE content ADD COLUMN tags VARCHAR(500) NOT NULL DEFAULT '''' AFTER images',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3. 清理旧演示数据（保证可重复执行）
DELETE FROM `user` WHERE id BETWEEN 1001 AND 1006;
DELETE FROM content WHERE id BETWEEN 2001 AND 2099;
DELETE FROM comment WHERE id BETWEEN 3001 AND 3099;
DELETE FROM user_action WHERE id BETWEEN 5001 AND 5099;
DELETE FROM user_action WHERE id BETWEEN 6001 AND 6099;

-- 4. 演示用户
INSERT INTO `user` (id, phone, nickname, role, status) VALUES
(1001, '13901110001', '爱吃的老王', 'REVIEWER', 'NORMAL'),
(1002, '13901110002', '小周探店',   'REVIEWER', 'NORMAL'),
(1003, '13901110003', '漫步的猫',   'REVIEWER', 'NORMAL'),
(1004, '13901110004', '豆豆酱',     'REVIEWER', 'NORMAL'),
(1005, '13901110005', '唐山本地通', 'REVIEWER', 'NORMAL'),
(1006, '13901110006', '路过的小美', 'USER',     'NORMAL');

-- 5. 演示内容（8 篇，图片为公共示例图源，仅演示用）
INSERT INTO content (id, user_id, title, `text`, images, video_key, cover_key, duration, region_code, poi_name, status, reject_reason, like_count, view_count, tags) VALUES
(2001, 1001, '唐山这家老味烧烤，本地人从小吃到大',
 '开了二十多年的老店，就在建设南路小区门口。\n招牌是烤腰子和羊排，配一瓣蒜绝了。\n人均30，老板娘特别热情，去晚了要排队。\n总之：闭眼冲，不好吃你回来找我。',
 '["https://picsum.photos/seed/dp2001a/600/450","https://picsum.photos/seed/dp2001b/600/450","https://picsum.photos/seed/dp2001c/600/450"]',
 '', '', 0, '130200', '老王烧烤（建设南路店）', 'APPROVED', '', 128, 321, '["唐山美食","探店","老店"]'),
(2002, 1002, '人均30吃到扶墙出！唐山自助烤肉天花板',
 '新开业搞活动，人均30随便造。\n肉类新鲜度在线，五花肉和牛舌必拿。\n饮料冰淇淋都含，学生党狂喜。\n扣一分在排风一般，吃完一身味。',
 '["https://picsum.photos/seed/dp2002a/600/450","https://picsum.photos/seed/dp2002b/600/450"]',
 '', '', 0, '130200', '炭火自助烤肉（万达店）', 'APPROVED', '', 56, 189, '["唐山美食","探店"]'),
(2003, 1003, '周末棋盘山遛娃全攻略，附路线图',
 '带娃党看过来！棋盘山现在设施完善了。\n上山走东线缓坡，推车也OK。\n山顶茶社能歇脚，娃在草坪撒欢。\n建议上午去，下午人多点。',
 '["https://picsum.photos/seed/dp2003a/600/450","https://picsum.photos/seed/dp2003b/600/450"]',
 '', '', 0, '130200', '棋盘山景区', 'APPROVED', '', 31, 97, '["遛娃","游玩"]'),
(2004, 1004, '新开的咖啡店值得二刷｜唐山探店',
 '建设路新开的社区咖啡，豆子是自家烘的。\n澳白奶咖比 6:4，入口很顺。\n店里能撸猫，老板养了只橘猪。\n缺点：座位少，周末要等位。',
 '["https://picsum.photos/seed/dp2004a/600/450"]',
 '', '', 0, '130200', '山有咖啡', 'APPROVED', '', 21, 66, '["咖啡","拍照"]'),
(2005, 1005, '唐山宴一日游：把这些唐山味道一次吃遍',
 '外地朋友来唐山，我必带他去唐山宴。\n棋子烧饼、饹馇、十三香小龙虾一条街吃遍。\n一楼小吃二楼正餐，人均50能吃到走不动。\n记得空着肚子来。',
 '["https://picsum.photos/seed/dp2005a/600/450","https://picsum.photos/seed/dp2005b/600/450","https://picsum.photos/seed/dp2005c/600/450"]',
 '', '', 0, '130200', '唐山宴', 'APPROVED', '', 88, 240, '["唐山美食","老店"]'),
(2006, 1001, '这家羊汤馆开了三十年，冬天必冲',
 '老字号羊汤，汤底奶白不膻。\n加烧饼泡汤是本地人标配吃法。\n大碗15加肉20，冬天喝完全身暖。',
 '["https://picsum.photos/seed/dp2006a/600/450"]',
 '', '', 0, '130200', '郝家羊汤（老店）', 'APPROVED', '', 45, 132, '["老店","唐山美食"]'),
(2007, 1005, '秦皇小巷夜市初体验，人均50吃撑',
 '秦皇岛新开的室内夜市，下雨天也能逛。\n推荐烤生蚝和榴莲饼，炒酸奶料超足。\n人均50吃到撑，适合朋友聚餐。',
 '["https://picsum.photos/seed/dp2007a/600/450","https://picsum.photos/seed/dp2007b/600/450"]',
 '', '', 0, '130300', '秦皇小巷夜市', 'APPROVED', '', 33, 110, '["探店","夜市"]'),
(2008, 1002, '唐山南湖灯光秀，免费还出片',
 '南湖音乐喷泉+灯光秀，每周五六日晚上8点。\n拍照机位在1号门桥上，逆光剪影绝了。\n地铁直达，停车免费2小时。',
 '["https://picsum.photos/seed/dp2008a/600/450","https://picsum.photos/seed/dp2008b/600/450"]',
 '', '', 0, '130200', '南湖景区', 'APPROVED', '', 67, 205, '["拍照","游玩"]');

-- 6. 演示评论
INSERT INTO comment (id, content_id, user_id, `text`) VALUES
(3001, 2001, 1004, '看完直接冲了，烤腰子确实一绝！'),
(3002, 2001, 1006, '求地址！周末带爸妈去'),
(3003, 2001, 1005, '老店了，我小时候我爹就带我去'),
(3004, 2001, 1003, '排队要排多久呀？'),
(3005, 2002, 1006, '上周去过，五花肉确实可以'),
(3006, 2002, 1001, '学生党福音，收藏了'),
(3007, 2002, 1005, '排风确实一般，建议穿外套'),
(3008, 2003, 1004, '娃他妈感谢攻略，周末就走起'),
(3009, 2003, 1006, '东线推车真的能上山吗？'),
(3010, 2004, 1002, '橘猪哈哈哈哈，必须去撸'),
(3011, 2004, 1005, '澳白好喝吗？我一般喝美式'),
(3012, 2005, 1001, '唐山宴yyds，外地朋友必带'),
(3013, 2005, 1004, '棋子烧饼哪家窗口最正宗？'),
(3014, 2006, 1006, '冬天喝羊汤是对冬天最基本的尊重'),
(3015, 2007, 1003, '室内夜市好评，夏天不怕热冬天不怕冷'),
(3016, 2008, 1001, '1号门桥上机位+1，出片');

-- 7. 演示点赞/收藏行为（user_action：type 1=赞 2=收藏）
INSERT INTO user_action (id, user_id, content_id, type) VALUES
(5001, 1004, 2001, 1),
(5002, 1,    2001, 1),
(5003, 1003, 2002, 1),
(5004, 1006, 2001, 1),
(5005, 1002, 2008, 1),
(5006, 1005, 2001, 1),
(5007, 1003, 2005, 1),
(5008, 1006, 2005, 1),
(6001, 1004, 2001, 2),
(6002, 1004, 2005, 2),
(6003, 1,    2002, 2),
(6004, 1006, 2001, 2);

-- 8. user 表加个人简介列（幂等）+ 演示简介
SET @e2 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA='dianping' AND TABLE_NAME='user' AND COLUMN_NAME='bio');
SET @d2 := IF(@e2=0,
    'ALTER TABLE `user` ADD COLUMN bio VARCHAR(100) NOT NULL DEFAULT '''' AFTER avatar',
    'SELECT 1');
PREPARE s2 FROM @d2; EXECUTE s2; DEALLOCATE PREPARE s2;
UPDATE `user` SET bio='唐山本地人，只推真好吃的' WHERE id=1001;
UPDATE `user` SET bio='探店五年，人均30也能吃出仪式感' WHERE id=1002;
UPDATE `user` SET bio='带娃和拍照是我的主业' WHERE id=1003;
UPDATE `user` SET bio='咖啡续命选手' WHERE id=1004;
UPDATE `user` SET bio='唐山活地图，问路找我' WHERE id=1005;

-- 9. comment 表支持回复（幂等）
SET @e3 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA='dianping' AND TABLE_NAME='comment' AND COLUMN_NAME='parent_id');
SET @d3 := IF(@e3=0,
    'ALTER TABLE comment ADD COLUMN parent_id BIGINT NOT NULL DEFAULT 0, ADD COLUMN reply_to_user_id BIGINT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE s3 FROM @d3; EXECUTE s3; DEALLOCATE PREPARE s3;

-- 10. 社交闭环：follow 关注表 / notify 消息表 / comment_like 评论点赞表
CREATE TABLE IF NOT EXISTS `follow` (
    `id`             BIGINT      NOT NULL,
    `user_id`        BIGINT      NOT NULL COMMENT '粉丝',
    `follow_user_id` BIGINT      NOT NULL COMMENT '被关注人',
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follow` (`user_id`, `follow_user_id`),
    KEY `idx_followed` (`follow_user_id`)
) COMMENT '关注关系';

CREATE TABLE IF NOT EXISTS `notify` (
    `id`          BIGINT       NOT NULL,
    `user_id`     BIGINT       NOT NULL COMMENT '接收人',
    `type`        VARCHAR(20)  NOT NULL COMMENT 'LIKE/COMMENT/REPLY/FOLLOW/FAV/AUDIT_PASS/AUDIT_REJECT',
    `actor_id`    BIGINT       NOT NULL DEFAULT 0 COMMENT '触发人，0=系统',
    `content_id`  BIGINT       NOT NULL DEFAULT 0,
    `comment_id`  BIGINT       NOT NULL DEFAULT 0,
    `text`        VARCHAR(200) NOT NULL DEFAULT '',
    `is_read`     TINYINT      NOT NULL DEFAULT 0,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) COMMENT '站内消息';

CREATE TABLE IF NOT EXISTS `comment_like` (
    `id`         BIGINT   NOT NULL,
    `comment_id` BIGINT   NOT NULL,
    `user_id`    BIGINT   NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cmt_like` (`comment_id`, `user_id`)
) COMMENT '评论点赞';

-- comment 表加点赞计数列（幂等）
SET @e4 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA='dianping' AND TABLE_NAME='comment' AND COLUMN_NAME='like_count');
SET @d4 := IF(@e4=0,
    'ALTER TABLE comment ADD COLUMN like_count INT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE s4 FROM @d4; EXECUTE s4; DEALLOCATE PREPARE s4;

-- 演示关注：小美(1006)和豆豆(1004)关注老王(1001)；小美关注本地通(1005)
INSERT IGNORE INTO follow (id, user_id, follow_user_id) VALUES
(7001, 1006, 1001),
(7002, 1004, 1001),
(7003, 1006, 1005);

-- 演示消息：老王收到 赞/评论/收藏/关注，小美收到审核系统通知
INSERT IGNORE INTO notify (id, user_id, type, actor_id, content_id, comment_id, text, is_read) VALUES
(8001, 1001, 'LIKE',    1006, 2001, 0,    '', 0),
(8002, 1001, 'COMMENT', 1004, 2001, 3001, '看完直接冲了，烤腰子确实一绝！', 0),
(8003, 1001, 'FAV',     1004, 2001, 0,    '', 1),
(8004, 1001, 'FOLLOW',  1006, 0,    0,    '', 0),
(8005, 1006, 'AUDIT_PASS', 0, 2001, 0,    '唐山这家老味烧烤，本地人从小吃到大', 1);
