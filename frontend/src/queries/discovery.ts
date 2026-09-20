import { computed, type ComputedRef } from 'vue';
import { useInfiniteQuery, useQuery } from '@tanstack/vue-query';
import { getDiscoveryCategories, listDiscoveryFeeds } from '@/api/discovery';
import { useAuthStore } from '@/stores/auth';

export const useDiscoveryCategoriesQuery = () => useQuery({
  queryKey: ['discoveryCategories'],
  queryFn: ({ signal }) => getDiscoveryCategories(signal),
  staleTime: 30 * 60 * 1000,
  gcTime: 60 * 60 * 1000,
});

export const useInfiniteDiscoveryFeedsQuery = (
  query: ComputedRef<string>,
  category: ComputedRef<string>,
  active: ComputedRef<boolean>,
) => {
  const auth = useAuthStore();
  return useInfiniteQuery({
    queryKey: computed(() => [
      'discoveryFeeds', auth.user?.userId ?? 'anonymous', query.value, category.value, 30,
    ] as const),
    enabled: active,
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => listDiscoveryFeeds({
      page: pageParam,
      size: 30,
      query: query.value,
      category: category.value === 'all' ? undefined : category.value,
      signal,
    }),
    getNextPageParam: (lastPage) => {
      const nextPage = lastPage.number + 1;
      return nextPage < lastPage.totalPages ? nextPage : undefined;
    },
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
  });
};
