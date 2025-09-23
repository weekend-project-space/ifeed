<template>
  <div class="flex h-full flex-col gap-6 pb-16 pt-6">
    <button class="inline-flex items-center text-sm font-medium text-blue-500 transition hover:text-blue-600"
      @click="goBack">
      ← 返回列表
    </button>

    <div class="flex-1">
      <div v-if="articlesStore.loading"
        class="w-full rounded-3xl border border-slate-200 bg-white p-12 text-center text-slate-400 shadow-sm">
        正在加载文章...
      </div>

      <div v-else-if="article" class="w-full space-y-8 rounded-3xl border border-slate-200 bg-white p-8 shadow-sm">
        <header class="space-y-5">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div class="space-y-2">
              <p class="text-sm text-slate-400">{{ article.feedTitle }} · {{ article.timeAgo }}</p>
              <h1 class="text-3xl font-semibold leading-tight text-slate-900">{{ article.title }}</h1>
            </div>
            <button
              class="inline-flex items-center gap-2 rounded-full border border-slate-200 px-5 py-2 text-sm font-medium transition"
              :class="isCollected ? 'bg-blue-50 border-blue-200 text-blue-600' : 'hover:border-blue-400 hover:text-blue-600'"
              @click="toggleCollection">
              <span>{{ isCollected ? '已收藏' : '收藏' }}</span>
            </button>
          </div>
          <div v-if="article.tags.length" class="flex flex-wrap gap-2 text-xs text-blue-600">
            <button
              v-for="tag in article.tags"
              :key="tag"
              type="button"
              class="rounded-full bg-blue-50 px-3 py-1 hover:bg-blue-100 transition"
              @click="handleTagClick(tag)"
            >
              #{{ tag }}
            </button>
          </div>
        </header>
        <section class="rounded-2xl bg-slate-50 p-5 leading-relaxed text-slate-600">
          <h2 class="mb-2 text-sm font-semibold text-slate-500">AI 摘要</h2>
          <p class="text-base">{{ article.summary }}</p>
        </section>
        <section class="space-y-4 text-slate-700">
          <h2 class="text-lg font-semibold">正文内容</h2>
          <div v-if="article.content" class="article-content" v-html="article.content"></div>
          <p v-else class="text-slate-400">暂无正文内容。</p>
        </section>
        <footer class="flex flex-wrap items-center gap-4 border-t border-slate-100 pt-4 text-sm text-slate-500">
          <a v-if="article.link" :href="article.link" target="_blank" rel="noopener"
            class="font-medium text-blue-500 transition hover:text-blue-600">
            在原文中打开
          </a>
          <a v-if="article.enclosure" :href="article.enclosure" target="_blank" rel="noopener"
            class="font-medium text-blue-500 transition hover:text-blue-600">
            查看附件
          </a>
          <span>最后更新：{{ article.timeAgo }}</span>
        </footer>
      </div>

      <div v-else class="w-full rounded-3xl border border-slate-200 bg-white p-12 text-center text-slate-400 shadow-sm">
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
  color: #1f2937;
}

.article-content :deep(p) {
  margin-bottom: 1.25rem;
}

.article-content :deep(p:last-child) {
  margin-bottom: 0;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  font-weight: 600;
  color: #0f172a;
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
  color: #2563eb;
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
  border-left: 3px solid #bfdbfe;
  background: #eff6ff;
  padding: 1rem 1.25rem;
  margin: 1.5rem 0;
  color: #1e3a8a;
}

.article-content :deep(pre) {
  background: #0f172a;
  color: #f8fafc;
  padding: 1rem 1.25rem;
  margin: 1.75rem 0;
  border-radius: 0.75rem;
  overflow-x: auto;
}

.article-content :deep(code) {
  background: #e2e8f0;
  padding: 0.15rem 0.4rem;
  border-radius: 0.4rem;
  font-size: 0.95rem;
}

.article-content :deep(pre code) {
  background: transparent;
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
  border: 1px solid #e2e8f0;
  padding: 0.75rem 0.9rem;
  text-align: left;
}

.article-content :deep(thead th) {
  background: #f1f5f9;
}
</style>
