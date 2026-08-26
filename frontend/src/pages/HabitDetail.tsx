import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { completeHabit, deleteHabit, getHabit, getHistory } from '../api/habits'
import CompletionCalendar from '../components/CompletionCalendar'
import TreeBadge from '../components/TreeBadge'
import type { Habit, HabitCompletion } from '../types'
import './HabitDetail.css'

export default function HabitDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const habitId = Number(id)

  const [habit, setHabit] = useState<Habit | null>(null)
  const [history, setHistory] = useState<HabitCompletion[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [completing, setCompleting] = useState(false)

  const today = new Date().toISOString().split('T')[0]

  useEffect(() => {
    Promise.all([getHabit(habitId), getHistory(habitId)])
      .then(([h, hist]) => {
        setHabit(h)
        setHistory(hist)
      })
      .catch(() => setError('Could not load habit.'))
      .finally(() => setLoading(false))
  }, [habitId])

  async function handleComplete() {
    if (!habit) return
    setCompleting(true)
    try {
      await completeHabit(habitId, today)
      const [updated, hist] = await Promise.all([getHabit(habitId), getHistory(habitId)])
      setHabit(updated)
      setHistory(hist)
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message ??
            'Already completed today'
          : 'Already completed today'
      setError(msg)
    } finally {
      setCompleting(false)
    }
  }

  async function handleDelete() {
    if (!window.confirm('Delete this habit? Historical completions will also be removed.')) return
    await deleteHabit(habitId)
    navigate('/')
  }

  if (loading) return <p className="state-msg">Loading…</p>
  if (error && !habit) return <p className="state-msg state-msg--error">{error}</p>
  if (!habit) return null

  return (
    <div className="habit-detail">
      <header className="habit-detail__header">
        <Link to="/" className="back-link">← Back</Link>
        <h1>{habit.name}</h1>
        <TreeBadge stage={habit.treeStage} state={habit.treeState} size="lg" />
      </header>

      <div className="stat-row">
        <div className="stat">
          <span className="stat__value">🔥 {habit.currentStreak}</span>
          <span className="stat__label">Current streak</span>
        </div>
        <div className="stat">
          <span className="stat__value">🏆 {habit.longestStreak}</span>
          <span className="stat__label">Longest streak</span>
        </div>
        <div className="stat">
          <span className="stat__value">{history.length}</span>
          <span className="stat__label">Total completions</span>
        </div>
      </div>

      {error && <p className="state-msg state-msg--error">{error}</p>}

      <div className="habit-detail__complete">
        {habit.todayCompleted ? (
          <p className="done-today">✅ Completed today</p>
        ) : (
          <button className="btn btn--primary" onClick={handleComplete} disabled={completing}>
            {completing ? 'Saving…' : 'Complete today'}
          </button>
        )}
      </div>

      <section className="habit-detail__section">
        <h2>Last 6 weeks</h2>
        <CompletionCalendar completions={history} />
      </section>

      <section className="habit-detail__section">
        <h2>History</h2>
        {history.length === 0 ? (
          <p className="state-msg">No completions yet.</p>
        ) : (
          <ul className="history-list">
            {history.slice(0, 30).map((c) => (
              <li key={c.id} className="history-list__item">
                {c.completionDate}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="habit-detail__danger">
        <button className="btn btn--danger" onClick={handleDelete}>Delete habit</button>
      </section>
    </div>
  )
}
