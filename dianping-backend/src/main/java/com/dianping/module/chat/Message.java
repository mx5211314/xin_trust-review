package com.dianping.module.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String text;
    /** 0 未读 1 已读 */
    private Integer isRead;
    private LocalDateTime createTime;
}
