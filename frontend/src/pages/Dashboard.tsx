import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getHabits } from '../api/habits'
import HabitCard from '../components/HabitCard'
import type { Habit } from '../types'
import './Dashboard.css'

export default function Dashboard() {
  const [habits, setHabits] = useState<Habit[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getHabits()
      .then(setHabits)
      .catch(() => setError('Could not load habits. Is the backend running?'))
      .finally(() => setLoading(false))
  }, [])

  const completed = habits.filter((h) => h.todayCompleted).length
  const total = habits.length

  function handleHabitUpdated(updated: Habit) {
    setHabits((prev) => prev.map((h) => (h.id === updated.id ? updated : h)))
  }

  return (
    <div className="dashboard">
      <header className="dashboard__header">
        <div>
          <h1 className="dashboard__title">commit.</h1>
          <p className="dashboard__subtitle">
            {new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' })}
          </p>
        </div>
        <Link to="/habits/new" className="btn btn--primary">+ New Habit</Link>
      </header>

      {total > 0 && (
        <div className="dashboard__progress">
          <div
            className="dashboard__progress-bar"
            style={{ width: `${(completed / total) * 100}%` }}
          />
          <span className="dashboard__progress-label">
            {completed} / {total} done today
          </span>
        </div>
      )}

      {loading && <p className="state-msg">Loading…</p>}
      {error && <p className="state-msg state-msg--error">{error}</p>}

      {!loading && !error && habits.length === 0 && (
        <div className="dashboard__empty">
          <p>No habits yet.</p>
          <Link to="/habits/new" className="btn btn--primary">Create your first habit</Link>
        </div>
      )}

      <div className="habit-list">
        {habits.map((habit) => (
          <HabitCard key={habit.id} habit={habit} onUpdated={handleHabitUpdated} />
        ))}
      </div>
    </div>
  )
}
