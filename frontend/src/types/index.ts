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

export interface Habit {
  id: number
  name: string
  scheduleType: string
  active: boolean
  startDate: string
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

export interface CreateHabitPayload {
  name: string
  startDate: string
}

export interface UpdateHabitPayload {
  name?: string
  active?: boolean
}
