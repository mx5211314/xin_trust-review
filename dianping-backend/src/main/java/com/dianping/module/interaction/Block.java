package com.dianping.module.interaction;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("block")
public class Block {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 拉黑发起方 */
    private Long userId;
    /** 被拉黑方 */
    private Long blockedId;
    private LocalDateTime createTime;
}
