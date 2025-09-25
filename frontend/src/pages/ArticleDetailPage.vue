<template>
  <div class="flex h-full flex-col gap-6 pb-16 pt-6">
    <button
      class="inline-flex items-center text-sm font-medium text-primary transition hover:opacity-80"
      @click="goBack"
    >
      ← 返回列表
    </button>

    <div class="flex-1">
      <div
        v-if="articlesStore.loading"
        class="w-full rounded-3xl border border-outline/40 bg-surface-container p-12 text-center text-text-muted"
      >
        正在加载文章...
      </div>

      <div
        v-else-if="article"
        class="w-full space-y-8 rounded-3xl border border-outline/40 bg-surface-container p-8"
      >
        <header class="space-y-5">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div class="space-y-2">
              <p class="text-sm text-text-muted">{{ article.feedTitle }} · {{ article.timeAgo }}</p>
              <h1 class="text-3xl font-semibold leading-tight text-text">{{ article.title }}</h1>
            </div>
            <button
              class="inline-flex items-center gap-2 rounded-full border border-outline/50 px-5 py-2 text-sm font-medium transition hover:border-primary/60 hover:text-primary"
              :class="isCollected ? 'border-transparent bg-primary/15 text-primary' : ''"
              @click="toggleCollection"
            >
              <span>{{ isCollected ? '已收藏' : '收藏' }}</span>
            </button>
          </div>
          <div v-if="article.tags.length" class="flex flex-wrap gap-2 text-xs text-primary">
            <button
              v-for="tag in article.tags"
              :key="tag"
              type="button"
              class="rounded-full bg-primary/10 px-3 py-1 transition hover:bg-primary/15"
              @click="handleTagClick(tag)"
            >
              #{{ tag }}
            </button>
          </div>
        </header>
        <section class="rounded-2xl border border-outline/30 bg-surface-variant/70 p-5 leading-relaxed text-text-secondary">
          <h2 class="mb-2 text-sm font-semibold uppercase tracking-wide text-text-muted">AI 摘要</h2>
          <p class="text-base text-text">{{ article.summary }}</p>
        </section>
        <section class="space-y-4 text-text">
          <h2 class="text-lg font-semibold">正文内容</h2>
          <div v-if="article.content" class="article-content" v-html="article.content"></div>
          <p v-else class="text-text-muted">暂无正文内容。</p>
        </section>
        <footer class="flex flex-wrap items-center gap-4 border-t border-outline/30 pt-4 text-sm text-text-secondary">
          <a
            v-if="article.link"
            :href="article.link"
            target="_blank"
            rel="noopener"
            class="font-medium text-primary transition hover:opacity-80"
          >
            在原文中打开
          </a>
          <a
            v-if="article.enclosure"
            :href="article.enclosure"
            target="_blank"
            rel="noopener"
            class="font-medium text-primary transition hover:opacity-80"
          >
            查看附件
          </a>
          <span>最后更新：{{ article.timeAgo }}</span>
        </footer>
      </div>

      <div
        v-else
        class="w-full rounded-3xl border border-outline/40 bg-surface-container p-12 text-center text-text-muted"
      >
        未找到文章或加载失败。
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { useArticlesStore } from '../stores/articles';
import { useCollectionsStore } from '../stores/collections';

interface Props {
  id: string;
}

const props = defineProps<Props>();

const router = useRouter();
const articlesStore = useArticlesStore();
const collectionsStore = useCollectionsStore();
const { currentArticle } = storeToRefs(articlesStore);
const { items: collectionItems } = storeToRefs(collectionsStore);

const article = computed(() => currentArticle.value);
const isCollected = computed(() => collectionsStore.isCollected(props.id));
const scrollTracked = ref(false);

const loadArticle = async (articleId: string) => {
  if (!articleId) {
    return;
  }
  try {
    await articlesStore.fetchArticleById(articleId);
    articlesStore.recordHistory(articleId);
    if (!collectionItems.value.length) {
      await collectionsStore.fetchCollections();
    }
  } catch (err) {
    console.warn('文章详情加载失败', err);
  }
};

const toggleCollection = async () => {
  try {
    await collectionsStore.toggleCollection(props.id, { title: article.value?.title });
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
  router.push({ name: 'home', query: { tags: tag.toLowerCase() } });
};

const handleScroll = () => {
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
});

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll);
});

watch(
  () => props.id,
  (next) => {
    scrollTracked.value = false;
    loadArticle(next);
  }
);
</script>

<style scoped>
.article-content {
  font-size: 1rem;
  line-height: 1.8;
  color: rgb(var(--md-text));
}

.article-content :deep(p) {
  margin-bottom: 1.25rem;
  color: inherit;
}

.article-content :deep(p:last-child) {
  margin-bottom: 0;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  font-weight: 600;
  color: rgb(var(--md-text));
  margin-top: 2.5rem;
  margin-bottom: 1rem;
}

.article-content :deep(h1) {
  font-size: 1.875rem;
}

.article-content :deep(h2) {
  font-size: 1.5rem;
}

.article-content :deep(h3) {
  font-size: 1.25rem;
}

.article-content :deep(a) {
  color: rgb(var(--md-primary));
  text-decoration: underline;
}

.article-content :deep(ul),
.article-content :deep(ol) {
  margin-bottom: 1.5rem;
  padding-left: 1.5rem;
}

.article-content :deep(li) {
  margin-bottom: 0.5rem;
}

.article-content :deep(blockquote) {
  border-left: 3px solid rgba(var(--md-primary), 0.35);
  background: rgba(var(--md-primary), 0.1);
  padding: 1rem 1.25rem;
  margin: 1.5rem 0;
  color: rgb(var(--md-primary));
}

.article-content :deep(pre) {
  background: rgb(var(--md-inverse));
  color: rgb(var(--md-on-inverse));
  padding: 1rem 1.25rem;
  margin: 1.75rem 0;
  border-radius: 0.75rem;
  overflow-x: auto;
}

.article-content :deep(code) {
  background: rgba(var(--md-outline), 0.25);
  color: rgb(var(--md-text));
  padding: 0.15rem 0.4rem;
  border-radius: 0.4rem;
  font-size: 0.95rem;
}

.article-content :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
  border-radius: 0;
}

.article-content :deep(img) {
  display: block;
  width: 100%;
  height: auto;
  border-radius: 1rem;
  margin: 2rem 0;
}

.article-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 2rem 0;
  font-size: 0.95rem;
}

.article-content :deep(th),
.article-content :deep(td) {
  border: 1px solid rgba(var(--md-outline), 0.4);
  padding: 0.75rem 0.9rem;
  text-align: left;
}

.article-content :deep(thead th) {
  background: rgba(var(--md-outline), 0.15);
}
</style>
