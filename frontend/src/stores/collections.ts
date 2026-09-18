import {defineStore} from 'pinia';
import {
    addCollection as addCollectionRequest,
    removeCollection as removeCollectionRequest,
} from '@/api/collections';

export const useCollectionsStore = defineStore('collections', () => {
    const addCollection = (articleId: string) =>
        addCollectionRequest(articleId);

    const removeCollection = (articleId: string) =>
        removeCollectionRequest(articleId);

    const toggleCollection = (articleId: string, meta?: { collected?: boolean }) =>
        meta?.collected ? removeCollection(articleId) : addCollection(articleId);

    return {
        addCollection,
        removeCollection,
        toggleCollection
    };
});
