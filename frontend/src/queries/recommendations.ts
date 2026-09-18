import {computed} from 'vue';
import {useInfiniteQuery} from '@tanstack/vue-query';
import {useAuthStore} from '@/stores/auth';
import {listRecommendations} from '@/api/recommendations';

const RECOMMENDATION_PAGE_SIZE = 20;

export const useInfiniteRecommendationsQuery = () => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useInfiniteQuery({
    queryKey: computed(() => [
      'recommendations',
      userId.value,
      RECOMMENDATION_PAGE_SIZE,
    ] as const),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => listRecommendations(
      pageParam,
      RECOMMENDATION_PAGE_SIZE,
      signal,
    ),
    getNextPageParam: (lastPage) => {
      const nextPage = lastPage.number + 1;
      return nextPage < lastPage.totalPages ? nextPage : undefined;
    },
    maxPages: 10,
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
};

export { RECOMMENDATION_PAGE_SIZE };
