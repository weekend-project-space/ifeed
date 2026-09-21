import { computed, toValue, type MaybeRefOrGetter } from 'vue';
import { useInfiniteQuery, type InfiniteData, type QueryClient } from '@tanstack/vue-query';
import { useAuthStore } from '@/stores/auth';
import type { PageResponse } from '@/types/api';

const LIBRARY_PAGE_SIZE = 20;
type LibraryItem = { articleId: string };

export const libraryQueryKey = (resource: string, userId: string, sort: string, filter?: string) =>
  [resource, userId, 'infinite', LIBRARY_PAGE_SIZE, sort, filter ?? 'all'] as const;

export const useInfiniteLibraryQuery = <Item extends LibraryItem>(
  resource: string,
  sort: string,
  fetchPage: (page: number, size: number, signal?: AbortSignal, filter?: string) => Promise<PageResponse<Item>>,
  filter?: MaybeRefOrGetter<string | undefined>,
) => {
  const auth = useAuthStore();
  const query = useInfiniteQuery({
    queryKey: computed(() => libraryQueryKey(resource, auth.user?.userId ?? 'anonymous', sort, toValue(filter))),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => fetchPage(pageParam, LIBRARY_PAGE_SIZE, signal, toValue(filter)),
    getNextPageParam: lastPage => lastPage.number + 1 < lastPage.totalPages ? lastPage.number + 1 : undefined,
    enabled: computed(() => auth.isAuthenticated),
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
  const items = computed(() => {
    const seen = new Set<string>();
    return (query.data.value?.pages.flatMap(page => page.content) ?? []).filter(item => {
      if (seen.has(item.articleId)) return false;
      seen.add(item.articleId);
      return true;
    });
  });
  return {
    ...query,
    items,
    total: computed(() => query.data.value?.pages[0]?.totalElements ?? 0),
    errorMessage: computed(() => query.isFetchNextPageError.value ? '' : query.error.value?.message ?? ''),
  };
};

export const removeLibraryArticle = async (client: QueryClient, queryKey: readonly string[], articleId: string) => {
  await client.cancelQueries({ queryKey });
  client.setQueriesData<InfiniteData<PageResponse<LibraryItem>>>({ queryKey }, previous => {
    if (!previous || !previous.pages.some(page => page.content.some(item => item.articleId === articleId))) return previous;
    return {
      ...previous,
      pages: previous.pages.map(page => ({
        ...page,
        content: page.content.filter(item => item.articleId !== articleId),
        totalElements: Math.max(0, page.totalElements - 1),
        totalPages: Math.ceil(Math.max(0, page.totalElements - 1) / page.size),
      })),
    };
  });
};
