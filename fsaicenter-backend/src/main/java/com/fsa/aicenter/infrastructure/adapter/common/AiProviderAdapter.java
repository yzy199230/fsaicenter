package com.fsa.aicenter.infrastructure.adapter.common;

import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.valueobject.ProviderType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AI提供商适配器统一接口
 * <p>
 * 所有AI提供商适配器必须实现此接口，实现类通过Spring自动注册。
 * 接口设计遵循以下原则：
 * <ul>
 *   <li>统一接口：屏蔽不同提供商的API差异</li>
 *   <li>易于扩展：新增提供商只需实现接口，无需修改业务代码</li>
 *   <li>自动注册：Spring自动发现和注册所有实现类</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>
 * {@code
 * @Component
 * public class OpenAiAdapter implements AiProviderAdapter {
 *     @Override
 *     public ProviderType getProviderType() {
 *         return ProviderType.REMOTE;
 *     }
 *
 *     @Override
 *     public String getProviderCode() {
 *         return "openai";
 *     }
 *
 *     // ... 实现其他方法
 * }
 * }
 * </pre>
 * </p>
 */
public interface AiProviderAdapter {

    /**
     * 获取提供商类型
     * <p>
     * 用于区分远程提供商和本地提供商
     * </p>
     *
     * @return 提供商类型（REMOTE/LOCAL）
     */
    ProviderType getProviderType();

    /**
     * 获取提供商代码
     * <p>
     * 提供商的唯一标识，如：
     * <ul>
     *   <li>openai: OpenAI</li>
     *   <li>qwen: 通义千问</li>
     *   <li>wenxin: 文心一言</li>
     *   <li>ollama: Ollama本地模型</li>
     * </ul>
     * </p>
     *
     * @return 提供商代码
     */
    String getProviderCode();

    /**
     * 非流式调用
     * <p>
     * 适用于需要一次性返回完整结果的场景
     * </p>
     *
     * @param model   AI模型聚合根（包含模型配置和提供商信息）
     * @param request 统一请求参数
     * @return 响应结果的Mono
     */
    Mono<AiResponse> call(AiModel model, AiRequest request);

    /**
     * 流式调用
     * <p>
     * 适用于需要实时返回生成内容的场景，如聊天对话。
     * 流式响应通过SSE（Server-Sent Events）传输。
     * </p>
     *
     * @param model   AI模型聚合根（包含模型配置和提供商信息）
     * @param request 统一请求参数
     * @return 流式响应的Flux，每个元素是一个数据块
     */
    Flux<AiStreamChunk> callStream(AiModel model, AiRequest request);

    /**
     * 转换请求参数
     * <p>
     * 将统一的AiRequest转换为提供商特定的请求格式。
     * 子类实现时应处理：
     * <ul>
     *   <li>参数映射：将标准参数映射到提供商参数</li>
     *   <li>参数校验：校验提供商特定的参数约束</li>
     *   <li>默认值处理：设置提供商特定的默认值</li>
     * </ul>
     * </p>
     *
     * @param request 统一请求参数
     * @return 提供商特定的请求对象
     */
    Object convertRequest(AiRequest request);

    /**
     * 转换响应结果
     * <p>
     * 将提供商特定的响应格式转换为统一的AiResponse。
     * 子类实现时应处理：
     * <ul>
     *   <li>字段映射：将提供商字段映射到标准字段</li>
     *   <li>token统计：提取或计算token使用情况</li>
     *   <li>错误处理：转换提供商特定的错误信息</li>
     * </ul>
     * </p>
     *
     * @param rawResponse 提供商原始响应对象
     * @return 统一响应结果
     */
    AiResponse convertResponse(Object rawResponse);

    /**
     * 检查是否支持流式响应
     * <p>
     * 默认实现：返回true（大部分提供商都支持流式）
     * </p>
     *
     * @return true表示支持流式响应
     */
    default boolean supportsStream() {
        return true;
    }

    /**
     * 检查是否支持流式调用
     * <p>
     * 检查模型和适配器是否都支持流式响应
     * </p>
     *
     * @param model AI模型
     * @return true表示支持
     */
    default boolean supportsStreamForModel(AiModel model) {
        if (model == null) {
            return false;
        }
        return supportsStream() && model.supportStream();
    }

    /**
     * 向量嵌入
     * <p>
     * 将文本转换为向量表示，用于语义搜索、相似度计算等场景
     * </p>
     *
     * @param model   AI模型聚合根
     * @param request 统一请求参数（使用input字段）
     * @return 响应结果的Mono（包含embedding字段）
     */
    default Mono<AiResponse> embedding(AiModel model, AiRequest request) {
        return Mono.error(new UnsupportedOperationException("该提供商不支持向量嵌入功能"));
    }

    /**
     * 图像生成
     * <p>
     * 根据文本提示词或参考图片生成图像
     * </p>
     *
     * @param model   AI模型聚合根
     * @param request 统一请求参数（使用prompt/image字段）
     * @return 响应结果的Mono（包含imageUrls字段）
     */
    default Mono<AiResponse> generateImage(AiModel model, AiRequest request) {
        return Mono.error(new UnsupportedOperationException("该提供商不支持图像生成功能"));
    }

    /**
     * 图像识别/视觉分析
     * <p>
     * 分析图片内容，支持图片理解、多模态分析
     * </p>
     *
     * @param model   AI模型聚合根
     * @param request 统一请求参数（使用image字段和messages）
     * @return 响应结果的Mono（包含content字段）
     */
    default Mono<AiResponse> imageRecognition(AiModel model, AiRequest request) {
        return Mono.error(new UnsupportedOperationException("该提供商不支持图像识别功能"));
    }

    /**
     * 语音识别（ASR）
     * <p>
     * 将语音转换为文字
     * </p>
     *
     * @param model   AI模型聚合根
     * @param request 统一请求参数（使用audio字段）
     * @return 响应结果的Mono（包含content字段）
     */
    default Mono<AiResponse> speechToText(AiModel model, AiRequest request) {
        return Mono.error(new UnsupportedOperationException("该提供商不支持语音识别功能"));
    }

    /**
     * 流式语音识别（ASR）
     * <p>
     * 将语音转换为文字，支持流式输出识别结果
     * </p>
     *
     * @param model   AI模型聚合根
     * @param request 统一请求参数（使用audio字段）
     * @return 流式响应的Flux，每个元素是一个识别文本块
     */
    default Flux<AiStreamChunk> speechToTextStream(AiModel model, AiRequest request) {
        return Flux.error(new UnsupportedOperationException("该提供商不支持流式语音识别功能"));
    }

    /**
     * 语音合成（TTS）
     * <p>
     * 将文字转换为语音
     * </p>
     *
     * @param model   AI模型聚合根
     * @param request 统一请求参数（使用input字段）
     * @return 响应结果的Mono（包含audioUrl字段）
     */
    default Mono<AiResponse> textToSpeech(AiModel model, AiRequest request) {
        return Mono.error(new UnsupportedOperationException("该提供商不支持语音合成功能"));
    }

    /**
     * 视频生成
     * <p>
     * 根据文本或图片生成视频
     * </p>
     *
     * @param model   AI模型聚合根
     * @param request 统一请求参数（使用prompt/image字段）
     * @return 响应结果的Mono（包含videoUrl字段）
     */
    default Mono<AiResponse> generateVideo(AiModel model, AiRequest request) {
        return Mono.error(new UnsupportedOperationException("该提供商不支持视频生成功能"));
    }
}
