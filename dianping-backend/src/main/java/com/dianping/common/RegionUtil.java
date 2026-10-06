package com.dianping.common;

/**
 * 行政区划码工具。
 *
 * 编码规则（GB/T 2260）：省 = xx0000 · 市 = xxxx00 · 区县 = xxxxxx。
 *
 * 注意这里有两个**不同**的换算，别混用 —— 它们的方向是相反的：
 *
 *   - {@link #filterPrefix}：用户选了哪一级，就只看哪一级。
 *     选"路南区"应当只看到路南区的笔记，所以区县码原样返回。
 *   - {@link #cityCode}：这条内容/门店**归属哪个城市**。
 *     即便用户选的是路南区，发布到区县的笔记也要能在"唐山市"的聚合里出现，
 *     所以区县码要归到市级前缀。
 *
 * 混用的后果很具体：把 cityCode 的逻辑用到 feed 筛选上，
 * "筛选路南区"会变成"整个唐山市"；反过来则"按城市看"会漏掉所有区县内容。
 */
public final class RegionUtil {

    private RegionUtil() {
    }

    /**
     * 筛选前缀：按用户选择的层级保留精度。
     * <pre>
     *   130000（省）   → "13"     召回全省
     *   130200（市）   → "1302"   召回该市所有区县（含 130202）
     *   130202（区县） → "130202" 精确到该区县
     * </pre>
     * 非 6 位的输入（前端历史上可能传 "13" 这类短写）原样返回，保持向后兼容。
     * 空白返回 null，调用方据此跳过该条件。
     */
    public static String filterPrefix(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String c = code.trim();
        if (c.length() != 6) {
            return c;
        }
        if (c.endsWith("0000")) {
            return c.substring(0, 2);
        }
        if (c.endsWith("00")) {
            return c.substring(0, 4);
        }
        return c;
    }

    /**
     * 归属城市码：取市级前缀（4 位，如 "1302"），用于按城市聚合门店。
     * <pre>
     *   130200（唐山市） → "1302"
     *   130202（路南区） → "1302"   ← 区县归到所属城市
     *   130000（河北省） → null     ← 省级无法确定具体城市
     * </pre>
     * 返回 null 表示"无法归属到具体城市"，调用方应存空串，
     * 这类门店只会出现在不带城市筛选的列表里。
     */
    public static String cityCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String c = code.trim();
        if (c.length() != 6 || c.endsWith("0000")) {
            return null;
        }
        return c.substring(0, 4);
    }
}
