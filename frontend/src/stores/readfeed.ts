import { defineStore } from 'pinia';
import { request } from '../api/client';

export const useReadFeedStore = defineStore('readfeed', () => {
  const recordFeedRead = async (feedId: string) => {
    await request(`/api/user/readfeed/${encodeURIComponent(feedId)}`, { method: 'POST' });
  };

  return {
    recordFeedRead
  };
});

