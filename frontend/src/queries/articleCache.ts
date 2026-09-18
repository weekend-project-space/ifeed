import type { InfiniteData, QueryClient } from '@tanstack/vue-query';
import type { ArticleDto } from '@/api/articles';
import type { PageResponse } from '@/types/api';

export type InfiniteArticleData = InfiniteData<PageResponse<ArticleDto>>;

const updateArticleInPages = (
  data: InfiniteArticleData,
  articleId: string,
  collected: boolean,
): InfiniteArticleData => ({
  ...data,
  pages: data.pages.map((page) => ({
    ...page,
    content: page.content.map((article) => (
      article.id === articleId ? {...article, collected} : article
    )),
  })),
});

/** Update collection state without refetching every loaded infinite-query page. */
export const updateArticleCollectedCaches = (
  queryClient: QueryClient,
  userId: string,
  articleId: string,
  collected: boolean,
) => {
  for (const queryKey of [
    ['recommendations', userId],
    ['feedArticles', userId],
    ['subscriptionArticles', userId],
  ]) {
    queryClient.setQueriesData<InfiniteArticleData>({queryKey}, (previous) => (
      previous ? updateArticleInPages(previous, articleId, collected) : previous
    ));
  }
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
