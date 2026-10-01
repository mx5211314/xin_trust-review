package com.dianping.module.wechat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订阅消息授权额度（微信一次性订阅：用户每授权一次，可下发一条）。
 * 仅作本地节流记账；真实额度以微信服务器为准。
 */
@Data
@TableName("subscribe_grant")
public class SubscribeGrant {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    /** 模板业务键：audit / interact（映射到配置里的模板 ID） */
    private String templateKey;
    private Integer grantCount;
    private LocalDateTime updateTime;
}
