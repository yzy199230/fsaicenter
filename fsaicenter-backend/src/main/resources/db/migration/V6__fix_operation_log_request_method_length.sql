-- 修复 admin_operation_log.request_method 字段长度
-- 原 varchar(10) 只能存 HTTP 方法名，实际代码写入的是完整 Java 方法签名
ALTER TABLE admin_operation_log ALTER COLUMN request_method TYPE varchar(500);
