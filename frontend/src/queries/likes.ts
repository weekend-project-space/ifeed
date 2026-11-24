import { listLikes } from '@/api/likes';
import { useInfiniteLibraryQuery } from './library';

export const useLikesQuery = () => useInfiniteLibraryQuery('likes', 'likedAt,desc', listLikes);
