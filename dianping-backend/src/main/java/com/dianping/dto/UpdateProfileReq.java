package com.dianping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileReq(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 20, message = "昵称最多 20 字")
        String nickname,
        @Size(max = 300, message = "头像地址过长")
        String avatar,
        @Size(max = 100, message = "简介最多 100 字")
        String bio) {
}
