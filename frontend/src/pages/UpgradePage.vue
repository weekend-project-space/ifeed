<template>
  <div class="mx-auto px-4 py-6 pb-24 text-text sm:px-6 lg:px-8">
    <section class="border-b border-outline/10 pb-6">
      <div class="flex items-start gap-4 sm:gap-6">
        <img v-if="userInfo?.avatarUrl" :src="userInfo.avatarUrl" alt="头像"
          class="h-20 w-20 shrink-0 rounded-full border border-outline/10 object-cover sm:h-24 sm:w-24" />
        <div v-else
          class="flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-primary text-3xl font-medium text-primary-foreground sm:h-24 sm:w-24">
          {{ userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}</div>
        <div class="min-w-0 flex-1">
          <h1 class="truncate text-2xl font-bold leading-tight sm:text-3xl">{{ userInfo?.username || '访客' }}</h1>
          <div class="mt-1 flex flex-wrap items-center gap-2 text-xs text-text-secondary sm:text-sm">
            <span>@{{ userInfo?.username?.toLowerCase() || 'guest' }}</span><span aria-hidden="true">·</span>
            <span class="rounded-full bg-surface-container px-2 py-0.5 font-medium text-text">{{ userInfo?.currentPlan
              || 'Free' }}</span>
          </div>
          <div class="mt-4 flex flex-wrap gap-x-5 gap-y-2 text-xs text-text-secondary">
            <span><strong class="text-text">{{ historyQuery.total.value }}</strong> 条阅读记录</span>
            <span><strong class="text-text">{{ likesQuery.total.value }}</strong> 条喜欢</span>
            <span><strong class="text-text">{{ folderCount }}</strong> 个收藏夹</span>
          </div>
        </div>
      </div>
      <div class="mt-5 flex flex-wrap gap-2">
        <router-link to="/feed/channels" class="profile-action"><svg class="h-4 w-4" fill="none" stroke="currentColor"
            viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>管理订阅</router-link>
        <button v-if="canInstall" type="button" class="profile-action" @click="handleInstallPWA"><svg class="h-4 w-4"
            fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>安装应用</button>
        <button type="button" class="profile-action" @click="handleLogout"><svg class="h-4 w-4" fill="none"
            stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>退出登录</button>
      </div>
    </section>

    <nav class="-mb-px flex gap-1 overflow-x-auto border-b border-outline/10 pt-3 scrollbar-none" aria-label="个人中心">
      <button v-for="tab in tabs" :key="tab.id" type="button" class="profile-tab"
        :class="activeTab === tab.id ? 'profile-tab-active' : ''" :aria-selected="activeTab === tab.id"
        @click="activeTab = tab.id">{{ tab.label }}</button>
    </nav>

    <main class="pt-6">
      <template v-if="activeTab === 'overview'">
        <button type="button"
          class="mb-8 flex w-full items-center justify-between gap-4 rounded-xl border border-primary/20 bg-primary/5 p-4 text-left transition hover:border-primary/40 sm:p-5"
          @click="activeTab = 'plans'">
          <span class="min-w-0"><span
              class="mb-1 block text-xs font-bold uppercase tracking-wider text-primary">升级套餐</span><span
              class="block truncate text-sm font-bold sm:text-base">升级至 Standard 或 Pro 会员</span><span
              class="mt-1 block text-xs text-text-secondary">解锁思维导图、更多订阅和 AI 能力</span></span>
          <span
            class="shrink-0 rounded-full bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground">查看方案</span>
        </button>
        <div class="space-y-10">
          <Shelf title="最近阅读" :items="recentHistory" time-field="readAt" :loading="historyPending" :error="historyError"
            to="/history" />
          <Shelf title="我的喜欢" :items="recentLikes" time-field="likedAt" :loading="likesPending" :error="likesError"
            to="/likes" />
          <section>
            <div class="mb-4 flex items-center justify-between">
              <h2 class="text-lg font-bold sm:text-xl">收藏夹</h2><router-link to="/collections"
                class="text-xs font-semibold text-text-secondary hover:text-primary">查看全部</router-link>
            </div>
            <div v-if="foldersPending" class="text-sm text-text-secondary">正在加载收藏夹…</div>
            <div v-else-if="folders.length === 0"
              class="rounded-xl border border-outline/10 bg-surface-container/30 py-8 text-center text-sm text-text-secondary">
              暂未创建收藏夹</div>
            <div v-else class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4"><router-link
                v-for="folder in folders.slice(0, 8)" :key="folder.folderId"
                :to="{ path: '/collections', query: { folderId: folder.folderId } }" class="folder-card"><span
                  class="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary"><svg
                    class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                  </svg></span><span class="min-w-0 flex-1 truncate text-sm font-medium">{{ folder.name }}</span><svg
                  class="h-4 w-4 shrink-0 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m9 18 6-6-6-6" />
                </svg></router-link></div>
          </section>
        </div>
      </template>

      <template v-else-if="activeTab === 'likes'">
        <SectionHeader title="我的喜欢" to="/likes" />
        <DatedArticleList v-if="likesItems.length" :items="likesItems" time-field="likedAt" :removing="false"
          remove-label="取消喜欢" />
        <p v-if="likesPending" class="py-10 text-center text-sm text-text-secondary">正在加载喜欢…</p>
        <p v-else-if="likesError" class="py-10 text-center text-sm text-red-600">{{ likesError }}</p>
        <p v-else-if="!likesItems.length" class="py-10 text-center text-sm text-text-secondary">还没有喜欢的文章</p>
        <InfiniteLoadMore v-if="likesItems.length" :has-next-page="likesQuery.hasNextPage.value"
          :loading="likesQuery.isFetchingNextPage.value" :fetching="likesQuery.isFetching.value"
          :failed="likesQuery.isFetchNextPageError.value" :blocked="likesQuery.isError.value"
          :fetch-more="likesQuery.fetchNextPage" />
      </template>

      <template v-else-if="activeTab === 'history'">
        <SectionHeader title="阅读历史" to="/history" />
        <DatedArticleList v-if="historyItems.length" :items="historyItems" time-field="readAt" :removing="false"
          remove-label="删除记录" />
        <p v-if="historyPending" class="py-10 text-center text-sm text-text-secondary">正在加载阅读历史…</p>
        <p v-else-if="historyError" class="py-10 text-center text-sm text-red-600">{{ historyError }}</p>
        <p v-else-if="!historyItems.length" class="py-10 text-center text-sm text-text-secondary">还没有阅读记录</p>
        <InfiniteLoadMore v-if="historyItems.length" :has-next-page="historyQuery.hasNextPage.value"
          :loading="historyQuery.isFetchingNextPage.value" :fetching="historyQuery.isFetching.value"
          :failed="historyQuery.isFetchNextPageError.value" :blocked="historyQuery.isError.value"
          :fetch-more="historyQuery.fetchNextPage" />
      </template>

      <template v-else-if="activeTab === 'collections'">
        <SectionHeader title="收藏夹" to="/collections" />
        <div v-if="foldersPending" class="py-10 text-center text-sm text-text-secondary">正在加载收藏夹…</div>
        <div v-else-if="!folders.length" class="py-10 text-center text-sm text-text-secondary">还没有收藏夹</div>
        <div v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3"><router-link v-for="folder in folders"
            :key="folder.folderId" :to="{ path: '/collections', query: { folderId: folder.folderId } }"
            class="folder-card"><span
              class="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary"><svg
                class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
              </svg></span><span class="flex-1 text-sm font-medium">{{ folder.name }}</span><svg
              class="h-4 w-4 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m9 18 6-6-6-6" />
            </svg></router-link></div>
        <InfiniteLoadMore v-if="folders.length" :has-next-page="foldersQuery.hasNextPage.value"
          :loading="foldersQuery.isFetchingNextPage.value" :fetching="foldersQuery.isFetching.value"
          :failed="foldersQuery.isFetchNextPageError.value" :blocked="foldersQuery.isError.value"
          :fetch-more="foldersQuery.fetchNextPage" />
      </template>

      <template v-else>
        <SectionHeader title="会员方案" />
        <div class="mb-6 rounded-xl border border-primary/20 bg-primary/5 p-4 text-sm text-text-secondary">当前套餐：<strong
            class="text-text">{{ userInfo?.currentPlan || 'Free' }}</strong></div>
        <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
          <PlanCard name="Free" description="体验基础功能" price="免费" :features="freeFeatures"
            :current="currentPlan === 'free'" />
          <PlanCard name="Standard" description="适合个人深度阅读" price="¥36/年" :features="standardFeatures"
            :current="currentPlan === 'standard'" recommended @select="selectPlan('standard')" />
          <PlanCard name="Pro" description="适合专业用户" price="¥68/年" :features="proFeatures"
            :current="currentPlan === 'pro'" @select="selectPlan('pro')" />
        </div>
        <div class="mt-6 overflow-x-auto rounded-xl border border-outline/10 bg-surface p-4">
          <h3 class="mb-3 text-center font-bold">功能对比</h3>
          <table class="w-full min-w-[520px] text-xs">
            <thead>
              <tr class="border-b border-outline/10">
                <th class="px-3 py-3 text-left text-text-secondary">功能</th>
                <th class="px-2 py-3">Free</th>
                <th class="px-2 py-3">Standard</th>
                <th class="px-2 py-3">Pro</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in comparisonData" :key="row.feature" class="border-b border-outline/10 last:border-0">
                <td class="px-3 py-3">{{ row.feature }}</td>
                <td class="px-2 py-3 text-center text-text-secondary">{{ row.free }}</td>
                <td class="px-2 py-3 text-center">{{ row.standard }}</td>
                <td class="px-2 py-3 text-center">{{ row.pro }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </main>

    <div v-if="showPaymentModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60" @click="closePaymentModal"></div>
      <div
        class="relative z-10 max-h-[90vh] w-full max-w-md overflow-y-auto rounded-2xl bg-surface p-6 shadow-2xl sm:p-8">
        <button type="button" aria-label="关闭支付弹窗" class="absolute right-4 top-4 text-text-secondary hover:text-text"
          @click="closePaymentModal">×</button>
        <div class="mb-5 text-center">
          <h3 class="text-xl font-medium">扫码支付</h3>
          <p class="mt-1 text-sm text-text-secondary">{{ selectedPlanName }} - {{ selectedPlanPrice }}</p>
        </div>
        <div class="mb-5 flex justify-center rounded-xl bg-surface-container p-5"><img v-if="selectedPlan"
            src="/weixin-pay-qrcode.png" alt="支付二维码" class="h-64 w-64 object-contain sm:h-80 sm:w-80"
            @error="handleImageError" /></div>
        <div class="rounded-xl bg-primary/10 p-4 text-xs text-text-secondary">
          <p class="mb-2 font-medium text-text">支付提示</p>
          <ul class="space-y-1">
            <li v-if="userInfo">付款留言处填写账号：<strong class="text-text">{{ userInfo.username }}</strong></li>
            <li>支付成功后，套餐将在 2 小时内自动激活</li>
            <li>记得使用优惠码 <strong class="text-text">AIR</strong> 享受 85 折优惠</li>
            <li><a class="text-primary hover:underline" href="https://zhidayingxiao.cn/to/06g6yb" target="_blank"
                rel="noopener noreferrer">联系客服</a></li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, h, defineComponent } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { usePWAStore } from '@/stores/pwa';
import { useHistoryQuery } from '@/queries/history';
import { useLikesQuery } from '@/queries/likes';
import { useCollectionFoldersQuery } from '@/queries/collectionFolders';
import DatedArticleList from '@/components/DatedArticleList.vue';
import InfiniteLoadMore from '@/components/InfiniteLoadMore.vue';
import type { HistoryEntryDto } from '@/api/history';
import type { LikeDto } from '@/api/likes';
import type { CollectionFolderDto } from '@/api/collectionFolders';

defineOptions({ name: 'UpgradePage' });
const router = useRouter();
const authStore = useAuthStore();
const pwaStore = usePWAStore();
const { user: userInfo } = storeToRefs(authStore);
const { canInstall } = storeToRefs(pwaStore);
const activeTab = ref<'overview' | 'likes' | 'collections' | 'history' | 'plans'>('overview');
const selectedPlan = ref<string | null>(null);
const showPaymentModal = ref(false);
const tabs = [{ id: 'overview', label: '总览' }, { id: 'likes', label: '喜欢' }, { id: 'collections', label: '收藏' }, { id: 'history', label: '历史' }, { id: 'plans', label: '会员' }] as const;

const historyQuery = useHistoryQuery();
const likesQuery = useLikesQuery();
const foldersQuery = useCollectionFoldersQuery();
const historyItems = computed(() => historyQuery.items.value as HistoryEntryDto[]);
const likesItems = computed(() => likesQuery.items.value as LikeDto[]);
const recentHistory = computed(() => historyItems.value.slice(0, 10));
const recentLikes = computed(() => likesItems.value.slice(0, 10));
const folders = computed<CollectionFolderDto[]>(() => [
  { folderId: 'default', name: '默认收藏', createdAt: '', updatedAt: '' },
  ...(foldersQuery.data.value?.pages.flatMap(page => page.content) ?? []),
]);
const folderCount = computed(() => foldersQuery.data.value?.pages[0]?.totalElements
  ? foldersQuery.data.value.pages[0].totalElements + 1 : 1);
const historyPending = computed(() => historyQuery.isPending.value);
const likesPending = computed(() => likesQuery.isPending.value);
const foldersPending = computed(() => foldersQuery.isPending.value);
const historyError = computed(() => historyQuery.errorMessage.value);
const likesError = computed(() => likesQuery.errorMessage.value);
const currentPlan = computed(() => (userInfo.value?.currentPlan || 'Free').toLowerCase());
const freeFeatures = ['订阅 60 个 RSS 源', '每天 AI 处理 3 篇文章', '基础摘要功能', '内容分类和标签', '关键词搜索'];
const standardFeatures = ['订阅 300 个 RSS 源', '每天 AI 处理 30 篇文章', '创建 2 个订阅源', '智能摘要和关键信息提取', '内容分类和标签管理', '文章思维导图', '语义搜索'];
const proFeatures = ['订阅 2000 个 RSS 源', '每天 AI 处理 100 篇文章', '创建 6 个订阅源', '智能推荐（多路召回+重排）', '文章思维导图', '语义搜索', '优先客户支持'];
const comparisonData = [
  { feature: 'RSS 订阅源数量', free: '60 个', standard: '300 个', pro: '2000 个' }, { feature: 'AI 处理文章数/天', free: '3 篇', standard: '30 篇', pro: '100 篇' }, { feature: '创建订阅源', free: '—', standard: '2 个', pro: '6 个' }, { feature: '智能摘要', free: '基础', standard: '✓', pro: '✓' }, { feature: '内容分类和标签', free: '✓', standard: '✓', pro: '✓' }, { feature: '文章思维导图', free: '—', standard: '✓', pro: '✓' }, { feature: '搜索', free: '关键词', standard: '关键词 + 语义', pro: '关键词 + 语义' }, { feature: '智能推荐', free: '基础', standard: '基础', pro: '多路召回+重排' },
];
const selectedPlanName = computed(() => selectedPlan.value === 'standard' ? 'Standard 套餐' : 'Pro 套餐');
const selectedPlanPrice = computed(() => selectedPlan.value === 'standard' ? '¥36/年' : '¥68/年');
const selectPlan = (plan: string) => { selectedPlan.value = plan; showPaymentModal.value = true; };
const closePaymentModal = () => { showPaymentModal.value = false; };
const handleImageError = () => console.warn('Failed to load QR code image');
const handleLogout = async () => { await authStore.logout(); await router.replace({ name: 'auth' }); };
const handleInstallPWA = async () => { await pwaStore.installPWA(); };
</script>

<script lang="ts">
import { defineComponent, h, onBeforeUnmount, onMounted, onUpdated, ref, type PropType } from 'vue';
import { RouterLink } from 'vue-router';
import { formatRelativeTime } from '@/utils/datetime';

type ShelfItem = {
  articleId: string;
  title?: string;
  thumbnail?: string;
  summary?: string;
  feedTitle?: string;
  readAt?: string;
  likedAt?: string;
};

const Shelf = defineComponent({
  props: {
    title: { type: String, required: true },
    items: { type: Array as PropType<ShelfItem[]>, default: () => [] },
    timeField: { type: String as PropType<'readAt' | 'likedAt'>, required: true },
    loading: Boolean,
    error: String,
    to: { type: String, required: true },
  },
  setup(props) {
    const shelfRef = ref<HTMLElement | null>(null);
    const canScrollLeft = ref(false);
    const canScrollRight = ref(false);
    let observedShelf: HTMLElement | null = null;
    const updateScrollButtons = () => {
      const shelf = shelfRef.value;
      if (!shelf) return;
      canScrollLeft.value = shelf.scrollLeft > 1;
      canScrollRight.value = shelf.scrollLeft + shelf.clientWidth < shelf.scrollWidth - 1;
    };
    const scrollShelf = (direction: number) => {
      shelfRef.value?.scrollBy({ left: direction * Math.max(240, shelfRef.value.clientWidth * 0.75), behavior: 'smooth' });
    };
    const bindShelf = () => {
      if (observedShelf === shelfRef.value) return;
      observedShelf?.removeEventListener('scroll', updateScrollButtons);
      observedShelf = shelfRef.value;
      observedShelf?.addEventListener('scroll', updateScrollButtons, { passive: true });
    };
    onMounted(() => {
      bindShelf();
      updateScrollButtons();
      window.addEventListener('resize', updateScrollButtons);
    });
    onUpdated(() => {
      bindShelf();
      updateScrollButtons();
    });
    onBeforeUnmount(() => {
      observedShelf?.removeEventListener('scroll', updateScrollButtons);
      window.removeEventListener('resize', updateScrollButtons);
    });

    return () => h('section', { class: 'space-y-3' }, [
      h('div', { class: 'flex items-center justify-between' }, [
        h('h2', { class: 'text-lg font-bold sm:text-xl' }, props.title),
        h('div', { class: 'flex items-center gap-2' }, [
          h(RouterLink, { to: props.to, class: 'text-xs font-semibold text-text-secondary hover:text-primary' }, () => '查看全部'),
          h('button', {
            type: 'button', title: '向左滚动', 'aria-label': '向左滚动', disabled: !canScrollLeft.value,
            class: 'shelf-arrow', onClick: () => scrollShelf(-1),
          }, '‹'),
          h('button', {
            type: 'button', title: '向右滚动', 'aria-label': '向右滚动', disabled: !canScrollRight.value,
            class: 'shelf-arrow', onClick: () => scrollShelf(1),
          }, '›'),
        ]),
      ]),
      props.loading
        ? h('p', { class: 'py-8 text-sm text-text-secondary' }, '正在加载…')
        : props.error
          ? h('p', { class: 'py-8 text-sm text-red-600' }, props.error)
          : h('div', { ref: shelfRef, class: 'flex gap-4 overflow-x-auto pb-2 scrollbar-none scroll-smooth' }, props.items.slice(0, 10).map(item => {
            const timestamp = item[props.timeField];
            return h(RouterLink, {
              key: item.articleId,
              to: `/articles/${item.articleId}`,
              class: 'group flex w-48 shrink-0 flex-col sm:w-56',
            }, () => [
              h('div', { class: 'relative mb-2.5 aspect-video w-full overflow-hidden rounded-xl border border-outline/10 bg-surface-container' }, [
                item.thumbnail
                  ? h('img', { src: item.thumbnail, alt: item.title || '封面', class: 'h-full w-full object-cover transition-transform duration-300 group-hover:scale-105', loading: 'lazy' })
                  : h('div', { class: 'flex h-full items-center justify-center p-3 text-center' }, [
                    h('span', { class: 'line-clamp-2 text-xs leading-relaxed text-text-secondary' }, item.summary || item.title || '暂无内容缩略'),
                  ]),
              ]),
              h('h3', { class: 'line-clamp-2 text-xs font-semibold leading-snug text-text transition-colors group-hover:text-primary sm:text-sm' }, item.title || '未命名文章'),
              h('p', { class: 'mt-1 flex items-center gap-1.5 truncate text-[11px] text-text-secondary' }, [
                item.feedTitle ? h('span', { class: 'truncate' }, item.feedTitle) : null,
                item.feedTitle && timestamp ? h('span', '•') : null,
                timestamp ? h('span', { class: 'shrink-0' }, formatRelativeTime(timestamp)) : null,
              ]),
            ]);
          })),
    ]);
  },
});

export default defineComponent({
  components: {
    Shelf,
    SectionHeader: defineComponent({ props: { title: String, to: String }, setup(props) { return () => h('div', { class: 'mb-4 flex items-center justify-between' }, [h('h2', { class: 'text-lg font-bold sm:text-xl' }, props.title), props.to ? h('a', { href: props.to, class: 'text-xs font-semibold text-text-secondary hover:text-primary' }, '打开完整列表') : null]); } }),
    PlanCard: defineComponent({ props: { name: String, description: String, price: String, features: Array, current: Boolean, recommended: Boolean }, emits: ['select'], setup(props, { emit }) { return () => h('article', { class: ['relative flex flex-col justify-between rounded-xl border p-5', props.current ? 'border-primary/50 bg-primary/5' : 'border-outline/10 bg-surface'] }, [props.recommended ? h('span', { class: 'absolute right-4 top-3 rounded-full bg-primary px-2 py-0.5 text-[10px] font-bold text-primary-foreground' }, '推荐') : null, h('div', [h('h3', { class: 'text-lg font-bold' }, props.name), h('p', { class: 'mt-1 text-xs text-text-secondary' }, props.description), h('p', { class: 'my-5 text-3xl font-extrabold' }, props.price), h('ul', { class: 'space-y-2 text-xs' }, (props.features ?? []).map(feature => h('li', { class: 'flex gap-2' }, [h('span', { class: 'text-primary' }, '✓'), feature])))]), props.current ? h('button', { disabled: true, class: 'mt-6 rounded-full bg-surface-container py-2.5 text-xs font-semibold text-text-secondary' }, '当前方案') : h('button', { class: 'mt-6 rounded-full bg-surface-container py-2.5 text-xs font-semibold hover:bg-primary hover:text-primary-foreground', onClick: () => emit('select') }, `选择 ${props.name}`)]); } }),
  },
});
</script>

<style scoped>
.profile-action {
  @apply inline-flex items-center gap-1.5 rounded-full bg-surface-container px-3.5 py-1.5 text-xs font-medium transition hover:bg-surface-container/70;
}

.profile-tab {
  @apply shrink-0 border-b-2 border-transparent px-4 py-3 text-sm text-text-secondary transition hover:text-text;
}

.profile-tab-active {
  @apply border-primary font-semibold text-primary;
}

.folder-card {
  @apply flex min-w-0 items-center gap-3 rounded-xl border border-outline/10 bg-surface p-3 transition hover:border-primary/40 hover:bg-surface-container/40;
}

.shelf-arrow {
  @apply flex h-8 w-8 items-center justify-center rounded-full border border-outline/20 text-xl leading-none text-text transition hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-30;
}

:global(.scrollbar-none::-webkit-scrollbar) {
  display: none;
  width: 0;
  height: 0;
}

:global(.scrollbar-none) {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
