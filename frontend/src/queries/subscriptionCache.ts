import { queryClient } from '@/queryClient';
import { resetInfiniteArticleCaches } from './articleCache';

export const invalidateSubscriptionCaches = () => {
  resetInfiniteArticleCaches(queryClient, ['subscriptionArticles']);
  return Promise.all([
    'discoveryFeeds', 'feed', 'subscriptionArticles', 'subscriptionInsights',
  ].map((key) => queryClient.invalidateQueries({ queryKey: [key] })));
};
