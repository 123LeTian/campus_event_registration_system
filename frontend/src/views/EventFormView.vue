<script setup lang="ts">
import { ArrowLeft, Check, MagicStick } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus/es/components/form/index'
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { generateEventIntroduction } from '@/api/ai'
import { createEvent, getEventDetail, updateEvent } from '@/api/event'
import { usePageNavigation } from '@/composables/usePageNavigation'
import { useAuthStore } from '@/stores/auth'
import type { EventPayload } from '@/types/event'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const ready = ref(false)
const timesLocked = ref(false)
const minimumCapacity = ref(1)
const aiPanelVisible = ref(false)
const aiIntroductionLoading = ref(false)
const aiError = ref('')
const aiContext = reactive({
  activityType: '',
  audience: '',
  keywords: '',
})
const editing = computed(() => Boolean(route.params.id))
const { goBack, returnLabel, returnTarget } = usePageNavigation(
  () => editing.value ? `/events/${route.params.id}` : '/manage/events',
  ['event-detail', 'manage-events'],
)
const canUseAi = computed(() => authStore.user?.role === 'ORGANIZER' || authStore.user?.role === 'ADMIN')
const emptyForm = (): EventPayload => ({ title: '', copywriting: '', description: '', location: '', startTime: '', endTime: '', registrationDeadline: '', maxParticipants: 30 })
const form = reactive<EventPayload>(emptyForm())
function getErrorMessage(error: unknown, fallback: string) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response
    const message = response?.data?.message?.trim()
    if (message) return message
  }
  return fallback
}

function formatAiError(error: unknown) {
  return `${getErrorMessage(error, 'AI 生成失败')}，当前活动内容未改变。`
}

const endTimeValidator = (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
  if (form.startTime && form.endTime && form.endTime <= form.startTime) return callback(new Error('结束时间必须晚于开始时间'))
  callback()
}

const registrationDeadlineValidator = (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
  if (form.startTime && form.registrationDeadline && form.registrationDeadline >= form.startTime) return callback(new Error('报名截止时间必须早于开始时间'))
  callback()
}
const rules: FormRules<EventPayload> = {
  title: [{ required: true, whitespace: true, message: '请输入活动标题', trigger: 'blur' }, { max: 100, message: '标题不能超过 100 字', trigger: 'blur' }],
  copywriting: [{ required: true, whitespace: true, message: '请输入活动文案', trigger: 'blur' }, { max: 300, message: '活动文案不能超过 300 字', trigger: 'blur' }],
  description: [{ required: true, whitespace: true, message: '请输入活动介绍', trigger: 'blur' }],
  location: [{ required: true, whitespace: true, message: '请输入活动地点', trigger: 'blur' }, { max: 100, message: '地点不能超过 100 字', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }, { validator: endTimeValidator, trigger: 'change' }],
  registrationDeadline: [{ required: true, message: '请选择报名截止时间', trigger: 'change' }, { validator: registrationDeadlineValidator, trigger: 'change' }],
  maxParticipants: [{ required: true, type: 'integer', min: 1, max: 2147483647, message: '人数上限必须是大于 0 的整数', trigger: 'change' }],
}

async function validateTimeFields() {
  if (!ready.value || loading.value || !formRef.value) return
  await nextTick()
  const fields: (keyof EventPayload)[] = []
  if (form.endTime) fields.push('endTime')
  else formRef.value.clearValidate('endTime')
  if (form.registrationDeadline) fields.push('registrationDeadline')
  else formRef.value.clearValidate('registrationDeadline')
  if (fields.length === 0) return
  await formRef.value.validateField(fields).catch(() => undefined)
}

async function loadForm() {
  ready.value = false
  loading.value = true
  timesLocked.value = false
  minimumCapacity.value = 1
  aiPanelVisible.value = false
  aiError.value = ''
  Object.assign(aiContext, { activityType: '', audience: '', keywords: '' })
  Object.assign(form, emptyForm())
  try {
    if (editing.value) {
      const event = await getEventDetail(String(route.params.id))
      if (authStore.user?.role !== 'ADMIN' && event.organizerId !== authStore.user?.id) {
        ElMessage.error('无权编辑他人活动')
        await goBack()
        return
      }
      if (event.displayStatus === 'FINISHED') {
        ElMessage.warning('活动已结束，不能编辑')
        await router.replace(returnTarget.value.name === 'event-detail' ? returnTarget.value.fullPath : {
          path: `/events/${event.id}`,
          query: { returnTo: returnTarget.value.fullPath },
        })
        return
      }
      for (const key of Object.keys(form) as (keyof EventPayload)[]) {
        Object.assign(form, { [key]: event[key] })
      }
      timesLocked.value = event.currentParticipants > 0
      minimumCapacity.value = Math.max(1, event.currentParticipants)
    }
    ready.value = true
    formRef.value?.clearValidate()
  } catch {
    // Leave saving disabled when the event could not be loaded.
  } finally {
    loading.value = false
  }
}

function buildAiContext() {
  return {
    title: form.title.trim(),
    location: form.location.trim(),
    audience: aiContext.audience.trim(),
    activityType: aiContext.activityType.trim(),
    keywords: aiContext.keywords.trim(),
  }
}

function validateAiContext() {
  const missing: string[] = []
  if (!form.title.trim()) missing.push('活动标题')
  if (!form.location.trim()) missing.push('活动地点')
  if (!aiContext.activityType.trim()) missing.push('活动类型')
  if (!aiContext.audience.trim()) missing.push('适合人群')
  if (!aiContext.keywords.trim()) missing.push('关键词')
  if (missing.length > 0) {
    ElMessage.warning(`请先填写${missing.join('、')}`)
    return false
  }
  return true
}

function toggleAiPanel() {
  aiError.value = ''
  aiPanelVisible.value = !aiPanelVisible.value
}

async function generateIntroduction() {
  if (aiIntroductionLoading.value || !validateAiContext()) return
  aiError.value = ''
  aiIntroductionLoading.value = true
  try {
    const result = await generateEventIntroduction(buildAiContext())
    form.copywriting = result.promotion?.trim() || ''
    form.description = result.introduction?.trim() || ''
    ElMessage.success('AI 已生成并填入活动文案和活动介绍')
    await formRef.value?.validateField(['copywriting', 'description']).catch(() => undefined)
  } catch (error) {
    aiError.value = formatAiError(error)
  } finally {
    aiIntroductionLoading.value = false
  }
}

async function save() {
  if (!ready.value || saving.value || !formRef.value) return
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    const payload = { ...form }
    if (editing.value) await updateEvent(String(route.params.id), payload)
    else await createEvent(payload)
    ElMessage.success(editing.value ? '活动更新成功' : '活动发布成功')
    await goBack()
  } catch {
    // The API interceptor displays the server error while preserving the form.
  } finally {
    saving.value = false
  }
}

watch(() => route.params.id, loadForm, { immediate: true })
watch(
  () => [form.startTime, form.endTime, form.registrationDeadline],
  () => {
    void validateTimeFields()
  },
)
</script>

<template>
  <main class="app-shell">
    <section class="page-heading">
      <div>
        <h1>{{ editing ? '编辑活动' : '新建活动' }}</h1>
        <p class="section-note">填写基础信息后可使用 AI 生成活动文案和介绍草稿，最终保存内容仍以表单为准。</p>
      </div>
      <el-button :icon="ArrowLeft" :disabled="saving" @click="goBack">{{ returnLabel }}</el-button>
    </section>
    <section v-loading="loading" class="form-panel">
      <el-alert v-if="!loading && !ready" title="活动信息加载失败" type="error" :closable="false" />
      <el-alert v-if="timesLocked" title="已有报名，活动时间不可修改" type="info" :closable="false" />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="loading || saving || !ready" @submit.prevent="save">
        <section class="form-section">
          <div class="section-heading">
            <h2>基础信息</h2>
            <span class="section-note">标题、地点、文案和介绍会直接展示给学生。</span>
          </div>
          <el-form-item label="活动标题" prop="title"><el-input v-model.trim="form.title" maxlength="100" show-word-limit placeholder="例如：2026 校园科技文化节" /></el-form-item>
          <el-form-item label="活动地点" prop="location"><el-input v-model.trim="form.location" maxlength="100" show-word-limit placeholder="例如：学生活动中心报告厅" /></el-form-item>
          <div class="description-field-heading">
            <span class="required-field-label">活动文案与活动介绍</span>
            <button
              v-if="canUseAi"
              class="inline-ai-button"
              type="button"
              :disabled="loading || saving || !ready"
              @click.stop.prevent="toggleAiPanel"
            >
              <el-icon><MagicStick /></el-icon>
              {{ aiPanelVisible ? '收起 AI 辅助' : 'AI 辅助' }}
            </button>
          </div>
          <section v-if="canUseAi && aiPanelVisible" class="inline-ai-panel">
            <p class="inline-ai-note">结合已填写的活动标题、地点和补充信息生成活动文案与活动介绍，生成后会直接填入下方两个输入框。</p>
            <div class="inline-ai-grid">
              <label class="inline-ai-field">
                <span class="required-ai-label">活动类型</span>
                <el-input v-model.trim="aiContext.activityType" maxlength="100" placeholder="例如：校园科技、社团交流" />
              </label>
              <label class="inline-ai-field">
                <span class="required-ai-label">适合人群</span>
                <el-input v-model.trim="aiContext.audience" maxlength="100" placeholder="例如：全校学生" />
              </label>
              <label class="inline-ai-field">
                <span class="required-ai-label">关键词</span>
                <el-input v-model.trim="aiContext.keywords" maxlength="300" placeholder="例如：创新、社团、实践" />
              </label>
            </div>
            <el-alert v-if="aiError" class="inline-ai-alert" :title="aiError" type="warning" :closable="false" show-icon />
            <div class="action-row inline-ai-actions">
              <el-button type="primary" :icon="MagicStick" :loading="aiIntroductionLoading" @click="generateIntroduction">生成并填入</el-button>
            </div>
          </section>
          <el-form-item label="活动文案" prop="copywriting" class="description-form-item">
            <el-input v-model.trim="form.copywriting" maxlength="300" show-word-limit placeholder="用一句话概括活动亮点，适合在活动卡片中展示" />
          </el-form-item>
          <el-form-item label="活动介绍" prop="description" class="description-form-item">
            <el-input v-model.trim="form.description" type="textarea" :rows="6" placeholder="说明活动内容、流程、注意事项和适合人群" />
          </el-form-item>
        </section>

        <section class="form-section">
          <div class="section-heading">
            <h2>时间与名额</h2>
            <span class="section-note">结束时间必须晚于开始时间，报名截止必须早于开始时间。</span>
          </div>
          <div class="form-grid">
            <el-form-item label="开始时间" prop="startTime"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm:ss" :disabled="timesLocked" /></el-form-item>
            <el-form-item label="结束时间" prop="endTime"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm:ss" :disabled="timesLocked" /></el-form-item>
            <el-form-item label="报名截止时间" prop="registrationDeadline"><el-date-picker v-model="form.registrationDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm:ss" :disabled="timesLocked" /></el-form-item>
            <el-form-item label="人数上限" prop="maxParticipants"><el-input-number v-model="form.maxParticipants" :min="minimumCapacity" :max="2147483647" :precision="0" controls-position="right" /></el-form-item>
          </div>
        </section>

        <div class="action-row">
          <el-button native-type="submit" type="primary" :icon="Check" :loading="saving" :disabled="!ready">{{ editing ? '保存修改' : '发布活动' }}</el-button>
          <el-button @click="goBack">取消</el-button>
        </div>
      </el-form>
    </section>
  </main>
</template>
