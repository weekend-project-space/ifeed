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
          <svg class="w-6 h-6 text-red-600 dark:text-red-400 flex-shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
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
    <article v-else-if="article" class="space-y-6">
      <!-- Header -->
      <header class="space-y-3 border-b border-secondary/10 pb-6">
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

      <div class="grid grid-cols-1 lg:grid-cols-[1fr_240px] gap-8">
        <!-- Main Content -->
        <div class="space-y-6 min-w-0">
          <!-- Summary (mobile) -->
          <section
              v-if="article.summary"
              class="lg:hidden p-4 bg-secondary/5 rounded-lg border border-secondary/10">
            <h2 class="text-xs font-semibold text-secondary/60 uppercase tracking-wide mb-2">
              AI 摘要
            </h2>
            <p class="text-sm text-secondary/80 leading-relaxed">
              {{ article.summary }}
            </p>
          </section>

          <!-- TOC (mobile) -->
          <nav
              v-if="tocItems.length"
              class="lg:hidden p-4 bg-secondary/5 rounded-lg border border-secondary/10">
            <h2 class="text-xs font-semibold text-secondary/60 uppercase tracking-wide mb-2">
              目录
            </h2>
            <div class="space-y-1">
              <button
                  v-for="item in tocItems"
                  :key="item.id"
                  type="button"
                  class="block w-full text-left py-1.5 px-2 text-sm rounded transition-colors"
                  :class="activeHeadingId === item.id
                  ? 'text-secondary bg-secondary/10 font-medium'
                  : 'text-secondary/70 hover:text-secondary hover:bg-secondary/5'"
                  :style="{ paddingLeft: `${getTocPadding(item.level)}px` }"
                  @click="scrollToHeading(item.id)">
                {{ item.text }}
              </button>
            </div>
          </nav>

          <!-- Media Attachment -->
          <media-attachment
              v-if="article.enclosure"
              :url="article.enclosure"
              :type="article.enclosureType"
              :title="article.title"
              :artist="article.feedTitle || article.author"
              :cover-image="article.thumbnail "
          />

          <!-- Article Body -->
          <div class="prose prose-gray dark:prose-invert max-w-none">
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
        <aside class="hidden lg:block">
          <div class="sticky top-8 space-y-4">
            <!-- Summary -->
            <section
                v-if="article.summary"
                class="p-4 bg-secondary/5 rounded-lg border border-secondary/10">
              <h2 class="text-xs font-semibold text-secondary/60 uppercase tracking-wide mb-2">
                AI 摘要
              </h2>
              <p class="text-sm text-secondary/80 leading-relaxed">
                {{ article.summary }}
              </p>
            </section>

            <!-- TOC -->
            <nav
                v-if="tocItems.length"
                class="p-4 bg-secondary/5 rounded-lg border border-secondary/10">
              <h2 class="text-xs font-semibold text-secondary/60 uppercase tracking-wide mb-2">
                目录
              </h2>
              <div class="space-y-1">
                <button
                    v-for="item in tocItems"
                    :key="item.id"
                    type="button"
                    class="block w-full text-left py-1.5 px-2 text-sm rounded transition-colors"
                    :class="activeHeadingId === item.id
                    ? 'text-secondary bg-secondary/10 font-medium'
                    : 'text-secondary/70 hover:text-secondary hover:bg-secondary/5'"
                    :style="{ paddingLeft: `${getTocPadding(item.level)}px` }"
                    @click="scrollToHeading(item.id)">
                  {{ item.text }}
                </button>
              </div>
            </nav>
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { useArticlesStore } from '../stores/articles';
import { useCollectionsStore } from '../stores/collections';
import MediaAttachment from "../components/MediaAttachment.vue";

interface Props {
  id: string;
}

interface TocItem {
  id: string;
  text: string;
  level: number;
}

const props = defineProps<Props>();

const router = useRouter();
const articlesStore = useArticlesStore();
const collectionsStore = useCollectionsStore();
const { currentArticle } = storeToRefs(articlesStore);

// State
const localArticle = ref<any | null>(null);
const article = computed(() => localArticle.value);
const errorMessage = ref('');

const scrollTracked = ref(false);
const articleContentRef = ref<HTMLElement | null>(null);
const tocItems = ref<TocItem[]>([]);
const headingElements = ref<HTMLElement[]>([]);
const activeHeadingId = ref('');
const imageCleanupFns = ref<Array<() => void>>([]);

const HEADING_SCROLL_OFFSET = 128;

// Utilities
function throttle(fn: (...args: any[]) => void, wait = 100) {
  let last = 0;
  return function (...args: any[]) {
    const now = Date.now();
    if (now - last >= wait) {
      last = now;
      fn(...args);
    }
  };
}

function debounce(fn: (...args: any[]) => void, wait = 200) {
  let t: ReturnType<typeof setTimeout> | null = null;
  return function (...args: any[]) {
    if (t) clearTimeout(t);
    t = setTimeout(() => fn(...args), wait);
  };
}

// Simplified slug creator
const createSlug = (text: string, index: number) => {
  const base = text
      .trim()
      .toLowerCase()
      .replace(/\s+/g, '-')
      .replace(/[^\p{L}\p{N}\-\u4e00-\u9fff]+/gu, '')
      .substring(0, 50);

  return base ? `${base}-${index}` : `section-${index}`;
};

const getTocPadding = (level: number) => 12 + Math.max(0, level - 1) * 12;

// Heading navigation
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
    window.requestAnimationFrame(() => {
      updateActiveHeading();
    });
  } else {
    updateActiveHeading();
  }
};

const getScrollTop = () => {
  return window.scrollY ?? document.documentElement.scrollTop ?? document.body.scrollTop ?? 0;
};

const updateActiveHeading = () => {
  if (!headingElements.value.length) {
    activeHeadingId.value = '';
    return;
  }

  const scrollPosition = getScrollTop() + HEADING_SCROLL_OFFSET;
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
    top: top < 0 ? 0 : top,
    behavior: 'smooth',
  });
};

// Scroll handling
const handleScrollInternal = () => {
  updateActiveHeading();

  if (scrollTracked.value) return;

  const maxScroll = document.documentElement.scrollHeight - window.innerHeight;
  if (maxScroll <= 0) return;

  const progress = getScrollTop() / maxScroll;
  if (progress > 0.3) {
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

// Image load listeners with cleanup
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
      nextTick().then(() => refreshHeadingNavigation());
    };

    img.addEventListener('load', onLoad, { once: true });

    imageCleanupFns.value.push(() => {
      img.removeEventListener('load', onLoad);
    });
  });
};

// Load article
const loadArticle = async (articleId: string) => {
  if (!articleId) return;

  // Reset state
  errorMessage.value = '';
  tocItems.value = [];
  headingElements.value = [];
  activeHeadingId.value = '';

  try {
    await articlesStore.fetchArticleById(articleId);

    // Shallow copy instead of deep copy
    localArticle.value = currentArticle.value ? { ...currentArticle.value } : null;

    // Scroll to top first, then reset tracking
    window.scrollTo({ top: 0, behavior: 'instant' });
    await nextTick();
    scrollTracked.value = false;

    await nextTick();
    await refreshHeadingNavigation();
    attachImageLoadListeners();
  } catch (err) {
    console.error('文章详情加载失败', err);
    errorMessage.value = '文章加载失败，请稍后重试';
    localArticle.value = null;
  }
};

// Collection toggle
const toggleCollection = async () => {
  if (!props.id) return;
  try {
    await collectionsStore.toggleCollection(props.id, {
      title: localArticle.value?.title,
      collected: !!localArticle.value?.collected,
    });

    await loadArticle(props.id);
  } catch (err) {
    console.warn('收藏操作失败', err);
  }
};

// Tag click
const handleTagClick = (tag: string) => {
  if (!tag) return;
  router.push({ name: 'feedsSubscriptions', query: { tags: tag.toLowerCase() } });
};

// Lifecycle
onMounted(async () => {
  await loadArticle(props.id);
  window.addEventListener('scroll', handleScroll, { passive: true });
  window.addEventListener('resize', handleResize, { passive: true });
});

onBeforeUnmount(() => {
  // Cleanup image listeners
  imageCleanupFns.value.forEach(fn => fn());
  imageCleanupFns.value = [];

  window.removeEventListener('scroll', handleScroll);
  window.removeEventListener('resize', handleResize);
});

// Watch for id changes
watch(
    () => props.id,
    async (next) => {
      scrollTracked.value = false;
      await loadArticle(next);
    }
);

// Watch for store updates
watch(
    () => currentArticle.value,
    (next) => {
      if (next && !errorMessage.value) {
        localArticle.value = { ...next };
        nextTick().then(() => {
          refreshHeadingNavigation();
          attachImageLoadListeners();
        });
      }
    },
    { immediate: true }
);

// Watch for content changes
watch(
    () => article.value?.content,
    async () => {
      if (errorMessage.value) return;
      await nextTick();
      await refreshHeadingNavigation();
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