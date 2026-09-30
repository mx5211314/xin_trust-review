package com.dianping.common;

import lombok.Getter;

/**
 * 业务错误码，与接口契约文档 v0.1 的错误码表一一对应。
 */
@Getter
public enum ResultCode {
    OK(0, "ok"),
    PARAM_ERROR(1001, "参数错误"),
    UNAUTHORIZED(1002, "请先登录"),
    FORBIDDEN(1003, "没有权限"),
    NOT_FOUND(1004, "内容不存在"),
    CONTENT_ILLEGAL(2001, "内容含违规信息"),
    AUDITING(2002, "内容审核中，请稍后"),
    DUPLICATE_ACTION(2003, "重复操作"),
    SYSTEM_ERROR(5000, "系统繁忙，请稍后重试");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
