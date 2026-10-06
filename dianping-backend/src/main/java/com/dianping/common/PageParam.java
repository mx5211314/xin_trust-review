package com.dianping.common;

/**
 * 分页参数归一化 —— **所有分页入口都必须先过这里**。
 *
 * 为什么必须有：page / pageSize 来自 URL query，是**完全不可信的外部输入**。
 * 不校验会出两类问题：
 *   1. pageSize 为负 → 算出的偏移量/长度变成负数 → subList / LIMIT 抛异常 → 5000
 *   2. pageSize 过大 → 一次把整表查出来，既是性能风险，也是个"拖库"入口
 *
 * 采取**收敛（clamp）而不是报错**的策略：
 * 分页越界属于客户端笔误，直接把参数夹到合法区间即可；
 * 报 1001 反而会把一个能正常返回的请求变成失败，对调用方更不友好。
 * （代价是客户端拿到的条数可能少于自己传的 pageSize —— 这是有意为之，
 *   上限必须由服务端说了算，不能由调用方指定。）
 */
public final class PageParam {

    /** 不传 / 非法时的每页条数 */
    public static final int DEFAULT_SIZE = 20;

    /** 服务端硬上限：任何分页接口都不允许一次取超过这个数 */
    public static final int MAX_SIZE = 100;

    private PageParam() {
    }

    /** 页码：小于 1 一律按第 1 页处理 */
    public static int page(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    /** 每页条数：小于 1 用默认值，超过上限夹到上限 */
    public static int size(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
