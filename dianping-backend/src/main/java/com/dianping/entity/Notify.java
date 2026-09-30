package com.dianping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notify")
public class Notify {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    Long userId;
    /** LIKE/COMMENT/REPLY/FOLLOW/FAV/AUDIT_PASS/AUDIT_REJECT */
    String type;
    /** 触发人，0=系统 */
    Long actorId;
    Long contentId;
    Long commentId;
    String text;
    Integer isRead;
    private LocalDateTime createTime;
}
