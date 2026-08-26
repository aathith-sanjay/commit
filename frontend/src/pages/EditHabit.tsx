import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getHabit, updateHabit } from '../api/habits'
import type { DayOfWeek, Habit, ScheduleType } from '../types'
import { DAYS_OF_WEEK } from '../types'
import './HabitForm.css'

const CATEGORIES = ['Health', 'Fitness', 'Learning', 'Mindfulness', 'Productivity', 'Social', 'Other']

export default function EditHabit() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const habitId = Number(id)

  const [habit, setHabit] = useState<Habit | null>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [category, setCategory] = useState('')
  const [scheduleType, setScheduleType] = useState<ScheduleType>('DAILY')
  const [scheduleDays, setScheduleDays] = useState<DayOfWeek[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getHabit(habitId).then((h) => {
      setHabit(h)
      setName(h.name)
      setDescription(h.description ?? '')
      setCategory(h.category ?? '')
      setScheduleType(h.scheduleType)
      setScheduleDays(h.scheduleDays ? (h.scheduleDays.split(',') as DayOfWeek[]) : [])
    }).catch(() => setError('Could not load habit.')).finally(() => setLoading(false))
  }, [habitId])

  function toggleDay(day: DayOfWeek) {
    setScheduleDays((prev) => prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day])
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim()) return
    if (scheduleType === 'SPECIFIC_DAYS' && scheduleDays.length === 0) {
      setError('Please select at least one day.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await updateHabit(habitId, {
        name: name.trim(),
        description: description.trim() || undefined,
        category: category || undefined,
        scheduleType,
        scheduleDays: scheduleType === 'SPECIFIC_DAYS' ? scheduleDays.join(',') : undefined,
      })
      navigate(`/habits/${habitId}`)
    } catch (err: unknown) {
      setError(extractMessage(err) ?? 'Failed to save habit')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <p className="state-msg">Loading…</p>
  if (error && !habit) return <p className="state-msg state-msg--error">{error}</p>

  return (
    <div className="habit-form-page">
      <header className="habit-form-page__header">
        <Link to={`/habits/${habitId}`} className="back-link">← Back</Link>
        <h1>Edit Habit</h1>
      </header>
      <form className="habit-form" onSubmit={handleSubmit}>
        <label className="form-field">
          <span className="form-label">Name <span className="required">*</span></span>
          <input className="form-input" type="text" maxLength={150} value={name} onChange={(e) => setName(e.target.value)} autoFocus required />
        </label>

        <label className="form-field">
          <span className="form-label">Description</span>
          <textarea className="form-input form-textarea" value={description} onChange={(e) => setDescription(e.target.value)} rows={2} />
        </label>

        <label className="form-field">
          <span className="form-label">Category</span>
          <select className="form-input" value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">No category</option>
            {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </label>

        <div className="form-field">
          <span className="form-label">Schedule</span>
          <div className="schedule-tabs">
            {(['DAILY', 'SPECIFIC_DAYS', 'WEEKLY'] as ScheduleType[]).map((t) => (
              <button key={t} type="button" className={`schedule-tab ${scheduleType === t ? 'schedule-tab--active' : ''}`} onClick={() => setScheduleType(t)}>
                {t === 'DAILY' ? 'Every day' : t === 'WEEKLY' ? 'Once a week' : 'Specific days'}
              </button>
            ))}
          </div>
          {scheduleType === 'SPECIFIC_DAYS' && (
            <div className="day-picker">
              {DAYS_OF_WEEK.map((day) => (
                <button key={day} type="button" className={`day-btn ${scheduleDays.includes(day) ? 'day-btn--active' : ''}`} onClick={() => toggleDay(day)}>
                  {day.slice(0, 2)}
                </button>
              ))}
            </div>
          )}
        </div>

        {error && <p className="form-error">{error}</p>}
        <button type="submit" className="btn btn--primary btn--full" disabled={saving || !name.trim()}>
          {saving ? 'Saving…' : 'Save changes'}
        </button>
      </form>
    </div>
  )
}

function extractMessage(err: unknown): string | undefined {
  if (err && typeof err === 'object' && 'response' in err) {
    return (err as { response?: { data?: { message?: string } } }).response?.data?.message
  }
}
