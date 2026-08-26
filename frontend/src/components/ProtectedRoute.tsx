import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <div style={{ padding: 32, color: '#6b7280' }}>Loading…</div>
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}
