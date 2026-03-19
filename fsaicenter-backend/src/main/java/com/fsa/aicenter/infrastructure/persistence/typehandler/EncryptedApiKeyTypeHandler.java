package com.fsa.aicenter.infrastructure.persistence.typehandler;

import com.fsa.aicenter.common.util.ApiKeyEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 加密 API Key 类型处理器
 * <p>
 * MyBatis Plus 自定义类型处理器，自动加解密数据库中的 API Key 字段。
 * 写入数据库时自动加密，读取时自动解密。
 * </p>
 *
 * 使用方式：
 * <pre>
 * {@code
 * @TableField(value = "api_key", typeHandler = EncryptedApiKeyTypeHandler.class)
 * private String apiKey;
 * }
 * </pre>
 *
 * 注意：不能添加 @Component，否则 MyBatis-Plus 会将 BaseTypeHandler<String> 自动注册为
 * 所有 String 类型的全局 TypeHandler，导致 username 等普通字段也被加密。
 * 此 TypeHandler 仅通过 @TableField 显式指定使用，由 MyBatis 通过反射实例化。
 * 加密器通过 EncryptedApiKeyTypeHandler.initEncryptor() 静态方法注入。
 *
 * @author FSA AI Center
 */
@Slf4j
public class EncryptedApiKeyTypeHandler extends BaseTypeHandler<String> {

    private static ApiKeyEncryptor encryptor;

    /**
     * 初始化加密器（由配置类调用）
     */
    public static void initEncryptor(ApiKeyEncryptor apiKeyEncryptor) {
        encryptor = apiKeyEncryptor;
        log.info("EncryptedApiKeyTypeHandler 加密器初始化完成");
    }

    /**
     * 设置非空参数（加密后写入数据库）
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        if (encryptor == null) {
            log.warn("加密器未初始化，API Key 将以明文存储");
            ps.setString(i, parameter);
            return;
        }

        try {
            String encrypted = encryptor.encrypt(parameter);
            ps.setString(i, encrypted);
            log.debug("API Key 已加密存储");
        } catch (Exception e) {
            log.error("API Key 加密失败，将以明文存储", e);
            ps.setString(i, parameter);
        }
    }

    /**
     * 获取可空结果（从数据库读取并解密）
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String encrypted = rs.getString(columnName);
        return decrypt(encrypted);
    }

    /**
     * 获取可空结果（从数据库读取并解密）
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String encrypted = rs.getString(columnIndex);
        return decrypt(encrypted);
    }

    /**
     * 获取可空结果（从存储过程读取并解密）
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String encrypted = cs.getString(columnIndex);
        return decrypt(encrypted);
    }

    /**
     * 解密辅助方法
     */
    private String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return encrypted;
        }

        if (encryptor == null) {
            log.warn("加密器未初始化，返回原始值");
            return encrypted;
        }

        try {
            return encryptor.decrypt(encrypted);
        } catch (Exception e) {
            log.error("API Key 解密失败，返回原始值", e);
            return encrypted;
        }
    }
}
