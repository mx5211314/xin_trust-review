package com.dianping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentReq(
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 500, message = "评论最多 500 字")
        String text,
        /** 回复的评论 id，0/不传 = 顶级评论 */
        Long parentId) {
}
