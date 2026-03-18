package com.fsa.aicenter.application.service;

import com.fsa.aicenter.common.exception.AllModelsFailedException;
import com.fsa.aicenter.common.exception.ModelNotFoundException;
import com.fsa.aicenter.common.exception.NoAvailableModelException;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.repository.ModelRepository;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.infrastructure.adapter.AdapterFactory;
import com.fsa.aicenter.infrastructure.adapter.common.AiProviderAdapter;
import com.fsa.aicenter.infrastructure.adapter.common.AiRequest;
import com.fsa.aicenter.infrastructure.adapter.common.AiResponse;
import com.fsa.aicenter.infrastructure.adapter.common.AiStreamChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * AI代理服务
 * <p>
 * 核心业务服务类，负责：
 * <ul>
 *   <li>模型选择：支持指定具体模型或按类型选择（使用降级策略）</li>
 *   <li>流式调用：调用AI提供商的流式接口</li>
 *   <li>非流式调用：调用AI提供商的非流式接口</li>
 *   <li>降级处理：当主模型失败时自动切换到备用模型</li>
 * </ul>
 * </p>
 *
 * @author FSA AI Center
 */
@Service
public class AiProxyService {

    private static final Logger log = LoggerFactory.getLogger(AiProxyService.class);

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private AdapterFactory adapterFactory;

    /**
     * 根据模型代码选择模型（不降级）
     * <p>
     * 当用户指定了具体的模型代码时，直接查询该模型。
     * 如果模型不存在或已禁用，直接抛出异常，不进行降级。
     * </p>
     *
     * @param modelCode 模型代码
     * @return AI模型
     * @throws ModelNotFoundException 当模型不存在时
     */
    public AiModel selectModelByCode(String modelCode) {
        if (modelCode == null || modelCode.isEmpty()) {
            throw new IllegalArgumentException("模型代码不能为空");
        }

        log.debug("选择指定模型: {}", modelCode);

        AiModel model = modelRepository.findByCode(modelCode)
                .orElseThrow(() -> new ModelNotFoundException(modelCode));

        if (!model.isEnabled()) {
            log.warn("模型已禁用: {}", modelCode);
            throw new ModelNotFoundException(modelCode);
        }

        log.info("成功选择模型: {} ({})", model.getName(), model.getCode());
        return model;
    }

    /**
     * 根据模型类型选择模型（支持降级）
     * <p>
     * 当用户只指定了模型类型时，查询该类型的所有启用模型，
     * 返回第一个作为主模型，后续模型作为降级备选。
     * </p>
     *
     * @param modelType 模型类型
     * @return AI模型列表（第一个为主模型，其余为备用模型）
     * @throws NoAvailableModelException 当该类型无可用模型时
     */
    public List<AiModel> selectModelsByType(ModelType modelType) {
        if (modelType == null) {
            throw new IllegalArgumentException("模型类型不能为空");
        }

        log.debug("查询类型为 {} 的可用模型", modelType.getDesc());

        List<AiModel> models = modelRepository.findEnabledByType(modelType);

        if (models.isEmpty()) {
            log.warn("该类型没有可用模型: {}", modelType.getDesc());
            throw new NoAvailableModelException(modelType);
        }

        log.info("找到 {} 个可用的 {} 模型: {}", models.size(), modelType.getDesc(),
                models.stream().map(AiModel::getCode).toList());

        return models;
    }

    /**
     * 非流式调用（指定模型代码，不降级）
     * <p>
     * 用户指定具体模型时，调用该模型的非流式接口。
     * 如果调用失败，直接返回错误，不进行降级。
     * </p>
     *
     * @param modelCode 模型代码
     * @param request   请求参数
     * @return 响应结果的Mono
     */
    public Mono<AiResponse> call(String modelCode, AiRequest request) {
        if (modelCode == null || modelCode.isEmpty()) {
            return Mono.error(new IllegalArgumentException("模型代码不能为空"));
        }

        log.info("开始非流式调用，模型: {}", modelCode);

        try {
            AiModel model = selectModelByCode(modelCode);
            AiProviderAdapter adapter = adapterFactory.getAdapter(model);

            return adapter.call(model, request)
                    .doOnSuccess(response -> log.info("非流式调用成功，模型: {}, tokens: {}",
                            modelCode, response.calculateTotalTokens()))
                    .doOnError(error -> log.error("非流式调用失败，模型: {}", modelCode, error));

        } catch (Exception e) {
            log.error("非流式调用异常，模型: {}", modelCode, e);
            return Mono.error(e);
        }
    }

    /**
     * 非流式调用（按类型选择，支持降级）
     * <p>
     * 用户只指定模型类型时，按降级策略依次尝试该类型的所有模型。
     * 如果所有模型都失败，抛出AllModelsFailedException。
     * </p>
     *
     * @param modelType 模型类型
     * @param request   请求参数
     * @return 响应结果的Mono
     */
    public Mono<AiResponse> callWithFallback(ModelType modelType, AiRequest request) {
        if (modelType == null) {
            return Mono.error(new IllegalArgumentException("模型类型不能为空"));
        }

        log.info("开始非流式调用（支持降级），类型: {}", modelType.getDesc());

        List<AiModel> models = selectModelsByType(modelType);

        return tryModelsSequentially(models, request, 0, null);
    }

    /**
     * 依次尝试模型列表中的每个模型（递归实现）
     *
     * @param models    模型列表
     * @param request   请求参数
     * @param index     当前尝试的模型索引
     * @param lastError 上一次的错误
     * @return 响应结果的Mono
     */
    private Mono<AiResponse> tryModelsSequentially(List<AiModel> models, AiRequest request,
                                                    int index, Throwable lastError) {
        if (index >= models.size()) {
            // 所有模型都失败了
            log.error("所有模型调用失败，共尝试 {} 个模型", models.size());
            return Mono.error(new AllModelsFailedException(
                    models.get(0).getType(), models.size(), lastError));
        }

        AiModel model = models.get(index);
        log.info("尝试调用模型 ({}/{}): {} ({})",
                index + 1, models.size(), model.getName(), model.getCode());

        try {
            AiProviderAdapter adapter = adapterFactory.getAdapter(model);

            return adapter.call(model, request)
                    .doOnSuccess(response -> log.info("模型调用成功: {}, tokens: {}",
                            model.getCode(), response.calculateTotalTokens()))
                    .onErrorResume(error -> {
                        log.warn("模型调用失败: {}, 错误: {}, 尝试下一个模型",
                                model.getCode(), error.getMessage());
                        // 递归尝试下一个模型
                        return tryModelsSequentially(models, request, index + 1, error);
                    });

        } catch (Exception e) {
            log.warn("获取适配器失败: {}, 错误: {}, 尝试下一个模型",
                    model.getCode(), e.getMessage());
            // 递归尝试下一个模型
            return tryModelsSequentially(models, request, index + 1, e);
        }
    }

    /**
     * 流式调用（指定模型代码，不降级）
     * <p>
     * 用户指定具体模型时，调用该模型的流式接口。
     * 如果调用失败，直接返回错误，不进行降级。
     * </p>
     *
     * @param modelCode 模型代码
     * @param request   请求参数
     * @return 流式响应的Flux
     */
    public Flux<AiStreamChunk> callStream(String modelCode, AiRequest request) {
        if (modelCode == null || modelCode.isEmpty()) {
            return Flux.error(new IllegalArgumentException("模型代码不能为空"));
        }

        log.info("开始流式调用，模型: {}", modelCode);

        try {
            AiModel model = selectModelByCode(modelCode);
            AiProviderAdapter adapter = adapterFactory.getAdapter(model);

            if (!adapter.supportsStreamForModel(model)) {
                return Flux.error(new UnsupportedOperationException(
                        String.format("模型不支持流式响应: %s", modelCode)));
            }

            return adapter.callStream(model, request)
                    .doOnComplete(() -> log.info("流式调用完成，模型: {}", modelCode))
                    .doOnError(error -> log.error("流式调用失败，模型: {}", modelCode, error));

        } catch (Exception e) {
            log.error("流式调用异常，模型: {}", modelCode, e);
            return Flux.error(e);
        }
    }

    /**
     * 流式调用（按类型选择，支持降级）
     * <p>
     * 用户只指定模型类型时，按降级策略依次尝试该类型的所有模型。
     * 如果所有模型都失败，抛出AllModelsFailedException。
     * </p>
     *
     * @param modelType 模型类型
     * @param request   请求参数
     * @return 流式响应的Flux
     */
    public Flux<AiStreamChunk> callStreamWithFallback(ModelType modelType, AiRequest request) {
        if (modelType == null) {
            return Flux.error(new IllegalArgumentException("模型类型不能为空"));
        }

        log.info("开始流式调用（支持降级），类型: {}", modelType.getDesc());

        List<AiModel> models = selectModelsByType(modelType);

        return tryModelsStreamSequentially(models, request, 0, null);
    }

    /**
     * 依次尝试模型列表中的每个模型（流式，递归实现）
     *
     * @param models    模型列表
     * @param request   请求参数
     * @param index     当前尝试的模型索引
     * @param lastError 上一次的错误
     * @return 流式响应的Flux
     */
    private Flux<AiStreamChunk> tryModelsStreamSequentially(List<AiModel> models, AiRequest request,
                                                             int index, Throwable lastError) {
        if (index >= models.size()) {
            // 所有模型都失败了
            log.error("所有模型流式调用失败，共尝试 {} 个模型", models.size());
            return Flux.error(new AllModelsFailedException(
                    models.get(0).getType(), models.size(), lastError));
        }

        AiModel model = models.get(index);
        log.info("尝试流式调用模型 ({}/{}): {} ({})",
                index + 1, models.size(), model.getName(), model.getCode());

        try {
            AiProviderAdapter adapter = adapterFactory.getAdapter(model);

            if (!adapter.supportsStreamForModel(model)) {
                log.warn("模型不支持流式响应: {}, 尝试下一个模型", model.getCode());
                return tryModelsStreamSequentially(models, request, index + 1,
                        new UnsupportedOperationException("模型不支持流式响应"));
            }

            return adapter.callStream(model, request)
                    .doOnComplete(() -> log.info("流式调用成功: {}", model.getCode()))
                    .onErrorResume(error -> {
                        log.warn("模型流式调用失败: {}, 错误: {}, 尝试下一个模型",
                                model.getCode(), error.getMessage());
                        // 递归尝试下一个模型
                        return tryModelsStreamSequentially(models, request, index + 1, error);
                    });

        } catch (Exception e) {
            log.warn("获取适配器失败: {}, 错误: {}, 尝试下一个模型",
                    model.getCode(), e.getMessage());
            // 递归尝试下一个模型
            return tryModelsStreamSequentially(models, request, index + 1, e);
        }
    }
}
