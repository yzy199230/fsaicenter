package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.ApiKeyModelAccessPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * API密钥模型访问权限Mapper接口
 */
@Mapper
public interface ApiKeyModelAccessMapper extends BaseMapper<ApiKeyModelAccessPO> {

    @Select("SELECT * FROM api_key_model_access WHERE api_key_id = #{apiKeyId} AND is_deleted = 0")
    List<ApiKeyModelAccessPO> selectByApiKeyId(@Param("apiKeyId") Long apiKeyId);

    @Update("UPDATE api_key_model_access SET is_deleted = 1 WHERE api_key_id = #{apiKeyId} AND model_id = #{modelId} AND is_deleted = 0")
    void deleteByApiKeyIdAndModelId(@Param("apiKeyId") Long apiKeyId, @Param("modelId") Long modelId);

    @Update("UPDATE api_key_model_access SET is_deleted = 1 WHERE api_key_id = #{apiKeyId} AND is_deleted = 0")
    void deleteAllByApiKeyId(@Param("apiKeyId") Long apiKeyId);

    @Select("SELECT * FROM api_key_model_access WHERE api_key_id = #{apiKeyId} AND model_id = #{modelId} LIMIT 1")
    ApiKeyModelAccessPO selectOneIgnoreLogicDelete(@Param("apiKeyId") Long apiKeyId, @Param("modelId") Long modelId);

    @Update("UPDATE api_key_model_access SET access_type = #{accessType}, is_deleted = 0 WHERE id = #{id}")
    void restoreAndUpdate(@Param("id") Long id, @Param("accessType") Integer accessType);
}
