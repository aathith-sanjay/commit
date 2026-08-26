import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getHabit, getHistory, getAnalytics, completeHabit, undoCompletion, archiveHabit } from '../api/habits'
import ContributionCalendar from '../components/ContributionCalendar'
import SkeletonCard from '../components/SkeletonCard'
import type { AnalyticsResponse, Habit, HabitCompletion } from '../types'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import './HabitDetail.css'

const MILESTONES = [7, 14, 30, 60, 90]
const TREE_EMOJI: Record<string, string> = {
  SEED: '🌱', HERB: '🌿', SHRUB: '🪴', SAPLING: '🌳',
  YOUNG_TREE: '🌲', TREE: '🌳', FLOWERING_TREE: '🌸', FRUIT_TREE: '🍎', MATURE_TREE: '🏔️',
}
const TREE_STATE_LABEL: Record<string, string> = { ALIVE: 'Alive', DEAD: 'Dead' }

function DetailSkeleton() {
  return (
    <div className="detail-page">
      <div className="skeleton-row"><div className="skeleton skeleton--sm" /></div>
      <div className="detail-hero">
        <div className="skeleton skeleton--circle" />
        <div style={{ flex: 1 }}><div className="skeleton skeleton--title" /><div className="skeleton skeleton--text" style={{ marginTop: 8 }} /></div>
      </div>
      <div className="stats-grid">
        {[1,2,3,4].map(i => <SkeletonCard key={i} />)}
      </div>
    </div>
  )
}

export default function HabitDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const habitId = Number(id)

  const [habit, setHabit] = useState<Habit | null>(null)
  const [history, setHistory] = useState<HabitCompletion[]>([])
  const [analytics, setAnalytics] = useState<AnalyticsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [completing, setCompleting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const today = new Date().toISOString().split('T')[0]

  async function load() {
    setError(null)
    try {
      const [h, hist, an] = await Promise.all([
        getHabit(habitId),
        getHistory(habitId),
        getAnalytics(habitId).catch(() => null),
      ])
      setHabit(h)
      setHistory(hist)
      setAnalytics(an)
    } catch {
      setError('Could not load habit.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [habitId])

  async function handleComplete() {
    setCompleting(true)
    try {
      await completeHabit(habitId, today)
      await load()
    } catch {
      setError('Failed to mark complete.')
    } finally {
      setCompleting(false)
    }
  }

  async function handleUndo() {
    setCompleting(true)
    try {
      await undoCompletion(habitId, today)
      await load()
    } catch {
      setError('Could not undo completion.')
    } finally {
      setCompleting(false)
    }
  }

  async function handleArchive() {
    if (!confirm(`Archive "${habit?.name}"?`)) return
    await archiveHabit(habitId)
    navigate('/')
  }

  if (loading) return <DetailSkeleton />

  if (error && !habit) return (
    <div className="detail-page">
      <Link to="/" className="back-link">← Back</Link>
      <div className="error-state">
        <p>{error}</p>
        <button className="btn btn--primary" onClick={load}>Retry</button>
      </div>
    </div>
  )

  if (!habit) return <p className="state-msg">Habit not found.</p>

  const completionRate = analytics ? Math.round(analytics.completionRate * 100) : null
  const nextMilestone = MILESTONES.find((m) => m > habit.currentStreak)

  return (
    <div className="detail-page">
      <header className="detail-header">
        <Link to="/" className="back-link">← Back</Link>
        <div className="detail-header__actions">
          <Link to={`/habits/${habitId}/edit`} className="btn btn--ghost btn--sm">Edit</Link>
          <button className="btn btn--ghost btn--sm btn--danger" onClick={handleArchive}>Archive</button>
        </div>
      </header>

      <div className="detail-hero">
        <span className="detail-tree-emoji">{TREE_EMOJI[habit.treeStage] ?? '🌱'}</span>
        <div>
          <h1 className="detail-name">{habit.name}</h1>
          {habit.description && <p className="detail-desc">{habit.description}</p>}
          <div className="detail-badges">
            {habit.category && <span className="badge badge--cat">{habit.category}</span>}
            <span className={`badge badge--state ${habit.treeState === 'DEAD' ? 'badge--dead' : 'badge--alive'}`}>
              {TREE_STATE_LABEL[habit.treeState] ?? habit.treeState}
            </span>
          </div>
        </div>
      </div>

      <div className="detail-action">
        {habit.todayCompleted ? (
          <div className="detail-action__done">
            <span>✅ Completed today</span>
            <button className="btn-link" onClick={handleUndo} disabled={completing}>Undo</button>
          </div>
        ) : (
          <button className="btn btn--primary" onClick={handleComplete} disabled={completing}>
            {completing ? 'Saving…' : 'Complete today'}
          </button>
        )}
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <span className="stat-value">🔥 {habit.currentStreak}</span>
          <span className="stat-label">Current streak</span>
        </div>
        <div className="stat-card">
          <span className="stat-value">⭐ {habit.longestStreak}</span>
          <span className="stat-label">Longest streak</span>
        </div>
        {analytics && (
          <div className="stat-card">
            <span className="stat-value">{analytics.totalCompletions}</span>
            <span className="stat-label">Total completions</span>
          </div>
        )}
        {completionRate !== null && (
          <div className="stat-card">
            <span className="stat-value">{completionRate}%</span>
            <span className="stat-label">Completion rate</span>
          </div>
        )}
      </div>

      {nextMilestone && (
        <div className="milestone-bar">
          <span>🎯 {nextMilestone - habit.currentStreak} days to {nextMilestone}-day badge</span>
          <div className="milestone-bar__track">
            <div className="milestone-bar__fill" style={{ width: `${(habit.currentStreak / nextMilestone) * 100}%` }} />
          </div>
        </div>
      )}

      <div className="milestone-badges">
        {MILESTONES.map((m) => (
          <div key={m} className={`milestone-badge ${habit.longestStreak >= m ? 'milestone-badge--earned' : ''}`} title={`${m}-day streak`}>
            🏅 {m}d
          </div>
        ))}
      </div>

      <section className="detail-section">
        <h3 className="detail-section__title">Contribution history</h3>
        <ContributionCalendar completions={history} />
      </section>

      {analytics && analytics.weeklyStats && analytics.weeklyStats.length > 0 && (
        <section className="detail-section">
          <h3 className="detail-section__title">Weekly completions</h3>
          <ResponsiveContainer width="100%" height={160}>
            <BarChart data={analytics.weeklyStats.slice(-16)} margin={{ top: 4, right: 0, left: -24, bottom: 0 }}>
              <XAxis dataKey="weekLabel" tick={{ fontSize: 10 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 10 }} />
              <Tooltip />
              <Bar dataKey="completions" fill="#16a34a" radius={[3, 3, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </section>
      )}

      {error && <p className="form-error">{error}</p>}
    </div>
  )
}
