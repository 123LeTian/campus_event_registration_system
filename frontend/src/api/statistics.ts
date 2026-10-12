import request from '@/api/request'
import type { EventStatisticsPage, StatisticsOverview, StatusDistributionItem } from '@/types/statistics'

export function getStatisticsOverview() {
  return request.get<unknown, StatisticsOverview>('/statistics/overview')
}

export function getEventStatistics(params: { page?: number; pageSize?: number } = {}) {
  return request.get<unknown, EventStatisticsPage>('/statistics/events', { params })
}

export function getStatusDistribution() {
  return request.get<unknown, StatusDistributionItem[]>('/statistics/status-distribution')
}
