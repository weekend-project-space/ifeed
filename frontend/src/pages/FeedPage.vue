<template>
  <div class="space-y-10">
    <section class="relative overflow-hidden rounded-3xl border border-outline/40 bg-surface-container px-8 py-10">
      <div
        class="pointer-events-none absolute inset-0 bg-gradient-to-br from-primary/25 via-primary/10 to-transparent blur-3xl">
      </div>
      <div class="relative flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
        <div class="space-y-6">
          <div class="flex flex-wrap items-center gap-3 text-xs font-semibold uppercase tracking-[0.3em] text-primary">
            <span class="rounded-full bg-primary/15 px-3 py-1">Feed Channel</span>
            <span v-if="detail?.siteUrl" class="truncate text-primary/80">{{ detail.siteUrl }}</span>
          </div>
          <div class="flex flex-col gap-5 sm:flex-row sm:items-center sm:gap-6">
            <img v-if="detail.avatar"
                :src="detail.avatar"
                class="flex h-20 w-20 items-center justify-center rounded-full bg-primary text-3xl font-bold text-primary-foreground shadow-xl"
                />
            <div
                v-else
              class="flex h-20 w-20 items-center justify-center rounded-full bg-primary text-3xl font-bold text-primary-foreground shadow-xl">
              {{ channelInitial }}
            </div>
            <div class="space-y-2">
              <h1 class="text-3xl font-bold leading-tight text-text sm:text-4xl">
                {{ channelTitle }}
              </h1>
              <p class="text-sm text-text-secondary">
                最近更新 {{ latestUpdateText }} · 源地址 {{ detail?.url ?? '未知' }}
              </p>
            </div>
          </div>
          <div class="flex flex-wrap items-center gap-4 text-sm text-text-secondary">
            <span class="inline-flex items-center gap-2 rounded-full bg-surface/60 px-4 py-2">
              <svg class="h-4 w-4 text-primary" viewBox="0 0 20 20" fill="none" stroke="currentColor"
                stroke-width="1.6">
                <path stroke-linecap="round" stroke-linejoin="round" d="M5 4h10M5 10h10M5 16h10" />
              </svg>
              文章 {{ detail?.articleCount ?? 0 }} 篇
            </span>
            <span class="inline-flex items-center gap-2 rounded-full bg-surface/60 px-4 py-2">
              <svg class="h-4 w-4 text-primary" viewBox="0 0 20 20" fill="none" stroke="currentColor"
                stroke-width="1.6">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M10 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm0 2c-2.667 0-8 1.333-8 4v1h16v-1c0-2.667-5.333-4-8-4Z" />
              </svg>
              订阅者 {{ detail?.subscriberCount ?? 0 }} 名
            </span>
            <span class="inline-flex items-center gap-2 rounded-full bg-surface/60 px-4 py-2">
              <svg class="h-4 w-4 text-primary" viewBox="0 0 20 20" fill="none" stroke="currentColor"
                stroke-width="1.6">
                <path stroke-linecap="round" stroke-linejoin="round" d="M10 2v4m0 8v4m8-8h-4M6 10H2" />
              </svg>
              最近抓取 {{ lastFetchedText }}
            </span>
          </div>
          <div class="flex flex-wrap items-center gap-3">
            <a v-if="detail?.siteUrl" :href="detail.siteUrl" target="_blank" rel="noopener noreferrer"
              class="inline-flex items-center gap-2 rounded-full border border-primary/30 bg-surface px-4 py-2 text-sm font-medium text-primary transition hover:bg-primary/10">
              访问官网
              <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
                <path stroke-linecap="round" stroke-linejoin="round" d="M7 7h6v6M7 13l6-6" />
              </svg>
            </a>
            <button type="button"
              class="inline-flex items-center gap-2 rounded-full px-5 py-2 text-sm font-semibold transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary/40"
              :class="detail?.subscribed ? 'border border-primary/30 bg-primary/10 text-primary hover:bg-primary/15' : 'bg-primary text-primary-foreground hover:bg-primary/90'"
              :disabled="subscriptionSubmitting || feedLoading" @click="toggleSubscription">
              <svg class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                <path v-if="detail?.subscribed" d="M7.707 13.707 4 10l1.414-1.414L7.707 10.88l6.879-6.88L16 5.414z" />
                <path v-else d="M10 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16Zm-.75-4.5v-3h-3v-1.5h3v-3h1.5v3h3v1.5h-3v3z" />
              </svg>
              {{ detail?.subscribed ? '取消订阅' : '订阅频道' }}
            </button>
            <button type="button"
              class="inline-flex items-center gap-2 rounded-full border border-outline/30 px-4 py-2 text-sm font-medium text-text transition hover:border-primary/40 hover:text-primary"
              :disabled="feedLoading || articlesLoading" @click="refreshChannel">
              <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M4.5 7a5.5 5.5 0 0 1 9.9-2.1M15.5 13a5.5 5.5 0 0 1-9.9 2.1" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.5 4V7h-3" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M5.5 16v-3h3" />
              </svg>
              刷新频道
            </button>
          </div>
          <p v-if="feedError" class="text-sm text-danger">{{ feedError }}</p>
        </div>

        <form
          class="flex w-full max-w-md flex-col gap-3 rounded-3xl border border-outline/30 bg-surface p-6 shadow-lg/20"
          @submit.prevent="handleLookup">
          <div class="space-y-2">
            <p class="text-xs font-semibold uppercase tracking-widest text-text-muted">快速定位频道</p>
            <p class="text-sm text-text-secondary">粘贴 Feed URL，快速跳转到对应频道。</p>
          </div>
          <div class="flex flex-col gap-2 sm:flex-row">
            <input v-model.trim="lookupUrl" type="url" placeholder="https://example.com/feed.xml"
              class="flex-1 rounded-2xl border border-outline/40 bg-surface-container px-4 py-3 text-sm text-text transition focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
              :disabled="isLookingUp" required />
            <button type="submit"
              class="inline-flex items-center justify-center rounded-2xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="isLookingUp">
              {{ isLookingUp ? '查询中...' : '跳转频道' }}
            </button>
          </div>
          <p v-if="lookupStatus" :class="lookupStatusType" class="text-xs">{{ lookupStatus }}</p>
        </form>
      </div>
    </section>

    <section class="rounded-3xl border border-outline/40 bg-surface-container px-6 py-6">
      <header class="flex flex-wrap items-center justify-between gap-4 border-b border-outline/20 pb-4">
        <div>
          <h2 class="text-xl font-semibold text-text">频道文章</h2>
          <p class="text-sm text-text-secondary">基于订阅源实时抓取的最新内容。</p>
        </div>
        <div class="flex flex-wrap items-center gap-3 text-sm text-primary">
          <button type="button"
            class="inline-flex items-center gap-2 rounded-full border border-primary/20 px-4 py-2 font-medium transition hover:bg-primary/10"
            :disabled="articlesLoading" @click="refreshArticles">
            <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
              <path stroke-linecap="round" stroke-linejoin="round" d="M4 4v4h4M16 16v-4h-4" />
              <path stroke-linecap="round" stroke-linejoin="round"
                d="M5.5 8.5A5.5 5.5 0 0 1 16 7.5M14.5 11.5A5.5 5.5 0 0 1 4 12.5" />
            </svg>
            刷新列表
          </button>
        </div>
      </header>

      <div v-if="articlesLoading" class="py-20 text-center text-text-muted">正在加载文章...</div>
      <div v-else>
        <div v-if="selectedTagDisplay"
          class="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-outline/20 bg-surface px-4 py-2 text-sm text-text">
          <span>标签筛选：#{{ selectedTagDisplay }}</span>
          <button class="text-sm font-medium text-primary hover:underline" @click="clearTag">清除标签</button>
        </div>
        <ArticleList :items="channelArticles" empty-message="该频道暂时没有文章，稍后再来看看。" @select="handleSelect"
          @toggle-favorite="handleToggleFavorite" @select-tag="handleSelectTag" />
        <p v-if="articleError" class="mt-4 text-sm text-danger">{{ articleError }}</p>
      </div>

      <div class="mt-6 flex items-center justify-between border-t border-outline/20 pt-4 text-sm text-text-secondary">
        <button
          class="rounded-full border border-outline/30 px-3 py-2 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-outline/20 disabled:text-text-muted disabled:opacity-70"
          :disabled="!hasPrevious" @click="prevPage">
          上一页
        </button>
        <span>第 {{ currentPage }} 页</span>
        <button
          class="rounded-full border border-outline/30 px-3 py-2 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-outline/20 disabled:text-text-muted disabled:opacity-70"
          :disabled="!hasNext" @click="nextPage">
          下一页
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import ArticleList from '../components/ArticleList.vue';
import { useArticlesStore } from '../stores/articles';
import { useCollectionsStore } from '../stores/collections';
import { useFeedStore } from '../stores/feeds';
import { useSubscriptionsStore } from '../stores/subscriptions';
import { useReadFeedStore } from '../stores/readfeed';
import { formatRelativeTime } from '../utils/datetime';

const route = useRoute();
const router = useRouter();
const feedStore = useFeedStore();
const articlesStore = useArticlesStore();
const collectionsStore = useCollectionsStore();
const subscriptionsStore = useSubscriptionsStore();
const readFeedStore = useReadFeedStore();

const { detail, loading: feedLoading, error: feedError } = storeToRefs(feedStore);
const {
  items,
  loading: articlesLoading,
  error: articleError,
  hasNextPage,
  hasPreviousPage,
  page
} = storeToRefs(articlesStore);
const { items: collectionItems } = storeToRefs(collectionsStore);
const { items: subscriptionItems, submitting: subscriptionSubmitting } = storeToRefs(subscriptionsStore);

const lookupUrl = ref('');
const lookupStatus = ref<string | null>(null);
const lookupStatusType = computed(() => (lookupStatus.value && lookupStatus.value.includes('成功')) ? 'text-primary' : 'text-danger');
const isLookingUp = ref(false);

const currentFeedId = computed(() => {
  const raw = route.params.feedId;
  if (Array.isArray(raw)) {
    return raw[0] ?? null;
  }
  return typeof raw === 'string' ? raw : null;
});

const routePage = computed(() => {
  const raw = Array.isArray(route.query.page) ? route.query.page[0] : route.query.page;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
});

const routeTag = computed(() => {
  const raw = Array.isArray(route.query.tags) ? route.query.tags[0] : route.query.tags;
  if (typeof raw === 'string') {
    const trimmed = raw.trim();
    if (trimmed.length > 0) {
      return trimmed;
    }
  }
  return null;
});

const normalizedTag = computed(() => {
  const tag = routeTag.value;
  return tag ? tag.toLowerCase() : null;
});

const selectedTagDisplay = computed(() => routeTag.value);

const channelTitle = computed(() => {
  if (!detail.value) {
    return '订阅源频道';
  }
  const title = detail.value.title?.trim();
  if (title) {
    return title;
  }
  const site = detail.value.siteUrl?.trim();
  if (site) {
    return site;
  }
  return detail.value.url;
});

const channelInitial = computed(() => {
  const text = channelTitle.value?.trim();
  if (!text) {
    return 'F';
  }
  return text.charAt(0).toUpperCase();
});

const latestUpdateText = computed(() => {
  const target = detail.value;
  if (!target) {
    return '暂无更新';
  }
  const timestamp = target.latestPublishedAt ?? target.lastUpdated ?? target.lastFetched;
  return timestamp ? formatRelativeTime(timestamp) : '暂无更新';
});

const lastFetchedText = computed(() => {
  const target = detail.value;
  if (!target || !target.lastFetched) {
    return '暂无记录';
  }
  return formatRelativeTime(target.lastFetched);
});

const channelArticles = computed(() =>
  items.value.map((item) => ({
    ...item,
    collected: collectionsStore.isCollected(item.id)
  }))
);

const currentPage = computed(() => page.value);
const hasNext = computed(() => hasNextPage.value);
const hasPrevious = computed(() => hasPreviousPage.value);

const buildQuery = (overrides?: { page?: number; tags?: string | null }) => {
  const query: Record<string, string> = {};
  const hasTagOverride = overrides && Object.prototype.hasOwnProperty.call(overrides, 'tags');
  const tag = hasTagOverride ? overrides?.tags ?? null : routeTag.value;
  if (tag) {
    query.tags = tag;
  }
  const nextPage = overrides?.page ?? routePage.value;
  if (nextPage > 1) {
    query.page = String(nextPage);
  }
  return query;
};

const fetchArticles = async (targetPage = 1) => {
  const feedId = currentFeedId.value;
  if (!feedId) {
    return;
  }
  try {
    await articlesStore.fetchArticles({
      page: targetPage,
      size: 20,
      sort: 'publishedAt,desc',
      feedId,
      tags: normalizedTag.value
    });
  } catch (err) {
    console.warn('频道文章加载失败', err);
  }
};

const ensureCollections = async () => {
  if (collectionItems.value.length) {
    return;
  }
  try {
    await collectionsStore.fetchCollections();
  } catch (err) {
    console.warn('收藏列表加载失败', err);
  }
};

const ensureSubscriptions = async () => {
  if (subscriptionItems.value.length) {
    return;
  }
  try {
    await subscriptionsStore.fetchSubscriptions();
  } catch (err) {
    console.warn('订阅列表加载失败', err);
  }
};

const loadFeed = async () => {
  const feedId = currentFeedId.value;
  if (!feedId) {
    feedStore.clear();
    return;
  }
  lookupStatus.value = null;
  try {
    await feedStore.fetchById(feedId);
  } catch (err) {
    return;
  }
  // Mark this feed as read on entering the page (non-blocking)
  readFeedStore.recordFeedRead(feedId)
    .then(() => subscriptionsStore.fetchSubscriptions())
    .catch((err) => console.warn('记录订阅已读失败', err));
  await ensureCollections();
  await ensureSubscriptions();
};

watch(currentFeedId, async () => {
  await loadFeed();
}, { immediate: true });

watch([currentFeedId, routePage, normalizedTag], async ([feedId, page]) => {
  if (!feedId) {
    return;
  }
  await fetchArticles(page || 1);
}, { immediate: true });

const handleLookup = async () => {
  lookupStatus.value = null;
  const url = lookupUrl.value.trim();
  if (!url) {
    lookupStatus.value = '请输入有效的订阅链接';
    return;
  }
  isLookingUp.value = true;
  try {
    const result = await feedStore.lookupByUrl(url);
    if (result?.feedId) {
      lookupStatus.value = '查询成功，正在跳转频道';
      if (result.feedId !== currentFeedId.value) {
        await router.push({ name: 'feed', params: { feedId: result.feedId } });
      } else {
        await loadFeed();
      }
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : '订阅源查询失败';
    lookupStatus.value = message;
  } finally {
    isLookingUp.value = false;
  }
};

const refreshChannel = async () => {
  const feedId = currentFeedId.value;
  if (!feedId) {
    return;
  }
  try {
    await feedStore.fetchById(feedId);
  } catch (err) {
    console.warn('订阅源信息刷新失败', err);
  }
  const targetPage = routePage.value || 1;
  await fetchArticles(targetPage);
};

const refreshArticles = async () => {
  const targetPage = routePage.value || 1;
  await fetchArticles(targetPage);
};

const handleSelect = (articleId: string) => {
  articlesStore.recordHistory(articleId);
  router.push({ name: 'article-detail', params: { id: articleId } });
};

const handleToggleFavorite = async (articleId: string) => {
  const target = channelArticles.value.find((item) => item.id === articleId);
  try {
    await collectionsStore.toggleCollection(articleId, { title: target?.title });
  } catch (err) {
    console.warn('收藏操作失败', err);
  }
};

const handleSelectTag = (tag: string) => {
  if (!tag) {
    return;
  }
  if (!currentFeedId.value) {
    return;
  }
  router.push({
    name: 'feed',
    params: { feedId: currentFeedId.value },
    query: buildQuery({ page: 1, tags: tag.toLowerCase() })
  });
};

const clearTag = () => {
  if (!routeTag.value || !currentFeedId.value) {
    return;
  }
  router.push({
    name: 'feed',
    params: { feedId: currentFeedId.value },
    query: buildQuery({ page: 1, tags: null })
  });
};

const nextPage = () => {
  if (!hasNext.value || !currentFeedId.value) {
    return;
  }
  router.push({
    name: 'feed',
    params: { feedId: currentFeedId.value },
    query: buildQuery({ page: routePage.value + 1 })
  });
};

const prevPage = () => {
  if (!hasPrevious.value || !currentFeedId.value) {
    return;
  }
  router.push({
    name: 'feed',
    params: { feedId: currentFeedId.value },
    query: buildQuery({ page: Math.max(1, routePage.value - 1) })
  });
};

const toggleSubscription = async () => {
  if (!detail.value || !currentFeedId.value) {
    return;
  }
  try {
    if (detail.value.subscribed) {
      await subscriptionsStore.removeSubscription(detail.value.feedId);
    } else {
      await subscriptionsStore.addSubscription(detail.value.url);
    }
    await feedStore.fetchById(detail.value.feedId);
  } catch (err) {
    console.warn('订阅操作失败', err);
  }
};
</script>
