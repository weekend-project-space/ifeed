<template>
  <div class=" max-w-4xl mx-auto">
    <!-- Header -->
    <div class="border-b border-gray-200 dark:border-gray-700">
      <div class="px-6 py-6">
        <h1 class="text-2xl font-normal text-gray-900 dark:text-gray-100 mb-1">订阅源</h1>
        <p class="text-sm text-gray-600 dark:text-gray-400">查找并添加您感兴趣的订阅源</p>
      </div>
    </div>

    <!-- Tabs -->
    <div class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-10">
      <div class="px-6">
        <div class="flex gap-0">
          <button
              v-for="tab in tabs"
              :key="tab.key"
              type="button"
              class="px-4 py-3 text-sm font-medium transition-all relative"
              :class="activeTab === tab.key
                ? 'text-secondary'
                : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'"
              @click="activeTab = tab.key">
            {{ tab.label }}
            <div
                v-if="activeTab === tab.key"
                class="absolute bottom-0 left-0 right-0 h-[3px] bg-secondary rounded-t-full"></div>
          </button>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="px-6 py-6">
      <!-- Manual Add Tab -->
      <SubscriptionsAddManual v-if="activeTab === 'manual'" />

      <!-- Search Tab -->
      <SubscriptionsAddSearch v-else-if="activeTab === 'search'" />

      <!-- OPML Import Tab -->
      <SubscriptionsAddOpml v-else />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useSubscriptionsStore } from '../stores/subscriptions';
import { storeToRefs } from 'pinia';
import SubscriptionsAddManual from './components/SubscriptionsAddManual.vue';
import SubscriptionsAddSearch from './components/SubscriptionsAddSearch.vue';
import SubscriptionsAddOpml from './components/SubscriptionsAddOpml.vue';

const subscriptionsStore = useSubscriptionsStore();
const { items } = storeToRefs(subscriptionsStore);

const tabs = [
  { key: 'search', label: '查找订阅' },
  { key: 'manual', label: '手动添加' },
  { key: 'opml', label: '导入 OPML' }
] as const;

type TabKey = (typeof tabs)[number]['key'];

const activeTab = ref<TabKey>('search');

onMounted(() => {
  if (!items.value.length) {
    subscriptionsStore.fetchSubscriptions();
  }
});
</script>