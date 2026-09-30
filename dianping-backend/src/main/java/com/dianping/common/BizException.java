package com.dianping.common;

import lombok.Getter;

/**
 * 业务异常：msg 会直接透出给客户端展示，所以文案要写用户能看懂的话。
 */
@Getter
public class BizException extends RuntimeException {
    private final ResultCode code;

    public BizException(ResultCode code) {
        super(code.getMsg());
        this.code = code;
    }

    public BizException(ResultCode code, String msg) {
        super(msg);
        this.code = code;
    }
}
