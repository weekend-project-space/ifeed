<template>
    <div class="mx-auto max-w-7xl px-0 py-8 sm:px-6">
      <!-- Channel Header -->
      <section class="mb-8 space-y-4">
        <!-- Channel Info -->
        <div class="flex items-start gap-4">
          <!-- Avatar -->
          <div class="flex-shrink-0">
            <img
                v-if="detail?.avatar"
                :src="detail.avatar"
                class="h-16 w-16 rounded-full bg-gray-100 dark:bg-gray-800 object-cover"
                alt="频道头像" />
            <div
                v-else
                class="flex h-16 w-16 items-center justify-center rounded-full bg-secondary/10 text-2xl font-semibold text-secondary">
              {{ channelInitial }}
            </div>
          </div>

          <!-- Info & Actions -->
          <div class="flex-1 min-w-0 space-y-3">
            <div>
              <h1 class="text-2xl font-normal text-gray-900 dark:text-gray-100 mb-1">
                {{ channelTitle }}
              </h1>
              <p class="text-sm text-gray-600 dark:text-gray-400 ">
                {{ detail?.description}}   最近更新 {{ latestUpdateText }} · 最近抓取 {{ lastFetchedText }}
              </p>
            </div>

            <!-- Stats -->
            <div class="flex flex-wrap gap-4 text-sm text-gray-600 dark:text-gray-400">
              <span class="flex items-center gap-1.5">
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                {{ detail?.articleCount ?? 0 }} 篇文章
              </span>
              <span class="flex items-center gap-1.5">
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
                {{ detail?.subscriberCount ?? 0 }} 订阅者
              </span>
            </div>

            <!-- Actions -->
            <div class="flex flex-wrap gap-2">
              <button
                  type="button"
                  class="px-4 py-2 text-sm font-medium rounded-full transition-colors"
                  :class="detail?.subscribed
                  ? 'text-secondary bg-secondary/10 hover:bg-secondary/20'
                  : 'text-white bg-secondary hover:bg-secondary/90'"
                  :disabled="subscriptionSubmitting || feedLoading"
                  @click="toggleSubscription">
                {{ detail?.subscribed ? '已订阅' : '订阅' }}
              </button>

              <a
                  v-if="detail?.siteUrl"
                  :href="detail.siteUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-800 rounded-full hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors">
                访问网站
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                </svg>
              </a>
            </div>
            <div v-if="detail?.url" class="flex items-center gap-2 text-xs text-gray-500 dark:text-gray-500">
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
              </svg>
              <span class="truncate">{{ detail.url }}</span>
            </div>
            <!-- Error Warning -->
            <div
                v-if="hasFetchIssue"
                class="flex items-start gap-2 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-sm text-red-700 dark:text-red-400">
              <svg class="h-5 w-5 flex-shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
              </svg>
              <span class="truncate">{{ fetchIssueLabel }}</span>
            </div>

            <p v-if="feedError" class="text-sm text-red-600 dark:text-red-400">
              {{ feedError }}
            </p>
          </div>
        </div>

        <!-- URL Info -->

      </section>

      <!-- Tag Filter -->
      <div
          v-if="selectedTagDisplay"
          class="mb-6 flex items-center justify-between gap-4 p-3 bg-secondary/5 border border-secondary/20 rounded-lg">
        <span class="text-sm text-gray-700 dark:text-gray-300">
          标签筛选：<span class="font-medium text-secondary">{{ selectedTagDisplay }}</span>
        </span>
        <button
            class="text-sm font-medium text-secondary hover:underline"
            @click="clearTag">
          清除
        </button>
      </div>

      <!-- Articles Section -->
      <section class="space-y-6">
        <article-list
            title="文章列表"
            subtitle=""
            :items="items"
            :loading="articlesLoading"
            empty-message="该频道暂时没有文章，稍后再来看看。"
            @select="handleSelect"
            @select-tag="handleSelectTag"
            @refresh="refreshArticles" />

        <p v-if="articleError" class="text-sm text-red-600 dark:text-red-400">
          {{ articleError }}
        </p>

        <!-- Pagination -->
        <pagination
            v-if="items.length && !articlesLoading"
            :current-page="page"
            :has-previous-page="hasPreviousPage"
            :has-next-page="hasNextPage"
            :disabled="articlesLoading"
            @prev-page="prevPage"
            @next-page="nextPage"
        />
      </section>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useArticlesStore } from '../stores/articles';
import { useFeedStore } from '../stores/feeds';
import { useSubscriptionsStore } from '../stores/subscriptions';
import { useReadFeedStore } from '../stores/readfeed';
import { formatRelativeTime } from '../utils/datetime';

const route = useRoute();
const router = useRouter();
const feedStore = useFeedStore();
const articlesStore = useArticlesStore();
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
const { items: subscriptionItems, submitting: subscriptionSubmitting } = storeToRefs(subscriptionsStore);

const currentFeedId = computed(() => {
  const raw = route.params.feedId;
  if (Array.isArray(raw)) return raw[0] ?? null;
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
    if (trimmed.length > 0) return trimmed;
  }
  return null;
});

const normalizedTag = computed(() => {
  const tag = routeTag.value;
  return tag ? tag.toLowerCase() : null;
});

const selectedTagDisplay = computed(() => routeTag.value);

const channelTitle = computed(() => {
  if (!detail.value) return '订阅源频道';
  const title = detail.value.title?.trim();
  if (title) return title;
  const site = detail.value.siteUrl?.trim();
  if (site) return site;
  return detail.value.url;
});

const channelInitial = computed(() => {
  const text = channelTitle.value?.trim();
  if (!text) return 'F';
  return text.charAt(0).toUpperCase();
});

const latestUpdateText = computed(() => {
  const target = detail.value;
  if (!target) return '暂无更新';
  const timestamp = target.latestPublishedAt ?? target.lastUpdated ?? target.lastFetched;
  return timestamp ? formatRelativeTime(timestamp) : '暂无更新';
});

const lastFetchedText = computed(() => {
  const target = detail.value;
  if (!target || !target.lastFetched) return '暂无记录';
  return formatRelativeTime(target.lastFetched);
});

const failureCount = computed(() => detail.value?.failureCount ?? 0);

const fetchErrorMessage = computed(() => {
  const message = detail.value?.fetchError;
  if (!message) return null;
  const trimmed = message.trim();
  return trimmed.length ? trimmed : null;
});

const hasFetchIssue = computed(() => failureCount.value > 0 || !!fetchErrorMessage.value);

const fetchIssueLabel = computed(() => {
  const parts: string[] = [];
  if (failureCount.value > 0) {
    parts.push(`连续失败 ${failureCount.value} 次`);
  }
  if (fetchErrorMessage.value) {
    parts.push(`错误：${fetchErrorMessage.value}`);
  }
  return parts.join(' · ') || '抓取异常';
});

const buildQuery = (overrides?: { page?: number; tags?: string | null }) => {
  const query: Record<string, string> = {};
  const hasTagOverride = overrides && Object.prototype.hasOwnProperty.call(overrides, 'tags');
  const tag = hasTagOverride ? overrides?.tags ?? null : routeTag.value;
  if (tag) query.tags = tag;
  const nextPage = overrides?.page ?? routePage.value;
  if (nextPage > 1) query.page = String(nextPage);
  return query;
};

const fetchArticles = async (targetPage = 1) => {
  const feedId = currentFeedId.value;
  if (!feedId) return;

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

const ensureSubscriptions = async () => {
  if (subscriptionItems.value.length) return;

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

  try {
    await feedStore.fetchById(feedId);
  } catch (err) {
    return;
  }

  setTimeout(async () => {
    await ensureSubscriptions();
    if (!(subscriptionItems.value.find((sub) => sub.feedId === feedId)?.isRead)) {
      readFeedStore.recordFeedRead(feedId)
          .then(() => subscriptionsStore.fetchSubscriptions())
          .catch((err) => console.warn('记录订阅已读失败', err));
    }
  }, 1000);
};

onMounted(() => {
  watch(currentFeedId, async () => {
    await loadFeed();
  }, { immediate: true });
});

watch([currentFeedId, routePage, normalizedTag], async ([feedId, page]) => {
  if (!feedId) return;
  await fetchArticles(page || 1);
}, { immediate: true });

const refreshArticles = async () => {
  const targetPage = routePage.value || 1;
  await fetchArticles(targetPage);
};

const handleSelect = (articleId: string) => {
  articlesStore.recordHistory(articleId);
  router.push({ name: 'article-detail', params: { id: articleId } });
};

const handleSelectTag = (tag: string) => {
  if (!tag || !currentFeedId.value) return;

  router.push({
    name: 'feed',
    params: { feedId: currentFeedId.value },
    query: buildQuery({ page: 1, tags: tag.toLowerCase() })
  });
};

const clearTag = () => {
  if (!routeTag.value || !currentFeedId.value) return;

  router.push({
    name: 'feed',
    params: { feedId: currentFeedId.value },
    query: buildQuery({ page: 1, tags: null })
  });
};

const nextPage = () => {
  if (!hasNextPage.value || !currentFeedId.value) return;

  router.push({
    name: 'feed',
    params: { feedId: currentFeedId.value },
    query: buildQuery({ page: routePage.value + 1 })
  });
};

const prevPage = () => {
  if (!hasPreviousPage.value || !currentFeedId.value) return;

  router.push({
    name: 'feed',
    params: { feedId: currentFeedId.value },
    query: buildQuery({ page: Math.max(1, routePage.value - 1) })
  });
};

const toggleSubscription = async () => {
  if (!detail.value || !currentFeedId.value) return;

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