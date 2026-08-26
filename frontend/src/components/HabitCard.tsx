import { useState } from 'react'
import { Link } from 'react-router-dom'
import { completeHabit } from '../api/habits'
import type { Habit } from '../types'
import TreeBadge from './TreeBadge'
import './HabitCard.css'

interface Props {
  habit: Habit
  onUpdated: (habit: Habit) => void
  scheduledToday?: boolean
}

export default function HabitCard({ habit, onUpdated, scheduledToday = true }: Props) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const today = new Date().toISOString().split('T')[0]

  async function handleComplete() {
    setLoading(true)
    setError(null)
    try {
      await completeHabit(habit.id, today)
      onUpdated({ ...habit, todayCompleted: true, currentStreak: habit.currentStreak + 1 })
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'response' in err
        ? (err as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'Failed'
        : 'Failed'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={`habit-card ${habit.todayCompleted ? 'habit-card--done' : ''} ${!scheduledToday ? 'habit-card--unscheduled' : ''}`}>
      <div className="habit-card__header">
        <div className="habit-card__meta">
          <Link to={`/habits/${habit.id}`} className="habit-card__name">{habit.name}</Link>
          {habit.category && <span className="habit-card__category">{habit.category}</span>}
        </div>
        <TreeBadge stage={habit.treeStage} state={habit.treeState} />
      </div>

      <div className="habit-card__sub">
        <span className="habit-card__streak">🔥 {habit.currentStreak} day streak</span>
        {!scheduledToday && <span className="habit-card__not-due">Not due today</span>}
      </div>

      {error && <p className="habit-card__error">{error}</p>}

      <div className="habit-card__actions">
        {!scheduledToday ? null : habit.todayCompleted ? (
          <span className="habit-card__done-label">✅ Done today</span>
        ) : (
          <button className="btn btn--primary btn--sm" onClick={handleComplete} disabled={loading}>
            {loading ? 'Saving…' : 'Complete'}
          </button>
        )}
      </div>
    </div>
  )
}
