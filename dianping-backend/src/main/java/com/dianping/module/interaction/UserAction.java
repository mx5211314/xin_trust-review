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
    /** 收藏所属收藏夹 id（0 = 未分类）；仅 type=FAV 时使用 */
    private Long folderId;
    private LocalDateTime createTime;

    public static final int TYPE_LIKE = 1;
    public static final int TYPE_FAV = 2;
    /** 3 = 浏览（进入详情页即记一条，覆盖式去重，供"浏览记录"时间线） */
    public static final int TYPE_VIEW = 3;
}
