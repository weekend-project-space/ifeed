<template>
  <div>
    <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">
      粘贴 RSS 地址或网站链接，系统会自动检测支持的订阅源
    </p>
    <form @submit.prevent="handleAdd">
      <div class="flex items-center gap-2 mb-3">
        <input
            v-model.trim="newFeedUrl"
            type="url"
            required
            placeholder="https://example.com/feed.xml"
            class="flex-1 px-4 py-2.5 text-sm text-gray-900 dark:text-gray-100 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-full hover:border-gray-400 dark:hover:border-gray-500 focus:outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20 transition-all" />
        <button
            type="submit"
            class="px-6 py-2.5 text-sm font-medium text-white bg-secondary rounded-full hover:bg-secondary/90 disabled:bg-gray-300 dark:disabled:bg-gray-700 disabled:text-gray-500 disabled:cursor-not-allowed transition-all"
            :disabled="subscriptionsStore.submitting">
-          {{ subscriptionsStore.submitting ? '添加中...' : '添加' }}
        </button>
      </div>
    </form>
    <p v-if="subscriptionsStore.error" class="mt-3 px-4 py-2.5 text-sm text-red-700 dark:text-red-400 bg-red-50 dark:bg-red-900/20 rounded-lg">
      {{ subscriptionsStore.error }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useSubscriptionsStore } from '../../stores/subscriptions';

const subscriptionsStore = useSubscriptionsStore();
const newFeedUrl = ref('');

const handleAdd = async () => {
  if (!newFeedUrl.value) return;
  try {
    await subscriptionsStore.addSubscription(newFeedUrl.value);
    newFeedUrl.value = '';
  } catch (err) {
    // 错误信息由 store 维护
  }
};
</script>
