package com.dianping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectReq(
        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 200, message = "驳回原因最多 200 字")
        String reason) {
}
