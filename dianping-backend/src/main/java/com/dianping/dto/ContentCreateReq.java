package com.dianping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 发布点评。图文必填 images；视频必填 videoKey + coverKey。
 * key 来自 GET /oss/upload-params 返回的 key（后端生成）。
 */
public record ContentCreateReq(
        @NotBlank(message = "标题不能为空")
        @Size(max = 30, message = "标题最多 30 字")
        String title,

        @NotBlank(message = "正文不能为空")
        @Size(max = 2000, message = "正文最多 2000 字")
        String text,

        @Size(max = 9, message = "图片最多 9 张")
        List<String> images,

        String videoKey,
        String coverKey,
        Integer duration,

        @NotNull(message = "地区不能为空")
        String regionCode,

        String poiName,

        @Size(max = 5, message = "话题最多 5 个")
        List<String> tags) {
}
