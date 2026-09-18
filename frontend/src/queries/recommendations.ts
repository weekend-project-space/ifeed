import { computed, type ComputedRef } from 'vue';
import { keepPreviousData, useQuery } from '@tanstack/vue-query';
import { useAuthStore } from '@/stores/auth';
import { listRecommendations } from '@/api/recommendations';

const RECOMMENDATION_PAGE_SIZE = 60;

export const useRecommendationsQuery = (page: ComputedRef<number>) => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useQuery({
    queryKey: computed(() => [
      'recommendations',
      userId.value,
      page.value,
      RECOMMENDATION_PAGE_SIZE,
    ] as const),
    queryFn: ({ signal }) => listRecommendations(
      Math.max(0, page.value - 1),
      RECOMMENDATION_PAGE_SIZE,
      signal,
    ),
    placeholderData: keepPreviousData,
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
};

export { RECOMMENDATION_PAGE_SIZE };
