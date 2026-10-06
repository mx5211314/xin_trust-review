package com.dianping.module.poi;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店（POI）。
 *
 * 存在的意义：把门店从"一段文本"变成"一个实体"。
 * 详见 sql/init.sql 里 poi 表的注释、以及仓库根目录《门店体系改造方案_v0.1.md》。
 */
@Data
@TableName("poi")
public class Poi {

    // ---- status 取值 ----
    /** 正常可用 */
    public static final String STATUS_NORMAL = "NORMAL";
    /** 待审核（点评人新建的门店先进这个状态，审核通过才对他人可见） */
    public static final String STATUS_PENDING = "PENDING";
    /** 审核未通过 */
    public static final String STATUS_REJECTED = "REJECTED";
    /** 已被合并到其它门店（不删记录，用 merged_to 指向正主） */
    public static final String STATUS_MERGED = "MERGED";
    /** 停业 */
    public static final String STATUS_CLOSED = "CLOSED";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private String address;
    private BigDecimal lng;
    private BigDecimal lat;
    private String category;
    private String regionCode;
    /** 规范化城市码：按城市聚合门店用。与地区筛选同一思路——把要匹配的前缀提前算好存下来 */
    private String cityCode;
    private String phone;
    private String openHours;
    private String status;
    /** 被合并时指向"正主"门店 id；保留记录以便回滚与追溯 */
    private Long mergedTo;
    private Long createdBy;
    /** 关联笔记数（冗余，榜单排序用） */
    private Integer contentCount;
    /** 综合评分（预留，打分功能未做） */
    private BigDecimal score;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
