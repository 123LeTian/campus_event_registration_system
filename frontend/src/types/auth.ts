export type UserRole = 'STUDENT' | 'ORGANIZER' | 'ADMIN'

export interface UserInfo {
  id: number
  username: string
  name: string
  role: UserRole

}

export interface LoginRequest {
  username: string
  password: string

}

export interface RegisterRequest {
  username: string
  password: string
  name: string

}

export interface LoginResponse {
  token: string
  user: UserInfo

}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T

}
