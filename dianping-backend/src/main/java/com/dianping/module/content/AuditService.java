package com.dianping.module.content;

import com.dianping.module.content.Content;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 内容机审（占位实现）。
 *
 * 接真实审核的步骤（阿里云内容安全为例，约半天工作量）：
 * 1. 开通内容安全服务，拿 AccessKey（内容安全按量计费，约 1~2 元/千次）
 * 2. 引入依赖 aliyun-java-sdk-green，发布时同步调用文本+图片审核接口
 *    - 文本：TextModeration；图片：ImageModeration；视频：先截帧再走图片审核
 * 3. pass -> APPROVED；review/block -> 停 PENDING 转后台人工 或 直接 REJECTED
 * 4. 上线后保留本 mock 开关作为灰度兜底
 */
@Service
public class AuditService {

    @Value("${dianping.audit.mock-auto-pass}")
    private boolean mockAutoPass;

    /**
     * @return true=机审通过；false=转人工（状态停在 PENDING，等后台处理）
     */
    public boolean machinePass(Content content) {
        // TODO 接入真实内容安全 API，返回审核结论
        return mockAutoPass;
    }
}
