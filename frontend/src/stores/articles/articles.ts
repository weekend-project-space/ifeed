import {ref} from 'vue';
import {defineStore} from 'pinia';
import {request, RequestOptions} from '@/api/client';
import {ArticleDto, ArticleDetail, normalizeArticleDetail} from "./types";

export const useArticlesStore = defineStore('articles', () => {
    const currentArticle = ref<ArticleDetail | null>(null);
    const loading = ref(false);
    const error = ref<string | null>(null);
    const historyTracker = ref(new Set<string>());

    const fetchArticleById = async (articleId: string, options?: RequestOptions) => {
        loading.value = true;
        error.value = null;
        try {
            const data = await request<ArticleDto>(`/api/articles/${articleId}`, options);
            currentArticle.value = normalizeArticleDetail(data);
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
                json: {articleId}
            });
        } catch (err) {
            console.warn('记录阅读历史失败', err);
        }
    };

    const clearCurrentArticle = () => {
        currentArticle.value = null;
    };

    return {
        currentArticle,
        loading,
        error,
        fetchArticleById,
        clearCurrentArticle,
        recordHistory,
    };
});