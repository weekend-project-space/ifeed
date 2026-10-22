<template>
  <AppDialog :title="title ?? (includeAll ? '收藏文件夹' : '移动到文件夹')" :busy="busy" @close="$emit('close')">
    <p v-if="error || mutationError" role="alert" class="mb-3 text-xs text-red-600 dark:text-red-400">{{ error || mutationError }}</p>

    <!-- 删除确认态 -->
    <div v-if="deleting" class="space-y-4">
      <p class="text-xs leading-relaxed text-text-secondary sm:text-sm">
        删除“<strong class="font-semibold text-text">{{ deleting.name }}</strong>”？其中的收藏会移回默认收藏夹，收藏时间保持不变。
      </p>
      <div class="flex justify-end gap-2.5 pt-2">
        <button :disabled="busy" class="inline-flex h-9 items-center justify-center rounded-full px-4 text-xs font-semibold text-text-secondary hover:bg-surface-container transition" @click="deleting = null">取消</button>
        <button :disabled="busy" class="inline-flex h-9 items-center justify-center rounded-full bg-red-600 px-4 text-xs font-semibold text-white shadow-xs hover:bg-red-700 transition disabled:opacity-40" @click="removeFolder">{{ busy ? '删除中…' : '确认删除' }}</button>
      </div>
    </div>

    <!-- 正常编辑/列表态 -->
    <template v-else>
      <!-- MD3 输入行 -->
      <form class="mb-4 flex flex-nowrap items-center gap-2" @submit.prevent="saveFolder">
        <label for="folder-name" class="sr-only">{{ editing ? '文件夹新名称' : '新文件夹名称' }}</label>
        <input id="folder-name" v-model="name" maxlength="50" required :disabled="busy" :placeholder="editing ? '文件夹新名称' : '新文件夹名称'"
               class="min-w-0 flex-1 rounded-full border border-outline/20 bg-surface-container/40 px-4 py-2 text-xs text-text outline-none transition placeholder:text-text-secondary/60 focus:border-primary focus:bg-surface sm:text-sm" />
        <button :disabled="busy || !name.trim()" class="inline-flex h-9 shrink-0 items-center justify-center rounded-full bg-primary px-4 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 disabled:opacity-40">
          {{ editing ? '保存' : '创建' }}
        </button>
        <button v-if="editing" type="button" :disabled="busy" class="px-2 text-xs text-text-secondary hover:text-text" @click="resetEditor">取消编辑</button>
      </form>

      <!-- 选项列表 -->
      <div class="max-h-[50vh] space-y-1.5 overflow-y-auto pr-1">
        <button v-if="includeAll" :disabled="busy" :aria-pressed="selected === 'all'" class="folder-option" @click="select('all', '全部收藏')">
          <span>全部收藏</span>
          <span v-if="selected === 'all'" class="text-primary font-bold">✓</span>
        </button>
        <button :disabled="busy" :aria-pressed="selected === 'default'" class="folder-option" @click="select('default', '默认收藏夹')">
          <span>默认收藏夹</span>
          <span v-if="selected === 'default'" class="text-primary font-bold">✓</span>
        </button>

        <div v-for="folder in folders" :key="folder.folderId" class="flex items-center gap-1 rounded-2xl hover:bg-surface-container/60 transition">
          <button :disabled="busy" :aria-pressed="selected === folder.folderId" class="folder-option min-w-0 flex-1" @click="select(folder.folderId, folder.name)">
            <span class="truncate">{{ folder.name }}</span>
            <span v-if="selected === folder.folderId" class="text-primary font-bold">✓</span>
          </button>
          <button :disabled="busy" :aria-label="`重命名${folder.name}`" class="p-2 text-xs font-medium text-text-secondary hover:text-text" @click="editFolder(folder)">编辑</button>
          <button :disabled="busy" :aria-label="`删除${folder.name}`" class="p-2 text-xs font-medium text-red-600 hover:text-red-700 dark:text-red-400" @click="deleting = folder; mutation.reset()">删除</button>
        </div>

        <p v-if="foldersQuery.isPending.value" role="status" class="p-4 text-center text-xs text-text-secondary">正在加载文件夹…</p>
        <p v-else-if="!folders.length && !foldersQuery.isError.value" class="p-4 text-center text-xs text-text-secondary">还没有自定义文件夹</p>
        <div v-if="foldersQuery.isError.value" role="alert" class="p-3 text-center text-xs text-red-600">
          文件夹加载失败 <button class="underline ml-1 font-semibold" @click="foldersQuery.refetch()">重试</button>
        </div>
        <button v-if="foldersQuery.hasNextPage.value" ref="sentinel" :disabled="foldersQuery.isFetching.value || busy" class="w-full p-2.5 text-center text-xs text-text-secondary hover:text-text" @click="loadMore">
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
@reference "../styles/tailwind.css";

.folder-option {
  @apply flex min-h-11 w-full items-center justify-between gap-2 rounded-2xl px-4 text-left text-xs sm:text-sm font-medium transition hover:bg-surface-container/70 disabled:opacity-50;
}
.folder-option[aria-pressed="true"] {
  @apply bg-primary/10 text-primary font-semibold;
}
</style>