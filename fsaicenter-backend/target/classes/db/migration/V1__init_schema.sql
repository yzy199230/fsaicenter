-- FSA AI Center 数据库初始化脚本
-- PostgreSQL 14+
-- 创建时间: 2024-01-01

-- ============================================
-- 1. 管理员用户表
-- ============================================
CREATE TABLE IF NOT EXISTS admin_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密',
    real_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(500),
    status INTEGER DEFAULT 1 COMMENT '1:启用 0:禁用',
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(50),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_admin_user_username ON admin_user(username) WHERE is_deleted = 0;
CREATE INDEX idx_admin_user_status ON admin_user(status) WHERE is_deleted = 0;

COMMENT ON TABLE admin_user IS '管理员用户表';
COMMENT ON COLUMN admin_user.username IS '用户名';
COMMENT ON COLUMN admin_user.password IS '密码(BCrypt加密)';

-- ============================================
-- 2. 角色表
-- ============================================
CREATE TABLE IF NOT EXISTS admin_role (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_admin_role_code ON admin_role(role_code) WHERE is_deleted = 0;

COMMENT ON TABLE admin_role IS '角色表';

-- ============================================
-- 3. 权限表
-- ============================================
CREATE TABLE IF NOT EXISTS admin_permission (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    permission_type VARCHAR(20) COMMENT 'MENU/BUTTON/API',
    permission_path VARCHAR(200),
    icon VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_admin_permission_parent ON admin_permission(parent_id) WHERE is_deleted = 0;
CREATE INDEX idx_admin_permission_code ON admin_permission(permission_code) WHERE is_deleted = 0;

COMMENT ON TABLE admin_permission IS '权限表';

-- ============================================
-- 4. 用户角色关联表
-- ============================================
CREATE TABLE IF NOT EXISTS admin_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0,
    UNIQUE(user_id, role_id)
);

CREATE INDEX idx_user_role_user ON admin_user_role(user_id) WHERE is_deleted = 0;
CREATE INDEX idx_user_role_role ON admin_user_role(role_id) WHERE is_deleted = 0;

COMMENT ON TABLE admin_user_role IS '用户角色关联表';

-- ============================================
-- 5. 角色权限关联表
-- ============================================
CREATE TABLE IF NOT EXISTS admin_role_permission (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0,
    UNIQUE(role_id, permission_id)
);

CREATE INDEX idx_role_permission_role ON admin_role_permission(role_id) WHERE is_deleted = 0;
CREATE INDEX idx_role_permission_perm ON admin_role_permission(permission_id) WHERE is_deleted = 0;

COMMENT ON TABLE admin_role_permission IS '角色权限关联表';

-- ============================================
-- 6. 操作日志表
-- ============================================
CREATE TABLE IF NOT EXISTS admin_operation_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    operation_type VARCHAR(50),
    operation_desc VARCHAR(500),
    request_method VARCHAR(10),
    request_url VARCHAR(500),
    request_params TEXT,
    request_ip VARCHAR(50),
    user_agent VARCHAR(500),
    response_status INTEGER,
    response_time_ms INTEGER,
    error_message TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_operation_log_user ON admin_operation_log(user_id) WHERE is_deleted = 0;
CREATE INDEX idx_operation_log_time ON admin_operation_log(created_time DESC);

COMMENT ON TABLE admin_operation_log IS '操作日志表';

-- ============================================
-- 7. AI提供商表
-- ============================================
CREATE TABLE IF NOT EXISTS ai_provider (
    id BIGSERIAL PRIMARY KEY,
    provider_code VARCHAR(50) NOT NULL UNIQUE,
    provider_name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(50) COMMENT 'OPENAI/QWEN/WENXIN/SPARK/DOUBAO/OLLAMA/VLLM/GENERIC',
    base_url VARCHAR(500),
    protocol_type VARCHAR(20) COMMENT 'OPENAI_COMPATIBLE/CUSTOM',
    chat_endpoint VARCHAR(200),
    embedding_endpoint VARCHAR(200),
    image_endpoint VARCHAR(200),
    video_endpoint VARCHAR(200),
    extra_headers TEXT COMMENT 'JSON格式额外请求头',
    request_template TEXT COMMENT 'JSON格式请求模板',
    response_mapping TEXT COMMENT 'JSON格式响应映射',
    auth_type VARCHAR(20) COMMENT 'BEARER/API_KEY/CUSTOM',
    auth_header VARCHAR(50),
    auth_prefix VARCHAR(20),
    api_key_required BOOLEAN DEFAULT true,
    description VARCHAR(500),
    status INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_provider_code ON ai_provider(provider_code) WHERE is_deleted = 0;
CREATE INDEX idx_provider_status ON ai_provider(status) WHERE is_deleted = 0;

COMMENT ON TABLE ai_provider IS 'AI提供商表';

-- ============================================
-- 8. AI模型表
-- ============================================
CREATE TABLE IF NOT EXISTS ai_model (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    model_code VARCHAR(100) NOT NULL,
    model_name VARCHAR(200) NOT NULL,
    model_type VARCHAR(50) COMMENT 'CHAT/EMBEDDING/IMAGE/VIDEO/TTS/ASR',
    support_stream BOOLEAN DEFAULT false,
    max_tokens INTEGER,
    config JSONB COMMENT '模型配置',
    description VARCHAR(500),
    capabilities JSONB COMMENT '模型能力配置',
    status INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0,
    UNIQUE(provider_id, model_code)
);

CREATE INDEX idx_model_provider ON ai_model(provider_id) WHERE is_deleted = 0;
CREATE INDEX idx_model_type ON ai_model(model_type) WHERE is_deleted = 0;
CREATE INDEX idx_model_status ON ai_model(status) WHERE is_deleted = 0;
CREATE INDEX idx_model_code ON ai_model(model_code) WHERE is_deleted = 0;

COMMENT ON TABLE ai_model IS 'AI模型表';

-- ============================================
-- 9. 模型API Key表（上游提供商的Key）
-- ============================================
CREATE TABLE IF NOT EXISTS model_api_key (
    id BIGSERIAL PRIMARY KEY,
    model_id BIGINT NOT NULL,
    key_name VARCHAR(100),
    api_key VARCHAR(500) NOT NULL COMMENT '上游提供商API Key',
    weight INTEGER DEFAULT 1 COMMENT '权重，用于负载均衡',
    total_requests BIGINT DEFAULT 0,
    success_requests BIGINT DEFAULT 0,
    failed_requests BIGINT DEFAULT 0,
    last_used_time TIMESTAMP,
    last_success_time TIMESTAMP,
    last_fail_time TIMESTAMP,
    health_status INTEGER DEFAULT 1 COMMENT '1:健康 0:不健康',
    fail_count INTEGER DEFAULT 0,
    rate_limit_per_minute INTEGER,
    rate_limit_per_day INTEGER,
    quota_total BIGINT COMMENT '-1表示无限制',
    quota_used BIGINT DEFAULT 0,
    expire_time TIMESTAMP,
    status INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    description VARCHAR(500),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_model_api_key_model ON model_api_key(model_id) WHERE is_deleted = 0;
CREATE INDEX idx_model_api_key_status ON model_api_key(status) WHERE is_deleted = 0;
CREATE INDEX idx_model_api_key_health ON model_api_key(health_status) WHERE is_deleted = 0;

COMMENT ON TABLE model_api_key IS '模型API Key表（上游提供商）';

-- ============================================
-- 10. API密钥表（对外提供的Key）
-- ============================================
CREATE TABLE IF NOT EXISTS api_key (
    id BIGSERIAL PRIMARY KEY,
    key_value VARCHAR(100) NOT NULL UNIQUE COMMENT 'API Key值（hash存储）',
    key_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    quota_total BIGINT DEFAULT -1 COMMENT '总配额，-1表示无限制',
    quota_used BIGINT DEFAULT 0,
    rate_limit_per_minute INTEGER DEFAULT 60,
    rate_limit_per_day INTEGER DEFAULT 10000,
    allowed_model_types VARCHAR(200) COMMENT '允许的模型类型，逗号分隔',
    allowed_ip_whitelist TEXT COMMENT 'IP白名单，逗号分隔',
    expire_time TIMESTAMP,
    status INTEGER DEFAULT 1,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX idx_api_key_value ON api_key(key_value) WHERE is_deleted = 0;
CREATE INDEX idx_api_key_status ON api_key(status) WHERE is_deleted = 0;

COMMENT ON TABLE api_key IS 'API密钥表（对外提供）';

-- ============================================
-- 11. API密钥模型访问权限表
-- ============================================
CREATE TABLE IF NOT EXISTS api_key_model_access (
    id BIGSERIAL PRIMARY KEY,
    api_key_id BIGINT NOT NULL,
    model_id BIGINT NOT NULL,
    access_type INTEGER DEFAULT 1 COMMENT '1:允许 0:禁止',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0,
    UNIQUE(api_key_id, model_id)
);

CREATE INDEX idx_api_key_model_key ON api_key_model_access(api_key_id) WHERE is_deleted = 0;
CREATE INDEX idx_api_key_model_model ON api_key_model_access(model_id) WHERE is_deleted = 0;

COMMENT ON TABLE api_key_model_access IS 'API密钥模型访问权限表';

-- ============================================
-- 12. 计费规则表
-- ============================================
CREATE TABLE IF NOT EXISTS billing_rule (
    id BIGSERIAL PRIMARY KEY,
    model_id BIGINT NOT NULL,
    billing_type VARCHAR(50) COMMENT 'TOKEN/IMAGE/AUDIO_DURATION',
    unit_price DECIMAL(10,6) COMMENT '单位价格（元）',
    input_unit_price DECIMAL(10,6) COMMENT '输入单价（元），仅TOKEN类型',
    output_unit_price DECIMAL(10,6) COMMENT '输出单价（元），仅TOKEN类型',
    unit_amount INTEGER DEFAULT 1000 COMMENT '计费单位数量',
    currency VARCHAR(10) DEFAULT 'CNY',
    effective_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP,
    description VARCHAR(500),
    status INTEGER DEFAULT 1,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_billing_rule_model ON billing_rule(model_id) WHERE is_deleted = 0;
CREATE INDEX idx_billing_rule_status ON billing_rule(status) WHERE is_deleted = 0;

COMMENT ON TABLE billing_rule IS '计费规则表';

-- ============================================
-- 13. 计费记录表
-- ============================================
CREATE TABLE IF NOT EXISTS billing_record (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(100) NOT NULL,
    api_key_id BIGINT NOT NULL,
    model_id BIGINT NOT NULL,
    billing_type VARCHAR(50),
    usage_amount BIGINT COMMENT '使用量：Token数/图片数/音频秒数',
    unit_price DECIMAL(10,6),
    total_cost DECIMAL(12,4),
    currency VARCHAR(10) DEFAULT 'CNY',
    billing_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_billing_record_request ON billing_record(request_id);
CREATE INDEX idx_billing_record_api_key ON billing_record(api_key_id) WHERE is_deleted = 0;
CREATE INDEX idx_billing_record_model ON billing_record(model_id) WHERE is_deleted = 0;
CREATE INDEX idx_billing_record_time ON billing_record(billing_time DESC);

COMMENT ON TABLE billing_record IS '计费记录表';

-- ============================================
-- 14. 请求日志表（分区表）
-- ============================================
CREATE TABLE IF NOT EXISTS request_log (
    id BIGSERIAL,
    request_id VARCHAR(100) NOT NULL,
    api_key_id BIGINT NOT NULL,
    model_id BIGINT NOT NULL,
    request_type VARCHAR(50) COMMENT 'CHAT/EMBEDDING/IMAGE/AUDIO',
    is_stream BOOLEAN DEFAULT false,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    request_ip VARCHAR(50),
    user_agent VARCHAR(500),
    http_status INTEGER,
    response_time_ms INTEGER,
    error_message TEXT,
    status INTEGER COMMENT '1:成功 0:失败',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0,
    PRIMARY KEY (id, created_time)
) PARTITION BY RANGE (created_time);

-- 创建分区（按月）
CREATE TABLE request_log_2024_01 PARTITION OF request_log
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE request_log_2024_02 PARTITION OF request_log
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE request_log_2024_03 PARTITION OF request_log
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

CREATE INDEX idx_request_log_request_id ON request_log(request_id);
CREATE INDEX idx_request_log_api_key ON request_log(api_key_id) WHERE is_deleted = 0;
CREATE INDEX idx_request_log_model ON request_log(model_id) WHERE is_deleted = 0;
CREATE INDEX idx_request_log_time ON request_log(created_time DESC);

COMMENT ON TABLE request_log IS '请求日志表（分区表）';

-- ============================================
-- 15. 请求日志详情表
-- ============================================
CREATE TABLE IF NOT EXISTS request_log_detail (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(100) NOT NULL,
    request_body TEXT,
    response_body TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_request_log_detail_request ON request_log_detail(request_id);

COMMENT ON TABLE request_log_detail IS '请求日志详情表';

-- ============================================
-- 16. 模型模板表
-- ============================================
CREATE TABLE IF NOT EXISTS model_template (
    id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(100) NOT NULL UNIQUE,
    template_name VARCHAR(200) NOT NULL,
    provider_type VARCHAR(50),
    model_type VARCHAR(50),
    config_template JSONB,
    description VARCHAR(500),
    status INTEGER DEFAULT 1,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_model_template_code ON model_template(template_code) WHERE is_deleted = 0;

COMMENT ON TABLE model_template IS '模型模板表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入默认管理员（密码: admin123，BCrypt加密）
INSERT INTO admin_user (username, password, real_name, email, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin@fsa-ai.com', 1)
ON CONFLICT (username) DO NOTHING;

-- 插入默认角色
INSERT INTO admin_role (role_code, role_name, description, status)
VALUES
    ('SUPER_ADMIN', '超级管理员', '拥有所有权限', 1),
    ('ADMIN', '管理员', '拥有大部分管理权限', 1),
    ('OPERATOR', '运营人员', '拥有运营相关权限', 1)
ON CONFLICT (role_code) DO NOTHING;

-- 关联管理员和角色
INSERT INTO admin_user_role (user_id, role_id)
SELECT u.id, r.id FROM admin_user u, admin_role r
WHERE u.username = 'admin' AND r.role_code = 'SUPER_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
