<script setup lang="ts">
import { ArrowLeft, Calendar, Edit, Location, Minus, Plus, User } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { getEventDetail } from '@/api/event'
import { cancelEventRegistration, registerForEvent } from '@/api/registration'
import { usePageNavigation } from '@/composables/usePageNavigation'
import { useAuthStore } from '@/stores/auth'
import type { EventItem } from '@/types/event'

const route = useRoute()
const authStore = useAuthStore()
const { openPage, goBack, returnLabel } = usePageNavigation('/events', [
  'events', 'manage-events', 'my-registrations', 'event-registrations',
])
const loading = ref(false)
const error = ref(false)
const registrationLoading = ref(false)
const event = ref<EventItem>()

const canManage = computed(() => {
  if (!authStore.user || !event.value) {
    return false
  }
  return authStore.user.role === 'ADMIN' || (authStore.user.role === 'ORGANIZER' && authStore.user.id === event.value.organizerId)

})

const isStudent = computed(() => authStore.user?.role === 'STUDENT')
const canCancel = computed(() => Boolean(event.value?.registeredByCurrentUser && event.value?.displayStatus !== 'FINISHED'))
const canRegister = computed(() => Boolean(isStudent.value && event.value && !event.value.registeredByCurrentUser && event.value.displayStatus === 'REGISTRATION_OPEN'))
const registrationMessage = computed(() => {
  if (!event.value) return ''
  if (event.value.registeredByCurrentUser) return '你已报名该活动'
  if (event.value.displayStatus === 'FULL') return '活动名额已满'
  if (event.value.displayStatus === 'CLOSED') return '报名已截止'
  if (event.value.displayStatus === 'FINISHED') return '活动已结束'
  if (event.value.displayStatus === 'DELETED') return '活动已删除'
  return ''
})

const statusMeta: Record<string, { label: string; type: 'success' | 'info' | 'warning' | 'danger' }> = {
  REGISTRATION_OPEN: { label: '报名开放', type: 'success' },
  CLOSED: { label: '报名截止', type: 'warning' },
  FULL: { label: '名额已满', type: 'danger' },
  FINISHED: { label: '已结束', type: 'info' },
  DELETED: { label: '已删除', type: 'info' },
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function normalizedRate(value?: number) {
  return Math.min(100, Math.max(0, Number(value) || 0))
}

async function loadDetail() {
  loading.value = true
  error.value = false
  event.value = undefined
  try {
    event.value = await getEventDetail(String(route.params.id))
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function editEvent() {
  if (event.value) {
    void openPage(`/manage/events/${event.value.id}/edit`)
  }
}

async function register() {
  if (!event.value || registrationLoading.value || event.value.displayStatus !== 'REGISTRATION_OPEN') return
  registrationLoading.value = true
  try {
    event.value = await registerForEvent(event.value.id)
    ElMessage.success('报名成功')
  } catch {
    // The API interceptor displays the backend message and the current detail remains visible.
  } finally {
    registrationLoading.value = false
  }
}

async function cancelRegistration() {
  if (!event.value || registrationLoading.value || !canCancel.value) return
  try {
    await ElMessageBox.confirm('确认取消本次活动报名吗？取消后名额会恢复。', '取消报名', {
      confirmButtonText: '确认取消',
      cancelButtonText: '暂不取消',
      type: 'warning',
    })
  } catch {
    return
  }
  registrationLoading.value = true
  try {
    event.value = await cancelEventRegistration(event.value.id)
    ElMessage.success('已取消报名')
  } catch {
    // The API interceptor displays the backend message and the current detail remains visible.
  } finally {
    registrationLoading.value = false
  }
}

watch(() => route.params.id, loadDetail, { immediate: true })
</script>

<template>
  <main class="app-shell event-detail-page">
    <section class="page-heading detail-page-heading">
      <div>
        <p class="eyebrow">活动详情</p>
        <h1>{{ event?.title || '活动详情' }}</h1>
      </div>
      <div class="action-row">
        <el-button :icon="ArrowLeft" @click="goBack">{{ returnLabel }}</el-button>
      </div>
    </section>

    <section v-loading="loading" class="detail-panel">
      <el-alert v-if="error" title="活动不存在或暂时无法加载" type="error" :closable="false" show-icon />
      <template v-if="event">
        <div class="detail-hero">
          <div>
            <div class="detail-title-line">
              <el-tag :type="statusMeta[event.displayStatus]?.type || 'info'">{{ statusMeta[event.displayStatus]?.label || event.displayStatus }}</el-tag>
              <span class="section-note">报名率 {{ normalizedRate(event.registrationRate).toFixed(0) }}%</span>
            </div>
            <p class="detail-location-line"><el-icon><Location /></el-icon>{{ event.location }}</p>
            <p class="detail-copywriting">{{ event.copywriting }}</p>
            <p class="detail-description">{{ event.description }}</p>
          </div>

          <aside class="registration-box">
            <strong>{{ event.currentParticipants }} / {{ event.maxParticipants }} 人</strong>
            <el-progress :percentage="normalizedRate(event.registrationRate)" :stroke-width="8" :show-text="false" />
            <p>剩余名额：{{ event.remainingParticipants }}</p>
            <template v-if="isStudent">
              <el-button
                v-if="event.registeredByCurrentUser"
                type="warning"
                :icon="Minus"
                :loading="registrationLoading"
                :disabled="event.displayStatus === 'FINISHED' || event.displayStatus === 'DELETED'"
                @click="cancelRegistration"
              >
                {{ event.displayStatus === 'FINISHED' ? '活动已结束' : '取消报名' }}
              </el-button>
              <el-button
                v-else
                type="primary"
                :icon="Plus"
                :loading="registrationLoading"
                :disabled="!canRegister"
                @click="register"
              >
                {{ registrationMessage || '立即报名' }}
              </el-button>
              <p v-if="registrationMessage">{{ registrationMessage }}</p>
            </template>
            <el-button
              v-else-if="canManage"
              type="primary"
              :icon="Edit"
              :disabled="event.displayStatus === 'FINISHED'"
              @click="editEvent"
            >编辑活动</el-button>
          </aside>
        </div>

        <div class="detail-info-grid">
          <div class="detail-info-item">
            <span>组织者</span>
            <strong><el-icon><User /></el-icon><span>{{ event.organizerName }}</span></strong>
          </div>
          <div class="detail-info-item">
            <span>活动时间</span>
            <strong><el-icon><Calendar /></el-icon><span>{{ formatDateTime(event.startTime) }} 至 {{ formatDateTime(event.endTime) }}</span></strong>
          </div>
          <div class="detail-info-item">
            <span>报名截止</span>
            <strong><el-icon><Calendar /></el-icon><span>{{ formatDateTime(event.registrationDeadline) }}</span></strong>
          </div>
        </div>
      </template>
    </section>
  </main>
</template>
