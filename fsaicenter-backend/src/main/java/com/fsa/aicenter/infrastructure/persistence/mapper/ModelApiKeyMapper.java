package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.ModelApiKeyPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 模型API Key Mapper
 *
 * @author FSA AI Center
 */
@Mapper
public interface ModelApiKeyMapper extends BaseMapper<ModelApiKeyPO> {

    /**
     * 记录成功请求
     */
    @Update("UPDATE model_api_key SET " +
            "total_requests = total_requests + 1, " +
            "success_requests = success_requests + 1, " +
            "last_used_time = #{now}, " +
            "last_success_time = #{now}, " +
            "fail_count = 0, " +
            "health_status = 1 " +
            "WHERE id = #{id}")
    void recordSuccess(@Param("id") Long id, @Param("now") java.time.LocalDateTime now);

    /**
     * 记录失败请求
     */
    @Update("UPDATE model_api_key SET " +
            "total_requests = total_requests + 1, " +
            "failed_requests = failed_requests + 1, " +
            "last_used_time = #{now}, " +
            "last_fail_time = #{now}, " +
            "fail_count = fail_count + 1, " +
            "health_status = CASE WHEN fail_count + 1 >= 5 THEN 0 ELSE health_status END " +
            "WHERE id = #{id}")
    void recordFailure(@Param("id") Long id, @Param("now") java.time.LocalDateTime now);

    /**
     * 消费配额
     */
    @Update("UPDATE model_api_key SET " +
            "quota_used = quota_used + #{amount} " +
            "WHERE id = #{id} AND (quota_total = -1 OR quota_used + #{amount} <= quota_total)")
    int consumeQuota(@Param("id") Long id, @Param("amount") long amount);
}
