import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElAlert } from 'element-plus/es/components/alert/index'
import { ElButton } from 'element-plus/es/components/button/index'
import { ElDatePicker } from 'element-plus/es/components/date-picker/index'
import { ElEmpty } from 'element-plus/es/components/empty/index'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index'
import { ElIcon } from 'element-plus/es/components/icon/index'
import { ElInput } from 'element-plus/es/components/input/index'
import { ElInputNumber } from 'element-plus/es/components/input-number/index'
import { ElLoading } from 'element-plus/es/components/loading/index'
import { ElPagination } from 'element-plus/es/components/pagination/index'
import { ElProgress } from 'element-plus/es/components/progress/index'
import { ElSegmented } from 'element-plus/es/components/segmented/index'
import { ElSkeleton, ElSkeletonItem } from 'element-plus/es/components/skeleton/index'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index'
import { ElTag } from 'element-plus/es/components/tag/index'
import { ElTooltip } from 'element-plus/es/components/tooltip/index'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import './styles.css'

const app = createApp(App)

app.use(createPinia()).use(router)

;[
  ElAlert,
  ElButton,
  ElDatePicker,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElInputNumber,
  ElLoading,
  ElPagination,
  ElProgress,
  ElSegmented,
  ElSkeleton,
  ElSkeletonItem,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTooltip,
].forEach((component) => app.use(component))

app.mount('#app')
