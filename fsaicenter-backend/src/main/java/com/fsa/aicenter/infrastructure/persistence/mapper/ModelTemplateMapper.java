package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.ModelTemplatePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ModelTemplateMapper extends BaseMapper<ModelTemplatePO> {

    /**
     * 按条件查询模板列表
     */
    @Select("<script>" +
            "SELECT * FROM model_template WHERE 1=1" +
            "<if test='providerCode != null'> AND provider_code = #{providerCode}</if>" +
            "<if test='type != null'> AND type = #{type}</if>" +
            "<if test='source != null'> AND source = #{source}</if>" +
            " ORDER BY source DESC, provider_code, code" +
            "</script>")
    List<ModelTemplatePO> selectByConditions(
            @Param("providerCode") String providerCode,
            @Param("type") String type,
            @Param("source") String source
    );

    /**
     * 根据提供商代码和模板代码查询
     */
    @Select("SELECT * FROM model_template WHERE provider_code = #{providerCode} AND code = #{code}")
    ModelTemplatePO selectByProviderAndCode(
            @Param("providerCode") String providerCode,
            @Param("code") String code
    );
}
