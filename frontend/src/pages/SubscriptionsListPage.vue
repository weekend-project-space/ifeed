<template>
  <div class="min-h-full bg-gray-50/70 dark:bg-gray-950/40">
    <div class="max-w-screen-lg mx-auto px-4 py-6 sm:px-6 sm:py-8">
      <!-- Header -->
      <div class="mb-7">
        <h1 class="mb-5 text-2xl font-semibold tracking-tight text-gray-900 dark:text-gray-100 sm:text-3xl">
          所有订阅
        </h1>

        <!-- Tabs -->
        <div class="border-b border-gray-200 dark:border-gray-800">
          <nav class="-mb-px flex gap-6 overflow-x-auto sm:gap-8" aria-label="Tabs">
            <button
                v-for="tab in tabs"
                :key="tab.key"
                @click="currentTab = tab.key"
                :class="[
                currentTab === tab.key
                  ? 'border-secondary text-secondary'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300',
                'whitespace-nowrap border-b-2 px-1 py-3 text-sm font-medium transition-colors'
              ]"
              :aria-current="currentTab === tab.key ? 'page' : undefined"
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
