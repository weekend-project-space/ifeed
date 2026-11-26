import type { InfiniteData, QueryClient } from '@tanstack/vue-query';
import type { ArticleDto } from '@/api/articles';
import type { PageResponse } from '@/types/api';

export type InfiniteArticleData = InfiniteData<PageResponse<ArticleDto>>;
type ArticleStatePatch = Partial<Pick<ArticleDto, 'collected' | 'liked' | 'folderId'>>;
const ARTICLE_QUERIES = new Set([
  'article', 'recommendations', 'feedArticles', 'subscriptionArticles',
  'search', 'articleRecommendations', 'collections', 'likes', 'history',
]);

const containsArticle = (data: unknown, articleId: string): boolean => {
  if (Array.isArray(data)) return data.some(item => containsArticle(item, articleId));
  if (!data || typeof data !== 'object') return false;
  const value = data as Record<string, unknown>;
  if (value.id === articleId || value.articleId === articleId) return true;
  return containsArticle(value.pages, articleId) || containsArticle(value.content, articleId);
};

const patchArticle = (data: unknown, articleId: string, patch: ArticleStatePatch): unknown => {
  if (Array.isArray(data)) return data.map(item => patchArticle(item, articleId, patch));
  if (!data || typeof data !== 'object') return data;
  const value = data as Record<string, unknown>;
  if (value.id === articleId || value.articleId === articleId) return {...value, ...patch};
  if (Array.isArray(value.pages)) return {...value, pages: patchArticle(value.pages, articleId, patch)};
  if (Array.isArray(value.content)) return {...value, content: patchArticle(value.content, articleId, patch)};
  return data;
};

/** Patch the account's existing list/detail caches without refetching loaded pages. */
export const updateArticleStateCaches = async (
  client: QueryClient,
  userId: string,
  articleId: string,
  patch: ArticleStatePatch,
) => {
  const filters = {
    predicate: (query: {queryKey: readonly unknown[]; state: {data: unknown}}) =>
      query.queryKey[1] === userId && ARTICLE_QUERIES.has(String(query.queryKey[0]))
      && containsArticle(query.state.data, articleId),
  };
  // Keep an in-flight page/detail request from overwriting the confirmed action.
  await client.cancelQueries(filters);
  client.setQueriesData(filters, previous => patchArticle(previous, articleId, patch));
};

/** Keep the first page and drop later pages before invalidating a changed list. */
export const resetInfiniteArticleCaches = (
  queryClient: QueryClient,
  queryKey: readonly unknown[],
) => {
  queryClient.setQueriesData<InfiniteArticleData>({queryKey}, (previous) => {
    if (!previous || previous.pages.length <= 1) return previous;
    return {
      ...previous,
      pages: previous.pages.slice(0, 1),
      pageParams: previous.pageParams.slice(0, 1),
    };
  });
};
