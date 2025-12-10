import {computed, ref} from 'vue';
import {defineStore} from 'pinia';
import {request} from '@/api/client';
import type {PageResponse} from '@/types/api';
import {ArticleDto, ArticleListItem, normalizeArticle} from "./types";

export const useFeedArticlesStore = defineStore('feedArticles', () => {
    const items = ref<ArticleListItem[]>([]);
    const loading = ref(false);
    const error = ref<string | null>(null);
    const page = ref(1);
    const size = ref(20);
    const total = ref<number | null>(null);
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

    const fetchArticles = async (override?: {
        page?: number;
        size?: number;
        sort?: string;
        feedId?: string | null;
        tags?: string | null;
    }) => {
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
                        sort: override?.sort ?? 'publishedAt,desc',
                        feedId: nextFeedId ?? undefined,
                        tags: nextTag ?? undefined,
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
        feedId: feedIdFilter,
        tag: tagFilter,
    };
});