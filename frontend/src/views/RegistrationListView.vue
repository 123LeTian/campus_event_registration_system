<script setup lang="ts">
import { ArrowLeft, Refresh, View } from '@element-plus/icons-vue'
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { getEventDetail } from '@/api/event'
import { getEventRegistrations } from '@/api/registration'
import { useListQuery } from '@/composables/useListQuery'
import { usePageNavigation } from '@/composables/usePageNavigation'
import type { RegistrationItem, RegistrationStatusFilter } from '@/types/registration'

const route = useRoute()
const { openPage, goBack, returnLabel } = usePageNavigation('/manage/events', ['manage-events'])
const loading = ref(false)
const error = ref(false)
const records = ref<RegistrationItem[]>([])
const total = ref(0)
const fallbackEventTitle = ref('')
const { query, syncQuery } = useListQuery({ page: 1, pageSize: 10, status: 'ALL' as RegistrationStatusFilter }, ['ALL', 'ACTIVE', 'CANCELLED'])
const eventTitle = computed(() => records.value[0]?.eventTitle || fallbackEventTitle.value || `活动 #${route.params.id}`)
const statusLabels: Record<string, string> = { ACTIVE: '有效报名', CANCELLED: '已取消' }

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

async function loadRegistrations() {
  loading.value = true
  error.value = false
  try {
    await syncQuery()
    const [eventDetail, page] = await Promise.all([
      getEventDetail(String(route.params.id)).catch(() => undefined),
      getEventRegistrations(String(route.params.id), { ...query }),
    ])
    fallbackEventTitle.value = eventDetail?.title || ''
    records.value = page.records
    total.value = page.total
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function changeFilter() {
  query.page = 1
  void loadRegistrations()
}

watch(() => route.params.id, loadRegistrations, { immediate: true })
</script>

<template>
  <main class="app-shell">
    <section class="page-heading">
      <div>
        <h1>报名名单</h1>
        <p class="section-note">查看“{{ eventTitle }}”的全部、有效报名和已取消报名记录。</p>
      </div>
      <div class="action-row">
        <el-button :icon="ArrowLeft" @click="goBack">{{ returnLabel }}</el-button>
        <el-tooltip content="刷新"><el-button circle :icon="Refresh" :loading="loading" aria-label="刷新" @click="loadRegistrations" /></el-tooltip>
      </div>
    </section>

    <section class="toolbar-card">
      <div class="toolbar-line">
        <el-segmented
          v-model="query.status"
          :options="[
            { label: '全部', value: 'ALL' },
            { label: '有效报名', value: 'ACTIVE' },
            { label: '已取消', value: 'CANCELLED' },
          ]"
          @change="changeFilter"
        />
      </div>
    </section>

    <el-alert v-if="error" class="state-alert" title="报名名单加载失败，请确认活动存在且你有权限查看" type="error" :closable="false" show-icon />
    <section class="management-panel">
      <el-table v-loading="loading" :data="records" row-key="id" empty-text="暂无报名记录">
        <el-table-column prop="studentName" label="学生姓名" min-width="130" />
        <el-table-column prop="studentUsername" label="学生账号" min-width="140" />
        <el-table-column label="报名时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.registrationTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ statusLabels[row.status] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="取消时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.cancelledTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="查看活动"><el-button circle :icon="View" aria-label="查看活动" @click="openPage(`/events/${row.eventId}`)" /></el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </section>
    <div v-if="total > query.pageSize" class="pagination-line">
      <el-pagination v-model:current-page="query.page" :page-size="query.pageSize" :total="total" layout="prev, pager, next" @current-change="loadRegistrations" />
    </div>
  </main>
</template>
