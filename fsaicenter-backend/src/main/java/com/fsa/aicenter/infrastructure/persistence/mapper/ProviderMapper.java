package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.ProviderPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 提供商Mapper接口
 */
@Mapper
public interface ProviderMapper extends BaseMapper<ProviderPO> {

    @Select("SELECT * FROM ai_provider WHERE provider_code = #{providerCode} AND is_deleted = 0")
    ProviderPO selectByCode(@Param("providerCode") String providerCode);

    @Select("SELECT * FROM ai_provider WHERE status = #{status} AND is_deleted = 0 ORDER BY sort_order")
    List<ProviderPO> selectEnabled(@Param("status") Integer status);
}
