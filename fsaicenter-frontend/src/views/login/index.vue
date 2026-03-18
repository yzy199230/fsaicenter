<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>FSAICenter</h1>
        <p>AI Token 统一管理系统</p>
      </div>

      <a-form
        :model="formData"
        :rules="rules"
        @submit="handleSubmit"
        layout="vertical"
      >
        <a-form-item field="username" label="用户名">
          <a-input
            v-model="formData.username"
            placeholder="请输入用户名"
            size="large"
          >
            <template #prefix>
              <icon-user />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item field="password" label="密码">
          <a-input-password
            v-model="formData.password"
            placeholder="请输入密码"
            size="large"
          >
            <template #prefix>
              <icon-lock />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            long
            size="large"
            :loading="loading"
          >
            登录
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { IconUser, IconLock } from '@arco-design/web-vue/es/icon'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import type { LoginRequest } from '@/types/user'

const router = useRouter()
const userStore = useUserStore()

const formData = reactive<LoginRequest>({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }]
}

const loading = ref(false)

const handleSubmit = async ({ errors }: any) => {
  if (errors) return

  loading.value = true
  try {
    const res = await authApi.login(formData)
    userStore.setLoginInfo(res)
    Message.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: #666;
}
</style>
