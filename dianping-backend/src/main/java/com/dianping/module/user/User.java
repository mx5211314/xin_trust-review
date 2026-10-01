package com.dianping.module.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    /** 个人简介 */
    private String bio;
    /** USER / REVIEWER / ADMIN */
    private String role;
    /** NORMAL / BANNED */
    private String status;
    /** 微信小程序 openid（微信登录/订阅消息用，未接入微信登录前为空） */
    private String openid;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
