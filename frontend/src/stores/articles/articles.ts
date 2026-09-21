import {defineStore} from 'pinia';
import {enrichArticle} from '@/api/articles';

export const useArticlesStore = defineStore('articles', () => {

    const enrich = async (articleId: string): Promise<{ summary: string; mindMap: string }> => {
        return enrichArticle(articleId);
    };

    return {
        enrich,
    };
});
