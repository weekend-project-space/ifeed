import { post } from './client';

export const recordFeedRead = (feedId: string) =>
  post(`/api/user/readfeed/${encodeURIComponent(feedId)}`);
