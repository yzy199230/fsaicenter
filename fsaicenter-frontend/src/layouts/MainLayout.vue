<template>
  <a-layout class="main-layout">
    <a-layout-sider
      :width="200"
      :collapsed="collapsed"
      collapsible
      @collapse="onCollapse"
    >
      <div class="logo">
        <span v-if="!collapsed">FSAICenter</span>
        <span v-else>FSA</span>
      </div>
      <a-menu
        :default-selected-keys="[currentRoute]"
        :style="{ width: '100%' }"
        @menu-item-click="onMenuClick"
      >
        <a-menu-item key="dashboard">
          <template #icon><icon-dashboard /></template>
          仪表盘
        </a-menu-item>

        <a-sub-menu key="system">
          <template #icon><icon-settings /></template>
          <template #title>系统管理</template>
          <a-menu-item key="system-users">用户管理</a-menu-item>
          <a-menu-item key="system-roles">角色管理</a-menu-item>
        </a-sub-menu>

        <a-menu-item key="apikey-list">
          <template #icon><icon-lock /></template>
          API Key 管理
        </a-menu-item>

        <a-sub-menu key="model">
          <template #icon><icon-robot /></template>
          <template #title>模型管理</template>
          <a-menu-item key="model-list">模型列表</a-menu-item>
          <a-menu-item key="model-providers">提供商管理</a-menu-item>
        </a-sub-menu>

        <a-menu-item key="billing-records">
          <template #icon><icon-file /></template>
          计费记录
        </a-menu-item>

        <a-menu-item key="logs-requests">
          <template #icon><icon-history /></template>
          请求日志
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="header">
        <div class="header-content">
          <a-breadcrumb>
            <a-breadcrumb-item>{{ currentTitle }}</a-breadcrumb-item>
          </a-breadcrumb>
          <div class="header-right">
            <a-dropdown>
              <a-space>
                <a-avatar :size="32">
                  <icon-user />
                </a-avatar>
                <span>{{ userStore.userInfo?.realName || userStore.userInfo?.username || '管理员' }}</span>
              </a-space>
              <template #content>
                <a-doption @click="handleLogout">
                  <icon-export />
                  退出登录
                </a-doption>
              </template>
            </a-dropdown>
          </div>
        </div>
      </a-layout-header>

      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import {
  IconDashboard,
  IconSettings,
  IconLock,
  IconRobot,
  IconFile,
  IconHistory,
  IconUser,
  IconExport
} from '@arco-design/web-vue/es/icon'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)

const currentRoute = computed(() => route.name as string)
const currentTitle = computed(() => route.meta.title as string || '仪表盘')

const onCollapse = (val: boolean) => {
  collapsed.value = val
}

const onMenuClick = (key: string) => {
  router.push({ name: key })
}

const handleLogout = async () => {
  try {
    await authApi.logout()
  } catch (e) {
    // ignore
  }
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.header {
  background: #fff;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.content {
  padding: 20px;
  background: #f5f5f5;
  overflow: auto;
}
</style>

