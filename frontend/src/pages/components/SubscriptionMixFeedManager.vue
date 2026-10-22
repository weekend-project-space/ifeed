<template>
  <div>
    <!-- 头部工具栏 -->
    <div class="mb-6 flex items-center justify-end gap-2">
      <button
          @click="refresh"
          :disabled="mixFeedsStore.loading || isRefreshing"
          class="flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text disabled:opacity-40"
          aria-label="刷新"
      >
        <svg
            class="h-4 w-4"
            :class="{ 'animate-spin': mixFeedsStore.loading || isRefreshing }"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
        >
          <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
        </svg>
      </button>

      <!-- MD3 主操作按钮 -->
      <button
          @click="openCreateModal"
          class="inline-flex h-9 items-center gap-1.5 rounded-full bg-primary px-4 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-[0.98]"
      >
        <svg class="h-4 w-4 stroke-[2.2]" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path stroke-linecap="round" d="M12 5v14M5 12h14"/>
        </svg>
        <span>新建订阅</span>
      </button>
    </div>

    <!-- 错误横幅 -->
    <div v-if="loadError" class="mb-6 rounded-2xl bg-red-500/10 p-4 text-xs text-red-600 dark:text-red-400" role="alert">
      <p class="font-semibold">订阅聚合加载失败</p>
      <p class="mt-0.5 opacity-90">{{ loadError }}</p>
      <button type="button" class="mt-2 font-semibold underline underline-offset-2 hover:opacity-80" @click="refresh">重试</button>
    </div>

    <!-- 骨架屏 -->
    <div v-if="mixFeedsStore.loading && !mixFeedsStore.myMixFeeds.length" class="space-y-3">
      <div v-for="i in 3" :key="i" class="flex items-start gap-4 rounded-2xl bg-surface-container/30 p-4 animate-pulse">
        <div class="h-10 w-10 shrink-0 rounded-full bg-surface-container"></div>
        <div class="flex-1 space-y-2 py-1">
          <div class="h-4 w-1/3 rounded bg-surface-container"></div>
          <div class="h-3 w-1/2 rounded bg-surface-container"></div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!mixFeedsStore.myMixFeeds.length"
         class="rounded-3xl border-2 border-dashed border-outline/20 bg-surface-container/20 py-16 text-center">
      <div class="mx-auto mb-3.5 flex h-14 w-14 items-center justify-center rounded-full bg-surface-container/80">
        <svg class="h-6 w-6 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
        </svg>
      </div>
      <h2 class="text-base font-bold text-text">没有聚合订阅</h2>
      <p class="mt-1 text-xs text-text-secondary mb-5">创建订阅以聚合多个来源的内容</p>
      <button
          @click="openCreateModal"
          class="inline-flex h-9 items-center gap-1.5 rounded-full bg-primary px-5 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-[0.98]"
      >
        新建订阅
      </button>
    </div>

    <!-- 列表容器：MD3 卡片阶梯 -->
    <div v-else class="space-y-2.5">
      <div
          v-for="feed in mixFeedsStore.myMixFeeds"
          :key="feed.id"
          class="group flex items-start gap-3.5 rounded-2xl bg-surface-container/30 p-3.5 transition hover:bg-surface-container/60 hover:shadow-xs sm:p-4"
      >
        <!-- 头像微型指示器 -->
        <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-surface text-text-secondary shadow-xs">
          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
          </svg>
        </div>

        <div class="flex-1 min-w-0">
          <div class="flex items-start justify-between gap-3 mb-1">
            <router-link :to="`/feed/${feed.id}`" class="flex-1 min-w-0">
              <h3 class="truncate text-xs font-bold text-text transition group-hover:text-primary sm:text-sm">
                {{ feed.name }}
              </h3>
            </router-link>

            <!-- 操作下拉 -->
            <div class="relative flex shrink-0 items-center">
              <button
                  type="button"
                  class="flex h-7 w-7 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface hover:text-text"
                  @click.stop="toggleDropdown(feed.id)"
                  :aria-expanded="activeDropdown === feed.id"
                  aria-label="更多操作"
              >
                <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="1" fill="currentColor"/>
                  <circle cx="12" cy="5" r="1" fill="currentColor"/>
                  <circle cx="12" cy="19" r="1" fill="currentColor"/>
                </svg>
              </button>
              <div
                  v-if="activeDropdown === feed.id"
                  class="absolute right-0 top-full z-20 mt-1 w-32 overflow-hidden rounded-2xl border border-outline/10 bg-surface py-1 shadow-xl"
              >
                <button
                    @click.stop="openEditModal(feed)"
                    class="w-full px-4 py-2 text-left text-xs font-medium text-text transition hover:bg-surface-container"
                >
                  编辑
                </button>
                <button
                    @click.stop="confirmDelete(feed)"
                    class="w-full px-4 py-2 text-left text-xs font-medium text-red-600 transition hover:bg-red-500/10 dark:text-red-400"
                >
                  删除
                </button>
              </div>
            </div>
          </div>

          <router-link :to="`/feed/${feed.id}`" class="block">
            <p class="mb-2 line-clamp-2 text-xs leading-relaxed text-text-secondary">
              {{ feed.description || '暂无描述' }}
            </p>
            <div class="flex flex-wrap items-center gap-2 text-xs text-text-secondary">
              <span v-if="feed.isPublic" class="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-[10px] font-semibold text-primary">
                公开
              </span>
              <span v-else class="inline-flex items-center rounded-full bg-surface-container px-2.5 py-0.5 text-[10px] font-semibold text-text-secondary">
                私有
              </span>
              <span>•</span>
              <span>{{ feed.subscriberCount }} 订阅者</span>
              <span>•</span>
              <span>{{ formatDate(feed.createdAt) }}</span>
            </div>
          </router-link>
        </div>
      </div>
    </div>

    <!-- 编辑/新建模态框：MD3 28px 大圆角与 Filled Text Field -->
    <transition name="modal">
      <div
          v-if="showModal"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
          @click.self="closeModal"
      >
        <div class="absolute inset-0 bg-black/45" />
        <div class="relative z-10 flex max-h-[90vh] w-full max-w-lg flex-col overflow-hidden rounded-[28px] bg-surface shadow-2xl">
          <div class="flex items-center justify-between border-b border-outline/10 px-6 py-4">
            <h3 class="text-base font-semibold text-text">
              {{ isEditing ? '编辑订阅' : '新建订阅' }}
            </h3>
            <button
                @click="closeModal"
                class="flex h-8 w-8 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <div class="overflow-y-auto p-6">
            <form @submit.prevent="handleSubmit" class="space-y-4">
              <!-- MD3 Filled Text Field: 名称 -->
              <div class="rounded-t-2xl bg-surface-container/60 px-4 pt-2.5 pb-2 border-b-2 border-outline/40 transition focus-within:border-primary focus-within:bg-surface-container/80">
                <label class="block text-[11px] font-medium text-text-secondary">名称</label>
                <input
                    v-model.trim="form.name"
                    type="text"
                    required
                    class="mt-0.5 block w-full border-0 bg-transparent p-0 text-xs text-text outline-none ring-0 placeholder:text-text-secondary/60 sm:text-sm"
                    placeholder="例如：科技精选"
                />
              </div>

              <!-- MD3 Filled Text Field: 描述 -->
              <div class="rounded-t-2xl bg-surface-container/60 px-4 pt-2.5 pb-2 border-b-2 border-outline/40 transition focus-within:border-primary focus-within:bg-surface-container/80">
                <label class="block text-[11px] font-medium text-text-secondary">描述</label>
                <textarea
                    v-model.trim="form.description"
                    rows="3"
                    class="mt-1 block min-h-16 w-full resize-y border-0 bg-transparent p-0 text-xs leading-relaxed text-text outline-none ring-0 placeholder:text-text-secondary/60"
                    placeholder="描述这个订阅的内容..."
                ></textarea>
              </div>

              <!-- 公开 Switch / Checkbox -->
              <label class="flex cursor-pointer items-center justify-between rounded-2xl bg-surface-container/40 p-3.5 transition hover:bg-surface-container/70 select-none">
                <div>
                  <div class="text-xs font-semibold text-text">公开此订阅</div>
                  <div class="text-[11px] text-text-secondary">允许其他用户浏览和订阅该合集</div>
                </div>
                <input
                    id="isPublic"
                    v-model="form.isPublic"
                    type="checkbox"
                    class="h-4 w-4 shrink-0 cursor-pointer rounded-sm border-outline text-primary focus:ring-primary/20"
                />
              </label>

              <!-- 选择源列表 -->
              <div>
                <label class="block text-xs font-semibold text-text-secondary mb-2">选择订阅源</label>
                <div class="max-h-48 overflow-y-auto rounded-2xl bg-surface-container/40 p-2.5 space-y-1">
                  <div v-if="!items.length" class="text-xs text-text-secondary text-center py-4">
                    暂无可聚合的订阅源
                  </div>
                  <label
                      v-for="sub in items"
                      :key="sub.feedId"
                      class="flex cursor-pointer items-center gap-2.5 rounded-xl p-2 transition hover:bg-surface select-none"
                      :class="selectedSourceFeedIds.includes(sub.feedId) ? 'bg-surface shadow-2xs' : ''"
                  >
                    <input
                        :id="'feed-' + sub.feedId"
                        type="checkbox"
                        :value="sub.feedId"
                        v-model="selectedSourceFeedIds"
                        class="h-4 w-4 shrink-0 rounded-sm border-outline text-primary focus:ring-primary/20"
                    />
                    <span class="truncate text-xs font-medium text-text flex-1">
                      {{ sub.title || sub.siteUrl || '未命名订阅' }}
                    </span>
                  </label>
                </div>
                <p class="mt-1.5 text-[11px] text-text-secondary">
                  已选择 <strong class="font-semibold text-text">{{ selectedSourceFeedIds.length }}</strong> 个订阅源
                </p>
              </div>

              <!-- 过滤规则 -->
              <div class="pt-2 border-t border-outline/10">
                <h4 class="text-xs font-semibold text-text-secondary mb-2.5">过滤规则</h4>
                <div class="space-y-2.5">
                  <div class="rounded-t-2xl bg-surface-container/60 px-4 pt-2 pb-1.5 border-b-2 border-outline/40 transition focus-within:border-primary">
                    <label class="block text-[11px] font-medium text-text-secondary">包含关键词 (逗号分隔)</label>
                    <input
                        v-model="keywordsInclude"
                        type="text"
                        class="mt-0.5 block w-full border-0 bg-transparent p-0 text-xs text-text outline-none ring-0 placeholder:text-text-secondary/60"
                        placeholder="例如：AI, 机器学习"
                    />
                  </div>

                  <div class="rounded-t-2xl bg-surface-container/60 px-4 pt-2 pb-1.5 border-b-2 border-outline/40 transition focus-within:border-primary">
                    <label class="block text-[11px] font-medium text-text-secondary">排除关键词 (逗号分隔)</label>
                    <input
                        v-model="keywordsExclude"
                        type="text"
                        class="mt-0.5 block w-full border-0 bg-transparent p-0 text-xs text-text outline-none ring-0 placeholder:text-text-secondary/60"
                        placeholder="例如：广告, 推广"
                    />
                  </div>
                </div>
              </div>

              <div v-if="mixFeedsStore.error" class="rounded-xl bg-red-500/10 p-3 text-xs text-red-600 dark:text-red-400">
                {{ mixFeedsStore.error }}
              </div>

              <div class="flex justify-end gap-2.5 pt-3">
                <button
                    type="button"
                    @click="closeModal"
                    class="inline-flex h-9 items-center justify-center rounded-full px-4 text-xs font-semibold text-text-secondary transition hover:bg-surface-container"
                >
                  取消
                </button>
                <button
                    type="submit"
                    :disabled="mixFeedsStore.loading"
                    class="inline-flex h-9 items-center justify-center rounded-full bg-primary px-5 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-[0.98] disabled:opacity-40"
                >
                  {{ mixFeedsStore.loading ? '保存中...' : '保存' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </transition>

    <!-- 删除确认弹窗 -->
    <div
        v-if="feedToDelete"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
        @click.self="feedToDelete = null"
    >
      <div class="absolute inset-0 bg-black/45" />
      <div class="relative z-10 w-full max-w-sm rounded-[28px] bg-surface p-6 shadow-2xl">
        <h3 class="text-base font-bold text-text mb-2">确认删除</h3>
        <p class="text-xs text-text-secondary mb-5 leading-relaxed">
          确定要删除订阅 <strong>{{ feedToDelete.name }}</strong> 吗？此操作无法撤销。
        </p>

        <div v-if="mixFeedsStore.error" class="mb-4 rounded-xl bg-red-500/10 p-3 text-xs text-red-600 dark:text-red-400">
          {{ mixFeedsStore.error }}
        </div>

        <div class="flex gap-2.5 justify-end">
          <button @click="feedToDelete = null" class="inline-flex h-9 items-center justify-center rounded-full px-4 text-xs font-semibold text-text-secondary hover:bg-surface-container transition">
            取消
          </button>
          <button
              @click="handleDelete"
              :disabled="mixFeedsStore.loading"
              class="inline-flex h-9 items-center justify-center rounded-full bg-red-600 px-4 text-xs font-semibold text-white shadow-xs transition hover:bg-red-700 disabled:opacity-40"
          >
            {{ mixFeedsStore.loading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 点击遮罩收起下拉 -->
    <div v-if="activeDropdown" @click="activeDropdown = null" class="fixed inset-0 z-10"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { type MixFeedListResponse, type MixFeedRequest, useMixFeedsStore } from '../../stores/mixFeeds';
import { useSubscriptionsStore } from '../../stores/subscriptions';
import { formatRelativeTime } from '../../utils/datetime';

defineOptions({ name: 'SubscriptionMixFeedManager' });

const mixFeedsStore = useMixFeedsStore();
const subscriptionsStore = useSubscriptionsStore();

const showModal = ref(false);
const isEditing = ref(false);
const editingId = ref<string | null>(null);
const feedToDelete = ref<MixFeedListResponse | null>(null);
const activeDropdown = ref<string | null>(null);
const isRefreshing = ref(false);
const loadError = ref<string | null>(null);
const items = computed(() => subscriptionsStore.items.filter(item => item.type === 'FEED'));

const form = ref<MixFeedRequest>({
  name: '',
  description: '',
  isPublic: false,
  filterConfig: {
    sourceFeeds: {},
    keywords: {
      include: [],
      exclude: []
    }
  }
});

const keywordsInclude = computed({
  get: () => form.value.filterConfig?.keywords?.include?.join(', ') || '',
  set: (val) => {
    if (!form.value.filterConfig) form.value.filterConfig = {};
    if (!form.value.filterConfig.keywords) form.value.filterConfig.keywords = {};
    form.value.filterConfig.keywords.include = val.split(/[,，]/).map(s => s.trim()).filter(Boolean);
  }
});

const keywordsExclude = computed({
  get: () => form.value.filterConfig?.keywords?.exclude?.join(', ') || '',
  set: (val) => {
    if (!form.value.filterConfig) form.value.filterConfig = {};
    if (!form.value.filterConfig.keywords) form.value.filterConfig.keywords = {};
    form.value.filterConfig.keywords.exclude = val.split(/[,，]/).map(s => s.trim()).filter(Boolean);
  }
});

const selectedSourceFeedIds = computed({
  get: () => Object.keys(form.value.filterConfig?.sourceFeeds || {}),
  set: (val: string[]) => {
    if (!form.value.filterConfig) form.value.filterConfig = {};
    const newSourceFeeds: Record<string, string> = {};
    val.forEach(id => {
      const feed = subscriptionsStore.items.find(item => item.feedId === id);
      if (feed) {
        newSourceFeeds[id] = feed.title || feed.siteUrl || '未命名订阅';
      }
    });
    form.value.filterConfig.sourceFeeds = newSourceFeeds;
  }
});

const formatDate = (date: string) => formatRelativeTime(date);

const toggleDropdown = (feedId: string) => {
  activeDropdown.value = activeDropdown.value === feedId ? null : feedId;
};

const refresh = async () => {
  if (isRefreshing.value || mixFeedsStore.loading) return;
  isRefreshing.value = true;
  try {
    loadError.value = null;
    await mixFeedsStore.fetchMyMixFeeds();
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '订阅聚合加载失败，请重试';
  } finally {
    setTimeout(() => isRefreshing.value = false, 500);
  }
};

const openCreateModal = () => {
  isEditing.value = false;
  editingId.value = null;
  form.value = {
    name: '',
    description: '',
    isPublic: false,
    filterConfig: {
      sourceFeeds: {},
      keywords: { include: [], exclude: [] }
    }
  };
  showModal.value = true;
};

const openEditModal = async (feed: MixFeedListResponse) => {
  try {
    const detail = await mixFeedsStore.fetchMixFeedDetail(feed.id);
    isEditing.value = true;
    editingId.value = feed.id;
    form.value = {
      name: detail.name,
      description: detail.description,
      isPublic: detail.isPublic,
      filterConfig: detail.filterConfig || {
        sourceFeeds: {},
        keywords: { include: [], exclude: [] }
      }
    };
    showModal.value = true;
  } catch (e) {
    console.error('Failed to fetch detail', e);
  }
};

const closeModal = () => {
  showModal.value = false;
  mixFeedsStore.error = null;
  activeDropdown.value = null;
};

const handleSubmit = async () => {
  try {
    if (isEditing.value && editingId.value) {
      await mixFeedsStore.updateMixFeed(editingId.value, form.value);
    } else {
      await mixFeedsStore.createMixFeed(form.value);
    }
    closeModal();
  } catch {
    // Handled in store
  }
};

const confirmDelete = (feed: MixFeedListResponse) => {
  mixFeedsStore.error = null;
  feedToDelete.value = feed;
};

const handleDelete = async () => {
  if (!feedToDelete.value) return;
  try {
    await mixFeedsStore.deleteMixFeed(feedToDelete.value.id);
    feedToDelete.value = null;
  } catch (e) {
    console.error('Delete failed:', e);
  }
};

onMounted(() => {
  if (!mixFeedsStore.myMixFeeds.length) {
    void refresh();
  }
  if (!subscriptionsStore.items.length) {
    void subscriptionsStore.fetchSubscriptions();
  }
});
</script>

<style scoped>
.modal-enter-active, .modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from, .modal-leave-to {
  opacity: 0;
}
</style>