import { BrowserRouter, Route, Routes } from 'react-router-dom'
import CreateHabit from './pages/CreateHabit'
import Dashboard from './pages/Dashboard'
import EditHabit from './pages/EditHabit'
import HabitDetail from './pages/HabitDetail'

// Strip trailing slash from VITE_BASE_PATH to get the React Router basename.
// e.g. '/commit/' -> '/commit', '/' -> '' (empty string = root)
const basename = (import.meta.env.VITE_BASE_PATH ?? '/').replace(/\/$/, '')

export default function App() {
  return (
    <BrowserRouter basename={basename}>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/habits/new" element={<CreateHabit />} />
        <Route path="/habits/:id" element={<HabitDetail />} />
        <Route path="/habits/:id/edit" element={<EditHabit />} />
      </Routes>
    </BrowserRouter>
  )
}
