import { computed } from 'vue';
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/vue-query';
import { useAuthStore } from '@/stores/auth';
import { createCollectionFolder, deleteCollectionFolder, listCollectionFolders, renameCollectionFolder } from '@/api/collectionFolders';

export const useCollectionFoldersQuery = () => {
  const auth = useAuthStore();
  return useInfiniteQuery({
    queryKey: computed(() => ['collectionFolders', auth.user?.userId ?? 'anonymous']),
    queryFn: ({ pageParam, signal }) => listCollectionFolders(pageParam, 20, signal),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => lastPage.number + 1 < lastPage.totalPages ? lastPage.number + 1 : undefined,
    enabled: computed(() => auth.isAuthenticated),
    staleTime: 5 * 60 * 1000,
  });
};

type FolderChange = { action: 'create'; name: string }
  | { action: 'rename'; folderId: string; name: string }
  | { action: 'delete'; folderId: string };

export const useCollectionFolderMutation = () => {
  const auth = useAuthStore();
  const client = useQueryClient();
  return useMutation({
    onMutate: () => ({ userId: auth.user?.userId ?? 'anonymous' }),
    mutationFn: async (change: FolderChange) => {
      if (change.action === 'create') return createCollectionFolder(change.name);
      if (change.action === 'rename') return renameCollectionFolder(change.folderId, change.name);
      await deleteCollectionFolder(change.folderId);
      return null;
    },
    onSuccess: async (_, change, context) => {
      if (auth.user?.userId !== context.userId) return;
      const keys = [['collectionFolders', context.userId]];
      if (change.action === 'delete') keys.push(['collections', context.userId], ['article', context.userId]);
      await Promise.all(keys.map(queryKey => client.invalidateQueries({ queryKey })));
    },
  });
};
