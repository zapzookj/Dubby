import { request } from '@/api/client';
import type { CursorPage, DiaryEntry } from '@/api/types';

export function approveDiaryCandidate(candidateId: number): Promise<DiaryEntry> {
  return request<DiaryEntry>(`/diary/candidates/${candidateId}/approve`, { method: 'POST' });
}

export function rejectDiaryCandidate(candidateId: number) {
  return request<{ rejected: boolean; derbyMessage: string }>(
    `/diary/candidates/${candidateId}/reject`,
    { method: 'POST' },
  );
}

export function getDiaryEntries(cursor?: number | null): Promise<CursorPage<DiaryEntry>> {
  const q = cursor ? `?cursor=${cursor}` : '';
  return request<CursorPage<DiaryEntry>>(`/diary/entries${q}`);
}

export function deleteDiaryEntry(entryId: number) {
  return request<{ derbyMessage: string }>(`/diary/entries/${entryId}`, { method: 'DELETE' });
}

export function shareDiaryEntry(entryId: number) {
  return request<{ shareText: string }>(`/diary/entries/${entryId}/share`, { method: 'POST' });
}
