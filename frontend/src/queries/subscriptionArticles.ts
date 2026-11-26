import { computed, type ComputedRef } from 'vue';
import { useInfiniteQuery, useQuery } from '@tanstack/vue-query';
import { useAuthStore } from '@/stores/auth';
import { listArticles, getSubscriptionInsights } from '@/api/articles';
import { useInfiniteQueryRefresh } from '@/composables/infiniteQueryRefresh';

export const SUBSCRIPTION_ARTICLES_PAGE_SIZE = 20;

export type { SubscriptionInsights } from '@/api/articles';

export const useInfiniteSubscriptionArticlesQuery = (
  tag: ComputedRef<string | null>,
  category: ComputedRef<string | null>,
  feedId: ComputedRef<string | null>,
) => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  const queryKey = computed(() => [
    'subscriptionArticles', userId.value, tag.value, category.value, feedId.value,
    SUBSCRIPTION_ARTICLES_PAGE_SIZE, 'publishedAt,desc',
  ] as const);
  const fetchPage = (page: number, signal: AbortSignal) => listArticles({
    page, size: SUBSCRIPTION_ARTICLES_PAGE_SIZE, sort: 'publishedAt,desc',
    tags: tag.value, category: category.value, feedId: feedId.value, signal,
  });
  const query = useInfiniteQuery({
    queryKey,
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => fetchPage(pageParam, signal),
    getNextPageParam: (lastPage) => {
      const nextPage = lastPage.number + 1;
      return nextPage < lastPage.totalPages ? nextPage : undefined;
    },
    maxPages: 10,
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
  const refreshState = useInfiniteQueryRefresh({
    queryKey, initialPageParam: 0, fetchFirstPage: signal => fetchPage(0, signal),
  });
  return {...query, ...refreshState};
};

export const useSubscriptionInsightsQuery = () => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useQuery({
    queryKey: computed(() => ['subscriptionInsights', userId.value, 12, null, null] as const),
    queryFn: ({ signal }) => getSubscriptionInsights(12, { signal }),
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
};
