package com.dianping;

import com.dianping.common.RegionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行政区划码换算。
 *
 * 这两个方法**方向相反**，是最容易混用的地方 —— 我自己写方案时就差点搞反：
 *   · filterPrefix：用户选了哪一级就只看哪一级（区县原样）
 *   · cityCode：归属哪个城市（区县要归到市级）
 * 混用的后果很具体：把 cityCode 用到 feed 筛选上，"筛选路南区"会变成"整个唐山市"。
 * 所以这里把两边的预期都钉死。
 */
class RegionUtilTest {

    @Test
    @DisplayName("filterPrefix：按所选层级保留精度")
    void filterPrefixKeepsLevel() {
        assertEquals("13", RegionUtil.filterPrefix("130000"), "省码取前 2 位");
        assertEquals("1302", RegionUtil.filterPrefix("130200"), "市码取前 4 位");
        assertEquals("130202", RegionUtil.filterPrefix("130202"), "区县码原样，不能放大到全市");
    }

    @Test
    @DisplayName("filterPrefix：非 6 位输入原样返回（兼容前端短写省码）")
    void filterPrefixToleratesShortCode() {
        assertEquals("13", RegionUtil.filterPrefix("13"));
        assertNull(RegionUtil.filterPrefix(null));
        assertNull(RegionUtil.filterPrefix("   "));
    }

    @Test
    @DisplayName("cityCode：市码与区县码都归到同一个市级前缀")
    void cityCodeFoldsDistrictIntoCity() {
        assertEquals("1302", RegionUtil.cityCode("130200"));
        // 关键用例：定位发布到"路南区"的笔记，也要能出现在"唐山市"的聚合里
        assertEquals("1302", RegionUtil.cityCode("130202"));
    }

    @Test
    @DisplayName("cityCode：省级无法确定具体城市，返回 null")
    void cityCodeNullForProvince() {
        assertNull(RegionUtil.cityCode("130000"), "省级码定不了城市，不能硬凑一个");
        assertNull(RegionUtil.cityCode(null));
        assertNull(RegionUtil.cityCode("1302"), "非 6 位无法判断层级");
    }
}
