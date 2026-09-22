<template>
  <div class="max-w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 text-text pb-24">
    <!-- 1. Header Profile Area (YouTube 风格用户资料) -->
    <div class="flex items-center gap-5 mb-6">
      <img
          v-if="userInfo?.avatarUrl"
          :src="userInfo.avatarUrl"
          alt="Avatar"
          class="h-20 w-20 sm:h-24 sm:w-24 rounded-full object-cover bg-primary/10 border border-outline/10 shrink-0"
      />
      <div
          v-else
          class="h-20 w-20 sm:h-24 sm:w-24 rounded-full bg-primary text-primary-foreground flex items-center justify-center text-3xl font-medium shrink-0"
      >
        {{ userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
      </div>

      <div class="flex-1 min-w-0">
        <h1 class="text-2xl sm:text-3xl font-bold truncate leading-tight">
          {{ userInfo?.username || '访客' }}
        </h1>
        <div class="flex items-center gap-2 mt-1 text-xs sm:text-sm text-text-secondary">
          <span>@{{ userInfo?.username?.toLowerCase() || 'guest' }}</span>
          <span>•</span>
          <span class="inline-flex items-center px-2 py-0.5 rounded-full bg-surface-container font-medium text-text">
            {{ userInfo?.currentPlan || 'Free' }}
          </span>
        </div>

        <!-- 药丸按钮组 (管理/安装/退出) -->
        <div class="flex items-center gap-2 mt-3 overflow-x-auto scrollbar-none">
          <router-link
              to="/feeds/channels"
              class="flex items-center px-3.5 py-1.5 rounded-full bg-surface-container hover:bg-surface-container/70 text-xs font-medium whitespace-nowrap transition-colors"
          >
            <svg class="w-3.5 h-3.5 mr-1.5 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
            管理订阅
          </router-link>

          <button
              v-if="canInstall"
              @click="handleInstallPWA"
              class="flex items-center px-3.5 py-1.5 rounded-full bg-surface-container hover:bg-surface-container/70 text-xs font-medium whitespace-nowrap transition-colors"
          >
            <svg class="w-3.5 h-3.5 mr-1.5 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
            </svg>
            安装应用
          </button>

          <button
              @click="handleLogout"
              class="flex items-center px-3.5 py-1.5 rounded-full bg-surface-container hover:bg-surface-container/70 text-xs font-medium whitespace-nowrap transition-colors"
          >
            <svg class="w-3.5 h-3.5 mr-1.5 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
            </svg>
            退出登录
          </button>
        </div>
      </div>
    </div>

    <!-- 2. YouTube Premium 风格横幅 (升级套餐入口) -->
    <div
        @click="showUpgradePlans = !showUpgradePlans"
        class="mb-8 p-4 sm:p-5 rounded-2xl bg-gradient-to-r from-primary/15 via-surface-container to-surface-container border border-primary/20 cursor-pointer flex items-center justify-between hover:border-primary/40 transition-all select-none"
    >
      <div class="pr-2">
        <div class="flex items-center gap-1.5 text-primary text-xs font-bold uppercase tracking-wider mb-1">
          <svg class="w-4 h-4 fill-current" viewBox="0 0 24 24">
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
          </svg>
          升级套餐
        </div>
        <p class="text-sm sm:text-base font-bold text-text">升级至 Standard 或 Pro 会员</p>
        <p class="text-xs text-text-secondary mt-0.5">解锁思维导图、更多订阅、AI能力</p>
      </div>

      <button
          type="button"
          class="flex items-center gap-1 text-xs font-semibold px-4 py-2 rounded-full bg-primary text-primary-foreground shrink-0 shadow-sm"
      >
        <span>{{ showUpgradePlans ? '收起方案' : '查看方案' }}</span>
        <svg
            class="w-3.5 h-3.5 transition-transform duration-200"
            :class="showUpgradePlans ? 'rotate-180' : ''"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
        </svg>
      </button>
    </div>

    <!-- 3. Collapsible Plans & Feature Comparison Section -->
    <transition name="expand">
      <div v-if="showUpgradePlans" class="space-y-6 mb-10 pt-1">
        <div class="text-center">
          <h2 class="text-xl sm:text-2xl font-bold text-text mb-1">选择你的套餐</h2>
          <p class="text-xs sm:text-sm text-text-secondary">解锁全部功能，提升阅读与搜索体验</p>
        </div>

        <!-- 定价卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
          <!-- Free Plan -->
          <div class="bg-surface rounded-2xl p-6 border border-outline/20 flex flex-col justify-between">
            <div>
              <div class="mb-3">
                <h3 class="text-lg font-bold text-text mb-0.5">Free</h3>
                <p class="text-xs text-text-secondary">体验基础功能</p>
              </div>
              <div class="mb-4">
                <div class="text-3xl font-extrabold text-text">免费</div>
              </div>
              <div class="space-y-2 mb-6">
                <div v-for="(feature, index) in freeFeatures" :key="index" class="flex items-start text-xs text-text">
                  <svg class="w-4 h-4 text-primary mr-2 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                  </svg>
                  <span>{{ feature }}</span>
                </div>
              </div>
            </div>
            <button disabled class="w-full py-2.5 rounded-full text-xs font-semibold bg-surface-container text-text-secondary cursor-not-allowed">
              {{ currentPlan === 'free' ? '当前方案' : '免费版本' }}
            </button>
          </div>

          <!-- Standard Plan -->
          <div
              @click="currentPlan !== 'standard' && selectPlan('standard')"
              :class="[
              'bg-surface rounded-2xl p-6 cursor-pointer transition-all relative border-2 flex flex-col justify-between',
              selectedPlan === 'standard' ? 'border-primary shadow-lg shadow-primary/10' : 'border-outline/20 hover:border-outline/60'
            ]"
          >
            <div class="absolute -top-2.5 right-4">
              <span class="bg-primary text-primary-foreground text-[10px] font-bold px-2 py-0.5 rounded-full tracking-wide">
                推荐
              </span>
            </div>
            <div>
              <div class="mb-3">
                <h3 class="text-lg font-bold text-text mb-0.5">Standard</h3>
                <p class="text-xs text-text-secondary">适合个人深度阅读</p>
              </div>
              <div class="mb-4">
                <div class="text-3xl font-extrabold text-text">
                  ¥36
                  <span class="text-xs font-normal text-text-secondary">/年</span>
                </div>
              </div>
              <div class="space-y-2 mb-6">
                <div v-for="(feature, index) in standardFeatures" :key="index" class="flex items-start text-xs text-text">
                  <svg class="w-4 h-4 text-primary mr-2 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                  </svg>
                  <span>{{ feature }}</span>
                </div>
              </div>
            </div>
            <button
                v-if="currentPlan === 'standard'"
                disabled
                class="w-full py-2.5 rounded-full text-xs font-semibold bg-surface-container text-text-secondary cursor-not-allowed"
            >
              当前方案
            </button>
            <button
                v-else
                @click.stop="selectPlan('standard')"
                :class="[
                'w-full py-2.5 rounded-full text-xs font-semibold transition-all',
                selectedPlan === 'standard'
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-surface-container text-text hover:bg-surface-container/70'
              ]"
            >
              {{ selectedPlan === 'standard' ? '已选择' : '选择 Standard' }}
            </button>
          </div>

          <!-- Pro Plan -->
          <div
              @click="currentPlan !== 'pro' && selectPlan('pro')"
              :class="[
              'bg-surface rounded-2xl p-6 cursor-pointer transition-all border-2 flex flex-col justify-between',
              selectedPlan === 'pro' ? 'border-primary shadow-lg shadow-primary/10' : 'border-outline/20 hover:border-outline/60'
            ]"
          >
            <div>
              <div class="mb-3">
                <h3 class="text-lg font-bold text-text mb-0.5">Pro</h3>
                <p class="text-xs text-text-secondary">适合专业用户</p>
              </div>
              <div class="mb-4">
                <div class="text-3xl font-extrabold text-text">
                  ¥68
                  <span class="text-xs font-normal text-text-secondary">/年</span>
                </div>
              </div>
              <div class="space-y-2 mb-6">
                <div v-for="(feature, index) in proFeatures" :key="index" class="flex items-start text-xs text-text">
                  <svg class="w-4 h-4 text-primary mr-2 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                  </svg>
                  <span>{{ feature }}</span>
                </div>
              </div>
            </div>
            <button
                v-if="currentPlan === 'pro'"
                disabled
                class="w-full py-2.5 rounded-full text-xs font-semibold bg-surface-container text-text-secondary cursor-not-allowed"
            >
              当前方案
            </button>
            <button
                v-else
                @click.stop="selectPlan('pro')"
                :class="[
                'w-full py-2.5 rounded-full text-xs font-semibold transition-all',
                selectedPlan === 'pro'
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-surface-container text-text hover:bg-surface-container/70'
              ]"
            >
              {{ selectedPlan === 'pro' ? '已选择' : '选择 Pro' }}
            </button>
          </div>
        </div>

        <div v-if="selectedPlan" class="text-center pt-2">
          <button
              @click="handlePayment"
              class="w-full sm:w-auto px-12 py-3 bg-primary text-primary-foreground rounded-full font-semibold text-sm hover:opacity-95 transition-opacity"
          >
            继续支付 ({{ selectedPlanName }})
          </button>
          <p class="mt-2 text-xs text-text-secondary">可随时取消订阅，无需承诺</p>
        </div>

        <!-- 功能对比表 -->
        <div class="bg-surface rounded-2xl p-6 border border-outline/20 mt-4">
          <h3 class="text-base font-bold text-text mb-4 text-center">功能对比</h3>
          <div class="overflow-x-auto">
            <table class="w-full min-w-[500px]">
              <thead>
              <tr class="border-b border-outline/20">
                <th class="text-left py-3 px-3 text-xs text-text-secondary font-medium">功能</th>
                <th class="text-center py-3 px-2 text-xs text-text font-semibold">Free</th>
                <th class="text-center py-3 px-2 text-xs text-text font-semibold">Standard</th>
                <th class="text-center py-3 px-2 text-xs text-text font-semibold">Pro</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="(row, index) in comparisonData" :key="index" class="border-b border-outline/10 last:border-b-0">
                <td class="py-3 px-3 text-xs text-text">{{ row.feature }}</td>
                <td class="py-3 px-2 text-center text-xs text-text-secondary">{{ row.free }}</td>
                <td class="py-3 px-2 text-center text-xs text-text font-medium">{{ row.standard }}</td>
                <td class="py-3 px-2 text-center text-xs text-text font-medium">{{ row.pro }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </transition>

    <!-- 4. Shelf: 历史记录 -->
    <section class="mb-10">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg sm:text-xl font-bold text-text">历史记录</h2>
        <div class="flex items-center gap-2">
          <router-link
              to="/history"
              class="px-3.5 py-1 rounded-full text-xs font-semibold text-text hover:bg-surface-container transition-colors"
          >
            查看全部
          </router-link>
          <button
              @click="scrollShelf(historyShelfRef, -1)"
              class="w-8 h-8 rounded-full border border-outline/20 flex items-center justify-center hover:bg-surface-container transition-colors"
              aria-label="向左滚动"
          >
            <svg class="w-4 h-4 text-text" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
          </button>
          <button
              @click="scrollShelf(historyShelfRef, 1)"
              class="w-8 h-8 rounded-full border border-outline/20 flex items-center justify-center hover:bg-surface-container transition-colors"
              aria-label="向右滚动"
          >
            <svg class="w-4 h-4 text-text" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 骨架屏 -->
      <div v-if="historyLoading" class="flex gap-4 overflow-x-auto pb-2 scrollbar-none">
        <div v-for="i in 5" :key="i" class="w-48 sm:w-56 shrink-0 animate-pulse">
          <div class="w-full aspect-video rounded-xl bg-surface-container mb-2.5"></div>
          <div class="h-3.5 bg-surface-container rounded w-11/12 mb-1.5"></div>
          <div class="h-3 bg-surface-container rounded w-2/3"></div>
        </div>
      </div>

      <!-- 报错提示 -->
      <div v-else-if="historyError" class="py-6 px-4 rounded-xl bg-surface-container/50 text-center">
        <p class="text-xs text-error">{{ historyError }}</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="recentHistory.length === 0" class="py-8 rounded-xl bg-surface-container/30 border border-outline/10 text-center">
        <p class="text-sm text-text-secondary">暂无历史阅读记录</p>
      </div>

      <!-- 历史条目轨道 -->
      <div
          v-else
          ref="historyShelfRef"
          class="flex gap-4 overflow-x-auto pb-2 scrollbar-none scroll-smooth"
      >
        <router-link
            v-for="item in recentHistory"
            :key="item.articleId"
            :to="`/articles/${item.articleId}`"
            class="w-48 sm:w-56 shrink-0 group flex flex-col"
        >
          <div class="w-full aspect-video rounded-xl bg-surface-container relative overflow-hidden mb-2.5 border border-outline/10">
            <img
                v-if="item.thumbnail"
                :src="item.thumbnail"
                :alt="item.title || '封面'"
                class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                loading="lazy"
            />
            <div v-else class="absolute inset-0 flex flex-col items-center justify-center p-3 text-center bg-surface-container/80">
              <span class="text-xs text-text-secondary line-clamp-2 leading-relaxed">
                {{ item.summary || item.title || '暂无内容缩略' }}
              </span>
            </div>
          </div>

          <div class="flex items-start justify-between gap-1.5">
            <h3 class="text-xs sm:text-sm font-semibold text-text line-clamp-2 leading-snug group-hover:text-primary transition-colors flex-1">
              {{ item.title || '未命名文章' }}
            </h3>
          </div>
          <p class="text-[11px] text-text-secondary mt-1 flex items-center gap-1.5 truncate">
            <span v-if="item.feedTitle" class="truncate">{{ item.feedTitle }}</span>
            <span v-if="item.feedTitle && item.readAt">•</span>
            <span v-if="item.readAt" class="shrink-0">{{ formatRelativeTime(item.readAt) }}</span>
          </p>
        </router-link>
      </div>
    </section>

    <!-- 5. Shelf: 我的喜欢 -->
    <section class="mb-10">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg sm:text-xl font-bold text-text">我的喜欢</h2>
        <div class="flex items-center gap-2">
          <router-link
              to="/likes"
              class="px-3.5 py-1 rounded-full text-xs font-semibold text-text hover:bg-surface-container transition-colors"
          >
            查看全部
          </router-link>
          <button
              @click="scrollShelf(likesShelfRef, -1)"
              class="w-8 h-8 rounded-full border border-outline/20 flex items-center justify-center hover:bg-surface-container transition-colors"
              aria-label="向左滚动"
          >
            <svg class="w-4 h-4 text-text" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
          </button>
          <button
              @click="scrollShelf(likesShelfRef, 1)"
              class="w-8 h-8 rounded-full border border-outline/20 flex items-center justify-center hover:bg-surface-container transition-colors"
              aria-label="向右滚动"
          >
            <svg class="w-4 h-4 text-text" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 骨架屏 -->
      <div v-if="likesLoading" class="flex gap-4 overflow-x-auto pb-2 scrollbar-none">
        <div v-for="i in 5" :key="i" class="w-48 sm:w-56 shrink-0 animate-pulse">
          <div class="w-full aspect-video rounded-xl bg-surface-container mb-2.5"></div>
          <div class="h-3.5 bg-surface-container rounded w-11/12 mb-1.5"></div>
          <div class="h-3 bg-surface-container rounded w-2/3"></div>
        </div>
      </div>

      <!-- 报错提示 -->
      <div v-else-if="likesError" class="py-6 px-4 rounded-xl bg-surface-container/50 text-center">
        <p class="text-xs text-error">{{ likesError }}</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="recentLikes.length === 0" class="py-8 rounded-xl bg-surface-container/30 border border-outline/10 text-center">
        <p class="text-sm text-text-secondary">暂无喜欢的文章</p>
      </div>

      <!-- 喜欢文章轨道 -->
      <div
          v-else
          ref="likesShelfRef"
          class="flex gap-4 overflow-x-auto pb-2 scrollbar-none scroll-smooth"
      >
        <router-link
            v-for="item in recentLikes"
            :key="item.articleId"
            :to="`/articles/${item.articleId}`"
            class="w-48 sm:w-56 shrink-0 group flex flex-col"
        >
          <!-- 16:9 缩略图带有爱心角标 -->
          <div class="w-full aspect-video rounded-xl bg-surface-container relative overflow-hidden mb-2.5 border border-outline/10">
            <img
                v-if="item.thumbnail"
                :src="item.thumbnail"
                :alt="item.title || '封面'"
                class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                loading="lazy"
            />
            <div v-else class="absolute inset-0 flex flex-col items-center justify-center p-3 text-center bg-surface-container/80">
              <span class="text-xs text-text-secondary line-clamp-2 leading-relaxed">
                {{ item.summary || item.title || '暂无内容缩略' }}
              </span>
            </div>
            <!-- 右上角小红心角标 -->
            <div class="absolute top-2 right-2 p-1.5 rounded-full bg-black/60 text-red-500 backdrop-blur-sm shadow">
              <svg class="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
            </div>
          </div>

          <div class="flex items-start justify-between gap-1.5">
            <h3 class="text-xs sm:text-sm font-semibold text-text line-clamp-2 leading-snug group-hover:text-primary transition-colors flex-1">
              {{ item.title || '未命名文章' }}
            </h3>
          </div>
          <p class="text-[11px] text-text-secondary mt-1 flex items-center gap-1.5 truncate">
            <span v-if="item.feedTitle" class="truncate">{{ item.feedTitle }}</span>
            <span v-if="item.feedTitle && item.likedAt">•</span>
            <span v-if="item.likedAt" class="shrink-0">{{ formatRelativeTime(item.likedAt) }}</span>
          </p>
        </router-link>
      </div>
    </section>

    <!-- 6. Shelf: 收藏夹 -->
    <section class="mb-10">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg sm:text-xl font-bold text-text">收藏夹</h2>
        <div class="flex items-center gap-2">
          <router-link
              to="/collections"
              class="px-3.5 py-1 rounded-full text-xs font-semibold text-text hover:bg-surface-container transition-colors"
          >
            查看全部
          </router-link>
          <button
              @click="scrollShelf(collectionsShelfRef, -1)"
              class="w-8 h-8 rounded-full border border-outline/20 flex items-center justify-center hover:bg-surface-container transition-colors"
              aria-label="向左滚动"
          >
            <svg class="w-4 h-4 text-text" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
          </button>
          <button
              @click="scrollShelf(collectionsShelfRef, 1)"
              class="w-8 h-8 rounded-full border border-outline/20 flex items-center justify-center hover:bg-surface-container transition-colors"
              aria-label="向右滚动"
          >
            <svg class="w-4 h-4 text-text" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 骨架屏 -->
      <div v-if="foldersLoading" class="flex gap-4 overflow-x-auto pb-2 scrollbar-none">
        <div v-for="i in 4" :key="i" class="w-48 sm:w-56 shrink-0 animate-pulse">
          <div class="w-full aspect-video rounded-xl bg-surface-container mb-2.5"></div>
          <div class="h-3.5 bg-surface-container rounded w-10/12 mb-1.5"></div>
          <div class="h-3 bg-surface-container rounded w-1/2"></div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="folders.length === 0" class="py-8 rounded-xl bg-surface-container/30 border border-outline/10 text-center">
        <p class="text-sm text-text-secondary">暂未创建收藏夹</p>
      </div>

      <!-- 收藏夹横划轨道 -->
      <div
          v-else
          ref="collectionsShelfRef"
          class="flex gap-4 overflow-x-auto pb-2 scrollbar-none scroll-smooth"
      >
        <router-link
            v-for="folder in folders"
            :key="folder.folderId"
            :to="{ path: '/collections', query: { folderId: folder.folderId } }"
            class="w-48 sm:w-56 shrink-0 group flex flex-col"
        >
          <!-- 仿 YouTube 播放列表叠层质感卡片 -->
          <div class="w-full aspect-video rounded-xl bg-surface-container relative overflow-hidden mb-2.5 border border-outline/10 flex flex-col justify-end p-3">
            <div class="absolute inset-0 flex items-center justify-center bg-surface-container-high/40 group-hover:scale-105 transition-transform duration-300">
              <svg class="w-10 h-10 text-primary/40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"/>
              </svg>
            </div>
            <!-- 右下角播放列表标识条 -->
            <div class="relative z-10 self-end px-2 py-0.5 rounded bg-black/60 backdrop-blur-sm text-[11px] text-white flex items-center gap-1 font-medium">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7"/>
              </svg>
              <span>收藏集</span>
            </div>
          </div>

          <h3 class="text-xs sm:text-sm font-semibold text-text truncate group-hover:text-primary transition-colors">
            {{ folder.name }}
          </h3>
        </router-link>
      </div>
    </section>

    <!-- 7. Grouped Settings Section (YouTube 简洁系统菜单列表) -->
    <div class="border-t border-outline/10 pt-4 space-y-1">
      <router-link
          to="/history"
          class="w-full flex items-center px-3 py-3.5 rounded-xl hover:bg-surface-container transition-colors"
      >
        <svg class="w-5 h-5 text-text mr-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <span class="flex-1 text-sm text-text font-normal">历史记录列表</span>
        <svg class="w-4 h-4 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
        </svg>
      </router-link>

      <a
          href="https://zhidayingxiao.cn/to/06g6yb"
          target="_blank"
          rel="noopener noreferrer"
          class="w-full flex items-center px-3 py-3.5 rounded-xl hover:bg-surface-container transition-colors"
      >
        <svg class="w-5 h-5 text-text mr-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <span class="flex-1 text-sm text-text font-normal">帮助与客服支持</span>
        <svg class="w-4 h-4 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
        </svg>
      </a>
    </div>

    <!-- 8. Payment Modal -->
    <div v-if="showPaymentModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black bg-opacity-60" @click="closePaymentModal"></div>

      <div class="relative bg-surface rounded-2xl sm:rounded-3xl shadow-2xl max-w-md w-full p-6 sm:p-8 z-10 max-h-[90vh] overflow-y-auto">
        <button @click="closePaymentModal" class="absolute top-4 right-4 sm:top-6 sm:right-6 text-text-secondary hover:text-text transition-colors">
          <svg class="w-5 h-5 sm:w-6 sm:h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>

        <div class="text-center mb-5 sm:mb-6">
          <h3 class="text-xl sm:text-2xl font-medium text-text mb-1 sm:mb-2">扫码支付</h3>
          <p class="text-sm sm:text-base text-text-secondary">{{ selectedPlanName }} - {{ selectedPlanPrice }}</p>
        </div>

        <div class="bg-surface-container rounded-xl sm:rounded-2xl p-6 sm:p-8 flex items-center justify-center mb-5 sm:mb-6">
          <div class="text-center">
            <div class="w-64 h-64 sm:w-80 sm:h-80 bg-surface rounded-xl sm:rounded-2xl flex items-center justify-center mb-3 sm:mb-4">
              <img
                  v-if="selectedPlan"
                  src="/weixin-pay-qrcode.png"
                  alt="支付二维码"
                  class="w-full h-full object-contain"
                  @error="handleImageError"
              />
              <span v-else class="text-text-secondary text-xs sm:text-sm">支付二维码</span>
            </div>
            <p class="text-xs sm:text-sm text-text-secondary">请使用微信扫描二维码完成支付</p>
          </div>
        </div>

        <div class="bg-primary/10 rounded-xl sm:rounded-2xl p-3.5 sm:p-4">
          <div class="flex items-start space-x-2.5 sm:space-x-3">
            <svg class="w-4 h-4 sm:w-5 sm:h-5 text-primary flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <div class="text-xs sm:text-sm text-text">
              <p class="font-medium mb-1.5 sm:mb-2">支付提示：</p>
              <ul class="space-y-1 sm:space-y-1.5 text-text-secondary">
                <li v-if="userInfo">• 付款留言处填写账号：<strong class="text-text">{{ userInfo.username }}</strong></li>
                <li>• 支付成功后，套餐将在 2 小时内自动激活</li>
                <li>• 记得使用优惠码 <strong class="text-text">AIR</strong> 享受 85 折优惠</li>
                <li>• 如有问题，<a class="text-primary hover:underline" href="https://zhidayingxiao.cn/to/06g6yb">请联系客服</a></li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { usePWAStore } from "../stores/pwa";
import { useHistoryQuery } from "@/queries/history";
import { useLikesQuery } from "@/queries/likes";
import { useCollectionFoldersQuery } from "@/queries/collectionFolders";
import type { HistoryEntryDto } from '@/api/history';
import type { LikeDto } from '@/api/like';

const router = useRouter();
const authStore = useAuthStore();
const pwaStore = usePWAStore();

const selectedPlan = ref<string | null>(null);
const showPaymentModal = ref(false);
const showUpgradePlans = ref(false);
const { user: userInfo } = storeToRefs(authStore);
const { canInstall } = storeToRefs(pwaStore);

// 1. 历史记录对接
const {
  items: historyItems,
  isLoading: historyLoading,
  errorMessage: historyError
} = useHistoryQuery();

const recentHistory = computed<HistoryEntryDto[]>(() => {
  return (historyItems.value || []).slice(0, 10);
});

// 2. 我的喜欢对接
const {
  items: likesItems,
  isLoading: likesLoading,
  errorMessage: likesError
} = useLikesQuery();

const recentLikes = computed<LikeDto[]>(() => {
  return (likesItems.value || []).slice(0, 10);
});

// 3. 收藏夹对接（包含默认收藏夹，并过滤掉空异常）
const {
  data: foldersData,
  isLoading: foldersLoading,
} = useCollectionFoldersQuery();

const folders = computed(() => {
  const dynamicFolders = foldersData.value?.pages.flatMap((page) => page.content) ?? [];
  return [{ folderId: 'default', name: '默认收藏' }, ...dynamicFolders];
});

// 4. 轨道平滑滚动
const historyShelfRef = ref<HTMLElement | null>(null);
const likesShelfRef = ref<HTMLElement | null>(null);
const collectionsShelfRef = ref<HTMLElement | null>(null);

const scrollShelf = (el: HTMLElement | null, direction: number) => {
  if (!el) return;
  const scrollAmount = el.clientWidth * 0.75;
  el.scrollBy({ left: direction * scrollAmount, behavior: 'smooth' });
};

// 5. 相对时间计算
const formatRelativeTime = (dateStr?: string) => {
  if (!dateStr) return '';
  const now = Date.now();
  const past = new Date(dateStr).getTime();
  const diffMinutes = Math.floor((now - past) / (1000 * 60));

  if (diffMinutes < 1) return '刚刚';
  if (diffMinutes < 60) return `${diffMinutes}分钟前`;
  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours}小时前`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 30) return `${diffDays}天前`;
  return new Date(dateStr).toLocaleDateString();
};

const standardFeatures = [
  '订阅 300 个 RSS 源',
  '每天 AI 处理 30 篇文章',
  '创建 2 个订阅源',
  '智能摘要和关键信息提取',
  '内容分类和标签管理',
  '文章思维导图',
  '语义搜索',
];

const proFeatures = [
  '订阅 2000 个 RSS 源',
  '每天 AI 处理 100 篇文章',
  '创建 6 个订阅源',
  '智能推荐（多路召回+重排）',
  '文章思维导图',
  '语义搜索',
  '优先客户支持',
];

const freeFeatures = [
  '订阅 60 个 RSS 源',
  '每天 AI 处理 3 篇文章',
  '基础摘要功能',
  '内容分类和标签',
  '关键词搜索',
];

const comparisonData = [
  { feature: 'RSS 订阅源数量', free: '60 个', standard: '300 个', pro: '2000 个' },
  { feature: 'AI 处理文章数/天', free: '3 篇', standard: '30 篇', pro: '100 篇' },
  { feature: '创建订阅源', free: '—', standard: '2 个', pro: '6 个' },
  { feature: '智能摘要', free: '基础', standard: '✓', pro: '✓' },
  { feature: '内容分类和标签', free: '✓', standard: '✓', pro: '✓' },
  { feature: '文章思维导图', free: '—', standard: '✓', pro: '✓' },
  { feature: '搜索', free: '关键词', standard: '关键词 + 语义', pro: '关键词 + 语义' },
  { feature: '智能推荐', free: '基础', standard: '基础', pro: '多路召回+重排' },
];

const selectedPlanName = computed(() => {
  if (selectedPlan.value === 'standard') return 'Standard 套餐';
  if (selectedPlan.value === 'pro') return 'Pro 套餐';
  return '';
});

const selectedPlanPrice = computed(() => {
  if (selectedPlan.value === 'standard') return '¥36/年';
  if (selectedPlan.value === 'pro') return '¥68/年';
  return '';
});

const currentPlan = computed(() => (userInfo.value?.currentPlan || 'Free').toLowerCase());

const selectPlan = (planId: string) => {
  selectedPlan.value = planId;
  showPaymentModal.value = true;
};

const handlePayment = () => {
  if (selectedPlan.value) {
    showPaymentModal.value = true;
  }
};

const closePaymentModal = () => {
  showPaymentModal.value = false;
};

const handleImageError = () => {
  console.warn('Failed to load QR code image');
};

const handleLogout = async () => {
  await authStore.logout();
  await router.replace({ name: 'auth' });
};

const handleInstallPWA = async () => {
  await pwaStore.installPWA();
};
</script>

<style scoped>
/* 隐藏横向滚动条保留平滑滑动能力 */
.scrollbar-none::-webkit-scrollbar {
  display: none;
}
.scrollbar-none {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* 方案抽屉动画 */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
  transform: translateY(-8px);
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 3000px;
  transform: translateY(0);
}
</style>