package com.fsa.aicenter.infrastructure.adapter.generic;

import com.fsa.aicenter.application.service.ModelApiKeySelector;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.entity.ModelApiKey;
import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.repository.ProviderRepository;
import com.fsa.aicenter.infrastructure.adapter.common.AiRequest;
import com.fsa.aicenter.infrastructure.adapter.common.AiResponse;
import com.fsa.aicenter.infrastructure.adapter.common.AiStreamChunk;
import com.fsa.aicenter.infrastructure.adapter.common.Message;
import okhttp3.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenericOpenAiAdapterTest {

    private MockWebServer mockWebServer;
    private GenericOpenAiAdapter adapter;

    @Mock
    private ModelApiKeySelector modelApiKeySelector;

    @Mock
    private ProviderRepository providerRepository;

    private OkHttpClient httpClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        adapter = new GenericOpenAiAdapter(httpClient, modelApiKeySelector, providerRepository);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("supports() - 支持 openai_compatible 协议")
    void supports_openaiCompatible_returnsTrue() {
        Provider provider = createProvider("openai_compatible");
        assertThat(adapter.supports(provider)).isTrue();
    }

    @Test
    @DisplayName("supports() - 支持 openai_like 协议")
    void supports_openaiLike_returnsTrue() {
        Provider provider = createProvider("openai_like");
        assertThat(adapter.supports(provider)).isTrue();
    }

    @Test
    @DisplayName("supports() - 支持 custom_http 协议")
    void supports_customHttp_returnsTrue() {
        Provider provider = createProvider("custom_http");
        assertThat(adapter.supports(provider)).isTrue();
    }

    @Test
    @DisplayName("supports() - 不支持 websocket 协议")
    void supports_websocket_returnsFalse() {
        Provider provider = createProvider("websocket");
        assertThat(adapter.supports(provider)).isFalse();
    }

    @Test
    @DisplayName("supports() - 不支持 sdk_required 协议")
    void supports_sdkRequired_returnsFalse() {
        Provider provider = createProvider("sdk_required");
        assertThat(adapter.supports(provider)).isFalse();
    }

    @Test
    @DisplayName("supports() - provider为null返回false")
    void supports_nullProvider_returnsFalse() {
        assertThat(adapter.supports(null)).isFalse();
    }

    @Test
    @DisplayName("supports() - protocolType为null返回false")
    void supports_nullProtocolType_returnsFalse() {
        Provider provider = new Provider();
        provider.setProtocolType(null);
        assertThat(adapter.supports(provider)).isFalse();
    }

    @Test
    @DisplayName("call() - 成功处理OpenAI格式响应")
    void call_success_parsesOpenAiResponse() {
        String responseBody = """
            {
                "id": "chatcmpl-123",
                "object": "chat.completion",
                "created": 1677652288,
                "model": "gpt-3.5-turbo",
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "Hello! How can I help you today?"
                    },
                    "finish_reason": "stop"
                }],
                "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 20,
                    "total_tokens": 30
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createChatRequest();
        Mono<AiResponse> result = adapter.call(model, request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo("chatcmpl-123");
                    assertThat(response.getContent()).isEqualTo("Hello! How can I help you today?");
                    assertThat(response.getPromptTokens()).isEqualTo(10);
                    assertThat(response.getCompletionTokens()).isEqualTo(20);
                    assertThat(response.getTotalTokens()).isEqualTo(30);
                    assertThat(response.getFinishReason()).isEqualTo("stop");
                    assertThat(response.getModel()).isEqualTo("gpt-3.5-turbo");
                })
                .verifyComplete();

        verify(modelApiKeySelector).recordSuccess(apiKey.getId());
        verify(modelApiKeySelector).consumeQuota(apiKey.getId(), 30);
    }

    @Test
    @DisplayName("call() - 处理401认证错误")
    void call_authError_throwsException() {
        String errorBody = """
            {
                "error": {
                    "message": "Invalid API key",
                    "type": "invalid_request_error",
                    "code": "invalid_api_key"
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(errorBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createChatRequest();
        Mono<AiResponse> result = adapter.call(model, request);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e.getMessage().contains("Invalid API key"))
                .verify();

        verify(modelApiKeySelector).recordFailure(apiKey.getId());
    }

    @Test
    @DisplayName("call() - 处理429限流错误")
    void call_rateLimitError_throwsException() {
        String errorBody = """
            {
                "error": {
                    "message": "Rate limit exceeded",
                    "type": "rate_limit_error"
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(429)
                .setBody(errorBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createChatRequest();
        Mono<AiResponse> result = adapter.call(model, request);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e.getMessage().contains("Rate limit"))
                .verify();
    }

    @Test
    @DisplayName("callStream() - 成功解析SSE流式响应")
    void callStream_success_parsesSSEChunks() {
        String sseResponse = """
            data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"role":"assistant","content":""},"finish_reason":null}]}

            data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

            data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":null}]}

            data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":2,"total_tokens":12}}

            data: [DONE]

            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(sseResponse)
                .setHeader("Content-Type", "text/event-stream"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createChatRequest();
        request.setStream(true);

        Flux<AiStreamChunk> result = adapter.callStream(model, request);

        StepVerifier.create(result)
                .assertNext(chunk -> {
                    assertThat(chunk.getId()).isEqualTo("chatcmpl-123");
                    assertThat(chunk.isDone()).isFalse();
                })
                .assertNext(chunk -> {
                    assertThat(chunk.getDelta()).isEqualTo("Hello");
                    assertThat(chunk.isDone()).isFalse();
                })
                .assertNext(chunk -> {
                    assertThat(chunk.getDelta()).isEqualTo("!");
                    assertThat(chunk.isDone()).isFalse();
                })
                .assertNext(chunk -> {
                    assertThat(chunk.isDone()).isTrue();
                    assertThat(chunk.getFinishReason()).isEqualTo("stop");
                    assertThat(chunk.getTotalTokens()).isEqualTo(12);
                })
                .verifyComplete();

        verify(modelApiKeySelector).recordSuccess(apiKey.getId());
    }

    @Test
    @DisplayName("call() - 使用response_mapping解析自定义响应格式")
    void call_withResponseMapping_parsesCustomFormat() {
        String responseBody = """
            {
                "request_id": "req-456",
                "result": {
                    "text": "This is the response content"
                },
                "usage": {
                    "input_tokens": 15,
                    "output_tokens": 25
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setProtocolType("openai_like");
        provider.setResponseMapping("""
            {
                "content_path": "result.text",
                "id_path": "request_id",
                "usage": {
                    "prompt_tokens": "usage.input_tokens",
                    "completion_tokens": "usage.output_tokens"
                }
            }
            """);

        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createChatRequest();
        Mono<AiResponse> result = adapter.call(model, request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo("req-456");
                    assertThat(response.getContent()).isEqualTo("This is the response content");
                    assertThat(response.getPromptTokens()).isEqualTo(15);
                    assertThat(response.getCompletionTokens()).isEqualTo(25);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("call() - 正确添加额外请求头")
    void call_withExtraHeaders_addsHeaders() throws InterruptedException {
        String responseBody = """
            {
                "id": "chatcmpl-123",
                "choices": [{
                    "message": {"content": "OK"},
                    "finish_reason": "stop"
                }],
                "usage": {"prompt_tokens": 5, "completion_tokens": 1, "total_tokens": 6}
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setExtraHeaders("""
            {"X-Custom-Header": "custom-value", "X-Another": "another-value"}
            """);

        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createChatRequest();
        adapter.call(model, request).block();

        var recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("X-Custom-Header")).isEqualTo("custom-value");
        assertThat(recordedRequest.getHeader("X-Another")).isEqualTo("another-value");
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-api-key");
    }

    @Test
    @DisplayName("getProviderCode() - 返回__generic__")
    void getProviderCode_returnsGeneric() {
        assertThat(adapter.getProviderCode()).isEqualTo("__generic__");
    }

    @Test
    @DisplayName("embedding() - 成功处理OpenAI格式Embedding响应")
    void embedding_success_parsesOpenAiResponse() {
        String responseBody = """
            {
                "object": "list",
                "data": [{
                    "index": 0,
                    "embedding": [0.0023, -0.0152, 0.0087, 0.0234, -0.0098]
                }],
                "model": "text-embedding-ada-002",
                "usage": {
                    "prompt_tokens": 8,
                    "total_tokens": 8
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setEmbeddingEndpoint("/embeddings");
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createEmbeddingRequest();
        Mono<AiResponse> result = adapter.embedding(model, request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getModel()).isEqualTo("text-embedding-ada-002");
                    assertThat(response.getEmbedding()).isNotNull();
                    assertThat(response.getEmbedding()).hasSize(5);
                    assertThat(response.getEmbedding().get(0)).isCloseTo(0.0023, org.assertj.core.api.Assertions.within(0.0001));
                    assertThat(response.getPromptTokens()).isEqualTo(8);
                    assertThat(response.getTotalTokens()).isEqualTo(8);
                })
                .verifyComplete();

        verify(modelApiKeySelector).recordSuccess(apiKey.getId());
        verify(modelApiKeySelector).consumeQuota(apiKey.getId(), 8);
    }

    @Test
    @DisplayName("embedding() - 使用response_mapping解析自定义Embedding格式")
    void embedding_withResponseMapping_parsesCustomFormat() {
        String responseBody = """
            {
                "request_id": "req-789",
                "vectors": [{
                    "idx": 0,
                    "vec": [0.0123, -0.0234, 0.0345]
                }],
                "model_name": "custom-embedding",
                "token_usage": {
                    "input_tokens": 12
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setEmbeddingEndpoint("/embeddings");
        provider.setProtocolType("openai_like");
        provider.setResponseMapping("""
            {
                "id_path": "request_id",
                "model_path": "model_name",
                "data_path": "vectors",
                "embedding_path": "vec",
                "usage": {
                    "prompt_tokens": "token_usage.input_tokens",
                    "total_tokens": "token_usage.input_tokens"
                }
            }
            """);

        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createEmbeddingRequest();
        Mono<AiResponse> result = adapter.embedding(model, request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo("req-789");
                    assertThat(response.getModel()).isEqualTo("custom-embedding");
                    assertThat(response.getEmbedding()).isNotNull();
                    assertThat(response.getEmbedding()).hasSize(3);
                    assertThat(response.getPromptTokens()).isEqualTo(12);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("embedding() - 处理401认证错误")
    void embedding_authError_throwsException() {
        String errorBody = """
            {
                "error": {
                    "message": "Invalid API key for embedding",
                    "type": "invalid_request_error"
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(errorBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setEmbeddingEndpoint("/embeddings");
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createEmbeddingRequest();
        Mono<AiResponse> result = adapter.embedding(model, request);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e.getMessage().contains("Invalid API key"))
                .verify();

        verify(modelApiKeySelector).recordFailure(apiKey.getId());
    }

    @Test
    @DisplayName("embedding() - 正确添加encodingFormat和dimensions参数")
    void embedding_withOptionalParameters_addsToRequest() throws InterruptedException {
        String responseBody = """
            {
                "object": "list",
                "data": [{
                    "index": 0,
                    "embedding": [0.1, 0.2, 0.3]
                }],
                "model": "text-embedding-3-small",
                "usage": {"prompt_tokens": 5, "total_tokens": 5}
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setEmbeddingEndpoint("/embeddings");
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createEmbeddingRequest();
        request.setEncodingFormat("float");
        request.setDimensions(512);

        adapter.embedding(model, request).block();

        var recordedRequest = mockWebServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();

        assertThat(requestBody).contains("\"encoding_format\":\"float\"");
        assertThat(requestBody).contains("\"dimensions\":512");
    }

    private Provider createProvider(String protocolType) {
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setCode("test-provider");
        provider.setName("Test Provider");
        provider.setProtocolType(protocolType);
        provider.setBaseUrl("https://api.test.com/v1");
        return provider;
    }

    private Provider createProviderWithUrl(String baseUrl) {
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setCode("test-provider");
        provider.setName("Test Provider");
        provider.setProtocolType("openai_compatible");
        provider.setBaseUrl(baseUrl);
        provider.setChatEndpoint("/chat/completions");
        provider.setAuthHeader("Authorization");
        provider.setAuthPrefix("Bearer ");
        return provider;
    }

    private AiModel createModel(Long providerId) {
        AiModel model = new AiModel();
        model.setId(100L);
        model.setCode("test-model");
        model.setName("Test Model");
        model.setProviderId(providerId);
        return model;
    }

    private ModelApiKey createApiKey() {
        ModelApiKey apiKey = new ModelApiKey();
        apiKey.setId(1L);
        apiKey.setKeyName("test-key");
        apiKey.setApiKey("test-api-key");
        return apiKey;
    }

    private AiRequest createChatRequest() {
        return AiRequest.builder()
                .model("test-model")
                .messages(List.of(
                        new Message("user", "Hello")
                ))
                .temperature(0.7)
                .maxTokens(100)
                .build();
    }

    private AiRequest createEmbeddingRequest() {
        return AiRequest.builder()
                .model("test-embedding-model")
                .input("Hello, world!")
                .build();
    }

    @Test
    @DisplayName("generateVideo() - 成功处理OpenAI格式Video响应")
    void generateVideo_success_parsesOpenAiResponse() {
        String responseBody = """
            {
                "id": "video-123",
                "object": "video.generation",
                "created": 1677652288,
                "model": "video-model-v1",
                "data": [{
                    "url": "https://example.com/video1.mp4",
                    "duration": 10,
                    "size": "1024x1024",
                    "aspect_ratio": "1:1"
                }]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setVideoEndpoint("/videos/generations");
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createVideoRequest();
        Mono<AiResponse> result = adapter.generateVideo(model, request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo("video-123");
                    assertThat(response.getModel()).isEqualTo("video-model-v1");
                    assertThat(response.getVideoUrls()).isNotNull();
                    assertThat(response.getVideoUrls()).hasSize(1);
                    assertThat(response.getVideoUrls().get(0)).isEqualTo("https://example.com/video1.mp4");
                    assertThat(response.getCreated()).isEqualTo(1677652288L);
                })
                .verifyComplete();

        verify(modelApiKeySelector).recordSuccess(apiKey.getId());
    }

    @Test
    @DisplayName("generateVideo() - 处理包含b64_json的Video响应")
    void generateVideo_withB64Json_parsesBase64Video() {
        String responseBody = """
            {
                "id": "video-456",
                "object": "video.generation",
                "created": 1677652290,
                "model": "video-model-v1",
                "data": [{
                    "b64_json": "base64encodedvideodata",
                    "duration": 15,
                    "size": "1920x1080",
                    "aspect_ratio": "16:9"
                }]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setVideoEndpoint("/videos/generations");
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createVideoRequest();
        Mono<AiResponse> result = adapter.generateVideo(model, request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo("video-456");
                    assertThat(response.getVideoUrls()).isNotNull();
                    assertThat(response.getVideoUrls()).hasSize(1);
                    assertThat(response.getVideoUrls().get(0)).startsWith("data:video/mp4;base64,");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("generateVideo() - 使用response_mapping解析自定义Video格式")
    void generateVideo_withResponseMapping_parsesCustomFormat() {
        String responseBody = """
            {
                "request_id": "video-req-789",
                "result": {
                    "videos": [{
                        "video_url": "https://custom.com/video.mp4",
                        "video_duration": 20,
                        "video_size": "720p",
                        "ratio": "9:16"
                    }]
                },
                "model_name": "custom-video-model"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setVideoEndpoint("/videos/generations");
        provider.setProtocolType("openai_like");
        provider.setResponseMapping("""
            {
                "id_path": "request_id",
                "model_path": "model_name",
                "data_path": "result.videos",
                "url_path": "video_url",
                "duration_path": "video_duration",
                "size_path": "video_size",
                "aspect_ratio_path": "ratio"
            }
            """);

        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createVideoRequest();
        Mono<AiResponse> result = adapter.generateVideo(model, request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo("video-req-789");
                    assertThat(response.getModel()).isEqualTo("custom-video-model");
                    assertThat(response.getVideoUrls()).isNotNull();
                    assertThat(response.getVideoUrls()).hasSize(1);
                    assertThat(response.getVideoUrls().get(0)).isEqualTo("https://custom.com/video.mp4");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("generateVideo() - 处理401认证错误")
    void generateVideo_authError_throwsException() {
        String errorBody = """
            {
                "error": {
                    "message": "Invalid API key for video generation",
                    "type": "invalid_request_error"
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(errorBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setVideoEndpoint("/videos/generations");
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = createVideoRequest();
        Mono<AiResponse> result = adapter.generateVideo(model, request);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e.getMessage().contains("Invalid API key"))
                .verify();

        verify(modelApiKeySelector).recordFailure(apiKey.getId());
    }

    @Test
    @DisplayName("generateVideo() - 正确添加可选参数")
    void generateVideo_withOptionalParameters_addsToRequest() throws InterruptedException {
        String responseBody = """
            {
                "id": "video-999",
                "data": [{"url": "https://example.com/video.mp4"}]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setVideoEndpoint("/videos/generations");
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = AiRequest.builder()
                .model("test-video-model")
                .prompt("A beautiful sunset")
                .n(2)
                .duration(15)
                .size("1920x1080")
                .aspectRatio("16:9")
                .responseFormat("url")
                .build();

        adapter.generateVideo(model, request).block();

        var recordedRequest = mockWebServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();

        assertThat(requestBody).contains("\"prompt\":\"A beautiful sunset\"");
        assertThat(requestBody).contains("\"n\":2");
        assertThat(requestBody).contains("\"duration\":15");
        assertThat(requestBody).contains("\"size\":\"1920x1080\"");
        assertThat(requestBody).contains("\"aspect_ratio\":\"16:9\"");
        assertThat(requestBody).contains("\"response_format\":\"url\"");
    }

    private AiRequest createVideoRequest() {
        return AiRequest.builder()
                .model("test-video-model")
                .prompt("A beautiful sunset over the ocean")
                .build();
    }

    @Test
    @DisplayName("generateImage() - 成功解析OpenAI格式响应")
    void generateImage_success_parsesOpenAiResponse() throws InterruptedException {
        String responseBody = """
            {
                "created": 1234567890,
                "data": [
                    {
                        "url": "https://example.com/image1.png",
                        "revised_prompt": "A beautiful sunset over the ocean"
                    },
                    {
                        "b64_json": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setImageEndpoint("/images/generations");
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = AiRequest.builder()
                .model("test-image-model")
                .prompt("A beautiful sunset")
                .build();

        Mono<AiResponse> result = adapter.generateImage(model, request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getImageUrls()).hasSize(2);
                    assertThat(response.getImageUrls().get(0)).isEqualTo("https://example.com/image1.png");
                    assertThat(response.getImageUrls().get(1)).startsWith("data:image/png;base64,");
                    assertThat(response.getCreated()).isEqualTo(1234567890L);
                })
                .verifyComplete();

        verify(modelApiKeySelector).recordSuccess(apiKey.getId());
    }

    @Test
    @DisplayName("generateImage() - 使用b64_json格式解析Base64图片")
    void generateImage_withB64Json_parsesBase64Image() throws InterruptedException {
        String responseBody = """
            {
                "created": 1234567890,
                "data": [
                    {
                        "b64_json": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setImageEndpoint("/images/generations");
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = AiRequest.builder()
                .model("test-image-model")
                .prompt("A cat")
                .responseFormat("b64_json")
                .build();

        Mono<AiResponse> result = adapter.generateImage(model, request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getImageUrls()).hasSize(1);
                    assertThat(response.getImageUrls().get(0)).startsWith("data:image/png;base64,");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("generateImage() - 使用response_mapping解析自定义格式")
    void generateImage_withResponseMapping_parsesCustomFormat() throws InterruptedException {
        String responseBody = """
            {
                "timestamp": 1234567890,
                "images": [
                    {
                        "image_url": "https://custom-api.com/img1.png",
                        "prompt_refined": "A refined sunset"
                    }
                ]
            }
            """;

        String mappingConfig = """
            {
                "created_path": "timestamp",
                "data_path": "images",
                "url_path": "image_url",
                "revised_prompt_path": "prompt_refined"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setImageEndpoint("/custom/images");
        provider.setResponseMapping(mappingConfig);
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = AiRequest.builder()
                .model("test-image-model")
                .prompt("A sunset")
                .build();

        Mono<AiResponse> result = adapter.generateImage(model, request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getImageUrls()).hasSize(1);
                    assertThat(response.getImageUrls().get(0)).isEqualTo("https://custom-api.com/img1.png");
                    assertThat(response.getCreated()).isEqualTo(1234567890L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("generateImage() - 认证错误抛出异常")
    void generateImage_authError_throwsException() throws InterruptedException {
        String responseBody = """
            {
                "error": {
                    "message": "Invalid API key",
                    "type": "authentication_error"
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setImageEndpoint("/images/generations");
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = AiRequest.builder()
                .model("test-image-model")
                .prompt("A sunset")
                .build();

        Mono<AiResponse> result = adapter.generateImage(model, request);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e.getMessage().contains("Invalid API key"))
                .verify();

        verify(modelApiKeySelector).recordFailure(apiKey.getId());
    }

    @Test
    @DisplayName("generateImage() - 正确添加可选参数")
    void generateImage_withOptionalParameters_addsToRequest() throws InterruptedException {
        String responseBody = """
            {
                "created": 1234567890,
                "data": [{"url": "https://example.com/image.png"}]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        Provider provider = createProviderWithUrl(mockWebServer.url("/v1").toString());
        provider.setImageEndpoint("/images/generations");
        AiModel model = createModel(1L);
        ModelApiKey apiKey = createApiKey();

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(modelApiKeySelector.selectKey(anyLong())).thenReturn(Optional.of(apiKey));

        AiRequest request = AiRequest.builder()
                .model("test-image-model")
                .prompt("A beautiful sunset")
                .negativePrompt("blurry, low quality")
                .n(2)
                .size("1024x1024")
                .responseFormat("url")
                .style("vivid")
                .quality("hd")
                .build();

        adapter.generateImage(model, request).block();

        var recordedRequest = mockWebServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();

        assertThat(requestBody).contains("\"prompt\":\"A beautiful sunset\"");
        assertThat(requestBody).contains("\"negative_prompt\":\"blurry, low quality\"");
        assertThat(requestBody).contains("\"n\":2");
        assertThat(requestBody).contains("\"size\":\"1024x1024\"");
        assertThat(requestBody).contains("\"response_format\":\"url\"");
        assertThat(requestBody).contains("\"style\":\"vivid\"");
        assertThat(requestBody).contains("\"quality\":\"hd\"");
    }
}