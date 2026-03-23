-- 修复 MiniMax provider 配置：
-- 1. base_url 改为国内 API 地址 https://api.minimaxi.com
-- 2. chat_endpoint 改为 OpenAI 兼容端点 /v1/chat/completions（M2.5/M2.7 等新模型需要）
-- 3. 更新描述信息
UPDATE ai_provider
SET base_url = 'https://api.minimaxi.com',
    chat_endpoint = '/v1/chat/completions',
    description = 'MiniMax 系列模型，包括 M2.7、M2.5、M2.1 等，支持 204K 上下文'
WHERE provider_code = 'minimax';

-- 修复 MiniMax-M2.5 的 max_tokens 为官方值 204800
UPDATE ai_model
SET max_tokens = 204800
WHERE model_code = 'MiniMax-M2.5';
