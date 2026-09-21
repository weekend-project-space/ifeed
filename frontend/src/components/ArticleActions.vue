<template>
  <div class="flex items-center gap-1">
    <button :disabled="likeMutation.isPending.value" :title="liked ? '取消喜欢' : '喜欢'" :aria-label="liked ? '取消喜欢' : '喜欢'" :aria-pressed="liked" class="action-button" :class="{ 'text-red-500': liked }" @click="toggleLike">
      <svg class="h-5 w-5" :fill="liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z" /></svg>
    </button>
    <button :disabled="collectionMutation.isPending.value" :title="collected ? '调整收藏文件夹' : '收藏'" :aria-label="collected ? '调整收藏文件夹' : '收藏'" :aria-pressed="collected" aria-haspopup="dialog" class="action-button" @click="openCollection">
      <svg class="h-5 w-5" :fill="collected ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M5 5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16l-7-3.5L5 21V5Z" /></svg>
    </button>
  </div>
  <CollectionFolderDialog v-if="showFolders" title="调整收藏文件夹" :selected="folderId ?? 'default'" :submitting="collectionMutation.isPending.value" :error="collectionError"
    @close="showFolders = false" @select="moveCollection">
    <template #footer="{ busy }">
      <button :disabled="busy" class="mt-4 w-full rounded-lg border border-red-200 px-3 py-2 text-sm text-red-600 disabled:opacity-50" @click="cancelCollection">取消收藏</button>
    </template>
  </CollectionFolderDialog>
  <Teleport to="body">
    <div v-if="actionError && !showFolders" role="alert" class="fixed bottom-6 left-1/2 z-50 flex max-w-[90vw] -translate-x-1/2 items-center gap-3 rounded-lg bg-red-50 p-4 text-sm text-red-700 shadow-lg">
      {{ actionError }}<button aria-label="关闭错误提示" @click="collectionMutation.reset(); likeMutation.reset()">✕</button>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onDeactivated, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useCollectionMutation, useLikeMutation } from '@/queries/articleActions';
import CollectionFolderDialog from './CollectionFolderDialog.vue';

const props = defineProps<{ articleId: string; collected: boolean; liked: boolean; folderId: string | null }>();
const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const collectionMutation = useCollectionMutation();
const likeMutation = useLikeMutation();
const showFolders = ref(false);
const collectionError = computed(() => collectionMutation.error.value?.message ?? '');
const actionError = computed(() => collectionError.value || likeMutation.error.value?.message || '');
const requireLogin = () => {
  if (auth.isAuthenticated) return true;
  void router.push({ name: 'auth', query: { redirect: route.fullPath } });
  return false;
};
const toggleLike = () => {
  if (requireLogin() && !likeMutation.isPending.value) likeMutation.mutate({ articleId: props.articleId, liked: !props.liked });
};
const openCollection = async () => {
  if (!requireLogin() || collectionMutation.isPending.value) return;
  collectionMutation.reset();
  const articleId = props.articleId;
  const userId = auth.user?.userId;
  try {
    if (!props.collected) await collectionMutation.mutateAsync({ articleId, collected: true });
    if (props.articleId === articleId && auth.user?.userId === userId && route.name === 'article') showFolders.value = true;
  } catch { return; }
};
const cancelCollection = async () => {
  if (!requireLogin() || collectionMutation.isPending.value) return;
  try {
    await collectionMutation.mutateAsync({ articleId: props.articleId, collected: false });
    showFolders.value = false;
  } catch { return; }
};
const moveCollection = async (folderId: string) => {
  if (!requireLogin() || collectionMutation.isPending.value) return;
  try {
    await collectionMutation.mutateAsync({ articleId: props.articleId, collected: true, folderId: folderId === 'default' ? null : folderId });
    showFolders.value = false;
  } catch { return; }
};
const reset = () => { showFolders.value = false; collectionMutation.reset(); likeMutation.reset(); };
watch(() => [props.articleId, auth.user?.userId], reset);
onDeactivated(reset);
</script>

<style scoped>
.action-button {
  @apply flex h-10 w-10 items-center justify-center rounded-full hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-50;
}
</style>
