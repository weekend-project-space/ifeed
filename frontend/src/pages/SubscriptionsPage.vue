<template>
  <div class="space-y-8">
    <section class="rounded-3xl border border-outline/40 bg-surface-container p-6">
      <h2 class="text-lg font-semibold text-text">添加新的订阅源</h2>
      <p class="mt-1 text-sm text-text-secondary">粘贴 RSS 地址或网站链接，我们会自动检测支持的内容。</p>
      <form class="mt-4 flex flex-col gap-3 md:flex-row" @submit.prevent="handleAdd">
        <input
          v-model.trim="newFeedUrl"
          type="url"
          required
          placeholder="https://example.com/feed.xml"
          class="flex-1 rounded-full border border-outline/50 bg-surface px-4 py-3 text-sm text-text transition focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
        />
        <button
          type="submit"
          class="inline-flex items-center justify-center rounded-full bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="subscriptionsStore.submitting"
        >
          {{ subscriptionsStore.submitting ? '添加中...' : '添加订阅' }}
        </button>
      </form>
      <p v-if="subscriptionsStore.error" class="mt-3 text-sm text-danger">{{ subscriptionsStore.error }}</p>
    </section>

    <section class="rounded-3xl border border-outline/40 bg-surface-container">
      <header class="flex items-center justify-between border-b border-outline/30 px-6 py-4">
        <div>
          <h2 class="text-lg font-semibold text-text">我的订阅</h2>
          <p class="text-sm text-text-secondary">当前共 {{ items.length }} 个订阅源。</p>
        </div>
        <button class="text-sm font-medium text-text-muted transition hover:text-text" @click="refresh">刷新</button>
      </header>
      <div v-if="subscriptionsStore.loading" class="py-12 text-center text-text-muted">加载中...</div>
      <ul v-else class="divide-y divide-outline/20">
        <li
          v-for="item in items"
          :key="item.feedId"
          class="flex flex-wrap items-center gap-4 px-6 py-4"
        >
          <div class="min-w-0 flex-1 space-y-1">
            <div class="flex flex-wrap items-center gap-3">
              <p class="truncate font-medium text-text">
                {{ displayTitle(item) }}
              </p>
              <span class="whitespace-nowrap rounded-full bg-surface-variant px-2.5 py-0.5 text-xs text-text-muted">
                最近更新 {{ formatRecentText(item.lastUpdated) }}
              </span>
            </div>
            <div class="flex flex-wrap items-center gap-3 text-sm text-text-secondary">
              <a
                :href="displaySiteUrl(item)"
                target="_blank"
                rel="noopener noreferrer"
                class="truncate text-sm text-primary hover:underline"
              >
                {{ displaySiteUrl(item) }}
              </a>
              <span class="text-text-muted">上次抓取 {{ formatRecentText(item.lastFetched) }}</span>
            </div>
          </div>
          <div class="flex shrink-0 items-center gap-2">
            <button
              class="rounded-full border border-primary/40 px-3 py-1.5 text-sm font-medium text-primary transition hover:bg-primary/10"
              @click="viewArticles(item.feedId)"
            >
              查看文章
            </button>
            <button
              class="text-sm font-medium text-danger transition hover:opacity-80"
              @click="remove(item.feedId)"
              :disabled="subscriptionsStore.submitting"
            >
              移除
            </button>
          </div>
        </li>
        <li v-if="!items.length" class="px-6 py-10 text-center text-text-muted">暂无订阅，先添加一个试试。</li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useSubscriptionsStore, type SubscriptionDto } from '../stores/subscriptions';
import { formatRelativeTime } from '../utils/datetime';

const subscriptionsStore = useSubscriptionsStore();
const { items } = storeToRefs(subscriptionsStore);
const newFeedUrl = ref('');
const router = useRouter();

const refresh = async () => {
  try {
    await subscriptionsStore.fetchSubscriptions();
  } catch (err) {
    console.warn('订阅刷新失败', err);
  }
};

const handleAdd = async () => {
  if (!newFeedUrl.value) {
    return;
  }
  try {
    await subscriptionsStore.addSubscription(newFeedUrl.value);
    newFeedUrl.value = '';
  } catch (err) {
    // 错误信息由 store 维护
  }
};

const remove = async (feedId: string) => {
  try {
    await subscriptionsStore.removeSubscription(feedId);
  } catch (err) {
    // 错误信息由 store 维护
  }
};

const viewArticles = (feedId: string) => {
  router.push({ name: 'home', query: { feedId } });
};

const displayTitle = (item: SubscriptionDto) => item.title || item.siteUrl || item.url;

const displaySiteUrl = (item: SubscriptionDto) => item.siteUrl || item.url;

const formatRecentText = (value?: string | null) => (value ? formatRelativeTime(value) : '暂无记录');

onMounted(() => {
  if (!items.value.length) {
    refresh();
  }
});
</script>
