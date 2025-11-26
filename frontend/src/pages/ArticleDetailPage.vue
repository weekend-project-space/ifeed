<template>
  <div class="mx-auto max-w-4xl px-4 py-8 sm:px-6">
    <!-- Loading State -->
    <div v-if="articlesStore.loading" class="flex flex-col items-center justify-center py-24 gap-3">
      <div class="relative h-10 w-10">
        <div class="absolute inset-0 rounded-full border-4 border-secondary/10"></div>
        <div class="absolute inset-0 animate-spin rounded-full border-4 border-transparent border-t-secondary"></div>
      </div>
      <p class="text-sm text-secondary/60">加载中...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="errorMessage" class="py-12">
      <div class="rounded-lg border border-red-200 bg-red-50 dark:bg-red-900/20 dark:border-red-800 p-6">
        <div class="flex items-start gap-3">
          <svg class="w-6 h-6 text-red-600 dark:text-red-400 flex-shrink-0 mt-0.5" fill="currentColor"
               viewBox="0 0 20 20">
            <path fill-rule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                  clip-rule="evenodd"/>
          </svg>
          <div class="flex-1">
            <p class="text-sm font-medium text-red-800 dark:text-red-200">{{ errorMessage }}</p>
            <button
                @click="loadArticle(props.id)"
                class="mt-3 text-sm font-medium text-red-600 dark:text-red-400 hover:underline"
            >
              点击重试
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Article Content -->
    <article v-else-if="article">
      <!-- Header -->
      <header class="space-y-3 border-b border-secondary/10 pb-6 mb-6">
        <!-- Meta Info -->
        <div class="flex items-center gap-2 text-sm text-secondary/60">
          <router-link
              v-if="article.feedId"
              :to="'/feeds/' + article.feedId"
              class="font-medium text-secondary hover:underline">
            {{ article.feedTitle }}
          </router-link>
          <span v-if="article.feedId">·</span>
          <span>{{ article.timeAgo }}</span>
        </div>

        <!-- Title -->
        <h1 class="text-3xl font-normal text-secondary leading-tight">
          {{ article.title }}
        </h1>

        <!-- Tags & Actions -->
        <div class="flex flex-wrap items-center gap-2">
          <div v-if="article.tags && article.tags.length" class="flex flex-wrap gap-2">
            <button
                v-for="tag in article.tags"
                :key="tag"
                type="button"
                class="px-3 py-1 text-xs font-medium rounded-full text-secondary hover:bg-secondary/10 transition-colors"
                @click="handleTagClick(tag)">
              #{{ tag }}
            </button>
          </div>
          <button
              class="ml-auto px-4 py-1.5 text-sm font-medium rounded-full transition-colors"
              :class="article.collected
              ? 'bg-secondary/10 text-secondary hover:bg-secondary/20'
              : 'bg-secondary/5 text-secondary hover:bg-secondary/10'"
              @click="toggleCollection">
            {{ article.collected ? '已收藏' : '收藏' }}
          </button>
        </div>
      </header>

      <div class="grid grid-cols-1 lg:grid-cols-[1fr_280px] gap-8">
        <!-- Main Content -->
        <div class="min-w-0">
          <!-- Summary (mobile) -->
          <aside-section
              v-if="showSummary"
              title="AI 摘要"
              class="lg:hidden mb-6">
            <p class="text-sm text-secondary/80 leading-relaxed" v-text="article.summary">
            </p>
          </aside-section>

          <!-- TOC (mobile) -->
          <toc-section
              v-if="showToc"
              :items="tocItems"
              :active-id="activeHeadingId"
              class="lg:hidden mb-6"
              @navigate="scrollToHeading"
          />

          <!-- Media Attachment -->
          <media-attachment
              v-if="article.enclosure"
              :url="article.enclosure"
              :type="article.enclosureType"
              :title="article.title"
              :artist="article.feedTitle || article.author"
              :cover-image="article.thumbnail"
              class="mb-6"
          />

          <!-- Article Body -->
          <div class="prose prose-gray dark:prose-invert max-w-none mb-6">
            <div
                v-if="article.content"
                ref="articleContentRef"
                class="article-content"
                v-html="article.content">
            </div>
            <p v-else class="text-secondary/50">
              暂无正文内容。
            </p>
          </div>

          <!-- Footer Links -->
          <footer class="flex flex-wrap items-center gap-4 pt-4 border-t border-secondary/10 text-sm">
            <a
                v-if="article.link"
                :href="article.link"
                target="_blank"
                rel="noopener"
                class="text-secondary hover:underline">
              查看原文
            </a>
            <span class="text-secondary/50">
              最后更新：{{ article.timeAgo }}
            </span>
          </footer>
        </div>

        <!-- Sidebar (desktop) -->
        <aside class="hidden lg:block ">
          <!-- Summary -->
          <div v-if="showSummary" class="mb-4">
            <aside-section title="AI 摘要">
              <p class="text-sm text-secondary/80 leading-relaxed" v-text="article.summary">
              </p>
            </aside-section>
          </div>

          <!-- TOC (Sticky) -->
          <div
              v-if="showToc"
              class="sticky top-20"
          >
            <toc-section
                :items="tocItems"
                :active-id="activeHeadingId"
                @navigate="scrollToHeading"
            />
          </div>
        </aside>
      </div>
    </article>

    <!-- Not Found State -->
    <div v-else class="flex items-center justify-center py-24">
      <p class="text-secondary/50">
        未找到文章。
      </p>
    </div>
  </div>
</template>


<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {storeToRefs} from 'pinia';
import {useRouter} from 'vue-router';
import {useArticlesStore} from '../stores/articles';
import {useCollectionsStore} from '../stores/collections';
import MediaAttachment from "../components/MediaAttachment.vue";
import AsideSection from "../components/AsideSection.vue";
import TocSection from "../components/TocSection.vue";

// ==================== Types ====================
interface Props {
  id: string;
}

interface Article {
  id: string;
  title: string;
  content?: string;
  summary?: string;
  feedId?: string;
  feedTitle?: string;
  timeAgo: string;
  tags?: string[];
  collected: boolean;
  link?: string;
  enclosure?: string;
  enclosureType?: string;
  thumbnail?: string;
  author?: string;
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
const {currentArticle} = storeToRefs(articlesStore);

// ==================== State ====================
const localArticle = ref<Article | null>(null);
const errorMessage = ref('');
const scrollTracked = ref(false);
const articleContentRef = ref<HTMLElement | null>(null);
const tocItems = ref<TocItem[]>([]);
const headingElements = ref<HTMLElement[]>([]);
const activeHeadingId = ref('');
const imageCleanupFns = ref<Array<() => void>>([]);
const abortControllerRef = ref<AbortController | null>(null);
const isInitialLoad = ref(true);

let refreshTimer: ReturnType<typeof setTimeout> | null = null;

// ==================== Constants ====================
const HEADING_SCROLL_OFFSET = 80;
const SCROLL_PROGRESS_THRESHOLD = 0.3;

// ==================== Computed ====================
const article = computed(() => localArticle.value);
const showSummary = computed(() => !!article.value?.summary);
const showToc = computed(() => tocItems.value.length > 0);

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

  const scrollPosition = getScrollTop() + HEADING_SCROLL_OFFSET + 30
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
    // 内容不够滚动,直接标记为已读
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
  // Cleanup old listeners
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

  // Cancel previous request
  if (abortControllerRef.value) {
    abortControllerRef.value.abort();
  }

  abortControllerRef.value = new AbortController();
  const currentController = abortControllerRef.value;

  // Reset state
  errorMessage.value = '';
  tocItems.value = [];
  headingElements.value = [];
  activeHeadingId.value = '';

  try {
    await articlesStore.fetchArticleById(articleId);

    // Check if request was cancelled
    if (currentController.signal.aborted) return;

    localArticle.value = currentArticle.value ? {...currentArticle.value} : null;

    // Scroll to top
    window.scrollTo({top: 0, behavior: 'instant'});
    await nextTick();

    // Reset scroll tracking and record history
    scrollTracked.value = false;
    articlesStore.recordHistory(props.id).catch(err => {
      console.warn('recordHistory failed', err);
    });

    await nextTick();
    await refreshHeadingNavigation();
    attachImageLoadListeners();

    isInitialLoad.value = false;
  } catch (err) {
    if (currentController.signal.aborted) return;
    console.error('文章详情加载失败', err);
    errorMessage.value = '文章加载失败,请稍后重试';
    localArticle.value = null;
  }
};

// ==================== Actions ====================
const toggleCollection = async () => {
  if (!props.id || !localArticle.value) return;

  try {
    await collectionsStore.toggleCollection(props.id, {
      title: localArticle.value.title,
      collected: localArticle.value.collected,
    });

    // Update local state
    if (localArticle.value) {
      localArticle.value.collected = !localArticle.value.collected;
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
  sessionStorage.setItem('origin', 'details')
  window.addEventListener('scroll', handleScroll, {passive: true});
  window.addEventListener('resize', handleResize, {passive: true});
});

onBeforeUnmount(() => {
  // Cancel pending requests
  if (abortControllerRef.value) {
    abortControllerRef.value.abort();
  }

  // Cleanup image listeners
  imageCleanupFns.value.forEach(fn => fn());
  imageCleanupFns.value = [];

  // Remove event listeners
  window.removeEventListener('scroll', handleScroll);
  window.removeEventListener('resize', handleResize);
  // Clear timers
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
      isInitialLoad.value = true;
      await loadArticle(newId);
    }
);

watch(
    () => currentArticle.value,
    (next) => {
      if (next && !errorMessage.value && !isInitialLoad.value) {
        localArticle.value = {...next};
        nextTick().then(() => {
          refreshHeadingNavigation();
          attachImageLoadListeners();
        });
      }
    }
);

watch(
    () => article.value?.content,
    async () => {
      if (errorMessage.value || isInitialLoad.value) return;
      await nextTick();
      debouncedRefreshHeadingNavigation();
      attachImageLoadListeners();
    }
);

</script>

<style scoped>
.article-content {
  @apply text-base leading-relaxed text-secondary/90;
}

.article-content :deep(p) {
  @apply mb-5 last:mb-0;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  @apply font-semibold text-secondary mt-8 mb-4;
  scroll-margin-top: 128px;
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
  @apply mb-6 pl-6;
}

.article-content :deep(li) {
  @apply mb-2;
}

.article-content :deep(blockquote) {
  @apply border-l-4 border-secondary/30 bg-secondary/5 px-6 py-4 my-6 rounded-r-lg;
}

.article-content :deep(pre) {
  @apply bg-secondary/5 p-4 my-6 rounded-lg overflow-x-auto border border-secondary/10;
}

.article-content :deep(pre code) {
  @apply bg-transparent p-0 rounded-none;
}

.article-content :deep(code) {
  @apply bg-secondary/10 text-secondary px-1.5 py-0.5 rounded text-sm;
}

.article-content :deep(img) {
  @apply w-full h-auto rounded-lg my-8;
}

.article-content :deep(table) {
  @apply w-full border-collapse my-8 text-sm border border-secondary/10;
}

.article-content :deep(th),
.article-content :deep(td) {
  @apply border border-secondary/10 px-4 py-2 text-left;
}

.article-content :deep(thead th) {
  @apply bg-secondary/5 font-semibold;
}

.article-content :deep(hr) {
  @apply border-0 h-px bg-secondary/10 my-8;
}
</style>