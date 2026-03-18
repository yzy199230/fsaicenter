package com.fsa.aicenter.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 加密工具类
 * <p>
 * 使用 AES-256-GCM 算法加密上游 Provider 的 API Key。
 * GCM 模式提供认证加密（AEAD），确保数据完整性和机密性。
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
public class ApiKeyEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    private final SecretKey secretKey;

    /**
     * 构造函数
     * <p>
     * 从配置文件读取加密密钥（Base64编码）。
     * 如果未配置，则生成随机密钥（仅用于开发环境）。
     * </p>
     *
     * @param encryptionKey Base64编码的256位密钥
     */
    public ApiKeyEncryptor(@Value("${security.api-key.encryption-key:}") String encryptionKey) {
        SecretKey key;
        if (encryptionKey == null || encryptionKey.isBlank()) {
            log.warn("未配置加密密钥，使用随机生成的密钥（仅用于开发环境）");
            key = generateRandomKey();
        } else {
            try {
                byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);
                if (decodedKey.length != 32) {
                    throw new IllegalArgumentException("加密密钥必须是256位（32字节）");
                }
                key = new SecretKeySpec(decodedKey, ALGORITHM);
                log.info("加密密钥加载成功");
            } catch (Exception e) {
                log.error("加密密钥加载失败，使用随机密钥", e);
                key = generateRandomKey();
            }
        }
        this.secretKey = key;
    }

    /**
     * 加密 API Key
     *
     * @param plaintext 明文 API Key
     * @return Base64编码的密文（包含IV）
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("待加密的API Key不能为空");
        }

        try {
            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // 初始化 Cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            // 加密
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 组合 IV + 密文
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            // Base64 编码
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            log.error("API Key 加密失败", e);
            throw new RuntimeException("API Key 加密失败", e);
        }
    }

    /**
     * 解密 API Key
     *
     * @param ciphertext Base64编码的密文（包含IV）
     * @return 明文 API Key
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new IllegalArgumentException("待解密的密文不能为空");
        }

        try {
            // Base64 解码
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            // 分离 IV 和密文
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] ciphertextBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertextBytes);

            // 初始化 Cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            // 解密
            byte[] plaintext = cipher.doFinal(ciphertextBytes);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("API Key 解密失败", e);
            throw new RuntimeException("API Key 解密失败", e);
        }
    }

    /**
     * 生成随机 AES-256 密钥
     *
     * @return SecretKey
     */
    private SecretKey generateRandomKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256, new SecureRandom());
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("生成随机密钥失败", e);
        }
    }

    /**
     * 生成 Base64 编码的随机密钥（用于配置文件）
     * <p>
     * 运行此方法生成密钥，然后配置到 application.yml 中。
     * </p>
     */
    public static String generateKeyForConfig() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256, new SecureRandom());
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("生成配置密钥失败", e);
        }
    }

    /**
     * 主方法：生成加密密钥
     * <p>
     * 运行此方法生成密钥，输出到控制台。
     * </p>
     */
    public static void main(String[] args) {
        String key = generateKeyForConfig();
        System.out.println("生成的加密密钥（请配置到 application.yml）：");
        System.out.println(key);
        System.out.println("\n配置示例：");
        System.out.println("security:");
        System.out.println("  api-key:");
        System.out.println("    encryption-key: " + key);
    }
}
