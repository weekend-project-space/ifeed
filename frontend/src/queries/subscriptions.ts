import { useMutation } from '@tanstack/vue-query';
import { addSubscription, removeSubscription, confirmOpmlImport, previewOpmlImport } from '@/api/subscriptions';
import { useSubscriptionsStore } from '@/stores/subscriptions';
import { invalidateSubscriptionCaches } from './subscriptionCache';

const useSubscriptionSuccess = () => {
  const store = useSubscriptionsStore();
  return async () => {
    // Keep the existing sidebar consumers in sync during the Query migration.
    await Promise.allSettled([invalidateSubscriptionCaches(), store.fetchSubscriptions()]);
  };
};

export const useSubscriptionMutation = () => useMutation({
  mutationFn: (input: { feedId?: string; feedUrl: string; subscribed: boolean }) =>
    input.subscribed
      ? removeSubscription(input.feedId!)
      : addSubscription(input.feedUrl, input.feedId ?? ''),
  onSuccess: useSubscriptionSuccess(),
});

export const useOpmlPreviewMutation = () => useMutation({ mutationFn: previewOpmlImport });

export const useOpmlImportMutation = () => useMutation({
  mutationFn: confirmOpmlImport,
  onSuccess: useSubscriptionSuccess(),
});
