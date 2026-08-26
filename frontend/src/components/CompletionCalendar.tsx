import type { HabitCompletion } from '../types'
import './CompletionCalendar.css'

interface Props {
  completions: HabitCompletion[]
}

export default function CompletionCalendar({ completions }: Props) {
  const completedSet = new Set(completions.map((c) => c.completionDate))

  // Show last 42 days (6 weeks)
  const today = new Date()
  const days: string[] = []
  for (let i = 41; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    days.push(d.toISOString().split('T')[0])
  }

  const weekLabels = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa']

  return (
    <div className="calendar">
      <div className="calendar__week-labels">
        {weekLabels.map((l) => (
          <span key={l} className="calendar__week-label">{l}</span>
        ))}
      </div>
      <div className="calendar__grid">
        {days.map((day) => (
          <div
            key={day}
            className={`calendar__cell ${completedSet.has(day) ? 'calendar__cell--done' : ''}`}
            title={day}
          />
        ))}
      </div>
    </div>
  )
}
