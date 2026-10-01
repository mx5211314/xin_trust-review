package com.dianping;

import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.module.content.Content;
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
        when(contentMapper.selectById(2001L)).thenReturn(content(100L));
        when(userActionMapper.selectCount(any())).thenReturn(0L);
        when(userActionMapper.insert(any(UserAction.class))).thenReturn(1);

        likeService.like(1006L, 2001L);

        verify(userActionMapper).insert(any(UserAction.class));
        verify(notifyService).send(eq(100L), anyString(), eq(1006L), eq(2001L), eq(0L), anyString());
    }

    @Test
    @DisplayName("重复点赞：返回 2003，不再写关系、不再通知")
    void duplicateLike() {
        when(contentMapper.selectById(2001L)).thenReturn(content(100L));
        when(userActionMapper.selectCount(any())).thenReturn(1L);

        BizException e = assertThrows(BizException.class, () -> likeService.like(1006L, 2001L));
        assertEquals(ResultCode.DUPLICATE_ACTION.getCode(), e.getCode().getCode());
        verify(userActionMapper, never()).insert(any(UserAction.class));
        verify(notifyService, never()).send(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("内容不存在：返回 404")
    void likeMissingContent() {
        when(contentMapper.selectById(9999L)).thenReturn(null);
        BizException e = assertThrows(BizException.class, () -> likeService.like(1L, 9999L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), e.getCode().getCode());
    }
}
