<template>
  <div class="mx-auto max-w-screen-xl px-4 py-6 pb-24 text-text sm:px-6 lg:px-8">

    <!-- 1. 登录用户的身份与配额状态概览 -->
    <section v-if="authStore.isAuthenticated" class="mb-8 rounded-[32px] bg-surface-container/50 p-6 sm:p-8">
      <div class="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
        <div class="flex items-center gap-5">
          <img v-if="userInfo?.avatarUrl" :src="userInfo.avatarUrl" alt="头像"
               class="h-20 w-20 shrink-0 rounded-full border-2 border-outline/20 object-cover shadow-sm sm:h-24 sm:w-24" />
          <div v-else
               class="flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-primary/15 text-3xl font-bold text-primary sm:h-24 sm:w-24">
            {{ userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <div>
            <div class="flex items-center gap-2.5">
              <h1 class="text-2xl font-bold tracking-tight text-text sm:text-3xl">{{ userInfo?.username || '访客' }}</h1>
              <span class="rounded-full bg-primary/15 px-3 py-1 text-xs font-semibold text-primary">
                {{ userInfo?.currentPlan || 'Free' }} 级别
              </span>
            </div>
            <p class="mt-1 text-xs text-text-secondary sm:text-sm">
              @{{ userInfo?.username?.toLowerCase() || 'guest' }} · 享受无损多源聚合与 AI 深度思考
            </p>

            <div class="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-xs text-text-secondary">
              <span>阅读记录：<strong class="font-semibold text-text">{{ historyQuery.total.value }}</strong></span>
              <span>收藏夹：<strong class="font-semibold text-text">{{ folderCount }}</strong></span>
              <span>喜欢文章：<strong class="font-semibold text-text">{{ likesQuery.total.value }}</strong></span>
            </div>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2.5 pt-2 lg:pt-0">
          <router-link to="/feed/channels"
                       class="inline-flex h-10 items-center gap-2 rounded-full bg-surface px-5 text-xs font-medium text-text shadow-xs transition hover:bg-surface-container active:scale-[0.98]">
            <svg class="h-4 w-4 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>
            <span>管理订阅源</span>
          </router-link>
          <button v-if="canInstall" type="button"
                  class="inline-flex h-10 items-center gap-2 rounded-full bg-surface px-5 text-xs font-medium text-text shadow-xs transition hover:bg-surface-container active:scale-[0.98]"
                  @click="handleInstallPWA">
            <svg class="h-4 w-4 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
            <span>安装桌面应用</span>
          </button>
          <button type="button"
                  class="inline-flex h-10 items-center gap-2 rounded-full bg-surface px-5 text-xs font-medium text-text-secondary transition hover:bg-red-500/10 hover:text-red-600 active:scale-[0.98]"
                  @click="handleLogout">
            <span>退出账号</span>
          </button>
        </div>
      </div>
    </section>

    <!-- 2. 未登录高诱惑 Hero 区 -->
    <section v-else class="mb-10 overflow-hidden rounded-[36px] bg-gradient-to-br from-primary/15 via-surface-container/60 to-surface p-8 sm:p-12 text-center relative shadow-xs">
      <div class="absolute -right-12 -top-12 h-64 w-64 rounded-full bg-primary/10 blur-3xl pointer-events-none" />
      <div class="mx-auto max-w-3xl space-y-5 relative z-10">
        <span class="inline-flex items-center gap-2 rounded-full bg-primary px-4 py-1.5 text-xs font-bold tracking-wide text-white shadow-sm">
          🔥 拒绝无效信息泛滥 · 迈入 10 倍速知识吸收时代
        </span>
        <h1 class="text-3xl font-extrabold tracking-tight text-text sm:text-5xl leading-tight">
          每天面对 1000+ 条未读资讯？<br class="hidden sm:inline"/>
          <span class="text-primary underline decoration-primary/30">5 分钟</span> 让你掌握全网核心脉络
        </h1>
        <p class="text-sm leading-relaxed text-text-secondary sm:text-base max-w-2xl mx-auto">
          免费版只能疲于奔命地刷标题？升级 <strong class="text-text font-semibold">Plus</strong>，让 AI 帮你自动过滤营销水贴、一键生成思维导图，甚至直接将全网优质资讯作为你专属 AI Agent 的搜索燃料。
        </p>
        <div class="pt-2 flex flex-wrap items-center justify-center gap-4">
          <router-link :to="loginLink"
                       class="inline-flex h-12 items-center justify-center rounded-full bg-primary px-8 text-sm font-semibold text-white shadow-lg transition hover:brightness-105 active:scale-[0.98]">
            立即登录解锁超级特权
          </router-link>
          <span class="text-xs text-text-secondary">已有 85%+ 的活跃极客选择了 Plus 方案</span>
        </div>
      </div>
    </section>

    <!-- 导航切换 Tab -->
    <nav class="mb-8 flex items-center justify-center gap-2 overflow-x-auto pb-2 [scrollbar-width:none]" aria-label="账户与会员">
      <button v-for="tab in tabs" :key="tab.id" type="button"
              class="inline-flex h-10 shrink-0 items-center rounded-full px-5 text-xs font-semibold transition select-none active:scale-95"
              :class="activeTab === tab.id
                ? 'bg-primary text-white shadow-xs'
                : 'bg-surface-container/70 text-text-secondary hover:bg-surface-container hover:text-text'"
              :aria-selected="activeTab === tab.id"
              @click="activeTab = tab.id">
        {{ tab.label }}
      </button>
    </nav>

    <main>
      <!-- 总览 Tab -->
      <template v-if="activeTab === 'overview'">
        <button type="button"
                class="mb-10 flex w-full items-center justify-between gap-4 rounded-[28px] bg-primary/8 p-6 text-left transition hover:bg-primary/12 sm:p-7"
                @click="activeTab = 'plans'">
          <div class="min-w-0">
            <span class="mb-1 block text-xs font-bold uppercase tracking-wider text-primary">会员特权升级</span>
            <span class="block truncate text-base font-bold text-text sm:text-xl">解锁 Plus 或 Pro 会员，获得完整订阅过滤与 AI 工具矩阵</span>
            <span class="mt-1 block text-xs text-text-secondary">高级订阅源管理、多源自动化热点追踪、AI 思维导图与 Agent 搜索工具</span>
          </div>
          <span class="inline-flex h-10 shrink-0 items-center justify-center rounded-full bg-primary px-6 text-xs font-semibold text-white shadow-xs">
            查看定价方案
          </span>
        </button>

        <div class="space-y-12">
          <Shelf title="最近阅读" :items="recentHistory" time-field="readAt" :loading="historyPending" :error="historyError" to="/history" />
          <Shelf title="我的喜欢" :items="recentLikes" time-field="likedAt" :loading="likesPending" :error="likesError" to="/likes" />

          <!-- 收藏夹快捷网格 -->
          <section>
            <div class="mb-4 flex items-center justify-between">
              <h2 class="text-base font-bold text-text sm:text-lg">收藏夹</h2>
              <router-link to="/collections" class="text-xs font-medium text-text-secondary hover:text-text">查看全部</router-link>
            </div>
            <div v-if="foldersPending" class="py-6 text-sm text-text-secondary">正在加载收藏夹…</div>
            <div v-else-if="folders.length === 0"
                 class="rounded-3xl border border-dashed border-outline/20 bg-surface-container/20 py-10 text-center text-xs text-text-secondary">
              暂未创建收藏夹
            </div>
            <div v-else class="grid grid-cols-2 gap-3.5 sm:grid-cols-3 lg:grid-cols-4">
              <router-link v-for="folder in folders.slice(0, 8)" :key="folder.folderId"
                           :to="{ path: '/collections', query: { folderId: folder.folderId } }"
                           class="flex min-w-0 items-center gap-3 rounded-2xl bg-surface-container/40 p-3.5 transition hover:bg-surface-container hover:shadow-xs">
                <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-surface text-text shadow-2xs">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"/></svg>
                </span>
                <span class="min-w-0 flex-1 truncate text-xs font-semibold text-text">{{ folder.name }}</span>
                <svg class="h-4 w-4 shrink-0 text-text-secondary/60" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m9 18 6-6-6-6"/></svg>
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
        <div v-else class="grid grid-cols-1 gap-3.5 sm:grid-cols-2 lg:grid-cols-3">
          <router-link v-for="folder in folders" :key="folder.folderId"
                       :to="{ path: '/collections', query: { folderId: folder.folderId } }"
                       class="flex min-w-0 items-center gap-3.5 rounded-2xl bg-surface-container/40 p-4 transition hover:bg-surface-container hover:shadow-xs">
            <span class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-surface text-text shadow-2xs">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"/></svg>
            </span>
            <span class="min-w-0 flex-1 truncate text-sm font-bold text-text">{{ folder.name }}</span>
            <svg class="h-4 w-4 text-text-secondary/60" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m9 18 6-6-6-6"/></svg>
          </router-link>
        </div>
        <InfiniteLoadMore v-if="folders.length" :has-next-page="foldersQuery.hasNextPage.value"
                          :loading="foldersQuery.isFetchingNextPage.value" :fetching="foldersQuery.isFetching.value"
                          :failed="foldersQuery.isFetchNextPageError.value" :blocked="foldersQuery.isError.value"
                          :fetch-more="foldersQuery.fetchNextPage" />
      </template>

      <!-- 会员方案 Tab -->
      <template v-else>
        <div class="mx-auto max-w-5xl">
          <div class="mb-10 text-center">
            <h2 class="text-2xl font-bold tracking-tight text-text sm:text-3xl">选择最适合你的高效阅读生产力方案</h2>
            <p class="mt-2 text-xs text-text-secondary sm:text-sm">使用优惠码 <strong class="text-primary font-semibold">AIR</strong> 立即享受首年 <span class="text-primary font-bold">85 折</span> 专属优惠</p>
          </div>

          <!-- 方案卡片三列网格 -->
          <div class="grid grid-cols-1 gap-6 md:grid-cols-3 items-stretch">
            <PlanCard name="Free" description="基础轻量体验" price="免费" :features="freeFeatures"
                      :current="currentPlan === 'free'" />
            <PlanCard name="Plus" description="个人深度阅读与源管理首选" price="¥36/年" original-price="¥42" equivalent="折合 ¥0.09/天" :features="plusFeatures"
                      :current="currentPlan === 'plus'" recommended @select="selectPlan('plus')" />
            <PlanCard name="Pro" description="全功能生产力与 AI Agent 接入" price="¥68/年" original-price="¥80" equivalent="折合 ¥0.18/天" :features="proFeatures"
                      :current="currentPlan === 'pro'" @select="selectPlan('pro')" />
          </div>

          <!-- 4大核心权益可视化特点看板 -->
          <div class="mt-16 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            <div class="rounded-[28px] bg-surface-container/30 p-6 space-y-3 transition hover:bg-surface-container/60">
              <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/15 text-primary text-lg">📥</div>
              <h3 class="text-base font-bold text-text">无损高额订阅</h3>
              <p class="text-xs leading-relaxed text-text-secondary">海量 RSS 订阅源统一纳管，打破信息茧房，源头资讯一手直达。</p>
            </div>
            <div class="rounded-[28px] bg-surface-container/30 p-6 space-y-3 transition hover:bg-surface-container/60">
              <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/15 text-primary text-lg">🎯</div>
              <h3 class="text-base font-bold text-text">多源自动化热点追踪</h3>
              <p class="text-xs leading-relaxed text-text-secondary">Plus 提供 2 个、Pro 提供 8 个自动化热点追踪，全天候过滤噪音直达干货。</p>
            </div>
            <div class="rounded-[28px] bg-surface-container/30 p-6 space-y-3 transition hover:bg-surface-container/60">
              <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/15 text-primary text-lg">🗺️</div>
              <h3 class="text-base font-bold text-text">AI 结构化思维导图</h3>
              <p class="text-xs leading-relaxed text-text-secondary">万字长文一键生成逻辑导图，脉络层级井然有序，直击文章核心。</p>
            </div>
            <div class="rounded-[28px] bg-surface-container/30 p-6 space-y-3 transition hover:bg-surface-container/60">
              <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/15 text-primary text-lg">🔍</div>
              <h3 class="text-base font-bold text-text">AI 搜索工具 (Agent API)</h3>
              <p class="text-xs leading-relaxed text-text-secondary">两档均支持为外部智能体（Agent）提供高效的语义搜索支持。</p>
            </div>
          </div>

          <!-- 功能对比表格 -->
          <div class="mt-12 overflow-hidden rounded-[28px] bg-surface-container/20 p-6 sm:p-8">
            <h3 class="mb-6 text-center text-sm font-bold tracking-tight text-text sm:text-base">核心功能权益详细对比</h3>
            <div class="overflow-x-auto">
              <table class="w-full min-w-[560px] text-xs">
                <thead>
                <tr class="border-b border-outline/15 text-text-secondary">
                  <th class="px-4 py-3.5 text-left font-semibold">功能权益</th>
                  <th class="px-3 py-3.5 text-center font-semibold">Free</th>
                  <th class="px-3 py-3.5 text-center font-semibold text-primary">Plus</th>
                  <th class="px-3 py-3.5 text-center font-semibold text-text">Pro</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-outline/10">
                <tr v-for="row in comparisonData" :key="row.feature" class="transition hover:bg-surface-container/40">
                  <td class="px-4 py-3.5 font-medium text-text">{{ row.feature }}</td>
                  <td class="px-3 py-3.5 text-center text-text-secondary">{{ row.free }}</td>
                  <td class="px-3 py-3.5 text-center font-bold text-primary">{{ row.plus }}</td>
                  <td class="px-3 py-3.5 text-center font-semibold text-text">{{ row.pro }}</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>
    </main>

    <!-- 微信支付模态框 -->
    <div v-if="showPaymentModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
         @click.self="closePaymentModal">
      <div class="absolute inset-0 bg-black/45" />
      <div class="relative z-10 max-h-[90vh] w-full max-w-md overflow-y-auto rounded-[32px] bg-surface p-7 shadow-2xl sm:p-8">
        <button type="button" aria-label="关闭支付弹窗"
                class="absolute right-6 top-6 flex h-8 w-8 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
                @click="closePaymentModal">✕</button>

        <div class="mb-6 text-center">
          <span class="inline-block rounded-full bg-primary/15 px-3 py-0.5 text-[10px] font-bold text-primary mb-2">已应用优惠码 AIR (85折)</span>
          <h3 class="text-xl font-bold tracking-tight text-text">微信扫码安全支付</h3>
          <p class="mt-1 text-xs text-text-secondary">{{ selectedPlanName }} ·
            <span class="line-through opacity-70">{{ selectedPlanPrice }}</span>
            <strong class="text-primary font-bold text-sm ml-1">实付 {{ discountedPrice }}</strong>
          </p>
        </div>

        <div class="mb-6 flex justify-center rounded-2xl bg-surface-container/40 p-5 shadow-2xs">
          <img v-if="selectedPlan" src="/weixin-pay-qrcode.png" alt="支付二维码"
               class="h-56 w-56 object-contain sm:h-64 sm:w-64" @error="handleImageError" />
        </div>

        <div class="rounded-2xl bg-surface-container/40 p-4 text-xs text-text-secondary space-y-2">
          <p class="font-semibold text-text">🛡️ 购买保障与提示</p>
          <ul class="space-y-1.5 leading-relaxed">
            <li v-if="userInfo">付款备注填写账号：<strong class="text-text font-mono">{{ userInfo.username }}</strong></li>
            <li>支付成功后系统将在 <strong class="text-text">2 小时内</strong> 自动开通权益</li>
            <li>如需企业定制发票或人工支持，请点击 <a class="text-primary hover:underline font-semibold" href="https://zhidayingxiao.cn/to/06g6yb" target="_blank" rel="noopener noreferrer">联系专属客服</a></li>
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
  { id: 'collections', label: '收藏夹' },
  { id: 'history', label: '阅读历史' },
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

const requestedPlan = computed<'plus' | 'pro' | null>(() => {
  const plan = route.query.plan;
  return plan === 'plus' || plan === 'pro' ? plan : null;
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

const freeFeatures = ['订阅 60 个 RSS 源', '每天 AI 处理 3 篇文章', '基础摘要功能', '关键词搜索'];
const plusFeatures = ['订阅 300 个 RSS 源', '2 个多源自动化热点追踪', '每天 AI 处理 15 篇文章', 'AI 结构化思维导图', '给 AI 使用的搜索工具 (Agent API)'];
const proFeatures = ['订阅 1200 个 RSS 源', '8 个多源自动化热点追踪', '每天 AI 处理 60 篇文章', 'AI 思维导图与长文深度摘要', '给 AI 使用的搜索工具 (Agent API)', '优先客户响应支持'];

const comparisonData = [
  { feature: 'RSS 订阅源上限', free: '60 个', plus: '300 个', pro: '1,200 个' },
  { feature: '多源自动化热点追踪', free: '—', plus: '2 个', pro: '8 个' },
  { feature: 'AI 深度摘要/天', free: '3 篇', plus: '15 篇', pro: '60 篇' },
  { feature: '文章思维导图', free: '—', plus: '✓', pro: '✓' },
  { feature: 'AI 搜索工具 (Agent API)', free: '—', plus: '✓', pro: '✓' },
  { feature: '优先客户响应支持', free: '—', plus: '—', pro: '✓' },
];

const selectedPlanName = computed(() => selectedPlan.value === 'plus' ? 'Plus 个人版' : 'Pro 专业版');
const selectedPlanPrice = computed(() => selectedPlan.value === 'plus' ? '¥36/年' : '¥68/年');
const discountedPrice = computed(() => selectedPlan.value === 'plus' ? '¥30.6 / 年' : '¥57.8 / 年');

const selectPlan = async (plan: 'plus' | 'pro') => {
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
          props.to ? h(RouterLink, { to: props.to, class: 'text-xs font-medium text-text-secondary hover:text-text' }, () => '查看全部') : null,
        ]);
      },
    }),
    PlanCard: defineComponent({
      props: { name: String, description: String, price: String, originalPrice: String, equivalent: String, features: Array, current: Boolean, recommended: Boolean },
      emits: ['select'],
      setup(props, { emit }) {
        return () => h('article', {
          class: [
            'relative flex flex-col justify-between rounded-[32px] p-7 transition shadow-xs',
            props.recommended
                ? 'bg-surface border-2 border-primary shadow-xl scale-[1.02] z-10'
                : 'bg-surface-container/30 border border-outline/10',
          ],
        }, [
          props.recommended
              ? h('span', { class: 'absolute -top-3.5 left-1/2 -translate-x-1/2 rounded-full bg-primary px-4 py-1 text-[11px] font-bold text-white shadow-sm' }, '🔥 极客首选·全网最高性价比')
              : null,
          h('div', [
            h('div', { class: 'flex items-center justify-between mb-2' }, [
              h('h3', { class: 'text-lg font-extrabold text-text' }, props.name),
              props.equivalent ? h('span', { class: 'rounded-full bg-primary/10 px-2.5 py-0.5 text-[10px] font-bold text-primary' }, props.equivalent) : null,
            ]),
            h('p', { class: 'text-xs text-text-secondary mb-5' }, props.description),
            h('div', { class: 'flex items-baseline gap-2 mb-6' }, [
              h('span', { class: 'text-3xl font-extrabold tracking-tight text-text' }, props.price),
              props.originalPrice ? h('span', { class: 'text-sm text-text-secondary line-through opacity-75' }, props.originalPrice) : null,
            ]),
            h('ul', { class: 'space-y-3 text-xs text-text-secondary mb-2' }, (props.features ?? []).map(feature => h('li', { class: 'flex items-start gap-2.5' }, [
              h('span', { class: 'text-xs font-bold text-primary mt-0.5' }, '✓'),
              h('span', { class: 'text-text leading-relaxed' }, feature as string),
            ]))),
          ]),
          props.current
              ? h('button', { disabled: true, class: 'mt-8 inline-flex h-11 w-full items-center justify-center rounded-full bg-surface-container text-xs font-semibold text-text-secondary cursor-not-allowed' }, '当前已激活方案')
              : h('button', {
                class: [
                  'mt-8 inline-flex h-11 w-full items-center justify-center rounded-full text-xs font-semibold transition active:scale-[0.98]',
                  props.recommended
                      ? 'bg-primary text-white shadow-md hover:brightness-105'
                      : 'bg-surface text-text border border-outline/20 hover:bg-surface-container',
                ],
                onClick: () => emit('select'),
              }, `立即开启 ${props.name}`),
        ]);
      },
    }),
  },
});
</script>