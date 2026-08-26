import { useState } from 'react'
import { Link } from 'react-router-dom'
import { completeHabit } from '../api/habits'
import type { Habit } from '../types'
import TreeBadge from './TreeBadge'
import './HabitCard.css'

interface Props {
  habit: Habit
  onUpdated: (habit: Habit) => void
}

export default function HabitCard({ habit, onUpdated }: Props) {
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
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message ??
            'Failed to complete habit'
          : 'Failed to complete habit'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={`habit-card ${habit.todayCompleted ? 'habit-card--done' : ''}`}>
      <div className="habit-card__header">
        <Link to={`/habits/${habit.id}`} className="habit-card__name">
          {habit.name}
        </Link>
        <TreeBadge stage={habit.treeStage} state={habit.treeState} />
      </div>

      <div className="habit-card__streak">
        🔥 {habit.currentStreak} day streak
      </div>

      {error && <p className="habit-card__error">{error}</p>}

      <div className="habit-card__actions">
        {habit.todayCompleted ? (
          <span className="habit-card__done-label">✅ Done today</span>
        ) : (
          <button
            className="btn btn--primary"
            onClick={handleComplete}
            disabled={loading}
          >
            {loading ? 'Saving…' : 'Complete'}
          </button>
        )}
      </div>
    </div>
  )
}
