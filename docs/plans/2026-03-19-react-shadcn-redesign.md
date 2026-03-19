# FSAICenter 前端全量重写 - React + shadcn/ui

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 Vue 3 + ArcoDesign 管理后台完全重写为 React + shadcn/ui 深色精致风格

**Architecture:** SPA 前端使用 React 18 + TypeScript + Vite 构建，shadcn/ui + TailwindCSS 提供 UI 组件和样式系统，Zustand 管理全局状态，React Router 处理路由，Recharts 实现图表，Axios 保持 API 层不变。

**Tech Stack:** React 18, TypeScript, Vite, TailwindCSS, shadcn/ui, Zustand, React Router v6, Recharts, Axios, Lucide Icons, Framer Motion

---

## 设计方向

### 视觉风格：深色精致风 (Vercel/Linear 风格)
- **背景色:** `hsl(240 10% 3.9%)` - 深色带微蓝
- **卡片背景:** `hsl(240 3.7% 15.9%)` - 深灰卡片
- **边框:** `hsl(240 3.7% 15.9%)` - 微妙边框
- **主色:** 蓝色-青色渐变 `#3b82f6 → #06b6d4`
- **文本:** `hsl(0 0% 98%)` 主文本 / `hsl(240 5% 64.9%)` 次要文本
- **字体:** Geist Sans + Geist Mono
- **特效:** 毛玻璃效果、微妙发光、流畅动画

### 模型卡片设计
- 玻璃质感卡片，悬浮光效
- 模型类型彩色图标徽章
- 提供商标签
- 状态发光指示器
- 配置参数可视化条
- 流式支持徽章
- 操作按钮组

---

## 任务列表

### Task 1: 项目基础设施
清除旧 Vue 代码，初始化 React + Vite + TailwindCSS + shadcn/ui

### Task 2: 全局样式和布局
深色主题 CSS 变量，侧边栏+顶栏布局，路由配置

### Task 3: API 层和状态管理
移植 Axios 请求、类型定义，Zustand store

### Task 4: 登录页
深色渐变背景登录页

### Task 5: Dashboard
统计卡片、图表、最近日志

### Task 6: 模型管理 (卡片视图)
模型卡片网格、筛选、CRUD

### Task 7: 提供商管理
提供商列表和 CRUD

### Task 8: API Key 管理
Key 列表、配额进度、CRUD

### Task 9: 计费记录
统计卡片、趋势图表、计费规则

### Task 10: 请求日志
日志列表、筛选、详情弹窗

### Task 11: 系统管理
用户管理、角色管理、权限分配
