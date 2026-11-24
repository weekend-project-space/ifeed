import { computed, type ComputedRef } from 'vue';
import { useQuery } from '@tanstack/vue-query';
import { useAuthStore } from '@/stores/auth';
import { getArticle, getArticleRecommendations } from '@/api/articles';
import type { ArticleDetail } from '@/stores/articles/types';
import { normalizeArticleDetail } from '@/stores/articles/types';

export const ARTICLE_RECOMMENDATIONS_TOP_K = 6;

export const articleQueryKey = (userId: string, articleId: string) =>
  ['article', userId, articleId] as const;

export const useArticleQuery = (articleId: ComputedRef<string>) => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useQuery({
    queryKey: computed(() => articleQueryKey(userId.value, articleId.value)),
    queryFn: ({ signal }) => getArticle(articleId.value, { signal })
      .then(normalizeArticleDetail),
    enabled: computed(() => Boolean(articleId.value)),
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
};

export const useArticleRecommendationsQuery = (articleId: ComputedRef<string>) => {
  const authStore = useAuthStore();
  const userId = computed(() => authStore.user?.userId ?? 'anonymous');

  return useQuery({
    queryKey: computed(() => [
      'articleRecommendations', userId.value, articleId.value, ARTICLE_RECOMMENDATIONS_TOP_K,
    ] as const),
    queryFn: ({ signal }) => getArticleRecommendations(
      articleId.value,
      ARTICLE_RECOMMENDATIONS_TOP_K,
      { signal },
    ),
    select: (data) => (Array.isArray(data) ? data : []),
    enabled: computed(() => Boolean(articleId.value)),
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
};

export type { ArticleDetail };
