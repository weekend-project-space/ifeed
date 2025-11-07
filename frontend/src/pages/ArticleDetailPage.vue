<template>
  <div class="flex h-full flex-col gap-4 pb-12 pt-4 sm:gap-6 sm:pb-16 sm:pt-6">
    <button class="inline-flex items-center text-sm font-medium text-primary transition hover:opacity-80"
      @click="goBack">
      ← 返回列表
    </button>

    <div class="flex-1">
      <div v-if="articlesStore.loading"
        class="mx-auto w-full max-w-4xl rounded-3xl border border-outline/40 bg-surface-container p-8 text-center text-text-muted sm:p-10 md:p-12">
        正在加载文章...
      </div>

      <div v-else-if="article"
        class="mx-auto w-full max-w-5xl space-y-6 rounded-3xl border border-outline/20 bg-surface p-4 sm:space-y-8 sm:p-6 md:space-y-10 md:p-10">
        <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:gap-8 xl:gap-10">
          <div class="flex-1 space-y-6 lg:max-w-[720px] lg:space-y-8">
            <header class="space-y-4 sm:space-y-5">
              <div class="flex flex-wrap items-start justify-between gap-3 sm:gap-4">
                <div class="space-y-2">
                  <p class="text-sm text-text-muted">
                    <router-link v-if="article.feedId" :to="'/feeds/' + article.feedId"
                      class="font-medium text-primary transition hover:opacity-80">{{ article.feedTitle }}
                    </router-link> · {{ article.timeAgo }}
                  </p>
                  <h1 class="text-3xl font-semibold leading-tight text-text">{{ article.title }}</h1>
                </div>
                <button
                  class="inline-flex items-center gap-2 rounded-full border border-outline/50 px-4 py-1.5 text-sm font-medium transition hover:border-primary/60 hover:text-primary sm:px-5 sm:py-2"
                  :class="isCollected ? 'border-transparent bg-primary/15 text-primary' : ''" @click="toggleCollection">
                  <span>{{ isCollected ? '已收藏' : '收藏' }}</span>
                </button>
              </div>
              <div v-if="article.tags.length" class="flex flex-wrap gap-2 text-xs text-primary">
                <button v-for="tag in article.tags" :key="tag" type="button"
                  class="rounded-full bg-primary/10 px-3 py-1 transition hover:bg-primary/15"
                  @click="handleTagClick(tag)">
                  #{{ tag }}
                </button>
              </div>
            </header>

            <div v-if="tocItems.length"
              class="toc-container rounded-2xl border border-outline/20 bg-surface p-3 text-sm text-text-secondary sm:p-4 lg:hidden">
              <div class="toc-header mb-3 flex items-center justify-between text-xs font-semibold text-text-muted">
                <span class="toc-title">章节导航</span>
                <span class="toc-hint text-[10px] text-text-disabled">点击跳转</span>
              </div>
              <nav class="toc-list">
                <button v-for="item in tocItems" :key="item.id" type="button"
                  class="toc-item block w-full rounded-lg py-2 pr-3 text-left text-sm transition"
                  :class="activeHeadingId === item.id ? 'active bg-primary/10 text-primary font-medium' : 'text-text-secondary hover:bg-primary/5 hover:text-text'"
                  :style="{ paddingLeft: `${getTocPadding(item.level)}px` }" @click="scrollToHeading(item.id)">
                  {{ item.text }}
                </button>
              </nav>
            </div>

            <section v-if="article.summary"
              class="rounded-2xl border border-outline/20 bg-surface-variant/60 p-4 leading-relaxed text-text-secondary sm:p-5 lg:hidden">
              <h2 class="mb-2 text-sm font-semibold uppercase tracking-wide text-text-muted">AI 摘要</h2>
              <p class="text-base text-text">{{ article.summary }}</p>
            </section>
            <section class="space-y-3 text-text sm:space-y-4">
              <h2 class="text-lg font-semibold">正文内容</h2>
              <div v-if="article.content" ref="articleContentRef" class="article-content" v-html="article.content">
              </div>
              <p v-else class="text-text-muted">暂无正文内容。</p>
            </section>
            <footer
              class="flex flex-wrap items-center gap-3 border-t border-outline/30 pt-3 text-sm text-text-secondary sm:gap-4 sm:pt-4">
              <a v-if="article.link" :href="article.link" target="_blank" rel="noopener"
                class="font-medium text-primary transition hover:opacity-80">
                在原文中打开
              </a>
              <a v-if="article.enclosure" :href="article.enclosure" target="_blank" rel="noopener"
                class="font-medium text-primary transition hover:opacity-80">
                查看附件
              </a>
              <span>最后更新：{{ article.timeAgo }}</span>
            </footer>
          </div>
          <aside v-if="article.summary || tocItems.length"
            class="sticky top-24 hidden w-full max-w-xs shrink-0 space-y-5 lg:block">
            <div v-if="article.summary"
              class="rounded-2xl border border-outline/20 bg-surface p-5 text-sm leading-relaxed text-text-secondary">
              <h2 class="mb-3 text-xs font-semibold uppercase tracking-wide text-text-muted">AI 摘要</h2>
              <p class="text-base text-text">{{ article.summary }}</p>
            </div>
            <div v-if="tocItems.length"
              class="toc-container space-y-2 rounded-2xl border border-outline/20 bg-surface p-4 text-sm">
              <div class="toc-header text-xs font-semibold text-text-muted">章节导航</div>
              <nav class="toc-list">
                <button v-for="item in tocItems" :key="item.id" type="button"
                  class="toc-item block w-full rounded-lg py-2 pr-3 text-left text-sm transition"
                  :class="activeHeadingId === item.id ? 'active bg-primary/10 text-primary font-medium' : 'text-text-secondary hover:bg-primary/5 hover:text-text'"
                  :style="{ paddingLeft: `${getTocPadding(item.level)}px` }" @click="scrollToHeading(item.id)">
                  {{ item.text }}
                </button>
              </nav>
            </div>
          </aside>
        </div>
      </div>

      <div v-else
        class="w-full rounded-3xl border border-outline/40 bg-surface-container p-8 text-center text-text-muted sm:p-10 md:p-12">
        未找到文章或加载失败。
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
  if (!articleId) {
    return;
  }
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

const goBack = () => {
  router.back();
};

const handleTagClick = (tag: string) => {
  if (!tag) {
    return;
  }
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
  if (!id) {
    return;
  }
  const target = document.getElementById(id);
  if (!target) {
    return;
  }

  const top = target.getBoundingClientRect().top + window.scrollY - HEADING_SCROLL_OFFSET;
  window.scrollTo({
    top: top < 0 ? 0 : top,
    behavior: 'smooth',
  });
};

const handleScroll = () => {
  updateActiveHeading();
  if (scrollTracked.value) {
    return;
  }
  const maxScroll = document.documentElement.scrollHeight - window.innerHeight;
  if (maxScroll <= 0) {
    return;
  }
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

<style lang="scss">
.article-content {
  font-size: 1rem;
  line-height: 1.8;
  color: rgb(var(--md-text));

  p {
    margin-bottom: 1.25rem;
    color: inherit;

    &:last-child {
      margin-bottom: 0;
    }
  }

  h1,
  h2,
  h3 {
    font-weight: 600;
    color: rgb(var(--md-text));
    margin-top: 2.5rem;
    margin-bottom: 1rem;
    scroll-margin-top: 128px;
  }

  h1 {
    font-size: 1.875rem;
  }

  h2 {
    font-size: 1.5rem;
  }

  h3 {
    font-size: 1.25rem;
  }

  a {
    color: rgb(var(--md-primary));
    text-decoration: underline;
    transition: color 0.2s ease;
  }

  ul,
  ol {
    margin-bottom: 1.5rem;
    padding-left: 1.5rem;
  }

  li {
    margin-bottom: 0.5rem;
  }

  blockquote {
    border-left: 3px solid rgb(var(--md-primary) / 0.35);
    background: rgb(var(--md-primary) / 0.08);
    padding: 1.25rem 1.5rem;
    margin: 2rem 0;
    border-radius: 1rem;
    color: rgb(var(--md-text));
  }

  pre {
    background: rgb(var(--md-outline) / 0.1);
    padding: 1.1rem 1.4rem;
    margin: 1.75rem 0;
    border-radius: 0.75rem;
    overflow-x: auto;
    border: 1px solid rgb(var(--md-outline) / 0.25);

    code {
      background: transparent;
      color: inherit;
      padding: 0;
      border-radius: 0;
    }
  }

  code {
    background: rgb(var(--md-outline) / 0.18);
    color: rgb(var(--md-text));
    padding: 0.2rem 0.45rem;
    border-radius: 0.45rem;
    font-size: 0.95rem;
  }

  img {
    display: block;
    width: 100%;
    height: auto;
    border-radius: 1rem;
    margin: 2rem 0;
  }

  table {
    width: 100%;
    border-collapse: collapse;
    margin: 2rem 0;
    font-size: 0.95rem;
    border: 1px solid rgb(var(--md-outline) / 0.2);
  }

  th,
  td {
    border: 1px solid rgb(var(--md-outline) / 0.2);
    padding: 0.75rem 0.9rem;
    text-align: left;
  }

  thead th {
    background: rgb(var(--md-outline) / 0.12);
    font-weight: 600;
  }

  hr {
    border: none;
    height: 1px;
    background: rgb(var(--md-outline) / 0.35);
    margin: 2.5rem 0;
  }
}


.toc-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.toc-item {
  border-left: 2px solid transparent;
  background: transparent;
  color: rgba(var(--md-text), 0.75);
  transition: background 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.toc-item:hover {
  border-left-color: rgba(var(--md-outline), 0.35);
  background: rgba(var(--md-primary), 0.08);
  color: rgb(var(--md-text));
}

.toc-item.active {
  border-left-color: rgba(var(--md-primary), 0.65);
  background: rgba(var(--md-primary), 0.12);
  color: rgb(var(--md-primary));
  font-weight: 600;
}

.toc-item:focus-visible {
  outline: 2px solid rgba(var(--md-primary), 0.35);
  outline-offset: 2px;
}

@media (max-width: 768px) {
  .article-content {
    font-size: 0.98rem;
  }

  .article-content :deep(h1) {
    font-size: 1.65rem;
    margin-top: 2rem;
  }

  .article-content :deep(h2) {
    font-size: 1.35rem;
    margin-top: 2rem;
  }

  .article-content :deep(h3) {
    font-size: 1.15rem;
    margin-top: 1.75rem;
  }
}

@media (prefers-reduced-motion: reduce) {

  .toc-container,
  .toc-item {
    transition: none;
  }
}
</style>
