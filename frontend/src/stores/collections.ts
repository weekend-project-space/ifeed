import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { request } from '../api/client';
import type { PageResponse } from '../types/api';


interface CollectionDto {
  articleId: string;
  title?: string;
  collectedAt?: string;
}

export const useCollectionsStore = defineStore('collections', () => {
  const items = ref<CollectionDto[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const page = ref(1);
  const size = ref(10);
  const total = ref<number | null>(null);
  const totalPages = ref<number | null>(null);
  const ids = computed(() => new Set(items.value.map((item) => item.articleId)));

  const hasNextPage = computed(() => {
    if (totalPages.value === null) {
      return false;
    }
    return page.value < totalPages.value;
  });

  const hasPreviousPage = computed(() => page.value > 1);

  const recomputeTotalPages = () => {
    if (total.value === null) {
      totalPages.value = items.value.length ? 1 : 0;
      return;
    }
    if (total.value === 0) {
      totalPages.value = 0;
      return;
    }
    totalPages.value = Math.max(1, Math.ceil(total.value / size.value));
  };

  const isCollected = (articleId: string) => ids.value.has(articleId);

  const fetchCollections = async (override?: { page?: number; size?: number; sort?: string }) => {
    loading.value = true;
    error.value = null;
    const nextPage = override?.page ?? page.value;
    const nextSize = override?.size ?? size.value;
    const sort = override?.sort ?? 'collectedAt,desc';
    try {
      const response = await request<PageResponse<CollectionDto>>(
        '/api/user/collections',
        {
          query: {
            page: Math.max(0, nextPage - 1),
            size: nextSize,
            sort
          }
        }
      );
      const list = Array.isArray(response?.content) ? response.content : [];
      items.value = list;
      page.value = (response?.number ?? 0) + 1;
      size.value = response?.size ?? nextSize;
      total.value = response?.totalElements ?? list.length;
      totalPages.value = response?.totalPages ?? (list.length ? 1 : 0);
    } catch (err) {
      const message = err instanceof Error ? err.message : '收藏列表加载失败';
      error.value = message;
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const addCollection = async (articleId: string, meta?: { title?: string }) => {
    try {
      await request(`/api/user/collections/${articleId}`, { method: 'POST' });
      if (!isCollected(articleId)) {
        items.value = [
          ...items.value,
          { articleId, title: meta?.title, collectedAt: new Date().toISOString() }
        ];
        if (total.value === null) {
          total.value = items.value.length;
        } else {
          total.value += 1;
        }
        recomputeTotalPages();
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : '收藏失败';
      error.value = message;
      throw err;
    }
  };

  const removeCollection = async (articleId: string) => {
    try {
      await request(`/api/user/collections/${articleId}`, { method: 'DELETE' });
      items.value = items.value.filter((item) => item.articleId !== articleId);
      if (total.value !== null && total.value > 0) {
        total.value -= 1;
      }
      recomputeTotalPages();
    } catch (err) {
      const message = err instanceof Error ? err.message : '取消收藏失败';
      error.value = message;
      throw err;
    }
  };

  const toggleCollection = async (articleId: string, meta?: { title?: string }) => {
    if (isCollected(articleId)) {
      await removeCollection(articleId);
    } else {
      await addCollection(articleId, meta);
    }
  };

  return {
    items,
    loading,
    error,
    page,
    size,
    total,
    totalPages,
    hasNextPage,
    hasPreviousPage,
    isCollected,
    fetchCollections,
    addCollection,
    removeCollection,
    toggleCollection
  };
});
