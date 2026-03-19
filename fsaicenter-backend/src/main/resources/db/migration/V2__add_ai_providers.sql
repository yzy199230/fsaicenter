-- 初始化 AI 供应商数据
-- 包含国内外主流 AI 模型供应商

-- 1. OpenAI
INSERT INTO ai_provider (provider_code, provider_name, provider_type, base_url, protocol_type,
    chat_endpoint, embedding_endpoint, image_endpoint,
    auth_type, auth_header, auth_prefix, api_key_required,
    description, status, sort_order)
VALUES ('openai', 'OpenAI', 'remote', 'https://api.openai.com', 'OPENAI_COMPATIBLE',
    '/v1/chat/completions', '/v1/embeddings', '/v1/images/generations',
    'BEARER', 'Authorization', 'Bearer ', true,
    'OpenAI GPT 系列模型，包括 GPT-4o、GPT-4、GPT-3.5 等', 1, 1)
ON CONFLICT (provider_code) DO NOTHING;

-- 2. Anthropic (Claude)
INSERT INTO ai_provider (provider_code, provider_name, provider_type, base_url, protocol_type,
    chat_endpoint,
    auth_type, auth_header, auth_prefix, api_key_required,
    description, status, sort_order)
VALUES ('anthropic', 'Anthropic (Claude)', 'remote', 'https://api.anthropic.com', 'CUSTOM',
    '/v1/messages',
    'API_KEY', 'x-api-key', '', true,
    'Anthropic Claude 系列模型，包括 Claude 4、Claude 3.5 等', 1, 2)
ON CONFLICT (provider_code) DO NOTHING;

-- 3. Google (Gemini)
INSERT INTO ai_provider (provider_code, provider_name, provider_type, base_url, protocol_type,
    chat_endpoint,
    auth_type, auth_header, auth_prefix, api_key_required,
    description, status, sort_order)
VALUES ('google', 'Google (Gemini)', 'remote', 'https://generativelanguage.googleapis.com', 'CUSTOM',
    '/v1beta/models/{model}:generateContent',
    'API_KEY', 'x-goog-api-key', '', true,
    'Google Gemini 系列模型，包括 Gemini 2.0、Gemini 1.5 等', 1, 3)
ON CONFLICT (provider_code) DO NOTHING;

-- 4. 阿里云 DashScope（通义千问）
INSERT INTO ai_provider (provider_code, provider_name, provider_type, base_url, protocol_type,
    chat_endpoint, embedding_endpoint, image_endpoint,
    auth_type, auth_header, auth_prefix, api_key_required,
    description, status, sort_order)
VALUES ('dashscope', '阿里云 DashScope', 'remote', 'https://dashscope.aliyuncs.com/compatible-mode', 'OPENAI_COMPATIBLE',
    '/v1/chat/completions', '/v1/embeddings', '/v1/images/generations',
    'BEARER', 'Authorization', 'Bearer ', true,
    '阿里云通义千问系列模型，包括 Qwen-Max、Qwen-Plus、Qwen-Turbo 等', 1, 4)
ON CONFLICT (provider_code) DO NOTHING;

-- 5. 火山引擎（豆包）
INSERT INTO ai_provider (provider_code, provider_name, provider_type, base_url, protocol_type,
    chat_endpoint, embedding_endpoint,
    auth_type, auth_header, auth_prefix, api_key_required,
    description, status, sort_order)
VALUES ('volcengine', '火山引擎 (豆包)', 'remote', 'https://ark.cn-beijing.volces.com/api', 'OPENAI_COMPATIBLE',
    '/v3/chat/completions', '/v3/embeddings',
    'BEARER', 'Authorization', 'Bearer ', true,
    '字节跳动火山引擎豆包大模型，包括 Doubao-pro、Doubao-lite 等', 1, 5)
ON CONFLICT (provider_code) DO NOTHING;

-- 6. 百度智能云（文心一言）
INSERT INTO ai_provider (provider_code, provider_name, provider_type, base_url, protocol_type,
    chat_endpoint, embedding_endpoint,
    auth_type, auth_header, auth_prefix, api_key_required,
    description, status, sort_order)
VALUES ('baidu', '百度智能云 (文心一言)', 'remote', 'https://qianfan.baidubce.com', 'OPENAI_COMPATIBLE',
    '/v2/chat/completions', '/v2/embeddings',
    'BEARER', 'Authorization', 'Bearer ', true,
    '百度文心一言系列模型，包括 ERNIE-4.0、ERNIE-3.5 等', 1, 6)
ON CONFLICT (provider_code) DO NOTHING;

-- 7. 腾讯混元
INSERT INTO ai_provider (provider_code, provider_name, provider_type, base_url, protocol_type,
    chat_endpoint,
    auth_type, auth_header, auth_prefix, api_key_required,
    description, status, sort_order)
VALUES ('tencent', '腾讯混元', 'remote', 'https://api.hunyuan.cloud.tencent.com', 'OPENAI_COMPATIBLE',
    '/v1/chat/completions',
    'BEARER', 'Authorization', 'Bearer ', true,
    '腾讯混元大模型，包括 Hunyuan-Pro、Hunyuan-Standard 等', 1, 7)
ON CONFLICT (provider_code) DO NOTHING;

-- 8. 智谱 AI（GLM）
INSERT INTO ai_provider (provider_code, provider_name, provider_type, base_url, protocol_type,
    chat_endpoint, embedding_endpoint, image_endpoint,
    auth_type, auth_header, auth_prefix, api_key_required,
    description, status, sort_order)
VALUES ('zhipu', '智谱 AI (GLM)', 'remote', 'https://open.bigmodel.cn/api/paas', 'OPENAI_COMPATIBLE',
    '/v4/chat/completions', '/v4/embeddings', '/v4/images/generations',
    'BEARER', 'Authorization', 'Bearer ', true,
    '智谱 AI GLM 系列模型，包括 GLM-4、GLM-4V、CogView 等', 1, 8)
ON CONFLICT (provider_code) DO NOTHING;

-- 9. 月之暗面（Kimi）
INSERT INTO ai_provider (provider_code, provider_name, provider_type, base_url, protocol_type,
    chat_endpoint,
    auth_type, auth_header, auth_prefix, api_key_required,
    description, status, sort_order)
VALUES ('moonshot', '月之暗面 (Kimi)', 'remote', 'https://api.moonshot.cn', 'OPENAI_COMPATIBLE',
    '/v1/chat/completions',
    'BEARER', 'Authorization', 'Bearer ', true,
    '月之暗面 Kimi 系列模型，包括 moonshot-v1-128k、moonshot-v1-32k 等', 1, 9)
ON CONFLICT (provider_code) DO NOTHING;

-- 10. 深度求索（DeepSeek）
INSERT INTO ai_provider (provider_code, provider_name, provider_type, base_url, protocol_type,
    chat_endpoint,
    auth_type, auth_header, auth_prefix, api_key_required,
    description, status, sort_order)
VALUES ('deepseek', '深度求索 (DeepSeek)', 'remote', 'https://api.deepseek.com', 'OPENAI_COMPATIBLE',
    '/v1/chat/completions',
    'BEARER', 'Authorization', 'Bearer ', true,
    '深度求索 DeepSeek 系列模型，包括 DeepSeek-V3、DeepSeek-R1 等', 1, 10)
ON CONFLICT (provider_code) DO NOTHING;

-- 11. MiniMax
INSERT INTO ai_provider (provider_code, provider_name, provider_type, base_url, protocol_type,
    chat_endpoint,
    auth_type, auth_header, auth_prefix, api_key_required,
    description, status, sort_order)
VALUES ('minimax', 'MiniMax', 'remote', 'https://api.minimax.chat', 'OPENAI_COMPATIBLE',
    '/v1/text/chatcompletion_v2',
    'BEARER', 'Authorization', 'Bearer ', true,
    'MiniMax 系列模型，包括 abab6.5s、abab5.5 等，支持语音合成与长文本', 1, 11)
ON CONFLICT (provider_code) DO NOTHING;
