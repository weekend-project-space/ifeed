import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { request } from '../api/client';
import type { PageResponse } from '../types/api';

export type SearchType = 'keyword' | 'semantic';

export interface SearchResultDto {
  id: string;
  title?: string;
  summary?: string;
  score?: number;
}

export const useSearchStore = defineStore('search', () => {
  const results = ref<SearchResultDto[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const page = ref(1);
  const size = ref(10);
  const total = ref<number | null>(null);
  const totalPages = ref<number | null>(null);
  const currentQuery = ref('');
  const currentType = ref<SearchType>('keyword');

  const hasNextPage = computed(() => {
    if (totalPages.value === null) {
      return false;
    }
    return page.value < totalPages.value;
  });

  const hasPreviousPage = computed(() => page.value > 1);

  const searchArticles = async (params: { query: string; page?: number; size?: number; type?: SearchType }) => {
    const query = params.query.trim();
    if (!query) {
      clear();
      return;
    }

    loading.value = true;
    error.value = null;
    const nextPage = params.page ?? 1;
    const nextSize = params.size ?? size.value;
    const nextType = params.type ?? currentType.value;

    try {
      const response = await request<PageResponse<SearchResultDto>>('/api/search', {
        query: {
          query,
          type: nextType,
          page: Math.max(0, nextPage - 1),
          size: nextSize
        }
      });

      results.value = Array.isArray(response?.content) ? response.content : [];
      page.value = (response?.number ?? 0) + 1;
      size.value = response?.size ?? nextSize;
      total.value = response?.totalElements ?? results.value.length;
      totalPages.value = response?.totalPages ?? (results.value.length ? 1 : 0);
      currentQuery.value = query;
      currentType.value = nextType;
    } catch (err) {
      const message = err instanceof Error ? err.message : '搜索失败';
      error.value = message;
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const clear = () => {
    results.value = [];
    total.value = null;
    totalPages.value = null;
    page.value = 1;
    currentQuery.value = '';
    error.value = null;
  };

  return {
    results,
    loading,
    error,
    page,
    size,
    total,
    totalPages,
    currentQuery,
    currentType,
    hasNextPage,
    hasPreviousPage,
    searchArticles,
    clear
  };
});
