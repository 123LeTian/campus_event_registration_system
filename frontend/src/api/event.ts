import request from '@/api/request'
import type { EventItem, EventPayload, EventQuery, PageResponse } from '@/types/event'

export function getEvents(params: EventQuery = {}) {
  return request.get<unknown, PageResponse<EventItem>>('/events', { params })
}

export function getEventDetail(id: number | string) {
  return request.get<unknown, EventItem>(`/events/${id}`)
}

export function createEvent(data: EventPayload) {
  return request.post<unknown, EventItem>('/events', data)
}

export function updateEvent(id: number | string, data: EventPayload) {
  return request.put<unknown, EventItem>(`/events/${id}`, data)
}

export function deleteEvent(id: number | string) {
  return request.delete<unknown, null>(`/events/${id}`)
}
