import {toValue, type MaybeRefOrGetter} from 'vue';
import {useQueryClient, type InfiniteData, type QueryKey} from '@tanstack/vue-query';
import {useRefresh} from './refresh';

type InfiniteQueryRefreshOptions<Page, PageParam> = {
  queryKey: MaybeRefOrGetter<QueryKey>;
  initialPageParam: PageParam;
  fetchFirstPage: (signal: AbortSignal) => Promise<Page>;
  enabled?: MaybeRefOrGetter<boolean>;
};

export const useInfiniteQueryRefresh = <Page, PageParam>({
  queryKey, initialPageParam, fetchFirstPage, enabled = true,
}: InfiniteQueryRefreshOptions<Page, PageParam>) => {
  const client = useQueryClient();
  return useRefresh({
    enabled,
    resetOn: queryKey,
    onRefresh: async signal => {
      const key = toValue(queryKey);
      await client.cancelQueries({queryKey: key, exact: true});
      if (signal.aborted) return;
      const page = await fetchFirstPage(signal);
      if (signal.aborted) return;
      // A reconnect may have started another fetch while the first page was loading.
      await client.cancelQueries({queryKey: key, exact: true});
      if (signal.aborted) return;
      client.setQueryData<InfiniteData<Page, PageParam>>(key, {pages: [page], pageParams: [initialPageParam]});
    },
  });
};
