import type { DisplayStatus, EventItem, PageResponse } from '@/types/event'

export type RegistrationResult = EventItem

export type RegistrationStatus = 'ACTIVE' | 'CANCELLED'
export type RegistrationStatusFilter = RegistrationStatus | 'ALL'

export interface RegistrationItem {
  id: number
  eventId: number
  eventTitle?: string
  eventLocation?: string
  eventStartTime?: string
  eventEndTime?: string
  eventDisplayStatus?: DisplayStatus
  userId: number
  studentUsername: string
  studentName: string
  status: RegistrationStatus
  registrationTime: string
  cancelledTime?: string
}

export interface RegistrationQuery {
  page?: number
  pageSize?: number
  status?: RegistrationStatusFilter
}

export type RegistrationPage = PageResponse<RegistrationItem>
