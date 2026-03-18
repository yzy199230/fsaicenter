# FSAICenter 前端项目

基于 Vue 3 + TypeScript + Arco Design 的 AI Token 管理系统前端。

## 技术栈

- **框架**: Vue 3.3 + TypeScript
- **构建工具**: Vite 5.0
- **UI 组件库**: Arco Design Vue 2.55
- **状态管理**: Pinia 2.1
- **路由**: Vue Router 4.2
- **HTTP 客户端**: Axios 1.6
- **日期处理**: Day.js 1.11

## 项目结构

```
fsaicenter-frontend/
├── src/
│   ├── api/              # API 接口定义
│   │   ├── auth.ts       # 认证接口
│   │   ├── apikey.ts     # API Key 接口
│   │   ├── model.ts      # 模型接口
│   │   └── billing.ts    # 计费接口
│   ├── assets/           # 静态资源
│   ├── layouts/          # 布局组件
│   │   └── MainLayout.vue
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── stores/           # Pinia 状态管理
│   │   └── user.ts
│   ├── types/            # TypeScript 类型定义
│   │   ├── user.ts
│   │   ├── apikey.ts
│   │   ├── model.ts
│   │   ├── billing.ts
│   │   └── common.ts
│   ├── utils/            # 工具函数
│   │   └── request.ts    # Axios 封装
│   ├── views/            # 页面组件
│   │   ├── login/        # 登录页
│   │   ├── dashboard/    # 仪表盘
│   │   ├── apikey/       # API Key 管理
│   │   ├── model/        # 模型管理
│   │   ├── billing/      # 计费记录
│   │   ├── logs/         # 请求日志
│   │   └── system/       # 系统管理
│   ├── App.vue
│   └── main.ts
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## 快速开始

### 1. 安装依赖

```bash
cd fsaicenter-frontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

构建产物在 `dist/` 目录。

### 4. 预览生产构建

```bash
npm run preview
```

## 功能模块

### ✅ 已实现

1. **登录认证**
   - 用户登录
   - Token 管理
   - 路由守卫

2. **仪表盘**
   - 统计数据展示
   - 图表区域（待集成 ECharts）

3. **API Key 管理**（核心功能）
   - API Key 列表
   - 创建/编辑/删除
   - 启用/禁用
   - 配额管理
   - 配额重置

4. **模型管理**
   - 模型列表
   - 基础 CRUD 操作

5. **布局组件**
   - 侧边栏菜单
   - 顶部导航
   - 面包屑
   - 用户下拉菜单

### ⏳ 待完善

1. **系统管理**
   - 用户管理（待实现）
   - 角色管理（待实现）
   - 权限管理（待实现）

2. **提供商管理**（待实现）

3. **计费记录**（待实现）

4. **请求日志**（待实现）

5. **数据可视化**
   - 集成 ECharts
   - 请求趋势图
   - 消费趋势图

## API 接口说明

前端通过 Vite 代理转发请求到后端：

```typescript
// vite.config.ts
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 接口规范

所有接口返回格式：

```typescript
{
  code: number      // 200 表示成功
  message: string   // 提示信息
  data: any         // 响应数据
}
```

### 认证方式

使用 Bearer Token 认证：

```
Authorization: Bearer <token>
```

Token 存储在 localStorage，由 Axios 拦截器自动添加到请求头。

## 开发规范

### 1. 组件命名

- 页面组件：`index.vue`
- 业务组件：`PascalCase.vue`
- 通用组件：`PascalCase.vue`

### 2. API 调用

```typescript
// 使用封装的 request 工具
import { apiKeyApi } from '@/api/apikey'

const data = await apiKeyApi.getPage({ page: 1, size: 10 })
```

### 3. 状态管理

```typescript
// 使用 Pinia
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
userStore.setToken(token)
```

### 4. 路由跳转

```typescript
import { useRouter } from 'vue-router'

const router = useRouter()
router.push({ name: 'dashboard' })
```

## 环境变量

创建 `.env.development` 和 `.env.production` 文件：

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080

# .env.production
VITE_API_BASE_URL=https://api.example.com
```

## 常见问题

### 1. 端口冲突

修改 `vite.config.ts` 中的 `server.port`。

### 2. 代理不生效

确保后端服务已启动在 `http://localhost:8080`。

### 3. 类型错误

运行 `npm run build` 检查 TypeScript 类型错误。

## 后续优化

1. **性能优化**
   - 路由懒加载（已实现）
   - 组件懒加载
   - 图片懒加载
   - 虚拟滚动（大数据列表）

2. **用户体验**
   - 加载骨架屏
   - 错误边界
   - 离线提示
   - 操作确认

3. **代码质量**
   - ESLint 规则完善
   - 单元测试（Vitest）
   - E2E 测试（Playwright）

4. **功能增强**
   - 国际化（i18n）
   - 主题切换
   - 暗黑模式
   - 导出功能

## 许可证

MIT
