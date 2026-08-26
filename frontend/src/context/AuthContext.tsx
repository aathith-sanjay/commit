import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { login as apiLogin, register as apiRegister, getMe } from '../api/auth'
import type { LoginPayload, RegisterPayload, User } from '../types/auth'

interface AuthContextValue {
  user: User | null
  loading: boolean
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    try {
      const stored = localStorage.getItem('commit_user')
      return stored ? JSON.parse(stored) : null
    } catch {
      return null
    }
  })
  const [loading, setLoading] = useState(!user)

  useEffect(() => {
    const token = localStorage.getItem('commit_token')
    if (!token) { setLoading(false); return }
    if (user) { setLoading(false); return }
    getMe()
      .then((u) => { setUser(u); localStorage.setItem('commit_user', JSON.stringify(u)) })
      .catch(() => { localStorage.removeItem('commit_token'); localStorage.removeItem('commit_user') })
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (payload: LoginPayload) => {
    const { token, user: u } = await apiLogin(payload)
    localStorage.setItem('commit_token', token)
    localStorage.setItem('commit_user', JSON.stringify(u))
    setUser(u)
  }, [])

  const register = useCallback(async (payload: RegisterPayload) => {
    const { token, user: u } = await apiRegister(payload)
    localStorage.setItem('commit_token', token)
    localStorage.setItem('commit_user', JSON.stringify(u))
    setUser(u)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('commit_token')
    localStorage.removeItem('commit_user')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
