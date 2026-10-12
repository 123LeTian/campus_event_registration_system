<script setup lang="ts">
import { Refresh, View } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'

import { getMyRegistrations } from '@/api/registration'
import { useListQuery } from '@/composables/useListQuery'
import { usePageNavigation } from '@/composables/usePageNavigation'
import type { RegistrationItem, RegistrationStatusFilter } from '@/types/registration'

const { openPage } = usePageNavigation()
const loading = ref(false)
const error = ref(false)
const records = ref<RegistrationItem[]>([])
const total = ref(0)
const { query, syncQuery } = useListQuery({ page: 1, pageSize: 10, status: 'ALL' as RegistrationStatusFilter }, ['ALL', 'ACTIVE', 'CANCELLED'])
const emptyText = computed(() => {
  if (query.status === 'ACTIVE') return '暂无有效报名'
  if (query.status === 'CANCELLED') return '暂无已取消报名'
  return '暂无报名记录'
})
const statusLabels: Record<string, string> = { ACTIVE: '有效报名', CANCELLED: '已取消' }
const eventStatuses: Record<string, string> = {
  REGISTRATION_OPEN: '报名开放', CLOSED: '报名截止', FULL: '名额已满', FINISHED: '已结束', DELETED: '已删除',
}
const eventStatusTypes: Record<string, 'success' | 'info' | 'warning' | 'danger'> = {
  REGISTRATION_OPEN: 'success',
  CLOSED: 'warning',
  FULL: 'danger',
  FINISHED: 'info',
  DELETED: 'info',
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

async function loadRegistrations() {
  loading.value = true
  error.value = false
  try {
    await syncQuery()
    const page = await getMyRegistrations({ ...query })
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

onMounted(loadRegistrations)
</script>

<template>
  <main class="app-shell">
    <section class="page-heading">
      <div>
        <h1>我的报名</h1>
        <p class="section-note section-note-lines">
          <span>集中查看自己的有效报名和已取消记录，核对活动时间、地点、报名状态与活动状态；</span>
          <span>活动可访问时可继续进入详情页。</span>
        </p>
      </div>
      <div class="action-row">
        <el-tooltip content="刷新"><el-button circle :icon="Refresh" :loading="loading" aria-label="刷新" @click="loadRegistrations" /></el-tooltip>
      </div>
    </section>

    <section class="toolbar-card registrations-toolbar-card">
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

    <el-alert v-if="error" class="state-alert" title="我的报名加载失败，请重试" type="error" :closable="false" show-icon />
    <section class="management-panel registrations-management-panel">
      <el-table v-loading="loading" :data="records" row-key="id" :empty-text="emptyText">
        <el-table-column prop="eventTitle" label="活动标题" min-width="190" show-overflow-tooltip />
        <el-table-column prop="eventLocation" label="地点" min-width="140" show-overflow-tooltip />
        <el-table-column label="活动时间" min-width="180">
          <template #default="{ row }"><div>{{ formatTime(row.eventStartTime) }}</div><div>{{ formatTime(row.eventEndTime) }}</div></template>
        </el-table-column>
        <el-table-column label="报名时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.registrationTime) }}</template>
        </el-table-column>
        <el-table-column label="取消时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.cancelledTime) }}</template>
        </el-table-column>
        <el-table-column label="报名状态" width="110">
          <template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ statusLabels[row.status] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="活动状态" width="110">
          <template #default="{ row }"><el-tag :type="eventStatusTypes[row.eventDisplayStatus] || 'info'">{{ eventStatuses[row.eventDisplayStatus] || row.eventDisplayStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-tooltip :content="row.eventDisplayStatus === 'DELETED' ? '活动已删除，详情不可用' : '查看详情'">
              <span>
                <el-button
                  circle
                  :icon="View"
                  aria-label="查看详情"
                  :disabled="row.eventDisplayStatus === 'DELETED'"
                  @click="openPage(`/events/${row.eventId}`)"
                />
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </section>
    <div v-if="total > query.pageSize" class="pagination-line">
      <el-pagination v-model:current-page="query.page" :page-size="query.pageSize" :total="total" layout="prev, pager, next" @current-change="loadRegistrations" />
    </div>
  </main>
</template>
