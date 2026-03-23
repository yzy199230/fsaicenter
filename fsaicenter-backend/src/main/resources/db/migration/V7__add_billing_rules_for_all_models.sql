-- 为所有模型添加计费规则
-- 国外模型按 1 USD ≈ 7.25 CNY 换算为人民币
-- unit_amount = 1000000（每百万 Token 为计价单位）
-- 价格单位：元/百万 Token

-- ============================================
-- 1. OpenAI GPT-5.4
--    参考 GPT-5.2 定价：$1.75/M input, $14.00/M output
--    换算：输入 12.6875 元/M，输出 101.50 元/M
-- ============================================
INSERT INTO billing_rule (model_id, billing_type, input_unit_price, output_unit_price, unit_amount, currency, effective_time, description, status)
SELECT id, 'TOKEN', 12.687500, 101.500000, 1000000, 'CNY', CURRENT_TIMESTAMP,
    'GPT-5.4 按 Token 计费（参考 GPT-5.2 定价 $1.75/$14.00 per M，汇率 7.25）', 1
FROM ai_model WHERE model_code = 'gpt-5.4';

-- ============================================
-- 2. Anthropic Claude Sonnet 4.6
--    官方定价：$3.00/M input, $15.00/M output
--    换算：输入 21.75 元/M，输出 108.75 元/M
-- ============================================
INSERT INTO billing_rule (model_id, billing_type, input_unit_price, output_unit_price, unit_amount, currency, effective_time, description, status)
SELECT id, 'TOKEN', 21.750000, 108.750000, 1000000, 'CNY', CURRENT_TIMESTAMP,
    'Claude Sonnet 4.6 按 Token 计费（$3.00/$15.00 per M，汇率 7.25）', 1
FROM ai_model WHERE model_code = 'claude-sonnet-4-6-20260217';

-- ============================================
-- 3. Google Gemini 3.1 Pro
--    参考 Gemini 3 Pro 定价：$2.00/M input, $12.00/M output
--    换算：输入 14.50 元/M，输出 87.00 元/M
-- ============================================
INSERT INTO billing_rule (model_id, billing_type, input_unit_price, output_unit_price, unit_amount, currency, effective_time, description, status)
SELECT id, 'TOKEN', 14.500000, 87.000000, 1000000, 'CNY', CURRENT_TIMESTAMP,
    'Gemini 3.1 Pro 按 Token 计费（参考 Gemini 3 Pro $2.00/$12.00 per M，汇率 7.25）', 1
FROM ai_model WHERE model_code = 'gemini-3.1-pro-preview';

-- ============================================
-- 4. 阿里云 Qwen3.5 Plus
--    官方定价：0.8 元/M input, 4.8 元/M output
-- ============================================
INSERT INTO billing_rule (model_id, billing_type, input_unit_price, output_unit_price, unit_amount, currency, effective_time, description, status)
SELECT id, 'TOKEN', 0.800000, 4.800000, 1000000, 'CNY', CURRENT_TIMESTAMP,
    'Qwen3.5 Plus 按 Token 计费（阿里云百炼官方定价）', 1
FROM ai_model WHERE model_code = 'qwen3.5-plus';

-- ============================================
-- 5. 火山引擎 Doubao Seed 2.0 Pro
--    官方定价：3.2 元/M input, 16 元/M output
-- ============================================
INSERT INTO billing_rule (model_id, billing_type, input_unit_price, output_unit_price, unit_amount, currency, effective_time, description, status)
SELECT id, 'TOKEN', 3.200000, 16.000000, 1000000, 'CNY', CURRENT_TIMESTAMP,
    'Doubao Seed 2.0 Pro 按 Token 计费（火山引擎方舟官方定价）', 1
FROM ai_model WHERE model_code = 'doubao-seed-2-0-pro';

-- ============================================
-- 6. 百度 ERNIE 5.0
--    官方定价：6 元/M input, 24 元/M output
-- ============================================
INSERT INTO billing_rule (model_id, billing_type, input_unit_price, output_unit_price, unit_amount, currency, effective_time, description, status)
SELECT id, 'TOKEN', 6.000000, 24.000000, 1000000, 'CNY', CURRENT_TIMESTAMP,
    'ERNIE 5.0 按 Token 计费（百度千帆官方定价）', 1
FROM ai_model WHERE model_code = 'ernie-5.0-thinking-latest';

-- ============================================
-- 7. 腾讯混元 TurboS
--    官方定价：0.8 元/M input, 2 元/M output
-- ============================================
INSERT INTO billing_rule (model_id, billing_type, input_unit_price, output_unit_price, unit_amount, currency, effective_time, description, status)
SELECT id, 'TOKEN', 0.800000, 2.000000, 1000000, 'CNY', CURRENT_TIMESTAMP,
    'Hunyuan TurboS 按 Token 计费（腾讯云官方定价）', 1
FROM ai_model WHERE model_code = 'hunyuan-turbos-latest';

-- ============================================
-- 8. 智谱 GLM-5
--    参考定价：$0.30/M input, $1.10/M output
--    换算：输入 2.175 元/M，输出 7.975 元/M
-- ============================================
INSERT INTO billing_rule (model_id, billing_type, input_unit_price, output_unit_price, unit_amount, currency, effective_time, description, status)
SELECT id, 'TOKEN', 2.175000, 7.975000, 1000000, 'CNY', CURRENT_TIMESTAMP,
    'GLM-5 按 Token 计费（$0.30/$1.10 per M，汇率 7.25）', 1
FROM ai_model WHERE model_code = 'glm-5';

-- ============================================
-- 9. 月之暗面 Kimi K2.5
--    参考 K2 定价：$0.55/M input, $2.20/M output
--    换算：输入 3.9875 元/M，输出 15.95 元/M
-- ============================================
INSERT INTO billing_rule (model_id, billing_type, input_unit_price, output_unit_price, unit_amount, currency, effective_time, description, status)
SELECT id, 'TOKEN', 3.987500, 15.950000, 1000000, 'CNY', CURRENT_TIMESTAMP,
    'Kimi K2.5 按 Token 计费（参考 K2 $0.55/$2.20 per M，汇率 7.25）', 1
FROM ai_model WHERE model_code = 'kimi-k2.5';

-- ============================================
-- 10. 深度求索 DeepSeek V3.2
--     官方定价（V3.2）：2 元/M input, 8 元/M output
-- ============================================
INSERT INTO billing_rule (model_id, billing_type, input_unit_price, output_unit_price, unit_amount, currency, effective_time, description, status)
SELECT id, 'TOKEN', 2.000000, 8.000000, 1000000, 'CNY', CURRENT_TIMESTAMP,
    'DeepSeek V3.2 按 Token 计费（DeepSeek 官方定价）', 1
FROM ai_model WHERE model_code = 'deepseek-chat';

-- ============================================
-- 11. MiniMax M2.5
--     官方定价：2.1 元/M input, 8.4 元/M output
-- ============================================
INSERT INTO billing_rule (model_id, billing_type, input_unit_price, output_unit_price, unit_amount, currency, effective_time, description, status)
SELECT id, 'TOKEN', 2.100000, 8.400000, 1000000, 'CNY', CURRENT_TIMESTAMP,
    'MiniMax M2.5 按 Token 计费（MiniMax 官方定价）', 1
FROM ai_model WHERE model_code = 'MiniMax-M2.5';
