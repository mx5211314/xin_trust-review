package com.dianping.module.poi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新建门店。
 *
 * 只有 `name` 必填 —— 用户一开始可能只知道店名（地址、电话常是后面补的）。
 * 强制填全等于逼用户放弃建店，反而助长"随便选一个已有的"。
 * regionCode 用于推导 cityCode，选填。
 */
public record PoiCreateReq(
        @NotBlank(message = "门店名不能为空")
        @Size(max = 100, message = "门店名最多 100 字")
        String name,

        @Size(max = 200, message = "地址最多 200 字")
        String address,

        Double lng,
        Double lat,

        @Size(max = 30, message = "分类过长")
        String category,

        String regionCode,

        @Size(max = 30, message = "电话过长")
        String phone,

        @Size(max = 100, message = "营业时间过长")
        String openHours) {
}
