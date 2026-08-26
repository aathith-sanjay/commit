import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import AccountSettings from './pages/AccountSettings'
import CreateHabit from './pages/CreateHabit'
import Dashboard from './pages/Dashboard'
import EditHabit from './pages/EditHabit'
import HabitDetail from './pages/HabitDetail'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ProtectedRoute from './components/ProtectedRoute'

// Strip trailing slash from VITE_BASE_PATH to get the React Router basename.
const basename = (import.meta.env.VITE_BASE_PATH ?? '/').replace(/\/$/, '')

function AppRoutes() {
  const { user } = useAuth()

  return (
    <Routes>
      {/* Public auth routes — redirect to home if already logged in */}
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route path="/register" element={user ? <Navigate to="/" replace /> : <RegisterPage />} />

      {/* Protected routes */}
      <Route path="/" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
      <Route path="/habits/new" element={<ProtectedRoute><CreateHabit /></ProtectedRoute>} />
      <Route path="/habits/:id" element={<ProtectedRoute><HabitDetail /></ProtectedRoute>} />
      <Route path="/habits/:id/edit" element={<ProtectedRoute><EditHabit /></ProtectedRoute>} />
      <Route path="/account" element={<ProtectedRoute><AccountSettings /></ProtectedRoute>} />

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter basename={basename}>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
