<script setup lang="ts">
import { DataAnalysis, Refresh } from '@element-plus/icons-vue'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use, type ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'

import { getEventStatistics, getStatisticsOverview, getStatusDistribution } from '@/api/statistics'
import type { EventStatistics, StatisticsOverview, StatusDistributionItem } from '@/types/statistics'

use([BarChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const loading = ref(false)
const error = ref(false)
const overview = ref<StatisticsOverview>({
  totalEvents: 0,
  totalRegistrations: 0,
  openEvents: 0,
  finishedEvents: 0,
})
const distribution = ref<StatusDistributionItem[]>([])
const events = ref<EventStatistics[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10 })

const statusMeta: Record<string, { label: string; type: 'success' | 'info' | 'warning' | 'danger' }> = {
  REGISTRATION_OPEN: { label: '报名中', type: 'success' },
  FULL: { label: '已满员', type: 'danger' },
  CLOSED: { label: '报名截止', type: 'warning' },
  FINISHED: { label: '已结束', type: 'info' },
}
const overviewCards = computed(() => [
  { label: '活动总数', value: overview.value.totalEvents },
  { label: '报名总人数', value: overview.value.totalRegistrations },
  { label: '报名中活动', value: overview.value.openEvents },
  { label: '已结束活动', value: overview.value.finishedEvents },
])
const hasDistribution = computed(() => distribution.value.some((item) => item.count > 0))
const hasEventChart = computed(() => events.value.length > 0)

const distributionChartRef = ref<HTMLDivElement>()
const rateChartRef = ref<HTMLDivElement>()
let distributionChart: ECharts | undefined
let rateChart: ECharts | undefined

async function loadStatistics() {
  loading.value = true
  error.value = false
  try {
    const [overviewResult, distributionResult, eventPage] = await Promise.all([
      getStatisticsOverview(),
      getStatusDistribution(),
      getEventStatistics({ page: query.page, pageSize: query.pageSize }),
    ])
    overview.value = overviewResult
    distribution.value = distributionResult
    events.value = eventPage.records
    total.value = eventPage.total
    await nextTick()
    renderCharts()
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  renderDistributionChart()
  renderRateChart()
}

function renderDistributionChart() {
  if (!distributionChartRef.value || !hasDistribution.value) {
    distributionChart?.dispose()
    distributionChart = undefined
    return
  }
  distributionChart ||= init(distributionChartRef.value)
  distributionChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}' },
    legend: { bottom: 0, left: 'center' },
    series: [
      {
        name: '活动状态',
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['50%', '43%'],
        avoidLabelOverlap: true,
        data: distribution.value.map((item) => ({ name: item.label, value: item.count })),
      },
    ],
  })
}

function renderRateChart() {
  if (!rateChartRef.value || !hasEventChart.value) {
    rateChart?.dispose()
    rateChart = undefined
    return
  }
  rateChart ||= init(rateChartRef.value)
  rateChart.setOption({
    grid: { left: 40, right: 18, top: 24, bottom: 72, containLabel: true },
    tooltip: { trigger: 'axis', valueFormatter: (value: number) => `${value}%` },
    xAxis: {
      type: 'category',
      data: events.value.map((event) => event.title),
      axisLabel: { interval: 0, rotate: 32, width: 96, overflow: 'truncate' },
    },
    yAxis: { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      {
        name: '报名率',
        type: 'bar',
        data: events.value.map((event) => event.registrationRate),
        barMaxWidth: 34,
        itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] },
      },
    ],
  })
}

function resizeCharts() {
  distributionChart?.resize()
  rateChart?.resize()
}

function changePage() {
  void loadStatistics()
}

onMounted(() => {
  void loadStatistics()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  distributionChart?.dispose()
  rateChart?.dispose()
})

watch(() => [distribution.value, events.value], () => {
  void nextTick(renderCharts)
})
</script>

<template>
  <main class="app-shell statistics-page">
    <section class="page-heading">
      <div>
        <h1>数据统计</h1>
        <p class="section-note">查看活动总量、报名人数、活动状态分布、活动报名率和统计明细。</p>
      </div>
      <div class="action-row">
        <el-tooltip content="刷新统计">
          <el-button circle :icon="Refresh" aria-label="刷新统计" :loading="loading" @click="loadStatistics" />
        </el-tooltip>
      </div>
    </section>

    <el-alert v-if="error" title="统计数据加载失败，请重试" type="error" :closable="false" show-icon />

    <section class="statistics-content-panel" aria-label="统计内容">
      <section v-loading="loading" class="statistics-overview">
        <article v-for="card in overviewCards" :key="card.label" class="statistics-card">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </section>

      <section class="statistics-chart-grid">
        <article class="statistics-panel">
          <div class="statistics-panel-heading">
            <div>
              <h2>活动状态分布</h2>
              <p>实时统计未删除活动的展示状态</p>
            </div>
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <el-empty v-if="!loading && !error && !hasDistribution" description="暂无活动状态数据" />
          <div v-else ref="distributionChartRef" class="chart-container" />
        </article>

        <article class="statistics-panel">
          <div class="statistics-panel-heading">
            <div>
              <h2>活动报名率</h2>
              <p>当前页活动的报名人数占最大人数比例</p>
            </div>
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <el-empty v-if="!loading && !error && !hasEventChart" description="暂无活动报名率数据" />
          <div v-else ref="rateChartRef" class="chart-container" />
        </article>
      </section>

      <section class="statistics-panel statistics-table-panel">
        <div class="statistics-panel-heading">
          <div>
            <h2>活动统计明细</h2>
            <p>报名人数、剩余名额与状态均来自统计接口</p>
          </div>
        </div>
        <el-table v-loading="loading" :data="events" row-key="eventId" empty-text="暂无活动统计数据">
          <el-table-column prop="title" label="活动标题" min-width="220" show-overflow-tooltip />
          <el-table-column label="当前 / 最大" min-width="120">
            <template #default="{ row }">{{ row.currentParticipants }} / {{ row.maxParticipants }}</template>
          </el-table-column>
          <el-table-column prop="remainingParticipants" label="剩余名额" min-width="110" />
          <el-table-column label="报名率" min-width="110">
            <template #default="{ row }">{{ row.registrationRate.toFixed(2) }}%</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusMeta[row.displayStatus]?.type || 'info'">
                {{ statusMeta[row.displayStatus]?.label || row.displayStatus }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="total > query.pageSize" class="pagination-line">
          <el-pagination
            v-model:current-page="query.page"
            :page-size="query.pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="changePage"
          />
        </div>
      </section>
    </section>
  </main>
</template>
