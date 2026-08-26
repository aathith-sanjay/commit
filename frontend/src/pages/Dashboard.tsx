import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getHabits } from '../api/habits'
import HabitCard from '../components/HabitCard'
import SkeletonCard from '../components/SkeletonCard'
import type { Habit } from '../types'
import './Dashboard.css'

const TREE_EMOJI: Record<string, string> = {
  SEED: '🌱', HERB: '🌿', SHRUB: '🪴', SAPLING: '🌳',
  YOUNG_TREE: '🌲', TREE: '🌳', FLOWERING_TREE: '🌸', FRUIT_TREE: '🍎', MATURE_TREE: '🏔️',
}

function isScheduledToday(habit: Habit): boolean {
  if (habit.scheduleType === 'DAILY') return true
  if (habit.scheduleType === 'WEEKLY') return true
  if (habit.scheduleType === 'SPECIFIC_DAYS' && habit.scheduleDays) {
    const DOW = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
    const today = DOW[new Date().getDay()]
    return habit.scheduleDays.split(',').includes(today)
  }
  return true
}

export default function Dashboard() {
  const [habits, setHabits] = useState<Habit[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeCategory, setActiveCategory] = useState<string>('All')
  const [showArchived, setShowArchived] = useState(false)

  function load(includeArchived = false) {
    setLoading(true)
    getHabits(includeArchived)
      .then(setHabits)
      .catch(() => setError('Could not load habits. Is the backend running?'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load(showArchived) }, [showArchived])

  function handleHabitUpdated(updated: Habit) {
    setHabits((prev) => prev.map((h) => (h.id === updated.id ? updated : h)))
  }

  const categories = useMemo(() => {
    const cats = new Set(habits.map((h) => h.category).filter(Boolean) as string[])
    return ['All', ...Array.from(cats).sort()]
  }, [habits])

  const filtered = useMemo(() => {
    if (activeCategory === 'All') return habits
    return habits.filter((h) => h.category === activeCategory)
  }, [habits, activeCategory])

  const scheduledToday = filtered.filter(isScheduledToday)
  const notScheduledToday = filtered.filter((h) => !isScheduledToday(h))
  const doneToday = scheduledToday.filter((h) => h.todayCompleted).length

  const today = new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' })

  return (
    <div className="dashboard">
      <header className="dashboard__header">
        <div>
          <h1 className="dashboard__title">commit.</h1>
          <p className="dashboard__subtitle">{today}</p>
        </div>
        <Link to="/habits/new" className="btn btn--primary">+ New</Link>
      </header>

      {!loading && habits.length > 0 && (
        <div className="dashboard__progress-section">
          <div className="dashboard__progress-bar-wrap">
            <div className="dashboard__progress-bar" style={{ width: `${scheduledToday.length ? (doneToday / scheduledToday.length) * 100 : 0}%` }} />
          </div>
          <span className="dashboard__progress-label">{doneToday} / {scheduledToday.length} done today</span>
        </div>
      )}

      {/* Category filter chips */}
      {categories.length > 1 && (
        <div className="filter-chips">
          {categories.map((cat) => (
            <button key={cat} className={`filter-chip ${activeCategory === cat ? 'filter-chip--active' : ''}`} onClick={() => setActiveCategory(cat)}>
              {cat}
            </button>
          ))}
        </div>
      )}

      {loading && (
        <div className="habit-list">
          {[1, 2, 3].map((i) => <SkeletonCard key={i} />)}
        </div>
      )}

      {error && (
        <div className="error-state">
          <p>{error}</p>
          <button className="btn btn--primary" onClick={() => load(showArchived)}>Retry</button>
        </div>
      )}

      {!loading && !error && habits.length === 0 && (
        <div className="dashboard__empty">
          <p>No habits yet.</p>
          <Link to="/habits/new" className="btn btn--primary">Create your first habit</Link>
        </div>
      )}

      {/* Scheduled today */}
      {scheduledToday.length > 0 && (
        <section className="habit-section">
          <div className="habit-list">
            {scheduledToday.map((habit) => (
              <HabitCard key={habit.id} habit={habit} onUpdated={handleHabitUpdated} scheduledToday={true} />
            ))}
          </div>
        </section>
      )}

      {/* Not scheduled today (dimmed) */}
      {notScheduledToday.length > 0 && (
        <section className="habit-section habit-section--muted">
          <h3 className="habit-section__title">Other habits</h3>
          <div className="habit-list">
            {notScheduledToday.map((habit) => (
              <HabitCard key={habit.id} habit={habit} onUpdated={handleHabitUpdated} scheduledToday={false} />
            ))}
          </div>
        </section>
      )}

      {/* My Garden */}
      {!loading && habits.length > 0 && (
        <section className="garden">
          <h3 className="garden__title">My Garden</h3>
          <div className="garden__grid">
            {habits.map((h) => (
              <Link key={h.id} to={`/habits/${h.id}`} className={`garden__tree ${h.treeState === 'DEAD' ? 'garden__tree--dead' : ''}`} title={`${h.name} — ${h.currentStreak} day streak`}>
                <span className="garden__tree-emoji">{TREE_EMOJI[h.treeStage] ?? '🌱'}</span>
                <span className="garden__tree-name">{h.name}</span>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* Archive toggle */}
      <div className="archive-toggle">
        <button className="btn-link" onClick={() => setShowArchived((v) => !v)}>
          {showArchived ? 'Hide archived habits' : 'Show archived habits'}
        </button>
      </div>
    </div>
  )
}
