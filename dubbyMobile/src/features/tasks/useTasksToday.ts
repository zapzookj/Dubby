import { useQuery, useQueryClient } from '@tanstack/react-query';

import { getTasksToday } from '@/api/endpoints/tasks';
import { localDateString, queryKeys } from '@/api/queryKeys';
import type { TaskItem, TasksTodayResponse } from '@/api/types';

/** 오늘의 업무 쿼리 — 로컬 날짜가 키에 포함되어 자정 넘김 시 자동 새 캐시 */
export function useTasksToday() {
  return useQuery({
    queryKey: queryKeys.tasksToday(localDateString()),
    queryFn: getTasksToday,
    staleTime: 5 * 60_000,
  });
}

/** 반응/저장 성공 시 캐시 부분 갱신 (refetch 없이) */
export function useUpdateTaskInCache() {
  const queryClient = useQueryClient();
  return (assignmentId: number, patch: Partial<TaskItem>) => {
    queryClient.setQueryData<TasksTodayResponse>(
      queryKeys.tasksToday(localDateString()),
      (prev) =>
        prev && {
          ...prev,
          tasks: prev.tasks.map((t) =>
            t.assignmentId === assignmentId ? { ...t, ...patch } : t,
          ),
        },
    );
  };
}
