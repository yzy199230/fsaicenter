# 多模态 API 转发架构设计

> 日期：2026-03-18
> 状态：待实施
> 范围：全部 7 种模态（Chat 多模态、Embedding、Image、Vision、ASR、TTS、Video）

## 一、设计决策总览

| 决策点 | 选择 | 理由 |
|---|---|---|
| API 兼容性 | 完全兼容 OpenAI API 格式 | OpenAI SDK 客户端可直连 |
| 模态覆盖 | 全部 7 种一次性打通 | 避免多次改造基础层 |
| 消息格式 | content 改为 Object，运行时判断 | 向后兼容 String，支持多模态数组 |
| 代理路由 | ProxyService 内部按 ModelType 分发 | 编排层职责，fallback 逻辑集中 |
| 适配器策略 | GenericOpenAiAdapter 按 endpoint 分别构造 | 一个适配器覆盖所有 OpenAI 兼容场景 |
| API 路径 | 双路径分控制器（/v1/... 原生格式 + /api/v1/ai/... Result 包装） | 不破坏已有客户端，同时兼容 OpenAI SDK |
| 计费策略 | 按模态分别制定（对齐行业实践） | Chat 按 token，Image 按张，ASR 按秒等 |

---

## 二、消息模型改造

### 2.1 Message.content 多态化

将 `Message.content` 从 `String` 改为 `Object`，支持两种形态：

**纯文本**（向后兼容）：
```json
{"role": "user", "content": "你好"}
```

**多模态数组**（OpenAI 兼容）：
```json
{"role": "user", "content": [
  {"type": "text", "text": "这张图片是什么？"},
  {"type": "image_url", "image_url": {"url": "https://...", "detail": "high"}},
  {"type": "input_audio", "input_audio": {"data": "base64...", "format": "wav"}}
]}
```

### 2.2 透传优先，不建 ContentPart 类型体系

项目本质是 API 代理/网关，多模态 content 大部分情况下是透传给上游 Provider 的。

Jackson 反序列化为 `String` 或 `List<LinkedHashMap>`，序列化时原样输出，格式保真。

Message 增加工具方法：
- `getTextContent()` — 提取纯文本（多模态时拼接所有 text 部分），用于 token 估算和日志
- `isMultimodal()` — 判断 content 是否为 List

**不建完整 ContentPart 体系的原因：**
- 零维护成本 — OpenAI 新增 content type 时自动透传，不需要新增类
- 序列化对称 — 反序列化和序列化格式保真
- 改动面最小 — 不引入额外类

---

## 三、AiProxyService 路由改造

### 3.1 dispatchByType 按 ModelType 自动分发

在 `call/callWithFallback` 内部，根据 `model.getType()` 路由到适配器的对应方法：

```java
private Mono<AiResponse> dispatchByType(AiProviderAdapter adapter, AiModel model, AiRequest request) {
    return switch (model.getType()) {
        case CHAT, IMAGE_RECOGNITION -> adapter.call(model, request);
        case EMBEDDING              -> adapter.embedding(model, request);
        case IMAGE                  -> adapter.generateImage(model, request);
        case ASR                    -> adapter.speechToText(model, request);
        case TTS                    -> adapter.textToSpeech(model, request);
        case VIDEO                  -> adapter.generateVideo(model, request);
    }
    // fallback 成功后，回填实际命中的模型和 provider 信息
    .doOnSuccess(resp -> {
        resp.setActualModel(model.getCode());
        resp.setActualModelName(model.getName());
        resp.setActualProvider(model.getProviderCode());
    });
}
```

### 3.2 关键设计点

- **IMAGE_RECOGNITION 走 `call()`** — Vision 本质是 chat completions 的多模态输入，OpenAI 走同一个端点
- **直接用 `model.getType()` 路由** — 不在 AiRequest 上冗余存 modelType，避免数据不一致
- **Stream 路由同理** — `dispatchStreamByType()` 对应处理流式场景

### 3.2.1 流式链路的实际命中模型回传

非流式链路通过 `AiResponse.actualModel/actualProvider` 回传实际命中模型（见 3.1），
但流式链路使用 `Flux<AiStreamChunk>`，当前 `AiStreamChunk` 没有这些字段（见 `AiStreamChunk.java`），
导致流式 fallback 后计费和日志仍按预选主模型记录（见 `ChatController.java:273`）。

#### AiStreamChunk 新增字段

```java
public class AiStreamChunk {
    // ... 现有字段 ...

    /** 实际命中的模型 code（fallback 场景下可能与请求主模型不同） */
    private String actualModel;
    /** 实际命中的 provider code */
    private String actualProvider;
}
```

#### dispatchStreamByType 回填

```java
private Flux<AiStreamChunk> dispatchStreamByType(AiProviderAdapter adapter, AiModel model, AiRequest request) {
    return switch (model.getType()) {
        case CHAT, IMAGE_RECOGNITION -> adapter.callStream(model, request);
        // ... 其他模态的流式支持 ...
    }
    .map(chunk -> {
        // 每个 chunk 都携带实际命中信息，Controller 完成时取最后一个
        chunk.setActualModel(model.getCode());
        chunk.setActualProvider(model.getProviderCode());
        return chunk;
    });
}
```

#### Controller 流式完成时使用 actualModel

```java
// 流式响应累计信息
AtomicReference<String> actualModelRef = new AtomicReference<>();
AtomicReference<String> actualProviderRef = new AtomicReference<>();

return chunkFlux
    .map(aiChunk -> {
        // 记录实际命中的模型（每个 chunk 都有，取最后一个即可）
        if (aiChunk.getActualModel() != null) {
            actualModelRef.set(aiChunk.getActualModel());
            actualProviderRef.set(aiChunk.getActualProvider());
        }
        // ... 现有转换逻辑 ...
    })
    .doOnComplete(() -> {
        // 使用实际命中的模型进行计费
        String billingModel = actualModelRef.get() != null ? actualModelRef.get() : selectedModel.getCode();
        // ... 计费和日志逻辑 ...
    });
```

> **设计原则**：流式和非流式的实际命中模型回传必须对称。`AiStreamChunk` 携带 `actualModel/actualProvider`，Controller 在 `doOnComplete` 时取最终值用于计费和日志。

### 3.3 Fallback 与 rawBody 透传的兼容设计

rawBody 透传（只改 model/endpoint，其余原样发送）与跨 provider fallback 存在冲突：
provider A 接受的扩展参数（如 `response_format`、`tools`、供应商私有字段）被原样带给 provider B，可能导致连续失败。

#### 约束规则

1. **Fallback 仅限同构 provider 组**：同一个 fallback 列表内的模型必须属于同协议族（如都是 OpenAI-compatible）。配置层校验不允许混入 SDK 类适配器（如 QwenAdapter）的模型。

2. **参数裁剪层**（在 `dispatchByType` 之前执行）：

```java
private Map<String, Object> sanitizeRawBody(Map<String, Object> rawBody, AiModel targetModel) {
    Map<String, Object> sanitized = new HashMap<>(rawBody);
    sanitized.put("model", targetModel.getCode());

    // 移除目标 provider 不支持的已知扩展字段
    Set<String> unsupported = targetModel.getProvider().getUnsupportedParams();
    if (unsupported != null) {
        unsupported.forEach(sanitized::remove);
    }
    return sanitized;
}
```

3. **Provider 新增 `unsupportedParams` 配置**（可选）：在 Provider 实体上维护该 provider 已知不支持的参数名集合，fallback 时主动剔除。

> **降级准则**：fallback 优先保证"能调通"而非"原样透传"。宁可丢掉非标准扩展参数，也不让 fallback 变成连续 400。

---

## 四、GenericOpenAiAdapter 多模态扩展

### 4.1 核心原则：透传 + 最小改写

对于 OpenAI 兼容 Provider，请求体最大化透传，只改必要字段（model 名、endpoint）。

### 4.2 AiRequest 新增 rawBody 字段

```java
public class AiRequest {
    // 现有类型化字段保留（给 SDK 类适配器用）
    private String model;
    private List<Message> messages;
    private Double temperature;
    // ...

    // 新增：原始请求体（给 OpenAI 兼容透传用）
    private Map<String, Object> rawBody;
}
```

Controller 层同时接收并保存原始请求体：

```java
@PostMapping("/v1/chat/completions")
public Mono<ResponseEntity<?>> chat(@RequestBody Map<String, Object> rawBody) {
    AiRequest request = ChatRequestConverter.convert(rawBody);
    request.setRawBody(rawBody);
    return aiProxyService.call(modelCode, request);
}
```

### 4.3 ASR multipart 入参契约

`/v1/audio/transcriptions` 是 `multipart/form-data`（不是 JSON），需要独立的入参路径。

#### Controller 层接收

```java
@PostMapping(value = "/v1/audio/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Mono<ResponseEntity<?>> transcribe(
        @RequestPart("file") MultipartFile file,
        @RequestParam("model") String model,
        @RequestParam(value = "language", required = false) String language,
        @RequestParam(value = "response_format", required = false) String responseFormat,
        @RequestParam(value = "temperature", required = false) Double temperature
) {
    AiRequest request = AsrRequestConverter.convert(file, model, language, responseFormat, temperature);
    return aiProxyService.call(modelCode, request).map(ResponseEntity::ok);
}
```

#### AiRequest 新增字段

```java
public class AiRequest {
    // ... 现有字段 ...

    // 新增：音频二进制数据（ASR 专用）
    private byte[] audioData;

    // 新增：音频文件名（保留原始扩展名，用于 multipart 转发）
    private String audioFilename;

    // 现有 audioFormat 字段复用（mp3/wav/flac 等）
}
```

> **不使用 `MultipartFile` 直接存入 AiRequest**，因为 MultipartFile 绑定了 Servlet 请求生命周期。Controller 层立即 `file.getBytes()` 拷贝到 `byte[] audioData`，后续层不依赖 HTTP 上下文。

#### 文件大小限制

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 25MB    # OpenAI Whisper 限制
      max-request-size: 30MB
```

#### 适配器转发

```java
// GenericOpenAiAdapter.speechToText()
public Mono<AiResponse> speechToText(AiModel model, AiRequest request) {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("file", new ByteArrayResource(request.getAudioData()) {
        @Override public String getFilename() { return request.getAudioFilename(); }
    }).contentType(MediaType.APPLICATION_OCTET_STREAM);
    builder.part("model", model.getCode());
    if (request.getLanguage() != null) builder.part("language", request.getLanguage());

    return sendMultipart(resolveEndpoint(model, "asr"), builder.build(), model)
           .map(this::parseAsrResponse);
}
```

### 4.4 适配器内部结构

提取共享 HTTP 基础设施，每种模态一对 build + parse 私有方法：

```java
class GenericOpenAiAdapter {
    // ── 共享基础设施 ──
    private Mono<String> sendJson(String url, Map<String,Object> body, AiModel model) { ... }
    private Mono<byte[]> sendJsonForBinary(String url, Map<String,Object> body, AiModel model) { ... }
    private Mono<String> sendMultipart(String url, MultipartBody body, AiModel model) { ... }
    private String resolveEndpoint(AiModel model, String endpointType) { ... }

    // ── Chat（rawBody 透传）──
    public Mono<AiResponse> call(AiModel model, AiRequest request) {
        Map<String,Object> body = request.getRawBody();
        body.put("model", model.getCode());
        return sendJson(resolveEndpoint(model, "chat"), body, model)
               .map(this::parseChatResponse);
    }

    // ── Embedding ──
    // ── Image ──
    // ── ASR (multipart) ──
    // ── TTS (binary response) ──
    // ── Video ──
}
```

### 4.5 端点能力检查

```java
private String resolveEndpoint(AiModel model, String type) {
    Provider provider = getProvider(model);
    String endpoint = switch (type) {
        case "chat"      -> provider.getChatEndpointOrDefault();
        case "embedding"  -> provider.getEmbeddingEndpoint();
        case "image"      -> provider.getImageEndpoint();
        case "asr"        -> provider.getAsrEndpoint();
        case "tts"        -> provider.getTtsEndpoint();
        case "video"      -> provider.getVideoEndpoint();
        default -> throw new IllegalArgumentException("Unknown: " + type);
    };
    if (endpoint == null) {
        throw new UnsupportedOperationException(provider.getCode() + " 不支持 " + type);
    }
    return provider.getBaseUrl() + endpoint;
}
```

Provider endpoint 为 null → 快速失败 → ProxyService fallback 自动跳到下一个 provider。

---

## 五、对外 API 控制器

### 5.1 端点全景

| 端点 | 模态 | 现状 | 动作 |
|---|---|---|---|
| `POST /v1/chat/completions` | CHAT + Vision | 有，路径和格式不完全兼容 | 改造 |
| `POST /v1/embeddings` | EMBEDDING | 有，接线有问题 | 修复 |
| `POST /v1/images/generations` | IMAGE | 有，接线有问题 | 修复 |
| `POST /v1/audio/transcriptions` | ASR | 无 | 新增 |
| `POST /v1/audio/speech` | TTS | 无 | 新增 |
| `POST /v1/videos/generations` | VIDEO | 无 | 新增 |
| `GET /v1/models` | 模型列表 | 无 | 新增 |

### 5.2 双路径分协议实现

`/v1/...`（OpenAI 兼容）和 `/api/v1/ai/...`（旧客户端）的响应格式互不兼容，**不能共享同一个方法**：

| 维度 | `/v1/...`（OpenAI 兼容） | `/api/v1/ai/...`（旧客户端） |
|---|---|---|
| 正常响应 | 原生 OpenAI JSON | `Result<T>` 包装 |
| 异常响应 | `{"error": {...}}` | `Result.error(...)` |
| 流式响应 | `data: {...}\n\n` + `data: [DONE]` | `Result<Flux<SSE>>` |
| 认证方式 | `Authorization: Bearer sk-xxx` | 现有 API Key 体系 |

#### 控制器拆分策略

新建 **OpenAI 兼容控制器**（`@RequestMapping("/v1")`），与现有控制器分离：

```java
// 新建：OpenAI 兼容控制器，直接返回原生格式
@RestController
@RequestMapping("/v1")
public class OpenAiCompatChatController {

    @PostMapping("/chat/completions")
    public Mono<ResponseEntity<String>> chat(@RequestBody Map<String, Object> rawBody) {
        AiRequest request = ChatRequestConverter.convert(rawBody);
        request.setRawBody(rawBody);
        return aiProxyService.call(modelCode, request)
               .map(resp -> ResponseEntity.ok()
                   .contentType(MediaType.APPLICATION_JSON)
                   .body(resp.getRawResponse()));  // 直接透传 JSON
    }
}

// 现有控制器保持不变，继续返回 Result<T>
@RestController
@RequestMapping("/api/v1/ai")
public class ChatController {
    // ... 现有代码不改 ...
}
```

两套控制器共享底层 `AiProxyService`，只在 Controller 层分叉响应格式。

#### 共享逻辑提取

将鉴权、计费、配额扣减等共用逻辑提取到 Service 层或 `@Aspect`，避免两套控制器重复代码：

```
请求 → 鉴权 Filter → Controller（分叉点）→ AiProxyService（共享）→ Adapter
                         ↓                        ↓
                   /v1: 原生 JSON            /api/v1/ai: Result<T>
```

### 5.4 /v1 路径过滤器注册

现有 `FilterConfig` 的 `addUrlPatterns("/api/*")` 只拦截 `/api/...` 路径（见 `FilterConfig.java:28,41`）。
新建的 `/v1/...` 端点如果不纳入过滤器链，将**绕过 API Key 认证、限流、配额扣减**。

#### FilterConfig 改造

```java
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyAuthenticationFilterRegistration(
            ApiKeyAuthenticationFilter filter) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*", "/v1/*");   // ← 新增 /v1/*
        registration.setName("apiKeyAuthFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*", "/v1/*");   // ← 新增 /v1/*
        registration.setName("rateLimitFltr");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }
}
```

同步修改 `ApiKeyAuthenticationFilter.shouldNotFilter()` 和 `RateLimitFilter.shouldNotFilter()`：
当前排除路径用 `startsWith("/v3/api-docs")` 等硬编码白名单，`/v1/models` 不在白名单内所以会正常拦截。
需要确认 `/v1/models` 是否需要认证（OpenAI 需要），如果有无需认证的 `/v1` 子路径，需要在 `shouldNotFilter` 中显式排除。

### 5.5 过滤器阶段异常的 OpenAI 格式输出

`OpenAiErrorHandler`（`@RestControllerAdvice`）**只能捕获 Controller 层抛出的异常**。
认证失败（`AuthenticationException`）和限流（`RateLimitException`）都在 Filter 内抛出（见 `ApiKeyAuthenticationFilter.java:64`、`RateLimitFilter.java:107`），
这些异常不会进入 `@RestControllerAdvice`，而是被 Servlet 容器的默认错误处理接管，对 `/v1` 路径返回的不是 OpenAI 格式。

#### 方案：Filter 内直接写回 OpenAI 格式 JSON

```java
// 新建工具类：统一 OpenAI 错误响应写回
public final class OpenAiErrorResponseWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void writeError(HttpServletResponse response, int httpStatus,
                                   String errorCode, String errorType, String message)
            throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of("error", Map.of(
            "message", message,
            "type", errorType,
            "param", (Object) JSONObject.NULL,  // 或 JsonNodeFactory.instance.nullNode()
            "code", errorCode
        ));
        response.getWriter().write(MAPPER.writeValueAsString(body));
    }
}
```

#### Filter 改造（以认证过滤器为例）

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    try {
        String apiKeyValue = extractApiKey(request);
        ApiKey apiKey = validateApiKey(apiKeyValue, request);
        request.setAttribute(API_KEY_ATTRIBUTE, apiKey);
        filterChain.doFilter(request, response);
    } catch (AuthenticationException e) {
        log.warn("API Key authentication failed: {} - IP: {}, Path: {}",
                e.getMessage(), getClientIp(request), request.getRequestURI());

        if (request.getRequestURI().startsWith("/v1/")) {
            // /v1 路径：返回 OpenAI 格式错误
            OpenAiErrorResponseWriter.writeError(response, 401,
                "invalid_api_key", "invalid_request_error", e.getMessage());
            return;  // 不再向上抛出
        }
        throw e;  // /api 路径：沿用原逻辑，交给 GlobalExceptionHandler
    }
}
```

`RateLimitFilter` 同理，限流异常时对 `/v1/` 路径写回 `429 rate_limit_exceeded`。

> **设计原则**：过滤器阶段的异常不经过 Spring MVC 异常处理链路，必须在 filter 内判断路径前缀并直接写回响应。`/api/*` 路径维持原有 throw 行为，`/v1/*` 路径在 filter 内短路返回 OpenAI JSON。

### 5.3 响应透传（区分 JSON 与二进制）

响应分两条路径，**不共享同一个字段**：

#### 5.3.1 JSON 模态（Chat、Embedding、Image、ASR、Video）

AiResponse 新增 `rawResponse` 字段（**String，仅用于 JSON 响应体**）：

```java
public class AiResponse {
    // 现有字段保留
    private String content;
    private Integer promptTokens;
    // ...

    // 新增：上游原始 JSON 响应（仅限 JSON 模态）
    private String rawResponse;

    // 新增：fallback 实际命中信息（由 AiProxyService.dispatchByType 填充）
    private String actualModel;       // 实际调用的模型 code（可能与请求的主模型不同）
    private String actualModelName;   // 实际调用的模型名称
    private String actualProvider;    // 实际调用的 provider code
}
```

Controller 层：有 rawResponse 则直接透传 JSON，否则从 AiResponse 字段构造标准格式。
**计费和日志必须使用实际命中的模型**，而非请求时选定的主模型。

#### 5.3.3 实际命中模型与现有事件/日志 schema 的衔接

**现状问题**：
- `BillingEvent` 构造器要求 `AiModel model`（完整聚合根对象，见 `BillingEvent.java:33`）
- `DomainEventPublisher.publishBillingEvent()` 签名同样是 `AiModel model`（见 `DomainEventPublisher.java:44`）
- `AiModel` 聚合根没有 `providerCode` 字段（见 `AiModel.java`），需要通过 Provider 关联查询
- 设计文档上一版示例直接传 `String actualModel`，与现有签名不兼容

**修复方案 — AiProxyService 返回完整 AiModel 而非 String code**：

`dispatchByType` 回填时保存的是 `AiModel` 对象引用（ProxyService 本身就持有 model 列表），
AiResponse/AiStreamChunk 用 String 字段给 Controller 用于 OpenAI 兼容响应输出，
但 **计费/日志继续使用 AiModel 对象**，由 AiProxyService 额外返回。

```java
/**
 * AiProxyService 调用结果（封装 response + 实际命中的 AiModel）
 */
@Data
@AllArgsConstructor
public class ProxyResult<T> {
    private T response;         // AiResponse 或 Flux<AiStreamChunk>
    private AiModel actualModel; // fallback 后实际命中的 AiModel 聚合根
}
```

Controller 使用示例：

```java
// 非流式
ProxyResult<AiResponse> result = aiProxyService.callWithResult(modelCode, aiRequest);
AiResponse aiResponse = result.getResponse();
AiModel actualModel = result.getActualModel();

// 计费 — 使用 AiModel 对象，与现有 BillingEvent 签名兼容
eventPublisher.publishBillingEvent(
    requestId, apiKey,
    actualModel,              // AiModel 聚合根，非 String
    actualTokens, requestTime, responseTime
);

// OpenAI 兼容响应 — 使用 String code
ResponseEntity.ok().body(aiResponse.getRawResponse());
```

**AiModel 补充 providerCode 关联**：

当前 `AiModel` 只有 `providerId`，缺少反查 Provider code 的能力。两种方案：

方案 A（推荐）：AiProxyService 在加载模型时就关联好 Provider，在 `ProxyResult` 中同时提供 `actualModel` 和 `actualProvider`（`Provider` 对象或 String code）：

```java
@Data
@AllArgsConstructor
public class ProxyResult<T> {
    private T response;
    private AiModel actualModel;
    private String actualProviderCode;  // 从 Provider 缓存中直接取
}
```

方案 B：给 AiModel 增加瞬态字段 `providerCode`，在 Repository 层查询时一并填充。

> **关键约束**：无论哪种方案，`BillingEvent` 和 `DomainEventPublisher` 的现有签名（接收 `AiModel`）保持不变，避免改动计费下游。`actualModel`/`actualProvider` 的 String 字段仅用于 OpenAI 兼容 JSON 响应输出。

#### 5.3.2 二进制模态（TTS）

`/v1/audio/speech` 返回的是 **二进制音频流**（mp3/opus/aac/flac），不是 JSON。
不走 rawResponse，适配器返回包含**上游原始响应头**的二进制响应对象。

#### 二进制响应载体

```java
/**
 * 适配器返回的二进制响应，保留上游 HTTP 响应的元信息
 */
@Data
@Builder
public class BinaryResponse {
    /** 二进制响应体 */
    private byte[] body;
    /** 上游响应的 Content-Type（如 audio/mpeg, audio/opus, audio/aac） */
    private String contentType;
    /** 上游响应的 Content-Disposition（可选） */
    private String contentDisposition;
    /** 上游 HTTP 状态码 */
    private int statusCode;
}
```

#### AiProviderAdapter 接口新增

```java
Mono<BinaryResponse> textToSpeechBinary(AiModel model, AiRequest request);
```

#### GenericOpenAiAdapter 实现 — 透传上游 headers

```java
public Mono<BinaryResponse> textToSpeechBinary(AiModel model, AiRequest request) {
    Map<String,Object> body = Map.of(
        "model", model.getCode(),
        "input", request.getInput(),
        "voice", request.getVoice()
    );
    // sendJsonForBinaryWithHeaders 返回 Mono<ClientResponse>，保留完整 HTTP 响应
    return webClient.post()
        .uri(resolveEndpoint(model, "tts"))
        .headers(h -> applyAuth(h, model))
        .bodyValue(body)
        .exchangeToMono(clientResponse -> clientResponse.bodyToMono(byte[].class)
            .map(bytes -> BinaryResponse.builder()
                .body(bytes)
                .statusCode(clientResponse.statusCode().value())
                .contentType(clientResponse.headers().contentType()
                    .map(MediaType::toString).orElse("application/octet-stream"))
                .contentDisposition(clientResponse.headers().asHttpHeaders()
                    .getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .build()));
}
```

> **关键区别**：不再硬编码 `audio/mpeg` 和 `speech.mp3`。`contentType` 和 `contentDisposition` 从上游响应原样透传，确保 mp3/opus/aac/flac 等所有格式正确传递。

#### Controller 层

```java
@PostMapping("/v1/audio/speech")  // 不指定 produces，由上游 Content-Type 决定
public Mono<ResponseEntity<byte[]>> speech(@RequestBody Map<String, Object> rawBody) {
    AiRequest request = TtsRequestConverter.convert(rawBody);
    return aiProxyService.callBinary(modelCode, request)
        .map(binary -> {
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(binary.getStatusCode())
                .contentType(MediaType.parseMediaType(binary.getContentType()));
            if (binary.getContentDisposition() != null) {
                builder.header(HttpHeaders.CONTENT_DISPOSITION, binary.getContentDisposition());
            }
            return builder.body(binary.getBody());
        });
}
```

> **设计原则**：JSON 模态用 `rawResponse: String` 透传；二进制模态使用 `BinaryResponse` 对象携带 `bytes + upstream status + upstream headers`，Controller 层原样转发，不在网关层假设音频格式。现有 `AiResponse.audioUrl/audioBase64` 字段继续给旧路径 `/api/v1/ai/...` 使用。

---

## 六、计费策略（对齐行业实践）

| 模态 | 计费单位 | 估算方式 | 行业依据 |
|---|---|---|---|
| CHAT | input token + output token | 输入文本长度预估 | 全行业标准 |
| IMAGE_RECOGNITION | 与 CHAT 统一 | 图片按固定 token 折算 | OpenAI/Gemini：图片计为 token |
| EMBEDDING | input token（输出不计） | 输入文本长度 | OpenAI：$0.02/1M tokens |
| IMAGE | 按张 × 系数 | 尺寸+质量系数 | OpenAI/通义：按张+尺寸 |
| ASR | 按音频秒数 | 从文件解析时长 | OpenAI Whisper：$0.006/分钟 |
| TTS | 按输入字符数 | 文本 length | OpenAI：$15/1M 字符 |
| VIDEO | 按秒 × 分辨率系数 | duration × resolution | 百度积分制简化版 |

---

## 七、Provider 实体扩展

### 7.1 新增字段

```java
private String asrEndpoint;   // 默认 /audio/transcriptions
private String ttsEndpoint;   // 默认 /audio/speech
```

### 7.2 数据库 Migration

```sql
ALTER TABLE provider ADD COLUMN asr_endpoint VARCHAR(255);
ALTER TABLE provider ADD COLUMN tts_endpoint VARCHAR(255);
```

---

## 八、双协议异常处理

现有 `GlobalExceptionHandler` 全部返回 `Result<Void>`，**仅对 `/api/v1/ai/...` 路径生效**。
`/v1/...` 路径需要 OpenAI 格式异常，两者必须隔离。

### 8.1 异常处理器拆分

```java
// 新建：仅匹配 /v1 路径下的 OpenAI 兼容控制器
@RestControllerAdvice(basePackages = "com.fsa.aicenter.interfaces.api.openai")
@Order(1)  // 优先于全局处理器
public class OpenAiErrorHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handle(BusinessException e) {
        return ResponseEntity.status(mapHttpStatus(e))
               .body(Map.of("error", Map.of(
                   "message", e.getMessage(),
                   "type", mapErrorType(e),
                   "param", JSONObject.NULL,
                   "code", mapErrorCode(e)
               )));
    }
    // ... 其他异常类型同理
}

// 现有全局处理器不变，继续返回 Result<Void>
@RestControllerAdvice
@Order(2)
public class GlobalExceptionHandler {
    // ... 现有代码保持 ...
}
```

**隔离机制**：`@RestControllerAdvice(basePackages = ...)` + `@Order` 确保 `/v1` 路径的异常被 `OpenAiErrorHandler` 优先捕获，不会落入 `GlobalExceptionHandler` 的 `Result<Void>` 包装。

### 8.2 OpenAI 错误格式

```json
{
  "error": {
    "message": "错误描述",
    "type": "invalid_request_error",
    "param": null,
    "code": "invalid_api_key"
  }
}
```

### 8.3 错误映射表

| 场景 | HTTP 状态码 | error.code | error.type |
|---|---|---|---|
| API Key 无效 | 401 | invalid_api_key | invalid_request_error |
| 配额不足 | 429 | insufficient_quota | insufficient_quota |
| 模型不支持该模态 | 400 | model_not_supported | invalid_request_error |
| 参数校验失败 | 400 | invalid_request | invalid_request_error |
| 所有模型均失败 | 503 | model_unavailable | server_error |
| 限流 | 429 | rate_limit_exceeded | rate_limit_error |

---

## 九、实现路线图

### 阶段 1 — 基础层改造（类型变更，需同步修复消费方）

> **风险说明**：`Message.content` 从 `String` 改为 `Object` 不是"只加字段"。该字段被 14 处代码直接消费（包括校验、估算、序列化），类型变更会导致编译错误或运行时 ClassCastException。阶段 1 必须同步修复所有消费方。

| 步骤 | 改动 | 文件 | 影响说明 |
|---|---|---|---|
| 1.1 | Message.content 改为 Object + getTextContent()/isMultimodal() | Message.java | 类型变更 |
| 1.2 | `@NotBlank` → 自定义校验器 `@ValidContent`（兼容 String 和 List） | Message.java + ValidContentValidator.java（新建） | `@NotBlank` 只接受 CharSequence，Object 类型会校验失败 |
| 1.3 | ChatController.estimateTokens() 改用 `getTextContent()` | ChatController.java:390 | `m.getContent().length()` → `m.getTextContent().length()` |
| 1.4 | GenericOpenAiAdapter.convertMessages() 改为 `Map<String, Object>` | GenericOpenAiAdapter.java:471 | `Map<String, String>` → `Map<String, Object>` |
| 1.5 | 所有其他适配器的 getContent() 调用排查修复 | DoubaoAdapter, WenxinAdapter, OllamaAdapter, VllmAdapter, OpenAiAdapter, SparkAdapter | 各适配器的 convertMessages 方法 |
| 1.6 | QwenAdapter 特殊处理（已用 Object content） | QwenAdapter.java:2075 | 已在做 null 检查和 List 判断，需确认兼容 |
| 1.7 | AiRequest 新增 rawBody + audioData/audioFilename 字段 | AiRequest.java | 纯加字段，低风险 |
| 1.8 | AiResponse 新增 rawResponse + actualModel/actualProvider 字段 | AiResponse.java | 纯加字段，低风险 |
| 1.9 | AiStreamChunk 新增 actualModel/actualProvider 字段 | AiStreamChunk.java | 纯加字段，低风险。**流式链路闭环**：确保 fallback 后计费按实际命中模型计算 |
| 1.10 | Provider 新增 asrEndpoint、ttsEndpoint、unsupportedParams | Provider.java + migration SQL | 纯加字段，低风险 |
| 1.11 | 新建 BinaryResponse 二进制响应载体 | BinaryResponse.java（新建） | TTS 透传用，携带 bytes + upstream status + upstream headers |
| 1.12 | 新建 ProxyResult\<T\> 泛型包装 | ProxyResult.java（新建） | 封装 response + actualModel（AiModel 聚合根），确保计费事件签名兼容 |

**1.1~1.6 必须作为原子提交**：content 类型变更和所有消费方修复必须在同一个提交中完成，否则编译不过或运行时崩溃。

### 阶段 2 — 核心路由与适配器

| 步骤 | 改动 | 文件 |
|---|---|---|
| 2.1 | AiProxyService 加 dispatchByType 路由 | AiProxyService.java |
| 2.2 | GenericOpenAiAdapter 重构：提取共享 HTTP 基础设施 | GenericOpenAiAdapter.java |
| 2.3 | GenericOpenAiAdapter 实现 chat rawBody 透传 | GenericOpenAiAdapter.java |
| 2.4 | GenericOpenAiAdapter 实现 embedding() | GenericOpenAiAdapter.java |
| 2.5 | GenericOpenAiAdapter 实现 generateImage() | GenericOpenAiAdapter.java |
| 2.6 | GenericOpenAiAdapter 实现 speechToText() (multipart) | GenericOpenAiAdapter.java |
| 2.7 | GenericOpenAiAdapter 实现 textToSpeech() (binary response) | GenericOpenAiAdapter.java |
| 2.8 | GenericOpenAiAdapter 实现 generateVideo() | GenericOpenAiAdapter.java |
| 2.9 | GenericOpenAiAdapter 加 resolveEndpoint 能力检查 | GenericOpenAiAdapter.java |

### 阶段 3 — 对外 API 接入（双控制器 + 入口链路完整闭环）

| 步骤 | 改动 | 文件 |
|---|---|---|
| 3.1 | FilterConfig 注册 `/v1/*` 到认证 + 限流过滤器 | FilterConfig.java |
| 3.2 | 新建 OpenAiErrorResponseWriter 工具类 | OpenAiErrorResponseWriter.java（新建） |
| 3.3 | ApiKeyAuthenticationFilter 改造：/v1/ 路径异常时写回 OpenAI 格式 JSON | ApiKeyAuthenticationFilter.java |
| 3.4 | RateLimitFilter 改造：/v1/ 路径异常时写回 OpenAI 格式 JSON | RateLimitFilter.java |
| 3.5 | 新建 OpenAI 兼容 Chat 控制器（/v1/chat/completions） | OpenAiCompatChatController.java（新建） |
| 3.6 | 新建 OpenAI 兼容 Embeddings 控制器（/v1/embeddings） | OpenAiCompatEmbeddingsController.java（新建） |
| 3.7 | 新建 OpenAI 兼容 Image 控制器（/v1/images/generations） | OpenAiCompatImageController.java（新建） |
| 3.8 | 新建 OpenAI 兼容 Audio 控制器（ASR multipart + TTS binary） | OpenAiCompatAudioController.java（新建） |
| 3.9 | 新建 OpenAI 兼容 Video 控制器 | OpenAiCompatVideoController.java（新建） |
| 3.10 | 新建 ModelsController（/v1/models） | ModelsController.java（新建） |
| 3.11 | 现有 ChatController/EmbeddingsController/ImageController 保持不变 | 不修改 |

> **3.1~3.4 必须与 3.5+ 同时上线**：如果先上控制器不上过滤器改造，/v1 端点将无认证无限流。
> OpenAI 兼容控制器统一放在 `com.fsa.aicenter.interfaces.api.openai` 包下，与现有 `controller` 包隔离，便于 `@RestControllerAdvice(basePackages = ...)` 精确匹配异常处理器。

### 阶段 4 — 完善层

| 步骤 | 改动 | 文件 |
|---|---|---|
| 4.1 | OpenAI 兼容错误处理器（Controller 层异常） | OpenAiErrorHandler.java（新建） |
| 4.2 | 各模态计费逻辑（使用 ProxyResult.actualModel 而非 selectedModel） | 各 Controller 内 |
| 4.3 | 流式计费改造：从 AiStreamChunk.actualModel 取实际命中模型 | ChatController 流式路径 |
| 4.4 | 集成测试 | 各模态端到端测试 |
| 4.5 | 过滤器异常格式测试 | /v1 路径 401/429 返回 OpenAI JSON 验证 |

### 文件变更总览

| 类型 | 文件数 | 说明 |
|---|---|---|
| 修改 | 13+ | Message, AiRequest, AiResponse, AiStreamChunk, Provider, AiProxyService, GenericOpenAiAdapter, FilterConfig, ApiKeyAuthenticationFilter, RateLimitFilter, 6 个其他适配器（Doubao/Wenxin/Ollama/Vllm/OpenAi/Spark）, ChatController(estimateTokens + 流式计费) |
| 新建 | 12 | ValidContentValidator, BinaryResponse, ProxyResult, OpenAiErrorResponseWriter, OpenAiCompatChatController, OpenAiCompatEmbeddingsController, OpenAiCompatImageController, OpenAiCompatAudioController, OpenAiCompatVideoController, ModelsController, OpenAiErrorHandler, AiProviderAdapter(textToSpeechBinary) |
| SQL | 1 | Provider 加三列（asr_endpoint, tts_endpoint, unsupported_params） |
| **总计** | **26+** | |

### 逐阶段验证

- 阶段 1 完成 → **全量编译通过 + 现有单元测试全绿**（content 类型变更的波及面大，必须跑完整测试套件）
- 阶段 2 完成 → 内部单元测试验证各模态链路 + fallback 参数裁剪测试 + **流式 fallback 验证 AiStreamChunk.actualModel 正确回填**
- 阶段 3 完成 → 用 OpenAI SDK 对接测试各端点（同时验证旧路径 `/api/v1/ai/...` 不受影响）+ **验证 /v1 无 API Key 请求返回 401 OpenAI 格式 JSON** + **验证 /v1 限流触发返回 429 OpenAI 格式 JSON**
- 阶段 4 完成 → 错误场景（双协议异常格式验证）、计费验证（确认非流式和流式都使用 actualModel）、**TTS 端点验证返回的 Content-Type 与请求的 response_format 一致（不硬编码 audio/mpeg）**
