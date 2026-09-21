import { useMutation } from '@tanstack/vue-query';
import { addSubscription, removeSubscription, confirmOpmlImport, previewOpmlImport } from '@/api/subscriptions';
import { useSubscriptionsStore } from '@/stores/subscriptions';
import { invalidateSubscriptionCaches } from './subscriptionCache';

const useSubscriptionSync = () => {
  const store = useSubscriptionsStore();
  return async (removedFeedIds: string[] = []) => {
    if (removedFeedIds.length) {
      const removedIds = new Set(removedFeedIds);
      store.items = store.items.filter((item) => !removedIds.has(item.feedId));
    }
    // Keep the existing sidebar consumers in sync during the Query migration.
    await Promise.allSettled([invalidateSubscriptionCaches(), store.fetchSubscriptions()]);
  };
};

export const useSubscriptionMutation = () => {
  const synchronize = useSubscriptionSync();
  return useMutation({
    mutationFn: (input: { feedId?: string; feedUrl: string; subscribed: boolean }) =>
      input.subscribed
        ? removeSubscription(input.feedId!)
        : addSubscription(input.feedUrl, input.feedId ?? ''),
    onSuccess: (_, input) => synchronize(input.subscribed && input.feedId ? [input.feedId] : []),
  });
};

export const useBatchRemoveSubscriptionsMutation = () => {
  const synchronize = useSubscriptionSync();

  return useMutation({
    mutationFn: async (feedIds: string[]) => {
      const removedFeedIds: string[] = [];
      const failedFeedIds: string[] = [];
      for (const feedId of new Set(feedIds)) {
        try {
          await removeSubscription(feedId);
          removedFeedIds.push(feedId);
        } catch {
          failedFeedIds.push(feedId);
        }
      }
      return {removedFeedIds, failedFeedIds};
    },
    onSuccess: async ({removedFeedIds}) => {
      if (!removedFeedIds.length) return;
      await synchronize(removedFeedIds);
    },
  });
};

export const useOpmlPreviewMutation = () => useMutation({ mutationFn: previewOpmlImport });

export const useOpmlImportMutation = () => {
  const synchronize = useSubscriptionSync();
  return useMutation({
    mutationFn: confirmOpmlImport,
    onSuccess: () => synchronize(),
  });
};
