<template>
  <div>
    <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">
      上传 OPML 文件批量导入订阅，文件大小建议不超过 2 MB
    </p>
    <div class=" bg-white dark:bg-gray-800  dark:border-gray-700  mb-3 hover:shadow-sm transition-all space-y-5">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-gray-100 dark:bg-gray-700 rounded-full flex items-center justify-center flex-shrink-0">
          <svg class="w-5 h-5 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-sm font-medium text-gray-900 dark:text-gray-100 mb-0.5">选择 OPML 文件</p>
          <p class="text-xs text-gray-600 dark:text-gray-400">支持 .opml 和 .xml 格式</p>
        </div>

      </div>
      <div class="flex items-center gap-2 flex-shrink-0">
        <input
            ref="opmlFileInput"
            type="file"
            accept=".opml,.xml"
            class="text-xs text-gray-600 dark:text-gray-400 file:mr-2 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-xs file:font-medium file:bg-gray-100 dark:file:bg-gray-700 file:text-gray-900 dark:file:text-gray-100 hover:file:bg-gray-200 dark:hover:file:bg-gray-600 file:cursor-pointer cursor-pointer file:transition-all"
            @change="handleOpmlFileChange" />
        <button
            class="px-5 py-2 text-sm font-medium text-white bg-secondary rounded-full hover:bg-secondary/90 disabled:bg-gray-300 dark:disabled:bg-gray-700 disabled:text-gray-500 disabled:cursor-not-allowed transition-all"
            :disabled="opmlPreviewLoading || !opmlFile"
            @click="handlePreviewOpml">
          {{ opmlPreviewLoading ? '解析中...' : '解析' }}
        </button>
      </div>
    </div>
    <p v-if="opmlError" class="px-4 py-2.5 text-sm text-red-700 dark:text-red-400 bg-red-50 dark:bg-red-900/20 rounded-lg">
      {{ opmlError }}
    </p>

    <!-- OPML Modal -->
    <transition name="modal">
      <div
          v-if="showOpmlModal"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40"
          @click.self="closeOpmlModal">
        <div class="relative w-full max-w-3xl bg-white dark:bg-gray-800 rounded-lg shadow-2xl max-h-[90vh] flex flex-col">
          <!-- Modal Header -->
          <div class="flex items-center justify-between p-5 border-b border-gray-200 dark:border-gray-700">
            <div>
              <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100">OPML 导入预览</h3>
              <p class="text-sm text-gray-600 dark:text-gray-400 mt-0.5">选择需要导入的订阅源</p>
            </div>
            <button
                class="p-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-all"
                @click="closeOpmlModal">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Modal Content -->
          <div class="flex-1 overflow-y-auto p-5">
            <div v-if="opmlWarnings.length" class="mb-4 p-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-400 dark:border-amber-600 rounded-lg">
              <p class="text-sm font-medium text-gray-900 dark:text-gray-100 mb-2 flex items-center gap-2">
                <svg class="w-5 h-5 text-amber-500" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
                </svg>
                解析提醒
              </p>
              <ul class="text-sm text-gray-600 dark:text-gray-400 space-y-1">
                <li v-for="(warning, index) in opmlWarnings" :key="`warning-${index}`" class="flex items-start gap-2">
                  <span class="text-amber-500">•</span>
                  <span>{{ warning }}</span>
                </li>
              </ul>
            </div>

            <div v-if="opmlPreviewFeeds.length" class="space-y-3">
              <div class="flex items-center justify-between pb-3 border-b border-gray-200 dark:border-gray-700">
                <p class="text-sm text-gray-600 dark:text-gray-400">
                  共 <span class="font-medium text-gray-900 dark:text-gray-100">{{ opmlPreviewFeeds.length }}</span> 个订阅源
                </p>
                <button
                    class="text-sm font-medium text-secondary hover:underline"
                    type="button"
                    @click="toggleSelectAll">
                  {{ isAllSelected ? '取消全选' : '全选' }}
                </button>
              </div>

              <div class="space-y-2">
                <label
                    v-for="feed in opmlPreviewFeeds"
                    :key="feed.feedUrl"
                    class="flex items-start gap-3 p-3 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-750 transition-all cursor-pointer"
                    :class="{ 'opacity-50 cursor-not-allowed': feed.alreadySubscribed || feed.errors.length > 0 }">
                  <input
                      type="checkbox"
                      class="mt-0.5 w-4 h-4 text-secondary border-gray-300 dark:border-gray-600 rounded focus:ring-secondary focus:ring-2"
                      v-model="feed.selected"
                      :disabled="feed.alreadySubscribed || feed.errors.length > 0" />
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 mb-1">
                      <h4 class="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{{ feed.title }}</h4>
                      <span
                          v-if="feed.alreadySubscribed"
                          class="px-2 py-0.5 text-xs font-medium text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 rounded-full flex-shrink-0">
                        已订阅
                      </span>
                      <span
                          v-if="feed.errors.length"
                          class="px-2 py-0.5 text-xs font-medium text-red-700 dark:text-red-400 bg-red-50 dark:bg-red-900/20 rounded-full flex-shrink-0">
                        无法导入
                      </span>
                    </div>
                    <a
                        :href="feed.siteUrl"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="text-xs text-secondary hover:underline block truncate mb-0.5">
                      {{ feed.siteUrl }}
                    </a>
                    <p class="text-xs text-gray-600 dark:text-gray-400 truncate">{{ feed.feedUrl }}</p>
                    <p v-if="feed.errors.length" class="text-xs text-red-700 dark:text-red-400 mt-1">
                      {{ feed.errors.join('；') }}
                    </p>
                  </div>
                </label>
              </div>
            </div>

            <p v-else-if="!opmlConfirmResult" class="py-16 text-center text-sm text-gray-600 dark:text-gray-400">
              暂无可预览的订阅
            </p>

            <div v-if="opmlConfirmResult" class="p-4 bg-green-50 dark:bg-green-900/20 border border-green-500 dark:border-green-600 rounded-lg">
              <div class="flex items-center justify-between mb-2">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-green-500 rounded-full flex items-center justify-center flex-shrink-0">
                    <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                  <div>
                    <p class="text-sm font-medium text-gray-900 dark:text-gray-100">导入完成</p>
                    <p class="text-xs text-gray-600 dark:text-gray-400">成功导入 {{ opmlConfirmResult.importedCount }} 个订阅</p>
                  </div>
                </div>
                <button
                    class="px-4 py-2 text-sm font-medium text-secondary bg-white dark:bg-gray-700 rounded-full hover:bg-gray-100 dark:hover:bg-gray-600 transition-all"
                    @click="handleSuccessClose">
                  关闭
                </button>
              </div>
              <ul v-if="opmlConfirmResult.skipped.length" class="text-xs text-gray-600 dark:text-gray-400 space-y-1 mt-2 pl-13">
                <li v-for="(item, index) in opmlConfirmResult.skipped" :key="`${item.feedUrl}-${index}`" class="flex items-start gap-2">
                  <span class="text-gray-600 dark:text-gray-400">•</span>
                  <span>{{ item.feedUrl || '未知订阅' }}：{{ translateSkippedReason(item.reason) }}</span>
                </li>
              </ul>
            </div>
          </div>

          <!-- Modal Footer -->
          <div v-if="!opmlConfirmResult" class="p-5 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-750">
            <div class="flex items-center justify-between">
              <div class="text-sm text-gray-600 dark:text-gray-400">
                <span>已选 <span class="font-medium text-gray-900 dark:text-gray-100">{{ selectedCount }}</span> 项</span>
                <span class="mx-2">•</span>
                <span>剩余额度 <span class="font-medium text-gray-900 dark:text-gray-100">{{ remainingQuota }}</span></span>
                <span v-if="selectedCount > remainingQuota" class="ml-3 text-red-700 dark:text-red-400 font-medium">
                  已超过可用额度
                </span>
              </div>
              <button
                  type="button"
                  class="px-6 py-2 text-sm font-medium text-white bg-secondary rounded-full hover:bg-secondary/90 disabled:bg-gray-300 dark:disabled:bg-gray-700 disabled:text-gray-500 disabled:cursor-not-allowed transition-all"
                  :disabled="opmlConfirmLoading || !hasSelectedFeeds || selectedCount > remainingQuota"
                  @click="handleConfirmOpml">
                {{ opmlConfirmLoading ? '导入中...' : '确认导入' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  useSubscriptionsStore,
  type OpmlImportResultDto,
  type OpmlPreviewFeedDto
} from '../../stores/subscriptions';

const subscriptionsStore = useSubscriptionsStore();

interface OpmlPreviewFeedView extends OpmlPreviewFeedDto {
  selected: boolean;
}

const opmlFile = ref<File | null>(null);
const opmlFileInput = ref<HTMLInputElement | null>(null);
const opmlPreviewFeeds = ref<OpmlPreviewFeedView[]>([]);
const opmlWarnings = ref<string[]>([]);
const opmlError = ref<string | null>(null);
const opmlPreviewLoading = ref(false);
const opmlConfirmLoading = ref(false);
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
  opmlPreviewLoading.value = true;
  opmlError.value = null;
  opmlConfirmResult.value = null;
  try {
    const result = await subscriptionsStore.previewOpmlImport(opmlFile.value);
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
  } finally {
    opmlPreviewLoading.value = false;
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
  opmlConfirmLoading.value = true;
  opmlError.value = null;
  try {
    const payload = opmlPreviewFeeds.value.map((feed) => ({
      feedUrl: feed.feedUrl,
      title: feed.title,
      siteUrl: feed.siteUrl,
      avatar: feed.avatar ?? undefined,
      selected: feed.selected
    }));
    const result = await subscriptionsStore.confirmOpmlImport(payload);
    opmlConfirmResult.value = result;
    await subscriptionsStore.fetchSubscriptions();
    opmlPreviewFeeds.value = [];
    opmlWarnings.value = [];
    resetOpmlInput();
  } catch (error) {
    const message = error instanceof Error ? error.message : '导入失败';
    opmlError.value = message;
  } finally {
    opmlConfirmLoading.value = false;
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
