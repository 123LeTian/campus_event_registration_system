export type DisplayStatus = 'REGISTRATION_OPEN' | 'CLOSED' | 'FULL' | 'FINISHED' | 'DELETED'
export type EventStatusFilter = DisplayStatus

export interface EventItem {
  id: number
  title: string
  copywriting: string
  description: string
  location: string
  organizerId: number
  organizerName: string
  startTime: string
  endTime: string
  registrationDeadline: string
  maxParticipants: number
  currentParticipants: number
  remainingParticipants: number
  registrationRate: number
  status: string
  displayStatus: DisplayStatus
  registeredByCurrentUser: boolean
  createdAt: string
  updatedAt: string

}

export interface EventPayload {
  title: string
  copywriting: string
  description: string
  location: string
  startTime: string
  endTime: string
  registrationDeadline: string
  maxParticipants: number
}

export interface EventQuery {
  page?: number
  pageSize?: number
  keyword?: string
  status?: EventStatusFilter
  mine?: boolean
}

export interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}
