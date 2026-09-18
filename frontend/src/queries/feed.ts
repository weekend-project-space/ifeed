import { computed, type ComputedRef } from 'vue';
import { keepPreviousData, useQuery } from '@tanstack/vue-query';
import { useAuthStore } from '@/stores/auth';
import { getFeed, listFeedArticles } from '@/api/feed';

export const FEED_ARTICLES_PAGE_SIZE = 20;

export const useFeedQuery = (feedId: ComputedRef<string | null>) => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useQuery({
    queryKey: computed(() => ['feed', userId.value, feedId.value] as const),
    queryFn: ({ signal }) => getFeed(feedId.value!, signal),
    enabled: computed(() => Boolean(feedId.value)),
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
};

export const useFeedArticlesQuery = (
  feedId: ComputedRef<string | null>,
  page: ComputedRef<number>,
  tag: ComputedRef<string | null>,
) => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useQuery({
    queryKey: computed(() => [
      'feedArticles', userId.value, feedId.value, tag.value, page.value, FEED_ARTICLES_PAGE_SIZE, 'publishedAt,desc',
    ] as const),
    queryFn: ({ signal }) => listFeedArticles(
      feedId.value!,
      Math.max(0, page.value - 1),
      FEED_ARTICLES_PAGE_SIZE,
      tag.value,
      signal,
    ),
    enabled: computed(() => Boolean(feedId.value)),
    placeholderData: keepPreviousData,
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
};

export type { FeedDetail } from '@/api/feed';
