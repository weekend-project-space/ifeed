<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6">
    <!-- Loading State -->
    <div v-if="articlesStore.loading" class="flex flex-col items-center justify-center py-32 gap-4">
      <div class="relative h-12 w-12">
        <div class="absolute inset-0 rounded-full border-4 border-gray-200 dark:border-gray-700"></div>
        <div class="absolute inset-0 animate-spin rounded-full border-4 border-transparent border-t-blue-500"></div>
      </div>
      <p class="text-sm text-gray-600 dark:text-gray-400">加载中...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="errorMessage" class="py-12">
      <div class="rounded-xl bg-red-50 dark:bg-red-900/10 p-6">
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
            <p class="text-sm text-red-700 dark:text-red-300">{{ errorMessage }}</p>
            <button
                @click="loadArticle(props.id)"
                class="mt-3 px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-full transition-colors"
            >
              重试
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Article Content -->
    <article v-else-if="article">
      <!-- Header - Clean and Simple -->
      <div class="mb-6">
        <header class="space-y-4">
          <!-- Title -->
          <h1 class="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white leading-tight">
            {{ article.title }}
          </h1>

          <!-- Meta Info Bar -->
          <div class="flex flex-wrap items-center gap-3 text-sm text-gray-600 dark:text-gray-400">
            <router-link
                v-if="article.feedId"
                :to="'/feeds/' + article.feedId"
                class="flex items-center gap-2 font-medium text-gray-900 dark:text-gray-100 hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
              <img
                  v-if="article.feedAvatar"
                  :src="article.feedAvatar"
                  :alt="article.feedTitle"
                  class="w-8 h-8 rounded-full object-cover"
                  @error="(e) => (e.target as HTMLImageElement).style.display = 'none'"
              />
              <div
                  v-else
                  class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center text-white text-xs font-semibold">
                {{ article.feedTitle?.charAt(0).toUpperCase() }}
              </div>
              {{ article.feedTitle }}
            </router-link>
            <span>•</span>
            <span>{{ article.timeAgo }}</span>
          </div>

          <!-- Actions Bar -->
          <div class="flex flex-wrap items-center gap-3 pt-2">
            <!-- Tags -->
            <div v-if="article.tags && article.tags.length" class="flex flex-wrap gap-2 flex-1">
              <button
                  v-for="tag in article.tags"
                  :key="tag"
                  type="button"
                  class="px-3 py-1.5 text-xs font-medium rounded-full bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
                  @click="handleTagClick(tag)">
                #{{ tag }}
              </button>
            </div>

            <!-- Collect Button -->
            <Teleport to="#header-action">
              <button
                  class="px-5 py-2.5 text-sm font-medium rounded-full transition-all inline-flex items-center gap-2"
                  :class="article.collected
                    ? 'bg-blue-600 text-white hover:bg-blue-700'
                    : 'bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700'"
                  @click="toggleCollection">
                <svg class="w-4 h-4" :class="article.collected ? 'fill-current' : 'fill-none'" stroke="currentColor"
                     stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"/>
                </svg>
                {{ article.collected ? '已收藏' : '收藏' }}
              </button>
            </Teleport>
          </div>
        </header>
      </div>

      <div class="">
        <!-- Main Content Area -->
        <div class="min-w-0">
          <!-- YouTube Style Tabs -->
          <div class="mb-4">
            <div class="flex gap-1 border-b border-gray-200 dark:border-gray-700">
              <button
                  @click="activeMainTab = 'content'"
                  :class="[
                    'px-6 py-3 text-sm font-medium transition-all relative flex items-center gap-2',
                    activeMainTab === 'content'
                      ? 'text-gray-900 dark:text-white'
                      : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
                  ]"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
                正文
                <div
                    v-if="activeMainTab === 'content'"
                    class="absolute bottom-0 left-0 right-0 h-0.5 bg-gray-900 dark:bg-white"
                ></div>
              </button>

              <button
                  v-if="showSummary"
                  @click="activeMainTab = 'summary'"
                  :class="[
                    'px-6 py-3 text-sm font-medium transition-all relative flex items-center gap-2',
                    activeMainTab === 'summary'
                      ? 'text-gray-900 dark:text-white'
                      : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
                  ]"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"/>
                </svg>
                摘要
                <div
                    v-if="activeMainTab === 'summary'"
                    class="absolute bottom-0 left-0 right-0 h-0.5 bg-gray-900 dark:bg-white"
                ></div>
              </button>

              <button
                  v-if="showMindMap"
                  @click="activeMainTab = 'mindmap'"
                  :class="[
                    'px-6 py-3 text-sm font-medium transition-all relative flex items-center gap-2',
                    activeMainTab === 'mindmap'
                      ? 'text-gray-900 dark:text-white'
                      : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
                  ]"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/>
                </svg>
                思维导图
                <div
                    v-if="activeMainTab === 'mindmap'"
                    class="absolute bottom-0 left-0 right-0 h-0.5 bg-gray-900 dark:bg-white"
                ></div>
              </button>
            </div>
          </div>


          <!-- Tab Content -->
          <div class="tab-content">
            <div v-show="activeMainTab === 'content'" class="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-6">
              <!-- Content Tab -->
              <div>
                <!-- Media Attachment -->
                <media-attachment
                    v-if="article.enclosure"
                    :url="article.enclosure"
                    :type="article.enclosureType"
                    :title="article.title"
                    :artist="article.feedTitle || article.author"
                    :cover-image="article.thumbnail"
                    class="mb-8 rounded-xl overflow-hidden"
                />

                <!-- Article Body -->
                <div class="prose prose-lg prose-gray dark:prose-invert max-w-none">
                  <div
                      v-if="article.content"
                      ref="articleContentRef"
                      class="article-content"
                      v-html="article.content">
                  </div>
                  <div v-else class="text-center py-20">
                    <svg class="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600 mb-3" fill="none"
                         stroke="currentColor"
                         viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                    </svg>
                    <p class="text-gray-500 dark:text-gray-400">暂无正文内容</p>
                  </div>
                </div>

                <!-- Footer Links -->
                <footer
                    class="flex flex-wrap items-center gap-4 pt-8 mt-8 border-t border-gray-200 dark:border-gray-700">
                  <a
                      v-if="article.link"
                      :href="article.link"
                      target="_blank"
                      rel="noopener"
                      class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-full transition-colors">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
                    </svg>
                    查看原文
                  </a>
                  <span class="inline-flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                  </svg>
                  {{ article.timeAgo }}
                </span>
                </footer>
              </div>
              <!-- Sidebar (Desktop Only)-->
              <aside class="hidden lg:block">
                <div class="sticky top-24">
                  <!-- Show TOC only when viewing content -->
                  <div v-if="activeMainTab === 'content' && showToc"
                       class="bg-gray-50 dark:bg-gray-800/50 rounded-xl p-5">
                    <h3 class="text-sm font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M4 6h16M4 12h16M4 18h7"/>
                      </svg>
                      目录
                    </h3>
                    <div class="max-h-[calc(100vh-14rem)] overflow-y-auto custom-scrollbar">
                      <toc-section
                          :items="tocItems"
                          :active-id="activeHeadingId"
                          @navigate="scrollToHeading"
                      />
                    </div>
                  </div>
                  <article-card-list class="mt-5" :items="detailsItems" :loading="detailsLoading" compact></article-card-list>
                </div>
              </aside>
            </div>

            <!-- Summary Tab -->
            <div v-show="activeMainTab === 'summary'">
              <div class="prose prose-lg prose-gray dark:prose-invert max-w-none">
                <div class="flex items-start gap-3 p-4 bg-blue-50 dark:bg-blue-900/10 rounded-xl mb-6">
                  <svg class="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" fill="none"
                       stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                  </svg>
                  <div class="text-sm text-blue-900 dark:text-blue-200">
                    <strong class="font-medium">AI 生成摘要</strong>
                    <p class="mt-1 text-blue-700 dark:text-blue-300">以下摘要由 AI 自动生成，仅供参考</p>
                  </div>
                </div>
                <div v-html="article.summary"
                     class="article-content text-gray-700 dark:text-gray-300 leading-relaxed text-base ">
                </div>
              </div>
            </div>

            <!-- Mindmap Tab -->
            <div v-show="activeMainTab === 'mindmap'">
              <div v-if="mindmapMarkdown"
                   class="rounded-xl overflow-hidden bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
                <iframe
                    ref="mindmapFrame"
                    src="/md2mindmap.html"
                    class="w-full"
                    :style="`max-height: 500vh; height: ${mindmapMarkdown.split('\n').length*1.5}vh;`"
                    frameborder="0"
                    @load="sendMindmapData"
                />
              </div>
              <div v-else class="text-center py-24">
                <div
                    class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gray-100 dark:bg-gray-800 mb-4">
                  <svg class="w-8 h-8 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor"
                       viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/>
                  </svg>
                </div>
                <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-2">思维导图</h3>
                <p class="text-gray-500 dark:text-gray-400">文章内容太少，无法生成思维导图</p>
              </div>
            </div>
          </div>
        </div>


      </div>
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
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {storeToRefs} from 'pinia';
import {useRouter} from 'vue-router';
import {useArticlesStore} from '../stores/articles/articles';
import {useCollectionsStore} from '../stores/collections';
import {useRecommendArticlesStore} from '../stores/articles/recommendArticles'
import MediaAttachment from "../components/MediaAttachment.vue";
import TocSection from "../components/TocSection.vue";
import ArticleCardList from "../components/ArticleCardList.vue";

// ==================== Types ====================
interface Props {
  id: string;
}

interface TocItem {
  id: string;
  text: string;
  level: number;
}

// ==================== Props & Stores ====================
const props = defineProps<Props>();

const router = useRouter();
const articlesStore = useArticlesStore();
const collectionsStore = useCollectionsStore();
const recommendArticlesStore = useRecommendArticlesStore();
const {currentArticle} = storeToRefs(articlesStore);
const {detailsItems, loading: detailsLoading} = storeToRefs(recommendArticlesStore);

// ==================== State ====================
const errorMessage = ref('');
const scrollTracked = ref(false);
const articleContentRef = ref<HTMLElement | null>(null);
const mindmapFrame = ref<HTMLIFrameElement | null>(null);
const tocItems = ref<TocItem[]>([]);
const headingElements = ref<HTMLElement[]>([]);
const activeHeadingId = ref('');
const imageCleanupFns = ref<Array<() => void>>([]);
const abortControllerRef = ref<AbortController | null>(null);
const activeMainTab = ref<'content' | 'summary' | 'mindmap'>('content');
const mindmapMarkdown = ref('');

let refreshTimer: ReturnType<typeof setTimeout> | null = null;

// ==================== Constants ====================
const HEADING_SCROLL_OFFSET = 80;
const SCROLL_PROGRESS_THRESHOLD = 0.3;

// ==================== Computed ====================
const article = computed(() => currentArticle.value);
const showSummary = computed(() => !!article.value?.summary);
const showToc = computed(() => tocItems.value.length > 0);
const showMindMap = computed(() => tocItems.value.length > 0 || article.value.mindMap != null);

// ==================== Utility Functions ====================
function throttle<T extends (...args: any[]) => void>(fn: T, wait = 100): T {
  let last = 0;
  return function (this: any, ...args: any[]) {
    const now = Date.now();
    if (now - last >= wait) {
      last = now;
      fn.apply(this, args);
    }
  } as T;
}

function debounce<T extends (...args: any[]) => void>(fn: T, wait = 200): T {
  let t: ReturnType<typeof setTimeout> | null = null;
  return function (this: any, ...args: any[]) {
    if (t) clearTimeout(t);
    t = setTimeout(() => fn.apply(this, args), wait);
  } as T;
}

const createSlug = (text: string, index: number): string => {
  const base = text
      .trim()
      .toLowerCase()
      .replace(/\s+/g, '-')
      .replace(/[^\p{L}\p{N}\-\u4e00-\u9fff]+/gu, '')
      .substring(0, 50);

  return base ? `${base}-${index}` : `section-${index}`;
};

const getScrollTop = (): number => {
  return window.scrollY ?? document.documentElement.scrollTop ?? document.body.scrollTop ?? 0;
};

// ==================== Mindmap Generation ====================

const sendMindmapData = () => {
  if (!mindmapFrame.value || !mindmapMarkdown.value) return;

  try {
    const iframe = mindmapFrame.value.contentWindow;
    if (iframe) {
      // Clean up markdown newlines
      const cleanedMarkdown = mindmapMarkdown.value
          .replaceAll('\\\\\\\\n', '\n')
          .replaceAll('\\\\n', '\n');
      iframe.postMessage(cleanedMarkdown, '*');
    }
  } catch (error) {
    console.error('Failed to send mindmap data:', error);
  }
};

// ==================== Heading Navigation ====================
const refreshHeadingNavigation = async () => {
  await nextTick();
  const container = articleContentRef.value;
  if (!container) {
    tocItems.value = [];
    headingElements.value = [];
    activeHeadingId.value = '';
    return;
  }

  const headings = Array.from(container.querySelectorAll('h1, h2, h3')) as HTMLElement[];
  if (!headings.length) {
    tocItems.value = [];
    headingElements.value = [];
    activeHeadingId.value = '';
    return;
  }

  const items: TocItem[] = headings.map((heading, index) => {
    const level = Number(heading.tagName[1]) || 1;
    const id = createSlug(heading.textContent ?? '', index);
    heading.id = id;

    return {
      id,
      text: heading.textContent?.trim() || `章节 ${index + 1}`,
      level,
    };
  });

  tocItems.value = items;
  headingElements.value = headings;


  if (typeof window !== 'undefined' && typeof window.requestAnimationFrame === 'function') {
    window.requestAnimationFrame(updateActiveHeading);
  } else {
    updateActiveHeading();
  }
};

const debouncedRefreshHeadingNavigation = () => {
  if (refreshTimer) clearTimeout(refreshTimer);
  refreshTimer = setTimeout(() => {
    refreshHeadingNavigation();
  }, 100);
};

const updateActiveHeading = () => {
  if (!headingElements.value.length) {
    activeHeadingId.value = '';
    return;
  }

  const scrollPosition = getScrollTop() + HEADING_SCROLL_OFFSET + 30;
  let currentId = headingElements.value[0].id || '';

  for (const heading of headingElements.value) {
    const top = heading.getBoundingClientRect().top + getScrollTop();
    if (scrollPosition >= top) {
      currentId = heading.id;
    } else {
      break;
    }
  }

  activeHeadingId.value = currentId;
};

const scrollToHeading = (id: string) => {
  if (!id) return;
  const target = document.getElementById(id);
  if (!target) return;

  const top = target.getBoundingClientRect().top + getScrollTop() - HEADING_SCROLL_OFFSET;
  window.scrollTo({
    top: Math.max(0, top),
    behavior: 'smooth',
  });
};

// ==================== Scroll Handling ====================
const handleScrollInternal = () => {
  updateActiveHeading();

  if (scrollTracked.value || !article.value) return;

  const maxScroll = document.documentElement.scrollHeight - window.innerHeight;
  if (maxScroll <= 0) {
    scrollTracked.value = true;
    articlesStore.recordHistory(props.id).catch(err => {
      console.warn('recordHistory failed', err);
    });
    return;
  }

  const progress = getScrollTop() / maxScroll;
  if (progress > SCROLL_PROGRESS_THRESHOLD) {
    scrollTracked.value = true;
    articlesStore.recordHistory(props.id).catch(err => {
      console.warn('recordHistory failed', err);
    });
  }
};

const handleScroll = throttle(handleScrollInternal, 120);
const handleResize = debounce(() => {
  refreshHeadingNavigation();
}, 150);

// ==================== Image Load Listeners ====================
const attachImageLoadListeners = () => {
  imageCleanupFns.value.forEach(fn => fn());
  imageCleanupFns.value = [];

  const container = articleContentRef.value;
  if (!container) return;

  const imgs = Array.from(container.querySelectorAll('img')) as HTMLImageElement[];
  imgs.forEach((img) => {
    if (img.complete) return;

    const onLoad = () => {
      debouncedRefreshHeadingNavigation();
    };

    img.addEventListener('load', onLoad, {once: true});

    imageCleanupFns.value.push(() => {
      img.removeEventListener('load', onLoad);
    });
  });
};

// ==================== Article Loading ====================
const loadArticle = async (articleId: string) => {
  if (!articleId) return;

  if (abortControllerRef.value) {
    abortControllerRef.value.abort();
  }

  abortControllerRef.value = new AbortController();
  const currentController = abortControllerRef.value;

  errorMessage.value = '';
  tocItems.value = [];
  headingElements.value = [];
  activeHeadingId.value = '';
  activeMainTab.value = 'content';
  mindmapMarkdown.value = '';

  try {
    await articlesStore.fetchArticleById(articleId, {
      signal: currentController.signal
    });
    if (currentController.signal.aborted) return;

    window.scrollTo({top: 0, behavior: 'auto'});
    await nextTick();

    scrollTracked.value = false;
    articlesStore.recordHistory(props.id).catch(err => {
      console.warn('recordHistory failed', err);
    });

    await refreshHeadingNavigation();
    mindmapMarkdown.value = article.value.mindMap;
    attachImageLoadListeners();

    await recommendArticlesStore.fetchDetailsArticles()
  } catch (err) {
    if (currentController.signal.aborted) return;
    console.error('文章详情加载失败', err);
    errorMessage.value = '文章加载失败,请稍后重试';
  }
};

// ==================== Actions ====================
const toggleCollection = async () => {
  if (!props.id || !article.value) return;

  try {
    await collectionsStore.toggleCollection(props.id, {
      title: article.value.title,
      collected: article.value.collected,
    });

    if (articlesStore.currentArticle) {
      articlesStore.currentArticle.collected = !articlesStore.currentArticle.collected;
    }
  } catch (err) {
    console.warn('收藏操作失败', err);
  }
};

const handleTagClick = (tag: string) => {
  if (!tag) return;
  router.push({name: 'feedsSubscriptions', query: {tags: tag.toLowerCase()}});
};

// ==================== Lifecycle ====================
onMounted(async () => {
  await loadArticle(props.id);
  window.addEventListener('scroll', handleScroll, {passive: true});
  window.addEventListener('resize', handleResize, {passive: true});
});

onBeforeUnmount(() => {
  if (abortControllerRef.value) {
    abortControllerRef.value.abort();
  }

  imageCleanupFns.value.forEach(fn => fn());
  imageCleanupFns.value = [];

  window.removeEventListener('scroll', handleScroll);
  window.removeEventListener('resize', handleResize);

  if (refreshTimer) {
    clearTimeout(refreshTimer);
    refreshTimer = null;
  }
});

// ==================== Watchers ====================
watch(
    () => props.id,
    async (newId) => {
      if (!newId) return;
      scrollTracked.value = false;
      await loadArticle(newId);
    }
);

watch(
    () => article.value?.content,
    async () => {
      if (errorMessage.value) return;
      debouncedRefreshHeadingNavigation();
      attachImageLoadListeners();
    }
);

watch(activeMainTab, async (newTab) => {
  if (newTab === 'mindmap' && mindmapMarkdown.value) {
    await nextTick();
    // 延迟发送，确保 iframe 已完全加载
    setTimeout(sendMindmapData, 200);
  }
});
</script>

<style scoped>
/* Custom scrollbar for TOC */
.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: rgba(156, 163, 175, 0.3) transparent;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(156, 163, 175, 0.3);
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(156, 163, 175, 0.5);
}

/* Article Content Styles - Clean & Flat */
.article-content {
  @apply text-base leading-relaxed text-gray-800 dark:text-gray-200;
}

.article-content :deep(p) {
  @apply mb-5 last:mb-0;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  @apply font-bold text-gray-900 dark:text-white mt-10 mb-5;
  scroll-margin-top: 100px;
}

.article-content :deep(h1) {
  @apply text-3xl;
}

.article-content :deep(h2) {
  @apply text-2xl;
}

.article-content :deep(h3) {
  @apply text-xl;
}

.article-content :deep(ul),
.article-content :deep(ol) {
  @apply mb-5 pl-6 space-y-2;
}

.article-content :deep(li) {
  @apply text-gray-700 dark:text-gray-300;
}

.article-content :deep(blockquote) {
  @apply border-l-4 border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-800/50 px-5 py-4 my-6 text-gray-700 dark:text-gray-300 italic;
}

.article-content :deep(pre) {
  @apply bg-gray-50 dark:bg-gray-900 p-5 my-6 overflow-x-auto rounded-lg;
}

.article-content :deep(pre code) {
  @apply bg-transparent p-0 rounded-none text-sm;
}

.article-content :deep(code) {
  @apply bg-gray-100 dark:bg-gray-800 text-blue-600 dark:text-blue-400 px-2 py-1 text-sm font-mono rounded;
}

.article-content :deep(img) {
  @apply w-full h-auto my-8 rounded-lg;
}

.article-content :deep(table) {
  @apply w-full border-collapse my-8 text-sm;
}

.article-content :deep(th),
.article-content :deep(td) {
  @apply border border-gray-200 dark:border-gray-700 px-4 py-3 text-left;
}

.article-content :deep(thead th) {
  @apply bg-gray-50 dark:bg-gray-800 font-semibold text-gray-900 dark:text-white;
}

.article-content :deep(tbody tr:hover) {
  @apply bg-gray-50 dark:bg-gray-800/30;
}

.article-content :deep(hr) {
  @apply border-0 h-px bg-gray-200 dark:bg-gray-700 my-8;
}

.article-content :deep(a) {
  @apply text-blue-600 dark:text-blue-400 hover:underline;
}

.tab-content {
  @apply min-h-[60vh];
}
</style>