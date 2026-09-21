<template>
  <AppDialog :title="title ?? (includeAll ? '收藏文件夹' : '移动到文件夹')" :busy="busy" @close="$emit('close')">
    <p v-if="error || mutationError" role="alert" class="mb-3 text-sm text-red-600">{{ error || mutationError }}</p>
    <div v-if="deleting" class="space-y-4">
      <p class="text-sm">删除“{{ deleting.name }}”？其中的收藏会移回默认收藏夹，收藏时间保持不变。</p>
      <div class="flex justify-end gap-3">
        <button :disabled="busy" class="px-3 py-2" @click="deleting = null">取消</button>
        <button :disabled="busy" class="rounded-lg bg-red-600 px-3 py-2 text-white disabled:opacity-50" @click="removeFolder">{{ busy ? '删除中…' : '确认删除' }}</button>
      </div>
    </div>
    <template v-else>
      <form class="mb-4 flex flex-wrap gap-2" @submit.prevent="saveFolder">
        <label for="folder-name" class="sr-only">{{ editing ? '文件夹新名称' : '新文件夹名称' }}</label>
        <input id="folder-name" v-model="name" maxlength="50" required :disabled="busy" :placeholder="editing ? '文件夹新名称' : '新文件夹名称'"
          class="min-w-0 flex-1 rounded-lg border border-gray-300 bg-transparent px-3 py-2 text-sm dark:border-gray-700" />
        <button :disabled="busy || !name.trim()" class="rounded-lg bg-secondary px-3 py-2 text-sm text-white disabled:opacity-50">{{ editing ? '保存' : '创建' }}</button>
        <button v-if="editing" type="button" :disabled="busy" class="px-2 text-sm" @click="resetEditor">取消编辑</button>
      </form>
      <div class="max-h-[50vh] space-y-1 overflow-y-auto">
        <button v-if="includeAll" :disabled="busy" :aria-pressed="selected === 'all'" class="folder-option" @click="select('all', '全部收藏')">
          全部收藏 <span v-if="selected === 'all'">✓</span>
        </button>
        <button :disabled="busy" :aria-pressed="selected === 'default'" class="folder-option" @click="select('default', '默认收藏夹')">
          默认收藏夹 <span v-if="selected === 'default'">✓</span>
        </button>
        <div v-for="folder in folders" :key="folder.folderId" class="flex items-center gap-1 rounded-lg hover:bg-surface-container">
          <button :disabled="busy" :aria-pressed="selected === folder.folderId" class="folder-option min-w-0 flex-1" @click="select(folder.folderId, folder.name)">
            <span class="truncate">{{ folder.name }}</span><span v-if="selected === folder.folderId">✓</span>
          </button>
          <button :disabled="busy" :aria-label="`重命名${folder.name}`" class="p-2 text-xs text-text-secondary" @click="editFolder(folder)">编辑</button>
          <button :disabled="busy" :aria-label="`删除${folder.name}`" class="p-2 text-xs text-red-600" @click="deleting = folder; mutation.reset()">删除</button>
        </div>
        <p v-if="foldersQuery.isPending.value" role="status" class="p-3 text-sm text-text-secondary">正在加载文件夹…</p>
        <p v-else-if="!folders.length && !foldersQuery.isError.value" class="p-3 text-sm text-text-secondary">还没有自定义文件夹</p>
        <div v-if="foldersQuery.isError.value" role="alert" class="p-3 text-sm text-red-600">
          文件夹加载失败 <button class="underline" @click="foldersQuery.refetch()">重试</button>
        </div>
        <button v-if="foldersQuery.hasNextPage.value" ref="sentinel" :disabled="foldersQuery.isFetching.value || busy" class="w-full p-3 text-sm text-text-secondary" @click="loadMore">
          {{ foldersQuery.isFetchingNextPage.value ? '加载中…' : '加载更多文件夹' }}
        </button>
      </div>
      <slot name="footer" :busy="busy" />
    </template>
  </AppDialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import AppDialog from './AppDialog.vue';
import type { CollectionFolderDto } from '@/api/collectionFolders';
import { useCollectionFolderMutation, useCollectionFoldersQuery } from '@/queries/collectionFolders';
import { useInfiniteScroll } from '@/composables/infiniteScroll';

const props = defineProps<{ selected: string; title?: string; includeAll?: boolean; submitting?: boolean; error?: string }>();
const emit = defineEmits<{ close: []; select: [folderId: string, name: string]; deleted: [folderId: string]; renamed: [folder: CollectionFolderDto] }>();
const foldersQuery = useCollectionFoldersQuery();
const folders = computed(() => foldersQuery.data.value?.pages.flatMap(page => page.content) ?? []);
const mutation = useCollectionFolderMutation();
const busy = computed(() => Boolean(props.submitting || mutation.isPending.value));
const mutationError = computed(() => mutation.error.value instanceof Error ? mutation.error.value.message : '');
const name = ref('');
const editing = ref<CollectionFolderDto | null>(null);
const deleting = ref<CollectionFolderDto | null>(null);
const { sentinel, loadMore } = useInfiniteScroll({
  hasNextPage: foldersQuery.hasNextPage, isLoading: foldersQuery.isFetching,
  hasError: foldersQuery.isError, enabled: () => !busy.value && !deleting.value,
  onLoadMore: () => foldersQuery.fetchNextPage(),
});
const resetEditor = () => { editing.value = null; name.value = ''; };
const editFolder = (folder: CollectionFolderDto) => { editing.value = folder; name.value = folder.name; mutation.reset(); };
const select = (folderId: string, folderName: string) => { if (!busy.value) emit('select', folderId, folderName); };
const saveFolder = async () => {
  if (busy.value || !name.value.trim()) return;
  try {
    const result = await mutation.mutateAsync(editing.value
      ? { action: 'rename', folderId: editing.value.folderId, name: name.value.trim() }
      : { action: 'create', name: name.value.trim() });
    if (editing.value && result) emit('renamed', result);
    resetEditor();
  } catch { return; }
};
const removeFolder = async () => {
  if (!deleting.value || busy.value) return;
  const folderId = deleting.value.folderId;
  try {
    await mutation.mutateAsync({ action: 'delete', folderId });
    deleting.value = null;
    if (editing.value?.folderId === folderId) resetEditor();
    emit('deleted', folderId);
  } catch { return; }
};
</script>

<style scoped>
.folder-option {
  @apply flex w-full items-center justify-between gap-2 rounded-lg px-3 py-3 text-left text-sm hover:bg-surface-container disabled:opacity-50;
}
.folder-option[aria-pressed="true"] {
  @apply bg-surface-container font-semibold;
}
</style>
