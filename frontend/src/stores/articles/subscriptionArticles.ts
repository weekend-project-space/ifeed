import {computed, ref} from 'vue';
import {defineStore} from 'pinia';
import {request} from '@/api/client';
import type {PageResponse} from '@/types/api';
import {ArticleDto, ArticleListItem, normalizeArticle} from "./types";

export const useSubscriptionArticlesStore = defineStore('subscriptionArticles', () => {
    const items = ref<ArticleListItem[]>([]);
    const loading = ref(false);
    const error = ref<string | null>(null);
    const page = ref(1);
    const size = ref(20);
    const total = ref<number | null>(null);
    const totalPages = ref<number | null>(null);
    const tagFilter = ref<string | null>(null);
    const categoryFilter = ref<string | null>(null);

    // insights state
    const insightsLoading = ref(false);
    const insightsError = ref<string | null>(null);
    const insights = ref<{
        categories: { category: string; count: number }[];
        hotTags: { tag: string; count: number }[]
    }>({
        categories: [],
        hotTags: []
    });

    const hasNextPage = computed(() => {
        if (totalPages.value === null) {
            return false;
        }
        return page.value < totalPages.value;
    });

    const hasPreviousPage = computed(() => page.value > 1);

    const fetchArticles = async (override?: {
        page?: number;
        size?: number;
        sort?: string;
        tags?: string | null;
        category?: string | null
    }) => {
        loading.value = true;
        error.value = null;
        const nextPage = override?.page ?? page.value;
        const nextSize = override?.size ?? size.value;
        const hasTagOverride = override !== undefined && Object.prototype.hasOwnProperty.call(override, 'tags');
        const nextTag = hasTagOverride ? override?.tags ?? null : tagFilter.value;
        const hasCategoryOverride = override !== undefined && Object.prototype.hasOwnProperty.call(override, 'category');
        const nextCategory = hasCategoryOverride ? override?.category ?? null : categoryFilter.value;

        try {
            const response = await request<PageResponse<ArticleDto>>(
                '/api/articles',
                {
                    query: {
                        page: Math.max(0, nextPage - 1),
                        size: nextSize,
                        sort: override?.sort ?? 'publishedAt,desc',
                        tags: nextTag ?? undefined,
                        category: nextCategory ?? undefined
                    }
                }
            );

            const list = Array.isArray(response?.content) ? response.content : [];
            items.value = list.map(normalizeArticle);
            page.value = (response?.number ?? 0) + 1;
            size.value = response?.size ?? nextSize;
            total.value = response?.totalElements ?? list.length;
            totalPages.value = response?.totalPages ?? (list.length ? 1 : 0);
            tagFilter.value = nextTag;
            categoryFilter.value = nextCategory;
        } catch (err) {
            const message = err instanceof Error ? err.message : '文章加载失败';
            error.value = message;
            throw err;
        } finally {
            loading.value = false;
        }
    };

    const fetchInsights = async (params?: { top?: number; from?: string; to?: string }) => {
        insightsLoading.value = true;
        insightsError.value = null;
        try {
            const data = await request<{
                categories: { category: string; count: number }[];
                hotTags: { tag: string; count: number }[]
            }>(
                '/api/articles/insights',
                {query: {top: params?.top ?? 12, from: params?.from, to: params?.to}}
            );
            insights.value = {
                categories: Array.isArray(data?.categories) ? data.categories : [],
                hotTags: Array.isArray(data?.hotTags) ? data.hotTags : []
            };
            return insights.value;
        } catch (err) {
            const message = err instanceof Error ? err.message : '加载概览失败';
            insightsError.value = message;
            insights.value = {categories: [], hotTags: []};
            throw err;
        } finally {
            insightsLoading.value = false;
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
        fetchArticles,
        tag: tagFilter,
        category: categoryFilter,
        insights,
        insightsLoading,
        insightsError,
        fetchInsights
    };
});