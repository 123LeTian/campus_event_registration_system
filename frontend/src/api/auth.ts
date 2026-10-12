import request from '@/api/request'
import type { LoginRequest, LoginResponse, RegisterRequest, UserInfo } from '@/types/auth'

export function login(data: LoginRequest) {
  return request.post<unknown, LoginResponse>('/auth/login', data)

}

export function register(data: RegisterRequest) {
  return request.post<unknown, UserInfo>('/auth/register', data)

}

export function getCurrentUser() {
  return request.get<unknown, UserInfo>('/users/me')

}
