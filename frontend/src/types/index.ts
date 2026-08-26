export type TreeState = 'ALIVE' | 'DEAD' | 'RECOVERING'

export type TreeStage =
  | 'SEED'
  | 'HERB'
  | 'SHRUB'
  | 'SAPLING'
  | 'YOUNG_TREE'
  | 'TREE'
  | 'FLOWERING_TREE'
  | 'FRUIT_TREE'
  | 'MATURE_TREE'

export type ScheduleType = 'DAILY' | 'WEEKLY' | 'SPECIFIC_DAYS'

export const DAYS_OF_WEEK = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'] as const
export type DayOfWeek = (typeof DAYS_OF_WEEK)[number]

export interface Habit {
  id: number
  name: string
  description?: string
  category?: string
  scheduleType: ScheduleType
  scheduleDays?: string
  active: boolean
  startDate: string
  endDate?: string
  timezone: string
  currentStreak: number
  longestStreak: number
  treeState: TreeState
  treeStage: TreeStage
  todayCompleted: boolean
  createdAt: string
  updatedAt: string
}

export interface HabitCompletion {
  id: number
  habitId: number
  completionDate: string
  createdAt: string
}

export interface StreakResponse {
  currentStreak: number
  longestStreak: number
  todayCompleted: boolean
}

export interface TreeResponse {
  treeState: TreeState
  treeStage: TreeStage
  currentStreak: number
  longestStreak: number
}

export interface WeekStat {
  weekLabel: string
  scheduled: number
  completed: number
}

export interface MonthStat {
  monthLabel: string
  scheduled: number
  completed: number
}

export interface AnalyticsResponse {
  totalCompletions: number
  completionRate: number
  currentStreak: number
  longestStreak: number
  consistencyScore: number
  weeklyStats: WeekStat[]
  monthlyStats: MonthStat[]
}

export interface CreateHabitPayload {
  name: string
  startDate: string
  description?: string
  category?: string
  scheduleType?: ScheduleType
  scheduleDays?: string
  endDate?: string
  timezone?: string
}

export interface UpdateHabitPayload {
  name?: string
  description?: string
  category?: string
  active?: boolean
  scheduleType?: ScheduleType
  scheduleDays?: string
  endDate?: string
  timezone?: string
}
