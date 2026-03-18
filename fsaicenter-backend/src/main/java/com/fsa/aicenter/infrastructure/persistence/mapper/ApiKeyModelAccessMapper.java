package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.ApiKeyModelAccessPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * API密钥模型访问权限Mapper接口
 */
@Mapper
public interface ApiKeyModelAccessMapper extends BaseMapper<ApiKeyModelAccessPO> {

    /**
     * 根据API密钥ID查询所有访问权限
     */
    List<ApiKeyModelAccessPO> selectByApiKeyId(@Param("apiKeyId") Long apiKeyId);

    /**
     * 删除指定API密钥和模型的访问权限
     */
    void deleteByApiKeyIdAndModelId(@Param("apiKeyId") Long apiKeyId, @Param("modelId") Long modelId);
}
