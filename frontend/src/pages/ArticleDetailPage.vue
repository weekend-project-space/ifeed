<template>
  <div class="text-text min-h-screen">
    <!-- 骨架屏：M3 表面容器脉冲 -->
    <div v-if="articleLoading" class="mx-auto max-w-screen-md px-4 py-12 animate-pulse sm:px-6">
      <div class="space-y-6 mb-12">
        <div class="h-10 rounded-2xl bg-surface-container w-3/4"></div>
        <div class="h-6 rounded-xl bg-surface-container/60 w-1/2"></div>
        <div class="flex items-center gap-4">
          <div class="h-11 w-11 rounded-full bg-surface-container"></div>
          <div class="space-y-2 flex-1">
            <div class="h-4 rounded bg-surface-container w-32"></div>
            <div class="h-3 rounded bg-surface-container/60 w-24"></div>
          </div>
        </div>
      </div>
      <div class="space-y-4">
        <div class="h-4 rounded bg-surface-container"></div>
        <div class="h-4 rounded bg-surface-container"></div>
        <div class="h-4 rounded bg-surface-container w-5/6"></div>
        <div class="my-8 h-64 rounded-3xl bg-surface-container/60"></div>
      </div>
    </div>

    <!-- 错误横幅 -->
    <div v-else-if="articleError" class="mx-auto max-w-screen-md px-4 py-12 sm:px-6">
      <div class="rounded-3xl bg-red-500/10 p-6">
        <div class="flex items-start gap-4">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-red-500/20 text-red-600 dark:text-red-400">
            <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                    clip-rule="evenodd"/>
            </svg>
          </div>
          <div class="flex-1">
            <h3 class="text-sm font-semibold text-red-900 dark:text-red-200">加载失败</h3>
            <p class="mt-1 text-xs text-red-700 dark:text-red-300">{{ articleError }}</p>
            <button @click="articleQuery.refetch()"
                    class="mt-3 inline-flex h-8 items-center rounded-full bg-red-600 px-4 text-xs font-semibold text-white shadow-xs hover:bg-red-700 transition">
              重试
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 文章正文区域 -->
    <article v-else-if="article" class="mx-auto max-w-screen-md px-4 sm:px-6">
      <header class="pt-10 pb-6 sm:pt-12">
        <!-- 标题 -->
        <h1 class="mb-6 font-serif text-2xl font-bold leading-tight tracking-tight text-text break-words sm:text-3xl lg:text-4xl">
          <a :href="article.link" v-text="article.title" target="_blank" rel="noopener noreferrer" class="hover:text-primary transition-colors"></a>
        </h1>

        <!-- 来源与元信息 -->
        <div class="mb-6 flex flex-wrap items-center justify-between gap-4">
          <router-link v-if="article.feedId" :to="'/feed/' + article.feedId" class="group flex items-center gap-3">
            <img v-if="article.feedAvatar" :src="article.feedAvatar" :alt="article.feedTitle"
                 class="h-11 w-11 rounded-full object-cover shadow-2xs"
                 @error="(e) => (e.target as HTMLImageElement).style.display = 'none'"/>
            <div v-else
                 class="flex h-11 w-11 items-center justify-center rounded-full bg-surface-container-high text-base font-bold text-text">
              {{ article.feedTitle?.charAt(0).toUpperCase() }}
            </div>
            <div class="flex flex-col">
              <span class="text-xs font-semibold text-text group-hover:text-primary transition-colors sm:text-sm">{{ article.feedTitle }}</span>
              <div class="flex items-center gap-1.5 text-[11px] text-text-secondary">
                <span>{{ article.timeAgo }}</span>
                <span>•</span>
                <span>{{ readingTime }} 分钟阅读</span>
              </div>
            </div>
          </router-link>

          <!-- 彻底防抖订阅按钮：锁定宽度 w-20 (80px)，精简文案 -->
          <button
              v-if="article.feedId && feedDetail"
              type="button"
              class="group relative inline-flex h-8.5 w-20 shrink-0 items-center justify-center gap-1.5 rounded-full text-xs font-semibold shadow-xs transition-all duration-200 select-none active:scale-95 disabled:cursor-not-allowed disabled:opacity-40"
              :class="[
                feedDetail.subscribed
                  ? 'border border-outline/30 bg-surface text-text hover:border-red-500/40 hover:bg-red-500/10 hover:text-red-600 dark:hover:text-red-400'
                  : 'bg-primary text-white hover:brightness-105 hover:shadow-sm'
              ]"
              :disabled="subscriptionSubmitting || feedQuery.isPending.value"
              @click="toggleFeedSubscription">
            <!-- 正在处理态 -->
            <template v-if="subscriptionSubmitting">
              <svg class="h-3.5 w-3.5 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M21 12a9 9 0 1 1-6.219-8.56" />
              </svg>
            </template>

            <!-- 已订阅态：默认对勾，Hover时变叉 -->
            <template v-else-if="feedDetail.subscribed">
              <span class="inline-flex items-center gap-1 group-hover:hidden">
                <svg class="h-3.5 w-3.5 text-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                <span>已订</span>
              </span>
              <span class="hidden items-center gap-1 group-hover:inline-flex">
                <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
                <span>退订</span>
              </span>
            </template>

            <!-- 未订阅态 -->
            <template v-else>
              <svg class="h-3.5 w-3.5 stroke-[2.5]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              <span>订阅</span>
            </template>
          </button>
        </div>

        <p v-if="subscriptionError" class="mb-3 text-xs text-red-600 dark:text-red-400" role="alert">
          {{ subscriptionError }}
        </p>

        <!-- 工具栏：标签与操作 -->
        <div class="flex flex-wrap items-center justify-between gap-3 border-y border-outline/10 py-3">
          <div v-if="article.tags?.length" class="flex flex-wrap gap-1.5">
            <button v-for="tag in article.tags.slice(0, 3)" :key="tag" type="button"
                    class="rounded-full bg-surface-container/60 px-3 py-1 text-[11px] font-medium text-text-secondary transition hover:bg-surface-container hover:text-text"
                    @click="handleTagClick(tag)">
              #{{ tag }}
            </button>
          </div>
          <div v-else></div>

          <div class="flex items-center gap-1">
            <template v-if="headerActionExists && route.name === 'article'">
              <Teleport to="#header-action">
                <button
                    class="flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
                    :class="linkCopied ? 'text-green-600 dark:text-green-400' : ''"
                    title="复制链接"
                    aria-label="复制文章链接"
                    @click="copyArticleLink">
                  <svg v-if="!linkCopied" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                       stroke="currentColor">
                    <rect x="8" y="8" width="13" height="13" rx="2"
                          stroke-width="1.8"/>
                    <path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3"
                          stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>

                  <svg v-else class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                       stroke="currentColor">
                    <path d="M5 12.5l4.5 4.5L19 7"
                          stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
                <ArticleActions :article-id="article.id" :collected="article.collected ?? false" :liked="article.liked" :folder-id="article.folderId" />
              </Teleport>
            </template>
            <ArticleActions v-if="!headerActionExists" :article-id="article.id" :collected="article.collected ?? false" :liked="article.liked" :folder-id="article.folderId" />
          </div>
        </div>
      </header>

      <p v-if="historyMutation.isError.value" role="alert" class="py-2 text-xs text-red-600">
        阅读记录保存失败 <button class="underline ml-1 font-semibold" :disabled="historyMutation.isPending.value" @click="historyMutation.mutate(articleId)">重试</button>
      </p>

      <!-- MD3 黏性导航分段 (Content / Summary / Mindmap) -->
      <div v-if="article.summary || article.mindMap || article.requiresUpgrade"
           class="sticky top-0 z-20 border-b border-outline/10 bg-surface/90 py-2.5 backdrop-blur-md">
        <nav class="flex gap-1.5" role="tablist">
          <button
              @click="handleTabSwitch('content')"
              class="inline-flex h-8 items-center rounded-full px-4 text-xs font-medium transition select-none"
              :class="activeMainTab === 'content'
                ? 'bg-primary/15 font-semibold text-primary'
                : 'text-text-secondary hover:bg-surface-container hover:text-text'"
              role="tab"
              :aria-selected="activeMainTab === 'content'"
          >
            正文
          </button>
          <button
              v-if="article.summary || article.requiresUpgrade"
              @click="handleTabSwitch('summary')"
              class="inline-flex h-8 items-center rounded-full px-4 text-xs font-medium transition select-none"
              :class="activeMainTab === 'summary'
                ? 'bg-primary/15 font-semibold text-primary'
                : 'text-text-secondary hover:bg-surface-container hover:text-text'"
              role="tab"
              :aria-selected="activeMainTab === 'summary'"
          >
            AI 摘要
          </button>
          <button
              v-if="article.mindMap || article.requiresUpgrade"
              @click="handleTabSwitch('mindmap')"
              class="inline-flex h-8 items-center rounded-full px-4 text-xs font-medium transition select-none"
              :class="activeMainTab === 'mindmap'
                ? 'bg-primary/15 font-semibold text-primary'
                : 'text-text-secondary hover:bg-surface-container hover:text-text'"
              role="tab"
              :aria-selected="activeMainTab === 'mindmap'"
          >
            思维导图
          </button>
        </nav>
      </div>

      <!-- Tab 主体内容 -->
      <div class="py-8">
        <!-- 1. 正文面板 -->
        <div v-show="activeMainTab === 'content'" role="tabpanel">
          <media-attachment v-if="article.enclosure" mode="global" :url="article.enclosure"
                            :type="article.enclosureType"
                            :title="article.title" :artist="article.feedTitle || article.author" :track-id="article.id"
                            :cover-image="article.thumbnail" class="mb-8 rounded-2xl overflow-hidden"/>

          <div class="prose-custom">
            <div v-if="article.content" ref="articleContentRef" class="article-content" v-html="article.content"></div>
            <div v-else-if="!article.enclosure" class="py-20 text-center">
              <svg class="mx-auto mb-2.5 h-10 w-10 text-text-secondary/40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                      d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              <p class="text-xs text-text-secondary">暂无正文内容</p>
            </div>
          </div>

          <footer class="mt-14 border-t border-outline/10 pt-6">
            <div class="flex items-center justify-between text-xs text-text-secondary">
              <a v-if="article.link" :href="article.link" target="_blank" rel="noopener noreferrer"
                 class="inline-flex items-center gap-1.5 font-semibold text-text hover:text-primary transition-colors">
                <span>查看原文</span>
                <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
                </svg>
              </a>
              <span>{{ article.timeAgo }}</span>
            </div>
          </footer>
        </div>

        <!-- 2. AI 摘要面板 -->
        <div v-show="activeMainTab === 'summary'" role="tabpanel">
          <div v-if="isEnriching" class="flex flex-col items-center justify-center py-20 gap-3">
            <svg class="h-6 w-6 animate-spin text-text-secondary" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
            </svg>
            <p class="text-xs text-text-secondary">AI 正在生成精炼摘要...</p>
          </div>

          <div v-else-if="article.summary" class="prose-custom space-y-4">
            <div class="flex items-start gap-3 rounded-2xl bg-surface-container/50 p-4">
              <svg class="mt-0.5 h-4 w-4 shrink-0 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              <div class="text-xs leading-relaxed text-text-secondary">
                <strong class="font-semibold text-text">智能推荐摘要</strong>
                <p class="mt-0.5">由 AI 语言模型自动抓取并总结核心提炼，仅供参考。</p>
              </div>
            </div>
            <div v-html="article.summary" class="article-content"></div>
          </div>

          <div v-else class="py-20 text-center">
            <div class="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-surface-container/80">
              <svg class="h-6 w-6 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
              </svg>
            </div>
            <h3 class="text-sm font-bold text-text mb-1">高级智能摘要</h3>
            <p class="text-xs text-text-secondary mb-5">此功能仅限会员套餐使用</p>
            <router-link to="/upgrade"
                         class="inline-flex h-9 items-center justify-center rounded-full bg-primary px-5 text-xs font-semibold text-white shadow-xs hover:brightness-105 transition">
              升级会员体验
            </router-link>
          </div>
        </div>

        <!-- 3. 思维导图面板 -->
        <div v-show="activeMainTab === 'mindmap'" role="tabpanel">
          <div v-if="isEnriching" class="flex flex-col items-center justify-center py-20 gap-3">
            <svg class="h-6 w-6 animate-spin text-text-secondary" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
            </svg>
            <p class="text-xs text-text-secondary">AI 正在生成思维导图结构...</p>
          </div>

          <div v-else-if="mindmapMarkdown" class="overflow-hidden rounded-3xl bg-surface-container/30 shadow-xs">
            <iframe ref="mindmapFrame" src="/md2mindmap.html" class="w-full"
                    :style="`max-height: 500rem; height: ${mindmapHeight}rem;`" frameborder="0" @load="sendMindmapData"/>
          </div>

          <div v-else class="py-20 text-center">
            <div class="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-surface-container/80">
              <svg class="h-6 w-6 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
              </svg>
            </div>
            <h3 class="text-sm font-bold text-text mb-1">文章思维导图</h3>
            <p class="text-xs text-text-secondary mb-5">可视化知识结构仅限会员使用</p>
            <router-link to="/upgrade"
                         class="inline-flex h-9 items-center justify-center rounded-full bg-primary px-5 text-xs font-semibold text-white shadow-xs hover:brightness-105 transition">
              升级会员体验
            </router-link>
          </div>
        </div>
      </div>
    </article>

    <!-- 404 状态 -->
    <div v-else class="flex flex-col items-center justify-center py-28 text-center">
      <div class="mx-auto mb-3 flex h-16 w-16 items-center justify-center rounded-full bg-surface-container/80">
        <svg class="h-7 w-7 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
      </div>
      <h3 class="text-base font-bold text-text mb-1">未找到文章</h3>
      <p class="text-xs text-text-secondary">该文章可能已被删除或源已下线</p>
    </div>

    <!-- 底部推荐阅读 -->
    <div v-if="detailsItems.length > 0" class="mt-12 rounded-t-[32px] bg-surface-container/25 py-12">
      <div class="mx-auto max-w-screen-md px-4 sm:px-6">
        <h2 class="mb-6 text-xl font-bold tracking-tight text-text">推荐阅读</h2>
        <article-card-list :items="detailsItems" :loading="detailsLoading" key-field="id" compact/>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, watch } from 'vue';
import { useMutation, useQueryClient } from '@tanstack/vue-query';
import { useRoute, useRouter } from 'vue-router';
import { useArticlesStore } from '../stores/articles/articles';
import ArticleActions from '../components/ArticleActions.vue';
import { useHistoryMutation } from '../queries/articleActions';
import MediaAttachment from "../components/MediaAttachment.vue";
import ArticleCardList from "../components/ArticleCardList.vue";
import { useAuthStore } from "../stores/auth";
import { md2html } from "../utils/markdown";
import { normalizeArticle, type ArticleDetail } from '../stores/articles/types';
import { articleQueryKey, useArticleQuery, useArticleRecommendationsQuery } from '../queries/article';
import { useFeedQuery } from '../queries/feed';
import { useDocumentTitle } from '../composables/documentTitle';
import { useSubscriptionMutation } from '../queries/subscriptions';

defineOptions({ name: 'ArticleDetailPage' });

interface Props {
  id: string;
  tab: 'content' | 'summary' | 'mindmap';
}

const props = defineProps<Props>();
const router = useRouter();
const route = useRoute();
const articlesStore = useArticlesStore();
const authStore = useAuthStore();
const queryClient = useQueryClient();

const pageActive = ref(false);
const articleContentRef = ref<HTMLElement | null>(null);
const mindmapFrame = ref<HTMLIFrameElement | null>(null);
type MainTab = 'content' | 'summary' | 'mindmap';
const activeMainTab = computed(() => props.tab);
const mindmapMarkdown = ref('');
const linkCopied = ref(false);
const lazyLoadObserver = ref<IntersectionObserver | null>(null);
const headerActionExists = ref(false);

const AVERAGE_WORDS_PER_MINUTE = 200;

const articleId = computed(() => props.id ?? '');
const userId = computed(() => authStore.user?.userId ?? 'anonymous');
const currentArticleQueryKey = computed(() => articleQueryKey(userId.value, articleId.value));
const articleQuery = useArticleQuery(articleId);
const recommendationsQuery = useArticleRecommendationsQuery(articleId);
const article = computed(() => articleQuery.data.value ?? null);
const articleFeedId = computed(() => article.value?.feedId ?? null);
const feedQuery = useFeedQuery(articleFeedId);
const feedDetail = computed(() => feedQuery.data.value ?? null);
const articleLoading = computed(() => articleQuery.isPending.value);
const articleError = computed(() => {
  const error = articleQuery.error.value;
  return error instanceof Error ? error.message : error ? '文章加载失败，请稍后重试' : '';
});
const detailsItems = computed(() => (recommendationsQuery.data.value ?? []).map(normalizeArticle));
const detailsLoading = computed(() => recommendationsQuery.isPending.value);
useDocumentTitle(() => article.value?.title);

const historyMutation = useHistoryMutation('record');
const readingKey = computed(() => pageActive.value && authStore.isAuthenticated && article.value?.id === articleId.value
    ? `${userId.value}:${articleId.value}` : '');
watch(readingKey, (key) => {
  historyMutation.reset();
  if (key) historyMutation.mutate(articleId.value);
});

const mindmapHeight = computed(() => {
  if (!mindmapMarkdown.value) return 50;
  const lines = mindmapMarkdown.value.split('\n').length;
  return Math.max(50, Math.min(lines * 1.1, 500));
});

const readingTime = computed(() => {
  if (!article.value?.content) return 0;
  let text = article.value.content.replace(/<[^>]*>/g, '');
  text = text.replace(/```[\s\S]*?```/g, '');
  text = text.replace(/\|[\s\S]*?\|/g, '');
  const chineseChars = text.match(/[\u4e00-\u9fa5]/g)?.length || 0;
  const englishWords = text.match(/[a-zA-Z]+/g)?.length || 0;
  const minutes = Math.ceil((chineseChars / 300 + englishWords / AVERAGE_WORDS_PER_MINUTE));
  return Math.max(1, minutes);
});

const sendMindmapData = () => {
  if (!mindmapFrame.value || !mindmapMarkdown.value) return;
  try {
    const iframe = mindmapFrame.value.contentWindow;
    if (iframe) {
      const cleanedMarkdown = mindmapMarkdown.value
          .replaceAll('\\\\\\\\n', '\n')
          .replaceAll('\\\\n', '\n')
          .replaceAll('\n\n', '\n');
      iframe.postMessage(cleanedMarkdown, '*');
    }
  } catch (error) {
    console.error('Failed to send mindmap data:', error);
  }
};

const handleTabSwitch = async (tab: MainTab) => {
  const query = { ...route.query };
  if (tab === 'content') {
    delete query.tab;
  } else {
    query.tab = tab;
  }
  if (props.tab !== tab) {
    await router.push({ query });
  }
  if (tab === 'content') return;
  if (tab === 'summary' && article.value?.summary) return;
  if (tab === 'mindmap' && mindmapMarkdown.value) return;
  if (!authStore.isAuthenticated || authStore.user?.currentPlan === 'FREE') return;
  if (isEnriching.value || enrichMutation.isPending.value) return;

  try {
    const res = await enrichMutation.mutateAsync(articleId.value);
    mindmapMarkdown.value = res.mindMap ?? '';
    if (tab === 'mindmap' && mindmapMarkdown.value) {
      await nextTick();
      setTimeout(sendMindmapData, 200);
    }
  } catch (err) {
    console.warn('AI 增强获取失败', err);
  }
};

const enrichMutation = useMutation({
  mutationFn: (id: string) => articlesStore.enrich(id),
  onSuccess: (result) => {
    queryClient.setQueryData<ArticleDetail>(currentArticleQueryKey.value, (previous) => previous ? {
      ...previous,
      summary: md2html(result.summary),
      mindMap: result.mindMap,
    } : previous);
  },
});
const isEnriching = computed(() => enrichMutation.isPending.value);

const subscriptionMutation = useSubscriptionMutation();
const subscriptionSubmitting = subscriptionMutation.isPending;
const subscriptionError = ref('');
const toggleFeedSubscription = async () => {
  const feed = feedDetail.value;
  if (!feed || subscriptionSubmitting.value) return;
  subscriptionError.value = '';
  try {
    await subscriptionMutation.mutateAsync({
      feedId: feed.feedId, feedUrl: feed.url, subscribed: feed.subscribed,
    });
  } catch (error) {
    subscriptionError.value = error instanceof Error ? error.message : '订阅操作失败，请重试';
  }
};

const handleTagClick = (tag: string) => {
  if (!tag) return;
  router.push({ name: 'feedsSubscriptions', query: { tags: tag.toLowerCase() } });
};

const copyArticleLink = async () => {
  try {
    const url = window.location.href;
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(url);
    } else {
      const textArea = document.createElement('textarea');
      textArea.value = url;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      textArea.style.top = '-999999px';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      try {
        document.execCommand('copy');
        textArea.remove();
      } catch (err) {
        textArea.remove();
        throw err;
      }
    }
    linkCopied.value = true;
    setTimeout(() => {
      linkCopied.value = false;
    }, 2000);
  } catch (err) {
    console.error('复制链接失败:', err);
  }
};

const setupLazyLoading = () => {
  const container = articleContentRef.value;
  if (!container) return;
  if (lazyLoadObserver.value) lazyLoadObserver.value.disconnect();
  const images = Array.from(container.querySelectorAll('img')) as HTMLImageElement[];
  if ('loading' in HTMLImageElement.prototype) {
    images.forEach((img) => { img.loading = 'lazy'; });
    return;
  }
  lazyLoadObserver.value = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        const img = entry.target as HTMLImageElement;
        const originalSrc = img.getAttribute('data-lazy-src');
        if (originalSrc) {
          img.src = originalSrc;
          img.removeAttribute('data-lazy-src');
          lazyLoadObserver.value?.unobserve(img);
        }
      }
    });
  }, { rootMargin: '50px 0px', threshold: 0.01 });

  images.forEach((img) => {
    if (img.src && !img.hasAttribute('data-lazy-src')) {
      img.setAttribute('data-lazy-src', img.src);
      img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1 1"%3E%3C/svg%3E';
      lazyLoadObserver.value?.observe(img);
    }
  });
};

onMounted(async () => {
  headerActionExists.value = !!document.querySelector('#header-action');
  pageActive.value = true;
});

onActivated(() => { pageActive.value = true; });
onDeactivated(() => {
  pageActive.value = false;
  linkCopied.value = false;
});
onBeforeUnmount(() => {
  linkCopied.value = false;
  if (lazyLoadObserver.value) {
    lazyLoadObserver.value.disconnect();
    lazyLoadObserver.value = null;
  }
  pageActive.value = false;
});

watch([() => article.value?.id, () => article.value?.content, () => article.value?.mindMap], async ([id], previous) => {
  if (!article.value) return;
  mindmapMarkdown.value = article.value.mindMap ?? '';
  if (id !== previous?.[0]) window.scrollTo({ top: 0, behavior: 'auto' });
  await nextTick();
  setupLazyLoading();
}, { immediate: true });

watch(activeMainTab, async (newTab) => {
  if (newTab === 'mindmap' && mindmapMarkdown.value) {
    await nextTick();
    setTimeout(sendMindmapData, 200);
  }
});
</script>

<style scoped>
@reference "../styles/tailwind.css";

.prose-custom {
  @apply text-text;
}

.article-content {
  font-family: charter, Georgia, Cambria, "Times New Roman", Times, serif;
  @apply text-base leading-relaxed text-text break-words sm:text-lg sm:leading-8;
}

.article-content :deep(p) {
  @apply mb-5 leading-relaxed text-text;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  @apply font-bold text-text mt-8 mb-3 tracking-tight;
}

.article-content :deep(h1) { @apply text-2xl leading-tight; }
.article-content :deep(h2) { @apply text-xl leading-snug; }
.article-content :deep(h3) { @apply text-lg leading-snug; }

.article-content :deep(ul),
.article-content :deep(ol) {
  @apply mb-5 pl-6 space-y-2 text-text;
}

.article-content :deep(li) {
  @apply leading-relaxed;
}

.article-content :deep(blockquote) {
  @apply border-l-4 border-primary/40 bg-surface-container/30 pl-4 py-2 my-5 rounded-r-xl italic text-text-secondary;
}

.article-content :deep(pre) {
  @apply bg-surface-container/60 p-4 my-5 rounded-2xl text-xs overflow-x-auto max-w-full text-text;
}

.article-content :deep(code) {
  @apply bg-surface-container px-1.5 py-0.5 text-xs font-mono rounded-md text-primary break-words;
}

.article-content :deep(pre code) {
  @apply bg-transparent p-0 text-xs text-text break-normal;
}

.article-content :deep(img) {
  @apply w-full h-auto my-6 rounded-2xl max-w-full shadow-xs;
}

.article-content :deep(a) {
  @apply text-primary underline underline-offset-2 hover:opacity-80;
}

.article-content :deep(hr) {
  @apply border-0 h-px bg-outline/20 my-8;
}

.article-content :deep(table) {
  @apply w-full border-collapse my-5 text-xs;
}

.article-content :deep(th),
.article-content :deep(td) {
  @apply border border-outline/20 px-3.5 py-2.5 text-left;
}

.article-content :deep(thead th) {
  @apply bg-surface-container font-semibold text-text;
}
</style>