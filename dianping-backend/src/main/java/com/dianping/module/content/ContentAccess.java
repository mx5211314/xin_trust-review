package com.dianping.module.content;

import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 内容可见性的**唯一判定入口**。
 *
 * 为什么要单独抽一个类：原来详情、评论读取、评论发布、点赞、收藏各写各的，
 * 结果详情拦住了下架笔记，评论接口却能照样读写同一条内容 —— 同一份规则两套口径。
 * 规则一旦分散就必然漂移，所以统一收在这里，新增入口只要调它就够了。
 *
 * 判定规则（白名单式，未知状态默认不可见）：
 *   APPROVED           任何登录用户可见
 *   PENDING / REJECTED 仅作者本人可见（作者要能回看"审核中 / 被驳回"）
 *   其他未知状态        一律不可见（将来新增状态默认安全，不会意外放行）
 *
 * 不可见时抛 NOT_FOUND 而不是 FORBIDDEN：不向外泄露"这条内容存在、只是不属于你"，
 * 与原有 detail 的行为保持一致。
 *
 * 注意：本类只依赖 Mapper，**不依赖 ContentService** ——
 * CommentService 需要用它，而 ContentService 已经依赖 CommentService，
 * 若把规则写进 ContentService 会形成循环依赖。
 */
@Component
@RequiredArgsConstructor
public class ContentAccess {

    private final ContentMapper contentMapper;

    /** 取内容并校验可见性；不存在或不可见都抛 NOT_FOUND */
    public Content require(Long me, Long contentId) {
        return require(me, contentId, false);
    }

    /**
     * 同上，但允许**管理员审核场景**放行不可见内容。
     *
     * 为什么需要这个口子：审核页要能看到下架/待审笔记的评论，
     * 否则「管理员查看违规内容」这条链路又断在评论上 ——
     * 收紧可见性时最容易顺手把管理员一起挡住。
     *
     * 注意：放行的只是**可见性**，内容不存在依然 404（管理员也不能凭空看不存在的东西）。
     * 写操作（评论/点赞/收藏）不要传 true —— 管理员同样不该给不可见内容刷互动。
     */
    public Content require(Long me, Long contentId, boolean isAdmin) {
        Content c = contentMapper.selectById(contentId);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!isAdmin && !canSee(c, me)) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return c;
    }

    /** 纯判定，不抛异常（如列表过滤、条件渲染等场景） */
    public boolean canSee(Content c, Long me) {
        if (c == null) {
            return false;
        }
        String st = c.getStatus();
        if ("APPROVED".equals(st)) {
            return true;
        }
        if (!"PENDING".equals(st) && !"REJECTED".equals(st)) {
            return false;
        }
        return me != null && me.equals(c.getUserId());
    }
}
