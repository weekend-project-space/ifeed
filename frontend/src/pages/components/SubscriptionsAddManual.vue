<template>
  <div>
    <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">
      粘贴 RSS 地址或网站链接，系统会自动检测支持的订阅源
    </p>
    <div class="mb-6">
      <form @submit.prevent="handleAdd" class="relative">
        <div class="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
          <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <!-- 小圆点 -->
            <circle cx="5" cy="19" r="1.5" fill="currentColor" stroke="none"/>
            <!-- 内层弧线 -->
            <path d="M 4 11 A 8 8 0 0 1 13 20"/>
            <!-- 外层弧线 -->
            <path d="M 4 4 A 15 15 0 0 1 20 20"/>
          </svg>
        </div>
        <input
            v-model.trim="newFeedUrl"
            type="search"
            placeholder="粘贴 RSS 地址或网站链接..."
            class="w-full pl-11 pr-4 py-3 text-base text-gray-900 dark:text-gray-100 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-full shadow-sm focus:outline-none focus:border-secondary focus:ring-1 focus:ring-secondary transition-shadow"
        />
        <button
            type="submit"
            class="absolute right-2 top-1.5 bottom-1.5 px-4 text-sm font-medium text-white bg-secondary dark:bg-secondary/10 rounded-full hover:bg-secondary/90 transition-colors disabled:opacity-50"
            :disabled="subscriptionsStore.submitting">
          {{ subscriptionsStore.submitting ? '添加中...' : '添加' }}
        </button>
      </form>
    </div>
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
