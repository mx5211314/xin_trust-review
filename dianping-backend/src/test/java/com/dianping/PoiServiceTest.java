package com.dianping;

import com.dianping.common.BizException;
import com.dianping.module.poi.Poi;
import com.dianping.module.poi.PoiCreateReq;
import com.dianping.module.poi.PoiMapper;
import com.dianping.module.poi.PoiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 门店规则。重点覆盖三处最容易做错的地方（都写在 PoiService 类注释里）：
 *   1. 谁能建店 / 建成什么状态
 *   2. 待审门店的可见性（仅创建者）
 *   3. 不可用状态的门店不许被关联
 */
@ExtendWith(MockitoExtension.class)
class PoiServiceTest {

    @Mock
    PoiMapper poiMapper;
    /** 迁移/重算计数用；本测试不覆盖那两条路径，但构造注入必须给上，否则 @InjectMocks 造不出实例 */
    @Mock
    com.dianping.module.content.ContentMapper contentMapper;

    @InjectMocks
    PoiService poiService;

    private static final long REVIEWER = 1001L;
    private static final long ADMIN = 1L;
    private static final long OTHER = 2002L;

    private Poi poi(Long id, String status, Long creator) {
        Poi p = new Poi();
        p.setId(id);
        p.setName("老王烧烤");
        p.setStatus(status);
        p.setCreatedBy(creator);
        p.setContentCount(0);
        return p;
    }

    private PoiCreateReq req(String name, String regionCode) {
        return new PoiCreateReq(name, "", null, null, "", regionCode, "", "");
    }

    // ---------- 建店状态 ----------

    @Test
    @DisplayName("点评人建店：进 PENDING 待审（门店是公共资产，不能随手就对所有人可见）")
    void reviewerCreatesPending() {
        when(poiMapper.selectOne(any())).thenReturn(null);
        when(poiMapper.insert(any(Poi.class))).thenAnswer(inv -> {
            Poi p = inv.getArgument(0);
            p.setId(8801L);
            return 1;
        });

        Long id = poiService.create(REVIEWER, false, req("老王烧烤", "130200"));

        assertNotNull(id);
        // 用 ArgumentCaptor 而不是 argThat：BaseMapper.insert 有重载，
        // argThat 推断不出类型会有歧义（实际编译报错过）
        ArgumentCaptor<Poi> cap = ArgumentCaptor.forClass(Poi.class);
        verify(poiMapper).insert(cap.capture());
        Poi saved = cap.getValue();
        assertEquals(Poi.STATUS_PENDING, saved.getStatus(), "点评人建店应进待审");
        assertEquals("1302", saved.getCityCode(), "130200 应归到市级前缀 1302");
        assertEquals(REVIEWER, saved.getCreatedBy());
    }

    @Test
    @DisplayName("管理员建店：直接 NORMAL 可用")
    void adminCreatesNormal() {
        when(poiMapper.selectOne(any())).thenReturn(null);
        when(poiMapper.insert(any(Poi.class))).thenAnswer(inv -> {
            inv.getArgument(0, Poi.class).setId(8802L);
            return 1;
        });

        poiService.create(ADMIN, true, req("某某博物馆", "130202"));

        ArgumentCaptor<Poi> cap = ArgumentCaptor.forClass(Poi.class);
        verify(poiMapper).insert(cap.capture());
        assertEquals(Poi.STATUS_NORMAL, cap.getValue().getStatus(), "管理员建店应直接可用");
    }

    @Test
    @DisplayName("同名同城已存在：复用而不新建（最廉价的一道防脏）")
    void reuseExistingSameNameAndCity() {
        Poi existing = poi(8801L, Poi.STATUS_NORMAL, ADMIN);
        when(poiMapper.selectOne(any())).thenReturn(existing);

        Long id = poiService.create(REVIEWER, false, req("老王烧烤", "130200"));

        assertEquals(8801L, id);
        verify(poiMapper, never()).insert(any(Poi.class));
    }

    // ---------- 可见性 ----------

    @Test
    @DisplayName("NORMAL 门店：所有人可见")
    void normalVisibleToAll() {
        assertTrue(poiService.canSee(poi(1L, Poi.STATUS_NORMAL, ADMIN), OTHER));
    }

    @Test
    @DisplayName("PENDING 门店：仅创建者可见（否则未审的脏数据会提前露给别人）")
    void pendingOnlyVisibleToCreator() {
        Poi p = poi(8801L, Poi.STATUS_PENDING, REVIEWER);
        assertTrue(poiService.canSee(p, REVIEWER));
        assertFalse(poiService.canSee(p, OTHER));
    }

    @Test
    @DisplayName("REJECTED / MERGED / CLOSED：一律不可见（白名单式，新增状态默认安全）")
    void otherStatusesInvisible() {
        for (String st : new String[]{Poi.STATUS_REJECTED, Poi.STATUS_MERGED, Poi.STATUS_CLOSED}) {
            assertFalse(poiService.canSee(poi(1L, st, REVIEWER), REVIEWER), st + " 不该可见");
        }
    }

    // ---------- 关联校验 ----------

    @Test
    @DisplayName("关联：门店已被合并时，返回的是正主 id（不能挂到废记录上）")
    void requireLinkableFollowsMergedTo() {
        Poi merged = poi(8801L, Poi.STATUS_MERGED, ADMIN);
        merged.setMergedTo(8809L);
        Poi target = poi(8809L, Poi.STATUS_NORMAL, ADMIN);
        when(poiMapper.selectById(8801L)).thenReturn(merged);
        when(poiMapper.selectById(8809L)).thenReturn(target);

        assertEquals(8809L, poiService.requireLinkable(REVIEWER, 8801L));
    }

    @Test
    @DisplayName("关联：已驳回/已停业门店不可关联")
    void requireLinkableRejectsUnusable() {
        when(poiMapper.selectById(8801L)).thenReturn(poi(8801L, Poi.STATUS_REJECTED, ADMIN));
        BizException e = assertThrows(BizException.class,
                () -> poiService.requireLinkable(REVIEWER, 8801L));
        assertEquals("该门店不可关联", e.getMessage());
    }

    @Test
    @DisplayName("关联：别人的待审门店不能关联（只有创建者能用自己的待审店）")
    void requireLinkableRejectsOthersPending() {
        when(poiMapper.selectById(8801L)).thenReturn(poi(8801L, Poi.STATUS_PENDING, OTHER));
        assertThrows(BizException.class, () -> poiService.requireLinkable(REVIEWER, 8801L));
    }

    @Test
    @DisplayName("关联：门店不存在时给出参数错误，而不是让它挂 null 进去")
    void requireLinkableMissing() {
        when(poiMapper.selectById(9999L)).thenReturn(null);
        BizException e = assertThrows(BizException.class,
                () -> poiService.requireLinkable(REVIEWER, 9999L));
        assertEquals("门店不存在", e.getMessage());
    }
}
