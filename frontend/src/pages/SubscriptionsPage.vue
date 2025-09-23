<template>
  <div class="space-y-8">
    <section class="bg-white rounded-3xl border border-slate-200 shadow-sm p-6">
      <h2 class="text-lg font-semibold text-slate-900">添加新的订阅源</h2>
      <p class="text-sm text-slate-500 mt-1">粘贴 RSS 地址或网站链接，我们会自动检测支持的内容。</p>
      <form class="mt-4 flex flex-col md:flex-row gap-3" @submit.prevent="handleAdd">
        <input v-model.trim="newFeedUrl" type="url" required placeholder="https://example.com/feed.xml"
          class="flex-1 rounded-xl border-slate-200 focus:border-primary focus:ring-primary/40" />
        <button type="submit"
          class="inline-flex items-center justify-center px-4 py-2 rounded-xl bg-primary text-white font-medium shadow hover:bg-blue-600 transition disabled:opacity-60 disabled:cursor-not-allowed"
          :disabled="subscriptionsStore.submitting">
          {{ subscriptionsStore.submitting ? '添加中...' : '添加订阅' }}
        </button>
      </form>
      <p v-if="subscriptionsStore.error" class="mt-3 text-sm text-red-500">{{ subscriptionsStore.error }}</p>
    </section>

    <section class="bg-white rounded-3xl border border-slate-200 shadow-sm">
      <header class="flex items-center justify-between px-6 py-4 border-b border-slate-100">
        <div>
          <h2 class="text-lg font-semibold text-slate-900">我的订阅</h2>
          <p class="text-sm text-slate-500">当前共 {{ items.length }} 个订阅源。</p>
        </div>
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="refresh">刷新</button>
      </header>
      <div v-if="subscriptionsStore.loading" class="py-12 text-center text-slate-400">加载中...</div>
      <ul v-else class="divide-y divide-slate-100">
        <li
          v-for="item in items"
          :key="item.feedId"
          class="px-6 py-4 flex flex-wrap items-center gap-4"
        >
          <div class="min-w-0 flex-1 space-y-1">
            <div class="flex flex-wrap items-center gap-3">
              <p class="font-medium text-slate-800 truncate">
                {{ displayTitle(item) }}
              </p>
              <span class="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs text-slate-500 whitespace-nowrap">
                最近更新 {{ formatRecentText(item.lastUpdated) }}
              </span>
            </div>
            <div class="flex flex-wrap items-center gap-3 text-sm text-slate-500">
              <a
                :href="displaySiteUrl(item)"
                target="_blank"
                rel="noopener noreferrer"
                class="text-sm text-blue-500 truncate"
              >
                {{ displaySiteUrl(item) }}
              </a>
              <span class="text-slate-400">上次抓取 {{ formatRecentText(item.lastFetched) }}</span>
            </div>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <button
              class="rounded-xl border border-blue-500 px-3 py-1.5 text-sm text-blue-600 transition hover:bg-blue-50"
              @click="viewArticles(item.feedId)"
            >
              查看文章
            </button>
            <button
              class="text-sm text-red-500 hover:text-red-600"
              @click="remove(item.feedId)"
              :disabled="subscriptionsStore.submitting"
            >
              移除
            </button>
          </div>
        </li>
        <li v-if="!items.length" class="px-6 py-10 text-center text-slate-400">暂无订阅，先添加一个试试。</li>
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
