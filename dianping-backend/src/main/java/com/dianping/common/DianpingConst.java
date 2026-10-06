package com.dianping.common;

/** 全局常量：角色 / 状态 / Redis key（消除魔法字符串，统一维护） */
public final class DianpingConst {

    private DianpingConst() {
    }

    // ---- 角色 ----
    public static final String ROLE_USER = "USER";
    public static final String ROLE_REVIEWER = "REVIEWER";
    public static final String ROLE_ADMIN = "ADMIN";

    // ---- 用户状态 ----
    public static final String USER_NORMAL = "NORMAL";
    public static final String USER_BANNED = "BANNED";

    // ---- 内容状态 ----
    public static final String CONTENT_PENDING = "PENDING";
    public static final String CONTENT_APPROVED = "APPROVED";
    public static final String CONTENT_REJECTED = "REJECTED";
    public static final String CONTENT_TAKEN_DOWN = "TAKEN_DOWN";

    // ---- Redis key ----
    /** 待落库的点赞计数内容集合（sync task 消费） */
    public static final String REDIS_LIKE_DIRTY = "like:dirty";
    /** 用户状态缓存：user:status:{userId} -> NORMAL/BANNED */
    public static final String REDIS_USER_STATUS = "user:status:";
    /** 限流前缀：rate:{biz}:{key} */
    public static final String REDIS_RATE = "rate:";
    /** user:status 缓存秒数 */
    public static final long USER_STATUS_TTL_SECONDS = 60;

    // ---- 短信验证码（短信码/错误次数/发送频率 三把 key）----
    /** 验证码本体：sms:code:{phone} -> 6 位码（带 TTL，过期即失效） */
    public static final String REDIS_SMS_CODE = "sms:code:";
    /** 错误次数：sms:attempt:{phone} -> 已错次数（用于防暴力枚举） */
    public static final String REDIS_SMS_ATTEMPT = "sms:attempt:";
    /** 发送频率锁：sms:sent:{phone}（SET NX EX，保证同号 N 秒内只发一次） */
    public static final String REDIS_SMS_SENT = "sms:sent:";
}
