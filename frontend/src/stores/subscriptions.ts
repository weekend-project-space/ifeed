import { ref } from 'vue';
import { defineStore } from 'pinia';
import { request } from '../api/client';

export interface SubscriptionDto {
  feedId: string;
  title?: string;
  url: string;
  siteUrl?: string;
  lastFetched?: string;
  lastUpdated?: string;

  isRead?: boolean;
}

export const useSubscriptionsStore = defineStore('subscriptions', () => {
  const items = ref<SubscriptionDto[]>([]);
  const loading = ref(false);
  const submitting = ref(false);
  const error = ref<string | null>(null);

  const fetchSubscriptions = async () => {
    loading.value = true;
    error.value = null;
    try {
      const response = await request<SubscriptionDto[]>('/api/subscriptions');
      items.value = Array.isArray(response)
        ? response.map((item) => ({
            ...item,
            siteUrl: item.siteUrl ?? item.url
          }))
        : [];
    } catch (err) {
      const message = err instanceof Error ? err.message : '订阅列表加载失败';
      error.value = message;
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const addSubscription = async (feedUrl: string) => {
    if (!feedUrl) {
      error.value = '请输入有效的订阅链接';
      return;
    }
    submitting.value = true;
    error.value = null;
    try {
      await request('/api/subscriptions', {
        method: 'POST',
        json: { feedUrl }
      });
      await fetchSubscriptions();
    } catch (err) {
      const message = err instanceof Error ? err.message : '添加订阅失败';
      error.value = message;
      throw err;
    } finally {
      submitting.value = false;
    }
  };

  const removeSubscription = async (feedId: string) => {
    submitting.value = true;
    error.value = null;
    try {
      await request(`/api/subscriptions/${feedId}`, { method: 'DELETE' });
      items.value = items.value.filter((item) => item.feedId !== feedId);
    } catch (err) {
      const message = err instanceof Error ? err.message : '取消订阅失败';
      error.value = message;
      throw err;
    } finally {
      submitting.value = false;
    }
  };

  return {
    items,
    loading,
    submitting,
    error,
    fetchSubscriptions,
    addSubscription,
    removeSubscription
  };
});
