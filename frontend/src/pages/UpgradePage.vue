<template>
  <div class="mx-auto max-w-screen-xl px-4 py-6 pb-24 text-text sm:px-6 lg:px-8">

    <!-- 1. 登录用户的身份与配额状态概览 -->
    <section v-if="authStore.isAuthenticated" class="mb-8 rounded-[32px] bg-surface-container/50 p-6 sm:p-8">
      <div class="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
        <div class="flex items-center gap-5">
          <img v-if="userInfo?.avatarUrl" :src="userInfo.avatarUrl" :alt="tr('upgradePage.avatar')"
               class="h-20 w-20 shrink-0 rounded-full border-2 border-outline/20 object-cover shadow-sm sm:h-24 sm:w-24" />
          <div v-else
               class="flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-primary/15 text-3xl font-bold text-primary sm:h-24 sm:w-24">
            {{ userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <div>
            <div class="flex items-center gap-2.5">
              <h1 class="text-2xl font-bold tracking-tight text-text sm:text-3xl">{{ userInfo?.username || tr('common.guest') }}</h1>
              <span class="rounded-full bg-primary/15 px-3 py-1 text-xs font-semibold text-primary">
                {{ userInfo?.currentPlan || 'Free' }} {{ tr('upgradePage.level') }}
              </span>
            </div>
            <p class="mt-1 text-xs text-text-secondary sm:text-sm">
              @{{ userInfo?.username?.toLowerCase() || 'guest' }} {{ tr('upgradePage.membershipTagline') }}
            </p>

            <div class="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-xs text-text-secondary">
              <span>{{ tr('upgradePage.readingHistoryLabel') }}<strong class="font-semibold text-text">{{ historyQuery.total.value }}</strong></span>
              <span>{{ tr('upgradePage.collectionsLabel') }}<strong class="font-semibold text-text">{{ folderCount }}</strong></span>
              <span>{{ tr('upgradePage.likedArticlesLabel') }}<strong class="font-semibold text-text">{{ likesQuery.total.value }}</strong></span>
            </div>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2.5 pt-2 lg:pt-0">
          <router-link to="/feed/channels"
                       class="inline-flex h-10 items-center gap-2 rounded-full bg-surface px-5 text-xs font-medium text-text shadow-xs transition hover:bg-surface-container active:scale-[0.98]">
            <svg class="h-4 w-4 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>
            <span>{{ tr('upgradePage.manageFeeds') }}</span>
          </router-link>
          <button v-if="canInstall" type="button"
                  class="inline-flex h-10 items-center gap-2 rounded-full bg-surface px-5 text-xs font-medium text-text shadow-xs transition hover:bg-surface-container active:scale-[0.98]"
                  @click="handleInstallPWA">
            <svg class="h-4 w-4 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
            <span>{{ tr('upgradePage.installApp') }}</span>
          </button>
          <button type="button"
                  class="inline-flex h-10 items-center gap-2 rounded-full bg-surface px-5 text-xs font-medium text-text-secondary transition hover:bg-red-500/10 hover:text-red-600 active:scale-[0.98]"
                  @click="handleLogout">
            <span>{{ tr('upgradePage.signOut') }}</span>
          </button>
          <LanguageSwitcher />
        </div>
      </div>
    </section>

    <!-- 2. 未登录高诱惑 Hero 区 -->
    <section v-else class="mb-10 overflow-hidden rounded-[36px] bg-gradient-to-br from-primary/15 via-surface-container/60 to-surface p-8 sm:p-12 text-center relative shadow-xs">
      <div class="relative z-10 mb-5 flex justify-end"><LanguageSwitcher /></div>
      <div class="absolute -right-12 -top-12 h-64 w-64 rounded-full bg-primary/10 blur-3xl pointer-events-none" />
      <div class="mx-auto max-w-3xl space-y-5 relative z-10">
        <span class="inline-flex items-center gap-2 rounded-full bg-primary px-4 py-1.5 text-xs font-bold tracking-wide text-white shadow-sm">
          {{ tr('upgradePage.heroBadge') }}
        </span>
        <h1 class="text-3xl font-extrabold tracking-tight text-text sm:text-5xl leading-tight">
          {{ tr('upgradePage.heroQuestion') }}<br class="hidden sm:inline"/>
          <span class="text-primary underline decoration-primary/30">{{ tr('upgradePage.fiveMinutes') }}</span> {{ tr('upgradePage.heroOutcome') }}
        </h1>
        <i18n-t keypath="upgradePage.heroDescription" tag="p" scope="global" class="text-sm leading-relaxed text-text-secondary sm:text-base max-w-2xl mx-auto">
          <template #plan><strong class="text-text font-semibold">Plus</strong></template>
        </i18n-t>
        <div class="pt-2 flex flex-wrap items-center justify-center gap-4">
          <router-link :to="loginLink"
                       class="inline-flex h-12 items-center justify-center rounded-full bg-primary px-8 text-sm font-semibold text-white shadow-lg transition hover:brightness-105 active:scale-[0.98]">
            {{ tr('upgradePage.loginToUnlock') }}
          </router-link>
          <span class="text-xs text-text-secondary">{{ tr('upgradePage.socialProof') }}</span>
        </div>
      </div>
    </section>

    <!-- 导航切换 Tab -->
    <nav class="mb-8 flex items-center justify-center gap-2 overflow-x-auto pb-2 [scrollbar-width:none]" :aria-label="tr('upgradePage.accountMembership')">
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
            <span class="mb-1 block text-xs font-bold uppercase tracking-wider text-primary">{{ tr('upgradePage.upgradeMembership') }}</span>
            <span class="block truncate text-base font-bold text-text sm:text-xl">{{ tr('upgradePage.upgradeHint') }}</span>
            <span class="mt-1 block text-xs text-text-secondary">{{ tr('upgradePage.premiumFeatures') }}</span>
          </div>
          <span class="inline-flex h-10 shrink-0 items-center justify-center rounded-full bg-primary px-6 text-xs font-semibold text-white shadow-xs">
            {{ tr('upgradePage.viewPricing') }}
          </span>
        </button>

        <div class="space-y-12">
          <Shelf :title="tr('upgradePage.recentReading')" :items="recentHistory" time-field="readAt" :loading="historyPending" :error="historyError" to="/history" />
          <Shelf :title="tr('upgradePage.title')" :items="recentLikes" time-field="likedAt" :loading="likesPending" :error="likesError" to="/likes" />

          <!-- 收藏夹快捷网格 -->
          <section>
            <div class="mb-4 flex items-center justify-between">
              <h2 class="text-base font-bold text-text sm:text-lg">{{ tr('upgradePage.collections') }}</h2>
              <router-link to="/collections" class="text-xs font-medium text-text-secondary hover:text-text">{{ tr('common.viewAll') }}</router-link>
            </div>
            <div v-if="foldersPending" class="py-6 text-sm text-text-secondary">{{ tr('upgradePage.loadingFolders') }}</div>
            <div v-else-if="folders.length === 0"
                 class="rounded-3xl border border-dashed border-outline/20 bg-surface-container/20 py-10 text-center text-xs text-text-secondary">
              {{ tr('upgradePage.noFoldersCreated') }}
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
        <SectionHeader :title="tr('upgradePage.title')" to="/likes" />
        <DatedArticleList v-if="likesItems.length" :items="likesItems" time-field="likedAt" :removing="false" :remove-label="tr('common.unlike')" />
        <p v-if="likesPending" class="py-12 text-center text-sm text-text-secondary">{{ tr('upgradePage.loadingLikes') }}</p>
        <p v-else-if="likesError" class="py-12 text-center text-sm text-red-600 dark:text-red-400">{{ likesError }}</p>
        <p v-else-if="!likesItems.length" class="py-12 text-center text-sm text-text-secondary">{{ tr('upgradePage.emptyTitle') }}</p>
        <InfiniteLoadMore v-if="likesItems.length" :has-next-page="likesQuery.hasNextPage.value"
                          :loading="likesQuery.isFetchingNextPage.value" :fetching="likesQuery.isFetching.value"
                          :failed="likesQuery.isFetchNextPageError.value" :blocked="likesQuery.isError.value"
                          :fetch-more="likesQuery.fetchNextPage" />
      </template>

      <!-- 阅读历史 Tab -->
      <template v-else-if="activeTab === 'history'">
        <SectionHeader :title="tr('upgradePage.readingHistory')" to="/history" />
        <DatedArticleList v-if="historyItems.length" :items="historyItems" time-field="readAt" :removing="false" :remove-label="tr('upgradePage.removeRecord')" />
        <p v-if="historyPending" class="py-12 text-center text-sm text-text-secondary">{{ tr('upgradePage.loadingHistory') }}</p>
        <p v-else-if="historyError" class="py-12 text-center text-sm text-red-600 dark:text-red-400">{{ historyError }}</p>
        <p v-else-if="!historyItems.length" class="py-12 text-center text-sm text-text-secondary">{{ tr('upgradePage.noReadingHistoryYet') }}</p>
        <InfiniteLoadMore v-if="historyItems.length" :has-next-page="historyQuery.hasNextPage.value"
                          :loading="historyQuery.isFetchingNextPage.value" :fetching="historyQuery.isFetching.value"
                          :failed="historyQuery.isFetchNextPageError.value" :blocked="historyQuery.isError.value"
                          :fetch-more="historyQuery.fetchNextPage" />
      </template>

      <!-- 收藏夹 Tab -->
      <template v-else-if="activeTab === 'collections'">
        <SectionHeader :title="tr('upgradePage.collections')" to="/collections" />
        <div v-if="foldersPending" class="py-12 text-center text-sm text-text-secondary">{{ tr('upgradePage.loadingFolders') }}</div>
        <div v-else-if="!folders.length" class="py-12 text-center text-sm text-text-secondary">{{ tr('upgradePage.noFolders') }}</div>
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
            <h2 class="text-2xl font-bold tracking-tight text-text sm:text-3xl">{{ tr('upgradePage.choosePlan') }}</h2>
            <p class="mt-2 text-xs text-text-secondary sm:text-sm">{{ tr('upgradePage.useCoupon') }} <strong class="text-primary font-semibold">AIR</strong> {{ tr('upgradePage.firstYearOffer') }} <span class="text-primary font-bold">{{ tr('upgradePage.discountPercent') }}</span> {{ tr('upgradePage.exclusiveOffer') }}</p>
          </div>

          <!-- 方案卡片三列网格 -->
          <div class="grid grid-cols-1 gap-6 md:grid-cols-3 items-stretch">
            <PlanCard name="Free" :description="tr('upgradePage.freeDescription')" :price="tr('upgradePage.freePrice')" :features="freeFeatures"
                      :current="currentPlan === 'free'" />
            <PlanCard name="Plus" :description="tr('upgradePage.plusDescription')" :price="tr('upgradePage.plusPrice')" original-price="¥42" :equivalent="tr('upgradePage.plusEquivalent')" :features="plusFeatures"
                      :current="currentPlan === 'plus'" recommended @select="selectPlan('plus')" />
            <PlanCard name="Pro" :description="tr('upgradePage.proDescription')" :price="tr('upgradePage.proPrice')" original-price="¥80" :equivalent="tr('upgradePage.proEquivalent')" :features="proFeatures"
                      :current="currentPlan === 'pro'" @select="selectPlan('pro')" />
          </div>

          <!-- 4大核心权益可视化特点看板 -->
          <div class="mt-16 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            <div class="rounded-[28px] bg-surface-container/30 p-6 space-y-3 transition hover:bg-surface-container/60">
              <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/15 text-primary text-lg">📥</div>
              <h3 class="text-base font-bold text-text">{{ tr('upgradePage.feedFeatureTitle') }}</h3>
              <p class="text-xs leading-relaxed text-text-secondary">{{ tr('upgradePage.feedFeatureDescription') }}</p>
            </div>
            <div class="rounded-[28px] bg-surface-container/30 p-6 space-y-3 transition hover:bg-surface-container/60">
              <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/15 text-primary text-lg">🎯</div>
              <h3 class="text-base font-bold text-text">{{ tr('upgradePage.trackingFeatureTitle') }}</h3>
              <p class="text-xs leading-relaxed text-text-secondary">{{ tr('upgradePage.trackingFeatureDescription') }}</p>
            </div>
            <div class="rounded-[28px] bg-surface-container/30 p-6 space-y-3 transition hover:bg-surface-container/60">
              <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/15 text-primary text-lg">🗺️</div>
              <h3 class="text-base font-bold text-text">{{ tr('upgradePage.mindMapFeatureTitle') }}</h3>
              <p class="text-xs leading-relaxed text-text-secondary">{{ tr('upgradePage.mindMapFeatureDescription') }}</p>
            </div>
            <div class="rounded-[28px] bg-surface-container/30 p-6 space-y-3 transition hover:bg-surface-container/60">
              <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/15 text-primary text-lg">🔍</div>
              <h3 class="text-base font-bold text-text">{{ tr('upgradePage.agentFeatureTitle') }}</h3>
              <p class="text-xs leading-relaxed text-text-secondary">{{ tr('upgradePage.agentFeatureDescription') }}</p>
            </div>
          </div>

          <!-- 功能对比表格 -->
          <div class="mt-12 overflow-hidden rounded-[28px] bg-surface-container/20 p-6 sm:p-8">
            <h3 class="mb-6 text-center text-sm font-bold tracking-tight text-text sm:text-base">{{ tr('upgradePage.comparePlans') }}</h3>
            <div class="overflow-x-auto">
              <table class="w-full min-w-[560px] text-xs">
                <thead>
                <tr class="border-b border-outline/15 text-text-secondary">
                  <th class="px-4 py-3.5 text-left font-semibold">{{ tr('upgradePage.features') }}</th>
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
        <button type="button" :aria-label="tr('upgradePage.closePayment')"
                class="absolute right-6 top-6 flex h-8 w-8 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
                @click="closePaymentModal">✕</button>

        <div class="mb-6 text-center">
          <span class="inline-block rounded-full bg-primary/15 px-3 py-0.5 text-[10px] font-bold text-primary mb-2">{{ tr('upgradePage.couponApplied') }}</span>
          <h3 class="text-xl font-bold tracking-tight text-text">{{ tr('upgradePage.scanToPay') }}</h3>
          <p class="mt-1 text-xs text-text-secondary">{{ selectedPlanName }} ·
            <span class="line-through opacity-70">{{ selectedPlanPrice }}</span>
            <strong class="text-primary font-bold text-sm ml-1">{{ tr('upgradePage.amountDue') }} {{ discountedPrice }}</strong>
          </p>
        </div>

        <div class="mb-6 flex justify-center rounded-2xl bg-surface-container/40 p-5 shadow-2xs">
          <img v-if="selectedPlan" src="/weixin-pay-qrcode.png" :alt="tr('upgradePage.paymentQrCode')"
               class="h-56 w-56 object-contain sm:h-64 sm:w-64" @error="handleImageError" />
        </div>

        <div class="rounded-2xl bg-surface-container/40 p-4 text-xs text-text-secondary space-y-2">
          <p class="font-semibold text-text">{{ tr('upgradePage.paymentNotes') }}</p>
          <ul class="space-y-1.5 leading-relaxed">
            <li v-if="userInfo">{{ tr('upgradePage.paymentAccount') }}<strong class="text-text font-mono">{{ userInfo.username }}</strong></li>
            <li>{{ tr('upgradePage.activationPrefix') }} <strong class="text-text">{{ tr('upgradePage.activationTime') }}</strong> {{ tr('upgradePage.activationSuffix') }}</li>
            <li>{{ tr('upgradePage.supportPrefix') }} <a class="text-primary hover:underline font-semibold" href="https://zhidayingxiao.cn/to/06g6yb" target="_blank" rel="noopener noreferrer">{{ tr('upgradePage.contactSupport') }}</a></li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import LanguageSwitcher from '@/components/LanguageSwitcher.vue';
import { useDocumentTitle } from '@/composables/documentTitle';
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
useDocumentTitle(() => tr('upgradePage.accountMembership'));
const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const pwaStore = usePWAStore();
const { user: userInfo } = storeToRefs(authStore);
const { canInstall } = storeToRefs(pwaStore);

const tabDefinitions = computed(() => [
  { id: 'overview', label: tr('upgradePage.overview') },
  { id: 'likes', label: tr('common.like') },
  { id: 'collections', label: tr('upgradePage.collections') },
  { id: 'history', label: tr('upgradePage.readingHistory') },
  { id: 'plans', label: tr('upgradePage.membership') },
] as const);

type TabId = typeof tabDefinitions.value[number]['id'];
const tabs = computed(() => authStore.isAuthenticated
    ? tabDefinitions.value
    : tabDefinitions.value.filter(tab => tab.id === 'plans'));

const requestedTab = computed<TabId | null>(() => {
  const tab = route.query.tab;
  if (typeof tab !== 'string') return null;
  return tabDefinitions.value.some(item => item.id === tab) ? tab as TabId : null;
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
    { folderId: 'default', name: tr('upgradePage.defaultCollection'), createdAt: '', updatedAt: '' },
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

const freeFeatures = computed(() => [tr('upgradePage.freeFeedLimit'), tr('upgradePage.freeAiLimit'), tr('upgradePage.basicSummaries'), tr('upgradePage.keywordSearch')]);
const plusFeatures = computed(() => [tr('upgradePage.plusFeedLimit'), tr('upgradePage.plusTrackingLimit'), tr('upgradePage.plusAiLimit'), tr('upgradePage.mindMapFeatureTitle'), tr('upgradePage.agentSearch')]);
const proFeatures = computed(() => [tr('upgradePage.proFeedLimit'), tr('upgradePage.proTrackingLimit'), tr('upgradePage.proAiLimit'), tr('upgradePage.advancedAi'), tr('upgradePage.agentSearch'), tr('upgradePage.prioritySupport')]);

const comparisonData = computed(() => [
  { feature: tr('upgradePage.rssLimit'), free: tr('upgradePage.freeFeeds'), plus: tr('upgradePage.plusFeeds'), pro: tr('upgradePage.proFeeds') },
  { feature: tr('upgradePage.trackingFeatureTitle'), free: '—', plus: tr('upgradePage.plusTrackers'), pro: tr('upgradePage.proTrackers') },
  { feature: tr('upgradePage.dailyAiSummaries'), free: tr('upgradePage.freeSummaries'), plus: tr('upgradePage.plusSummaries'), pro: tr('upgradePage.proSummaries') },
  { feature: tr('upgradePage.articleMindMap'), free: '—', plus: '✓', pro: '✓' },
  { feature: tr('upgradePage.agentFeatureTitle'), free: '—', plus: '✓', pro: '✓' },
  { feature: tr('upgradePage.prioritySupport'), free: '—', plus: '—', pro: '✓' },
]);

const selectedPlanName = computed(() => selectedPlan.value === 'plus' ? tr('upgradePage.plusPlanName') : tr('upgradePage.proPlanName'));
const selectedPlanPrice = computed(() => selectedPlan.value === 'plus' ? tr('upgradePage.plusPrice') : tr('upgradePage.proPrice'));
const discountedPrice = computed(() => selectedPlan.value === 'plus' ? tr('upgradePage.plusDiscountPrice') : tr('upgradePage.proDiscountPrice'));

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
          h(RouterLink, { to: props.to, class: 'mr-2 text-xs font-medium text-text-secondary hover:text-text' }, () => tr('common.viewAll')),
          h('button', {
            type: 'button', title: tr('upgradePage.scrollLeft'), 'aria-label': tr('upgradePage.scrollLeft'), disabled: !canScrollLeft.value,
            class: 'flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-surface-container/60 transition hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-30',
            onClick: () => scrollShelf(-1),
          }, [h('svg', { class: 'h-4 w-4 text-text', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
            h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M15 19l-7-7 7-7' }),
          ])]),
          h('button', {
            type: 'button', title: tr('upgradePage.scrollRight'), 'aria-label': tr('upgradePage.scrollRight'), disabled: !canScrollRight.value,
            class: 'flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-surface-container/60 transition hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-30',
            onClick: () => scrollShelf(1),
          }, [h('svg', { class: 'h-4 w-4 text-text', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
            h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9 5l7 7-7 7' }),
          ])]),
        ]),
      ]),
      props.loading
          ? h('p', { class: 'py-8 text-xs text-text-secondary' }, tr('upgradePage.loadingEllipsis'))
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
                        ? h('img', { src: item.thumbnail, alt: item.title || tr('common.cover'), class: 'h-full w-full object-cover transition-transform duration-300 group-hover:scale-105', loading: 'lazy' })
                        : h('div', { class: 'flex h-full items-center justify-center p-3 text-center' }, [
                          h('span', { class: 'line-clamp-2 text-xs leading-relaxed text-text-secondary' }, item.summary || item.title || tr('upgradePage.noPreview')),
                        ]),
                  ]),
                  h('h3', { class: 'line-clamp-2 text-xs font-semibold leading-snug text-text transition-colors group-hover:text-primary sm:text-sm' }, item.title || tr('common.untitledArticle')),
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
          props.to ? h(RouterLink, { to: props.to, class: 'text-xs font-medium text-text-secondary hover:text-text' }, () => tr('common.viewAll')) : null,
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
              ? h('span', { class: 'absolute -top-3.5 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full bg-primary px-4 py-1 text-[11px] font-bold text-white shadow-sm' }, tr('upgradePage.recommendedPlan'))
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
              ? h('button', { disabled: true, class: 'mt-8 inline-flex h-11 w-full items-center justify-center rounded-full bg-surface-container text-xs font-semibold text-text-secondary cursor-not-allowed' }, tr('upgradePage.activePlan'))
              : h('button', {
                class: [
                  'mt-8 inline-flex h-11 w-full items-center justify-center rounded-full text-xs font-semibold transition active:scale-[0.98]',
                  props.recommended
                      ? 'bg-primary text-white shadow-md hover:brightness-105'
                      : 'bg-surface text-text border border-outline/20 hover:bg-surface-container',
                ],
                onClick: () => emit('select'),
              }, tr('upgradePage.activatePlan', { value0: props.name })),
        ]);
      },
    }),
  },
});
</script>
