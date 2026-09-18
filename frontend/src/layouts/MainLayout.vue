<template>
  <div class="min-h-screen text-gray-900 dark:text-gray-100 transition-colors duration-300 pt-safe">

    <!-- Header -->
    <header class="sticky top-0 z-30 bg-white/95 dark:bg-surface/95 backdrop-blur-sm transition-transform duration-200"
      :class="[
        isSidebarCollapsed ? 'lg:pl-16' : 'lg:pl-72',
        isHeaderHidden ? '-translate-y-full lg:translate-y-0' : 'translate-y-0',
        !isAtTop ? '' : '',
        route.name === 'home' || route.name === 'article' ? '' : 'hidden lg:block'
      ]">
      <div class="flex items-center justify-between gap-3 px-4 py-3 lg:px-5 lg:py-2">
        <!-- Left: Logo (mobile) + Sidebar toggle (desktop) -->
        <div class="flex items-center gap-3">
          <!-- Mobile Logo -->
          <RouterLink :to="{ name: 'home' }" class="flex items-center gap-2 lg:hidden">
            <img class="h-8 w-8 rounded-2xl" src="/logo.svg" alt="iFeed" />
            <span class="text-lg font-semibold">IFeed</span>
          </RouterLink>

          <!-- Desktop sidebar toggle -->
          <button type="button"
            class="hidden lg:flex h-10 w-10 items-center justify-center rounded-full hover:bg-surface-container/60 transition"
            @click="toggleSidebar">
            <svg class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" :d="icons.menu" />
            </svg>
          </button>
        </div>

        <!-- Center: Desktop Search Bar -->
        <div class="hidden lg:flex flex-1 max-w-2xl mx-auto">
          <div class="relative w-full group">
            <input v-model="search" type="search" placeholder="搜索文章、标签、订阅..."
              class="w-full h-10 pl-24 pr-14 rounded-full border bg-white dark:bg-gray-900 text-base transition-all duration-200"
              :class="[
                searchFocused
                  ? 'border-transparent shadow-lg ring-1 ring-gray-300 dark:ring-gray-700'
                  : 'border-gray-300 dark:border-gray-700 hover:shadow-md hover:border-gray-400 dark:hover:border-gray-600'
              ]" @focus="searchFocused = true" @blur="searchFocused = false" @keyup.enter="handleSearch" />

            <!-- Source Selector (replaces search icon) -->
            <div class="absolute left-3 top-1/2 -translate-y-1/2" ref="searchSourceRef">
              <button type="button"
                class="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-medium transition-colors hover:bg-surface-container/60"
                :class="searchFocused ? 'text-primary' : 'text-gray-600 dark:text-gray-400'"
                @click="toggleSourceDropdown">
                {{ searchSource === 'owner' ? '订阅' : '发现' }}
                <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              <!-- Dropdown -->
              <div v-if="showSourceDropdown"
                class="absolute top-full mt-1 left-0 w-20 bg-white dark:bg-gray-900 rounded-lg shadow-lg ring-1 ring-gray-200 dark:ring-gray-800 overflow-hidden z-50">
                <button type="button"
                  class="w-full px-3 py-2 text-xs text-left hover:bg-surface-container/60 transition"
                  :class="{ 'bg-gray-50 dark:bg-gray-850 font-medium': searchSource === 'owner' }"
                  @click="selectSource('owner')">
                  订阅
                </button>
                <button type="button"
                  class="w-full px-3 py-2 text-xs text-left hover:bg-surface-container/60 transition"
                  :class="{ 'bg-gray-50 dark:bg-gray-850 font-medium': searchSource === 'global' }"
                  @click="selectSource('global')">
                  发现
                </button>
              </div>
            </div>

            <!-- Search Button -->
            <button type="button"
              class="absolute right-2 top-1/2 -translate-y-1/2 flex h-8 w-8 items-center justify-center rounded-full transition-colors"
              :class="search ? 'bg-primary text-primary-foreground hover:bg-primary/90' : 'text-gray-400 hover:bg-surface-container/60'"
              @click="handleSearch">
              <svg class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" :d="icons.search" />
              </svg>
            </button>
          </div>
        </div>

        <!-- Right: header-action + actions (shared for mobile & desktop) -->
        <div class="flex items-center gap-2 min-w-[12rem] justify-end">
          <!-- Teleport 挂载点：移动端和桌面端共用 -->
          <div class="flex items-center gap-2" id="header-action"></div>

          <!-- Mobile search + avatar -->
          <div v-show="route.name != 'article'" class="flex items-center gap-2 lg:hidden">
            <button type="button"
              class="flex h-10 w-10 items-center justify-center rounded-full hover:bg-surface-container/60 transition"
              @click="openSearchPage">
              <svg class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" :d="icons.search" />
              </svg>
            </button>
            <router-link to="/upgrade">
              <img v-if="user && user.avatarUrl" :src="user.avatarUrl" class="h-9 w-9 rounded-full bg-primary" />
              <div v-else
                class="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-semibold">
                {{ userInitials }}
              </div>
            </router-link>
          </div>

          <!-- Desktop actions -->
          <div v-show="route.name != 'article'" class="hidden lg:flex items-center gap-2">
            <!-- Theme Toggle -->
            <button type="button"
              class="flex h-10 w-10 items-center justify-center rounded-full hover:bg-secondary/20 transition"
              @click="toggleTheme">
              <svg v-if="isDark" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.5"
                viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M21.752 15.002A9.718 9.718 0 0118 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 003 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 009.002-5.998z" />
              </svg>
              <svg v-else class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M12 3v2.25m6.364.386l-1.591 1.591M21 12h-2.25m-.386 6.364l-1.591-1.591M12 18.75V21m-4.773-4.227l-1.591 1.591M5.25 12H3m4.227-4.773L5.636 5.636M15.75 12a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0z" />
              </svg>
            </button>

            <!-- Add Subscription Button -->
            <RouterLink :to="{ name: 'discover' }"
              class="hidden sm:flex items-center gap-2 h-10 px-4 rounded-full bg-secondary/5 text-secondary hover:bg-secondary/20 text-sm font-medium transition">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 5v14M5 12h14"></path>
              </svg>
              订阅
            </RouterLink>

            <RouterLink :to="{ name: 'discover' }"
              class="flex sm:hidden h-10 w-10 items-center justify-center rounded-full hover:bg-secondary/20 transition">
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 5v14M5 12h14"></path>
              </svg>
            </RouterLink>

            <!-- User Menu (YouTube Style - Pure CSS Hover) -->
            <div class="relative group hidden md:block">
              <router-link to="/upgrade">
                <img v-if="user && user.avatarUrl" :src="user.avatarUrl" class="h-9 w-9 rounded-full bg-primary" />
                <div v-else
                  class="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-semibold cursor-pointer group-hover:ring-2 group-hover:ring-primary/30 transition-all">
                  {{ userInitials }}
                </div>
              </router-link>
              <!-- Dropdown Menu -->
              <div
                class="absolute right-0 mt-2 w-64 origin-top-right rounded-xl bg-white dark:bg-gray-900 shadow-lg ring-1 ring-black ring-opacity-5 overflow-hidden z-50 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 transform scale-95 group-hover:scale-100">
                <!-- User Info Section -->
                <div class="px-4 py-3 border-b border-gray-200 dark:border-gray-800">
                  <div class="flex items-center gap-3">
                    <img v-if="user && user.avatarUrl" :src="user.avatarUrl" class="h-10 w-10 rounded-full bg-primary" />
                    <div v-else
                      class="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-semibold">
                      {{ userInitials }}
                    </div>
                    <div class="flex-1 min-w-0">
                      <p class="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">
                        {{ user?.username ?? '访客' }}
                      </p>
                      <p class="text-xs text-gray-500 dark:text-gray-400 truncate">
                        {{ user?.email ?? '' }}
                      </p>
                    </div>
                  </div>
                </div>

                <!-- Menu Items -->
                <div class="py-1">
                  <RouterLink to="/upgrade"
                    class="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 dark:text-gray-300 hover:bg-surface-container/60 transition">
                    <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
                    </svg>
                    升级套餐
                  </RouterLink>

                  <RouterLink to="/feeds/channels"
                    class="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 dark:text-gray-300 hover:bg-surface-container/60 transition">
                    <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z" />
                      <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                    管理订阅
                  </RouterLink>
                </div>

                <!-- Logout Section -->
                <div v-if="user" class="border-t border-gray-200 dark:border-gray-800 py-1">
                  <button type="button"
                    class="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-red-600 dark:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/20 transition"
                    @click="handleLogout">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1">
                      </path>
                    </svg>
                    退出登录
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- Full Screen Search Page (Mobile) -->
    <transition name="search-slide">
      <div v-if="showMobileSearch" class="fixed inset-0 z-50 bg-white dark:bg-gray-950 lg:hidden">
        <div class="flex flex-col h-full">
          <!-- Search Header -->
          <div class="flex items-center gap-3 px-4 py-3 border-b border-gray-200 dark:border-gray-800">
            <button type="button"
              class="flex h-10 w-10 items-center justify-center rounded-full hover:bg-surface-container/60 transition"
              @click="closeMobileSearch">
              <svg class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div class="flex-1 relative">
              <input ref="mobileSearchInput" v-model="search" type="search" placeholder="搜索文章、标签、订阅..."
                class="w-full h-11 pl-4 pr-12 rounded-full border border-gray-300 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition"
                @keyup.enter="handleSearchAndClose" />
              <button v-if="search" type="button"
                class="absolute right-2 top-1/2 -translate-y-1/2 flex h-8 w-8 items-center justify-center rounded-full bg-primary text-white hover:bg-primary/90 transition"
                @click="handleSearchAndClose">
                <svg class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" :d="icons.search" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Search Source Tabs -->
          <div class="flex gap-2 px-4 py-3 border-b border-gray-200 dark:border-gray-800">
            <button type="button" class="flex-1 py-2 px-4 rounded-full text-sm font-medium transition"
              :class="searchSource === 'owner' ? 'bg-primary text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300'"
              @click="searchSource = 'owner'">
              我的订阅
            </button>
            <button type="button" class="flex-1 py-2 px-4 rounded-full text-sm font-medium transition"
              :class="searchSource === 'global' ? 'bg-primary text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300'"
              @click="searchSource = 'global'">
              全局发现
            </button>
          </div>

          <!-- Search Content -->
          <div class="flex-1 overflow-y-auto p-4">
            <div v-if="!search" class="space-y-6">
              <!-- Recent Searches -->
              <div v-if="recentSearches.length > 0">
                <h3 class="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3">最近搜索</h3>
                <div class="space-y-2">
                  <button v-for="term in recentSearches" :key="term" type="button"
                    class="flex items-center gap-3 w-full px-4 py-3 rounded-lg hover:bg-surface-container/60 transition"
                    @click="searchFromHistory(term)">
                    <svg class="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" stroke-width="2"
                      viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span class="flex-1 text-left text-gray-700 dark:text-gray-300">{{ term }}</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- Layout Container -->
    <div class="flex" :class="isSidebarCollapsed ? 'lg:pl-16' : 'lg:pl-72'">
      <!-- Desktop Sidebar -->
      <aside :class="[
        'fixed left-0 top-[0px] bottom-0 z-30 hidden lg:block border-r border-gray-200 dark:border-gray-800 transition-all duration-200',
        isSidebarCollapsed ? 'w-15' : 'w-72'
      ]">
        <div class="flex h-full flex-col overflow-hidden py-4">

          <RouterLink :to="{ name: 'home' }"
            class="flex items-center text-lg  px-4 pt-0 pb-5 text-gray-700 dark:text-gray-300"
            :class="isSidebarCollapsed ? 'justify-center px-2' : 'gap-3 px-3'">
            <img class="h-7 w-7 rounded-2xl opacity-70 dark:invert" src="/logo.svg" alt="iFeed" />

            <span v-if="!isSidebarCollapsed" class="flex items-center gap-2">
              IFeed
              <span>
                <span
                  class="bg-secondary text-white shadow-md  rounded-full px-1 text-[8px]  opacity-60 font-medium ">Beta</span>
              </span>
            </span>
          </RouterLink>
          <nav class="flex-1 overflow-y-auto px-2 " :class="{ 'space-y-1': isSidebarCollapsed }">
            <div v-for="(section, index) in navSections" :key="section.id" class="mb-3  space-y-0.5"
              v-show="!(isSidebarCollapsed && section?.id == 'subscriptions')">
              <RouterLink v-if="section.title && !isSidebarCollapsed" :to="section.to"
                class="flex items-center gap-1 px-3 py-2.5 rounded-lg text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 transition-colors duration-150 cursor-pointer"
                :class="[
                  isSectionActive(section)
                    ? 'bg-primary/10 dark:bg-primary/20 text-text font-semibold '
                    : 'text-gray-700 dark:text-gray-300 hover:bg-surface-container/60']">
                <span>{{ section.title }}</span>
                <svg class="h-3 w-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                </svg>
              </RouterLink>
              <div class="space-y-0.5">
                <component v-for="item in section.items" :is="item.to ? 'RouterLink' : 'button'" :key="item.id"
                  v-bind="item.to ? { to: item.to } : { type: 'button' }"
                  class="flex w-full items-center rounded-lg py-2.5 text-sm font-medium transition" :class="[
                    isSidebarCollapsed ? 'justify-center px-2' : 'gap-3 px-3',
                    isActiveItem(item)
                      ? 'bg-primary/10 dark:bg-primary/20 text-text font-semibold '
                      : item.danger
                        ? 'text-red-500 hover:bg-red-50 dark:hover:bg-red-950'
                        : 'text-gray-700 dark:text-gray-300 hover:bg-surface-container/60'
                  ]" :title="isSidebarCollapsed ? item.label : undefined" @click="handleNavItemClick(item)">
                  <span v-if="item.icon" class="flex h-6 w-6 items-center justify-center rounded-lg">
                    <svg class="h-6 w-6" :viewBox="item.icon.viewBox ?? '0 0 24 24'"
                      :fill="item.icon.stroke ? 'none' : 'currentColor'"
                      :stroke="item.icon.stroke ? 'currentColor' : 'none'"
                      :stroke-width="item.icon.stroke ? 1.5 : undefined"
                      :stroke-linecap="item.icon.stroke ? 'round' : undefined"
                      :stroke-linejoin="item.icon.stroke ? 'round' : undefined">
                      <path v-for="path in item.icon.paths" :key="path" :d="path" />
                    </svg>
                  </span>
                  <img v-else-if="item.avatar" :src="item.avatar" class="h-6 w-6 rounded-full " />
                  <span v-else-if="item.avatarText"
                    class="flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold"
                    :class="item.accent ?? 'bg-primary text-primary-foreground'">
                    {{ item.avatarText }}
                  </span>
                  <span v-if="!isSidebarCollapsed" class="truncate">{{ item.label }}</span>
                  <span v-if="item.badge && !isSidebarCollapsed" class="ml-auto flex h-1 w-1 rounded-full bg-primary" />
                </component>
              </div>
            </div>
          </nav>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="flex-1 min-w-0 sm:px-6 sm:pt-6" :class="[
        route.name !== 'article-detail' ? 'min-h-[calc(100vh-5em)] pb-24 lg:pb-24' : 'min-h-[calc(100vh-5em)] pb-28 lg:pb-24',
        route.name === 'feedsSubscriptions' ? 'pt-0 px-0' : 'px-1 pt-5'
      ]">
        <!-- Subscriptions Horizontal Scroll (only on feedsSubscriptions page) -->
        <div v-if="route.name === 'feedsSubscriptions' && subscriptionsStore.items.length > 0"
          class="lg:hidden border-b border-gray-200 dark:border-gray-800 py-3">
          <div class="overflow-x-scroll scrollbar-hide px-4" style="-webkit-overflow-scrolling: touch;">
            <div class="flex gap-5 py-1">
              <!-- 全部 -->
              <RouterLink
                :to="{ name: 'feedsSubscriptions' }"
                class="flex flex-col items-center gap-1.5 flex-shrink-0 w-[3rem] group">
                <div class="relative">
                  <div
                    class="w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold ring-2 transition-all"
                    :class="!route.query.feedId
                      ? 'bg-primary text-primary-foreground ring-primary ring-offset-2'
                      : 'bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400 ring-transparent group-hover:ring-gray-300 dark:group-hover:ring-gray-700'">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h16" />
                    </svg>
                  </div>
                </div>
                <span class="text-[11px] text-center text-gray-700 dark:text-gray-300 leading-tight">全部</span>
              </RouterLink>

              <RouterLink v-for="subscription in subscriptionsStore.items" :key="subscription.feedId"
                :to="{ name: 'feedsSubscriptions', query: { feedId: subscription.feedId } }"
                class="flex flex-col items-center gap-1.5 flex-shrink-0 w-[3rem] group">
                <!-- Avatar with Badge -->
                <div class="relative">
                  <img v-if="subscription.avatar && !subscriptionAvatarErrors[subscription.feedId]"
                    :src="subscription.avatar" :alt="getSubscriptionLabel(subscription)"
                    class="w-9 h-9 rounded-full object-cover ring-2 transition-all"
                    :class="route.query.feedId === subscription.feedId
                      ? 'ring-primary ring-offset-2'
                      : !subscription.isRead
                        ? 'ring-primary ring-offset-2'
                        : 'ring-transparent group-hover:ring-gray-300 dark:group-hover:ring-gray-700'"
                    @error="subscriptionAvatarErrors[subscription.feedId] = true" />
                  <div v-else
                    class="w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold ring-2 transition-all"
                    :class="[
                      getSubscriptionAccent(subscription),
                      route.query.feedId === subscription.feedId
                        ? 'ring-primary ring-offset-2'
                        : !subscription.isRead
                          ? 'ring-primary ring-offset-2'
                          : 'ring-transparent group-hover:ring-gray-300 dark:group-hover:ring-gray-700'
                    ]">
                    {{ getSubscriptionInitials(subscription) }}
                  </div>
                  <!-- Selected Indicator -->
                  <span v-if="route.query.feedId === subscription.feedId"
                    class="absolute bottom-0 right-0 w-3 h-3 bg-primary rounded-full ring-2 ring-white dark:ring-gray-950 flex items-center justify-center">
                    <svg class="w-2 h-2 text-white" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </span>
                  <!-- Unread Indicator (only when not selected) -->
                  <span v-else-if="!subscription.isRead"
                    class="absolute bottom-0 right-0 w-3 h-3 bg-primary rounded-full ring-2 ring-white dark:ring-gray-950"></span>
                  <!-- Error Indicator -->
                  <span v-if="hasSubscriptionError(subscription)"
                    class="absolute -top-1 -right-1 w-4 h-4 bg-red-500 rounded-full flex items-center justify-center shadow-md">
                    <svg class="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" stroke-width="2.5"
                      viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </span>
                </div>
                <!-- Label -->
                <span
                  class="text-[11px] text-center line-clamp-2 w-full leading-tight transition-colors"
                  :class="route.query.feedId === subscription.feedId
                    ? 'text-primary font-medium'
                    : 'text-gray-700 dark:text-gray-300'">
                  {{ getSubscriptionLabel(subscription) }}
                </span>
              </RouterLink>
            </div>
          </div>
        </div>

        <div :class="route.name === 'feedsSubscriptions' ? 'px-1 pt-5 sm:px-6 sm:pt-6' : ''">
          <global-audio-player>
            <router-view v-slot="{ Component }">
              <KeepAlive :include="['HomePage', 'FeedSubscriptionsPage', 'FeedDetailPage', 'SearchPage', 'ArticleDetailPage']">
                <component :is="Component" />
              </KeepAlive>
            </router-view>
          </global-audio-player>
        </div>
      </main>
    </div>

    <!-- Mobile Bottom Tab Bar -->
    <nav v-show="route.name !== 'article'"
      class="lg:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 dark:bg-gray-950/95 backdrop-blur-lg border-t border-gray-200 dark:border-gray-800 pb-safe">
      <div class="flex items-center justify-around h-16 px-2">
        <!-- Home Tab -->
        <RouterLink :to="{ name: 'home' }"
          class="flex flex-col items-center justify-center flex-1 gap-1 py-2 transition-colors"
          :class="isTabActive('home') ? 'text-primary' : 'text-gray-600 dark:text-gray-400'">
          <svg class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" :d="icons.home" />
          </svg>
          <span class="text-[10px] font-medium">首页</span>
        </RouterLink>
        <!-- Collections Tab -->
        <RouterLink :to="{ name: 'radar' }"
                    class="flex flex-col items-center justify-center flex-1 gap-1 py-2 transition-colors"
                    :class="isTabActive('radar') ? 'text-primary' : 'text-gray-600 dark:text-gray-400'">
          <svg class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" :d="icons.radar" />
          </svg>
          <span class="text-[10px] font-medium">雷达</span>
        </RouterLink>


        <!-- Discover Tab (Center with larger icon) -->
        <RouterLink :to="{ name: 'discover' }"
          class="flex flex-col items-center justify-center flex-1 gap-1 py-2 -mt-2 transition-colors">
          <div class="flex items-center justify-center h-12 w-12 rounded-full transition-all"
            :class="isTabActive('discover') ? 'bg-primary text-white shadow-lg' : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400'">
            <svg class="h-7 w-7" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" :d="icons.plus" />
            </svg>
          </div>
        </RouterLink>

        <!-- Subscriptions Tab -->
        <RouterLink :to="{ name: 'feedsSubscriptions' }"
                    class="flex flex-col items-center justify-center flex-1 gap-1 py-2 transition-colors relative"
                    :class="isTabActive('feedsSubscriptions') ? 'text-primary' : 'text-gray-600 dark:text-gray-400'">
          <svg class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" :d="icons.inbox" />
          </svg>
          <span class="text-[10px] font-medium">订阅</span>
          <span v-if="hasUnreadSubscriptions" class="absolute top-1 right-1/4 h-2 w-2 rounded-full bg-red-500"></span>
        </RouterLink>

        <!-- Profile Tab -->
        <RouterLink :to="{ name: 'upgrade' }"
          class="flex flex-col items-center justify-center flex-1 gap-1 py-2 transition-colors"
          :class="isTabActive('upgrade') ? 'text-primary' : 'text-gray-600 dark:text-gray-400'">
          <div class="relative">
            <img v-if="user && user.avatarUrl" :src="user.avatarUrl" class="h-6 w-6 rounded-full" />
            <div v-else
              class="flex h-6 w-6 items-center justify-center rounded-full bg-gray-200 dark:bg-gray-700 text-[10px] font-semibold">
              {{ userInitials }}
            </div>
          </div>
          <span class="text-[10px] font-medium">我的</span>
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
// Icon Components
const icons = {
  menu: 'M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5',
  home: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6',
  inbox: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10',
  clock: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z',
  bookmark: 'M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z',
  radar: 'M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0 -18 0 M12 12m-6 0a6 6 0 1 0 12 0a6 6 0 1 0 -12 0 M12 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0 M12 12l7-2 M19 10a1 1 0 1 1-2 0a1 1 0 0 1 2 0',
  adjustments: 'M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4',
  chevronUp: 'M5 15l7-7 7 7',
  chevronDown: 'M19 9l-7 7-7-7',
  search: 'M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z',
  plus: 'M12 4.5v15m7.5-7.5h-15',
  close: 'M6 18L18 6M6 6l12 12'
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
const isAtTop = ref(true);
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
        icon: {
          stroke: true,
          paths: [icons.home],
          viewBox: '0 0 24 24'
        },
        activeMatch: (current) => {
          const view = current.query.view as string | undefined;
          const section = current.query.section as string | undefined;
          return current.name === 'home' && view !== 'shorts' && !section;
        }
      },
      {
        id: 'radar',
        label: '热点雷达',
        to: { name: 'radar' as const },
        icon: {
          stroke: true,
          paths: [icons.radar],
          viewBox: '0 0 24 24'
        },
        activeMatch: (current) => current.name === 'radar' || current.name === 'radarTopic'
      },
      //
      // {
      //   id: 'feedsSubscriptions',
      //   label: '订阅',
      //   to: {name: 'feedsSubscriptions' as const},
      //   icon: {
      //     stroke: true,
      //     paths: [icons.inbox],
      //     viewBox: '0 0 24 24'
      //   },
      //   activeMatch: (current) => current.name === 'feedsSubscriptions'
      // },
    ]
  },

];

const showAllSubscriptions = ref(false);

const subscriptionNavSection = computed<NavSection>(() => {
  const accentPalette = [
    'bg-slate-200 text-slate-900 dark:bg-slate-700 dark:text-slate-100',
    'bg-zinc-200 text-zinc-900 dark:bg-zinc-700 dark:text-zinc-100',
    'bg-stone-200 text-stone-900 dark:bg-stone-700 dark:text-stone-100',
    'bg-gray-200 text-gray-900 dark:bg-gray-700 dark:text-gray-100',
    'bg-neutral-200 text-neutral-900 dark:bg-neutral-700 dark:text-neutral-100'
  ];

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

    const initials =
      Array.from(label).slice(0, 2).join('').toUpperCase() || 'F';

    const danger = Boolean((s.failureCount ?? 0) > 0 || s.fetchError?.trim());

    return { subscription: s, label, initials, danger };
  });

  entries.sort((a, b) => {
    if (a.danger === b.danger) return 0;
    return a.danger ? 1 : -1;
  });

  const visibleEntries = showAllSubscriptions.value ? entries : entries.slice(0, 9);

  const items: NavItem[] = visibleEntries.map((meta, idx) => {
    const s = meta.subscription;
    const accent = meta.danger
      ? 'bg-red-100 text-red-600 dark:bg-red-950 dark:text-red-400 border border-red-300 dark:border-red-800'
      : accentPalette[idx % accentPalette.length];

    return {
      id: `sub-${s.feedId}`,
      label: meta.label,
      to: { name: 'feed' as const, params: { feedId: s.feedId } },
      avatar: s.avatar,
      avatarText: meta.initials,
      accent,
      activeMatch: (current) => {
        const paramId = typeof current.params.feedId === 'string' ? current.params.feedId : undefined;
        return current.name === 'feed' && paramId === s.feedId;
      },
      badge: s.isRead ? '' : '1',
      danger: meta.danger
    };
  });

  if (entries.length > 9) {
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
  return [...baseNavSections, subscriptionNavSection.value, {
    id: 'you',
    title: '我',
    // http://localhost:5173/upgrade
    to: { name: 'upgrade' as const },
    activeMatch: (current) => current.name === 'upgrade',
    items: [

      {
        id: 'history',
        label: '历史记录',
        to: { name: 'history' as const },
        icon: {
          stroke: true,
          paths: [icons.clock],
          viewBox: '0 0 24 24'
        },
        activeMatch: (current) => current.name === 'history'
      },
      {
        id: 'library',
        label: '收藏夹',
        to: { name: 'collections' as const },
        icon: {
          stroke: true,
          paths: [icons.bookmark],
          viewBox: '0 0 24 24'
        },
        activeMatch: (current) => {
          const tab = current.query.tab as string | undefined;
          return current.name === 'collections' && !tab;
        }
      }
    ]
  }];
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
    // Save to recent searches
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

const getSubscriptionAccent = (subscription: any): string => {
  const accentPalette = [
    'bg-blue-100 text-blue-600 dark:bg-blue-900 dark:text-blue-300',
    'bg-purple-100 text-purple-600 dark:bg-purple-900 dark:text-purple-300',
    'bg-pink-100 text-pink-600 dark:bg-pink-900 dark:text-pink-300',
    'bg-green-100 text-green-600 dark:bg-green-900 dark:text-green-300',
    'bg-yellow-100 text-yellow-600 dark:bg-yellow-900 dark:text-yellow-300',
    'bg-indigo-100 text-indigo-600 dark:bg-indigo-900 dark:text-indigo-300',
    'bg-teal-100 text-teal-600 dark:bg-teal-900 dark:text-teal-300',
    'bg-orange-100 text-orange-600 dark:bg-orange-900 dark:text-orange-300',
  ];

  if (hasSubscriptionError(subscription)) {
    return 'bg-red-100 text-red-600 dark:bg-red-950 dark:text-red-400';
  }

  const hash = subscription.feedId.split('').reduce((acc: number, char: string) => {
    return acc + char.charCodeAt(0);
  }, 0);
  return accentPalette[hash % accentPalette.length];
};

const hasSubscriptionError = (subscription: any): boolean => {
  return Boolean((subscription.failureCount ?? 0) > 0 || subscription.fetchError?.trim());
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

  isAtTop.value = current <= 0;

  // 只在移动端隐藏，桌面始终显示，由模板里的 lg:translate-y-0 保证
  if (Math.abs(delta) > 5) {
    if (current > 80 && delta > 0) {
      // 向下滚动且超过一定距离 -> 隐藏
      isHeaderHidden.value = true;
    } else if (delta < 0) {
      // 向上滚动 -> 显示
      isHeaderHidden.value = false;
    }
  }

  lastScrollY.value = current;
};

// Close dropdown when clicking outside
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

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.25s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(-100%);
}

/* Custom scrollbar */
nav::-webkit-scrollbar {
  width: 6px;
}

nav::-webkit-scrollbar-track {
  background: transparent;
}

nav::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.3);
  border-radius: 3px;
}

nav::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.5);
}

.dark nav::-webkit-scrollbar-thumb {
  background: rgba(75, 85, 99, 0.3);
}

.dark nav::-webkit-scrollbar-thumb:hover {
  background: rgba(75, 85, 99, 0.5);
}

/* Mobile search page animation */
.search-slide-enter-active,
.search-slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.search-slide-enter-from {
  transform: translateY(-100%);
}

.search-slide-leave-to {
  transform: translateY(-100%);
}

/* Safe area for iOS */
.pb-safe {
  padding-bottom: env(safe-area-inset-bottom);
}

.pt-safe {
  padding-top: env(safe-area-inset-top);
}

/* Hide scrollbar for horizontal scroll */
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.scrollbar-hide::-webkit-scrollbar {
  display: none;
}

/* Line clamp for subscription labels */
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
