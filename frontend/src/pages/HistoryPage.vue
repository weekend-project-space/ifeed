<template>
  <LibraryPageLayout
      :title="tr('historyPage.title')"
      :total="total"
      :total-text="total ? tr('historyPage.recordCount', { count: total }) : tr('common.noRecords')"
      :pending="query.isPending.value"
      :fetching="query.isFetching.value"
      :error="errorMessage"
      :refresh-label="tr('historyPage.refreshHistory')"
      :login-text="tr('historyPage.loginHint')"
      :empty-title="tr('historyPage.emptyTitle')"
      :empty-text="tr('historyPage.emptyDescription')"
      @refresh="query.refetch()"
  >
    <DatedArticleList
        :items="items"
        time-field="readAt"
        :removing="mutation.isPending.value"
        :remove-label="tr('historyPage.removeRecord')"
        @remove="remove"
    />
    <template #footer>
      <InfiniteLoadMore
          v-if="items.length"
          :has-next-page="query.hasNextPage.value"
          :loading="query.isFetchingNextPage.value"
          :fetching="query.isFetching.value"
          :failed="query.isFetchNextPageError.value"
          :blocked="query.isError.value"
          :fetch-more="query.fetchNextPage"
      />
    </template>
  </LibraryPageLayout>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
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
  const confirmed = await confirm({ title: tr('historyPage.removeRecord'), description: tr('historyPage.confirmRemove') }).catch(() => false);
  if (confirmed) {
    mutation.mutate(articleId);
  }
};
</script>
