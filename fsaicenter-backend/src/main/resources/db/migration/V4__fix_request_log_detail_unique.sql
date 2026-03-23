-- 添加 request_id 唯一约束，修复 ON CONFLICT 报错
-- 先删除旧的普通索引，再创建唯一索引
DROP INDEX IF EXISTS idx_request_log_detail_request;
CREATE UNIQUE INDEX idx_request_log_detail_request ON request_log_detail(request_id);
