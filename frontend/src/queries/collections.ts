import { computed, type ComputedRef } from 'vue';
import { keepPreviousData, useQuery } from '@tanstack/vue-query';
import { useAuthStore } from '@/stores/auth';
import { COLLECTIONS_SORT, listCollections } from '@/api/collections';

export const COLLECTIONS_PAGE_SIZE = 20;
export type { CollectionDto } from '@/api/collections';

export const collectionsQueryKey = (userId: string, page: number) =>
  ['collections', userId, page, COLLECTIONS_PAGE_SIZE, COLLECTIONS_SORT] as const;

export const useCollectionsQuery = (page: ComputedRef<number>) => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useQuery({
    queryKey: computed(() => collectionsQueryKey(userId.value, page.value)),
    queryFn: ({ signal }) => listCollections(
      Math.max(0, page.value - 1),
      COLLECTIONS_PAGE_SIZE,
      signal,
    ),
    enabled: computed(() => authStore.isAuthenticated),
    placeholderData: keepPreviousData,
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
};
