package com.fsa.aicenter.interfaces.v1.controller;

import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.apikey.repository.ApiKeyRepository;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.repository.ModelRepository;
import com.fsa.aicenter.interfaces.v1.dto.OpenAiModelListResponse;
import com.fsa.aicenter.interfaces.v1.dto.OpenAiModelObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI 兼容 - Models API
 */
@RestController
@RequestMapping("/v1/models")
@RequiredArgsConstructor
public class OpenAiModelsController {

    private final ModelRepository modelRepository;
    private final ApiKeyRepository apiKeyRepository;

    @GetMapping
    public OpenAiModelListResponse listModels(HttpServletRequest request) {
        ApiKey apiKey = (ApiKey) request.getAttribute("apiKey");

        // 获取该 API Key 允许访问的模型 ID 列表
        List<Long> allowedIds = apiKeyRepository.findAccessibleModelIds(apiKey.getId());

        // 获取所有启用的模型
        List<AiModel> models = modelRepository.findAll().stream()
                .filter(AiModel::isEnabled)
                .collect(Collectors.toList());

        // 如果有访问限制，则过滤
        if (allowedIds != null && !allowedIds.isEmpty()) {
            models = models.stream()
                    .filter(m -> allowedIds.contains(m.getId()))
                    .collect(Collectors.toList());
        }

        List<OpenAiModelObject> data = models.stream()
                .map(this::toModelObject)
                .collect(Collectors.toList());

        return OpenAiModelListResponse.builder().data(data).build();
    }

    @GetMapping("/{model}")
    public OpenAiModelObject getModel(@PathVariable String model) {
        AiModel aiModel = modelRepository.findByCode(model)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + model));
        return toModelObject(aiModel);
    }

    private OpenAiModelObject toModelObject(AiModel model) {
        Long created = model.getCreatedTime() != null
                ? model.getCreatedTime().toEpochSecond(ZoneOffset.UTC)
                : System.currentTimeMillis() / 1000;

        return OpenAiModelObject.builder()
                .id(model.getCode())
                .created(created)
                .ownedBy("fsaicenter")
                .build();
    }
}
