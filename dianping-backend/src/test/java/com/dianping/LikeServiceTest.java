package com.dianping;

import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.module.content.Content;
import com.dianping.module.content.ContentAccess;
import com.dianping.module.content.ContentMapper;
import com.dianping.module.content.LikeService;
import com.dianping.module.interaction.UserAction;
import com.dianping.module.interaction.UserActionMapper;
import com.dianping.module.notify.NotifyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 点赞核心规则：存在校验 / 幂等 / 通知触发 */
@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    ContentMapper contentMapper;
    @Mock
    UserActionMapper userActionMapper;
    @Mock
    StringRedisTemplate redis;
    @Mock
    NotifyService notifyService;
    /**
     * 可见性判定已被收敛到 ContentAccess，这里 mock 掉。
     * 本测试只关心"点赞自身的规则"，可见性规则由 ContentAccessTest 覆盖 ——
     * 在这里模拟可见性只会变成"测试 mock 的行为"，没有意义。
     */
    @Mock
    ContentAccess contentAccess;

    @InjectMocks
    LikeService likeService;

    private Content content(long ownerId) {
        Content c = new Content();
        c.setId(2001L);
        c.setUserId(ownerId);
        return c;
    }

    @Test
    @DisplayName("首次点赞：写入关系 + 通知作者")
    void firstLike() {
        // 点赞入口改为 contentAccess.require（可见性校验），不再直接查 ContentMapper
        when(contentAccess.require(1006L, 2001L)).thenReturn(content(100L));
        when(userActionMapper.selectCount(any())).thenReturn(0L);
        when(userActionMapper.insert(any(UserAction.class))).thenReturn(1);

        likeService.like(1006L, 2001L);

        verify(userActionMapper).insert(any(UserAction.class));
        verify(notifyService).send(eq(100L), anyString(), eq(1006L), eq(2001L), eq(0L), anyString());
    }

    @Test
    @DisplayName("重复点赞：返回 2003，不再写关系、不再通知")
    void duplicateLike() {
        when(contentAccess.require(1006L, 2001L)).thenReturn(content(100L));
        when(userActionMapper.selectCount(any())).thenReturn(1L);

        BizException e = assertThrows(BizException.class, () -> likeService.like(1006L, 2001L));
        assertEquals(ResultCode.DUPLICATE_ACTION.getCode(), e.getCode().getCode());
        verify(userActionMapper, never()).insert(any(UserAction.class));
        verify(notifyService, never()).send(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("内容不存在：返回 1004")
    void likeMissingContent() {
        // ContentAccess 对"不存在"和"不可见"都抛 NOT_FOUND（不泄露存在性）
        when(contentAccess.require(1L, 9999L)).thenThrow(new BizException(ResultCode.NOT_FOUND));

        BizException e = assertThrows(BizException.class, () -> likeService.like(1L, 9999L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), e.getCode().getCode());
        verify(userActionMapper, never()).insert(any(UserAction.class));
    }

    @Test
    @DisplayName("内容不可见（下架/待审且非作者）：同样返回 1004，不得写入点赞")
    void likeInvisibleContent() {
        // 这条用例正是本次新增可见性校验的意义：
        // 修复前点赞只判"存在"，能给下架笔记点赞
        when(contentAccess.require(1006L, 2001L)).thenThrow(new BizException(ResultCode.NOT_FOUND));

        BizException e = assertThrows(BizException.class, () -> likeService.like(1006L, 2001L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), e.getCode().getCode());
        verify(userActionMapper, never()).insert(any(UserAction.class));
        verify(notifyService, never()).send(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString());
    }
}
