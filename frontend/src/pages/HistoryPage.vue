<template>
  <LibraryPageLayout title="阅读记录" :total="total" :total-text="total ? '共 ' + total + ' 条记录' : '暂无记录'"
    :pending="query.isPending.value" :fetching="query.isFetching.value" :error="errorMessage"
    refresh-label="刷新阅读历史" login-text="登录后即可查看你的阅读记录"
    empty-title="还没有阅读记录" empty-text="开始阅读文章后，你的历史记录将显示在这里" @refresh="query.refetch()">
    <DatedArticleList :items="items" time-field="readAt" :removing="mutation.isPending.value" remove-label="删除" @remove="remove" />
    <template #footer>
      <InfiniteLoadMore v-if="items.length" :has-next-page="query.hasNextPage.value" :loading="query.isFetchingNextPage.value"
        :fetching="query.isFetching.value" :failed="query.isFetchNextPageError.value" :blocked="query.isError.value" :fetch-more="query.fetchNextPage" />
    </template>
  </LibraryPageLayout>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import LibraryPageLayout from '@/components/LibraryPageLayout.vue';
import DatedArticleList from '@/components/DatedArticleList.vue';
import InfiniteLoadMore from '@/components/InfiniteLoadMore.vue';
import { useHistoryQuery } from '@/queries/history';
import { useHistoryMutation } from '@/queries/articleActions';
import { confirm } from '@/composables/confirm';

defineOptions({ name: 'HistoryPage' });
const query = useHistoryQuery();
const { items, total } = query;
const mutation = useHistoryMutation('remove');
const errorMessage = computed(() => query.errorMessage.value || mutation.error.value?.message || '');
const remove = async (articleId: string) => {
  if (mutation.isPending.value) return;
  const confirmed = await confirm({ title: '删除记录', description: '确定要删除这条阅读记录吗？' }).catch(() => false);
  if (confirmed) mutation.mutate(articleId);
};
</script>
