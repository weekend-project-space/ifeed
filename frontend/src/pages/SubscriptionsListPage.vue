<template>
  <div class="">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 py-6">
      <!-- Header -->
      <div class="mb-6">
        <h1 class="text-2xl font-normal text-gray-900 dark:text-gray-100 mb-4">
          所有订阅
        </h1>

        <!-- Tabs -->
        <div class="border-b border-gray-200 dark:border-gray-700 mb-6">
          <nav class="-mb-px flex space-x-8" aria-label="Tabs">
            <button
                v-for="tab in tabs"
                :key="tab.key"
                @click="currentTab = tab.key"
                :class="[
                currentTab === tab.key
                  ? 'border-secondary text-secondary'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300',
                'whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors'
              ]"
            >
              {{ tab.name }}
            </button>
          </nav>
        </div>
      </div>

      <!-- Subscriptions Content -->
      <div v-if="currentTab === 'subscriptions'">
        <subscription-feed-list />
      </div>

      <!-- Mix Feeds Content -->
      <div v-else-if="currentTab === 'mix-feeds'">
        <subscription-mix-feed-manager />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import SubscriptionMixFeedManager from './components/SubscriptionMixFeedManager.vue';
import SubscriptionFeedList from './components/SubscriptionFeedList.vue';

const tabs = [
  { key: 'subscriptions', name: '订阅列表' },
  { key: 'mix-feeds', name: '我创建的订阅' }
] as const;

const currentTab = ref(tabs[0].key);
</script>