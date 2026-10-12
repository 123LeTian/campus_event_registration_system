<script setup lang="ts">
import { Calendar, Location, Refresh, Search, Setting, User } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getEvents } from '@/api/event'
import { useListQuery } from '@/composables/useListQuery'
import { usePageNavigation } from '@/composables/usePageNavigation'
import { useAuthStore } from '@/stores/auth'
import type { EventItem, EventStatusFilter } from '@/types/event'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const hasLoaded = ref(false)
const error = ref(false)
const events = ref<EventItem[]>([])
const total = ref(0)
const { openPage } = usePageNavigation()
const { query, syncQuery } = useListQuery({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: '' as EventStatusFilter | '',
}, ['', 'REGISTRATION_OPEN', 'FULL', 'CLOSED', 'FINISHED'])

const statusMeta: Record<string, { label: string; type: 'success' | 'info' | 'warning' | 'danger' }> = {
  REGISTRATION_OPEN: { label: '报名开放', type: 'success' },
  CLOSED: { label: '报名截止', type: 'warning' },
  FULL: { label: '名额已满', type: 'danger' },
  FINISHED: { label: '已结束', type: 'info' },
  DELETED: { label: '已删除', type: 'info' },
}

const emptyText = computed(() => (query.keyword || query.status ? '没有找到符合条件的活动' : '暂无活动'))
const initialLoading = computed(() => loading.value && !hasLoaded.value)

function formatDateTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function formatDateTimeRange(startTime: string, endTime: string) {
  return `${formatDateTime(startTime)} 至 ${formatDateTime(endTime)}`
}

function normalizedRate(event: EventItem) {
  return Math.min(100, Math.max(0, Number(event.registrationRate) || 0))
}

async function loadEvents() {
  loading.value = true
  error.value = false
  try {
    await syncQuery()
    const page = await getEvents({
      page: query.page,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
    })
    events.value = page.records
    total.value = page.total
  } catch {
    error.value = true
  } finally {
    hasLoaded.value = true
    loading.value = false
  }
}

function searchEvents() {
  query.page = 1
  void loadEvents()
}

function resetSearch() {
  query.keyword = ''
  query.status = ''
  query.page = 1
  void loadEvents()
}

function changeFilter() {
  query.page = 1
  void loadEvents()
}

function openDetail(id: number) {
  void openPage(`/events/${id}`)
}

onMounted(loadEvents)
</script>

<template>
  <main class="app-shell">
    <section class="page-heading">
      <div>
        <h1>活动大厅</h1>
        <p class="section-note section-note-lines">
          <span>浏览校园活动信息，按标题、地点、文案、介绍或活动状态筛选可参与的活动；</span>
          <span>卡片中可快速查看时间地点、组织者、剩余名额和报名进度。</span>
        </p>
      </div>
      <div class="action-row">
        <el-button v-if="authStore.user?.role === 'ORGANIZER' || authStore.user?.role === 'ADMIN'" :icon="Setting" @click="router.push('/manage/events')">活动管理</el-button>
        <el-tooltip content="刷新"><el-button :icon="Refresh" circle aria-label="刷新" :loading="loading" @click="loadEvents" /></el-tooltip>
      </div>
    </section>

    <section class="toolbar-card">
      <div class="toolbar-line">
        <el-input v-model.trim="query.keyword" :prefix-icon="Search" clearable placeholder="搜索标题、地点、文案或介绍" @keyup.enter="searchEvents" />
        <el-button type="primary" @click="searchEvents">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
      <div class="toolbar-line">
        <el-segmented
          v-model="query.status"
          :options="[
            { label: '全部', value: '' },
            { label: '报名中', value: 'REGISTRATION_OPEN' },
            { label: '已满员', value: 'FULL' },
            { label: '报名截止', value: 'CLOSED' },
            { label: '已结束', value: 'FINISHED' },
          ]"
          @change="changeFilter"
        />
      </div>
    </section>

    <el-alert v-if="error" class="state-alert" title="活动加载失败，请重试" type="error" :closable="false" show-icon />
    <section v-if="initialLoading" class="event-grid">
      <article v-for="index in 6" :key="index" class="event-card">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="button" style="width: 86px" />
            <el-skeleton-item variant="h3" style="width: 78%; margin-top: 16px" />
            <el-skeleton-item variant="text" />
            <el-skeleton-item variant="text" style="width: 88%" />
            <el-skeleton-item variant="text" style="width: 60%" />
          </template>
        </el-skeleton>
      </article>
    </section>
    <div v-else class="event-result-shell" v-loading="loading">
      <el-empty v-if="!error && events.length === 0" :description="emptyText" />
      <section v-else-if="!error" class="event-grid">
        <article v-for="event in events" :key="event.id" class="event-card">
          <div class="event-card-main">
            <div class="event-card-head">
              <el-tag :type="statusMeta[event.displayStatus]?.type || 'info'">{{ statusMeta[event.displayStatus]?.label || event.displayStatus }}</el-tag>
              <el-tag v-if="event.displayStatus === 'REGISTRATION_OPEN'" type="success" effect="plain">剩余 {{ event.remainingParticipants }}</el-tag>
            </div>
            <h2>{{ event.title }}</h2>
            <p class="event-location-line"><el-icon><Location /></el-icon>{{ event.location }}</p>
            <p class="event-copywriting">{{ event.copywriting }}</p>
            <p class="event-description">{{ event.description }}</p>
          </div>
          <div class="event-meta">
            <span><el-icon><Calendar /></el-icon>{{ formatDateTimeRange(event.startTime, event.endTime) }}</span>
            <span><el-icon><User /></el-icon>{{ event.organizerName }}</span>
          </div>
          <div class="capacity-line">
            <div class="capacity-text">
              <span>{{ event.currentParticipants }} / {{ event.maxParticipants }} 人</span>
              <span>报名率 {{ normalizedRate(event).toFixed(0) }}%</span>
            </div>
            <el-progress :percentage="normalizedRate(event)" :stroke-width="8" :show-text="false" />
          </div>
          <el-button type="primary" plain @click="openDetail(event.id)">查看详情</el-button>
        </article>
      </section>
    </div>

    <div v-if="total > query.pageSize" class="pagination-line">
      <el-pagination v-model:current-page="query.page" :page-size="query.pageSize" :total="total" layout="prev, pager, next" @current-change="loadEvents" />
    </div>
  </main>
</template>
