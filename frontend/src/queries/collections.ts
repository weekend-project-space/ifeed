import type { ComputedRef } from 'vue';
import { COLLECTIONS_SORT, listCollections } from '@/api/collections';
import { libraryQueryKey, useInfiniteLibraryQuery } from './library';

export const collectionsQueryKey = (userId: string, folderId?: string) =>
  libraryQueryKey('collections', userId, COLLECTIONS_SORT, folderId);

export const useCollectionsQuery = (folderId: ComputedRef<string | undefined>) =>
  useInfiniteLibraryQuery('collections', COLLECTIONS_SORT, listCollections, folderId);
