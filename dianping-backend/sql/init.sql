-- 点评平台 MVP 建表脚本
-- 执行：mysql -uroot -p < init.sql

CREATE DATABASE IF NOT EXISTS dianping DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE dianping;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL COMMENT '雪花ID',
    `phone`       VARCHAR(20)  NOT NULL COMMENT '手机号',
    `nickname`    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '头像URL',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER/REVIEWER/ADMIN',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/BANNED',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) COMMENT '用户';

-- 内容表（点评）
CREATE TABLE IF NOT EXISTS `content` (
    `id`           BIGINT       NOT NULL COMMENT '雪花ID',
    `user_id`      BIGINT       NOT NULL COMMENT '发布者ID',
    `title`        VARCHAR(64)  NOT NULL COMMENT '标题',
    `text`         VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '正文',
    `images`       VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '图片key，JSON数组',
    `video_key`    VARCHAR(500) NOT NULL DEFAULT '' COMMENT '视频key，空表示图文',
    `cover_key`    VARCHAR(500) NOT NULL DEFAULT '' COMMENT '视频封面key',
    `duration`     INT          NOT NULL DEFAULT 0 COMMENT '视频秒数',
    `region_code`  VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '地区码',
    `poi_name`     VARCHAR(100) NOT NULL DEFAULT '' COMMENT '店铺/地点名',
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/TAKEN_DOWN',
    `reject_reason` VARCHAR(200) NOT NULL DEFAULT '' COMMENT '驳回原因',
    `like_count`   INT          NOT NULL DEFAULT 0 COMMENT '点赞数(定时从Redis同步)',
    `view_count`   INT          NOT NULL DEFAULT 0 COMMENT '浏览数',
    `audit_time`   DATETIME     NULL COMMENT '审核时间',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_status_region_time` (`status`, `region_code`, `create_time`),
    KEY `idx_user` (`user_id`)
) COMMENT '内容';

-- 用户行为表（点赞关系，唯一索引天然幂等）
CREATE TABLE IF NOT EXISTS `user_action` (
    `id`          BIGINT   NOT NULL COMMENT '雪花ID',
    `user_id`     BIGINT   NOT NULL,
    `content_id`  BIGINT   NOT NULL,
    `type`        TINYINT  NOT NULL DEFAULT 1 COMMENT '1=点赞',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_content_type` (`user_id`, `content_id`, `type`),
    KEY `idx_content` (`content_id`)
) COMMENT '用户行为';

-- 初始管理员（手机号登录，dev 环境验证码固定 8888）
INSERT INTO `user` (`id`, `phone`, `nickname`, `role`)
VALUES (1, '13800000000', '管理员', 'ADMIN')
ON DUPLICATE KEY UPDATE `role` = 'ADMIN';
