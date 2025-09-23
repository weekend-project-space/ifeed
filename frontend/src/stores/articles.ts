import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import MarkdownIt from 'markdown-it';
import { request } from '../api/client';
import { formatRelativeTime } from '../utils/datetime';
import type { PageResponse } from '../types/api';

export interface ArticleDto {
  id: string;
  title: string;
  summary?: string;
  content?: string;
  link?: string;
  thumbnail?: string;
  enclosure?: string;
  feedTitle?: string;
  publishedAt?: string;
  tags?: string[];
}

export interface ArticleListItem {
  id: string;
  title: string;
  summary: string;
  link?: string;
  thumbnail?: string;
  enclosure?: string;
  feedTitle: string;
  publishedAt?: string;
  timeAgo: string;
  tags: string[];
}

export interface ArticleDetail extends ArticleListItem {
  content: string;
}


const normalizeArticle = (article: ArticleDto): ArticleListItem => {
  const feedTitle = article.feedTitle ?? '未知来源';
  const publishedAt = article.publishedAt;
  const tags = article.tags ?? [];
  return {
    id: String(article.id),
    title: article.title ?? '未命名文章',
    summary: article.summary ?? '暂无摘要。',
    link: article.link,
    thumbnail: article.thumbnail,
    enclosure: article.enclosure,
    feedTitle,
    publishedAt,
    timeAgo: formatRelativeTime(publishedAt ?? Date.now()),
    tags: Array.from(new Set(tags)).slice(0, 6)
  };
};

export const useArticlesStore = defineStore('articles', () => {
  const markdown = new MarkdownIt({
    html: false,
    linkify: true,
    breaks: true
  });

  const items = ref<ArticleListItem[]>([]);
  const currentArticle = ref<ArticleDetail | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const page = ref(1);
  const size = ref(10);
  const total = ref<number | null>(null);
  const historyTracker = ref(new Set<string>());

  const totalPages = ref<number | null>(null);
  const feedIdFilter = ref<string | null>(null);
  const tagFilter = ref<string | null>(null);

  const hasNextPage = computed(() => {
    if (totalPages.value === null) {
      return false;
    }
    return page.value < totalPages.value;
  });

  const hasPreviousPage = computed(() => page.value > 1);

  const fetchArticles = async (override?: { page?: number; size?: number; sort?: string; feedId?: string | null; tags?: string | null }) => {
    loading.value = true;
    error.value = null;
    const nextPage = override?.page ?? page.value;
    const nextSize = override?.size ?? size.value;
    const hasFeedOverride = override !== undefined && Object.prototype.hasOwnProperty.call(override, 'feedId');
    const nextFeedId = hasFeedOverride ? override?.feedId ?? null : feedIdFilter.value;
    const hasTagOverride = override !== undefined && Object.prototype.hasOwnProperty.call(override, 'tags');
    const nextTag = hasTagOverride ? override?.tags ?? null : tagFilter.value;

    try {
      const response = await request<PageResponse<ArticleDto>>(
        '/api/articles',
        {
          query: {
            page: Math.max(0, nextPage - 1),
            size: nextSize,
            sort: override?.sort,
            feedId: nextFeedId ?? undefined,
            tags: nextTag ?? undefined
          }
        }
      );

      const list = Array.isArray(response?.content) ? response.content : [];
      items.value = list.map(normalizeArticle);
      page.value = (response?.number ?? 0) + 1;
      size.value = response?.size ?? nextSize;
      total.value = response?.totalElements ?? list.length;
      totalPages.value = response?.totalPages ?? (list.length ? 1 : 0);
      feedIdFilter.value = nextFeedId;
      tagFilter.value = nextTag;
    } catch (err) {
      const message = err instanceof Error ? err.message : '文章加载失败';
      error.value = message;
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const fetchArticleById = async (articleId: string) => {
    loading.value = true;
    error.value = null;
    try {
      const data = await request<ArticleDto & { content?: string }>(`/api/articles/${articleId}`);
      const normalized = normalizeArticle(data);
      const rawContent = data.content ?? data.summary ?? '';
      const hasHtmlTags = /<\/?[a-z][\s\S]*>/i.test(rawContent);
      currentArticle.value = {
        ...normalized,
        content: hasHtmlTags ? rawContent : markdown.render(rawContent)
      };
      return currentArticle.value;
    } catch (err) {
      const message = err instanceof Error ? err.message : '文章加载失败';
      error.value = message;
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const recordHistory = async (articleId: string) => {
    if (!articleId || historyTracker.value.has(articleId)) {
      return;
    }
    historyTracker.value.add(articleId);
    try {
      await request('/api/user/history', {
        method: 'POST',
        json: { articleId }
      });
    } catch (err) {
      // 记录失败不影响主流程，保留在集合中避免重复请求
      console.warn('记录阅读历史失败', err);
    }
  };

  const clearCurrentArticle = () => {
    currentArticle.value = null;
  };

  return {
    items,
    currentArticle,
    loading,
    error,
    page,
    size,
    total,
    totalPages,
    hasNextPage,
    hasPreviousPage,
    fetchArticles,
    fetchArticleById,
    clearCurrentArticle,
    recordHistory,
    feedId: feedIdFilter,
    tag: tagFilter
  };
});
