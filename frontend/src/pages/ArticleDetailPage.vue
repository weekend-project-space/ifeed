<template>
  <div class="">
    <!-- Loading State - Skeleton -->
    <div v-if="articleLoading" class="max-w-screen-md  mx-auto px-3 sm:px-6 py-12 animate-pulse">
      <!-- Header Skeleton -->
      <div class="space-y-6 mb-12">
        <div class="h-12 bg-gray-200 dark:bg-gray-800 rounded-lg w-3/4"></div>
        <div class="h-8 bg-gray-200 dark:bg-gray-800 rounded-lg w-1/2"></div>
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 bg-gray-200 dark:bg-gray-800 rounded-full"></div>
          <div class="space-y-2 flex-1">
            <div class="h-4 bg-gray-200 dark:bg-gray-800 rounded w-32"></div>
            <div class="h-3 bg-gray-200 dark:bg-gray-800 rounded w-24"></div>
          </div>
        </div>
      </div>
      <!-- Content Skeleton -->
      <div class="space-y-4">
        <div class="h-4 bg-gray-200 dark:bg-gray-800 rounded"></div>
        <div class="h-4 bg-gray-200 dark:bg-gray-800 rounded"></div>
        <div class="h-4 bg-gray-200 dark:bg-gray-800 rounded w-5/6"></div>
        <div class="h-64 bg-gray-200 dark:bg-gray-800 rounded-lg my-8"></div>
        <div class="h-4 bg-gray-200 dark:bg-gray-800 rounded"></div>
        <div class="h-4 bg-gray-200 dark:bg-gray-800 rounded"></div>
        <div class="h-4 bg-gray-200 dark:bg-gray-800 rounded w-4/5"></div>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="articleError" class="max-w-screen-md  mx-auto  px-3 sm:px-6 py-12">
      <div class="rounded-lg bg-red-50 dark:bg-red-900/10 p-6 border border-red-100 dark:border-red-900/20">
        <div class="flex items-start gap-4">
          <div
              class="flex-shrink-0 w-10 h-10 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
            <svg class="w-5 h-5 text-red-600 dark:text-red-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                    clip-rule="evenodd"/>
            </svg>
          </div>
          <div class="flex-1">
            <h3 class="text-sm font-medium text-red-900 dark:text-red-200 mb-1">加载失败</h3>
            <p class="text-sm text-red-700 dark:text-red-300">{{ articleError }}</p>
            <button @click="articleQuery.refetch()"
                    class="mt-3 px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-full transition-colors">
              重试
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Article Content -->
    <article v-else-if="article" class="max-w-screen-md  mx-auto  px-3 sm:px-6 ">
      <!-- Header Section - Medium Style -->
      <header class="pt-12 pb-8">
        <!-- Title -->
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-gray-50 leading-tight mb-6 font-serif break-words">
          {{ article.title }}
        </h1>

        <!-- Author & Meta Info -->
        <div class="flex items-center justify-between gap-4 mb-8">
          <router-link v-if="article.feedId" :to="'/feeds/' + article.feedId" class="flex items-center gap-3 group">
            <img v-if="article.feedAvatar" :src="article.feedAvatar" :alt="article.feedTitle"
                 class="w-12 h-12 rounded-full object-cover"
                 @error="(e) => (e.target as HTMLImageElement).style.display = 'none'"/>
            <div v-else
                 class="w-12 h-12 rounded-full bg-gradient-to-br from-gray-700 to-gray-900 dark:from-gray-600 dark:to-gray-800 flex items-center justify-center text-white text-lg font-semibold">
              {{ article.feedTitle?.charAt(0).toUpperCase() }}
            </div>
            <div class="flex flex-col">
              <span class="text-sm font-medium text-gray-900 dark:text-gray-100 group-hover:underline">{{
                  article.feedTitle
                }}</span>
              <div class="flex items-center gap-2 text-xs text-gray-500 dark:text-gray-400">
                <span>{{ article.timeAgo }}</span>
                <span>•</span>
                <span>{{ readingTime }} 分钟阅读</span>
              </div>
            </div>
          </router-link>

          <!-- Copy Link Button -->
          <button
              @click="copyArticleLink"
              class="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors group relative"
              :class="{ 'text-green-600 dark:text-green-400': linkCopied }"
              title="复制链接"
              aria-label="复制文章链接"
          >
            <svg v-if="!linkCopied"
                 class="w-5 h-5 text-gray-600 dark:text-gray-400 group-hover:text-gray-900 dark:group-hover:text-gray-100"
                 fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
            </svg>
            <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
            </svg>
            <!-- Tooltip -->
            <span v-if="linkCopied"
                  class="absolute -bottom-10 sm:-bottom-8 left-1/2 transform -translate-x-1/2 px-2 py-1 text-xs bg-gray-900 dark:bg-gray-100 text-white dark:text-gray-900 rounded whitespace-nowrap pointer-events-none z-10">
              已复制
            </span>
          </button>
        </div>

        <!-- Action Bar -->
        <div class="flex items-center justify-between py-4 border-y  border-gray-100 dark:border-gray-800">
          <div class="flex items-center gap-3">
            <!-- Tags -->
            <div v-if="article.tags && article.tags.length" class="flex flex-wrap gap-2">
              <button v-for="tag in article.tags.slice(0, 3)" :key="tag" type="button"
                      class="px-3 py-1 text-xs font-medium rounded-full bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
                      @click="handleTagClick(tag)">
                {{ tag }}
              </button>
            </div>
          </div>

          <!-- Collect Button -->
          <template v-if="headerActionExists && route.name === 'article'">
            <Teleport to="#header-action">
              <button
                      class="flex h-10 w-10 items-center justify-center text-sm font-medium rounded-full transition-all text-gray-900 dark:text-gray-100 hover:bg-gray-200 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                      :disabled="collectionSubmitting"
                      :title="article.collected ? '取消收藏' : '收藏'"
                      :aria-label="article.collected ? '取消收藏' : '收藏'"
                      :aria-pressed="article.collected"
                      @click="toggleCollection">
                <svg class="w-5 h-5" :class="article.collected ? 'fill-current' : 'fill-none'" stroke="currentColor"
                     stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round"
                        d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"/>
                </svg>
              </button>
            </Teleport>
          </template>
          <button v-if="!headerActionExists"
                  class="p-2 text-sm font-medium rounded-full transition-all inline-flex items-center gap-2 text-gray-900 dark:text-gray-100 hover:bg-gray-200 dark:hover:bg-gray-700"
                  :disabled="collectionSubmitting"
                  :title="article.collected ? '取消收藏' : '收藏'"
                  :aria-label="article.collected ? '取消收藏' : '收藏'"
                  :aria-pressed="article.collected"
                  @click="toggleCollection"
          >
            <svg class="w-4 h-4" :class="article.collected ? 'fill-current' : 'fill-none'" stroke="currentColor"
                 stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"/>
            </svg>
          </button>
        </div>
      </header>

      <!-- Tabs Navigation - Clean Style -->
      <div v-if="article.summary||article.mindMap||article.requiresUpgrade"
           class=" border-b  bg-white/95 dark:bg-gray-950/95  border-gray-200 dark:border-gray-800 sticky top-0 backdrop-blur-md z-20">
        <div class="max-w-screen-lg mx-auto px-3">
          <nav class="flex gap-8" role="tablist">
            <button
                @click="handleTabSwitch('content')"
                :class="['py-4 text-sm font-medium transition-colors relative', activeMainTab === 'content' ? 'text-gray-900 dark:text-gray-100' : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100']"
                role="tab"
                :aria-selected="activeMainTab === 'content'"
                :tabindex="activeMainTab === 'content' ? 0 : -1"
            >
              正文
              <div v-if="activeMainTab === 'content'"
                   class="absolute bottom-0 left-0 right-0 h-0.5 bg-gray-900 dark:bg-gray-100"></div>
            </button>
            <button
                v-if="article.summary || article.requiresUpgrade"
                @click="handleTabSwitch('summary')"
                :class="['py-4 text-sm font-medium transition-colors relative', activeMainTab === 'summary' ? 'text-gray-900 dark:text-gray-100' : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100']"
                role="tab"
                :aria-selected="activeMainTab === 'summary'"
                :tabindex="activeMainTab === 'summary' ? 0 : -1"
            >
              摘要
              <div v-if="activeMainTab === 'summary'"
                   class="absolute bottom-0 left-0 right-0 h-0.5 bg-gray-900 dark:bg-gray-100"></div>
            </button>
            <button
                v-if="article.mindMap || article.requiresUpgrade"
                @click="handleTabSwitch('mindmap')"
                :class="['py-4 text-sm font-medium transition-colors relative', activeMainTab === 'mindmap' ? 'text-gray-900 dark:text-gray-100' : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100']"
                role="tab"
                :aria-selected="activeMainTab === 'mindmap'"
                :tabindex="activeMainTab === 'mindmap' ? 0 : -1"
            >
              思维导图
              <div v-if="activeMainTab === 'mindmap'"
                   class="absolute bottom-0 left-0 right-0 h-0.5 bg-gray-900 dark:bg-gray-100"></div>
            </button>
          </nav>
        </div>
      </div>

      <!-- Tab Content -->
      <div class="py-12">
        <!-- Content Tab -->
        <div v-show="activeMainTab === 'content'" role="tabpanel" class="animate-fade-in">
          <!-- Media Attachment -->
          <media-attachment v-if="article.enclosure" mode="global" :url="article.enclosure"
                            :type="article.enclosureType"
                            :title="article.title" :artist="article.feedTitle || article.author" :track-id="article.id"
                            :cover-image="article.thumbnail" class="mb-12 rounded-lg overflow-hidden"/>

          <!-- Article Body - Medium Typography -->
          <div class="prose-custom">
            <div v-if="article.content" ref="articleContentRef" class="article-content" v-html="article.content"></div>
            <div v-else class="text-center py-20">
              <svg class="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600 mb-3" fill="none" stroke="currentColor"
                   viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              <p class="text-gray-500 dark:text-gray-400">暂无正文内容</p>
            </div>
          </div>

          <!-- Footer -->
          <footer class="mt-16 pt-8 border-t border-gray-200 dark:border-gray-800">
            <div class="flex items-center justify-between">
              <a v-if="article.link" :href="article.link" target="_blank" rel="noopener"
                 class="inline-flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-gray-100 hover:text-gray-600 dark:hover:text-gray-400 transition-colors">
                查看原文
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
                </svg>
              </a>
              <span class="text-sm text-gray-500 dark:text-gray-400">{{ article.timeAgo }}</span>
            </div>
          </footer>
        </div>

        <!-- Summary Tab -->
        <div v-show="activeMainTab === 'summary'" role="tabpanel" class="animate-fade-in">
          <div v-if="isEnriching" class="flex flex-col items-center justify-center py-24 gap-4">
            <svg class="w-8 h-8 animate-spin text-gray-400 dark:text-gray-600" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
            </svg>
            <p class="text-sm text-gray-500 dark:text-gray-400">AI 正在生成摘要...</p>
          </div>
          <div v-else-if="article.summary" class="prose-custom">
            <div
                class="flex items-start gap-3 p-4 bg-gray-50 dark:bg-gray-900 rounded-lg mb-8 border border-gray-200 dark:border-gray-800">
              <svg class="w-5 h-5 text-gray-600 dark:text-gray-400 flex-shrink-0 mt-0.5" fill="none"
                   stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              <div class="text-sm text-gray-700 dark:text-gray-300">
                <strong class="font-medium">AI 生成摘要</strong>
                <p class="mt-1">以下摘要由 AI 自动生成，仅供参考</p>
              </div>
            </div>
            <div v-html="article.summary" class="article-content"></div>
          </div>
          <div v-else class="text-center py-24">
            <div
                class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gray-100 dark:bg-gray-800 mb-4">
              <svg class="w-8 h-8 text-gray-400 dark:text-gray-600" fill="none" stroke="currentColor"
                   viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
              </svg>
            </div>
            <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-2">摘要功能</h3>
            <p class="text-gray-500 dark:text-gray-400 mb-6">此功能仅限会员使用</p>
            <router-link to="/upgrade"
                         class="inline-flex items-center text-white bg-gray-900 dark:bg-gray-100 dark:text-gray-900 hover:bg-gray-800 dark:hover:bg-gray-200 px-6 py-3 text-sm font-medium rounded-full transition-colors">
              升级会员
            </router-link>
          </div>
        </div>

        <!-- Mindmap Tab -->
        <div v-show="activeMainTab === 'mindmap'" role="tabpanel" class="animate-fade-in">
          <div v-if="isEnriching" class="flex flex-col items-center justify-center py-24 gap-4">
            <svg class="w-8 h-8 animate-spin text-gray-400 dark:text-gray-600" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
            </svg>
            <p class="text-sm text-gray-500 dark:text-gray-400">AI 正在生成思维导图...</p>
          </div>
          <div v-else-if="mindmapMarkdown"
               class="rounded-lg overflow-hidden bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800">
            <iframe ref="mindmapFrame" src="/md2mindmap.html" class="w-full"
                    :style="`max-height: 500rem; height: ${mindmapHeight}rem;`" frameborder="0" @load="sendMindmapData"/>
          </div>
          <div v-else class="text-center py-24">
            <div
                class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gray-100 dark:bg-gray-800 mb-4">
              <svg class="w-8 h-8 text-gray-400 dark:text-gray-600" fill="none" stroke="currentColor"
                   viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
              </svg>
            </div>
            <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-2">思维导图</h3>
            <p class="text-gray-500 dark:text-gray-400 mb-6">此功能仅限会员使用</p>
            <router-link to="/upgrade"
                         class="inline-flex items-center text-white bg-gray-900 dark:bg-gray-100 dark:text-gray-900 hover:bg-gray-800 dark:hover:bg-gray-200 px-6 py-3 text-sm font-medium rounded-full transition-colors">
              升级会员
            </router-link>
          </div>
        </div>
      </div>

      <!-- Recommended Articles Section -->

    </article>
    <!-- Not Found State -->
    <div v-else class="flex flex-col items-center justify-center py-32">
      <div class="inline-flex items-center justify-center w-20 h-20 rounded-full bg-gray-100 dark:bg-gray-800 mb-4">
        <svg class="w-10 h-10 text-gray-400 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
      </div>
      <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-2">未找到文章</h3>
      <p class="text-gray-500 dark:text-gray-400">该文章可能已被删除或不存在</p>
    </div>
    <div v-if="detailsItems.length > 0" class="bg-gray-50 dark:bg-gray-900/50 py-16">
      <div class="max-w-screen-md  mx-auto  px-3 sm:px-6">
        <h2 class="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-8">推荐阅读</h2>
        <article-card-list :items="detailsItems" :loading="detailsLoading" key-field="id" compact/>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {useMutation, useQueryClient} from '@tanstack/vue-query';
import {useRoute, useRouter} from 'vue-router';
import {useArticlesStore} from '../stores/articles/articles';
import {useCollectionsStore} from '../stores/collections';
import MediaAttachment from "../components/MediaAttachment.vue";
import ArticleCardList from "../components/ArticleCardList.vue";
import {useAuthStore} from "../stores/auth";
import {md2html} from "../utils/markdown";
import {normalizeArticle, type ArticleDetail} from '../stores/articles/types';
import {articleQueryKey, useArticleQuery, useArticleRecommendationsQuery} from '../queries/article';

defineOptions({name: 'ArticleDetailPage'});

interface Props {
  id: string;
  tab: 'content' | 'summary' | 'mindmap';
}

const props = defineProps<Props>();
const router = useRouter();
const route = useRoute();
const articlesStore = useArticlesStore();
const collectionsStore = useCollectionsStore();
const authStore = useAuthStore();
const queryClient = useQueryClient();

const scrollTracked = ref(false);
const articleContentRef = ref<HTMLElement | null>(null);
const mindmapFrame = ref<HTMLIFrameElement | null>(null);
type MainTab = 'content' | 'summary' | 'mindmap';
const activeMainTab = computed(() => props.tab);
const mindmapMarkdown = ref('');
const linkCopied = ref(false);
const lazyLoadObserver = ref<IntersectionObserver | null>(null);
const headerActionExists = ref(false);

const SCROLL_PROGRESS_THRESHOLD = 0.3;
const AVERAGE_WORDS_PER_MINUTE = 200;

const articleId = computed(() => props.id ?? '');
const userId = computed(() => authStore.user?.userId ?? 'anonymous');
const currentArticleQueryKey = computed(() => articleQueryKey(userId.value, articleId.value));
const articleQuery = useArticleQuery(articleId);
const recommendationsQuery = useArticleRecommendationsQuery(articleId);
const article = computed(() => articleQuery.data.value ?? null);
const articleLoading = computed(() => articleQuery.isPending.value);
const articleError = computed(() => {
  const error = articleQuery.error.value;
  return error instanceof Error ? error.message : error ? '文章加载失败,请稍后重试' : '';
});
const detailsItems = computed(() => (recommendationsQuery.data.value ?? []).map(normalizeArticle));
const detailsLoading = computed(() => recommendationsQuery.isPending.value);
const historyMutation = useMutation({
  mutationFn: (id: string) => articlesStore.recordHistory(id),
});

// 计算思维导图高度
const mindmapHeight = computed(() => {
  if (!mindmapMarkdown.value) return 50;
  const lines = mindmapMarkdown.value.split('\n').length;
  return Math.max(50, Math.min(lines * 1.1, 500));
});

// 计算阅读时间
const readingTime = computed(() => {
  if (!article.value?.content) return 0;

  // 移除 HTML 标签
  let text = article.value.content.replace(/<[^>]*>/g, '');

  // 移除代码块（它们通常阅读较慢）
  text = text.replace(/```[\s\S]*?```/g, '');

  // 移除表格（简化处理）
  text = text.replace(/\|[\s\S]*?\|/g, '');

  // 计算字数（中文按字符，英文按单词）
  const chineseChars = text.match(/[\u4e00-\u9fa5]/g)?.length || 0;
  const englishWords = text.match(/[a-zA-Z]+/g)?.length || 0;

  // 中文约 300 字/分钟，英文约 200 词/分钟
  const minutes = Math.ceil((chineseChars / 300 + englishWords / AVERAGE_WORDS_PER_MINUTE));
  return Math.max(1, minutes); // 至少显示 1 分钟
});

const getScrollTop = (): number => {
  return window.scrollY ?? document.documentElement.scrollTop ?? document.body.scrollTop ?? 0;
};

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

// 节流函数
const throttle = <T extends (...args: any[]) => void>(fn: T, delay: number): T => {
  let lastCall = 0;
  return ((...args: any[]) => {
    const now = Date.now();
    if (now - lastCall >= delay) {
      lastCall = now;
      fn(...args);
    }
  }) as T;
};

const handleScroll = throttle(() => {
  // 记录阅读历史
  if (scrollTracked.value || !article.value) return;

  const windowHeight = window.innerHeight;
  const documentHeight = document.documentElement.scrollHeight;
  const scrollTop = getScrollTop();
  const maxScroll = documentHeight - windowHeight;
  if (authStore.isAuthenticated) {
    if (maxScroll <= 0) {
      scrollTracked.value = true;
      historyMutation.mutate(articleId.value);
      return;
    }

    const progress = scrollTop / maxScroll;
    if (progress > SCROLL_PROGRESS_THRESHOLD) {
      scrollTracked.value = true;
      historyMutation.mutate(articleId.value);
    }
  }

}, 100);

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

const collectionMutation = useMutation({
  mutationFn: (payload: { id: string; collected: boolean }) =>
    collectionsStore.toggleCollection(payload.id, {
      collected: payload.collected,
    }),
  onSuccess: (_, payload) => {
    queryClient.setQueryData<ArticleDetail>(currentArticleQueryKey.value, (previous) => previous ? {
      ...previous,
      collected: !payload.collected,
    } : previous);
    void Promise.all([
      queryClient.invalidateQueries({queryKey: ['collections']}),
      queryClient.invalidateQueries({queryKey: ['recommendations']}),
      queryClient.invalidateQueries({queryKey: ['search']}),
      queryClient.invalidateQueries({queryKey: ['feedArticles']}),
      queryClient.invalidateQueries({queryKey: ['subscriptionArticles']}),
    ]);
  },
});
const collectionSubmitting = computed(() => collectionMutation.isPending.value);

const toggleCollection = () => {
  if (!article.value || collectionMutation.isPending.value) return;
  collectionMutation.mutate({
    id: articleId.value,
    collected: article.value.collected ?? false,
  });
};

const handleTagClick = (tag: string) => {
  if (!tag) return;
  router.push({name: 'feedsSubscriptions', query: {tags: tag.toLowerCase()}});
};

// 复制文章链接（带降级方案）
const copyArticleLink = async () => {
  try {
    const url = window.location.href;

    // 优先使用现代 Clipboard API
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(url);
    } else {
      // 降级方案：使用传统方法
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
    // 可以在这里添加一个 toast 提示用户复制失败
  }
};

// 添加图片懒加载（改进版）
const setupLazyLoading = () => {
  const container = articleContentRef.value;
  if (!container) return;

  // 清理旧的 observer
  if (lazyLoadObserver.value) {
    lazyLoadObserver.value.disconnect();
  }

  const images = Array.from(container.querySelectorAll('img')) as HTMLImageElement[];

  // 如果浏览器支持原生懒加载
  if ('loading' in HTMLImageElement.prototype) {
    images.forEach((img) => {
      img.loading = 'lazy';
    });
    return;
  }

  // 降级方案：使用 Intersection Observer
  lazyLoadObserver.value = new IntersectionObserver(
      (entries) => {
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
      },
      {
        rootMargin: '50px 0px', // 提前 50px 开始加载
        threshold: 0.01
      }
  );

  images.forEach((img) => {
    if (img.src && !img.hasAttribute('data-lazy-src')) {
      // 保存原始 src
      img.setAttribute('data-lazy-src', img.src);
      // 设置占位图
      img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1 1"%3E%3C/svg%3E';
      lazyLoadObserver.value?.observe(img);
    }
  });
};

onMounted(async () => {
  // 检查 Teleport 目标是否存在
  headerActionExists.value = !!document.querySelector('#header-action');
  window.addEventListener('scroll', handleScroll, {passive: true});
});

onBeforeUnmount(() => {
  // 清理 Intersection Observer
  if (lazyLoadObserver.value) {
    lazyLoadObserver.value.disconnect();
    lazyLoadObserver.value = null;
  }

  window.removeEventListener('scroll', handleScroll);
});

watch(() => article.value, async (newArticle) => {
  if (!newArticle) return;
  scrollTracked.value = false;
  mindmapMarkdown.value = newArticle.mindMap ?? '';
  window.scrollTo({top: 0, behavior: 'auto'});
  await nextTick();
  if (authStore.isAuthenticated) {
    historyMutation.mutate(articleId.value);
  }
  setupLazyLoading();
}, {immediate: true});

watch(activeMainTab, async (newTab) => {
  if (newTab === 'mindmap' && mindmapMarkdown.value) {
    await nextTick();
    setTimeout(sendMindmapData, 200);
  }
});
</script>

<style scoped>
/* Medium-inspired Typography */
.prose-custom {
  @apply text-gray-900 dark:text-gray-100;
}

.article-content {
  font-family: charter, Georgia, Cambria, "Times New Roman", Times, serif;
  @apply text-lg leading-7 text-gray-900 dark:text-gray-100;
}

.article-content :deep(p) {
  @apply mb-4 leading-7;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  font-family: sohne, "Helvetica Neue", Helvetica, Arial, sans-serif;
  @apply font-bold text-gray-900 dark:text-gray-50 mt-8 mb-2.5;
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
  @apply mb-4 pl-8 space-y-1.5;
}

.article-content :deep(li) {
  @apply text-gray-800 dark:text-gray-200 leading-7;
}

.article-content :deep(blockquote) {
  @apply border-l-4 border-gray-900 dark:border-gray-100 pl-6 py-2 my-5 text-gray-800 dark:text-gray-200 italic text-xl leading-8;
}

.article-content :deep(pre) {
  @apply bg-gray-50 dark:bg-gray-900 p-4 my-5 rounded text-sm overflow-x-auto max-w-full;
}

.article-content :deep(code) {
  @apply bg-gray-100 dark:bg-gray-800 text-red-600 dark:text-red-400 px-1.5 py-0.5 text-sm font-mono rounded break-words;
}

.article-content :deep(pre code) {
  @apply bg-transparent p-0 text-sm break-normal;
}

.article-content :deep(img) {
  @apply w-full h-auto my-6 rounded max-w-full;
}

.article-content :deep(a) {
  @apply text-gray-900 dark:text-gray-100 underline hover:text-gray-600 dark:hover:text-gray-400;
}

.article-content :deep(hr) {
  @apply border-0 h-px bg-gray-300 dark:bg-gray-700 my-6;
}

.article-content :deep(table) {
  @apply w-full border-collapse my-5 text-sm;
}

.article-content :deep(th),
.article-content :deep(td) {
  @apply border border-gray-300 dark:border-gray-700 px-4 py-3 text-left;
}

.article-content :deep(thead th) {
  @apply bg-gray-50 dark:bg-gray-900 font-semibold;
}

/* Tab transition animation */
.animate-fade-in {
  animation: fadeIn 0.2s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
