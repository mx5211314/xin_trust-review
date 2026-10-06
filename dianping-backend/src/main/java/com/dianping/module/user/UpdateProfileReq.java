package com.dianping.module.user;

import jakarta.validation.constraints.Size;

/**
 * 编辑资料请求。
 *
 * **字段语义：null/不传 = 保持原值；空串 = 明确清空**（仅 bio 支持清空）。
 *
 * 注意 nickname 上**没有 @NotBlank**：本接口是局部更新，
 * 只想换头像时不该被迫带上昵称。但"昵称不能为空"这条业务规则依然存在，
 * 由调用方（前端保存前校验）与 updateProfile 的判空共同保证 ——
 * 传了昵称就必须非空，不传就保持原值。
 */
public record UpdateProfileReq(
        @Size(max = 20, message = "昵称最多 20 字")
        String nickname,
        @Size(max = 300, message = "头像地址过长")
        String avatar,
        @Size(max = 100, message = "简介最多 100 字")
        String bio) {
}
