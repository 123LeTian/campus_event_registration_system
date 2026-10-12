<script setup lang="ts">
import { ArrowLeft, Delete, Edit, List, Plus, Search, View } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { deleteEvent, getEvents } from '@/api/event'
import { useListQuery } from '@/composables/useListQuery'
import { usePageNavigation } from '@/composables/usePageNavigation'
import { useAuthStore } from '@/stores/auth'
import type { EventItem, EventStatusFilter } from '@/types/event'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const error = ref(false)
const events = ref<EventItem[]>([])
const total = ref(0)
const deletingId = ref<number>()
const { openPage } = usePageNavigation()
const { query, syncQuery } = useListQuery({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: '' as EventStatusFilter | '',
}, ['', 'REGISTRATION_OPEN', 'FULL', 'CLOSED', 'FINISHED', ...(authStore.user?.role === 'ADMIN' ? ['DELETED'] : [])])
const statuses: Record<string, string> = {
  REGISTRATION_OPEN: '报名开放', CLOSED: '报名截止', FULL: '名额已满', FINISHED: '已结束', DELETED: '已删除',
}
const statusTypes: Record<string, 'success' | 'info' | 'warning' | 'danger'> = {
  REGISTRATION_OPEN: 'success',
  CLOSED: 'warning',
  FULL: 'danger',
  FINISHED: 'info',
  DELETED: 'info',
}
const formatTime = (value: string) => value.replace('T', ' ').slice(0, 16)
const statusOptions = computed(() => {
  const options = [
    { label: '全部', value: '' },
    { label: '报名中', value: 'REGISTRATION_OPEN' },
    { label: '已满员', value: 'FULL' },
    { label: '报名截止', value: 'CLOSED' },
    { label: '已结束', value: 'FINISHED' },
  ]
  if (authStore.user?.role === 'ADMIN') {
    options.push({ label: '已删除', value: 'DELETED' })
  }
  return options
})

async function loadEvents() {
  loading.value = true
  error.value = false
  try {
    await syncQuery()
    const result = await getEvents({
      ...query,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      mine: authStore.user?.role === 'ORGANIZER' ? true : undefined,
    })
    events.value = result.records
    total.value = result.total
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function search() {
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

async function removeEvent(event: EventItem) {
  try {
    await ElMessageBox.confirm(`确认删除“${event.title}”？删除后活动将不再展示，历史记录仍会保留。`, '删除活动', {
      confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning',
    })
  } catch {
    return
  }
  deletingId.value = event.id
  try {
    await deleteEvent(event.id)
    ElMessage.success('活动已删除')
    if (events.value.length === 1 && query.page > 1) query.page--
    await loadEvents()
  } catch {
    // The API interceptor displays the server error.
  } finally {
    deletingId.value = undefined
  }
}

onMounted(loadEvents)
</script>

<template>
  <main class="app-shell">
    <section class="page-heading">
      <div>
        <h1>活动管理</h1>
        <p class="section-note">{{ authStore.user?.role === 'ADMIN' ? '查看和维护全部活动，支持搜索、状态筛选、名单查看和活动编辑。' : '查看和维护自己创建的活动，支持搜索、状态筛选、名单查看和活动编辑。' }}</p>
      </div>
      <div class="action-row">
        <el-button :icon="ArrowLeft" @click="router.push('/events')">活动大厅</el-button>
        <el-button type="primary" :icon="Plus" @click="openPage('/manage/events/new')">新建活动</el-button>
      </div>
    </section>
    <section class="toolbar-card">
      <div class="toolbar-line">
        <el-input v-model.trim="query.keyword" :prefix-icon="Search" placeholder="搜索标题、地点、文案或介绍" clearable @keyup.enter="search" @clear="search" />
        <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
      <div class="toolbar-line">
        <el-segmented v-model="query.status" :options="statusOptions" @change="changeFilter" />
      </div>
    </section>
    <el-alert v-if="error" class="state-alert" title="活动加载失败，请重试" type="error" :closable="false" show-icon />
    <section class="management-panel">
      <el-table v-loading="loading" :data="events" row-key="id" empty-text="暂无活动">
        <el-table-column prop="title" label="活动标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="organizerName" label="组织者" min-width="120" />
        <el-table-column prop="location" label="地点" min-width="150" show-overflow-tooltip />
        <el-table-column label="活动时间" min-width="180">
          <template #default="{ row }"><div>{{ formatTime(row.startTime) }}</div><div>{{ formatTime(row.endTime) }}</div></template>
        </el-table-column>
        <el-table-column label="人数 / 剩余" min-width="125">
          <template #default="{ row }">{{ row.currentParticipants }} / {{ row.maxParticipants }}<br />剩余 {{ row.remainingParticipants }}</template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }"><el-tag :type="statusTypes[row.displayStatus] || 'info'">{{ statuses[row.displayStatus] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="196" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-tooltip content="查看详情"><el-button circle :icon="View" aria-label="查看详情" @click="openPage(`/events/${row.id}`)" /></el-tooltip>
              <el-tooltip content="查看名单"><el-button circle :icon="List" aria-label="查看名单" @click="openPage(`/manage/events/${row.id}/registrations`)" /></el-tooltip>
              <el-tooltip :content="row.displayStatus === 'FINISHED' ? '已结束活动不能编辑' : '编辑活动'">
                <span><el-button circle :icon="Edit" aria-label="编辑活动" :disabled="row.displayStatus === 'FINISHED'" @click="openPage(`/manage/events/${row.id}/edit`)" /></span>
              </el-tooltip>
              <el-tooltip :content="row.displayStatus === 'DELETED' ? '活动已删除' : '删除活动'"><el-button circle type="danger" plain :icon="Delete" aria-label="删除活动" :loading="deletingId === row.id" :disabled="deletingId !== undefined || row.displayStatus === 'DELETED'" @click="removeEvent(row)" /></el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
    <div v-if="total > query.pageSize" class="pagination-line">
      <el-pagination v-model:current-page="query.page" :page-size="query.pageSize" :total="total" layout="prev, pager, next" @current-change="loadEvents" />
    </div>
  </main>
</template>
