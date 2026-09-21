<template>
  <LibraryPageLayout title="我的喜欢" :total="total" :total-text="total ? '共 ' + total + ' 条喜欢' : '暂无喜欢'"
    :pending="query.isPending.value" :fetching="query.isFetching.value" :error="errorMessage"
    refresh-label="刷新我的喜欢" login-text="登录后即可查看你的喜欢"
    empty-title="还没有喜欢的文章" empty-text="阅读时点击爱心，你喜欢的文章将显示在这里" @refresh="query.refetch()">
    <DatedArticleList :items="items" time-field="likedAt" :removing="mutation.isPending.value" remove-label="取消喜欢" @remove="unlike" />
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
import { useLikesQuery } from '@/queries/likes';
import { useLikeMutation } from '@/queries/articleActions';

defineOptions({ name: 'LikesPage' });
const query = useLikesQuery();
const { items, total } = query;
const mutation = useLikeMutation();
const errorMessage = computed(() => query.errorMessage.value || mutation.error.value?.message || '');
const unlike = (articleId: string) => {
  if (!mutation.isPending.value) mutation.mutate({ articleId, liked: false });
};
</script>
