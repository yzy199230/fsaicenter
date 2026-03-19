-- 为每个 AI 供应商添加最新旗舰模型（截至 2026 年 3 月）
-- 使用子查询通过 provider_code 关联 provider_id

-- 1. OpenAI - GPT-5.4 (2026-03-05 发布，1.05M 上下文)
INSERT INTO ai_model (provider_id, model_code, model_name, model_type, support_stream, max_tokens, config, capabilities, description, status, sort_order)
SELECT id, 'gpt-5.4', 'GPT-5.4', 'CHAT', true, 1050000,
    '{"temperature": 0.7, "top_p": 1.0}'::jsonb,
    '{"chat": true, "vision": true, "function_call": true, "json_mode": true, "computer_use": true}'::jsonb,
    'OpenAI 最新旗舰模型，1.05M 上下文，内置 Computer Use，支持 128K 输出', 1, 1
FROM ai_provider WHERE provider_code = 'openai'
ON CONFLICT (provider_id, model_code) DO NOTHING;

-- 2. Anthropic - Claude Sonnet 4.6 (2026-02-17 发布，1M 上下文)
INSERT INTO ai_model (provider_id, model_code, model_name, model_type, support_stream, max_tokens, config, capabilities, description, status, sort_order)
SELECT id, 'claude-sonnet-4-6-20260217', 'Claude Sonnet 4.6', 'CHAT', true, 1000000,
    '{"temperature": 0.7, "top_p": 1.0}'::jsonb,
    '{"chat": true, "vision": true, "function_call": true, "extended_thinking": true, "computer_use": true}'::jsonb,
    'Anthropic 最强 Sonnet，1M 上下文，编码/Agent/长文推理全面升级', 1, 1
FROM ai_provider WHERE provider_code = 'anthropic'
ON CONFLICT (provider_id, model_code) DO NOTHING;

-- 3. Google - Gemini 3.1 Pro (2026-02-19 发布，1M 上下文，65K 输出)
INSERT INTO ai_model (provider_id, model_code, model_name, model_type, support_stream, max_tokens, config, capabilities, description, status, sort_order)
SELECT id, 'gemini-3.1-pro-preview', 'Gemini 3.1 Pro', 'CHAT', true, 1048576,
    '{"temperature": 0.7, "top_p": 1.0}'::jsonb,
    '{"chat": true, "vision": true, "function_call": true, "code_execution": true, "thinking": true}'::jsonb,
    'Google 最新旗舰推理模型，1M 上下文，65K 输出，推理能力较 3.0 Pro 提升 2x+', 1, 1
FROM ai_provider WHERE provider_code = 'google'
ON CONFLICT (provider_id, model_code) DO NOTHING;

-- 4. 阿里云 DashScope - Qwen3.5 Plus (2026-02-15 发布，397B 总参/17B 激活)
INSERT INTO ai_model (provider_id, model_code, model_name, model_type, support_stream, max_tokens, config, capabilities, description, status, sort_order)
SELECT id, 'qwen3.5-plus', 'Qwen3.5 Plus', 'CHAT', true, 131072,
    '{"temperature": 0.7, "top_p": 0.8, "enable_thinking": false}'::jsonb,
    '{"chat": true, "vision": true, "video": true, "function_call": true, "thinking": true, "search_agent": true}'::jsonb,
    '通义千问 3.5 Plus，397B 总参/17B 激活，多模态，性能超 Qwen3-Max 且成本更低', 1, 1
FROM ai_provider WHERE provider_code = 'dashscope'
ON CONFLICT (provider_id, model_code) DO NOTHING;

-- 5. 火山引擎 - Doubao Seed 2.0 Pro (2026-02 发布，Seed 系列最新)
INSERT INTO ai_model (provider_id, model_code, model_name, model_type, support_stream, max_tokens, config, capabilities, description, status, sort_order)
SELECT id, 'doubao-seed-2-0-pro', 'Doubao Seed 2.0 Pro', 'CHAT', true, 256000,
    '{"temperature": 0.7, "top_p": 0.9}'::jsonb,
    '{"chat": true, "vision": true, "function_call": true, "thinking": true, "multimodal": true}'::jsonb,
    '字节跳动豆包 Seed 2.0 旗舰版，256K 上下文，深度推理 + Agent，成本约为西方竞品 1/10', 1, 1
FROM ai_provider WHERE provider_code = 'volcengine'
ON CONFLICT (provider_id, model_code) DO NOTHING;

-- 6. 百度 - ERNIE 5.0 (2026-01-22 发布，2.4T 参数原生全模态)
INSERT INTO ai_model (provider_id, model_code, model_name, model_type, support_stream, max_tokens, config, capabilities, description, status, sort_order)
SELECT id, 'ernie-5.0-thinking-latest', 'ERNIE 5.0', 'CHAT', true, 128000,
    '{"temperature": 0.7, "top_p": 0.8}'::jsonb,
    '{"chat": true, "vision": true, "function_call": true, "thinking": true, "multimodal": true}'::jsonb,
    '百度文心 5.0，2.4T 参数原生全模态模型，支持文本/图像/音频/视频输入输出', 1, 1
FROM ai_provider WHERE provider_code = 'baidu'
ON CONFLICT (provider_id, model_code) DO NOTHING;

-- 7. 腾讯混元 - Hunyuan TurboS (2025-02 发布，560B 总参/56B 激活，Mamba+Transformer)
INSERT INTO ai_model (provider_id, model_code, model_name, model_type, support_stream, max_tokens, config, capabilities, description, status, sort_order)
SELECT id, 'hunyuan-turbos-latest', 'Hunyuan TurboS', 'CHAT', true, 256000,
    '{"temperature": 0.7, "top_p": 1.0}'::jsonb,
    '{"chat": true, "function_call": true, "thinking": true}'::jsonb,
    '腾讯混元 TurboS，560B 参数 Mamba+Transformer 混合架构，自适应思维链，256K 上下文', 1, 1
FROM ai_provider WHERE provider_code = 'tencent'
ON CONFLICT (provider_id, model_code) DO NOTHING;

-- 8. 智谱 AI - GLM-5 (2026-02-11 发布，744B MoE/40B 激活，MIT 开源)
INSERT INTO ai_model (provider_id, model_code, model_name, model_type, support_stream, max_tokens, config, capabilities, description, status, sort_order)
SELECT id, 'glm-5', 'GLM-5', 'CHAT', true, 200000,
    '{"temperature": 0.7, "top_p": 0.7}'::jsonb,
    '{"chat": true, "vision": true, "function_call": true, "agent": true}'::jsonb,
    '智谱 GLM-5，744B MoE/40B 激活，200K 上下文，Coding 和 Agent 开源 SOTA', 1, 1
FROM ai_provider WHERE provider_code = 'zhipu'
ON CONFLICT (provider_id, model_code) DO NOTHING;

-- 9. 月之暗面 - Kimi K2.5 (2026-01-27 发布，1T MoE/32B 激活，原生多模态)
INSERT INTO ai_model (provider_id, model_code, model_name, model_type, support_stream, max_tokens, config, capabilities, description, status, sort_order)
SELECT id, 'kimi-k2.5', 'Kimi K2.5', 'CHAT', true, 256000,
    '{"temperature": 0.7, "top_p": 0.95}'::jsonb,
    '{"chat": true, "vision": true, "video": true, "function_call": true, "thinking": true, "agent_swarm": true}'::jsonb,
    '月之暗面 Kimi K2.5，1T MoE/32B 激活，256K 上下文，原生多模态 + Agent Swarm', 1, 1
FROM ai_provider WHERE provider_code = 'moonshot'
ON CONFLICT (provider_id, model_code) DO NOTHING;

-- 10. 深度求索 - DeepSeek V3.2 (2025-12 发布，思考+工具一体化)
INSERT INTO ai_model (provider_id, model_code, model_name, model_type, support_stream, max_tokens, config, capabilities, description, status, sort_order)
SELECT id, 'deepseek-chat', 'DeepSeek V3.2', 'CHAT', true, 128000,
    '{"temperature": 0.7, "top_p": 1.0}'::jsonb,
    '{"chat": true, "function_call": true, "thinking": true, "reasoning": true}'::jsonb,
    'DeepSeek V3.2（deepseek-chat），128K 上下文，首个将思考集成到工具调用的模型，IMO 金牌', 1, 1
FROM ai_provider WHERE provider_code = 'deepseek'
ON CONFLICT (provider_id, model_code) DO NOTHING;

-- 11. MiniMax - M2.5 (2026-02-11 发布，228.7B MoE，192K 上下文)
INSERT INTO ai_model (provider_id, model_code, model_name, model_type, support_stream, max_tokens, config, capabilities, description, status, sort_order)
SELECT id, 'MiniMax-M2.5', 'MiniMax M2.5', 'CHAT', true, 192000,
    '{"temperature": 0.7, "top_p": 1.0}'::jsonb,
    '{"chat": true, "function_call": true, "thinking": true, "agent": true}'::jsonb,
    'MiniMax M2.5，228.7B MoE，192K 上下文，SWE-Bench 80.2% SOTA，极致性价比', 1, 1
FROM ai_provider WHERE provider_code = 'minimax'
ON CONFLICT (provider_id, model_code) DO NOTHING;
