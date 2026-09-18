import { computed, type ComputedRef } from 'vue';
import { keepPreviousData, useQuery } from '@tanstack/vue-query';
import { useAuthStore } from '@/stores/auth';
import { HISTORY_SORT, listHistory } from '@/api/history';

export const HISTORY_PAGE_SIZE = 20;
export type { HistoryEntryDto } from '@/api/history';

export const historyQueryKey = (userId: string, page: number) =>
  ['history', userId, page, HISTORY_PAGE_SIZE, HISTORY_SORT] as const;

export const useHistoryQuery = (page: ComputedRef<number>) => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useQuery({
    queryKey: computed(() => historyQueryKey(userId.value, page.value)),
    queryFn: ({ signal }) => listHistory(
      Math.max(0, page.value - 1),
      HISTORY_PAGE_SIZE,
      signal,
    ),
    enabled: computed(() => authStore.isAuthenticated),
    placeholderData: keepPreviousData,
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
};
