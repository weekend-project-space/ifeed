<template>
  <div>
    <!-- Header Actions -->
    <div class="mb-6 flex justify-end gap-2">
      <button
          @click="refresh"
          :disabled="mixFeedsStore.loading || isRefreshing"
          class="p-2 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors disabled:opacity-50"
          aria-label="刷新"
      >
        <svg
            class="w-5 h-5 mx-auto"
            :class="{ 'animate-spin': mixFeedsStore.loading || isRefreshing }"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
        >
          <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
        </svg>
      </button>
      <button
          @click="openCreateModal"
          class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-secondary hover:bg-secondary/90 rounded-full transition-colors"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14"/>
        </svg>
        <span>新建聚合订阅</span>
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="mixFeedsStore.loading && !mixFeedsStore.myMixFeeds.length" class="space-y-3">
      <div v-for="i in 3" :key="i" class="flex items-start gap-4 p-4 bg-gray-50 dark:bg-gray-800/50 rounded-lg animate-pulse">
        <div class="w-14 h-14 bg-gray-200 dark:bg-gray-700 rounded-full"></div>
        <div class="flex-1 space-y-2 py-1">
          <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded w-2/3"></div>
          <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/2"></div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!mixFeedsStore.myMixFeeds.length" class="text-center py-16">
      <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center">
        <svg class="w-8 h-8 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
        </svg>
      </div>
      <h2 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">没有聚合订阅</h2>
      <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">创建聚合订阅以聚合多个来源的内容</p>
      <button
          @click="openCreateModal"
          class="inline-flex items-center gap-2 px-5 py-2 text-sm font-medium text-white bg-secondary hover:bg-secondary/90 rounded-full transition-colors"
      >
        新建聚合订阅
      </button>
    </div>

    <!-- List -->
    <div v-else class="space-y-0">
      <div
          v-for="feed in mixFeedsStore.myMixFeeds"
          :key="feed.id"
          class="group flex items-start gap-4 px-4 py-4 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
      >
        <!-- Icon/Avatar Placeholder -->
        <div class="flex-shrink-0 w-10 h-10 rounded-full bg-secondary/10 dark:bg-secondary/20 flex items-center justify-center text-secondary">
          <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
          </svg>
        </div>

        <div class="flex-1 min-w-0">
          <div class="flex items-start justify-between gap-3 mb-1.5">
            <router-link :to="`/feeds/${feed.id}`" class="flex-1 min-w-0">
              <h3 class="text-base font-medium text-gray-900 dark:text-gray-100 line-clamp-1 group-hover:text-secondary transition-colors">
                {{ feed.name }}
              </h3>
            </router-link>

            <!-- Action Buttons -->
            <div class="flex items-center gap-1.5 flex-shrink-0">
              <button
                  class="relative p-1"
                  @click.stop="toggleDropdown(feed.id)"
                  :aria-expanded="activeDropdown === feed.id"
              >
                <svg class="w-5 h-5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="1" fill="currentColor"/>
                  <circle cx="12" cy="5" r="1" fill="currentColor"/>
                  <circle cx="12" cy="19" r="1" fill="currentColor"/>
                </svg>
                <!-- Dropdown Menu -->
                <div
                    v-if="activeDropdown === feed.id"
                    class="absolute right-0 top-full mt-1 w-36 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-1 z-10"
                >
                  <button
                      @click.stop="openEditModal(feed)"
                      class="w-full px-4 py-2 text-left text-sm text-gray-900 dark:text-gray-100 hover:bg-gray-100 dark:hover:bg-gray-700"
                  >
                    编辑
                  </button>
                  <button
                      @click.stop="confirmDelete(feed)"
                      class="w-full px-4 py-2 text-left text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20"
                  >
                    删除
                  </button>
                </div>
              </button>
            </div>
          </div>

          <router-link :to="`/feeds/${feed.id}`" class="block">
            <div class="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400 mb-1.5">
              <span v-if="feed.isPublic" class="inline-flex items-center gap-1 text-xs font-medium text-secondary bg-secondary/10 dark:bg-secondary/20 px-2 py-0.5 rounded-full">
                公开
              </span>
              <span v-else class="inline-flex items-center gap-1 text-xs font-medium text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded-full">
                私有
              </span>
              <span>•</span>
              <span>{{ feed.subscriberCount }} 订阅者</span>
              <span>•</span>
              <span>{{ formatDate(feed.createdAt) }}</span>
            </div>

            <p class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2 mb-2">
              {{ feed.description || '暂无描述' }}
            </p>
          </router-link>
        </div>
      </div>
    </div>

    <!-- Edit/Create Modal -->
    <transition name="modal">
      <div
          v-if="showModal"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40"
          @click.self="closeModal"
      >
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-lg w-full max-h-[90vh] flex flex-col">
          <div class="flex items-center justify-between p-5 border-b border-gray-200 dark:border-gray-700">
            <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100">
              {{ isEditing ? '编辑聚合订阅' : '新建聚合订阅' }}
            </h3>
            <button
                @click="closeModal"
                class="text-gray-400 hover:text-gray-500 dark:hover:text-gray-300"
            >
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          
          <div class="p-5 overflow-y-auto">
            <form @submit.prevent="handleSubmit" class="space-y-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">名称</label>
                <input
                    v-model.trim="form.name"
                    type="text"
                    required
                    class="w-full px-3 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary/50 focus:border-secondary text-sm text-gray-900 dark:text-gray-100"
                    placeholder="例如：科技精选"
                />
              </div>
              
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">描述</label>
                <textarea
                    v-model.trim="form.description"
                    rows="3"
                    class="w-full px-3 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary/50 focus:border-secondary text-sm text-gray-900 dark:text-gray-100"
                    placeholder="描述这个聚合订阅的内容..."
                ></textarea>
              </div>

              <div class="flex items-center gap-2">
                <input
                    id="isPublic"
                    v-model="form.isPublic"
                    type="checkbox"
                    class="w-4 h-4 text-secondary border-gray-300 dark:border-gray-600 rounded focus:ring-secondary"
                />
                <label for="isPublic" class="text-sm text-gray-700 dark:text-gray-300">公开此订阅（允许他人订阅）</label>
              </div>

              <!-- Source Feeds Selection -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">选择订阅源</label>
                <div class="max-h-48 overflow-y-auto border border-gray-200 dark:border-gray-700 rounded-lg p-3 space-y-2">
                  <div v-if="!subscriptionsStore.items.length" class="text-sm text-gray-500 text-center py-2">
                    暂无订阅源
                  </div>
                  <div
                      v-for="sub in subscriptionsStore.items"
                      :key="sub.feedId"
                      class="flex items-center gap-2"
                  >
                    <input
                        :id="'feed-' + sub.feedId"
                        type="checkbox"
                        :value="sub.feedId"
                        v-model="selectedSourceFeedIds"
                        class="w-4 h-4 text-secondary border-gray-300 dark:border-gray-600 rounded focus:ring-secondary"
                    />
                    <label :for="'feed-' + sub.feedId" class="text-sm text-gray-700 dark:text-gray-300 truncate cursor-pointer select-none flex-1">
                      {{ sub.title || sub.siteUrl || '未命名订阅' }}
                    </label>
                  </div>
                </div>
                <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  已选择 {{ selectedSourceFeedIds.length }} 个订阅源
                </p>
              </div>

              <!-- Filter Config (Simplified for now) -->
               <div class="border-t border-gray-200 dark:border-gray-700 pt-4 mt-4">
                 <h4 class="text-sm font-medium text-gray-900 dark:text-gray-100 mb-3">过滤规则</h4>
                 
                 <div class="space-y-3">
                   <div>
                     <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">包含关键词 (用逗号分隔)</label>
                     <input
                         v-model="keywordsInclude"
                         type="text"
                         class="w-full px-3 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary/50 focus:border-secondary text-sm text-gray-900 dark:text-gray-100"
                         placeholder="例如：AI, 机器学习"
                     />
                   </div>
                   
                   <div>
                     <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">排除关键词 (用逗号分隔)</label>
                     <input
                         v-model="keywordsExclude"
                         type="text"
                         class="w-full px-3 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary/50 focus:border-secondary text-sm text-gray-900 dark:text-gray-100"
                         placeholder="例如：广告, 推广"
                     />
                   </div>
                 </div>
               </div>

              <div v-if="mixFeedsStore.error" class="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 p-3 rounded-lg">
                {{ mixFeedsStore.error }}
              </div>

              <div class="flex justify-end gap-3 pt-2">
                <button
                    type="button"
                    @click="closeModal"
                    class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                >
                  取消
                </button>
                <button
                    type="submit"
                    :disabled="mixFeedsStore.loading"
                    class="px-4 py-2 text-sm font-medium text-white bg-secondary hover:bg-secondary/90 rounded-lg transition-colors disabled:opacity-50"
                >
                  {{ mixFeedsStore.loading ? '保存中...' : '保存' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </transition>

    <!-- Delete Confirmation -->
    <div
        v-if="feedToDelete"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40"
        @click.self="feedToDelete = null"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-sm w-full p-5">
        <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">确认删除</h3>
        <p class="text-sm text-gray-600 dark:text-gray-400 mb-5">
          确定要删除聚合订阅 <strong>{{ feedToDelete.name }}</strong> 吗? 此操作无法撤销。
        </p>
        
        <div v-if="mixFeedsStore.error" class="mb-4 text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 p-3 rounded-lg">
          {{ mixFeedsStore.error }}
        </div>
        
        <div class="flex gap-3 justify-end">
          <button @click="feedToDelete = null" class="px-4 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors">
            取消
          </button>
          <button
              @click="handleDelete"
              :disabled="mixFeedsStore.loading"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg disabled:opacity-50 transition-colors"
          >
            {{ mixFeedsStore.loading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
    <!-- Click outside to close dropdown -->
    <div v-if="activeDropdown" @click="activeDropdown = null" class="fixed inset-0 z-0"></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { storeToRefs } from 'pinia';
import { useMixFeedsStore, type MixFeedListResponse, type MixFeedRequest } from '../../stores/mixFeeds';
import { useSubscriptionsStore } from '../../stores/subscriptions';
import { formatRelativeTime } from '../../utils/datetime';

const mixFeedsStore = useMixFeedsStore();
const subscriptionsStore = useSubscriptionsStore();

const showModal = ref(false);
const isEditing = ref(false);
const editingId = ref<string | null>(null);
const feedToDelete = ref<MixFeedListResponse | null>(null);
const activeDropdown = ref<string | null>(null);
const isRefreshing = ref(false);

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
    
    // Convert array of IDs to Map<ID, Name>
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
    await mixFeedsStore.fetchMyMixFeeds();
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
  } catch (e) {
    // Error handled in store
  }
};

const confirmDelete = (feed: MixFeedListResponse) => {
  mixFeedsStore.error = null; // Clear any previous errors
  feedToDelete.value = feed;
};

const handleDelete = async () => {
  if (!feedToDelete.value) return;
  try {
    await mixFeedsStore.deleteMixFeed(feedToDelete.value.id);
    feedToDelete.value = null;
  } catch (e) {
    // Error is already set in the store, keep dialog open to show error
    console.error('Delete failed:', e);
  }
};

onMounted(() => {
  if (!mixFeedsStore.myMixFeeds.length) {
    mixFeedsStore.fetchMyMixFeeds();
  }
  if (!subscriptionsStore.items.length) {
    subscriptionsStore.fetchSubscriptions();
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
