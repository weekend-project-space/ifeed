import { computed, type ComputedRef } from 'vue';
import { useQuery } from '@tanstack/vue-query';
import { useAuthStore } from '@/stores/auth';
import { searchArticles, type SearchResultDto, type SearchType, type Source } from '@/api/search';

export type { SearchResultDto, SearchType, Source } from '@/api/search';

export const SEARCH_PAGE_SIZE = 20;

export const useSearchQuery = (
  query: ComputedRef<string>,
  type: ComputedRef<SearchType>,
  source: ComputedRef<Source | undefined>,
  page: ComputedRef<number>,
  filters: { feedId: ComputedRef<string | null>; tags: ComputedRef<string | null>; category: ComputedRef<string | null> },
) => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useQuery({
    queryKey: computed(() => [
      'search', userId.value, query.value, type.value, source.value ?? null, page.value, SEARCH_PAGE_SIZE,
      filters.feedId.value, filters.tags.value, filters.category.value,
    ] as const),
    queryFn: ({ signal }) => searchArticles(
      query.value,
      type.value,
      source.value,
      Math.max(0, page.value - 1),
      SEARCH_PAGE_SIZE,
      { feedId: filters.feedId.value, tags: filters.tags.value, category: filters.category.value },
      signal,
    ),
    enabled: computed(() => Boolean(query.value)),
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
};
