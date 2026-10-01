package com.dianping.module.interaction;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_action")
public class UserAction {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long contentId;
    /** 1 = 点赞 */
    private Integer type;
    private LocalDateTime createTime;

    public static final int TYPE_LIKE = 1;
    public static final int TYPE_FAV = 2;
}
