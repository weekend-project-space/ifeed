import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { request } from '@/api/client';
import type { SlimPageResponse } from '@/types/api';

export interface RadarTopicDto {
  topicId: string;
  title?: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
  articleCount: number;
  topKeywords?: string[];
}

export interface RadarItemDto {
  articleId: string;
  title?: string;
  summary?: string;
  thumbnail?: string;
  enclosure?: string;
  feedTitle?: string;
  publishedAt?: string;
  tags?: string[];
  score?: number;
  timeAgo?: string;
}

export type PageResponseWithMeta<T> = SlimPageResponse<T> & {
  meta?: Record<string, unknown>;
};

export const useRadarStore = defineStore('radar', () => {
  // digest
  const topics = ref<RadarTopicDto[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const page = ref(1);
  const size = ref(12);
  const total = ref<number | null>(null);
  const totalPages = ref<number | null>(null);
  const meta = ref<Record<string, unknown>>({});

  const snapshotId = computed(() => String(meta.value.snapshotId ?? ''));
  const generatedAt = computed(() => String(meta.value.generatedAt ?? ''));
  const windowHours = computed(() => Number(meta.value.windowHours ?? 0));

  const hasNextPage = computed(() => {
    if (totalPages.value === null) {
      return false;
    }
    return page.value < totalPages.value;
  });

  const hasPreviousPage = computed(() => page.value > 1);

  const fetchDigest = async (override?: {
    page?: number;
    size?: number;
    snapshotId?: string | null;
    windowHours?: number | null;
  }) => {
    loading.value = true;
    error.value = null;

    const nextPage = override?.page ?? page.value;
    const nextSize = override?.size ?? size.value;

    try {
      const response = await request<PageResponseWithMeta<RadarTopicDto>>(
        '/api/user/radar/digest',
        {
          query: {
            page: Math.max(0, nextPage - 1),
            size: nextSize,
            snapshotId: override?.snapshotId ?? undefined,
            windowHours: override?.windowHours ?? undefined,
          }
        }
      );

      const list = Array.isArray(response?.content) ? response.content : [];
      topics.value = list;
      page.value = (response?.page ?? 0) + 1;
      size.value = response?.size ?? nextSize;
      total.value = response?.totalElements ?? list.length;
      totalPages.value = response?.totalPages ?? (list.length ? 1 : 0);
      meta.value = (response?.meta ?? {}) as Record<string, unknown>;

      return response;
    } catch (err) {
      const message = err instanceof Error ? err.message : '热点雷达加载失败';
      error.value = message;
      throw err;
    } finally {
      loading.value = false;
    }
  };

  // topic detail
  const topic = ref<RadarTopicDto | null>(null);
  const items = ref<RadarItemDto[]>([]);
  const topicLoading = ref(false);
  const topicError = ref<string | null>(null);
  const topicPage = ref(1);
  const topicSize = ref(20);
  const topicTotal = ref<number | null>(null);
  const topicTotalPages = ref<number | null>(null);
  const topicMeta = ref<Record<string, unknown>>({});

  const topicHasNextPage = computed(() => {
    if (topicTotalPages.value === null) {
      return false;
    }
    return topicPage.value < topicTotalPages.value;
  });

  const topicHasPreviousPage = computed(() => topicPage.value > 1);

  const fetchTopicDetail = async (args: {
    topicId: string;
    snapshotId: string;
    page?: number;
    size?: number;
  }) => {
    topicLoading.value = true;
    topicError.value = null;

    const nextPage = args.page ?? topicPage.value;
    const nextSize = args.size ?? topicSize.value;

    try {
      const response = await request<PageResponseWithMeta<RadarItemDto>>(
        `/api/user/radar/topics/${args.topicId}`,
        {
          query: {
            snapshotId: args.snapshotId,
            page: Math.max(0, nextPage - 1),
            size: nextSize
          }
        }
      );

      const list = Array.isArray(response?.content) ? response.content : [];
      items.value = list;
      topicPage.value = (response?.page ?? 0) + 1;
      topicSize.value = response?.size ?? nextSize;
      topicTotal.value = response?.totalElements ?? list.length;
      topicTotalPages.value = response?.totalPages ?? (list.length ? 1 : 0);
      topicMeta.value = (response?.meta ?? {}) as Record<string, unknown>;
      topic.value = (topicMeta.value.topic as RadarTopicDto | undefined) ?? null;

      return response;
    } catch (err) {
      const message = err instanceof Error ? err.message : '话题详情加载失败';
      topicError.value = message;
      throw err;
    } finally {
      topicLoading.value = false;
    }
  };

  return {
    topics,
    loading,
    error,
    page,
    size,
    total,
    totalPages,
    meta,
    snapshotId,
    generatedAt,
    windowHours,
    hasNextPage,
    hasPreviousPage,
    fetchDigest,
    topic,
    items,
    topicLoading,
    topicError,
    topicPage,
    topicSize,
    topicTotal,
    topicTotalPages,
    topicMeta,
    topicHasNextPage,
    topicHasPreviousPage,
    fetchTopicDetail
  };
});
