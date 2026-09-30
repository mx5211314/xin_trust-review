package com.dianping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment_like")
public class CommentLike {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    Long commentId;
    Long userId;
    private LocalDateTime createTime;
}
