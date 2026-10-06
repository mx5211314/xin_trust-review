package com.dianping;

import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.module.content.Content;
import com.dianping.module.content.ContentAccess;
import com.dianping.module.content.ContentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 内容可见性规则。这是详情/评论/点赞/收藏共用的**唯一判定入口**，
 * 规则错一处，四个入口会一起错，所以单独覆盖。
 *
 * 注意这里测的是**真实规则**（只 mock 掉 Mapper），
 * 不是在 LikeServiceTest 里模拟可见性 —— 那样等于测试 mock 的行为。
 */
@ExtendWith(MockitoExtension.class)
class ContentAccessTest {

    @Mock
    ContentMapper contentMapper;

    @InjectMocks
    ContentAccess contentAccess;

    private static final long AUTHOR = 100L;
    private static final long STRANGER = 200L;
    private static final long ADMIN = 999L;

    private Content content(String status, Long ownerId) {
        Content c = new Content();
        c.setId(2001L);
        c.setStatus(status);
        c.setUserId(ownerId);
        return c;
    }

    // ---------- canSee：纯判定 ----------

    @Test
    @DisplayName("APPROVED：任何人可见")
    void approvedVisibleToAll() {
        assertTrue(contentAccess.canSee(content("APPROVED", AUTHOR), STRANGER));
        assertTrue(contentAccess.canSee(content("APPROVED", AUTHOR), AUTHOR));
    }

    @Test
    @DisplayName("PENDING / REJECTED：仅作者可见（作者要能回看审核中/被驳回）")
    void pendingRejectedOnlyAuthor() {
        assertTrue(contentAccess.canSee(content("PENDING", AUTHOR), AUTHOR));
        assertTrue(contentAccess.canSee(content("REJECTED", AUTHOR), AUTHOR));
        assertFalse(contentAccess.canSee(content("PENDING", AUTHOR), STRANGER));
        assertFalse(contentAccess.canSee(content("REJECTED", AUTHOR), STRANGER));
    }

    @Test
    @DisplayName("TAKEN_DOWN：连作者也不可见（下架是平台处罚，不是作者权限）")
    void takenDownInvisibleToAuthor() {
        assertFalse(contentAccess.canSee(content("TAKEN_DOWN", AUTHOR), AUTHOR));
        assertFalse(contentAccess.canSee(content("TAKEN_DOWN", AUTHOR), STRANGER));
    }

    @Test
    @DisplayName("未知状态：白名单式拒绝（将来新增状态默认安全，不会意外放行）")
    void unknownStatusDeniedByDefault() {
        assertFalse(contentAccess.canSee(content("SOME_FUTURE_STATUS", AUTHOR), AUTHOR));
    }

    @Test
    @DisplayName("匿名（未登录）：APPROVED 可见，非公开内容不可见")
    void anonymousSeesOnlyApproved() {
        // 断言依据：规则先看状态、再看"是不是作者"。
        // APPROVED 是公开内容，本来就该公开 —— 匿名可见是**有意为之**，不是漏判；
        // 实际链路上 /content/** 走登录拦截，me 不会为空，这里只是把语义钉死。
        assertTrue(contentAccess.canSee(content("APPROVED", AUTHOR), null));
        // 非公开内容依赖 me 与作者比对，me 为空必然拿不到
        assertFalse(contentAccess.canSee(content("PENDING", AUTHOR), null));
        assertFalse(contentAccess.canSee(content("REJECTED", AUTHOR), null));
    }

    @Test
    @DisplayName("内容为 null：不可见（不 NPE）")
    void nullContentIsInvisible() {
        assertFalse(contentAccess.canSee(null, AUTHOR));
    }

    // ---------- require：带异常 ----------

    @Test
    @DisplayName("不可见抛 NOT_FOUND 而不是 FORBIDDEN（不泄露内容存在性）")
    void invisibleThrowsNotFoundNotForbidden() {
        when(contentMapper.selectById(2001L)).thenReturn(content("TAKEN_DOWN", AUTHOR));
        BizException e = assertThrows(BizException.class, () -> contentAccess.require(STRANGER, 2001L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), e.getCode().getCode());
    }

    @Test
    @DisplayName("内容不存在：抛 NOT_FOUND")
    void missingThrowsNotFound() {
        when(contentMapper.selectById(9999L)).thenReturn(null);
        BizException e = assertThrows(BizException.class, () -> contentAccess.require(STRANGER, 9999L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), e.getCode().getCode());
    }

    // ---------- 管理员放行（审核场景）----------

    @Test
    @DisplayName("管理员可放行不可见内容（审核页要看下架/待审笔记及其评论）")
    void adminBypassesVisibility() {
        when(contentMapper.selectById(2001L)).thenReturn(content("TAKEN_DOWN", AUTHOR));
        assertDoesNotThrow(() -> contentAccess.require(ADMIN, 2001L, true));
    }

    @Test
    @DisplayName("管理员也不能看不存在的内容（放行的只是可见性，不是存在性）")
    void adminStillCannotSeeMissing() {
        when(contentMapper.selectById(9999L)).thenReturn(null);
        BizException e = assertThrows(BizException.class,
                () -> contentAccess.require(ADMIN, 9999L, true));
        assertEquals(ResultCode.NOT_FOUND.getCode(), e.getCode().getCode());
    }

    @Test
    @DisplayName("非管理员不享受放行")
    void nonAdminDoesNotBypass() {
        when(contentMapper.selectById(2001L)).thenReturn(content("TAKEN_DOWN", AUTHOR));
        assertThrows(BizException.class, () -> contentAccess.require(STRANGER, 2001L, false));
    }
}
