<template>
  <LibraryPageLayout
      :title="tr('likesPage.title')"
      :total="total"
      :total-text="total ? tr('likesPage.likeCount', { count: total }) : tr('likesPage.noLikes')"
      :pending="query.isPending.value"
      :fetching="query.isFetching.value"
      :error="errorMessage"
      :refresh-label="tr('likesPage.refreshLikes')"
      :login-text="tr('likesPage.loginHint')"
      :empty-title="tr('likesPage.emptyTitle')"
      :empty-text="tr('likesPage.emptyDescription')"
      @refresh="query.refetch()"
  >
    <DatedArticleList
        :items="items"
        time-field="likedAt"
        :removing="mutation.isPending.value"
        :remove-label="tr('common.unlike')"
        @remove="unlike"
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
import { useLikesQuery } from '@/queries/likes';
import { useLikeMutation } from '@/queries/articleActions';

defineOptions({ name: 'LikesPage' });

const query = useLikesQuery();
const { items, total } = query;
const mutation = useLikeMutation();
const errorMessage = computed(() => query.errorMessage.value || mutation.error.value?.message || '');

const unlike = (articleId: string) => {
  if (!mutation.isPending.value) {
    mutation.mutate({ articleId, liked: false });
  }
};
</script>
