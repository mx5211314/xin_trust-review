package com.dianping.module.comment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long contentId;
    private Long userId;
    private String text;
    /** 0 = 顶级评论；其他 = 被回复的评论 id */
    private Long parentId;
    /** 被回复的用户 id（用于前端显示"回复 @xx"） */
    private Long replyToUserId;
    /** 评论点赞数 */
    private Integer likeCount;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
