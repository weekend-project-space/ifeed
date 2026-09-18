import {ref} from 'vue';
import {defineStore} from 'pinia';
import {enrichArticle, recordArticleHistory} from '@/api/articles';

export const useArticlesStore = defineStore('articles', () => {
    const historyTracker = ref(new Set<string>());
    const historyInFlight = new Set<string>();

    const enrich = async (articleId: string): Promise<{ summary: string; mindMap: string }> => {
        return enrichArticle(articleId);
    };

    const recordHistory = async (articleId: string) => {
        if (!articleId || historyTracker.value.has(articleId) || historyInFlight.has(articleId)) {
            return;
        }
        historyInFlight.add(articleId);
        try {
            await recordArticleHistory(articleId);
            historyTracker.value.add(articleId);
        } catch (err) {
            console.warn('记录阅读历史失败', err);
            throw err;
        } finally {
            historyInFlight.delete(articleId);
        }
    };

    return {
        recordHistory,
        enrich,
    };
});
