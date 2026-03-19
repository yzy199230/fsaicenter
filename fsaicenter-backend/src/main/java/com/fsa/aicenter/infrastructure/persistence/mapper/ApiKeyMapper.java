package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.ApiKeyPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * API密钥Mapper接口
 */
@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKeyPO> {

    @Select("SELECT * FROM api_key WHERE key_value = #{keyValue} AND is_deleted = 0")
    ApiKeyPO selectByKeyValue(String keyValue);

    @Select("SELECT * FROM api_key WHERE status = 1 AND is_deleted = 0 AND (expire_time IS NULL OR expire_time > CURRENT_TIMESTAMP)")
    List<ApiKeyPO> selectActiveKeys();
}
