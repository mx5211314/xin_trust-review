package com.dianping.module.content;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容状态机：PENDING -> APPROVED -> TAKEN_DOWN
 *                      -> REJECTED
 */
@Data
@TableName("content")
public class Content {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String title;
    private String text;
    /** 图片 key 的 JSON 数组字符串 */
    private String images;
    /** 话题标签的 JSON 数组字符串，如 ["唐山美食","探店"] */
    private String tags;
    private String videoKey;
    private String coverKey;
    private Integer duration;
    private String regionCode;
    private String poiName;
    /** PENDING / APPROVED / REJECTED / TAKEN_DOWN */
    private String status;
    private String rejectReason;
    private Integer likeCount;
    private Integer viewCount;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
