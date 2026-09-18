import { defineStore } from 'pinia';
import { recordFeedRead as recordFeedReadRequest } from '@/api/readfeed';

export const useReadFeedStore = defineStore('readfeed', () => {
  const recordFeedRead = async (feedId: string) => {
    await recordFeedReadRequest(feedId);
  };

  return {
    recordFeedRead
  };
});
