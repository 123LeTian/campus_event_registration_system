import type { DisplayStatus, PageResponse } from '@/types/event'

export interface StatisticsOverview {
  totalEvents: number
  totalRegistrations: number
  openEvents: number
  finishedEvents: number
}

export interface EventStatistics {
  eventId: number
  title: string
  maxParticipants: number
  currentParticipants: number
  remainingParticipants: number
  registrationRate: number
  displayStatus: DisplayStatus

}

export interface StatusDistributionItem {
  status: Exclude<DisplayStatus, 'DELETED'>
  label: string
  count: number
}

export type EventStatisticsPage = PageResponse<EventStatistics>
