package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.ModelPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模型Mapper接口
 */
@Mapper
public interface ModelMapper extends BaseMapper<ModelPO> {

    /**
     * 根据模型代码查询
     */
    ModelPO selectByCode(@Param("modelCode") String modelCode);

    /**
     * 根据模型类型查询
     */
    List<ModelPO> selectByType(@Param("modelType") String modelType);

    /**
     * 根据模型类型和状态查询（启用的模型）
     */
    List<ModelPO> selectEnabledByType(@Param("modelType") String modelType, @Param("status") Integer status);

    /**
     * 条件查询模型列表
     */
    List<ModelPO> selectByCondition(@Param("keyword") String keyword,
                                    @Param("modelType") String modelType,
                                    @Param("providerId") Long providerId,
                                    @Param("status") Integer status);
}
