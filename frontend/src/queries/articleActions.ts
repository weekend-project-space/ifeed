import { useMutation, useQueryClient } from '@tanstack/vue-query';
import { useAuthStore } from '@/stores/auth';
import { addCollection, removeCollection } from '@/api/collections';
import { addLike, removeLike } from '@/api/likes';
import { recordArticleHistory } from '@/api/articles';
import { removeHistory } from '@/api/history';
import { articleQueryKey } from './article';
import { updateArticleCollectedCaches } from './articleCache';
import { removeLibraryArticle } from './library';
import type { ArticleDetail } from '@/stores/articles/types';

export const useCollectionMutation = () => {
  const auth = useAuthStore();
  const client = useQueryClient();
  return useMutation({
    onMutate: () => ({ userId: auth.user?.userId ?? 'anonymous' }),
    mutationFn: async (payload: { articleId: string; collected: boolean; folderId?: string | null }) => {
      if (payload.collected) return addCollection(payload.articleId, payload.folderId);
      await removeCollection(payload.articleId);
      return null;
    },
    onSuccess: async (result, payload, context) => {
      if (auth.user?.userId !== context.userId) return;
      const key = articleQueryKey(context.userId, payload.articleId);
      await client.cancelQueries({ queryKey: key });
      client.setQueryData<ArticleDetail>(key, previous => previous ? {
        ...previous, collected: payload.collected, folderId: result?.folderId ?? null,
      } : previous);
      updateArticleCollectedCaches(client, context.userId, payload.articleId, payload.collected);
      if (!payload.collected) await removeLibraryArticle(client, ['collections', context.userId], payload.articleId);
      await Promise.all([
        client.invalidateQueries({ queryKey: ['collections', context.userId] }),
        client.invalidateQueries({ queryKey: ['search', context.userId] }),
      ]);
    },
  });
};

export const useLikeMutation = () => {
  const auth = useAuthStore();
  const client = useQueryClient();
  return useMutation({
    onMutate: () => ({ userId: auth.user?.userId ?? 'anonymous' }),
    mutationFn: async (payload: { articleId: string; liked: boolean }) => {
      if (payload.liked) await addLike(payload.articleId);
      else await removeLike(payload.articleId);
    },
    onSuccess: async (_, payload, context) => {
      if (auth.user?.userId !== context.userId) return;
      const key = articleQueryKey(context.userId, payload.articleId);
      await client.cancelQueries({ queryKey: key });
      client.setQueryData<ArticleDetail>(key, previous => previous ? { ...previous, liked: payload.liked } : previous);
      if (!payload.liked) await removeLibraryArticle(client, ['likes', context.userId], payload.articleId);
      await client.invalidateQueries({ queryKey: ['likes', context.userId] });
    },
  });
};

export const useHistoryMutation = (action: 'record' | 'remove') => {
  const auth = useAuthStore();
  const client = useQueryClient();
  return useMutation({
    onMutate: () => ({ userId: auth.user?.userId ?? 'anonymous' }),
    mutationFn: (articleId: string) => action === 'record' ? recordArticleHistory(articleId) : removeHistory(articleId),
    onSuccess: async (_result, articleId, context) => {
      if (auth.user?.userId !== context.userId) return;
      if (action === 'remove') await removeLibraryArticle(client, ['history', context.userId], articleId);
      return client.invalidateQueries({ queryKey: ['history', context.userId] });
    },
  });
};
