<template>
  <div class="min-h-full text-text">
    <div class="mx-auto max-w-screen-lg px-4 py-6 sm:px-6 sm:py-8">
      <!-- 页面头部 -->
      <div class="mb-7">
        <h1 class="mb-5 text-2xl font-bold tracking-tight text-text sm:text-3xl">
          所有订阅
        </h1>

        <!-- MD3 Filter Chips 标签页导航 -->
        <nav class="flex items-center gap-2 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden" aria-label="订阅管理标签页">
          <button
              v-for="tab in tabs"
              :key="tab.key"
              type="button"
              @click="currentTab = tab.key"
              class="inline-flex h-9 shrink-0 items-center rounded-full px-4 text-xs font-medium transition select-none active:scale-[0.98]"
              :class="currentTab === tab.key
                ? 'bg-primary/15 font-semibold text-primary shadow-xs'
                : 'bg-surface-container/60 text-text-secondary hover:bg-surface-container hover:text-text'"
              :aria-current="currentTab === tab.key ? 'page' : undefined"
          >
            {{ tab.name }}
          </button>
        </nav>
      </div>

      <!-- 订阅列表内容 -->
      <div v-if="currentTab === 'subscriptions'">
        <subscription-feed-list />
      </div>

      <!-- 我创建的订阅（聚合源）内容 -->
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

defineOptions({ name: 'SubscriptionsPage' });

const tabs = [
  { key: 'subscriptions', name: '订阅列表' },
  { key: 'mix-feeds', name: '我创建的订阅' }
] as const;

const currentTab = ref(tabs[0].key);
</script>