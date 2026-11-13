package com.ferry.common.core.utils;

import com.ferry.common.core.exception.UtilException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 加密工具类
 *
 * <p>提供三类能力：
 * <ul>
 *   <li>密码：BCrypt 加密与校验（自带随机盐，密文 60 位）</li>
 *   <li>对称加密：AES 加解密（用于令牌等短数据）</li>
 *   <li>摘要：SHA-256（用于版本快照的内容比对）</li>
 * </ul>
 */
public final class CryptoUtil {

    private static final String AES = "AES";
    private static final String AES_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String SHA_256 = "SHA-256";
    private static final String BCRYPT_PREFIX = "$2";
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private CryptoUtil() {
    }

    // ==================== 密码 ====================

    /**
     * BCrypt 哈希密码
     *
     * @param raw 明文密码
     * @return String BCrypt 哈希
     */
    public static String bcryptEncode(String raw) {
        if (StrUtil.isBlank(raw)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return PASSWORD_ENCODER.encode(raw);
    }

    /**
     * BCrypt 校验密码
     *
     * @param raw    明文密码
     * @param encoded 数据库中的哈希
     * @return boolean 是否匹配
     */
    public static boolean bcryptMatches(String raw, String encoded) {
        if (StrUtil.isBlank(raw) || StrUtil.isBlank(encoded)) {
            return false;
        }
        // 非 BCrypt 密文（历史 MD5/SHA1 数据）一律不通过，需重置密码
        if (!encoded.startsWith(BCRYPT_PREFIX)) {
            return false;
        }
        return PASSWORD_ENCODER.matches(raw, encoded);
    }

    // ==================== 摘要 ====================

    /**
     * 计算字符串的 SHA-256 摘要
     *
     * @param content 待计算内容，空字符串合法
     * @return 小写十六进制摘要，长度 64
     */
    public static String sha256Hex(String content) {
        if (content == null) {
            throw new IllegalArgumentException("待计算内容不能为空");
        }
        try {
            byte[] digest = MessageDigest.getInstance(SHA_256).digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new UtilException("SHA-256计算错误", e);
        }
    }

    // ==================== AES ====================

    /**
     * AES 加密
     *
     * @param content 明文
     * @param key     密钥（长度必须为 16/24/32 字节）
     * @return String Base64 密文
     */
    public static String aesEncrypt(String content, String key) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES));
            byte[] cipherText = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new UtilException("AES加密错误", e);
        }
    }

    /**
     * AES 解密
     *
     * @param content Base64 密文
     * @param key     密钥（长度必须为 16/24/32 字节）
     * @return String 明文
     */
    public static String aesDecrypt(String content, String key) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES));
            byte[] cipherText = cipher.doFinal(Base64.getDecoder().decode(content));
            return new String(cipherText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new UtilException("AES解密错误", e);
        }
    }
}
