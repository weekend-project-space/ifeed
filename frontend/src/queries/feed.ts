import { computed, type ComputedRef } from 'vue';
import { useInfiniteQuery, useQuery } from '@tanstack/vue-query';
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

export const useInfiniteFeedArticlesQuery = (
  feedId: ComputedRef<string | null>,
  tag: ComputedRef<string | null>,
) => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useInfiniteQuery({
    queryKey: computed(() => [
      'feedArticles', userId.value, feedId.value, tag.value, FEED_ARTICLES_PAGE_SIZE, 'publishedAt,desc',
    ] as const),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => listFeedArticles(
      feedId.value!,
      pageParam,
      FEED_ARTICLES_PAGE_SIZE,
      tag.value,
      signal,
    ),
    enabled: computed(() => Boolean(feedId.value)),
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

export type { FeedDetail } from '@/api/feed';
