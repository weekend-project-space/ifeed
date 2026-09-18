import { computed, type ComputedRef } from 'vue';
import { useInfiniteQuery, useQuery } from '@tanstack/vue-query';
import { useAuthStore } from '@/stores/auth';
import { listArticles, getSubscriptionInsights } from '@/api/articles';

export const SUBSCRIPTION_ARTICLES_PAGE_SIZE = 20;

export type { SubscriptionInsights } from '@/api/articles';

export const useInfiniteSubscriptionArticlesQuery = (
  tag: ComputedRef<string | null>,
  category: ComputedRef<string | null>,
  feedId: ComputedRef<string | null>,
) => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useInfiniteQuery({
    queryKey: computed(() => [
      'subscriptionArticles', userId.value, tag.value, category.value, feedId.value,
      SUBSCRIPTION_ARTICLES_PAGE_SIZE, 'publishedAt,desc',
    ] as const),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => listArticles({
      page: pageParam,
      size: SUBSCRIPTION_ARTICLES_PAGE_SIZE,
      sort: 'publishedAt,desc',
      tags: tag.value,
      category: category.value,
      feedId: feedId.value,
      signal,
    }),
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
