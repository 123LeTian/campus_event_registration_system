<script setup lang="ts">
import { ArrowRight, Calendar, DataAnalysis, Finished, Setting, Tickets } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const roleLabel = computed(() => {
  const labels: Record<string, string> = { STUDENT: '学生', ORGANIZER: '组织者', ADMIN: '管理员' }
  return authStore.user ? labels[authStore.user.role] || authStore.user.role : '-'
})

const identitySummary = computed(() => {
  if (authStore.user?.role === 'STUDENT') return '学生账号用于参与校园活动，主要围绕活动浏览、活动报名和个人报名记录管理展开。'
  if (authStore.user?.role === 'ORGANIZER') return '组织者账号用于活动运营，主要负责创建和维护本人发布的活动，并查看对应活动的报名名单与统计信息。'
  if (authStore.user?.role === 'ADMIN') return '管理员账号用于系统管理，拥有全局视角，可维护全部活动数据并查看系统整体统计情况。'
  return ''
})

const userTitle = computed(() => {
  if (authStore.user?.role === 'STUDENT') return '学生用户'
  if (authStore.user?.role === 'ORGANIZER') return '活动组织者'
  if (authStore.user?.role === 'ADMIN') return '系统管理员'
  return '当前用户'
})

const roleGuide = computed(() => {
  const role = authStore.user?.role

  if (role === 'STUDENT') {
    return {
      title: '学生使用说明',
      intro: '学生账号主要围绕活动浏览、活动报名和个人报名记录管理展开。',
      features: [
        { title: '活动大厅', icon: Calendar, tone: 'green', path: '/events', description: '查看全部可访问活动，支持按关键词和活动状态筛选。' },
        { title: '我的报名', icon: Tickets, tone: 'blue', path: '/my/registrations', description: '查看自己的报名记录、报名状态和活动信息，符合条件时可取消报名。' },
      ],
      steps: ['在活动大厅浏览活动，按关键词或状态筛选感兴趣的内容', '进入活动详情页，确认时间、地点、名额和报名状态后提交报名', '在我的报名中查看报名记录，活动未开始前可按规则取消报名'],
      notes: ['学生不能创建活动、查看报名名单或访问统计页', '活动已满员、已截止或已结束时，系统会禁止继续报名'],
    }
  }

  if (role === 'ORGANIZER') {
    return {
      title: '组织者使用说明',
      intro: '组织者账号负责发布和维护自己创建的活动，并查看对应活动的报名情况。',
      features: [
        { title: '活动大厅', icon: Calendar, tone: 'green', path: '/events', description: '查看系统内面向用户展示的活动信息和活动状态。' },
        { title: '活动管理', icon: Setting, tone: 'violet', path: '/manage/events', description: '创建、编辑、删除本人发布的活动，并进入报名名单页面。' },
        { title: '数据统计', icon: DataAnalysis, tone: 'teal', path: '/statistics', description: '查看本人活动的报名总量、状态分布和报名率数据。' },
      ],
      steps: ['在活动管理中新建活动，填写标题、地点、活动文案、活动介绍、时间和容量', '活动发布后可继续维护活动信息，并在管理列表查看活动状态', '通过报名名单和数据统计核对报名人数、剩余名额和活动分布'],
      notes: ['组织者只能管理自己创建的活动，不能维护其他组织者的数据', '已有报名后，部分关键时间和容量规则会受到业务约束'],
    }
  }

  if (role === 'ADMIN') {
    return {
      title: '管理员使用说明',
      intro: '管理员账号拥有全局视角，用于维护活动数据和查看系统整体统计情况。',
      features: [
        { title: '活动大厅', icon: Calendar, tone: 'green', path: '/events', description: '查看普通用户可见的活动展示内容和活动状态。' },
        { title: '活动管理', icon: Setting, tone: 'violet', path: '/manage/events', description: '查看和维护全部活动，处理活动状态与基础信息。' },
        { title: '数据统计', icon: DataAnalysis, tone: 'teal', path: '/statistics', description: '查看全局活动数量、报名人数、状态分布和报名率数据。' },
      ],
      steps: ['在活动大厅查看活动对外展示效果', '在活动管理中查看全部活动，并维护需要调整的活动信息', '进入数据统计页核对全局报名人数、活动状态分布和活动报名率'],
      notes: ['管理员拥有全局管理视角，但操作仍受后端权限校验保护', '管理员可用于数据检查和维护，不影响学生与组织者的独立业务流程'],
    }
  }

  return {
    title: '系统使用说明',
    intro: '登录后系统会根据当前身份展示可访问的页面和操作范围。',
    features: [
      { title: '系统说明', icon: Finished, tone: 'green', path: '/', description: '查看系统定位、当前身份、功能范围和使用方式。' },
    ],
    steps: ['确认当前登录身份', '通过侧边导航进入对应业务页面', '按身份完成报名、管理或统计操作'],
    notes: ['页面入口会按角色显示', '最终权限以后端校验结果为准'],
  }
})

function openFeature(path: string) {
  void router.push(path)
}

</script>


<template>
  <main class="app-shell dashboard-shell">
    <section class="dashboard-welcome">
      <div>
        <h1>校园活动报名系统介绍</h1>
        <p class="system-intro-text">本系统面向校园讲座、社团活动、志愿活动等校园活动，覆盖活动发布、学生报名、名单查看、数据统计和 AI 辅助创作等能力。学生可浏览活动并管理个人报名记录，组织者可维护本人发布的活动、查看报名名单并使用 AI 生成活动文案与活动介绍，管理员可从全局视角维护活动数据并查看整体统计分析，帮助校园活动完成统一、规范的线上报名管理。</p>
      </div>
    </section>

    <section class="dashboard-content-panel" aria-label="系统介绍内容">
      <section class="operation-section">
        <div class="module-title">
          <span></span>
          <h2>当前身份与说明</h2>
        </div>
        <div class="identity-grid identity-grid-single">
          <article v-if="authStore.user" class="operation-card current-user-card identity-card">
            <h3>{{ userTitle }}</h3>
            <p class="identity-meta">
              <span>角色：{{ roleLabel }}</span>
              <span>账号：{{ authStore.user.username }}</span>
              <span>姓名：{{ authStore.user.name }}</span>
            </p>
            <p>{{ identitySummary }}</p>
          </article>
        </div>
      </section>

      <section class="module-section">
        <div class="module-title">
          <span></span>
          <h2>功能介绍</h2>
        </div>
        <div class="module-grid role-feature-grid">
          <button
            v-for="feature in roleGuide.features"
            :key="feature.title"
            class="module-card module-action-card"
            type="button"
            :aria-label="`进入${feature.title}`"
            @click="openFeature(feature.path)"
          >
            <span class="module-icon" :class="feature.tone"><el-icon><component :is="feature.icon" /></el-icon></span>
            <div>
              <h3>{{ feature.title }}</h3>
              <p>{{ feature.description }}</p>
            </div>
            <el-icon class="module-arrow"><ArrowRight /></el-icon>
          </button>
        </div>
      </section>

      <section class="operation-section">
        <div class="module-title">
          <span></span>
          <h2>使用说明</h2>
        </div>
        <div class="operation-grid">
          <article class="operation-card">
            <h3>操作流程</h3>
            <ol class="step-list">
              <li v-for="(step, index) in roleGuide.steps" :key="step">
                <span>{{ index + 1 }}</span>
                <p>{{ step }}</p>
              </li>
            </ol>
          </article>
          <article class="operation-card note-card">
            <h3>权限与注意事项</h3>
            <ul class="dashboard-list">
              <li v-for="note in roleGuide.notes" :key="note">{{ note }}</li>
            </ul>
          </article>
        </div>
      </section>
    </section>

  </main>
</template>
