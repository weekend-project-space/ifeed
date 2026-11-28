<template>
  <div class="min-h-full text-text">
    <div class="mx-auto max-w-screen-lg px-4 py-6 sm:px-6 sm:py-8">
      <!-- 页面头部 -->
      <div class="mb-7">
        <h1 class="text-2xl font-bold tracking-tight text-text sm:text-3xl">
          {{ tr('subscriptionsListPage.allSubscriptions') }}
        </h1>
      </div>

      <!-- 订阅列表内容 -->
      <subscription-feed-list />
    </div>
  </div>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import { useDocumentTitle } from '@/composables/documentTitle';
import SubscriptionFeedList from './components/SubscriptionFeedList.vue';

defineOptions({ name: 'SubscriptionsPage' });
useDocumentTitle(() => tr('subscriptionsListPage.allSubscriptions'));
</script>
