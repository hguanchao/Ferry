package com.ferry.devops.biz.integration.storage;

import org.springframework.stereotype.Component;

/**
 * S3Client 最小占位实现。
 *
 * <p>接入 AWS SDK v2（S3Client.headObject 读 metadata.sha256、s3Presigner.presignGetObject）
 * 或 OCI Registry HEAD manifest 时，替换本类实现即可，无需改动调用方。
 * 默认 {@link #digestExists(String)} 返回 true，{@link #presign(String)} 返回空串。
 */
@Component
public class S3ClientImpl implements S3Client {

    @Override
    public boolean digestExists(String digest) {
        // 接入点：HeadObject 读 metadata 摘要并与 digest 比对，失败/不一致返回 false。
        return true;
    }

    @Override
    public String presign(String digest) {
        // 接入点：生成预签名下载 URL；占位实现返回空串。
        return "";
    }
}
