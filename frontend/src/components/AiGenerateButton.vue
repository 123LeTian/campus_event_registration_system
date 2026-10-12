<script setup lang="ts">
import { MagicStick, Promotion } from '@element-plus/icons-vue'

withDefaults(defineProps<{
  kind: 'introduction' | 'promotion'
  loading?: boolean
  disabled?: boolean
}>(), {
  loading: false,
  disabled: false,
})

defineEmits<{
  click: []
}>()
</script>

<template>
  <el-button
    type="primary"
    plain
    :icon="kind === 'introduction' ? MagicStick : Promotion"
    :loading="loading"
    :disabled="disabled"
    @click="$emit('click')"
  >
    {{ kind === 'introduction' ? 'AI 生成活动介绍' : 'AI 生成宣传文案' }}
  </el-button>
</template>
