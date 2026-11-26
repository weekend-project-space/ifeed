<template>
  <div class="flex shrink-0 items-center gap-1" @click.stop>
    <button
        v-if="!compact"
        type="button"
        class="flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
        :class="linkCopied ? 'text-green-600 dark:text-green-400' : ''"
        :title="tr('common.copyLink')"
        :aria-label="tr('common.copyArticleLink')"
        @click="copyArticleLink">
      <svg v-if="!linkCopiedLoading" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
           stroke="currentColor">
        <rect x="8" y="8" width="13" height="13" rx="2"
              stroke-width="1.8"/>
        <path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3"
              stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>

      <svg v-else class="h-5 w-5" fill="none" viewBox="0 0 24 24"
           stroke="currentColor">
        <path d="M5 12.5l4.5 4.5L19 7"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </button>
    <button type="button" :disabled="likeMutation.isPending.value" :title="effectiveLiked ? tr('common.unlike') : tr('common.like')"
            :aria-label="effectiveLiked ? tr('common.unlike') : tr('common.like')" :aria-pressed="effectiveLiked" :aria-busy="likeMutation.isPending.value" class="action-button"
            :class="[inline ? 'action-text' : dense ? 'h-9 w-9' : 'h-10 w-10', effectiveLiked ? 'text-red-500' : 'text-text-secondary', inline && !effectiveLiked ? 'hover:text-primary' : '']" @click="toggleLike">
      <svg v-if="!inline" :class="compact ? 'h-4 w-4' : 'h-5 w-5'" :fill="effectiveLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" aria-hidden="true"
           viewBox="0 0 24 24">
        <path
            d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z"/>
      </svg>
      <span v-else>{{ effectiveLiked ? tr('common.unlike') : tr('common.like') }}</span>
    </button>
    <button type="button" :disabled="collectionMutation.isPending.value" :title="effectiveCollected ? tr('common.adjustFolder') : tr('common.collect')"
            :aria-label="effectiveCollected ? tr('common.adjustFolder') : tr('common.collect')" :aria-pressed="effectiveCollected" :aria-busy="collectionMutation.isPending.value"
            :aria-haspopup="effectiveCollected || !compact ? 'dialog' : undefined"
            class="action-button" :class="[inline ? 'action-text' : dense ? 'h-9 w-9' : 'h-10 w-10', effectiveCollected ? 'text-primary' : 'text-text-secondary', inline && !effectiveCollected ? 'hover:text-primary' : '']" @click="openCollection">
      <svg v-if="!inline" :class="compact ? 'h-4 w-4' : 'h-5 w-5'" :fill="effectiveCollected ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" aria-hidden="true"
           viewBox="0 0 24 24">
        <path d="M5 5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16l-7-3.5L5 21V5Z"/>
      </svg>
      <span v-else>{{ tr('common.collect') }}</span>
    </button>
  </div>
  <CollectionFolderDialog v-if="showFolders" :title="tr('common.adjustFolder')" :selected="folderId ?? 'default'"
                          :submitting="collectionMutation.isPending.value" :error="collectionError"
                          @close="showFolders = false" @select="moveCollection">
    <template #footer="{ busy }">
      <button :disabled="busy"
              class="mt-4 w-full rounded-lg border border-red-200 px-3 py-2 text-sm text-red-600 disabled:opacity-50"
              @click="cancelCollection">{{ tr('common.removeCollection') }}
      </button>
    </template>
  </CollectionFolderDialog>
  <Teleport to="body">
    <div v-if="ownsSavedNotice && !showFolders" role="status"
         class="fixed bottom-24 left-1/2 z-50 flex max-w-[90vw] -translate-x-1/2 items-center gap-3 rounded-2xl border border-outline/20 bg-surface px-4 py-3 text-xs text-text shadow-lg sm:bottom-6">
      <span>{{ tr('common.savedToDefaultFolder') }}</span>
      <button type="button" class="shrink-0 font-semibold text-primary" @click="openCollection">{{ tr('common.adjustFolder') }}</button>
      <button type="button" :aria-label="tr('common.close')" @click="clearSavedNotice">✕</button>
    </div>
    <div v-if="actionError && !showFolders" role="alert"
         class="fixed bottom-24 left-1/2 z-50 flex max-w-[90vw] -translate-x-1/2 items-center gap-3 rounded-lg bg-red-50 p-4 text-sm text-red-700 shadow-lg sm:bottom-6">
      {{ actionError }}
      <button :aria-label="tr('common.closeError')" @click="collectionMutation.reset(); likeMutation.reset()">✕</button>
    </div>
  </Teleport>
</template>

<script lang="ts">
import {shallowRef} from 'vue';

// One toast across all visible/cached cards, even if an article appears more than once.
const collectionNoticeOwner = shallowRef<symbol | null>(null);
</script>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import {computed, onActivated, onBeforeUnmount, onDeactivated, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useAuthStore} from '@/stores/auth';
import {useCollectionMutation, useLikeMutation} from '@/queries/articleActions';
import CollectionFolderDialog from './CollectionFolderDialog.vue';

const props = withDefaults(defineProps<{
  articleId: string;
  collected: boolean;
  liked: boolean;
  linkCopied?: boolean;
  folderId?: string | null;
  compact?: boolean;
  dense?: boolean;
  inline?: boolean;
}>(), {linkCopied: false, folderId: null, compact: false, dense: false, inline: false});
const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const collectionMutation = useCollectionMutation();
const likeMutation = useLikeMutation();
const showFolders = ref(false);
const collectionError = computed(() => collectionMutation.error.value?.message ?? '');
const actionError = computed(() => collectionError.value || likeMutation.error.value?.message || '');
const linkCopiedLoading = ref(false);
const optimisticLiked = ref<boolean | null>(null);
const optimisticCollected = ref<boolean | null>(null);
const effectiveLiked = computed(() => optimisticLiked.value ?? props.liked);
const effectiveCollected = computed(() => optimisticCollected.value ?? props.collected);
const noticeOwner = Symbol('collection-notice');
const ownsSavedNotice = computed(() => collectionNoticeOwner.value === noticeOwner);
let noticeTimer: ReturnType<typeof setTimeout> | undefined;
let active = true;
let generation = 0;
const clearSavedNotice = () => {
  clearTimeout(noticeTimer);
  if (ownsSavedNotice.value) collectionNoticeOwner.value = null;
};
const requireLogin = () => {
  if (auth.isAuthenticated) return true;
  void router.push({name: 'auth', query: {redirect: route.fullPath}});
  return false;
};
const copyArticleLink = async () => {
  try {
    const url = window.location.href;
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(url);
    } else {
      const textArea = document.createElement('textarea');
      textArea.value = url;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      textArea.style.top = '-999999px';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      try {
        document.execCommand('copy');
        textArea.remove();
      } catch (err) {
        textArea.remove();
        throw err;
      }
    }
    linkCopiedLoading.value = true;
    setTimeout(() => {
      linkCopiedLoading.value = false;
    }, 2000);
  } catch (err) {
    console.error('复制链接失败:', err);
  }
};

const toggleLike = async () => {
  if (!requireLogin() || likeMutation.isPending.value) return;
  clearSavedNotice();
  const currentGeneration = generation;
  optimisticLiked.value = !effectiveLiked.value;
  try {
    await likeMutation.mutateAsync({articleId: props.articleId, liked: optimisticLiked.value});
  } catch {
    // The mutation exposes the error; restoring the prop restores the confirmed state.
  } finally {
    if (generation === currentGeneration) optimisticLiked.value = null;
  }
};
const openCollection = async () => {
  if (!requireLogin() || collectionMutation.isPending.value) return;
  collectionMutation.reset();
  const articleId = props.articleId;
  const userId = auth.user?.userId;
  const originRoute = route.fullPath;
  const currentGeneration = generation;
  const wasCollected = effectiveCollected.value;
  clearSavedNotice();
  try {
    if (!wasCollected) {
      optimisticCollected.value = true;
      await collectionMutation.mutateAsync({articleId, collected: true});
    }
    if (!active || generation !== currentGeneration || props.articleId !== articleId
        || auth.user?.userId !== userId || route.fullPath !== originRoute) return;
    if (props.compact && !wasCollected) {
      collectionNoticeOwner.value = noticeOwner;
      noticeTimer = setTimeout(clearSavedNotice, 5000);
    } else showFolders.value = true;
  } catch {
    return;
  } finally {
    if (generation === currentGeneration) optimisticCollected.value = null;
  }
};
const cancelCollection = async () => {
  if (!requireLogin() || collectionMutation.isPending.value) return;
  try {
    await collectionMutation.mutateAsync({articleId: props.articleId, collected: false});
    showFolders.value = false;
  } catch {
    return;
  }
};
const moveCollection = async (folderId: string) => {
  if (!requireLogin() || collectionMutation.isPending.value) return;
  try {
    await collectionMutation.mutateAsync({
      articleId: props.articleId,
      collected: true,
      folderId: folderId === 'default' ? null : folderId
    });
    showFolders.value = false;
  } catch {
    return;
  }
};
const reset = () => {
  generation += 1;
  clearSavedNotice();
  optimisticLiked.value = null;
  optimisticCollected.value = null;
  showFolders.value = false;
  collectionMutation.reset();
  likeMutation.reset();
};
watch(() => [props.articleId, auth.user?.userId, route.fullPath], reset);
onActivated(() => { active = true; });
onDeactivated(() => { active = false; reset(); });
onBeforeUnmount(() => { active = false; reset(); });
</script>

<style scoped>
@reference "../styles/tailwind.css";

.action-button {
  @apply flex items-center justify-center rounded-full hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-50;
}

.action-text {
  @apply h-6 rounded-sm px-1 text-[11px] transition-colors hover:bg-transparent;
}
</style>
