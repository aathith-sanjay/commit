import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { createHabit } from '../api/habits'
import './CreateHabit.css'

export default function CreateHabit() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [startDate, setStartDate] = useState(new Date().toISOString().split('T')[0])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim()) return
    setLoading(true)
    setError(null)
    try {
      await createHabit({ name: name.trim(), startDate })
      navigate('/')
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message ??
            'Failed to create habit'
          : 'Failed to create habit'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="create-habit">
      <header className="create-habit__header">
        <Link to="/" className="back-link">← Back</Link>
        <h1>New Habit</h1>
      </header>

      <form className="create-habit__form" onSubmit={handleSubmit}>
        <label className="form-field">
          <span className="form-label">Habit name</span>
          <input
            type="text"
            className="form-input"
            placeholder="e.g. Read, Run, Journal"
            value={name}
            maxLength={150}
            onChange={(e) => setName(e.target.value)}
            autoFocus
            required
          />
        </label>

        <label className="form-field">
          <span className="form-label">Start date</span>
          <input
            type="date"
            className="form-input"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            required
          />
        </label>

        {error && <p className="form-error">{error}</p>}

        <button type="submit" className="btn btn--primary btn--full" disabled={loading || !name.trim()}>
          {loading ? 'Creating…' : 'Create habit'}
        </button>
      </form>
    </div>
  )
}
