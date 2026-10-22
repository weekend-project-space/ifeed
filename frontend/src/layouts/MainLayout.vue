<template>
  <div class="min-h-screen text-text transition-colors duration-300 pt-safe bg-surface">

    <!-- 顶部导航栏：MD3 Top App Bar -->
    <header class="sticky top-0 z-30 bg-surface/90 backdrop-blur-md transition-transform duration-200"
            :class="[
        isSidebarCollapsed ? 'lg:pl-16' : 'lg:pl-72',
        isHeaderHidden ? '-translate-y-full lg:translate-y-0' : 'translate-y-0',
        route.name === 'home' || route.name === 'article' ? '' : 'hidden lg:block'
      ]">
      <div class="flex items-center justify-between gap-3 px-4 py-3 lg:px-6 lg:py-2.5">
        <!-- 左侧：移动端 Logo / 返回 与 桌面侧边栏折叠按钮 -->
        <div class="flex items-center gap-3">
          <!-- 移动端 Logo -->
          <RouterLink :to="{ name: 'home' }" class="flex items-center gap-2.5 lg:hidden" v-if="route.name === 'home'">
            <img class="h-8 w-8 rounded-2xl shadow-2xs dark:invert" src="/logo.svg" alt="iFeed" />
            <span class="text-base font-bold tracking-tight text-text">IFeed</span>
          </RouterLink>

          <!-- 移动端非首页返回按钮 -->
          <button v-else type="button"
                  class="-ml-2 flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text lg:hidden"
                  @click="router.back()" aria-label="返回">
            <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
            </svg>
          </button>

          <!-- 桌面端侧边栏切换按钮：MD3 标准图标按钮 -->
          <button type="button"
                  class="hidden lg:flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text active:scale-95"
                  @click="toggleSidebar">
            <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" :d="icons.menu" />
            </svg>
          </button>
        </div>

        <!-- 中间：桌面端 MD3 搜索胶囊 -->
        <div class="hidden lg:flex flex-1 max-w-xl mx-auto">
          <div class="relative w-full group">
            <input v-model="search" type="search" placeholder="搜索文章、标签、订阅..."
                   class="w-full h-10 pl-24 pr-11 rounded-full border border-outline/20 bg-surface-container/50 text-xs text-text outline-none transition placeholder:text-text-secondary/60 focus:border-primary focus:bg-surface focus:ring-2 focus:ring-primary/20 sm:text-sm"
                   @focus="searchFocused = true" @blur="searchFocused = false" @keyup.enter="handleSearch" />

            <!-- 数据源选择胶囊 (订阅 / 发现) -->
            <div class="absolute left-1.5 top-1/2 -translate-y-1/2" ref="searchSourceRef">
              <button type="button"
                      class="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-semibold transition select-none"
                      :class="searchFocused ? 'bg-primary/10 text-primary' : 'bg-surface text-text-secondary hover:bg-surface-container-high hover:text-text shadow-2xs'"
                      @click="toggleSourceDropdown">
                <span>{{ searchSource === 'owner' ? '订阅' : '发现' }}</span>
                <svg class="h-3 w-3 opacity-75" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              <!-- 下拉选择面板 -->
              <div v-if="showSourceDropdown"
                   class="absolute top-full mt-1.5 left-0 w-24 overflow-hidden rounded-2xl border border-outline/10 bg-surface py-1 shadow-xl z-50">
                <button type="button"
                        class="w-full px-3.5 py-2 text-xs text-left transition select-none"
                        :class="searchSource === 'owner' ? 'bg-primary/10 font-bold text-primary' : 'text-text hover:bg-surface-container'"
                        @click="selectSource('owner')">
                  订阅
                </button>
                <button type="button"
                        class="w-full px-3.5 py-2 text-xs text-left transition select-none"
                        :class="searchSource === 'global' ? 'bg-primary/10 font-bold text-primary' : 'text-text hover:bg-surface-container'"
                        @click="selectSource('global')">
                  发现
                </button>
              </div>
            </div>

            <!-- 搜索触发按钮 -->
            <button type="button"
                    class="absolute right-1.5 top-1/2 -translate-y-1/2 flex h-7 w-7 items-center justify-center rounded-full transition"
                    :class="search ? 'bg-primary text-white shadow-2xs hover:brightness-105' : 'text-text-secondary hover:bg-surface-container'"
                    @click="handleSearch">
              <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" :d="icons.search" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 右侧：挂载点 + 操作工具栏 -->
        <div class="flex items-center gap-2 min-w-[10rem] justify-end">
          <div class="flex items-center gap-1.5" id="header-action"></div>

          <!-- 移动端搜索与头像 -->
          <div v-show="route.name !== 'article'" class="flex items-center gap-2 lg:hidden">
            <button type="button"
                    class="flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text active:scale-95"
                    @click="openSearchPage">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" :d="icons.search" />
              </svg>
            </button>
            <router-link :to="{ name: 'upgrade' }">
              <img v-if="user && user.avatarUrl" :src="user.avatarUrl" class="h-8 w-8 rounded-full object-cover shadow-2xs" />
              <div v-else
                   class="flex h-8 w-8 items-center justify-center rounded-full bg-surface-container-high text-xs font-bold text-text shadow-2xs">
                {{ userInitials }}
              </div>
            </router-link>
          </div>

          <!-- 桌面端动作按钮组 -->
          <div v-show="route.name !== 'article'" class="hidden lg:flex items-center gap-2">
            <!-- 主题切换 -->
            <button type="button"
                    class="flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text active:scale-95"
                    @click="toggleTheme">
              <svg v-if="isDark" class="h-4.5 w-4.5" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M21.752 15.002A9.718 9.718 0 0118 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 003 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 009.002-5.998z" />
              </svg>
              <svg v-else class="h-4.5 w-4.5" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M12 3v2.25m6.364.386l-1.591 1.591M21 12h-2.25m-.386 6.364l-1.591-1.591M12 18.75V21m-4.773-4.227l-1.591 1.591M5.25 12H3m4.227-4.773L5.636 5.636M15.75 12a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0z" />
              </svg>
            </button>

            <!-- 订阅按钮：MD3 Tonal 药丸 -->
            <RouterLink :to="{ name: 'discover' }"
                        class="inline-flex items-center gap-1.5 h-8.5 px-4 rounded-full bg-surface-container/60 hover:bg-surface-container text-xs font-semibold text-text shadow-2xs transition active:scale-95">
              <svg class="h-4 w-4 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 5v14M5 12h14"></path>
              </svg>
              <span>订阅</span>
            </RouterLink>

            <!-- 用户信息菜单 (MD3 Surface Dropdown) -->
            <div class="relative group">
              <router-link :to="{ name: 'upgrade'}" >
                <img v-if="user && user.avatarUrl" :src="user.avatarUrl" class="h-8.5 w-8.5 rounded-full object-cover shadow-2xs cursor-pointer group-hover:ring-2 group-hover:ring-primary/25 transition-all" />
                <div v-else
                     class="flex h-8.5 w-8.5 items-center justify-center rounded-full bg-surface-container-high text-xs font-bold text-text cursor-pointer shadow-2xs group-hover:ring-2 group-hover:ring-primary/25 transition-all">
                  {{ userInitials }}
                </div>
              </router-link>

              <!-- 悬浮菜单面板 -->
              <div
                  class="absolute right-0 mt-2 w-60 origin-top-right rounded-2xl bg-surface border border-outline/10 shadow-2xl overflow-hidden z-50 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 transform scale-95 group-hover:scale-100">
                <div class="px-4 py-3.5 border-b border-outline/10 bg-surface-container/20">
                  <p class="text-xs font-bold text-text truncate">
                    {{ user?.username ?? '访客' }}
                  </p>
                  <p class="text-[11px] text-text-secondary truncate mt-0.5">
                    {{ user?.email ?? '' }}
                  </p>
                </div>

                <div class="p-1">
                  <RouterLink to="/feed/channels"
                              class="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-text hover:bg-surface-container transition">
                    <svg class="h-4 w-4 text-text-secondary" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round"
                            d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z" />
                      <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                    <span>管理订阅</span>
                  </RouterLink>
                </div>

                <div v-if="user" class="border-t border-outline/10 p-1">
                  <button type="button"
                          class="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-semibold text-red-600 dark:text-red-400 hover:bg-red-500/10 transition"
                          @click="handleLogout">
                    <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round"
                            d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                    <span>退出登录</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- 移动端全屏搜索遮罩页 -->
    <transition name="search-slide">
      <div v-if="showMobileSearch" class="fixed inset-0 z-50 bg-surface lg:hidden">
        <div class="flex flex-col h-full">
          <div class="flex items-center gap-3 px-4 py-3 border-b border-outline/10">
            <button type="button"
                    class="flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text active:scale-95"
                    @click="closeMobileSearch">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div class="flex-1 relative">
              <input ref="mobileSearchInput" v-model="search" type="search" placeholder="搜索文章、标签、订阅..."
                     class="w-full h-10 pl-4 pr-11 rounded-full border border-outline/20 bg-surface-container/40 text-xs text-text outline-none focus:border-primary focus:bg-surface focus:ring-2 focus:ring-primary/20 transition"
                     @keyup.enter="handleSearchAndClose" />
              <button v-if="search" type="button"
                      class="absolute right-1.5 top-1/2 -translate-y-1/2 flex h-7 w-7 items-center justify-center rounded-full bg-primary text-white shadow-2xs hover:brightness-105 transition"
                      @click="handleSearchAndClose">
                <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" :d="icons.search" />
                </svg>
              </button>
            </div>
          </div>

          <div class="flex gap-2 px-4 py-3 border-b border-outline/10">
            <button type="button" class="flex-1 py-1.5 px-4 rounded-full text-xs font-semibold transition"
                    :class="searchSource === 'owner' ? 'bg-primary/15 text-primary' : 'bg-surface-container/60 text-text-secondary'"
                    @click="searchSource = 'owner'">
              我的订阅
            </button>
            <button type="button" class="flex-1 py-1.5 px-4 rounded-full text-xs font-semibold transition"
                    :class="searchSource === 'global' ? 'bg-primary/15 text-primary' : 'bg-surface-container/60 text-text-secondary'"
                    @click="searchSource = 'global'">
              全局发现
            </button>
          </div>

          <div class="flex-1 overflow-y-auto p-4">
            <div v-if="!search" class="space-y-4">
              <div v-if="recentSearches.length > 0">
                <h3 class="text-xs font-bold text-text-secondary uppercase tracking-wider mb-2.5">最近搜索</h3>
                <div class="space-y-1">
                  <button v-for="term in recentSearches" :key="term" type="button"
                          class="flex items-center gap-3 w-full px-3.5 py-2.5 rounded-2xl hover:bg-surface-container/60 transition text-left"
                          @click="searchFromHistory(term)">
                    <svg class="h-4 w-4 text-text-secondary" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span class="flex-1 text-xs font-medium text-text">{{ term }}</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 布局主体 -->
    <div class="flex" :class="isSidebarCollapsed ? 'lg:pl-16' : 'lg:pl-72'">
      <!-- 桌面端侧边抽屉：支持内容向右弹出菜单，解除外层 overflow-hidden -->
      <aside :class="[
        'fixed left-0 top-0 bottom-0 z-30 hidden lg:block transition-all duration-200 bg-surface ',
        isSidebarCollapsed ? 'w-18' : 'w-72'
      ]">
        <div class="flex h-full flex-col py-2">
          <!-- Logo 品牌区 -->
          <RouterLink :to="{ name: 'home' }"
                      class="flex items-center px-5 pt-1 pb-5 text-text transition select-none"
                      :class="isSidebarCollapsed ? 'justify-center px-0' : 'gap-3'">
            <img class="h-7 w-7 rounded-2xl shadow-xs dark:invert shrink-0" src="/logo.svg" alt="iFeed" />
            <span v-if="!isSidebarCollapsed" class="flex items-center gap-2 text-lg font-bold tracking-tight">
              IFeed
            </span>
          </RouterLink>

          <!-- A. 折叠迷你导航模式：Hover 订阅项向右弹出面板（使用 pl-2 连桥无缝衔接） -->
          <nav v-if="isSidebarCollapsed" class="flex-1 flex flex-col items-center space-y-2 px-1 overflow-visible">
            <!-- 首页 -->
            <RouterLink :to="{ name: 'home' }"
                        class="flex flex-col items-center justify-center w-14 py-2 rounded-2xl transition text-xs select-none"
                        :class="isActiveRoute('home') ? 'bg-primary/15 font-bold text-primary shadow-2xs' : 'text-text-secondary hover:bg-surface-container/60 hover:text-text'"
                        title="首页">
              <svg class="h-5 w-5 mb-1" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" :d="icons.home" />
              </svg>
              <span class="text-[10px]">首页</span>
            </RouterLink>

            <!-- 今日 -->
            <RouterLink :to="{ name: 'radar' }"
                        class="flex flex-col items-center justify-center w-14 py-2 rounded-2xl transition text-xs select-none"
                        :class="isActiveRoute('radar') ? 'bg-primary/15 font-bold text-primary shadow-2xs' : 'text-text-secondary hover:bg-surface-container/60 hover:text-text'"
                        title="今日">
              <svg class="h-5 w-5 mb-1" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" :d="icons.radar" />
              </svg>
              <span class="text-[10px]">今日</span>
            </RouterLink>

            <!-- 追踪 -->
            <RouterLink :to="{ name: 'topicTracking' }"
                        class="flex flex-col items-center justify-center w-14 py-2 rounded-2xl transition text-xs select-none"
                        :class="isActiveRoute('topicTracking') ? 'bg-primary/15 font-bold text-primary shadow-2xs' : 'text-text-secondary hover:bg-surface-container/60 hover:text-text'"
                        title="追踪">
              <svg class="h-5 w-5 mb-1" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" :d="icons.topic" />
              </svg>
              <span class="text-[10px]">追踪</span>
            </RouterLink>

            <!-- 订阅项：带透明连桥的 Hover 展开菜单 -->
            <div class="relative group/sub flex justify-center w-full">
              <!-- 触发器图标按钮 -->
              <RouterLink :to="{ name: 'feedsSubscriptions' }"
                          class="flex flex-col items-center justify-center w-14 py-2 rounded-2xl transition text-xs relative select-none"
                          :class="isActiveRoute('feedsSubscriptions') ? 'bg-primary/15 font-bold text-primary shadow-2xs' : 'text-text-secondary hover:bg-surface-container/60 hover:text-text'"
                          title="订阅">
                <svg class="h-5 w-5 mb-1" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" :d="icons.inbox" />
                </svg>
                <span class="text-[10px]">订阅</span>
                <span v-if="hasUnreadSubscriptions" class="absolute top-2 right-3 h-2 w-2 rounded-full bg-primary" />
              </RouterLink>

              <!-- 外层连接桥：pl-2 无缝承接鼠标判定 -->
              <div
                  class="invisible opacity-0 group-hover/sub:visible group-hover/sub:opacity-100 transition-all duration-200 ease-out transform scale-95 group-hover/sub:scale-100 absolute left-full top-0 pl-2 w-64 z-50 pointer-events-none group-hover/sub:pointer-events-auto">
                <div class="rounded-[24px] border border-outline/15 bg-surface/95 backdrop-blur-xl shadow-2xl p-2">
                  <div class="flex items-center justify-between px-3 py-2 border-b border-outline/10 mb-1">
                    <span class="text-xs font-bold text-text">我的订阅源</span>
                    <RouterLink :to="{ name: 'feedsSubscriptions' }" class="text-[11px] font-medium text-primary hover:underline">
                      查看全部 ({{ subscriptionsStore.items.length }})
                    </RouterLink>
                  </div>

                  <div class="max-h-80 overflow-y-auto space-y-0.5 pr-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">


                    <RouterLink
                        v-for="sub in subscriptionsStore.items"
                        :key="sub.feedId"
                        :to="{ name: 'feed', params: { feedId: sub.feedId } }"
                        class="flex items-center gap-2.5 px-2.5 py-1.5 rounded-xl text-xs text-text transition hover:bg-surface-container select-none group/item">
                      <img v-if="sub.avatar" :src="sub.avatar" class="h-6 w-6 shrink-0 rounded-full object-cover shadow-2xs" />
                      <span v-else class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-surface-container-high text-[10px] font-bold text-text-secondary">
                        {{ getSubscriptionInitials(sub) }}
                      </span>

                      <span class="truncate flex-1 font-medium group-hover/item:text-primary transition-colors">
                        {{ getSubscriptionLabel(sub) }}
                      </span>

                      <span v-if="!sub.isRead" class="h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
                    </RouterLink>

                    <div v-if="!subscriptionsStore.items.length" class="py-6 text-center text-xs text-text-secondary">
                      暂无订阅
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 我的 -->
            <RouterLink :to="{ name: 'upgrade' }"
                        class="flex flex-col items-center justify-center w-14 py-2 rounded-2xl transition text-xs select-none"
                        :class="isActiveRoute('upgrade') ? 'bg-primary/15 font-bold text-primary shadow-2xs' : 'text-text-secondary hover:bg-surface-container/60 hover:text-text'"
                        title="我的">
              <div class="relative mb-1">
                <img v-if="user && user.avatarUrl" :src="user.avatarUrl" class="h-5 w-5 rounded-full object-cover" />
                <div v-else
                     class="flex h-5 w-5 items-center justify-center rounded-full bg-surface-container-high text-text text-[9px] font-bold">
                  {{ userInitials }}
                </div>
              </div>
              <span class="text-[10px]">我的</span>
            </RouterLink>
          </nav>

          <!-- B. 展开完整侧边抽屉 -->
          <nav v-else class="flex-1 overflow-y-auto px-3 space-y-4">
            <div v-for="section in navSections" :key="section.id" class="space-y-1">
              <RouterLink v-if="section.title" :to="section.to"
                          class="flex items-center justify-between px-3.5 py-2 rounded-2xl text-xs font-bold uppercase tracking-wider transition select-none"
                          :class="isSectionActive(section)
                  ? 'bg-primary/10 text-primary'
                  : 'text-text-secondary hover:bg-surface-container/50 hover:text-text'">
                <span>{{ section.title }}</span>
                <svg class="h-3 w-3 opacity-60" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                </svg>
              </RouterLink>

              <div class="space-y-0.5">
                <component v-for="item in section.items" :is="item.to ? 'RouterLink' : 'button'" :key="item.id"
                           v-bind="item.to ? { to: item.to } : { type: 'button' }"
                           class="flex w-full items-center rounded-full py-2.5 px-3.5 text-xs font-medium transition gap-3 select-none"
                           :class="[
                    isActiveItem(item)
                      ? 'bg-primary/15 font-semibold text-primary'
                      : item.danger
                        ? 'text-red-600 dark:text-red-400 hover:bg-red-500/10'
                        : 'text-text hover:bg-surface-container/60'
                  ]" @click="handleNavItemClick(item)">
                  <span v-if="item.icon" class="flex h-5 w-5 items-center justify-center shrink-0">
                    <svg class="h-5 w-5" :viewBox="item.icon.viewBox ?? '0 0 24 24'"
                         :fill="item.icon.stroke ? 'none' : 'currentColor'"
                         :stroke="item.icon.stroke ? 'currentColor' : 'none'"
                         :stroke-width="item.icon.stroke ? 1.8 : undefined"
                         :stroke-linecap="item.icon.stroke ? 'round' : undefined"
                         :stroke-linejoin="item.icon.stroke ? 'round' : undefined">
                      <path v-for="path in item.icon.paths" :key="path" :d="path" />
                    </svg>
                  </span>
                  <img v-else-if="item.avatar" :src="item.avatar" class="h-5 w-5 rounded-full object-cover shrink-0 shadow-2xs" />
                  <span v-else-if="item.avatarText"
                        class="flex h-5 w-5 items-center justify-center rounded-full text-[10px] font-bold shrink-0 bg-surface-container-high text-text">
                    {{ item.avatarText }}
                  </span>
                  <span class="truncate flex-1 text-left">{{ item.label }}</span>
                  <span v-if="item.badge" class="ml-auto flex h-1.5 w-1.5 rounded-full bg-primary" />
                </component>
              </div>
            </div>
          </nav>
        </div>
      </aside>

      <!-- 主视图渲染容器 -->
      <main class="flex-1 min-w-0 sm:px-6 sm:pt-6" :class="[
        route.name !== 'article-detail' ? 'min-h-[calc(100vh-5em)] pb-24 lg:pb-24' : 'min-h-[calc(100vh-5em)] pb-28 lg:pb-24',
        route.name === 'feedsSubscriptions' ? 'pt-0 px-0' : 'px-1 pt-5'
      ]">
        <!-- 移动端横向订阅滑动栏 (仅 feedsSubscriptions 页面呈现) -->
        <div v-if="route.name === 'feedsSubscriptions' && subscriptionsStore.items.length > 0"
             class="lg:hidden border-b border-outline/10 py-3 bg-surface">
          <div class="overflow-x-scroll scrollbar-hide px-4" style="-webkit-overflow-scrolling: touch;">
            <div class="flex gap-4 py-1">
              <!-- 全部 -->
              <RouterLink :to="{ name: 'feedsSubscriptions' }"
                          class="flex flex-col items-center gap-1.5 shrink-0 w-12 group select-none">
                <div class="relative">
                  <div
                      class="w-10 h-10 rounded-full flex items-center justify-center text-xs font-bold transition-all shadow-xs"
                      :class="!route.query.feedId
                      ? 'bg-primary text-white ring-2 ring-primary/20'
                      : 'bg-surface-container text-text-secondary group-hover:bg-surface-container-high'">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h16" />
                    </svg>
                  </div>
                </div>
                <span class="text-[11px] text-center text-text font-medium leading-tight">全部</span>
              </RouterLink>

              <RouterLink v-for="subscription in subscriptionsStore.items" :key="subscription.feedId"
                          :to="{ name: 'feedsSubscriptions', query: { feedId: subscription.feedId } }"
                          class="flex flex-col items-center gap-1.5 shrink-0 w-12 group select-none">
                <div class="relative">
                  <img v-if="subscription.avatar && !subscriptionAvatarErrors[subscription.feedId]"
                       :src="subscription.avatar" :alt="getSubscriptionLabel(subscription)"
                       class="w-10 h-10 rounded-full object-cover transition-all shadow-xs"
                       :class="route.query.feedId === subscription.feedId ? 'ring-2 ring-primary ring-offset-2 ring-offset-surface' : ''"
                       @error="subscriptionAvatarErrors[subscription.feedId] = true" />
                  <div v-else
                       class="w-10 h-10 rounded-full flex items-center justify-center text-xs font-bold transition-all shadow-xs bg-surface-container-high text-text"
                       :class="route.query.feedId === subscription.feedId ? 'ring-2 ring-primary ring-offset-2 ring-offset-surface' : ''">
                    {{ getSubscriptionInitials(subscription) }}
                  </div>

                  <span v-if="route.query.feedId === subscription.feedId"
                        class="absolute bottom-0 right-0 w-3 h-3 bg-primary rounded-full ring-2 ring-surface flex items-center justify-center">
                    <svg class="w-2 h-2 text-white" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </span>
                  <span v-else-if="!subscription.isRead"
                        class="absolute bottom-0 right-0 w-2.5 h-2.5 bg-primary rounded-full ring-2 ring-surface" />
                </div>

                <span class="text-[11px] text-center line-clamp-1 w-full leading-tight transition-colors"
                      :class="route.query.feedId === subscription.feedId ? 'text-primary font-bold' : 'text-text-secondary'">
                  {{ getSubscriptionLabel(subscription) }}
                </span>
              </RouterLink>
            </div>
          </div>
        </div>

        <div :class="route.name === 'feedsSubscriptions' ? 'px-1 pt-5 sm:px-6 sm:pt-6' : ''">
          <global-audio-player>
            <router-view v-slot="{ Component }">
              <KeepAlive
                  :include="['HomePage', 'FeedSubscriptionsPage', 'FeedDetailPage', 'SearchPage', 'ArticleDetailPage', 'SubscriptionDiscoveryPage', 'CollectionsPage', 'LikesPage', 'HistoryPage']">
                <component :is="Component" />
              </KeepAlive>
            </router-view>
          </global-audio-player>
        </div>
      </main>
    </div>

    <!-- 移动端底部导航栏：MD3 Navigation Bar -->
    <nav v-show="route.name !== 'article'"
         class="lg:hidden fixed bottom-0 left-0 right-0 z-40 bg-surface/95 backdrop-blur-lg border-t border-outline/10 pb-safe">
      <div class="flex items-center justify-around h-16 px-3">
        <!-- 首页 Tab -->
        <RouterLink :to="{ name: 'home' }"
                    class="flex flex-col items-center justify-center flex-1 py-1 transition select-none"
                    :class="isTabActive('home') ? 'text-primary' : 'text-text-secondary'">
          <div class="flex items-center justify-center px-4 py-1 rounded-full transition"
               :class="isTabActive('home') ? 'bg-primary/15' : ''">
            <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" :d="icons.home" />
            </svg>
          </div>
          <span class="text-[10px] font-semibold mt-0.5">首页</span>
        </RouterLink>

        <!-- 追踪 Tab -->
        <RouterLink :to="{ name: 'topicTracking' }"
                    class="flex flex-col items-center justify-center flex-1 py-1 transition select-none"
                    :class="isTabActive('topicTracking') ? 'text-primary' : 'text-text-secondary'">
          <div class="flex items-center justify-center px-4 py-1 rounded-full transition"
               :class="isTabActive('topicTracking') ? 'bg-primary/15' : ''">
            <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" :d="icons.topic" />
            </svg>
          </div>
          <span class="text-[10px] font-semibold mt-0.5">追踪</span>
        </RouterLink>

        <!-- 发现 Tab -->
        <RouterLink :to="{ name: 'discover' }"
                    class="flex flex-col items-center justify-center flex-1 py-1 -mt-3 transition select-none">
          <div class="flex items-center justify-center h-12 w-12 rounded-full shadow-md transition-all active:scale-95"
               :class="isTabActive('discover') ? 'bg-primary text-white ring-2 ring-primary/20' : 'bg-surface-container-high text-text'">
            <svg class="h-6 w-6 stroke-[2.2]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" :d="icons.plus" />
            </svg>
          </div>
        </RouterLink>

        <!-- 订阅 Tab -->
        <RouterLink :to="{ name: 'feedsSubscriptions' }"
                    class="flex flex-col items-center justify-center flex-1 py-1 transition relative select-none"
                    :class="isTabActive('feedsSubscriptions') ? 'text-primary' : 'text-text-secondary'">
          <div class="flex items-center justify-center px-4 py-1 rounded-full transition"
               :class="isTabActive('feedsSubscriptions') ? 'bg-primary/15' : ''">
            <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" :d="icons.inbox" />
            </svg>
          </div>
          <span class="text-[10px] font-semibold mt-0.5">订阅</span>
          <span v-if="hasUnreadSubscriptions" class="absolute top-1.5 right-6 h-2 w-2 rounded-full bg-primary" />
        </RouterLink>

        <!-- 我的 Tab -->
        <RouterLink :to="{ name: 'upgrade' }"
                    class="flex flex-col items-center justify-center flex-1 py-1 transition select-none"
                    :class="isTabActive('upgrade') ? 'text-primary' : 'text-text-secondary'">
          <div class="flex items-center justify-center px-4 py-1 rounded-full transition"
               :class="isTabActive('upgrade') ? 'bg-primary/15' : ''">
            <img v-if="user && user.avatarUrl" :src="user.avatarUrl" class="h-5 w-5 rounded-full object-cover" />
            <div v-else
                 class="flex h-5 w-5 items-center justify-center rounded-full bg-surface-container-high text-[9px] font-bold text-text">
              {{ userInitials }}
            </div>
          </div>
          <span class="text-[10px] font-semibold mt-0.5">我的</span>
        </RouterLink>
      </div>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { RouteLocationNormalizedLoaded, RouteLocationRaw } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useAuthStore } from '../stores/auth';
import { useThemeStore } from '../stores/theme';
import { useSubscriptionsStore } from '../stores/subscriptions';
import { useMixFeedsStore } from "../stores/mixFeeds";
import GlobalAudioPlayer from "../components/GlobalAudioPlayer.vue";

const icons = {
  menu: 'M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5',
  home: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6',
  inbox: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10',
  clock: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z',
  bookmark: 'M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z',
  topic: 'M9 3 6 21M18 3l-3 18M3 9h18M2 15h18',
  radar: 'M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.5 4 6.5 2 2 3 4 3 6.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a6.953 6.953 0 0 0 1.5 1z',
  chevronUp: 'M5 15l7-7 7 7',
  chevronDown: 'M19 9l-7 7-7-7',
  search: 'M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z',
  plus: 'M12 4.5v15m7.5-7.5h-15'
};

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const themeStore = useThemeStore();
const subscriptionsStore = useSubscriptionsStore();
const mixFeedStore = useMixFeedsStore();

const { user } = storeToRefs(authStore);
const { isDark } = storeToRefs(themeStore);

const searchSourceRef = ref<HTMLElement | null>(null);
const isSidebarCollapsed = ref(localStorage.getItem('sidebar-collapsed') === 'true');
const search = ref('');
const searchFocused = ref(false);
const searchSource = ref<'owner' | 'global'>('owner');
const showSourceDropdown = ref(false);
const showMobileSearch = ref(false);

const isHeaderHidden = ref(false);
const lastScrollY = ref(0);
const mobileSearchInput = ref<HTMLInputElement | null>(null);
const recentSearches = ref<string[]>(JSON.parse(localStorage.getItem('recent-searches') || '[]'));
const subscriptionAvatarErrors = reactive<Record<string, boolean>>({});

type NavIcon = {
  paths: string[];
  stroke?: boolean;
  viewBox?: string;
};

type NavItem = {
  id: string;
  label: string;
  to?: RouteLocationRaw;
  icon?: NavIcon;
  avatarText?: string;
  avatar?: string;
  accent?: string;
  badge?: string;
  danger?: boolean;
  activeMatch?: (current: RouteLocationNormalizedLoaded) => boolean;
  action?: () => void;
};

type NavSection = {
  id: string;
  title?: string;
  to?: RouteLocationRaw;
  activeMatch?: (current: RouteLocationNormalizedLoaded) => boolean;
  items: NavItem[];
};

const baseNavSections: NavSection[] = [
  {
    id: 'primary',
    items: [
      {
        id: 'home',
        label: '首页',
        to: { name: 'home' as const },
        icon: { stroke: true, paths: [icons.home], viewBox: '0 0 24 24' },
        activeMatch: (current) => {
          const view = current.query.view as string | undefined;
          const section = current.query.section as string | undefined;
          return current.name === 'home' && view !== 'shorts' && !section;
        }
      },
      {
        id: 'radar',
        label: '今日',
        to: { name: 'radar' as const },
        icon: { stroke: true, paths: [icons.radar], viewBox: '0 0 24 24' },
        activeMatch: (current) => current.name === 'radar' || current.name === 'radarTopic'
      },
      {
        id: 'topic-tracking',
        label: '追踪',
        to: { name: 'topicTracking' as const },
        icon: { stroke: true, paths: [icons.topic], viewBox: '0 0 24 24' },
        activeMatch: (current) => current.name === 'topicTracking'
      }
    ]
  }
];

const showAllSubscriptions = ref(false);

const subscriptionNavSection = computed<NavSection>(() => {
  const entries = subscriptionsStore.items.map((s) => {
    const label =
        s.title?.trim() ||
        (() => {
          const candidate = s.siteUrl || s.url;
          if (!candidate) return '订阅源';
          try {
            return new URL(candidate).hostname || candidate;
          } catch {
            return candidate;
          }
        })();

    const initials = Array.from(label).slice(0, 2).join('').toUpperCase() || 'F';
    const danger = Boolean((s.failureCount ?? 0) > 0 || s.fetchError?.trim());

    return { subscription: s, label, initials, danger };
  });

  entries.sort((a, b) => {
    if (a.danger === b.danger) return 0;
    return a.danger ? 1 : -1;
  });

  const visibleEntries = showAllSubscriptions.value ? entries : entries.slice(0, 7);

  const items: NavItem[] = visibleEntries.map((meta) => {
    const s = meta.subscription;
    return {
      id: `sub-${s.feedId}`,
      label: meta.label,
      to: { name: 'feed' as const, params: { feedId: s.feedId } },
      avatar: s.avatar,
      avatarText: meta.initials,
      activeMatch: (current) => {
        const paramId = typeof current.params.feedId === 'string' ? current.params.feedId : undefined;
        return current.name === 'feed' && paramId === s.feedId;
      },
      badge: s.isRead ? '' : '1',
      danger: meta.danger
    };
  });

  if (entries.length > 7) {
    items.push({
      id: 'toggle-subscriptions',
      label: showAllSubscriptions.value ? '折叠' : `展开（${entries.length}）`,
      action: () => {
        showAllSubscriptions.value = !showAllSubscriptions.value;
      },
      icon: {
        stroke: true,
        paths: [showAllSubscriptions.value ? icons.chevronUp : icons.chevronDown],
        viewBox: '0 0 24 24'
      }
    } as unknown as NavItem);
  }

  return {
    id: 'subscriptions',
    title: '订阅',
    to: { name: 'feedsSubscriptions' as const },
    activeMatch: (current) => current.name === 'feedsSubscriptions',
    items
  };
});

const navSections = computed<NavSection[]>(() => {
  return [
    ...baseNavSections,
    subscriptionNavSection.value,
    {
      id: 'you',
      title: '我',
      to: { name: 'upgrade' as const },
      activeMatch: (current) => current.name === 'upgrade',
      items: [
        {
          id: 'history',
          label: '历史记录',
          to: { name: 'history' as const },
          icon: { stroke: true, paths: [icons.clock], viewBox: '0 0 24 24' },
          activeMatch: (current) => current.name === 'history'
        },
        {
          id: 'library',
          label: '收藏夹',
          to: { name: 'collections' as const },
          icon: { stroke: true, paths: [icons.bookmark], viewBox: '0 0 24 24' },
          activeMatch: (current) => {
            const tab = current.query.tab as string | undefined;
            return current.name === 'collections' && !tab;
          }
        },
        {
          id: 'likes',
          label: '喜欢',
          to: { name: 'likes' as const },
          icon: {
            stroke: true,
            paths: ['M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z'],
            viewBox: '0 0 24 24'
          },
          activeMatch: (current) => current.name === 'likes'
        }
      ]
    }
  ];
});

const handleNavItemClick = (item: NavItem) => {
  if (item.action) {
    item.action();
  }
};

const isActiveItem = (item: NavItem) => {
  if (item.activeMatch) {
    return item.activeMatch(route);
  }
  if (!item.to) {
    return false;
  }
  const resolved = router.resolve(item.to);
  if (resolved.name && resolved.name === route.name) {
    return true;
  }
  return resolved.path === route.path;
};

const userInitials = computed(() => {
  const username = user.value?.username ?? '';
  if (!username) return 'U';
  return username
      .split(/\s+/)
      .map((part) => part.charAt(0).toUpperCase())
      .join('')
      .slice(0, 2);
});

const toggleSourceDropdown = () => {
  showSourceDropdown.value = !showSourceDropdown.value;
};

const selectSource = (source: 'owner' | 'global') => {
  searchSource.value = source;
  showSourceDropdown.value = false;
};

const openSearchPage = () => {
  showMobileSearch.value = true;
  setTimeout(() => {
    mobileSearchInput.value?.focus();
  }, 100);
};

const closeMobileSearch = () => {
  showMobileSearch.value = false;
};

const handleSearchAndClose = () => {
  handleSearch();
  const keyword = search.value.trim();
  if (keyword) {
    const searches = recentSearches.value.filter(s => s !== keyword);
    searches.unshift(keyword);
    recentSearches.value = searches.slice(0, 10);
    localStorage.setItem('recent-searches', JSON.stringify(recentSearches.value));
  }
  closeMobileSearch();
};

const searchFromHistory = (term: string) => {
  search.value = term;
  handleSearchAndClose();
};

const isTabActive = (tabName: string): boolean => {
  if (tabName === 'home') {
    const view = route.query.view as string | undefined;
    const section = route.query.section as string | undefined;
    return route.name === 'home' && view !== 'shorts' && !section;
  }
  return route.name === tabName;
};

const hasUnreadSubscriptions = computed(() => {
  return subscriptionsStore.items.some(s => !s.isRead);
});

const getSubscriptionLabel = (subscription: any): string => {
  if (subscription.title?.trim()) {
    return subscription.title.trim();
  }
  const candidate = subscription.siteUrl || subscription.url;
  if (!candidate) return '订阅源';
  try {
    return new URL(candidate).hostname || candidate;
  } catch {
    return candidate;
  }
};

const getSubscriptionInitials = (subscription: any): string => {
  const label = getSubscriptionLabel(subscription);
  return Array.from(label).slice(0, 2).join('').toUpperCase() || 'F';
};

const handleSearch = () => {
  const keyword = search.value.trim();
  const currentType =
      route.name === 'search' && typeof route.query.type === 'string' ? route.query.type : undefined;

  const feedId = typeof route.query.feedId === 'string' ? route.query.feedId : undefined;
  const tag = typeof route.query.tags === 'string' ? route.query.tags : undefined;
  const category = typeof route.query.category === 'string' ? route.query.category : undefined;

  if (!keyword) {
    const query: Record<string, string> = {};
    if (feedId) query.feedId = feedId;
    if (tag) query.tags = tag;
    if (category) query.category = category;
    router.push({ name: 'home', query });
    return;
  }

  const query: Record<string, string> = {
    q: keyword,
    source: searchSource.value
  };
  if (currentType === 'keyword') query.type = currentType;
  if (feedId) query.feedId = feedId;
  if (tag) query.tags = tag;
  if (category) query.category = category;
  router.push({ name: 'search', query });
};

const handleLogout = async () => {
  await authStore.logout();
  router.replace({ name: 'auth' });
};

const toggleTheme = () => {
  themeStore.toggle();
};

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value;
  localStorage.setItem('sidebar-collapsed', String(isSidebarCollapsed.value));
};

const isSectionActive = (section: NavSection): boolean => {
  if (section.activeMatch) {
    return section.activeMatch(route);
  }
  if (!section.to) {
    return false;
  }
  const resolved = router.resolve(section.to);
  if (resolved.name && resolved.name === route.name) {
    return true;
  }
  return resolved.path === route.path;
};

const isActiveRoute = (name: string): boolean => {
  if (name === 'home') {
    return route.name === 'home' && !route.query.view && !route.query.section;
  }
  return route.name === name;
};

watch(
    () => route.query.q,
    (value) => {
      search.value = typeof value === 'string' ? value : '';
    },
    { immediate: true }
);

const handleScroll = () => {
  if (typeof window === 'undefined') return;
  const current = window.scrollY || window.pageYOffset || 0;
  const delta = current - lastScrollY.value;

  if (Math.abs(delta) > 5) {
    if (current > 80 && delta > 0) {
      isHeaderHidden.value = true;
    } else if (delta < 0) {
      isHeaderHidden.value = false;
    }
  }

  lastScrollY.value = current;
};

watch(showSourceDropdown, (isOpen) => {
  if (isOpen) {
    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as HTMLElement;
      if (searchSourceRef.value && !searchSourceRef.value.contains(target)) {
        showSourceDropdown.value = false;
        document.removeEventListener('click', handleClickOutside);
      }
    };
    setTimeout(() => {
      document.addEventListener('click', handleClickOutside);
    }, 0);
  }
});

onMounted(async () => {
  if (typeof window !== 'undefined') {
    lastScrollY.value = window.scrollY || window.pageYOffset || 0;
    window.addEventListener('scroll', handleScroll, { passive: true });
  }
  if (authStore.isAuthenticated) {
    await subscriptionsStore.fetchSubscriptions();
    mixFeedStore.clearMyMixFeeds();
  }
});

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('scroll', handleScroll);
  }
});
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

nav::-webkit-scrollbar {
  width: 4px;
}

nav::-webkit-scrollbar-track {
  background: transparent;
}

nav::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.25);
  border-radius: 9999px;
}

nav::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.4);
}

.search-slide-enter-active,
.search-slide-leave-active {
  transition: transform 0.28s cubic-bezier(0.2, 0, 0, 1);
}

.search-slide-enter-from {
  transform: translateY(-100%);
}

.search-slide-leave-to {
  transform: translateY(-100%);
}

.pb-safe {
  padding-bottom: env(safe-area-inset-bottom);
}

.pt-safe {
  padding-top: env(safe-area-inset-top);
}

.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
</style>