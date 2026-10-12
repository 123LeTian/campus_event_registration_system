<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus/es/components/form/index'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Lock, User, UserFilled } from '@element-plus/icons-vue'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import nankaiLogo from '@/assets/nankai-logo.png'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  name: '',
  password: '',
  confirmPassword: '',

})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }

}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 50, message: '账号长度必须在 3 到 50 个字符之间', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],

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
      await authStore.register({ username: form.username, password: form.password, name: form.name })
      ElMessage.success('注册成功，请登录')
      await router.push('/login')
    } finally {
      loading.value = false
    }
  })

}

function goLogin() {
  void router.push('/login')

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
    <section class="auth-panel auth-panel-wide" aria-label="用户注册">
      <aside class="auth-side" aria-label="注册说明">
        <div class="auth-side-brand">
          <img :src="nankaiLogo" alt="南开大学校徽" class="auth-brand-logo" />
          <div>
            <h1>校园活动报名系统</h1>
          </div>
        </div>

        <div class="auth-side-copy">
          <h2>创建学生报名入口</h2>
          <p>注册后可进入活动大厅，查看活动详情、提交报名，并在我的报名中跟踪状态。</p>
        </div>

        <div class="auth-feature-list" aria-label="学生账号能力">
          <div class="auth-feature-item">
            <strong>默认身份</strong>
            <span>普通注册固定创建学生账号。</span>
          </div>
          <div class="auth-feature-item">
            <strong>可用功能</strong>
            <span>浏览活动、提交报名、查看我的报名。</span>
          </div>
          <div class="auth-feature-item">
            <strong>权限边界</strong>
            <span>活动创建、名单和统计由组织者或管理员处理。</span>
          </div>
        </div>
      </aside>

      <el-form ref="formRef" :model="form" :rules="rules" class="auth-form" label-position="top" @keyup.enter="submit">
        <div class="auth-form-heading">
          <p>用户注册</p>
          <h2>创建学生账号</h2>
        </div>
        <div class="auth-note auth-note-combined" role="note">
          <strong>账号类型：学生账号</strong>
          <span>普通注册默认创建学生身份，用于浏览活动、提交报名和查看报名记录；组织者和管理员账号由系统初始化或管理员维护，本页不提供角色选择。</span>
        </div>
        <el-form-item label="账号" prop="username">
          <el-input v-model.trim="form.username" :prefix-icon="User" autocomplete="username" placeholder="newStudent" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model.trim="form.name" :prefix-icon="UserFilled" autocomplete="name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" :prefix-icon="Lock" show-password type="password" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" :prefix-icon="Lock" show-password type="password" autocomplete="new-password" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="auth-submit" @click="submit">注册</el-button>
        <div class="auth-switch">
          <span>已有账号？</span>
          <el-button text class="auth-link" @click="goLogin">去登录</el-button>
        </div>
      </el-form>
    </section>
  </main>
</template>
