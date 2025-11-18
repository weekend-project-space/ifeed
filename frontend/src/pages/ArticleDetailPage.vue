<template>
  <div class="min-h-screen dark:bg-gray-900">
    <div class="mx-auto max-w-4xl px-4 py-8 sm:px-6">
      <!-- Loading State -->
      <div v-if="articlesStore.loading" class="flex items-center justify-center py-24">
        <div class="inline-block h-8 w-8 animate-spin rounded-full border-4 border-gray-200 dark:border-gray-700 border-t-secondary-600"></div>
      </div>

      <!-- Article Content -->
      <article v-else-if="article" class="space-y-6">
        <!-- Header -->
        <header class="space-y-3 border-b border-gray-200 dark:border-gray-800 pb-6">
          <!-- Meta Info -->
          <div class="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
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
          <h1 class="text-3xl font-normal text-gray-900 dark:text-gray-100 leading-tight">
            {{ article.title }}
          </h1>

          <!-- Tags & Actions -->
          <div class="flex flex-wrap items-center gap-2">
            <div v-if="article.tags.length" class="flex flex-wrap gap-2">
              <button
                  v-for="tag in article.tags"
                  :key="tag"
                  type="button"
                  class="px-3 py-1 text-xs font-medium rounded-full bg-secondary/10 text-secondary hover:bg-secondary/20 transition-colors"
                  @click="handleTagClick(tag)">
                {{ tag }}
              </button>
            </div>
            <button
                class="ml-auto px-4 py-1.5 text-sm font-medium rounded-full transition-colors"
                :class="isCollected
                ? 'bg-secondary/10 text-secondary hover:bg-secondary/20'
                : 'bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700'"
                @click="toggleCollection">
              {{ isCollected ? '已收藏' : '收藏' }}
            </button>
          </div>
        </header>

        <div class="grid grid-cols-1 lg:grid-cols-[1fr_240px] gap-8">
          <!-- Main Content -->
          <div class="space-y-6 min-w-0">
            <!-- Summary (mobile) -->
            <section
                v-if="article.summary"
                class="lg:hidden p-4 bg-gray-50 dark:bg-gray-800/50 rounded-lg border border-gray-200 dark:border-gray-700">
              <h2 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">
                AI 摘要
              </h2>
              <p class="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
                {{ article.summary }}
              </p>
            </section>

            <!-- TOC (mobile) -->
            <nav
                v-if="tocItems.length"
                class="lg:hidden p-4 bg-gray-50 dark:bg-gray-800/50 rounded-lg border border-gray-200 dark:border-gray-700">
              <h2 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">
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
                    : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-800'"
                    :style="{ paddingLeft: `${getTocPadding(item.level)}px` }"
                    @click="scrollToHeading(item.id)">
                  {{ item.text }}
                </button>
              </div>
            </nav>

            <!-- Article Body -->
            <div class="prose prose-gray dark:prose-invert max-w-none">
              <div
                  v-if="article.content"
                  ref="articleContentRef"
                  class="article-content"
                  v-html="article.content">
              </div>
              <p v-else class="text-gray-500 dark:text-gray-400">
                暂无正文内容。
              </p>
            </div>

            <!-- Footer Links -->
            <footer class="flex flex-wrap items-center gap-4 pt-4 border-t border-gray-200 dark:border-gray-800 text-sm">
              <a
                  v-if="article.link"
                  :href="article.link"
                  target="_blank"
                  rel="noopener"
                  class="text-secondary hover:underline">
                查看原文
              </a>
              <a
                  v-if="article.enclosure"
                  :href="article.enclosure"
                  target="_blank"
                  rel="noopener"
                  class="text-secondary hover:underline">
                查看附件
              </a>
              <span class="text-gray-500 dark:text-gray-400">
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
                  class="p-4 bg-gray-50 dark:bg-gray-800/50 rounded-lg border border-gray-200 dark:border-gray-700">
                <h2 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">
                  AI 摘要
                </h2>
                <p class="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
                  {{ article.summary }}
                </p>
              </section>

              <!-- TOC -->
              <nav
                  v-if="tocItems.length"
                  class="p-4 bg-gray-50 dark:bg-gray-800/50 rounded-lg border border-gray-200 dark:border-gray-700">
                <h2 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">
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
                      : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-800'"
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

      <!-- Error State -->
      <div v-else class="flex items-center justify-center py-24">
        <p class="text-gray-500 dark:text-gray-400">
          未找到文章或加载失败。
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { useArticlesStore } from '../stores/articles';
import { useCollectionsStore } from '../stores/collections';

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
const { items: collectionItems } = storeToRefs(collectionsStore);

const article = computed(() => currentArticle.value);
const collectionState = computed(() => collectionsStore.isCollected(props.id));
const isCollected = computed(() => {
  if (article.value && typeof article.value.collected === 'boolean') {
    return article.value.collected;
  }
  return collectionState.value;
});

watch(collectionState, (value) => {
  if (article.value) {
    article.value.collected = value;
  }
});

const scrollTracked = ref(false);
const articleContentRef = ref<HTMLElement | null>(null);
const tocItems = ref<TocItem[]>([]);
const headingElements = ref<HTMLElement[]>([]);
const activeHeadingId = ref('');
const HEADING_SCROLL_OFFSET = 128;

const loadArticle = async (articleId: string) => {
  if (!articleId) return;

  tocItems.value = [];
  headingElements.value = [];
  activeHeadingId.value = '';

  try {
    await articlesStore.fetchArticleById(articleId);
    articlesStore.recordHistory(articleId);
    if (!collectionItems.value.length && typeof article.value?.collected !== 'boolean') {
      await collectionsStore.fetchCollections();
    }
  } catch (err) {
    console.warn('文章详情加载失败', err);
  }
};

const toggleCollection = async () => {
  try {
    await collectionsStore.toggleCollection(props.id, { title: article.value?.title });
    if (article.value) {
      article.value.collected = !article.value.collected;
    }
  } catch (err) {
    console.warn('收藏操作失败', err);
  }
};

const handleTagClick = (tag: string) => {
  if (!tag) return;
  router.push({ name: 'feedsSubscriptions', query: { tags: tag.toLowerCase() } });
};

const createSlug = (text: string, index: number, used: Map<string, number>) => {
  const normalized = text
      .trim()
      .toLowerCase()
      .replace(/\s+/g, '-')
      .replace(/[^\w\-一-龥]+/g, '');
  const base = normalized || `section-${index + 1}`;
  const count = used.get(base);
  if (count == null) {
    used.set(base, 0);
    return base;
  }
  const nextCount = count + 1;
  used.set(base, nextCount);
  return `${base}-${nextCount}`;
};

const getTocPadding = (level: number) => 12 + Math.max(0, level - 1) * 12;

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

  const usedIds = new Map<string, number>();
  const items: TocItem[] = headings.map((heading, index) => {
    const level = Number(heading.tagName[1]) || 1;
    let id = heading.getAttribute('id')?.trim() ?? '';
    if (id) {
      const seen = usedIds.get(id);
      if (seen != null) {
        const nextCount = seen + 1;
        usedIds.set(id, nextCount);
        id = `${id}-${nextCount}`;
        heading.id = id;
      } else {
        usedIds.set(id, 0);
        heading.id = id;
      }
    } else {
      const slug = createSlug(heading.textContent ?? '', index, usedIds);
      heading.id = slug;
      id = slug;
    }

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

const updateActiveHeading = () => {
  if (!headingElements.value.length) {
    activeHeadingId.value = '';
    return;
  }

  const scrollPosition = window.scrollY + HEADING_SCROLL_OFFSET;
  let currentId = headingElements.value[0].id;

  for (const heading of headingElements.value) {
    const top = heading.getBoundingClientRect().top + window.scrollY;
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

  const top = target.getBoundingClientRect().top + window.scrollY - HEADING_SCROLL_OFFSET;
  window.scrollTo({
    top: top < 0 ? 0 : top,
    behavior: 'smooth',
  });
};

const handleScroll = () => {
  updateActiveHeading();
  if (scrollTracked.value) return;

  const maxScroll = document.documentElement.scrollHeight - window.innerHeight;
  if (maxScroll <= 0) return;

  const progress = window.scrollY / maxScroll;
  if (progress > 0.3) {
    scrollTracked.value = true;
    articlesStore.recordHistory(props.id);
  }
};

onMounted(() => {
  loadArticle(props.id);
  window.addEventListener('scroll', handleScroll, { passive: true });
  window.addEventListener('resize', updateActiveHeading);
  refreshHeadingNavigation();
});

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll);
  window.removeEventListener('resize', updateActiveHeading);
});

watch(
    () => props.id,
    (next) => {
      scrollTracked.value = false;
      loadArticle(next);
    }
);

watch(
    () => article.value?.content,
    async () => {
      await refreshHeadingNavigation();
    }
);
</script>

<style scoped>
.article-content {
  @apply text-base leading-relaxed text-gray-800 dark:text-gray-200;
}

.article-content :deep(p) {
  @apply mb-5 last:mb-0;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  @apply font-semibold text-gray-900 dark:text-gray-100 mt-8 mb-4;
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
  @apply border-l-4 border-gray-300 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50 px-6 py-4 my-6 rounded-r-lg;
}

.article-content :deep(pre) {
  @apply bg-gray-100 dark:bg-gray-800 p-4 my-6 rounded-lg overflow-x-auto border border-gray-200 dark:border-gray-700;
}

.article-content :deep(pre code) {
  @apply bg-transparent p-0 rounded-none;
}

.article-content :deep(code) {
  @apply bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-gray-100 px-1.5 py-0.5 rounded text-sm;
}

.article-content :deep(img) {
  @apply w-full h-auto rounded-lg my-8;
}

.article-content :deep(table) {
  @apply w-full border-collapse my-8 text-sm border border-gray-200 dark:border-gray-700;
}

.article-content :deep(th),
.article-content :deep(td) {
  @apply border border-gray-200 dark:border-gray-700 px-4 py-2 text-left;
}

.article-content :deep(thead th) {
  @apply bg-gray-50 dark:bg-gray-800 font-semibold;
}

.article-content :deep(hr) {
  @apply border-0 h-px bg-gray-200 dark:bg-gray-700 my-8;
}
</style>