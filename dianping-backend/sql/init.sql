-- ============================================================
-- 本地点评平台 · 完整建表脚本（幂等，可重复执行）
-- 用法：mysql -uroot -p < init.sql
-- 说明：本文件是唯一的 schema 来源（含全部 8 张表与索引）；
--       demo-data.sql 只负责演示数据，不含建表。
-- ============================================================

CREATE DATABASE IF NOT EXISTS dianping DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE dianping;

-- ---------- 用户 ----------
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL COMMENT '雪花ID',
    `phone`       VARCHAR(20)  NOT NULL COMMENT '手机号',
    `nickname`    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '头像URL',
    `bio`         VARCHAR(100) NOT NULL DEFAULT '' COMMENT '个人简介',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER/REVIEWER/ADMIN',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/BANNED',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) COMMENT '用户';

-- ---------- 内容（点评）----------
CREATE TABLE IF NOT EXISTS `content` (
    `id`            BIGINT        NOT NULL COMMENT '雪花ID',
    `user_id`       BIGINT        NOT NULL COMMENT '发布者ID',
    `title`         VARCHAR(64)   NOT NULL COMMENT '标题',
    `text`          VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '正文',
    `images`        VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '图片key，JSON数组',
    `tags`          VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '话题标签，JSON数组',
    `video_key`     VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '视频key/URL，空表示图文',
    `cover_key`     VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '视频封面key',
    `duration`      INT           NOT NULL DEFAULT 0 COMMENT '视频秒数',
    `region_code`   VARCHAR(20)   NOT NULL DEFAULT '' COMMENT '地区码',
    `poi_name`      VARCHAR(100)  NOT NULL DEFAULT '' COMMENT '店铺/地点名',
    `status`        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/TAKEN_DOWN',
    `reject_reason` VARCHAR(200)  NOT NULL DEFAULT '' COMMENT '驳回原因',
    `like_count`    INT           NOT NULL DEFAULT 0 COMMENT '点赞数（定时按 user_action 重算）',
    `view_count`    INT           NOT NULL DEFAULT 0 COMMENT '浏览数',
    `audit_time`    DATETIME      NULL COMMENT '审核时间',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_status_region_time` (`status`, `region_code`, `create_time`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status_time` (`status`, `create_time`)
) COMMENT '内容';

-- ---------- 用户行为（点赞/收藏关系，唯一索引天然幂等）----------
CREATE TABLE IF NOT EXISTS `user_action` (
    `id`          BIGINT   NOT NULL COMMENT '雪花ID',
    `user_id`     BIGINT   NOT NULL,
    `content_id`  BIGINT   NOT NULL,
    `type`        TINYINT  NOT NULL DEFAULT 1 COMMENT '1=点赞 2=收藏',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_content_type` (`user_id`, `content_id`, `type`),
    KEY `idx_content` (`content_id`)
) COMMENT '用户行为';

-- ---------- 评论（支持楼中楼：parent_id=0 为顶级评论）----------
CREATE TABLE IF NOT EXISTS `comment` (
    `id`               BIGINT       NOT NULL COMMENT '雪花ID',
    `content_id`       BIGINT       NOT NULL,
    `user_id`          BIGINT       NOT NULL,
    `text`             VARCHAR(500) NOT NULL,
    `parent_id`        BIGINT       NOT NULL DEFAULT 0 COMMENT '0=顶级评论；其他=所属根评论id',
    `reply_to_user_id` BIGINT       NOT NULL DEFAULT 0 COMMENT '被回复人（显示"回复 @xx"）',
    `like_count`       INT          NOT NULL DEFAULT 0 COMMENT '评论点赞数',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_content_parent` (`content_id`, `parent_id`),
    KEY `idx_content` (`content_id`)
) COMMENT '评论';

-- ---------- 评论点赞 ----------
CREATE TABLE IF NOT EXISTS `comment_like` (
    `id`          BIGINT   NOT NULL,
    `comment_id`  BIGINT   NOT NULL,
    `user_id`     BIGINT   NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cmt_like` (`comment_id`, `user_id`)
) COMMENT '评论点赞';

-- ---------- 关注 ----------
CREATE TABLE IF NOT EXISTS `follow` (
    `id`             BIGINT   NOT NULL,
    `user_id`        BIGINT   NOT NULL COMMENT '粉丝',
    `follow_user_id` BIGINT   NOT NULL COMMENT '被关注人',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follow` (`user_id`, `follow_user_id`),
    KEY `idx_followed` (`follow_user_id`)
) COMMENT '关注关系';

-- ---------- 站内消息（互动 / 审核结果 / 管理员公告）----------
CREATE TABLE IF NOT EXISTS `notify` (
    `id`          BIGINT       NOT NULL,
    `user_id`     BIGINT       NOT NULL COMMENT '接收人',
    `type`        VARCHAR(20)  NOT NULL COMMENT 'LIKE/COMMENT/REPLY/FOLLOW/FAV/MESSAGE/AUDIT_PASS/AUDIT_REJECT/ANNOUNCE',
    `actor_id`    BIGINT       NOT NULL DEFAULT 0 COMMENT '触发人，0=系统/管理员',
    `content_id`  BIGINT       NOT NULL DEFAULT 0,
    `comment_id`  BIGINT       NOT NULL DEFAULT 0,
    `text`        VARCHAR(200) NOT NULL DEFAULT '',
    `is_read`     TINYINT      NOT NULL DEFAULT 0,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_user_type` (`user_id`, `type`)
) COMMENT '站内消息';

-- ---------- 私信 ----------
CREATE TABLE IF NOT EXISTS `message` (
    `id`           BIGINT       NOT NULL,
    `from_user_id` BIGINT       NOT NULL,
    `to_user_id`   BIGINT       NOT NULL,
    `text`         VARCHAR(500) NOT NULL,
    `is_read`      TINYINT      NOT NULL DEFAULT 0,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_pair` (`from_user_id`, `to_user_id`),
    KEY `idx_to_read` (`to_user_id`, `is_read`)
) COMMENT '私信';

-- ---------- 已有库的增量补列（幂等；新库由上方建表语句直接包含）----------
SET @e1 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA='dianping' AND TABLE_NAME='user' AND COLUMN_NAME='bio');
SET @d1 := IF(@e1=0, 'ALTER TABLE `user` ADD COLUMN bio VARCHAR(100) NOT NULL DEFAULT '''' AFTER avatar', 'SELECT 1');
PREPARE s1 FROM @d1; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @e2 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA='dianping' AND TABLE_NAME='content' AND COLUMN_NAME='tags');
SET @d2 := IF(@e2=0, 'ALTER TABLE content ADD COLUMN tags VARCHAR(500) NOT NULL DEFAULT '''' AFTER images', 'SELECT 1');
PREPARE s2 FROM @d2; EXECUTE s2; DEALLOCATE PREPARE s2;

-- ---------- 初始管理员（dev 环境验证码固定 8888）----------
INSERT INTO `user` (`id`, `phone`, `nickname`, `role`)
VALUES (1, '13800000000', '管理员', 'ADMIN')
ON DUPLICATE KEY UPDATE `role` = 'ADMIN';
