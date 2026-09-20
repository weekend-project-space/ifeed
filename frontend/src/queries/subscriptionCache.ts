import { queryClient } from '@/queryClient';

export const invalidateSubscriptionCaches = () => Promise.all([
  'discoveryFeeds', 'feed', 'subscriptionArticles', 'subscriptionInsights',
].map((key) => queryClient.invalidateQueries({ queryKey: [key] })));
