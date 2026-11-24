import { HISTORY_SORT, listHistory } from '@/api/history';
import { useInfiniteLibraryQuery } from './library';

export const useHistoryQuery = () => useInfiniteLibraryQuery('history', HISTORY_SORT, listHistory);
