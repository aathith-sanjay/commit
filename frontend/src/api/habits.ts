import type {
  CreateHabitPayload,
  Habit,
  HabitCompletion,
  StreakResponse,
  TreeResponse,
  UpdateHabitPayload,
} from '../types'
import client from './client'

export const getHabits = (): Promise<Habit[]> =>
  client.get<Habit[]>('/habits').then((r) => r.data)

export const createHabit = (payload: CreateHabitPayload): Promise<Habit> =>
  client.post<Habit>('/habits', payload).then((r) => r.data)

export const getHabit = (id: number): Promise<Habit> =>
  client.get<Habit>(`/habits/${id}`).then((r) => r.data)

export const updateHabit = (id: number, payload: UpdateHabitPayload): Promise<Habit> =>
  client.put<Habit>(`/habits/${id}`, payload).then((r) => r.data)

export const deleteHabit = (id: number): Promise<void> =>
  client.delete(`/habits/${id}`).then(() => undefined)

export const completeHabit = (id: number, completionDate: string): Promise<HabitCompletion> =>
  client.post<HabitCompletion>(`/habits/${id}/completions`, { completionDate }).then((r) => r.data)

export const getHistory = (id: number): Promise<HabitCompletion[]> =>
  client.get<HabitCompletion[]>(`/habits/${id}/history`).then((r) => r.data)

export const getStreak = (id: number): Promise<StreakResponse> =>
  client.get<StreakResponse>(`/habits/${id}/streak`).then((r) => r.data)

export const getTree = (id: number): Promise<TreeResponse> =>
  client.get<TreeResponse>(`/habits/${id}/tree`).then((r) => r.data)
