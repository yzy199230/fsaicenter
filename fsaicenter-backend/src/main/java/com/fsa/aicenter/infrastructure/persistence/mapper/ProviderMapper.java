package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.ProviderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 提供商Mapper接口
 */
@Mapper
public interface ProviderMapper extends BaseMapper<ProviderPO> {

    /**
     * 根据提供商代码查询
     */
    ProviderPO selectByCode(@Param("providerCode") String providerCode);

    /**
     * 查询所有启用的提供商
     */
    List<ProviderPO> selectEnabled(@Param("status") Integer status);
}
