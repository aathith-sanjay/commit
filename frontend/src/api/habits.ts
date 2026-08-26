import type {
  AnalyticsResponse,
  CreateHabitPayload,
  Habit,
  HabitCompletion,
  StreakResponse,
  TreeResponse,
  UpdateHabitPayload,
} from '../types'
import client from './client'

export const getHabits = (includeArchived = false): Promise<Habit[]> =>
  client.get<Habit[]>('/habits', { params: { includeArchived } }).then((r) => r.data)

export const createHabit = (payload: CreateHabitPayload): Promise<Habit> =>
  client.post<Habit>('/habits', payload).then((r) => r.data)

export const getHabit = (id: number): Promise<Habit> =>
  client.get<Habit>(`/habits/${id}`).then((r) => r.data)

export const updateHabit = (id: number, payload: UpdateHabitPayload): Promise<Habit> =>
  client.put<Habit>(`/habits/${id}`, payload).then((r) => r.data)

export const deleteHabit = (id: number): Promise<void> =>
  client.delete(`/habits/${id}`).then(() => undefined)

export const archiveHabit = (id: number): Promise<Habit> =>
  client.patch<Habit>(`/habits/${id}/archive`).then((r) => r.data)

export const restoreHabit = (id: number): Promise<Habit> =>
  client.patch<Habit>(`/habits/${id}/restore`).then((r) => r.data)

export const completeHabit = (id: number, completionDate: string): Promise<HabitCompletion> =>
  client.post<HabitCompletion>(`/habits/${id}/completions`, { completionDate }).then((r) => r.data)

export const undoCompletion = (id: number, completionDate: string): Promise<void> =>
  client.delete(`/habits/${id}/completions/${completionDate}`).then(() => undefined)

export const getHistory = (id: number): Promise<HabitCompletion[]> =>
  client.get<HabitCompletion[]>(`/habits/${id}/history`).then((r) => r.data)

export const getStreak = (id: number): Promise<StreakResponse> =>
  client.get<StreakResponse>(`/habits/${id}/streak`).then((r) => r.data)

export const getTree = (id: number): Promise<TreeResponse> =>
  client.get<TreeResponse>(`/habits/${id}/tree`).then((r) => r.data)

export const getAnalytics = (id: number): Promise<AnalyticsResponse> =>
  client.get<AnalyticsResponse>(`/habits/${id}/analytics`).then((r) => r.data)
