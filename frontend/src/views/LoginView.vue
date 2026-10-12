<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus/es/components/form/index'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Lock, User } from '@element-plus/icons-vue'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import nankaiLogo from '@/assets/nankai-logo.png'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',

})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],

}

async function submit() {
  if (!formRef.value) {
    return
  }
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    loading.value = true
    try {
      await authStore.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
      await router.push(redirect)
    } finally {
      loading.value = false
    }
  })

}

function goRegister() {
  void router.push('/register')

}

function updateAuthPointer(event: PointerEvent) {
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const x = ((event.clientX - rect.left) / rect.width) * 100
  const y = ((event.clientY - rect.top) / rect.height) * 100
  target.style.setProperty('--auth-pointer-x', `${x}%`)
  target.style.setProperty('--auth-pointer-y', `${y}%`)

}

function resetAuthPointer(event: PointerEvent) {
  const target = event.currentTarget as HTMLElement
  target.style.setProperty('--auth-pointer-x', '50%')
  target.style.setProperty('--auth-pointer-y', '50%')

}
</script>

<template>
  <main class="auth-page" @pointermove="updateAuthPointer" @pointerleave="resetAuthPointer">
    <section class="auth-panel auth-panel-wide" aria-label="账号登录">
      <aside class="auth-side" aria-label="系统说明">
        <div class="auth-side-brand">
          <img :src="nankaiLogo" alt="南开大学校徽" class="auth-brand-logo" />
          <div>
            <h1>校园活动报名系统</h1>
          </div>
        </div>

        <div class="auth-side-copy">
          <h2>让校园活动报名更清晰</h2>
          <p>面向学生、组织者和管理员，统一完成活动浏览、报名记录、名单管理和数据统计。</p>
        </div>

        <div class="auth-feature-list" aria-label="系统能力">
          <div class="auth-feature-item">
            <strong>学生报名</strong>
            <span>查看活动详情，完成报名或取消报名。</span>
          </div>
          <div class="auth-feature-item">
            <strong>活动管理</strong>
            <span>组织者维护活动信息并查看报名名单。</span>
          </div>
          <div class="auth-feature-item">
            <strong>数据统计</strong>
            <span>通过概览图表快速核对活动报名情况。</span>
          </div>
        </div>
      </aside>

      <el-form ref="formRef" :model="form" :rules="rules" class="auth-form" label-position="top" @keyup.enter="submit">
        <div class="auth-form-heading">
          <p>账号登录</p>
          <h2>欢迎回来</h2>
        </div>
        <el-form-item label="账号" prop="username">
          <el-input v-model.trim="form.username" :prefix-icon="User" autocomplete="username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            autocomplete="current-password"
            placeholder="请输入密码"
            show-password
            type="password"
          />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="auth-submit" @click="submit">登录</el-button>
        <div class="auth-switch">
          <span>还没有学生账号？</span>
          <el-button text class="auth-link" @click="goRegister">去注册</el-button>
        </div>
      </el-form>
    </section>
  </main>
</template>
