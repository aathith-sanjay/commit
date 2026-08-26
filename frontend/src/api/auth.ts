import type { AuthResponse, LoginPayload, RegisterPayload, User } from '../types/auth'
import client from './client'

const authClient = client

export const register = (payload: RegisterPayload): Promise<AuthResponse> =>
  authClient.post<AuthResponse>('/auth/register', payload).then((r) => r.data)

export const login = (payload: LoginPayload): Promise<AuthResponse> =>
  authClient.post<AuthResponse>('/auth/login', payload).then((r) => r.data)

export const getMe = (): Promise<User> =>
  authClient.get<User>('/auth/me').then((r) => r.data)
