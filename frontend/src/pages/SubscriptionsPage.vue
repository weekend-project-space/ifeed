<template>
  <div class="mx-auto max-w-5xl space-y-6 px-0 py-6 sm:space-y-8 sm:px-4 sm:py-8 lg:space-y-10 lg:py-10">
    <section class="rounded-2xl border border-outline/15 bg-surface transition">
      <div
        class="flex flex-col gap-3 border-b border-outline/10 px-3 py-4 md:flex-row md:items-end md:justify-between sm:px-6 sm:py-5">
        <div>
          <h2 class="text-lg font-semibold text-text">管理订阅源</h2>
          <p class="mt-1 text-sm text-text-secondary">选择手动添加或批量导入订阅。</p>
        </div>
        <nav
          class="relative flex gap-2 rounded-full border border-outline/20 bg-surface/70 p-1 text-sm font-medium text-text-muted">
          <div class="absolute inset-y-1 rounded-full bg-primary/10 shadow-sm transition-all duration-300 ease-out"
            :style="tabIndicatorStyle"></div>
          <button v-for="tab in tabs" :key="tab.key" type="button"
            class="relative z-[1] flex-1 rounded-full px-3 py-1.5 transition focus:outline-none focus:ring-2 focus:ring-primary/20 sm:px-4 sm:py-2"
            :class="activeTab === tab.key ? 'text-text' : 'text-text-muted hover:text-text'"
            @click="setActiveTab(tab.key)">
            {{ tab.label }}
          </button>
        </nav>
      </div>

      <div class="px-3 py-5 sm:px-6 sm:py-6">
        <div v-if="activeTab === 'manual'" class="space-y-4 rounded-xl bg-surface-container ">
          <p class="text-sm text-text-secondary">粘贴 RSS 地址或网站链接，我们会自动检测支持的内容。</p>
          <form class="flex flex-col gap-3 md:flex-row" @submit.prevent="handleAdd">
            <input v-model.trim="newFeedUrl" type="url" required placeholder="https://example.com/feed.xml"
              class="flex-1 rounded-full border border-outline/40 bg-surface px-3 py-2.5 text-sm text-text transition placeholder:text-text-muted focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 sm:px-4 sm:py-3" />
            <button type="submit"
              class="inline-flex items-center justify-center rounded-full bg-gradient-to-r from-primary to-primary/80 px-3.5 py-2.5 text-sm font-semibold text-primary-foreground transition hover:from-primary/90 hover:to-primary/70 disabled:cursor-not-allowed disabled:opacity-60 sm:px-5 sm:py-3"
              :disabled="subscriptionsStore.submitting">
              {{ subscriptionsStore.submitting ? '添加中...' : '添加订阅' }}
            </button>
          </form>
          <p v-if="subscriptionsStore.error" class="text-sm text-danger">{{ subscriptionsStore.error }}</p>
        </div>

        <div v-else-if="activeTab === 'search'" class="space-y-4 bg-surface-container">
          <p class="text-sm text-text-secondary">
            输入关键词、站点或订阅标题，快速查找系统内已存在的订阅源。
          </p>
          <form class="flex flex-col gap-3 md:flex-row" @submit.prevent="handleSearch">
            <input v-model.trim="searchQuery" type="search" placeholder="例如：36kr、https://example.com 或科技"
              class="flex-1 rounded-full border border-outline/40 bg-surface px-3 py-2.5 text-sm text-text transition placeholder:text-text-muted focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 sm:px-4 sm:py-3" />
            <button type="submit"
              class="inline-flex items-center justify-center rounded-full bg-gradient-to-r from-primary to-primary/80 px-3.5 py-2.5 text-sm font-semibold text-primary-foreground transition hover:from-primary/90 hover:to-primary/70 disabled:cursor-not-allowed disabled:opacity-60 sm:px-5 sm:py-3"
              :disabled="searchLoading">
              {{ searchLoading ? '搜索中...' : '搜索订阅' }}
            </button>
          </form>
          <p v-if="searchError" class="text-sm text-danger">{{ searchError }}</p>
          <div v-if="searchLoading" class="py-8 text-center text-sm text-text-muted sm:py-10">正在搜索订阅源...</div>
          <ul v-else class="space-y-3 sm:space-y-4">
            <li v-for="feed in searchResults" :key="feed.feedId"
              class="rounded-xl border border-outline/20 bg-surface p-3 transition hover:border-outline/40 sm:p-4">
              <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
                <div class="min-w-0 space-y-1.5">
                  <p class="truncate text-sm font-medium text-text">
                    {{ displayTitle(feed) }}
                  </p>
                  <div class="flex flex-wrap items-center gap-3 text-xs text-text-muted">
                    <span>最近更新 {{ formatRecentText(feed.lastUpdated) }}</span>
                    <span>上次抓取 {{ formatRecentText(feed.lastFetched) }}</span>
                    <span>订阅用户 {{ feed.subscriberCount ?? 0 }}</span>
                  </div>
                  <a :href="displaySiteUrl(feed)" target="_blank" rel="noopener noreferrer"
                    class="block truncate text-xs text-primary hover:underline">
                    {{ displaySiteUrl(feed) }}
                  </a>
                  <p v-if="feed.fetchError" class="text-xs text-danger">
                    最近错误：{{ feed.fetchError }}
                  </p>
                </div>
                <div class="flex flex-col items-stretch gap-2 text-xs sm:flex-row sm:items-center sm:gap-3">
                  <button v-if="isSubscribed(feed)" type="button"
                    class="rounded-full border border-outline/40 px-3 py-1.5 font-medium text-primary transition hover:border-primary hover:text-primary sm:px-4 sm:py-2"
                    @click="viewArticles(feed.feedId)">
                    查看频道
                  </button>
                  <button v-else type="button"
                    class="rounded-full bg-gradient-to-r from-primary to-primary/80 px-3 py-1.5 font-semibold text-primary-foreground transition hover:from-primary/90 hover:to-primary/70 disabled:cursor-not-allowed disabled:opacity-60 sm:px-4 sm:py-2"
                    :disabled="subscriptionsStore.submitting" @click="subscribeFromSearch(feed)">
                    {{ subscriptionsStore.submitting ? '处理中...' : '添加订阅' }}
                  </button>
                  <a :href="feed.url" target="_blank" rel="noopener noreferrer"
                    class="rounded-full border border-outline/30 px-3 py-1.5 text-center font-medium text-text-muted transition hover:border-outline/50 hover:text-text sm:px-4 sm:py-2">
                    访问 RSS
                  </a>
                </div>
              </div>
            </li>
            <li v-if="hasSearched && !searchResults.length && !searchError"
              class="py-10 text-center text-sm text-text-muted">
              未找到匹配的订阅源，换个关键词试试。
            </li>
          </ul>
        </div>

        <div v-else class="space-y-4 bg-surface-container">
          <p class="text-sm text-text-secondary">
            上传一个 OPML 文件批量导入订阅。我们会先解析并展示结果，你可以选择保留的订阅项。
          </p>
          <div class="flex flex-col gap-4    md:flex-row md:items-center md:justify-between">
            <div class="flex flex-1 items-center gap-4">
              <div
                class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl border border-outline/20 bg-primary/10 text-xs font-semibold uppercase tracking-[0.3em] text-primary">
                OP
              </div>
              <div>
                <p class="text-sm font-medium text-text">选择一个 .opml 文件</p>
                <p class="text-xs text-text-muted">文件大小建议不超过 2 MB。</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <input ref="opmlFileInput" type="file" accept=".opml,.xml"
                class="block w-full cursor-pointer rounded-full border border-outline/40 bg-surface px-3 py-2.5 text-sm text-text transition file:mr-3 file:rounded-full file:border-0 file:bg-primary file:px-3.5 file:py-2 file:text-sm file:font-semibold file:text-primary-foreground hover:border-primary focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 md:w-auto sm:px-4 sm:py-3 sm:file:px-4"
                @change="handleOpmlFileChange" />
              <button
                class="inline-flex items-center justify-center rounded-full bg-gradient-to-r from-primary to-primary/80 px-5 py-3 text-sm font-semibold text-primary-foreground transition hover:from-primary/90 hover:to-primary/70 disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="opmlPreviewLoading || !opmlFile" @click="handlePreviewOpml">
                {{ opmlPreviewLoading ? '解析中...' : '解析 OPML' }}
              </button>
            </div>
          </div>
          <p v-if="opmlError" class="text-sm text-danger">{{ opmlError }}</p>
        </div>
      </div>
    </section>

    <section class="rounded-2xl border border-outline/30 bg-surface-container">
      <header
        class="flex flex-wrap items-center justify-between gap-3 border-b border-outline/30 px-3 py-3 sm:px-6 sm:py-4">
        <div>
          <h2 class="text-lg font-semibold text-text">我的订阅</h2>
          <p class="text-sm text-text-secondary">当前共 {{ items.length }} 个订阅源。</p>
        </div>
        <button class="text-sm font-medium text-text-muted transition hover:text-text" @click="refresh">刷新</button>
      </header>
      <div v-if="subscriptionsStore.loading" class="py-9 text-center text-text-muted sm:py-12">加载中...</div>
      <ul v-else class="divide-y divide-outline/20">
        <li v-for="item in items" :key="item.feedId"
          class="flex flex-wrap items-center justify-between gap-3 px-3 py-3 sm:gap-4 sm:px-6 sm:py-4">
          <div class="min-w-0 space-y-1.5">
            <p class="truncate text-sm font-medium text-text">
              {{ displayTitle(item) }}
            </p>
            <div class="flex flex-wrap items-center gap-3 text-xs text-text-muted">
              <span>最近更新 {{ formatRecentText(item.lastUpdated) }}</span>
              <span>上次抓取 {{ formatRecentText(item.lastFetched) }}</span>
            </div>
            <a :href="displaySiteUrl(item)" target="_blank" rel="noopener noreferrer"
              class="block truncate text-xs text-primary hover:underline">
              {{ displaySiteUrl(item) }}
            </a>
            <div v-if="subscriptionIssues[item.feedId]"
              class="mt-1 inline-flex min-w-0 max-w-full items-center gap-2 rounded-full border border-danger/30 bg-danger/10 px-3 py-1 text-xs text-danger"
              :title="subscriptionIssues[item.feedId]?.tooltip">
              <svg class="h-3.5 w-3.5 flex-shrink-0" viewBox="0 0 20 20" fill="none" stroke="currentColor"
                stroke-width="1.6">
                <path stroke-linecap="round" stroke-linejoin="round" d="M10 3.5 2.5 16.5h15L10 3.5Zm0 5v3.5m0 2v.5" />
              </svg>
              <span class="truncate max-w-[60vw] sm:max-w-[16rem]">{{ subscriptionIssues[item.feedId]?.label }}</span>
            </div>
          </div>
          <div class="flex flex-wrap items-center gap-3">
            <button class="text-sm font-medium text-primary transition hover:opacity-80"
              @click="viewArticles(item.feedId)">
              查看频道
            </button>
            <button class="text-sm font-medium text-danger transition hover:opacity-80" @click="remove(item.feedId)"
              :disabled="subscriptionsStore.submitting">
              取消订阅
            </button>
          </div>
        </li>
        <li v-if="!items.length" class="px-3 py-7 text-center text-text-muted sm:px-6 sm:py-10">暂无订阅，先添加一个试试。</li>
      </ul>
    </section>
    <transition name="fade">
      <div v-if="showOpmlModal" class="fixed inset-0 z-50 flex items-center justify-center px-2 py-5 sm:px-4 sm:py-6"
        style="--tw-space-y-reverse:none">
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="closeOpmlModal"></div>
        <div class="relative z-10 w-full max-w-3xl overflow-hidden rounded-2xl border border-outline/15 bg-surface">
          <header
            class="flex items-start justify-between gap-3 border-b border-outline/15 px-3 py-3 sm:gap-4 sm:px-6 sm:py-4">
            <div>
              <h3 class="text-lg font-semibold text-text">OPML 导入预览</h3>
              <p class="text-sm text-text-secondary">勾选需要导入的订阅，确认后即可批量添加。</p>
            </div>
            <button class="rounded-full px-3 py-1 text-sm text-text-muted transition hover:bg-outline/10"
              @click="closeOpmlModal">
              关闭
            </button>
          </header>
          <div class="space-y-3 px-3 py-4 sm:space-y-4 sm:px-6 sm:py-5">
            <div v-if="opmlWarnings.length"
              class="rounded-xl border border-warning/30 bg-warning/10 p-4 text-sm text-warning-foreground">
              <p class="font-medium">解析提醒</p>
              <div class="mt-2 max-h-36 overflow-y-auto pr-1">
                <ul class="list-disc space-y-1 pl-5">
                  <li v-for="(warning, index) in opmlWarnings" :key="`warning-${index}`">{{ warning }}</li>
                </ul>
              </div>
            </div>
            <div v-if="opmlPreviewFeeds.length" class="max-h-[55vh] space-y-3 overflow-y-auto overflow-x-hidden pr-1">
              <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <p class="text-sm text-text-secondary">
                  解析得到 <span class="font-semibold text-text">{{ opmlPreviewFeeds.length }}</span> 个订阅项。
                </p>
                <button class="text-sm text-primary hover:text-primary/80" type="button" @click="toggleSelectAll">
                  {{ isAllSelected ? '全不选' : '全选可导入项' }}
                </button>
              </div>
              <ul class="space-y-2.5 sm:space-y-3">
                <li v-for="feed in opmlPreviewFeeds" :key="feed.feedUrl"
                  class="rounded-xl border border-outline/20 bg-surface p-3 sm:p-4">
                  <label class="flex flex-col gap-3 md:flex-row md:items-start md:gap-4">
                    <input type="checkbox"
                      class="mt-1 h-4 w-4 rounded border-outline/40 text-primary focus:ring-primary/20 md:mt-2"
                      v-model="feed.selected" :disabled="feed.alreadySubscribed || feed.errors.length > 0" />
                    <div class="flex-1 min-w-0 space-y-2">
                      <div class="flex flex-wrap items-center gap-2">
                        <p class="truncate text-sm font-medium text-text" :title="feed.title">
                          {{ feed.title }}
                        </p>
                        <span v-if="feed.alreadySubscribed"
                          class="inline-flex items-center rounded-full bg-outline/10 px-3 py-1 text-xs font-medium text-text-muted">
                          已订阅
                        </span>
                        <span v-if="feed.errors.length"
                          class="inline-flex items-center rounded-full bg-danger/10 px-3 py-1 text-xs font-medium text-danger">
                          无法导入
                        </span>
                      </div>
                      <div
                        class="flex flex-col gap-1 text-xs text-text-muted md:flex-row md:flex-wrap md:items-center md:gap-3">
                        <span class="flex items-center gap-1 min-w-0">
                          <span class="text-text-secondary">来源：</span>
                          <a :href="feed.siteUrl" target="_blank" rel="noopener noreferrer"
                            class="truncate text-primary hover:text-primary/80 hover:underline">{{ feed.siteUrl }}</a>
                        </span>
                        <span class="truncate break-all text-text-muted">
                          {{ feed.feedUrl }}
                        </span>
                      </div>
                      <p v-if="feed.errors.length" class="text-xs text-danger">
                        {{ feed.errors.join('；') }}
                      </p>
                    </div>
                  </label>
                </li>
              </ul>
            </div>
            <p v-else-if="!opmlConfirmResult" class="text-center text-sm text-text-muted">暂无可预览的订阅，请重新上传 OPML 文件。</p>
            <div v-if="!opmlConfirmResult" class="flex flex-col gap-3 md:flex-row md:items-center md:gap-4">
              <div class="flex flex-1 flex-col gap-2 text-xs text-text-muted md:flex-row md:items-center md:gap-3">
                <div class="flex flex-wrap items-center gap-3">
                  <span>已选 {{ selectedCount }} 项</span>
                  <span>剩余可导入额度 {{ remainingQuota }}</span>
                  <span v-if="selectedCount > remainingQuota" class="text-danger">已超过可用额度，请取消部分订阅</span>
                </div>
                <p class="text-xs text-text-muted">
                  仅会导入勾选的订阅项，重复或异常条目会被自动跳过。
                </p>
              </div>
              <button type="button"
                class="inline-flex items-center justify-center rounded-full bg-gradient-to-r from-primary to-primary/80 px-3.5 py-2.5 text-sm font-semibold text-primary-foreground transition hover:from-primary/90 hover:to-primary/70 disabled:cursor-not-allowed disabled:opacity-60 md:ml-auto sm:px-5 sm:py-3"
                :disabled="opmlConfirmLoading || !hasSelectedFeeds || selectedCount > remainingQuota"
                @click="handleConfirmOpml">
                {{ opmlConfirmLoading ? '导入中...' : '确认导入' }}
              </button>
            </div>
            <div v-if="opmlConfirmResult"
              class="rounded-xl border border-outline/20 bg-surface px-3 py-3 text-sm text-text-secondary sm:px-4 sm:py-4">
              <div class="flex flex-col gap-3">
                <div class="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
                  <div>
                    <p class="text-sm font-semibold text-text">导入完成</p>
                    <p class="text-xs text-text-muted">成功导入 {{ opmlConfirmResult.importedCount }} 个订阅。</p>
                  </div>
                  <button
                    class="rounded-full border border-outline/40 px-3 py-1.5 text-xs font-medium text-text-muted transition hover:border-outline/60 hover:text-text sm:px-4 sm:py-2"
                    @click="handleSuccessClose">
                    关闭
                  </button>
                </div>
                <ul v-if="opmlConfirmResult.skipped.length" class="space-y-1 text-xs text-text-muted">
                  <li v-for="(item, index) in opmlConfirmResult.skipped" :key="`${item.feedUrl}-${index}`">
                    {{ item.feedUrl || '未知订阅' }} ：{{ translateSkippedReason(item.reason) }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import {
  useSubscriptionsStore,
  type SubscriptionListItemDto,
  type SubscriptionSearchResultDto,
  type OpmlImportResultDto,
  type OpmlPreviewFeedDto
} from '../stores/subscriptions';
import { formatRelativeTime } from '../utils/datetime';

const subscriptionsStore = useSubscriptionsStore();
const { items, searchResults, searchLoading, searchError } = storeToRefs(subscriptionsStore);
const newFeedUrl = ref('');
const router = useRouter();

const tabs = [
  { key: 'search', label: '查找订阅' },
  { key: 'manual', label: '手动添加' },
  { key: 'opml', label: '导入 OPML' }
] as const;

type TabKey = (typeof tabs)[number]['key'];

interface OpmlPreviewFeedView extends OpmlPreviewFeedDto {
  selected: boolean;
}

const activeTab = ref<TabKey>('search');
const tabIndicatorStyle = computed(() => {
  const index = tabs.findIndex((tab) => tab.key === activeTab.value);
  const gapRem = 0.5;
  const totalGaps = Math.max(tabs.length - 1, 0);
  return {
    width: `calc((100% - ${totalGaps * gapRem}rem) / ${tabs.length})`,
    transform: `translateX(calc(${index * 100}% + ${index * gapRem}rem))`
  };
});

const setActiveTab = (tabKey: TabKey) => {
  activeTab.value = tabKey;
};

const searchQuery = ref('');
const hasSearched = ref(false);
const subscribedFeedIds = computed(() => new Set(items.value.map((item) => item.feedId)));
const isSubscribed = (feed: SubscriptionSearchResultDto | SubscriptionListItemDto) => {
  if ('subscribed' in feed) {
    return feed.subscribed;
  }
  return subscribedFeedIds.value.has(feed.feedId);
};

const resetSearchState = () => {
  searchQuery.value = '';
  hasSearched.value = false;
  subscriptionsStore.clearSearchResults();
};

watch(activeTab, (tab) => {
  if (tab !== 'search') {
    resetSearchState();
  }
});

watch(searchQuery, (value) => {
  if (!value || !value.trim()) {
    hasSearched.value = false;
    subscriptionsStore.clearSearchResults();
  } else if (searchError.value) {
    searchError.value = null;
  }
});

resetSearchState();

const handleSearch = async () => {
  try {
    if (!searchQuery.value.trim()) {
      hasSearched.value = false;
      await subscriptionsStore.searchSubscriptions(searchQuery.value);
      return;
    }
    await subscriptionsStore.searchSubscriptions(searchQuery.value);
    hasSearched.value = true;
  } catch (err) {
    // 错误信息由 store 维护
  }
};

const subscribeFromSearch = async (feed: SubscriptionSearchResultDto) => {
  try {
    if (isSubscribed(feed)) {
      return;
    }
    await subscriptionsStore.addSubscription(feed.url);
    feed.subscribed = true;
    feed.subscriberCount = (feed.subscriberCount ?? 0) + 1;
  } catch (err) {
    // 错误信息由 store 维护
  }
};

const opmlFile = ref<File | null>(null);
const opmlFileInput = ref<HTMLInputElement | null>(null);
const opmlPreviewFeeds = ref<OpmlPreviewFeedView[]>([]);
const opmlWarnings = ref<string[]>([]);
const opmlError = ref<string | null>(null);
const opmlPreviewLoading = ref(false);
const opmlConfirmLoading = ref(false);
const opmlConfirmResult = ref<OpmlImportResultDto | null>(null);
const showOpmlModal = ref(false);
const remainingQuota = ref(0);

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
  router.push({ name: 'feed', params: { feedId } });
};

const displayTitle = (item: SubscriptionListItemDto | SubscriptionSearchResultDto) =>
  item.title || item.siteUrl || item.url;

const displaySiteUrl = (item: SubscriptionListItemDto | SubscriptionSearchResultDto) => item.siteUrl || item.url;

const formatRecentText = (value?: string | null) => (value ? formatRelativeTime(value) : '暂无记录');

const subscriptionIssues = computed(() => {
  const issues: Record<string, { label: string; tooltip: string }> = {};
  items.value.forEach((item) => {
    const failures = item.failureCount ?? 0;
    const error = item.fetchError?.trim();
    if (!failures && !error) {
      return;
    }
    const parts: string[] = [];
    if (failures > 0) {
      parts.push(`连续失败 ${failures} 次`);
    }
    if (error) {
      parts.push(`最近错误：${error}`);
    }
    issues[item.feedId] = {
      label: parts.length ? `抓取异常 · ${parts.join(' · ')}` : '抓取异常 · 请稍后再试',
      tooltip: ['抓取异常', failures > 0 ? `连续失败 ${failures} 次` : null, error ? `最近错误：${error}` : null]
        .filter(Boolean)
        .join('\n')
    };
  });
  return issues;
});

const handleOpmlFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const [file] = target.files ?? [];
  opmlFile.value = file ?? null;
  opmlError.value = null;
};

const handlePreviewOpml = async () => {
  if (!opmlFile.value) {
    opmlError.value = '请选择一个 OPML 文件';
    return;
  }
  opmlPreviewLoading.value = true;
  opmlError.value = null;
  opmlConfirmResult.value = null;
  try {
    const result = await subscriptionsStore.previewOpmlImport(opmlFile.value);
    opmlWarnings.value = result.warnings ?? [];
    remainingQuota.value = result.remainingQuota ?? 0;
    opmlPreviewFeeds.value = result.feeds.map((feed) => ({
      ...feed,
      selected: !feed.alreadySubscribed && feed.errors.length === 0
    }));
    showOpmlModal.value = true;
  } catch (error) {
    const message = error instanceof Error ? error.message : '解析 OPML 失败';
    opmlError.value = message;
    opmlPreviewFeeds.value = [];
    opmlWarnings.value = [];
  } finally {
    opmlPreviewLoading.value = false;
  }
};

const selectableFeeds = computed(() =>
  opmlPreviewFeeds.value.filter((feed) => !feed.alreadySubscribed && feed.errors.length === 0)
);
const selectedCount = computed(() => selectableFeeds.value.filter((feed) => feed.selected).length);
const hasSelectedFeeds = computed(() => selectedCount.value > 0);
const isAllSelected = computed(
  () => selectableFeeds.value.length > 0 && selectableFeeds.value.every((feed) => feed.selected)
);

const toggleSelectAll = () => {
  const selectAll = !isAllSelected.value;
  opmlPreviewFeeds.value = opmlPreviewFeeds.value.map((feed) => {
    if (feed.alreadySubscribed || feed.errors.length > 0) {
      return feed;
    }
    return { ...feed, selected: selectAll };
  });
};

const resetOpmlInput = () => {
  opmlFile.value = null;
  if (opmlFileInput.value) {
    opmlFileInput.value.value = '';
  }
};

const closeOpmlModal = () => {
  showOpmlModal.value = false;
};

const handleSuccessClose = () => {
  closeOpmlModal();
  opmlConfirmResult.value = null;
};

const handleConfirmOpml = async () => {
  if (!hasSelectedFeeds.value) {
    opmlError.value = '请至少选择一个订阅项';
    return;
  }
  if (selectedCount.value > remainingQuota.value) {
    opmlError.value = '超过剩余额度，请取消部分订阅后再试';
    return;
  }
  opmlConfirmLoading.value = true;
  opmlError.value = null;
  try {
    const payload = opmlPreviewFeeds.value.map((feed) => ({
      feedUrl: feed.feedUrl,
      title: feed.title,
      siteUrl: feed.siteUrl,
      avatar: feed.avatar ?? undefined,
      selected: feed.selected
    }));
    const result = await subscriptionsStore.confirmOpmlImport(payload);
    opmlConfirmResult.value = result;
    await subscriptionsStore.fetchSubscriptions();
    opmlPreviewFeeds.value = [];
    opmlWarnings.value = [];
    resetOpmlInput();
  } catch (error) {
    const message = error instanceof Error ? error.message : '导入失败';
    opmlError.value = message;
  } finally {
    opmlConfirmLoading.value = false;
  }
};

const translateSkippedReason = (reason: string) => {
  const mapping: Record<string, string> = {
    ALREADY_SUBSCRIBED: '已订阅',
    INVALID_FEED: '无效订阅',
    DUPLICATE: '重复条目',
    FAILED: '导入失败'
  };
  return mapping[reason] ?? reason;
};

onMounted(() => {
  if (!items.value.length) {
    refresh();
  }
});
</script>
