<template>
  <div class="text-text min-h-screen">
    <!-- 骨架屏：M3 表面容器脉冲 -->
    <div v-if="articleLoading" class="mx-auto max-w-screen-md px-4 py-6 animate-pulse sm:px-6 sm:py-8">
      <div class="space-y-5 mb-8">
        <div class="h-10 rounded-2xl bg-surface-container w-3/4"></div>
        <div class="h-6 rounded-xl bg-surface-container/60 w-1/2"></div>
        <div class="flex items-center gap-4">
          <div class="h-9 w-9 rounded-full bg-surface-container"></div>
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
          <div
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-red-500/20 text-red-600 dark:text-red-400">
            <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                    clip-rule="evenodd"/>
            </svg>
          </div>
          <div class="flex-1">
            <h3 class="text-sm font-semibold text-red-900 dark:text-red-200">{{ tr('common.loadFailed') }}</h3>
            <p class="mt-1 text-xs text-red-700 dark:text-red-300">{{ articleError }}</p>
            <button @click="articleQuery.refetch()"
                    class="mt-3 inline-flex h-8 items-center rounded-full bg-red-600 px-4 text-xs font-semibold text-white shadow-xs hover:bg-red-700 transition">
              {{ tr('common.retry') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 文章正文区域 -->
    <article v-else-if="article" class="mx-auto max-w-screen-md px-4 sm:px-6">
      <header class="pt-6 pb-5 sm:pt-8 sm:pb-6">
        <!-- 标题 -->
        <h1 class="mb-5 text-2xl font-bold leading-snug tracking-tight text-text break-words sm:text-3xl lg:text-[32px]">
          <a :href="article.link" v-text="article.title" target="_blank" rel="noopener noreferrer"
             class="hover:text-primary transition-colors"></a>
        </h1>

        <!-- 来源与元信息 -->
        <div class="flex items-center justify-between gap-3">
          <router-link v-if="article.feedId" :to="'/feed/' + article.feedId" class="group flex min-w-0 flex-1 items-center gap-2.5">
            <img v-if="article.feedAvatar && !feedAvatarFailed" :src="article.feedAvatar" :alt="article.feedTitle"
                 class="h-9 w-9 shrink-0 rounded-full object-cover"
                 @error="feedAvatarFailed = true"/>
            <div v-else
                 class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-surface-container-high text-sm font-semibold text-text">
              {{ article.feedTitle?.charAt(0).toUpperCase() }}
            </div>
            <div class="flex min-w-0 flex-col gap-0.5">
              <span class="truncate text-[13px] font-semibold text-text group-hover:text-primary transition-colors">{{
                  article.feedTitle
                }}</span>
              <div class="flex flex-wrap items-center gap-x-1.5 text-xs text-text-secondary">
                <span>{{ article.timeAgo }}</span>
                <template v-if="readingTime > 0">
                  <span aria-hidden="true">·</span>
                  <span>{{ tr('articleDetailPage.readingTime', { count: readingTime }) }}</span>
                </template>
              </div>
            </div>
          </router-link>
          <div v-else class="flex flex-wrap items-center gap-x-1.5 text-xs text-text-secondary">
            <span>{{ article.feedTitle }}</span>
            <span aria-hidden="true">·</span>
            <span>{{ article.timeAgo }}</span>
            <template v-if="readingTime > 0">
              <span aria-hidden="true">·</span>
              <span>{{ tr('articleDetailPage.readingTime', { count: readingTime }) }}</span>
            </template>
          </div>

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
              <svg class="h-3.5 w-3.5 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2.5">
                <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
              </svg>
            </template>

            <!-- 已订阅态：默认对勾，Hover时变叉 -->
            <template v-else-if="feedDetail.subscribed">
              <span class="inline-flex items-center gap-1 group-hover:hidden">
                <svg class="h-3.5 w-3.5 text-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
                <span>{{ tr('common.subscribedShort') }}</span>
              </span>
              <span class="hidden items-center gap-1 group-hover:inline-flex">
                <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                     stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
                <span>{{ tr('common.unsubscribeShort') }}</span>
              </span>
            </template>

            <!-- 未订阅态 -->
            <template v-else>
              <svg class="h-3.5 w-3.5 stroke-[2.5]" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              <span>{{ tr('common.subscribe') }}</span>
            </template>
          </button>
        </div>

        <p v-if="subscriptionError" class="mt-3 text-xs text-red-600 dark:text-red-400" role="alert">
          {{ subscriptionError }}
        </p>

        <!-- 无标签且操作已挂载到 header 时，不保留空白工具栏 -->
        <div v-if="article.tags?.length || !headerActionExists" class="mt-4 flex flex-wrap items-center justify-between gap-3">
          <div v-if="article.tags?.length" class="flex min-w-0 flex-wrap gap-1.5">
            <button v-for="tag in article.tags.slice(0, 3)" :key="tag" type="button"
                    class="max-w-full break-words rounded-full bg-surface-container/60 px-2.5 py-1 text-xs text-text-secondary transition-colors hover:text-primary"
                    @click="handleTagClick(tag)">
              #{{ tag }}
            </button>
          </div>
          <ArticleActions v-if="!headerActionExists" :article-id="article.id" :collected="article.collected ?? false"
                          :liked="article.liked" :folder-id="article.folderId" dense/>
        </div>
        <Teleport v-if="headerActionExists && route.name === 'article'" to="#header-action">
          <ArticleActions :article-id="article.id" :collected="article.collected ?? false" :liked="article.liked"
                          :folder-id="article.folderId" dense/>
        </Teleport>
      </header>

      <p v-if="historyMutation.isError.value" role="alert" class="py-2 text-xs text-red-600">
        {{ tr('articleDetailPage.historySaveFailed') }}
        <button class="ml-1 font-semibold text-primary" :disabled="historyMutation.isPending.value"
                @click="historyMutation.mutate(articleId)">{{ tr('common.retry') }}
        </button>
      </p>

      <!-- 吸顶位置随全局 header 显隐变化 -->
      <div v-if="mainTabs.length > 1"
           class="article-tabs sticky z-20 border-b border-outline/10 bg-surface/95 backdrop-blur-md">
        <nav class="flex gap-3 sm:gap-5" role="tablist" :aria-label="tr('articleDetailPage.articleViews')">
          <button
              v-for="tab in mainTabs" :id="`article-tab-${tab.id}`" :key="tab.id" type="button"
              @click="handleTabSwitch(tab.id)" @keydown="handleTabKeydown($event, tab.id)"
              class="relative inline-flex h-11 shrink-0 items-center px-2 text-[13px] font-medium transition-colors select-none after:absolute after:inset-x-2 after:-bottom-px after:h-0.5 after:rounded-t-full after:transition-colors sm:text-sm"
              :class="activeMainTab === tab.id
                ? 'text-primary after:bg-primary'
                : 'text-text-secondary after:bg-transparent hover:text-primary'"
              role="tab"
              :aria-selected="activeMainTab === tab.id" :aria-controls="`article-panel-${tab.id}`"
              :tabindex="activeMainTab === tab.id ? 0 : -1"
          >
            {{ tab.label }}
          </button>
        </nav>
      </div>

      <!-- Tab 主体内容 -->
      <div class="pb-6 pt-5 sm:pt-6">
        <!-- 1. 正文面板 -->
        <div v-show="activeMainTab === 'content'" id="article-panel-content" role="tabpanel"
             :aria-labelledby="mainTabs.length > 1 ? 'article-tab-content' : undefined" tabindex="0">
          <media-attachment v-if="article.enclosure" mode="global" :url="article.enclosure"
                            :type="article.enclosureType"
                            :title="article.title" :artist="article.feedTitle || article.author" :track-id="article.id"
                            :cover-image="article.thumbnail" class="mb-8 rounded-2xl overflow-hidden"/>

          <div class="prose-custom">
            <div v-if="article.content" ref="articleContentRef" class="article-content" v-html="article.content"></div>
            <div v-else-if="!article.enclosure" class="py-20 text-center">
              <svg class="mx-auto mb-2.5 h-10 w-10 text-text-secondary/40" fill="none" stroke="currentColor"
                   viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                      d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              <p class="text-xs text-text-secondary">{{ tr('articleDetailPage.noBody') }}</p>
            </div>
          </div>

          <footer class="mt-10 border-t border-outline/10 pt-5">
            <div class="flex items-center justify-between text-xs text-text-secondary">
              <a v-if="article.link" :href="article.link" target="_blank" rel="noopener noreferrer"
                 class="inline-flex items-center gap-1.5 font-semibold text-text hover:text-primary transition-colors">
                <span>{{ tr('articleDetailPage.viewOriginal') }}</span>
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
        <div v-show="activeMainTab === 'summary'" id="article-panel-summary" role="tabpanel"
             :aria-labelledby="mainTabs.some(tab => tab.id === 'summary') ? 'article-tab-summary' : undefined" tabindex="0">
          <div v-if="isEnriching && !article.summary" role="status" class="flex flex-col items-center justify-center py-12 gap-3">
            <svg class="h-6 w-6 animate-spin text-text-secondary" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
            </svg>
            <p class="text-xs text-text-secondary">{{ tr('articleDetailPage.generatingSummary') }}</p>
          </div>

          <div v-else-if="article.summary" class="prose-custom">
            <p class="mb-5 text-xs leading-relaxed text-text-secondary">{{ tr('articleDetailPage.summaryDisclaimer') }}</p>
            <div ref="summaryContentRef" v-html="article.summary" class="article-content"></div>
          </div>

          <div v-else-if="canGenerateEnrichment || enrichmentError" class="py-12 text-center">
            <p :role="enrichmentError ? 'alert' : undefined" class="text-sm text-text-secondary">
              {{ enrichmentError || tr('articleDetailPage.noSummary') }}
            </p>
            <button type="button" class="mt-4 inline-flex h-9 items-center rounded-full bg-primary px-4 text-sm font-medium text-white disabled:opacity-50"
                    :disabled="isEnriching || !canGenerateEnrichment" @click="handleTabSwitch('summary')">
              {{ enrichmentError ? tr('common.retry') : tr('articleDetailPage.generateSummary') }}
            </button>
          </div>

          <div v-else class="py-20 text-center">
            <div class="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-surface-container/80">
              <svg class="h-6 w-6 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
              </svg>
            </div>
            <h3 class="text-sm font-bold text-text mb-1">{{ tr('articleDetailPage.premiumSummary') }}</h3>
            <p class="text-xs text-text-secondary mb-5">{{ tr('articleDetailPage.membersOnly') }}</p>
            <router-link :to="{ name: 'upgrade' }"
                         class="inline-flex h-9 items-center justify-center rounded-full bg-primary px-5 text-xs font-semibold text-white shadow-xs hover:brightness-105 transition">
              {{ tr('articleDetailPage.upgrade') }}
            </router-link>
          </div>
        </div>

        <!-- 3. 思维导图面板 -->
        <div v-show="activeMainTab === 'mindmap'" id="article-panel-mindmap" role="tabpanel"
             :aria-labelledby="mainTabs.some(tab => tab.id === 'mindmap') ? 'article-tab-mindmap' : undefined" tabindex="0">
          <div v-if="isEnriching && !mindmapMarkdown" role="status" class="flex flex-col items-center justify-center py-12 gap-3">
            <svg class="h-6 w-6 animate-spin text-text-secondary" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
            </svg>
            <p class="text-xs text-text-secondary">{{ tr('articleDetailPage.generatingMindMap') }}</p>
          </div>

          <div v-else-if="mindmapMarkdown" class="overflow-hidden rounded-xl bg-surface-container/30">
            <iframe ref="mindmapFrame" src="/md2mindmap.html" class="w-full" :title="tr('articleDetailPage.mindMap')"
                    :style="`max-height: 500rem; height: ${mindmapHeight}rem;`" frameborder="0"
                    @load="sendMindmapData"/>
          </div>

          <div v-else-if="canGenerateEnrichment || enrichmentError" class="py-12 text-center">
            <p :role="enrichmentError ? 'alert' : undefined" class="text-sm text-text-secondary">
              {{ enrichmentError || tr('articleDetailPage.noMindMap') }}
            </p>
            <button type="button" class="mt-4 inline-flex h-9 items-center rounded-full bg-primary px-4 text-sm font-medium text-white disabled:opacity-50"
                    :disabled="isEnriching || !canGenerateEnrichment" @click="handleTabSwitch('mindmap')">
              {{ enrichmentError ? tr('common.retry') : tr('articleDetailPage.generateMindMap') }}
            </button>
          </div>

          <div v-else class="py-20 text-center">
            <div class="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-surface-container/80">
              <svg class="h-6 w-6 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
              </svg>
            </div>
            <h3 class="text-sm font-bold text-text mb-1">{{ tr('articleDetailPage.articleMindMap') }}</h3>
            <p class="text-xs text-text-secondary mb-5">{{ tr('articleDetailPage.mindMapMembersOnly') }}</p>
            <router-link :to="{ name: 'upgrade' }"
                         class="inline-flex h-9 items-center justify-center rounded-full bg-primary px-5 text-xs font-semibold text-white shadow-xs hover:brightness-105 transition">
              {{ tr('articleDetailPage.upgrade') }}
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
      <h3 class="text-base font-bold text-text mb-1">{{ tr('articleDetailPage.articleNotFound') }}</h3>
      <p class="text-xs text-text-secondary">{{ tr('articleDetailPage.articleUnavailable') }}</p>
    </div>

    <!-- 底部推荐阅读 -->
    <section v-if="article && detailsItems.length > 0" class="mx-auto mt-6 max-w-screen-md px-4 pb-6 sm:px-6" aria-labelledby="recommended-reading-heading">
      <div class="border-t border-outline/10 pt-6">
        <h2 id="recommended-reading-heading" class="mb-3 text-base font-semibold text-text">{{ tr('articleDetailPage.recommendedReading') }}</h2>
        <article-card-list :items="detailsItems" :loading="detailsLoading" key-field="id" meta-field="timeAgo"
                           compact show-article-actions/>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import {computed, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, watch} from 'vue';
import {useMutation, useQueryClient} from '@tanstack/vue-query';
import {useRoute, useRouter} from 'vue-router';
import {useArticlesStore} from '../stores/articles/articles';
import ArticleActions from '../components/ArticleActions.vue';
import {useHistoryMutation} from '../queries/articleActions';
import MediaAttachment from "../components/MediaAttachment.vue";
import ArticleCardList from "../components/ArticleCardList.vue";
import {useAuthStore} from "../stores/auth";
import {md2html} from "../utils/markdown";
import {normalizeArticle, type ArticleDetail} from '../stores/articles/types';
import {articleQueryKey, useArticleQuery, useArticleRecommendationsQuery} from '../queries/article';
import {useFeedQuery} from '../queries/feed';
import {useDocumentTitle} from '../composables/documentTitle';
import {useSubscriptionMutation} from '../queries/subscriptions';

defineOptions({name: 'ArticleDetailPage'});

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
const summaryContentRef = ref<HTMLElement | null>(null);
const feedAvatarFailed = ref(false);
const mindmapFrame = ref<HTMLIFrameElement | null>(null);
type MainTab = 'content' | 'summary' | 'mindmap';
const activeMainTab = computed(() => props.tab);
const mindmapMarkdown = ref('');
const enrichmentError = ref('');
const lazyLoadObserver = ref<IntersectionObserver | null>(null);
const headerActionExists = ref(false);

const AVERAGE_WORDS_PER_MINUTE = 200;

const articleId = computed(() => props.id ?? '');
const userId = computed(() => authStore.user?.userId ?? 'anonymous');
const articleQuery = useArticleQuery(articleId);
const recommendationsQuery = useArticleRecommendationsQuery(articleId);
const article = computed(() => articleQuery.data.value ?? null);
const articleFeedId = computed(() => article.value?.feedId ?? null);
const feedQuery = useFeedQuery(articleFeedId);
const feedDetail = computed(() => feedQuery.data.value ?? null);
const articleLoading = computed(() => articleQuery.isPending.value);
const articleError = computed(() => {
  const error = articleQuery.error.value;
  return error instanceof Error ? error.message : error ? tr('articleDetailPage.articleLoadFailed') : '';
});
const detailsItems = computed(() => (recommendationsQuery.data.value ?? []).map(normalizeArticle));
const detailsLoading = computed(() => recommendationsQuery.isPending.value);
const canGenerateEnrichment = computed(() => authStore.isAuthenticated
    && Boolean(authStore.user?.currentPlan) && authStore.user?.currentPlan !== 'FREE');
const mainTabs = computed(() => {
  const tabs: { id: MainTab; label: string }[] = [{id: 'content', label: tr('articleDetailPage.body')}];
  if (article.value?.summary || article.value?.requiresUpgrade || canGenerateEnrichment.value || activeMainTab.value === 'summary') {
    tabs.push({id: 'summary', label: tr('articleDetailPage.aiSummary')});
  }
  if (article.value?.mindMap || article.value?.requiresUpgrade || canGenerateEnrichment.value || activeMainTab.value === 'mindmap') {
    tabs.push({id: 'mindmap', label: tr('articleDetailPage.mindMap')});
  }
  return tabs;
});
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
  const query = {...route.query};
  if (tab === 'content') {
    delete query.tab;
  } else {
    query.tab = tab;
  }
  if (props.tab !== tab) {
    await router.push({query});
  }
  if (tab === 'content') return;
  if (tab === 'summary' && article.value?.summary) return;
  if (tab === 'mindmap' && mindmapMarkdown.value) return;
  if (!canGenerateEnrichment.value || isEnriching.value) return;

  const request = {articleId: articleId.value, userId: userId.value};
  enrichmentError.value = '';
  try {
    const result = await enrichMutation.mutateAsync(request);
    if (articleId.value !== request.articleId || userId.value !== request.userId || !pageActive.value) return;
    if (!(tab === 'summary' ? result.summary : result.mindMap)) {
      enrichmentError.value = tr('articleDetailPage.enrichmentFailed');
    }
  } catch {
    if (articleId.value === request.articleId && userId.value === request.userId && pageActive.value) {
      enrichmentError.value = tr('articleDetailPage.enrichmentFailed');
    }
  }
};

const handleTabKeydown = async (event: KeyboardEvent, currentTab: MainTab) => {
  const index = mainTabs.value.findIndex(tab => tab.id === currentTab);
  let nextIndex: number;
  if (event.key === 'ArrowRight') nextIndex = (index + 1) % mainTabs.value.length;
  else if (event.key === 'ArrowLeft') nextIndex = (index - 1 + mainTabs.value.length) % mainTabs.value.length;
  else if (event.key === 'Home') nextIndex = 0;
  else if (event.key === 'End') nextIndex = mainTabs.value.length - 1;
  else return;
  event.preventDefault();
  const tab = mainTabs.value[nextIndex];
  // Focus follows selection immediately, including while AI generation is pending.
  const navigation = handleTabSwitch(tab.id);
  await nextTick();
  document.getElementById(`article-tab-${tab.id}`)?.focus();
  await navigation;
};

const enrichMutation = useMutation({
  mutationFn: async ({articleId}: {articleId: string; userId: string}) => {
    const result = await articlesStore.enrich(articleId);
    return {
      summary: result.summary?.trim() ? await md2html(result.summary) : '',
      mindMap: result.mindMap?.trim() ?? '',
    };
  },
  onSuccess: (result, request) => {
    // A late response belongs to the article and user that started the request.
    queryClient.setQueryData<ArticleDetail>(articleQueryKey(request.userId, request.articleId), (previous) => previous ? {
      ...previous,
      summary: result.summary || previous.summary,
      mindMap: result.mindMap || previous.mindMap,
    } : previous);
  },
});
const isEnriching = computed(() => enrichMutation.isPending.value);

const subscriptionMutation = useSubscriptionMutation();
const subscriptionSubmitting = subscriptionMutation.isPending;
const subscriptionError = ref('');
watch([articleId, userId], () => {
  enrichmentError.value = '';
  enrichMutation.reset();
  subscriptionError.value = '';
  feedAvatarFailed.value = false;
});
const toggleFeedSubscription = async () => {
  const feed = feedDetail.value;
  if (!feed || subscriptionSubmitting.value) return;
  subscriptionError.value = '';
  try {
    await subscriptionMutation.mutateAsync({
      feedId: feed.feedId, feedUrl: feed.url, subscribed: feed.subscribed,
    });
  } catch (error) {
    subscriptionError.value = error instanceof Error ? error.message : tr('articleDetailPage.subscriptionFailed');
  }
};

const handleTagClick = (tag: string) => {
  if (!tag) return;
  router.push({name: 'feedsSubscriptions', query: {tags: tag.toLowerCase()}});
};


const setupContentTables = () => {
  for (const container of [articleContentRef.value, summaryContentRef.value]) {
    container?.querySelectorAll('table').forEach(table => {
      if (table.parentElement?.classList.contains('article-table-scroll')) return;
      const wrapper = document.createElement('div');
      wrapper.className = 'article-table-scroll';
      wrapper.tabIndex = 0;
      wrapper.setAttribute('role', 'region');
      wrapper.setAttribute('aria-label', tr('articleDetailPage.articleTable'));
      table.before(wrapper);
      wrapper.append(table);
    });
  }
};

const setupLazyLoading = () => {
  const container = articleContentRef.value;
  if (!container) return;
  if (lazyLoadObserver.value) lazyLoadObserver.value.disconnect();
  const images = Array.from(container.querySelectorAll('img')) as HTMLImageElement[];
  if ('loading' in HTMLImageElement.prototype) {
    images.forEach((img) => {
      img.loading = 'lazy';
    });
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
  }, {rootMargin: '50px 0px', threshold: 0.01});

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

onActivated(() => {
  pageActive.value = true;
});
onDeactivated(() => {
  pageActive.value = false;
});
onBeforeUnmount(() => {
  if (lazyLoadObserver.value) {
    lazyLoadObserver.value.disconnect();
    lazyLoadObserver.value = null;
  }
  pageActive.value = false;
});

watch([() => article.value?.id, () => article.value?.content, () => article.value?.mindMap, () => article.value?.summary], async ([id], previous) => {
  if (!article.value) return;
  mindmapMarkdown.value = article.value.mindMap ?? '';
  if (id !== previous?.[0]) window.scrollTo({top: 0, behavior: 'auto'});
  await nextTick();
  setupContentTables();
  setupLazyLoading();
}, {immediate: true});

watch(activeMainTab, async (newTab) => {
  if (newTab === 'mindmap' && mindmapMarkdown.value) {
    await nextTick();
    sendMindmapData();
  }
});
</script>

<style scoped>
@reference "../styles/tailwind.css";

.article-tabs {
  top: var(--app-header-offset, 56px);
  transition: top 200ms ease;
}

@media (prefers-reduced-motion: reduce) {
  .article-tabs {
    transition: none;
  }
}

.prose-custom {
  @apply text-text;
}

.article-content {
  font-family: inherit;
  @apply text-base leading-[1.8] text-text break-words sm:text-lg;
}

.article-content :deep(p) {
  @apply mb-5 text-text;
  line-height: inherit;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  font-family: inherit;
  @apply font-bold text-text mt-8 mb-3 tracking-tight;
}

.article-content :deep(h1) {
  @apply text-2xl leading-tight;
}

.article-content :deep(h2) {
  @apply text-xl leading-snug;
}

.article-content :deep(h3) {
  @apply text-lg leading-snug;
}

.article-content :deep(ul),
.article-content :deep(ol) {
  @apply mb-5 pl-6 space-y-2 text-text;
}

.article-content :deep(li) {
  line-height: inherit;
}

.article-content :deep(ul) {
  list-style-type: disc;
}

.article-content :deep(ol) {
  list-style-type: decimal;
}

.article-content :deep(blockquote) {
  @apply border-l-4 border-primary/40 bg-surface-container/30 pl-4 py-2 my-5 rounded-r-xl italic text-text-secondary;
}

.article-content :deep(pre) {
  @apply bg-surface-container/60 p-4 my-5 rounded-xl text-sm leading-relaxed overflow-x-auto max-w-full text-text;
}

.article-content :deep(code) {
  @apply bg-surface-container px-1.5 py-0.5 text-[0.875em] font-mono rounded text-primary break-words;
}

.article-content :deep(pre code) {
  @apply bg-transparent p-0 text-sm text-text break-normal;
}

.article-content :deep(img) {
  @apply block w-auto h-auto mx-auto my-6 rounded-lg max-w-full;
}

.article-content :deep(a) {
  @apply text-primary underline underline-offset-2 hover:opacity-80;
}

.article-content :deep(hr) {
  @apply border-0 h-px bg-outline/20 my-8;
}

.article-content :deep(table) {
  @apply w-full border-collapse text-sm leading-relaxed;
}

.article-content :deep(.article-table-scroll) {
  @apply my-5 max-w-full overflow-x-auto;
}

.article-content :deep(.article-table-scroll:focus-visible) {
  outline: 2px solid rgb(var(--md-primary));
  outline-offset: 2px;
}

.article-content :deep(th),
.article-content :deep(td) {
  @apply border border-outline/20 px-3.5 py-2.5 text-left;
}

.article-content :deep(thead th) {
  @apply bg-surface-container font-semibold text-text;
}

.article-content > :deep(:first-child) {
  margin-top: 0;
}
</style>
