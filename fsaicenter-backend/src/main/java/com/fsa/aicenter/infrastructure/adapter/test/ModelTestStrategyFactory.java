package com.fsa.aicenter.infrastructure.adapter.test;

import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模型测试策略工厂
 *
 * @author FSA AI Center
 */
@Component
public class ModelTestStrategyFactory {

    private final Map<ModelType, ModelTestStrategy> strategies;

    public ModelTestStrategyFactory(List<ModelTestStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        ModelTestStrategy::getSupportedType,
                        Function.identity()
                ));
    }

    /**
     * 获取指定类型的测试策略
     *
     * @param type 模型类型
     * @return 测试策略
     */
    public ModelTestStrategy getStrategy(ModelType type) {
        ModelTestStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.MODEL_TYPE_NOT_SUPPORTED);
        }
        return strategy;
    }
}
