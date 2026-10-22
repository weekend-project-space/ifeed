<template>
  <div class="text-text">
    <p class="mb-4 text-xs leading-relaxed text-text-secondary sm:text-sm">
      上传 OPML 文件批量导入订阅，文件大小建议不超过 2 MB
    </p>

    <!-- 选择文件表面容器 -->
    <div class="mb-4 rounded-3xl bg-surface-container/40 p-5 space-y-4">
      <div class="flex items-center gap-3.5">
        <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-surface text-text-secondary shadow-xs">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </div>
        <div class="min-w-0 flex-1">
          <p class="text-xs font-semibold text-text sm:text-sm">选择本地 OPML 文件</p>
          <p class="mt-0.5 text-[11px] text-text-secondary">支持各大阅读器导出的 .opml 与 .xml 格式</p>
        </div>
      </div>

      <div class="flex flex-wrap items-center gap-2 pt-1">
        <input
            ref="opmlFileInput"
            type="file"
            accept=".opml,.xml"
            class="min-w-0 flex-1 text-xs text-text-secondary file:mr-3 file:cursor-pointer file:rounded-full file:border-0 file:bg-surface file:px-4 file:py-2 file:text-xs file:font-semibold file:text-text file:shadow-xs file:transition hover:file:bg-surface-container cursor-pointer"
            @change="handleOpmlFileChange" />

        <button
            type="button"
            class="inline-flex h-9 w-20 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-95 disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="opmlPreviewLoading || !opmlFile"
            @click="handlePreviewOpml">
          <svg v-if="opmlPreviewLoading" class="h-3.5 w-3.5 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M21 12a9 9 0 1 1-6.219-8.56" />
          </svg>
          <span v-else>解析</span>
        </button>
      </div>
    </div>

    <!-- 错误反馈 -->
    <p v-if="opmlError" class="mb-4 rounded-2xl bg-red-500/10 p-4 text-xs text-red-600 dark:text-red-400">
      {{ opmlError }}
    </p>

    <!-- OPML 预览导入弹窗：MD3 28px 大圆角模态框 -->
    <transition name="modal">
      <div
          v-if="showOpmlModal"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
          @click.self="closeOpmlModal">
        <div class="absolute inset-0 bg-black/45" />
        <div class="relative z-10 flex max-h-[90vh] w-full max-w-3xl flex-col overflow-hidden rounded-[28px] bg-surface shadow-2xl">

          <!-- 头部 -->
          <div class="flex items-center justify-between border-b border-outline/10 px-6 py-4">
            <div>
              <h3 class="text-base font-bold text-text">OPML 导入预览</h3>
              <p class="mt-0.5 text-xs text-text-secondary">勾选需要导入至本地的订阅源</p>
            </div>
            <button
                class="flex h-8 w-8 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
                @click="closeOpmlModal">
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- 内容主体 -->
          <div class="flex-1 overflow-y-auto p-6 space-y-4">
            <!-- 警示提示 -->
            <div v-if="opmlWarnings.length" class="rounded-2xl bg-amber-500/10 p-4 text-xs text-amber-700 dark:text-amber-400">
              <p class="mb-1.5 flex items-center gap-1.5 font-bold">
                <svg class="h-4 w-4 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
                </svg>
                解析提醒
              </p>
              <ul class="space-y-1 pl-5 list-disc opacity-90 leading-relaxed">
                <li v-for="(warning, index) in opmlWarnings" :key="`warning-${index}`">
                  {{ warning }}
                </li>
              </ul>
            </div>

            <!-- 源列表 -->
            <div v-if="opmlPreviewFeeds.length" class="space-y-3">
              <div class="flex items-center justify-between pb-2 border-b border-outline/10 text-xs">
                <span class="text-text-secondary">
                  共 <strong class="font-semibold text-text">{{ opmlPreviewFeeds.length }}</strong> 个订阅源
                </span>
                <button
                    class="font-semibold text-primary hover:underline"
                    type="button"
                    @click="toggleSelectAll">
                  {{ isAllSelected ? '取消全选' : '全选所有' }}
                </button>
              </div>

              <div class="space-y-2 max-h-[380px] overflow-y-auto pr-1">
                <label
                    v-for="feed in opmlPreviewFeeds"
                    :key="feed.feedUrl"
                    class="group flex cursor-pointer items-start gap-3 rounded-2xl bg-surface-container/30 p-3 transition select-none hover:bg-surface-container/60"
                    :class="{ 'opacity-50 cursor-not-allowed': feed.alreadySubscribed || feed.errors.length > 0 }">
                  <input
                      type="checkbox"
                      class="mt-0.5 h-4 w-4 shrink-0 rounded-sm border-outline text-primary focus:ring-primary/20"
                      v-model="feed.selected"
                      :disabled="feed.alreadySubscribed || feed.errors.length > 0" />

                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 mb-0.5">
                      <h4 class="truncate text-xs font-semibold text-text">{{ feed.title }}</h4>
                      <span
                          v-if="feed.alreadySubscribed"
                          class="rounded-full bg-surface-container px-2 py-0.2 text-[10px] font-medium text-text-secondary shrink-0">
                        已订阅
                      </span>
                      <span
                          v-if="feed.errors.length"
                          class="rounded-full bg-red-500/10 px-2 py-0.2 text-[10px] font-medium text-red-600 dark:text-red-400 shrink-0">
                        无法导入
                      </span>
                    </div>

                    <a
                        :href="feed.siteUrl"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="block truncate text-[11px] text-primary hover:underline mb-0.5">
                      {{ feed.siteUrl }}
                    </a>
                    <p class="truncate text-[11px] text-text-secondary/70">{{ feed.feedUrl }}</p>
                    <p v-if="feed.errors.length" class="mt-1 text-xs text-red-600 dark:text-red-400">
                      {{ feed.errors.join('；') }}
                    </p>
                  </div>
                </label>
              </div>
            </div>

            <p v-else-if="!opmlConfirmResult" class="py-16 text-center text-xs text-text-secondary">
              暂无可预览的订阅源
            </p>

            <!-- 导入成功结果 -->
            <div v-if="opmlConfirmResult" class="rounded-3xl bg-green-500/10 p-5">
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center gap-3">
                  <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-green-600 text-white shadow-xs">
                    <svg class="h-5 w-5 stroke-[2.5]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                  <div>
                    <p class="text-sm font-bold text-text">导入完成</p>
                    <p class="text-xs text-text-secondary">成功导入 {{ opmlConfirmResult.importedCount }} 个订阅源</p>
                  </div>
                </div>
                <button
                    class="inline-flex h-8 items-center justify-center rounded-full bg-surface px-4 text-xs font-semibold text-text shadow-xs hover:bg-surface-container transition"
                    @click="handleSuccessClose">
                  关闭
                </button>
              </div>

              <ul v-if="opmlConfirmResult.skipped.length" class="space-y-1 text-xs text-text-secondary pl-2 border-t border-outline/10 pt-2.5">
                <li v-for="(item, index) in opmlConfirmResult.skipped" :key="`${item.feedUrl}-${index}`" class="truncate">
                  • {{ item.feedUrl || '未知源' }}：{{ translateSkippedReason(item.reason) }}
                </li>
              </ul>
            </div>
          </div>

          <!-- 弹窗底部操作栏 -->
          <div v-if="!opmlConfirmResult" class="flex flex-wrap items-center justify-between gap-3 border-t border-outline/10 bg-surface-container/20 px-6 py-4">
            <div class="text-xs text-text-secondary">
              <span>已选 <strong class="font-semibold text-text">{{ selectedCount }}</strong> 项</span>
              <span class="mx-2">•</span>
              <span>剩余额度 <strong class="font-semibold text-text">{{ remainingQuota }}</strong></span>
              <span v-if="selectedCount > remainingQuota" class="ml-2 font-semibold text-red-600 dark:text-red-400">
                (已超可用额度)
              </span>
            </div>

            <button
                type="button"
                class="inline-flex h-9 w-24 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-95 disabled:cursor-not-allowed disabled:opacity-40"
                :disabled="opmlConfirmLoading || !hasSelectedFeeds || selectedCount > remainingQuota"
                @click="handleConfirmOpml">
              <svg v-if="opmlConfirmLoading" class="h-3.5 w-3.5 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M21 12a9 9 0 1 1-6.219-8.56" />
              </svg>
              <span v-else>确认导入</span>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  type OpmlImportResultDto,
  type OpmlPreviewFeedDto
} from '@/api/subscriptions';
import { useOpmlImportMutation, useOpmlPreviewMutation } from '@/queries/subscriptions';

defineOptions({ name: 'SubscriptionsAddOpml' });

const emit = defineEmits<{ success: [] }>();
const previewMutation = useOpmlPreviewMutation();
const importMutation = useOpmlImportMutation();

interface OpmlPreviewFeedView extends OpmlPreviewFeedDto {
  selected: boolean;
}

const opmlFile = ref<File | null>(null);
const opmlFileInput = ref<HTMLInputElement | null>(null);
const opmlPreviewFeeds = ref<OpmlPreviewFeedView[]>([]);
const opmlWarnings = ref<string[]>([]);
const opmlError = ref<string | null>(null);
const opmlPreviewLoading = previewMutation.isPending;
const opmlConfirmLoading = importMutation.isPending;
const opmlConfirmResult = ref<OpmlImportResultDto | null>(null);
const showOpmlModal = ref(false);
const remainingQuota = ref(0);

const handleOpmlFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const [file] = target.files ?? [];
  opmlFile.value = file ?? null;
  opmlError.value = null;
};

const handlePreviewOpml = async () => {
  if (!opmlFile.value) {
    opmlError.value = '请选择一个 OPML 文件';
    return;
  }
  opmlError.value = null;
  opmlConfirmResult.value = null;
  try {
    const result = await previewMutation.mutateAsync(opmlFile.value);
    opmlWarnings.value = result.warnings ?? [];
    remainingQuota.value = result.remainingQuota ?? 0;
    opmlPreviewFeeds.value = result.feeds.map((feed) => ({
      ...feed,
      selected: !feed.alreadySubscribed && feed.errors.length === 0
    }));
    showOpmlModal.value = true;
  } catch (error) {
    const message = error instanceof Error ? error.message : '解析 OPML 失败';
    opmlError.value = message;
    opmlPreviewFeeds.value = [];
    opmlWarnings.value = [];
  }
};

const selectableFeeds = computed(() =>
    opmlPreviewFeeds.value.filter((feed) => !feed.alreadySubscribed && feed.errors.length === 0)
);
const selectedCount = computed(() => selectableFeeds.value.filter((feed) => feed.selected).length);
const hasSelectedFeeds = computed(() => selectedCount.value > 0);
const isAllSelected = computed(
    () => selectableFeeds.value.length > 0 && selectableFeeds.value.every((feed) => feed.selected)
);

const toggleSelectAll = () => {
  const selectAll = !isAllSelected.value;
  opmlPreviewFeeds.value = opmlPreviewFeeds.value.map((feed) => {
    if (feed.alreadySubscribed || feed.errors.length > 0) return feed;
    return { ...feed, selected: selectAll };
  });
};

const resetOpmlInput = () => {
  opmlFile.value = null;
  if (opmlFileInput.value) {
    opmlFileInput.value.value = '';
  }
};

const closeOpmlModal = () => {
  showOpmlModal.value = false;
};

const handleSuccessClose = () => {
  closeOpmlModal();
  opmlConfirmResult.value = null;
  emit('success');
};

const handleConfirmOpml = async () => {
  if (!hasSelectedFeeds.value) {
    opmlError.value = '请至少选择一个订阅项';
    return;
  }
  if (selectedCount.value > remainingQuota.value) {
    opmlError.value = '超过剩余额度，请取消部分订阅后再试';
    return;
  }
  opmlError.value = null;
  try {
    const payload = opmlPreviewFeeds.value.map((feed) => ({
      feedUrl: feed.feedUrl,
      title: feed.title,
      siteUrl: feed.siteUrl,
      avatar: feed.avatar ?? undefined,
      selected: feed.selected
    }));
    const result = await importMutation.mutateAsync(payload);
    opmlConfirmResult.value = result;
    opmlPreviewFeeds.value = [];
    opmlWarnings.value = [];
    resetOpmlInput();
  } catch (error) {
    const message = error instanceof Error ? error.message : '导入失败';
    opmlError.value = message;
  }
};

const translateSkippedReason = (reason: string) => {
  const mapping: Record<string, string> = {
    ALREADY_SUBSCRIBED: '已订阅',
    INVALID_FEED: '无效订阅',
    DUPLICATE: '重复条目',
    FAILED: '导入失败'
  };
  return mapping[reason] ?? reason;
};
</script>

<style scoped>
.modal-enter-active, .modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from, .modal-leave-to {
  opacity: 0;
}
</style>