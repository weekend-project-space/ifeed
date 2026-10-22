<template>
  <div class="mx-auto max-w-screen-2xl px-2 py-6 pb-24 text-text sm:px-2 lg:px-6">
    <!-- 用户个人头部：MD3 动态表面卡片 -->
    <section v-if="authStore.isAuthenticated" class="rounded-[28px] bg-surface-container/40 p-5 sm:p-7">
      <div class="flex flex-col gap-5 sm:flex-row sm:items-center sm:gap-6">
        <img v-if="userInfo?.avatarUrl" :src="userInfo.avatarUrl" alt="头像"
             class="h-20 w-20 shrink-0 rounded-full border border-outline/20 object-cover sm:h-24 sm:w-24 shadow-xs" />
        <div v-else
             class="flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-surface-container-high text-2xl font-bold text-text sm:h-24 sm:w-24 shadow-xs">
          {{ userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
        </div>

        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-2.5">
            <h1 class="truncate text-2xl font-bold tracking-tight text-text sm:text-3xl">{{ userInfo?.username || '访客' }}</h1>
            <span class="rounded-full bg-primary/10 px-3 py-0.5 text-xs font-semibold text-primary">
              {{ userInfo?.currentPlan || 'Free' }}
            </span>
          </div>

          <p class="mt-1 text-xs text-text-secondary sm:text-sm">
            @{{ userInfo?.username?.toLowerCase() || 'guest' }}
          </p>

          <div class="mt-4 flex flex-wrap gap-x-5 gap-y-2 text-xs text-text-secondary">
            <span><strong class="font-semibold text-text">{{ historyQuery.total.value }}</strong> 条阅读记录</span>
            <span><strong class="font-semibold text-text">{{ likesQuery.total.value }}</strong> 条喜欢</span>
            <span><strong class="font-semibold text-text">{{ folderCount }}</strong> 个收藏夹</span>
          </div>
        </div>

        <!-- 动作操作组：MD3 Tonal Action Pills -->
        <div class="flex flex-wrap items-center gap-2 pt-2 sm:pt-0">
          <router-link to="/feed/channels"
                       class="inline-flex h-9 items-center gap-1.5 rounded-full bg-surface px-4 text-xs font-medium text-text shadow-xs transition hover:bg-surface-container hover:text-text active:scale-[0.98]">
            <svg class="h-4 w-4 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
            <span>管理订阅</span>
          </router-link>

          <button v-if="canInstall" type="button"
                  class="inline-flex h-9 items-center gap-1.5 rounded-full bg-surface px-4 text-xs font-medium text-text shadow-xs transition hover:bg-surface-container active:scale-[0.98]"
                  @click="handleInstallPWA">
            <svg class="h-4 w-4 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            <span>安装应用</span>
          </button>

          <button type="button"
                  class="inline-flex h-9 items-center gap-1.5 rounded-full bg-surface px-4 text-xs font-medium text-text shadow-xs transition hover:bg-surface-container active:scale-[0.98]"
                  @click="handleLogout">
            <svg class="h-4 w-4 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            <span>退出</span>
          </button>
        </div>
      </div>
    </section>

    <!-- 未登录横幅：MD3 柔和 Surface Tint -->
    <section v-else class="rounded-[28px] bg-surface-container/60 p-6 sm:p-8">
      <div class="max-w-2xl">
        <span class="inline-block rounded-full bg-primary/10 px-3 py-1 text-xs font-bold uppercase tracking-wider text-primary">
          iFeed 会员
        </span>
        <h1 class="mt-3 text-2xl font-bold tracking-tight text-text sm:text-3xl">升级你的阅读体验</h1>
        <p class="mt-2 text-sm leading-relaxed text-text-secondary sm:text-base">
          解锁更多订阅、AI 摘要、思维导图和智能推荐。登录后即可选择套餐并完成支付。
        </p>
        <router-link :to="loginLink"
                     class="mt-5 inline-flex h-10 items-center justify-center rounded-full bg-primary px-6 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-[0.98]">
          登录后购买
        </router-link>
      </div>
    </section>

    <!-- 导航切换：MD3 Filter Chips 标签群 -->
    <nav class="mt-6 flex items-center gap-1.5 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden" aria-label="账户与会员">
      <button v-for="tab in tabs" :key="tab.id" type="button"
              class="inline-flex h-9 shrink-0 items-center rounded-full px-4 text-xs font-medium transition select-none"
              :class="activeTab === tab.id
                ? 'bg-primary/15 font-semibold text-primary'
                : 'bg-surface-container/50 text-text-secondary hover:bg-surface-container hover:text-text'"
              :aria-selected="activeTab === tab.id"
              @click="activeTab = tab.id">
        {{ tab.label }}
      </button>
    </nav>

    <main class="pt-6">
      <!-- 总览 Tab -->
      <template v-if="activeTab === 'overview'">
        <!-- 升级提示横幅：MD3 Tonal Container 替代高饱和边框框线 -->
        <button type="button"
                class="mb-8 flex w-full items-center justify-between gap-4 rounded-3xl bg-surface-container/50 p-5 text-left transition hover:bg-surface-container/80 sm:p-6"
                @click="activeTab = 'plans'">
          <div class="min-w-0">
            <span class="block text-xs font-semibold uppercase tracking-wider text-text-secondary">升级套餐</span>
            <span class="mt-0.5 block truncate text-base font-bold text-text sm:text-lg">升级至 Standard 或 Pro 会员</span>
            <span class="mt-1 block text-xs text-text-secondary">解锁思维导图、更多订阅配额和专属智能流</span>
          </div>
          <span class="inline-flex h-9 shrink-0 items-center justify-center rounded-full bg-primary px-5 text-xs font-semibold text-white shadow-xs">
            查看方案
          </span>
        </button>

        <div class="space-y-10">
          <Shelf title="最近阅读" :items="recentHistory" time-field="readAt" :loading="historyPending" :error="historyError" to="/history" />
          <Shelf title="我的喜欢" :items="recentLikes" time-field="likedAt" :loading="likesPending" :error="likesError" to="/likes" />

          <!-- 收藏夹区域 -->
          <section>
            <div class="mb-4 flex items-center justify-between">
              <h2 class="text-base font-bold text-text sm:text-lg">收藏夹</h2>
              <router-link to="/collections" class="text-xs font-medium text-text-secondary hover:text-text">查看全部</router-link>
            </div>
            <div v-if="foldersPending" class="py-6 text-sm text-text-secondary">正在加载收藏夹…</div>
            <div v-else-if="folders.length === 0"
                 class="rounded-2xl border border-dashed border-outline/20 bg-surface-container/20 py-8 text-center text-xs text-text-secondary">
              暂未创建收藏夹
            </div>
            <div v-else class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
              <router-link v-for="folder in folders.slice(0, 8)" :key="folder.folderId"
                           :to="{ path: '/collections', query: { folderId: folder.folderId } }"
                           class="flex min-w-0 items-center gap-3 rounded-2xl bg-surface-container/40 p-3 transition hover:bg-surface-container hover:shadow-xs">
                <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-surface text-text-secondary shadow-xs">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                  </svg>
                </span>
                <span class="min-w-0 flex-1 truncate text-xs font-semibold text-text">{{ folder.name }}</span>
                <svg class="h-4 w-4 shrink-0 text-text-secondary/60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m9 18 6-6-6-6" />
                </svg>
              </router-link>
            </div>
          </section>
        </div>
      </template>

      <!-- 喜欢列表 Tab -->
      <template v-else-if="activeTab === 'likes'">
        <SectionHeader title="我的喜欢" to="/likes" />
        <DatedArticleList v-if="likesItems.length" :items="likesItems" time-field="likedAt" :removing="false" remove-label="取消喜欢" />
        <p v-if="likesPending" class="py-12 text-center text-sm text-text-secondary">正在加载喜欢…</p>
        <p v-else-if="likesError" class="py-12 text-center text-sm text-red-600 dark:text-red-400">{{ likesError }}</p>
        <p v-else-if="!likesItems.length" class="py-12 text-center text-sm text-text-secondary">还没有喜欢的文章</p>
        <InfiniteLoadMore v-if="likesItems.length" :has-next-page="likesQuery.hasNextPage.value"
                          :loading="likesQuery.isFetchingNextPage.value" :fetching="likesQuery.isFetching.value"
                          :failed="likesQuery.isFetchNextPageError.value" :blocked="likesQuery.isError.value"
                          :fetch-more="likesQuery.fetchNextPage" />
      </template>

      <!-- 阅读历史 Tab -->
      <template v-else-if="activeTab === 'history'">
        <SectionHeader title="阅读历史" to="/history" />
        <DatedArticleList v-if="historyItems.length" :items="historyItems" time-field="readAt" :removing="false" remove-label="删除记录" />
        <p v-if="historyPending" class="py-12 text-center text-sm text-text-secondary">正在加载阅读历史…</p>
        <p v-else-if="historyError" class="py-12 text-center text-sm text-red-600 dark:text-red-400">{{ historyError }}</p>
        <p v-else-if="!historyItems.length" class="py-12 text-center text-sm text-text-secondary">还没有阅读记录</p>
        <InfiniteLoadMore v-if="historyItems.length" :has-next-page="historyQuery.hasNextPage.value"
                          :loading="historyQuery.isFetchingNextPage.value" :fetching="historyQuery.isFetching.value"
                          :failed="historyQuery.isFetchNextPageError.value" :blocked="historyQuery.isError.value"
                          :fetch-more="historyQuery.fetchNextPage" />
      </template>

      <!-- 收藏夹 Tab -->
      <template v-else-if="activeTab === 'collections'">
        <SectionHeader title="收藏夹" to="/collections" />
        <div v-if="foldersPending" class="py-12 text-center text-sm text-text-secondary">正在加载收藏夹…</div>
        <div v-else-if="!folders.length" class="py-12 text-center text-sm text-text-secondary">还没有收藏夹</div>
        <div v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <router-link v-for="folder in folders" :key="folder.folderId"
                       :to="{ path: '/collections', query: { folderId: folder.folderId } }"
                       class="flex min-w-0 items-center gap-3 rounded-2xl bg-surface-container/40 p-3.5 transition hover:bg-surface-container hover:shadow-xs">
            <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-surface text-text-secondary shadow-xs">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
              </svg>
            </span>
            <span class="min-w-0 flex-1 truncate text-sm font-semibold text-text">{{ folder.name }}</span>
            <svg class="h-4 w-4 text-text-secondary/60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="m9 18 6-6-6-6" stroke-width="2" />
            </svg>
          </router-link>
        </div>
        <InfiniteLoadMore v-if="folders.length" :has-next-page="foldersQuery.hasNextPage.value"
                          :loading="foldersQuery.isFetchingNextPage.value" :fetching="foldersQuery.isFetching.value"
                          :failed="foldersQuery.isFetchNextPageError.value" :blocked="foldersQuery.isError.value"
                          :fetch-more="foldersQuery.fetchNextPage" />
      </template>

      <!-- 会员方案 Tab -->
      <template v-else>
        <SectionHeader title="会员方案" />
        <div class="mb-6 rounded-2xl bg-surface-container/50 p-4 text-xs text-text-secondary sm:text-sm">
          当前套餐：<strong class="font-semibold text-text">{{ authStore.isAuthenticated ? (userInfo?.currentPlan || 'Free') : '登录后查看' }}</strong>
          <span v-if="!authStore.isAuthenticated" class="ml-2">登录后可以购买并管理会员套餐。</span>
        </div>

        <!-- 方案卡片三列网格 -->
        <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
          <PlanCard name="Free" description="体验基础功能" price="免费" :features="freeFeatures"
                    :current="currentPlan === 'free'" />
          <PlanCard name="Standard" description="适合个人深度阅读" price="¥36/年" :features="standardFeatures"
                    :current="currentPlan === 'standard'" recommended @select="selectPlan('standard')" />
          <PlanCard name="Pro" description="适合专业用户" price="¥68/年" :features="proFeatures"
                    :current="currentPlan === 'pro'" @select="selectPlan('pro')" />
        </div>

        <!-- 功能对比表：MD3 柔和容器嵌入 -->
        <div class="mt-8 overflow-hidden rounded-[24px] bg-surface-container/30 p-5 sm:p-6">
          <h3 class="mb-4 text-center text-sm font-bold tracking-tight text-text sm:text-base">功能详细对比</h3>
          <div class="overflow-x-auto">
            <table class="w-full min-w-[520px] text-xs">
              <thead>
              <tr class="border-b border-outline/10 text-text-secondary">
                <th class="px-3 py-3 text-left font-semibold">功能</th>
                <th class="px-2 py-3 text-center font-semibold">Free</th>
                <th class="px-2 py-3 text-center font-semibold text-text">Standard</th>
                <th class="px-2 py-3 text-center font-semibold text-text">Pro</th>
              </tr>
              </thead>
              <tbody class="divide-y divide-outline/10">
              <tr v-for="row in comparisonData" :key="row.feature" class="transition hover:bg-surface-container/40">
                <td class="px-3 py-3 font-medium text-text">{{ row.feature }}</td>
                <td class="px-2 py-3 text-center text-text-secondary">{{ row.free }}</td>
                <td class="px-2 py-3 text-center font-semibold text-text">{{ row.standard }}</td>
                <td class="px-2 py-3 text-center font-semibold text-text">{{ row.pro }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
    </main>

    <!-- 支付弹窗：MD3 28px 大圆角模态框 -->
    <div v-if="showPaymentModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div class="absolute inset-0 bg-black/45" @click="closePaymentModal" />
      <div class="relative z-10 max-h-[90vh] w-full max-w-md overflow-y-auto rounded-[28px] bg-surface p-6 shadow-2xl sm:p-8">
        <button type="button" aria-label="关闭支付弹窗"
                class="absolute right-5 top-5 flex h-8 w-8 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
                @click="closePaymentModal">✕</button>

        <div class="mb-5 text-center">
          <h3 class="text-xl font-bold tracking-tight text-text">扫码支付</h3>
          <p class="mt-1 text-xs text-text-secondary">{{ selectedPlanName }} · {{ selectedPlanPrice }}</p>
        </div>

        <div class="mb-5 flex justify-center rounded-2xl bg-surface-container/60 p-5">
          <img v-if="selectedPlan" src="/weixin-pay-qrcode.png" alt="支付二维码"
               class="h-60 w-60 object-contain sm:h-72 sm:w-72" @error="handleImageError" />
        </div>

        <div class="rounded-2xl bg-surface-container/50 p-4 text-xs text-text-secondary">
          <p class="mb-2 font-semibold text-text">支付提示</p>
          <ul class="space-y-1.5 leading-relaxed">
            <li v-if="userInfo">付款留言处填写账号：<strong class="text-text">{{ userInfo.username }}</strong></li>
            <li>支付成功后，套餐将在 2 小时内自动激活</li>
            <li>记得使用优惠码 <strong class="text-text">AIR</strong> 享受 85 折优惠</li>
            <li><a class="text-primary hover:underline" href="https://zhidayingxiao.cn/to/06g6yb" target="_blank" rel="noopener noreferrer">联系客服</a></li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter, useRoute } from 'vue-router';
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
const route = useRoute();
const authStore = useAuthStore();
const pwaStore = usePWAStore();
const { user: userInfo } = storeToRefs(authStore);
const { canInstall } = storeToRefs(pwaStore);

const tabDefinitions = [
  { id: 'overview', label: '总览' },
  { id: 'likes', label: '喜欢' },
  { id: 'collections', label: '收藏' },
  { id: 'history', label: '历史' },
  { id: 'plans', label: '会员' },
] as const;

type TabId = typeof tabDefinitions[number]['id'];
const tabs = computed(() => authStore.isAuthenticated
    ? tabDefinitions
    : tabDefinitions.filter(tab => tab.id === 'plans'));

const requestedTab = computed<TabId | null>(() => {
  const tab = route.query.tab;
  if (typeof tab !== 'string') return null;
  return tabDefinitions.some(item => item.id === tab) ? tab as TabId : null;
});

const activeTab = ref<TabId>(authStore.isAuthenticated ? (requestedTab.value || 'overview') : 'plans');
const selectedPlan = ref<string | null>(null);
const showPaymentModal = ref(false);

const requestedPlan = computed<'standard' | 'pro' | null>(() => {
  const plan = route.query.plan;
  return plan === 'standard' || plan === 'pro' ? plan : null;
});

const loginLink = computed(() => ({
  name: 'auth',
  query: {
    redirect: router.resolve({ name: 'upgrade', query: { tab: 'plans' } }).fullPath,
  },
}));

const historyQuery = useHistoryQuery();
const likesQuery = useLikesQuery();
const foldersQuery = useCollectionFoldersQuery();
const historyItems = computed(() => historyQuery.items.value as HistoryEntryDto[]);
const likesItems = computed(() => likesQuery.items.value as LikeDto[]);
const recentHistory = computed(() => historyItems.value.slice(0, 10));
const recentLikes = computed(() => likesItems.value.slice(0, 10));

const folders = computed<CollectionFolderDto[]>(() => {
  if (!authStore.isAuthenticated) return [];
  return [
    { folderId: 'default', name: '默认收藏', createdAt: '', updatedAt: '' },
    ...(foldersQuery.data.value?.pages.flatMap(page => page.content) ?? []),
  ];
});

const folderCount = computed(() => {
  if (!authStore.isAuthenticated) return 0;
  return (foldersQuery.data.value?.pages[0]?.totalElements ?? 0) + 1;
});

const historyPending = computed(() => historyQuery.isPending.value);
const likesPending = computed(() => likesQuery.isPending.value);
const foldersPending = computed(() => foldersQuery.isPending.value);
const historyError = computed(() => historyQuery.errorMessage.value);
const likesError = computed(() => likesQuery.errorMessage.value);

const currentPlan = computed(() => authStore.isAuthenticated
    ? (userInfo.value?.currentPlan || 'Free').toLowerCase()
    : '');

const freeFeatures = ['订阅 60 个 RSS 源', '每天 AI 处理 3 篇文章', '基础摘要功能', '内容分类和标签', '关键词搜索'];
const standardFeatures = ['订阅 300 个 RSS 源', '每天 AI 处理 15 篇文章', '创建 2 个追踪', '智能摘要和关键信息提取', '内容分类和标签管理', '文章思维导图', '语义搜索'];
const proFeatures = ['订阅 1200 个 RSS 源', '每天 AI 处理 60 篇文章', '创建 8 个追踪', '智能推荐（多路召回+重排）', '文章思维导图', '语义搜索', '优先客户支持'];
const comparisonData = [
  { feature: 'RSS 订阅源数量', free: '60 个', standard: '5x', pro: '20x' },
  { feature: 'AI 处理文章数/天', free: '3 篇', standard: '5x', pro: '20x' },
  { feature: '创建追踪', free: '—', standard: '2 个', pro: '8 个' },
  { feature: '智能摘要', free: '基础', standard: '✓', pro: '✓' },
  { feature: '内容分类和标签', free: '✓', standard: '✓', pro: '✓' },
  { feature: '文章思维导图', free: '—', standard: '✓', pro: '✓' },
  { feature: '搜索', free: '关键词', standard: '关键词 + 语义', pro: '关键词 + 语义' },
  { feature: '智能推荐', free: '基础', standard: '基础+自定义查询词', pro: '基础+自定义查询词+超级过滤' },
];

const selectedPlanName = computed(() => selectedPlan.value === 'standard' ? 'Standard 套餐' : 'Pro 套餐');
const selectedPlanPrice = computed(() => selectedPlan.value === 'standard' ? '¥36/年' : '¥68/年');

const selectPlan = async (plan: 'standard' | 'pro') => {
  if (!authStore.isAuthenticated) {
    const redirect = router.resolve({ name: 'upgrade', query: { tab: 'plans', plan } }).fullPath;
    await router.push({ name: 'auth', query: { redirect } });
    return;
  }
  if (!userInfo.value) return;
  selectedPlan.value = plan;
  showPaymentModal.value = true;
};

const closePaymentModal = () => { showPaymentModal.value = false; };
const handleImageError = () => console.warn('Failed to load QR code image');
const handleLogout = async () => { await authStore.logout(); await router.replace({ name: 'auth' }); };
const handleInstallPWA = async () => { await pwaStore.installPWA(); };

watch(() => authStore.isAuthenticated, (isAuthenticated) => {
  if (!isAuthenticated) {
    activeTab.value = 'plans';
    selectedPlan.value = null;
    showPaymentModal.value = false;
  } else if (requestedTab.value) {
    activeTab.value = requestedTab.value;
  } else if (activeTab.value === 'plans' && !requestedPlan.value) {
    activeTab.value = 'overview';
  }
});

watch(requestedTab, (tab) => {
  if (authStore.isAuthenticated && tab) activeTab.value = tab;
});

watch([() => authStore.isAuthenticated, () => userInfo.value, requestedPlan],
    ([isAuthenticated, user, plan]) => {
      if (isAuthenticated && user && plan) {
        activeTab.value = 'plans';
        selectedPlan.value = plan;
        showPaymentModal.value = true;
      }
    },
    { immediate: true });
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
      const shelf = shelfRef.value;
      if (!shelf) return;
      shelf.scrollBy({ left: direction * Math.max(240, shelf.clientWidth * 0.75), behavior: 'smooth' });
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

    return () => h('section', { class: 'space-y-3.5' }, [
      h('div', { class: 'flex items-center justify-between' }, [
        h('h2', { class: 'text-base font-bold text-text sm:text-lg' }, props.title),
        h('div', { class: 'flex items-center gap-1.5' }, [
          h(RouterLink, { to: props.to, class: 'mr-2 text-xs font-medium text-text-secondary hover:text-text' }, () => '查看全部'),
          h('button', {
            type: 'button', title: '向左滚动', 'aria-label': '向左滚动', disabled: !canScrollLeft.value,
            class: 'flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-surface-container/60 transition hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-30',
            onClick: () => scrollShelf(-1),
          }, [h('svg', { class: 'h-4 w-4 text-text', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
            h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M15 19l-7-7 7-7' }),
          ])]),
          h('button', {
            type: 'button', title: '向右滚动', 'aria-label': '向右滚动', disabled: !canScrollRight.value,
            class: 'flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-surface-container/60 transition hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-30',
            onClick: () => scrollShelf(1),
          }, [h('svg', { class: 'h-4 w-4 text-text', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
            h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9 5l7 7-7 7' }),
          ])]),
        ]),
      ]),
      props.loading
          ? h('p', { class: 'py-8 text-xs text-text-secondary' }, '正在加载…')
          : props.error
              ? h('p', { class: 'py-8 text-xs text-red-600 dark:text-red-400' }, props.error)
              : h('div', { ref: shelfRef, class: 'flex gap-3.5 overflow-x-auto pb-2 [scrollbar-width:none] scroll-smooth [&::-webkit-scrollbar]:hidden' }, props.items.slice(0, 10).map(item => {
                const timestamp = item[props.timeField];
                return h(RouterLink, {
                  key: item.articleId,
                  to: `/articles/${item.articleId}`,
                  class: 'group flex w-48 shrink-0 flex-col sm:w-56',
                }, () => [
                  h('div', { class: 'relative mb-2 aspect-video w-full overflow-hidden rounded-2xl bg-surface-container/60' }, [
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
    SectionHeader: defineComponent({
      props: { title: String, to: String },
      setup(props) {
        return () => h('div', { class: 'mb-4 flex items-center justify-between' }, [
          h('h2', { class: 'text-base font-bold text-text sm:text-lg' }, props.title),
          props.to ? h(RouterLink, { to: props.to, class: 'text-xs font-medium text-text-secondary hover:text-text' }, () => '打开完整列表') : null,
        ]);
      },
    }),
    PlanCard: defineComponent({
      props: { name: String, description: String, price: String, features: Array, current: Boolean, recommended: Boolean },
      emits: ['select'],
      setup(props, { emit }) {
        return () => h('article', {
          class: [
            'relative flex flex-col justify-between rounded-[24px] p-6 transition',
            props.current
                ? 'bg-surface-container-high/60 shadow-xs'
                : props.recommended
                    ? 'bg-surface-container/80 shadow-xs'
                    : 'bg-surface-container/40',
          ],
        }, [
          props.recommended
              ? h('span', { class: 'absolute right-5 top-5 rounded-full bg-primary/10 px-2.5 py-0.5 text-[11px] font-bold text-primary' }, '推荐')
              : null,
          h('div', [
            h('h3', { class: 'text-base font-bold text-text' }, props.name),
            h('p', { class: 'mt-1 text-xs text-text-secondary' }, props.description),
            h('p', { class: 'my-5 text-2xl font-bold tracking-tight text-text sm:text-3xl' }, props.price),
            h('ul', { class: 'space-y-2.5 text-xs text-text-secondary' }, (props.features ?? []).map(feature => h('li', { class: 'flex items-center gap-2' }, [
              h('span', { class: 'text-xs font-bold text-primary' }, '✓'),
              h('span', { class: 'text-text' }, feature as string),
            ]))),
          ]),
          props.current
              ? h('button', { disabled: true, class: 'mt-6 inline-flex h-10 w-full items-center justify-center rounded-full bg-surface-container-high text-xs font-semibold text-text-secondary cursor-not-allowed' }, '当前方案')
              : h('button', {
                class: [
                  'mt-6 inline-flex h-10 w-full items-center justify-center rounded-full text-xs font-semibold transition active:scale-[0.98]',
                  props.recommended
                      ? 'bg-primary text-white shadow-xs hover:brightness-105'
                      : 'bg-surface text-text shadow-xs hover:bg-surface-container',
                ],
                onClick: () => emit('select'),
              }, `选择 ${props.name}`),
        ]);
      },
    }),
  },
});
</script>