package com.dianping.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：保证任何异常都返回统一返回体，msg 可直接展示。
 * 语义对齐：资源不存在 -> 404 码；唯一索引冲突 -> 2003；上传超限 -> 参数错误码。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        // 业务异常也留痕：1002 未登录带 token 上传这类问题，靠这行日志才能定位
        log.warn("biz error: code={}, msg={}", e.getCode().getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? ResultCode.PARAM_ERROR.getMsg() : fe.getDefaultMessage();
        return R.fail(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 路径/查询参数**类型转换失败**：如 `?poiId=abc` 或 `?poiId=`（空串转 Long）。
     *
     * 这类问题原来是掉进兜底的 5000「系统繁忙」，把调用方传错参数说成服务端故障 ——
     * 排查时会被带偏（看到 5000 先去查服务端，实际是自己拼错了参数）。
     * 统一按参数错误返回，并说明是哪个参数。
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: name={}, value={}", e.getName(), e.getValue());
        return R.fail(ResultCode.PARAM_ERROR, "参数 " + e.getName() + " 格式不对");
    }

    /** 静态资源/路径不存在：返回 404 语义（不再是 5000，也不记 error 日志） */
    @ExceptionHandler(NoResourceFoundException.class)
    public R<Void> handleNoResource(NoResourceFoundException e) {
        log.warn("not found: {}", e.getResourcePath());
        return R.fail(ResultCode.NOT_FOUND);
    }

    /** 上传超限 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleUploadSize(MaxUploadSizeExceededException e) {
        return R.fail(ResultCode.PARAM_ERROR, "文件太大：图片不超过 10MB，视频不超过 100MB");
    }

    /** 唯一索引冲突（并发下的兜底）：重复点赞/关注/收藏统一返回 2003 */
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicate(DuplicateKeyException e) {
        log.warn("duplicate key: {}", e.getMessage());
        return R.fail(ResultCode.DUPLICATE_ACTION);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception e) {
        log.error("system error", e);
        return R.fail(ResultCode.SYSTEM_ERROR);
    }
}
