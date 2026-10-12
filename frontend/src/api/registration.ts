import request from '@/api/request'
import type { RegistrationPage, RegistrationQuery, RegistrationResult } from '@/types/registration'

export function registerForEvent(eventId: number | string) {
  return request.post<unknown, RegistrationResult>(`/events/${eventId}/registrations`)
}

export function cancelEventRegistration(eventId: number | string) {
  return request.delete<unknown, RegistrationResult>(`/events/${eventId}/registrations/me`)
}

export function getEventRegistrations(eventId: number | string, params: RegistrationQuery = {}) {
  return request.get<unknown, RegistrationPage>(`/events/${eventId}/registrations`, { params })
}

export function getMyRegistrations(params: RegistrationQuery = {}) {
  return request.get<unknown, RegistrationPage>('/my/registrations', { params })
}
