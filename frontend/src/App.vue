<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter, RouterView } from 'vue-router'
import { Calendar, DataAnalysis, House, Setting, SwitchButton, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'

import nankaiLogo from '@/assets/nankai-logo.png'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const roleLabels: Record<string, string> = {
  STUDENT: '学生',
  ORGANIZER: '组织者',
  ADMIN: '管理员',
}

const roleClass = computed(() => {
  const role = authStore.user?.role?.toLowerCase()
  return role ? `role-${role}` : ''
})

const userInitial = computed(() => authStore.user?.name?.trim().slice(0, 1) || '用')

const showShell = computed(() => authStore.isLoggedIn && !route.meta.publicOnly)
const activeMenu = computed(() => {
  if (route.path.startsWith('/manage/events')) return '/manage/events'
  if (route.path.startsWith('/events')) return '/events'
  if (route.path.startsWith('/my/registrations')) return '/my/registrations'
  if (route.path.startsWith('/statistics')) return '/statistics'
  return '/'
})

const navItems = computed(() => {
  const role = authStore.user?.role
  return [
    { path: '/', label: '系统介绍', icon: House, visible: true },
    { path: '/events', label: '活动大厅', icon: Calendar, visible: true },
    { path: '/my/registrations', label: '我的报名', icon: Tickets, visible: role === 'STUDENT' },
    { path: '/manage/events', label: '活动管理', icon: Setting, visible: role === 'ORGANIZER' || role === 'ADMIN' },
    { path: '/statistics', label: '数据统计', icon: DataAnalysis, visible: role === 'ORGANIZER' || role === 'ADMIN' },
  ].filter((item) => item.visible)
})

async function selectMenu(path: string) {
  if (path !== route.path) {
    await router.push(path)
  }
}

async function logout() {
  authStore.logout()
  ElMessage.success('已退出登录')
  await router.push('/login')
}
</script>

<template>
  <div v-if="showShell" class="site-layout">
    <a class="skip-link" href="#main-content">跳到主内容</a>
    <aside class="site-sidebar">
      <div class="sidebar-brand">
        <button class="brand-mark" type="button" @click="router.push('/')">
          <span class="brand-logo brand-logo-emblem">
            <img :src="nankaiLogo" alt="南开大学校徽" />
          </span>
          <span class="brand-copy">
            <strong>校园活动报名系统</strong>
            <small>Campus Event Registration</small>
          </span>
        </button>
      </div>

      <p class="sidebar-section-title">主菜单</p>
      <nav class="sidebar-nav" aria-label="主导航">
        <button
          v-for="item in navItems"
          :key="item.path"
          class="sidebar-link"
          :class="{ active: activeMenu === item.path }"
          type="button"
          :aria-current="activeMenu === item.path ? 'page' : undefined"
          @click="selectMenu(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div v-if="authStore.user" class="sidebar-user" :class="roleClass">
        <p class="sidebar-user-label">账户信息</p>
        <div class="sidebar-user-main">
          <span class="user-avatar" aria-hidden="true">{{ userInitial }}</span>
          <div class="site-user-text">
            <em>当前身份：{{ roleLabels[authStore.user.role] || authStore.user.role }}</em>
          </div>
        </div>
        <dl class="sidebar-user-details">
          <div>
            <dt>账号</dt>
            <dd>{{ authStore.user.username }}</dd>
          </div>
          <div>
            <dt>姓名</dt>
            <dd>{{ authStore.user.name }}</dd>
          </div>
        </dl>
        <el-button class="logout-button" :icon="SwitchButton" plain @click="logout">退出登录</el-button>
      </div>
    </aside>

    <main id="main-content" class="site-main" tabindex="-1">
      <RouterView />
    </main>
  </div>
  <RouterView v-else />
</template>
