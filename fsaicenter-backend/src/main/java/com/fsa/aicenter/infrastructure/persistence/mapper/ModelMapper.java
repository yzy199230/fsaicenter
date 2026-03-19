package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.ModelPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 模型Mapper接口
 */
@Mapper
public interface ModelMapper extends BaseMapper<ModelPO> {

    @Select("SELECT * FROM ai_model WHERE model_code = #{modelCode} AND is_deleted = 0")
    ModelPO selectByCode(@Param("modelCode") String modelCode);

    @Select("SELECT * FROM ai_model WHERE model_type = #{modelType} AND is_deleted = 0 ORDER BY sort_order")
    List<ModelPO> selectByType(@Param("modelType") String modelType);

    @Select("SELECT * FROM ai_model WHERE model_type = #{modelType} AND status = #{status} AND is_deleted = 0 ORDER BY sort_order")
    List<ModelPO> selectEnabledByType(@Param("modelType") String modelType, @Param("status") Integer status);

    @Select("<script>SELECT * FROM ai_model WHERE is_deleted = 0" +
            "<if test='keyword != null and keyword != \"\"'> AND (model_code LIKE CONCAT('%',#{keyword},'%') OR model_name LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test='modelType != null and modelType != \"\"'> AND model_type = #{modelType}</if>" +
            "<if test='providerId != null'> AND provider_id = #{providerId}</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            " ORDER BY sort_order</script>")
    List<ModelPO> selectByCondition(@Param("keyword") String keyword,
                                    @Param("modelType") String modelType,
                                    @Param("providerId") Long providerId,
                                    @Param("status") Integer status);
}
