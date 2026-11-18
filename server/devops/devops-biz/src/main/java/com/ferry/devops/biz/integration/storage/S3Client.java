package com.ferry.devops.biz.integration.storage;

/**
 * 对象存储摘要反查客户端。
 *
 * <p>接入点：生产环境需对接 S3 兼容对象存储，用 HeadObject 读取对象 metadata 中的 sha256
 * （包制品）或用 Registry HEAD manifest 读取 Docker-Content-Digest（镜像制品），
 * 与 Jenkins 回传摘要比对，确认对象存在且未被覆盖。预签名用于生成受控下载链接。
 *
 * <p>当前为本域最小实现：不引入 aws sdk 依赖（父 pom 有 BOM 但本地未必缓存），
 * 以简单接口占位，默认返回 true / 空串，避免编译与运行期强依赖。
 */
public interface S3Client {

    /**
     * 摘要反查：对象存在且 metadata 摘要一致返回 true。
     *
     * @param digest sha256 摘要
     * @return 是否存在
     */
    boolean digestExists(String digest);

    /**
     * 生成对象的预签名访问地址。占位实现返回空串。
     *
     * @param digest 摘要
     * @return 预签名地址
     */
    String presign(String digest);
}
