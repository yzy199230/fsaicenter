package com.fsa.aicenter.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiKeyEncryptor 单元测试
 */
@DisplayName("API Key 加密工具测试")
class ApiKeyEncryptorTest {

    private ApiKeyEncryptor encryptor;

    @BeforeEach
    void setUp() {
        // 使用空配置，会生成随机密钥
        encryptor = new ApiKeyEncryptor("");
    }

    @Test
    @DisplayName("加密解密 - 成功场景")
    void testEncryptDecrypt_Success() {
        // Given
        String plaintext = "sk-1234567890abcdefghijklmnopqrstuvwxyz";

        // When
        String encrypted = encryptor.encrypt(plaintext);
        String decrypted = encryptor.decrypt(encrypted);

        // Then
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted); // 密文不等于明文
        assertEquals(plaintext, decrypted);    // 解密后等于明文
    }

    @Test
    @DisplayName("加密 - 空字符串抛出异常")
    void testEncrypt_EmptyString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptor.encrypt("");
        });
    }

    @Test
    @DisplayName("加密 - null 抛出异常")
    void testEncrypt_Null_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptor.encrypt(null);
        });
    }

    @Test
    @DisplayName("解密 - 空字符串抛出异常")
    void testDecrypt_EmptyString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptor.decrypt("");
        });
    }

    @Test
    @DisplayName("解密 - null 抛出异常")
    void testDecrypt_Null_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptor.decrypt(null);
        });
    }

    @Test
    @DisplayName("解密 - 无效密文抛出异常")
    void testDecrypt_InvalidCiphertext_ThrowsException() {
        assertThrows(RuntimeException.class, () -> {
            encryptor.decrypt("invalid-base64-string");
        });
    }

    @Test
    @DisplayName("多次加密同一明文 - 密文不同（IV随机）")
    void testEncrypt_SamePlaintext_DifferentCiphertext() {
        // Given
        String plaintext = "sk-test-api-key";

        // When
        String encrypted1 = encryptor.encrypt(plaintext);
        String encrypted2 = encryptor.encrypt(plaintext);

        // Then
        assertNotEquals(encrypted1, encrypted2); // 由于 IV 随机，密文不同
        assertEquals(plaintext, encryptor.decrypt(encrypted1));
        assertEquals(plaintext, encryptor.decrypt(encrypted2));
    }

    @Test
    @DisplayName("加密长字符串")
    void testEncrypt_LongString() {
        // Given
        String plaintext = "sk-" + "a".repeat(500); // 500+ 字符

        // When
        String encrypted = encryptor.encrypt(plaintext);
        String decrypted = encryptor.decrypt(encrypted);

        // Then
        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("加密包含特殊字符的字符串")
    void testEncrypt_SpecialCharacters() {
        // Given
        String plaintext = "sk-!@#$%^&*()_+-=[]{}|;':\",./<>?`~";

        // When
        String encrypted = encryptor.encrypt(plaintext);
        String decrypted = encryptor.decrypt(encrypted);

        // Then
        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("加密包含中文的字符串")
    void testEncrypt_ChineseCharacters() {
        // Given
        String plaintext = "sk-测试密钥-1234";

        // When
        String encrypted = encryptor.encrypt(plaintext);
        String decrypted = encryptor.decrypt(encrypted);

        // Then
        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("生成配置密钥 - 格式正确")
    void testGenerateKeyForConfig() {
        // When
        String key = ApiKeyEncryptor.generateKeyForConfig();

        // Then
        assertNotNull(key);
        assertFalse(key.isBlank());
        // Base64 编码的 256 位密钥应该是 44 个字符（32 字节 * 4/3 ≈ 43-44）
        assertTrue(key.length() >= 40);
    }

    @Test
    @DisplayName("使用配置密钥创建加密器")
    void testEncryptor_WithConfigKey() {
        // Given
        String configKey = ApiKeyEncryptor.generateKeyForConfig();
        ApiKeyEncryptor encryptorWithKey = new ApiKeyEncryptor(configKey);
        String plaintext = "sk-test-key";

        // When
        String encrypted = encryptorWithKey.encrypt(plaintext);
        String decrypted = encryptorWithKey.decrypt(encrypted);

        // Then
        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("不同加密器实例 - 无法互相解密（密钥不同）")
    void testEncrypt_DifferentEncryptors_CannotDecrypt() {
        // Given
        ApiKeyEncryptor encryptor1 = new ApiKeyEncryptor("");
        ApiKeyEncryptor encryptor2 = new ApiKeyEncryptor("");
        String plaintext = "sk-test-key";

        // When
        String encrypted = encryptor1.encrypt(plaintext);

        // Then
        assertThrows(RuntimeException.class, () -> {
            encryptor2.decrypt(encrypted); // 不同密钥，无法解密
        });
    }

    @Test
    @DisplayName("相同配置密钥的加密器 - 可以互相解密")
    void testEncrypt_SameConfigKey_CanDecrypt() {
        // Given
        String configKey = ApiKeyEncryptor.generateKeyForConfig();
        ApiKeyEncryptor encryptor1 = new ApiKeyEncryptor(configKey);
        ApiKeyEncryptor encryptor2 = new ApiKeyEncryptor(configKey);
        String plaintext = "sk-test-key";

        // When
        String encrypted = encryptor1.encrypt(plaintext);
        String decrypted = encryptor2.decrypt(encrypted);

        // Then
        assertEquals(plaintext, decrypted);
    }
}
