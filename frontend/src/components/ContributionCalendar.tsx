import type { HabitCompletion } from '../types'
import './ContributionCalendar.css'

interface Props {
  completions: HabitCompletion[]
  scheduledDates?: Set<string>
}

export default function ContributionCalendar({ completions, scheduledDates }: Props) {
  const completedSet = new Set(completions.map((c) => c.completionDate))

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  // Build 52 weeks × 7 days grid, aligned to Sunday
  const endDate = new Date(today)
  const startDayOfWeek = endDate.getDay() // 0=Sun
  const totalDays = 52 * 7 + startDayOfWeek
  const startDate = new Date(endDate)
  startDate.setDate(endDate.getDate() - totalDays + 1)

  const weeks: string[][] = []
  let week: string[] = []

  for (let d = new Date(startDate); d <= endDate; d.setDate(d.getDate() + 1)) {
    week.push(d.toISOString().split('T')[0])
    if (week.length === 7) {
      weeks.push(week)
      week = []
    }
  }
  if (week.length > 0) weeks.push(week)

  const MONTH_LABELS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

  // Month labels: find the first week that starts a new month
  const monthPositions: { label: string; col: number }[] = []
  let lastMonth = -1
  weeks.forEach((w, i) => {
    const m = new Date(w[0]).getMonth()
    if (m !== lastMonth) {
      monthPositions.push({ label: MONTH_LABELS[m], col: i })
      lastMonth = m
    }
  })

  function cellClass(dateStr: string) {
    const isCompleted = completedSet.has(dateStr)
    const isScheduled = scheduledDates ? scheduledDates.has(dateStr) : true
    const isFuture = dateStr > today.toISOString().split('T')[0]
    if (isFuture) return 'cal-cell cal-cell--future'
    if (!isScheduled) return 'cal-cell cal-cell--unscheduled'
    if (isCompleted) return 'cal-cell cal-cell--done'
    return 'cal-cell cal-cell--missed'
  }

  return (
    <div className="contrib-calendar">
      <div className="contrib-calendar__months">
        {monthPositions.map(({ label, col }) => (
          <span key={`${label}-${col}`} className="contrib-month-label" style={{ gridColumn: col + 1 }}>
            {label}
          </span>
        ))}
      </div>
      <div className="contrib-calendar__grid" style={{ gridTemplateColumns: `repeat(${weeks.length}, 12px)` }}>
        {weeks.map((week, wi) =>
          week.map((dateStr, di) => (
            <div key={dateStr} className={cellClass(dateStr)} title={dateStr} style={{ gridColumn: wi + 1, gridRow: di + 1 }} />
          ))
        )}
      </div>
    </div>
  )
}
