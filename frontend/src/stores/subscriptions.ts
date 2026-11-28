import { translate as tr } from '@/i18n';
import { ref } from 'vue';
import { defineStore } from 'pinia';
import {
    listSubscriptions,
    type SubscriptionListItemDto,
} from '@/api/subscriptions';

export type { SubscriptionListItemDto } from '@/api/subscriptions';

export const useSubscriptionsStore = defineStore('subscriptions', () => {
    const items = ref<SubscriptionListItemDto[]>([]);
    const loading = ref(false);
    const error = ref<string | null>(null);

    const fetchSubscriptions = async () => {
        loading.value = true;
        error.value = null;
        try {
            const response = await listSubscriptions();
            items.value = Array.isArray(response)
                ? response.map((item) => ({
                    ...item,
                    siteUrl: item.siteUrl ?? item.url,
                    isRead: item.isRead ?? false,
                    failureCount: item.failureCount ?? 0,
                    fetchError: item.fetchError ?? null
                }))
                : [];
        } catch (err) {
            const message = err instanceof Error ? err.message : tr('common.listFailed');
            error.value = message;
            throw err;
        } finally {
            loading.value = false;
        }
    };

    return {
        items,
        loading,
        error,
        fetchSubscriptions,
    };
});
