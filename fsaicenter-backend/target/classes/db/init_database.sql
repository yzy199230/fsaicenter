-- FSA AI Center 数据库初始化脚本（简化版）
-- 直接执行版本，不依赖 Flyway
-- PostgreSQL 14+

-- 创建数据库
CREATE DATABASE fsaicenter_dev
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'zh_CN.UTF-8'
    LC_CTYPE = 'zh_CN.UTF-8'
    TEMPLATE = template0;

\c fsaicenter_dev;

-- 启用 UUID 扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 后续执行 V1__init_schema.sql 中的所有表创建语句
