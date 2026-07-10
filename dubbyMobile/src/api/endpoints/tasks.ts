import { request } from '@/api/client';
import type { CursorPage, Reaction, TaskReactionResponse, TasksTodayResponse } from '@/api/types';

export function getTasksToday(): Promise<TasksTodayResponse> {
  return request<TasksTodayResponse>('/tasks/today');
}

export function reactToTask(assignmentId: number, reaction: Reaction): Promise<TaskReactionResponse> {
  return request<TaskReactionResponse>(`/tasks/${assignmentId}/reaction`, {
    method: 'POST',
    body: JSON.stringify({ reaction }),
  });
}

export function saveTask(assignmentId: number, saved: boolean) {
  return request<{ assignmentId: number; saved: boolean; derbyMessage: string }>(
    `/tasks/${assignmentId}/save`,
    { method: 'POST', body: JSON.stringify({ saved }) },
  );
}

export function shareTask(assignmentId: number) {
  return request<{ shareText: string }>(`/tasks/${assignmentId}/share`, { method: 'POST' });
}

export interface SavedTaskItem {
  assignmentId: number;
  templateCode: string;
  title: string;
  conclusion: string;
  note: string | null;
  assignedDate: string;
}

export function getSavedTasks(cursor?: number | null): Promise<CursorPage<SavedTaskItem>> {
  const q = cursor ? `?cursor=${cursor}` : '';
  return request<CursorPage<SavedTaskItem>>(`/tasks/saved${q}`);
}
