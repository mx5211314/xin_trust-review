package com.dianping.common;

import lombok.Data;

/**
 * 统一返回体：{"code":0,"msg":"ok","data":{}}
 * 约定：code=0 成功；4xxx 业务错误；5xxx 系统错误。
 */
@Data
public class R<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = ResultCode.OK.getCode();
        r.msg = ResultCode.OK.getMsg();
        r.data = data;
        return r;
    }

    public static R<Void> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(ResultCode rc, String msg) {
        R<T> r = new R<>();
        r.code = rc.getCode();
        r.msg = msg == null ? rc.getMsg() : msg;
        return r;
    }

    public static <T> R<T> fail(ResultCode rc) {
        return fail(rc, null);
    }
}
