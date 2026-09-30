package com.dianping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("follow")
public class Follow {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 粉丝 */
    Long userId;
    /** 被关注人 */
    Long followUserId;
    private LocalDateTime createTime;
}
