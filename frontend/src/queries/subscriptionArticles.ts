import { computed, type ComputedRef } from 'vue';
import { keepPreviousData, useQuery } from '@tanstack/vue-query';
import { useAuthStore } from '@/stores/auth';
import { listArticles, getSubscriptionInsights } from '@/api/articles';

export const SUBSCRIPTION_ARTICLES_PAGE_SIZE = 20;

export type { SubscriptionInsights } from '@/api/articles';

export const useSubscriptionArticlesQuery = (
  page: ComputedRef<number>,
  tag: ComputedRef<string | null>,
  category: ComputedRef<string | null>,
  feedId: ComputedRef<string | null>,
) => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useQuery({
    queryKey: computed(() => [
      'subscriptionArticles', userId.value, tag.value, category.value, feedId.value,
      page.value, SUBSCRIPTION_ARTICLES_PAGE_SIZE, 'publishedAt,desc',
    ] as const),
    queryFn: ({ signal }) => listArticles({
      page: Math.max(0, page.value - 1),
      size: SUBSCRIPTION_ARTICLES_PAGE_SIZE,
      sort: 'publishedAt,desc',
      tags: tag.value,
      category: category.value,
      feedId: feedId.value,
      signal,
    }),
    placeholderData: keepPreviousData,
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
