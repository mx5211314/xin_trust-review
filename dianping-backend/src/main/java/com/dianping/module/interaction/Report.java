package com.dianping.module.interaction;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 举报人 */
    private Long reporterId;
    /** 举报对象类型：CONTENT / COMMENT / USER */
    private String targetType;
    /** 举报对象 id（USER 时为被举报用户 id） */
    private Long targetId;
    /** 举报原因 */
    private String reason;
    /** 处理状态：PENDING / HANDLED / IGNORED */
    private String status;
    private LocalDateTime createTime;
}
