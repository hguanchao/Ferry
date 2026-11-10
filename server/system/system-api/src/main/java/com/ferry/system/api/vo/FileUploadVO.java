package com.ferry.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class FileUploadVO implements Serializable {

    @Schema(description = "文件访问地址")
    private String url;

    @Schema(description = "文件相对路径")
    private String relativePath;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "存储文件名")
    private String storedName;

    @Schema(description = "文件大小，单位字节")
    private Long size;

    @Schema(description = "文件内容类型")
    private String contentType;
}
