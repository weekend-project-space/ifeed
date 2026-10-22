<template>
  <LibraryPageLayout
      title="我的收藏"
      :total="total"
      :pending="collectionsQuery.isPending.value"
      :fetching="collectionsQuery.isFetching.value"
      :error="errorMessage"
      :empty-text="folderId ? '这个文件夹还没有收藏' : '这里还没有收藏，阅读文章时可以点击收藏按钮。'"
      @refresh="refresh"
  >
    <template #controls>
      <!-- MD3 Filter Chips 文件夹筛选 -->
      <div class="mb-5 flex flex-wrap items-center gap-1.5" role="group" aria-label="收藏文件夹">
        <button
            v-for="filter in filters"
            :key="filter.id"
            type="button"
            :aria-pressed="selectedFolder === filter.id"
            :title="filter.name"
            class="max-w-full truncate rounded-full px-3.5 py-1.5 text-xs font-medium transition select-none active:scale-95"
            :class="selectedFolder === filter.id
              ? 'bg-primary/15 font-semibold text-primary shadow-xs'
              : 'bg-surface-container/60 text-text-secondary hover:bg-surface-container hover:text-text'"
            @click="selectFolder(filter.id)"
        >
          {{ filter.name }}
        </button>

        <button
            v-if="foldersQuery.hasNextPage.value && !foldersQuery.isError.value"
            type="button"
            :disabled="foldersQuery.isFetching.value"
            class="inline-flex h-7 items-center rounded-full px-3 text-xs font-medium text-text-secondary transition hover:bg-surface-container hover:text-text disabled:opacity-40"
            @click="foldersQuery.fetchNextPage()"
        >
          {{ foldersQuery.isFetchingNextPage.value ? '加载中…' : '更多文件夹' }}
        </button>
      </div>

      <p v-if="foldersQuery.isPending.value" role="status" class="mb-4 text-xs text-text-secondary">正在加载文件夹…</p>

      <div v-if="foldersQuery.isError.value" role="alert" class="mb-4 rounded-2xl bg-red-500/10 p-3 text-xs text-red-600 dark:text-red-400">
        文件夹加载失败
        <button
            :disabled="foldersQuery.isFetching.value"
            class="ml-1 font-semibold underline disabled:opacity-40"
            @click="foldersQuery.isFetchNextPageError.value ? foldersQuery.fetchNextPage() : foldersQuery.refetch()"
        >
          重试加载
        </button>
      </div>
    </template>

    <ArticleCardList v-if="items.length" :items="items" :loading="false" show-action meta-field="collectedAt" meta-prefix="收藏于 ">
      <template #action-dropdown="{ item, close }">
        <router-link :to="'/articles/' + item.articleId" target="_blank" class="menu-action" @click="close">在新标签页打开</router-link>
        <div class="my-1 border-t border-outline/10"></div>
        <button :disabled="mutation.isPending.value" class="menu-action text-red-600 dark:text-red-400 hover:bg-red-500/10" @click="remove(item.articleId); close()">取消收藏</button>
      </template>
    </ArticleCardList>

    <template #footer>
      <InfiniteLoadMore
          v-if="items.length"
          :key="selectedFolder"
          :has-next-page="collectionsQuery.hasNextPage.value"
          :loading="collectionsQuery.isFetchingNextPage.value"
          :fetching="collectionsQuery.isFetching.value"
          :failed="collectionsQuery.isFetchNextPageError.value"
          :blocked="collectionsQuery.isError.value"
          :fetch-more="collectionsQuery.fetchNextPage"
      />
    </template>
  </LibraryPageLayout>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useQueryClient } from '@tanstack/vue-query';
import ArticleCardList from '@/components/ArticleCardList.vue';
import LibraryPageLayout from '@/components/LibraryPageLayout.vue';
import InfiniteLoadMore from '@/components/InfiniteLoadMore.vue';
import { useAuthStore } from '@/stores/auth';
import { collectionsQueryKey, useCollectionsQuery } from '@/queries/collections';
import { useCollectionFoldersQuery } from '@/queries/collectionFolders';
import { useCollectionMutation } from '@/queries/articleActions';
import { confirm } from '@/composables/confirm';

defineOptions({ name: 'CollectionsPage' });

const props = defineProps<{ folderId?: string }>();
const auth = useAuthStore();
const router = useRouter();
const client = useQueryClient();

const folderId = computed(() => props.folderId);
const selectedFolder = computed(() => props.folderId ?? 'all');
const foldersQuery = useCollectionFoldersQuery();

const filters = computed(() => [
  { id: 'all', name: '全部收藏' },
  { id: 'default', name: '默认收藏夹' },
  ...(foldersQuery.data.value?.pages.flatMap(page => page.content) ?? [])
      .map(folder => ({ id: folder.folderId, name: folder.name })),
]);

const collectionsQuery = useCollectionsQuery(folderId);
const { items, total } = collectionsQuery;
const mutation = useCollectionMutation();
const errorMessage = computed(() => collectionsQuery.errorMessage.value || mutation.error.value?.message || '');

const selectFolder = async (selected: string) => {
  if (selected === selectedFolder.value) return;
  mutation.reset();
  await client.resetQueries({ queryKey: collectionsQueryKey(auth.user?.userId ?? 'anonymous', selected === 'all' ? undefined : selected), exact: true });
  return router.push({ name: 'collections', query: selected === 'all' ? {} : { folderId: selected } });
};

const refresh = () => Promise.all([collectionsQuery.refetch(), foldersQuery.refetch()]);

const remove = async (articleId: string) => {
  if (mutation.isPending.value) return;
  const confirmed = await confirm({ title: '取消收藏', description: '确定要取消收藏这篇文章吗？' }).catch(() => false);
  if (confirmed) {
    mutation.mutate({ articleId, collected: false });
  }
};

watch([selectedFolder, filters, foldersQuery.hasNextPage, foldersQuery.isFetching, foldersQuery.isError, collectionsQuery.isSuccess, () => router.currentRoute.value.name], () => {
  if (router.currentRoute.value.name === 'collections' && collectionsQuery.isSuccess.value && !filters.value.some(filter => filter.id === selectedFolder.value)
      && foldersQuery.hasNextPage.value && !foldersQuery.isFetching.value && !foldersQuery.isError.value) {
    void foldersQuery.fetchNextPage();
  }
}, { immediate: true });

watch([folderId, () => auth.user?.userId], () => mutation.reset());
</script>

<style scoped>
@reference "../styles/tailwind.css";

.menu-action {
  @apply block w-full px-4 py-2 text-left text-xs font-medium text-text transition hover:bg-surface-container disabled:opacity-40;
}
</style>