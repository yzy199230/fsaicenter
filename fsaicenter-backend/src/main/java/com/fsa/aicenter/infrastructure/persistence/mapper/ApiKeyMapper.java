package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.ApiKeyPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * API密钥Mapper接口
 */
@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKeyPO> {

    /**
     * 根据密钥值查询
     */
    ApiKeyPO selectByKeyValue(String keyValue);

    /**
     * 查询所有启用的密钥
     */
    List<ApiKeyPO> selectActiveKeys();
}
