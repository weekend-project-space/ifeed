<template>
  <div class="min-h-screen text-gray-900 dark:text-gray-100 transition-colors duration-300"
       :class="{ 'overflow-hidden': mobileNavOpen }">

    <!-- Header -->
    <header :class="isSidebarCollapsed ? 'lg:pl-20' : 'lg:pl-72'"
            class="sticky top-0 z-30  bg-white/95 dark:bg-surface/95 backdrop-blur-sm">
      <div class="flex items-center  justify-between gap-3 px-4 py-3 lg:px-5">
        <!-- Logo & Menu Button -->
        <div class="flex items-center gap-3">
          <button
              type="button"
              class="flex h-10 w-10 items-center justify-center rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 transition lg:hidden"
              @click="mobileNavOpen = true">
            <svg class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" :d="icons.menu"/>
            </svg>
          </button>

          <button
              type="button"
              class="hidden lg:flex h-10 w-10 items-center justify-center rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 transition"
              @click="toggleSidebar">
            <svg class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" :d="icons.menu"/>
            </svg>
          </button>

        </div>

        <!-- Search Bar (Desktop) -->
        <div class="hidden lg:flex flex-1 max-w-2xl mx-auto">
          <div class="relative w-full group">
            <input
                v-model="search"
                type="search"
                placeholder="搜索文章、标签、订阅..."
                class="w-full h-12 pl-14 pr-4 rounded-full border bg-white dark:bg-gray-900 text-base transition-all duration-200"
                :class="[
                searchFocused
                  ? 'border-transparent shadow-lg ring-1 ring-gray-300 dark:ring-gray-700'
                  : 'border-gray-300 dark:border-gray-700 hover:shadow-md hover:border-gray-400 dark:hover:border-gray-600'
              ]"
                @focus="searchFocused = true"
                @blur="searchFocused = false"
                @keyup.enter="handleSearch"/>

            <!-- Search Icon -->
            <div class="absolute left-5 top-1/2 -translate-y-1/2">
              <svg class="h-5 w-5 transition-colors duration-200"
                   :class="searchFocused ? 'text-primary' : 'text-gray-400'"
                   fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" :d="icons.search"/>
              </svg>
            </div>

            <!-- Voice Search Button (Optional) -->
            <button
                v-if="search"
                type="button"
                class="absolute right-16 top-1/2 -translate-y-1/2 flex h-8 w-8 items-center justify-center rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 transition text-gray-500"
                @click="search = ''">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" :d="icons.close"/>
              </svg>
            </button>

            <!-- Search Button -->
            <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 flex h-9 w-9 items-center justify-center rounded-full transition-colors"
                :class="search ? 'bg-primary text-primary-foreground hover:bg-primary/90' : 'text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'"
                @click="handleSearch">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" :d="icons.search"/>
              </svg>
            </button>
          </div>
        </div>

        <div class="min-w-[12rem] flex  justify-end">
          <div class="flex items-center gap-2 " id="header-action">

          </div>
          <!-- Actions -->
          <div class="flex items-center gap-2" v-show="route.name!='article-detail'">
            <!-- Theme Toggle -->
            <button
                type="button"
                class="flex h-10 w-10 items-center justify-center rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 transition"
                @click="toggleTheme">
              <svg v-if="isDark" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.5"
                   viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M21.752 15.002A9.718 9.718 0 0118 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 003 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 009.002-5.998z"/>
              </svg>
              <svg v-else class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M12 3v2.25m6.364.386l-1.591 1.591M21 12h-2.25m-.386 6.364l-1.591-1.591M12 18.75V21m-4.773-4.227l-1.591 1.591M5.25 12H3m4.227-4.773L5.636 5.636M15.75 12a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0z"/>
              </svg>
            </button>


            <!-- Add Subscription Button -->
            <RouterLink
                :to="{ name: 'subscriptions' }"
                class="hidden sm:flex items-center gap-2 h-10 px-4 rounded-full bg-secondary/5 text-secondary hover:bg-secondary/20 text-sm font-medium transition">
              <span class="text-lg leading-none">＋</span>
              订阅
            </RouterLink>

            <RouterLink
                :to="{ name: 'subscriptions' }"
                class="flex sm:hidden h-10 w-10 items-center justify-center rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 transition">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
              </svg>
            </RouterLink>
            <!-- User Menu (YouTube Style - Pure CSS Hover) -->
            <div class="relative group hidden md:block">
              <div
                  class="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-semibold cursor-pointer group-hover:ring-2 group-hover:ring-primary/30 transition-all">
                {{ userInitials }}
              </div>

              <!-- Dropdown Menu -->
              <div
                  class="absolute right-0 mt-2 w-64 origin-top-right rounded-xl bg-white dark:bg-gray-900 shadow-lg ring-1 ring-black ring-opacity-5 overflow-hidden z-50 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 transform scale-95 group-hover:scale-100">
                <!-- User Info Section -->
                <div class="px-4 py-3 border-b border-gray-200 dark:border-gray-800">
                  <div class="flex items-center gap-3">
                    <div
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
                  <!--                <RouterLink-->
                  <!--                    to="/profile"-->
                  <!--                    class="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 transition">-->
                  <!--                  <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">-->
                  <!--                    <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />-->
                  <!--                  </svg>-->
                  <!--                  个人资料-->
                  <!--                </RouterLink>-->

                  <RouterLink
                      to="/feeds/channels"
                      class="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 transition">
                    <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round"
                            d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z"/>
                      <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                    </svg>
                    管理订阅
                  </RouterLink>
                </div>

                <!-- Logout Section -->
                <div class="border-t border-gray-200 dark:border-gray-800 py-1">
                  <button
                      type="button"
                      class="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-red-600 dark:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/20 transition"
                      @click="handleLogout">
                    <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round"
                            d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75"/>
                    </svg>
                    退出登录
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>


      <!-- Search Bar (Mobile) -->
      <div class="lg:hidden px-4 pb-3">
        <div class="relative">
          <input
              v-model="search"
              type="search"
              placeholder="搜索文章、标签、订阅..."
              class="w-full h-11 pl-12 pr-12 rounded-full border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition"
              @keyup.enter="handleSearch"/>
          <svg class="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" fill="none" stroke="currentColor"
               stroke-width="1.5" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"/>
          </svg>
          <button
              type="button"
              class="absolute right-2 top-1/2 -translate-y-1/2 flex h-8 w-8 items-center justify-center rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 transition"
              @click="handleSearch">
            <svg class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"/>
            </svg>
          </button>
        </div>
      </div>
    </header>

    <!-- Mobile Sidebar Overlay -->
    <transition name="fade">
      <div
          v-if="mobileNavOpen"
          class="fixed inset-0 z-30 bg-black/40 backdrop-blur-sm lg:hidden"
          @click="mobileNavOpen = false"/>
    </transition>

    <!-- Mobile Sidebar -->
    <transition name="slide">
      <div
          v-if="mobileNavOpen"
          class="fixed inset-y-0 left-0 z-40 w-72 bg-white dark:bg-gray-950 border-r border-gray-200 dark:border-gray-800 shadow-xl lg:hidden">
        <div class="flex h-full flex-col">
          <!-- Mobile Sidebar Header -->
          <div class="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-800">
            <RouterLink
                :to="{ name: 'home' }"
                class="flex items-center gap-2 text-base font-semibold"
                @click="mobileNavOpen = false">
              <img class="h-9 w-9 rounded-2xl" src="/logo.svg" alt="iFeed"/>
              <span>IFeed</span>
            </RouterLink>
            <button
                type="button"
                class="flex h-9 w-9 items-center justify-center rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 transition"
                @click="mobileNavOpen = false">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <!-- Mobile Nav -->
          <nav class="flex-1 overflow-y-auto p-2">
            <div v-for="(section, index) in navSections" :key="section.id" class="mb-6">
              <div v-if="section.title"
                   class="px-3 mb-2 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                {{ section.title }}
              </div>
              <div class="space-y-1">
                <component
                    v-for="item in section.items"
                    :is="item.to ? 'RouterLink' : 'button'"
                    :key="item.id"
                    v-bind="item.to ? { to: item.to } : { type: 'button' }"
                    class="flex w-full items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition"
                    :class="[
                    isActiveItem(item)
                      ? 'bg-primary/10 dark:bg-primary/20 text-text font-semibold'
                      : item.danger
                        ? 'text-red-500 hover:bg-red-50 dark:hover:bg-red-950'
                        : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800'
                  ]"
                    @click="handleNavItemClick(item)">
                  <span
                      v-if="item.icon"
                      class="flex h-6 w-6 items-center justify-center rounded-lg">
                    <svg class="h-6 h-6" :viewBox="item.icon.viewBox ?? '0 0 20 20'"
                         :fill="item.icon.stroke ? 'none' : 'currentColor'"
                         :stroke="item.icon.stroke ? 'currentColor' : 'none'"
                         :stroke-width="item.icon.stroke ? 1.6 : undefined"
                         :stroke-linecap="item.icon.stroke ? 'round' : undefined"
                         :stroke-linejoin="item.icon.stroke ? 'round' : undefined">
                      <path v-for="path in item.icon.paths" :key="path" :d="path"/>
                    </svg>
                  </span>
                  <img v-else-if="item.avatar" :src="item.avatar" class="h-6 w-6 rounded-full"/>
                  <span v-else-if="item.avatarText"
                        class="flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold"
                        :class="item.accent ?? 'bg-primary text-primary-foreground'">
                    {{ item.avatarText }}
                  </span>
                  <span class="flex-1 truncate">{{ item.label }}</span>
                  <span v-if="item.badge" class="flex h-1.5 w-1.5 rounded-full bg-primary"/>
                </component>
              </div>
            </div>
          </nav>

          <!-- Mobile User Section -->
          <div class="p-4 border-t border-gray-200 dark:border-gray-800">
            <div class="flex items-center gap-3 p-3 rounded-lg bg-gray-50 dark:bg-gray-900">
              <div
                  class="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-semibold">
                {{ userInitials }}
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium truncate">{{ user?.username ?? '访客' }}</p>
              </div>
            </div>
            <button
                type="button"
                class="mt-3 w-full py-2 rounded-lg bg-red-50 dark:bg-red-950 text-sm font-semibold text-red-500 hover:bg-red-100 dark:hover:bg-red-900 transition"
                @click="handleLogout">
              退出登录
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- Layout Container -->
    <div class="flex" :class="isSidebarCollapsed ? 'lg:pl-20' : 'lg:pl-72'">
      <!-- Desktop Sidebar -->
      <aside
          :class="[
          'fixed left-0 top-[0px] bottom-0 z-30 hidden lg:block bg-gray-50 dark:bg-gray-950 transition-all duration-200',
          isSidebarCollapsed ? 'w-20' : 'w-72'
        ]">
        <div class="flex h-full flex-col overflow-hidden py-4">

          <RouterLink :to="{ name: 'home' }" class="flex items-center gap-2 text-lg font-semibold px-4 pt-2 pb-5"
                      :class=" isSidebarCollapsed ? 'justify-center px-2' : 'gap-3 px-3'">
            <img class="h-7 w-7 rounded-2xl" src="/logo.svg" alt="iFeed"/>
            <span v-if="!isSidebarCollapsed">IFeed</span>
          </RouterLink>
          <nav class="flex-1 overflow-y-auto px-2" :class="{ 'space-y-1': isSidebarCollapsed }">
            <div v-for="(section, index) in navSections" :key="section.id" class="mb-6" v-show="!(isSidebarCollapsed&&section?.id=='subscriptions')">
              <div
                  v-if="section.title && !isSidebarCollapsed"
                  class="px-3 mb-2 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                {{ section.title }}
              </div>
              <div class="space-y-1">
                <component
                    v-for="item in section.items"
                    :is="item.to ? 'RouterLink' : 'button'"
                    :key="item.id"
                    v-bind="item.to ? { to: item.to } : { type: 'button' }"
                    class="flex w-full items-center rounded-lg py-2.5 text-sm font-medium transition"
                    :class="[
                    isSidebarCollapsed ? 'justify-center px-2' : 'gap-3 px-3',
                    isActiveItem(item)
                      ? 'bg-primary/10 dark:bg-primary/20 text-text font-semibold '
                      : item.danger
                        ? 'text-red-500 hover:bg-red-50 dark:hover:bg-red-950'
                        : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800'
                  ]"
                    :title="isSidebarCollapsed ? item.label : undefined"
                    @click="handleNavItemClick(item)">
                  <span
                      v-if="item.icon"
                      class="flex h-6 w-6 items-center justify-center rounded-lg">
                    <svg class="h-6 w-6" :viewBox="item.icon.viewBox ?? '0 0 24 24'"
                         :fill="item.icon.stroke ? 'none' : 'currentColor'"
                         :stroke="item.icon.stroke ? 'currentColor' : 'none'"
                         :stroke-width="item.icon.stroke ? 1.5 : undefined"
                         :stroke-linecap="item.icon.stroke ? 'round' : undefined"
                         :stroke-linejoin="item.icon.stroke ? 'round' : undefined">
                      <path v-for="path in item.icon.paths" :key="path" :d="path"/>
                    </svg>
                  </span>
                  <img v-else-if="item.avatar" :src="item.avatar" class="h-6 w-6 rounded-full"/>
                  <span v-else-if="item.avatarText"
                        class="flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold"
                        :class="item.accent ?? 'bg-primary text-primary-foreground'">
                    {{ item.avatarText }}
                  </span>
                  <span v-if="!isSidebarCollapsed" class=" truncate">{{ item.label }}</span>
                  <span v-if="item.badge && !isSidebarCollapsed" class="flex h-1 w-1 rounded-full bg-primary"/>
                </component>
              </div>
            </div>
          </nav>
        </div>
      </aside>

      <!-- Main Content -->
      <main
          class="flex-1 min-w-0 min-h-[calc(100vh-5em)]   px-3 pb-10 pt-5 sm:px-6 sm:pb-12 sm:pt-6">
        <router-view/>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import type {RouteLocationNormalizedLoaded, RouteLocationRaw} from 'vue-router';
import {storeToRefs} from 'pinia';
import {useAuthStore} from '../stores/auth';
import {useThemeStore} from '../stores/theme';
import {useSubscriptionsStore} from '../stores/subscriptions';
import {useMixFeedsStore} from "../stores/mixFeeds";

// Icon Components
const icons = {
  menu: 'M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5',
  home: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6',
  inbox: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10',
  clock: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z',
  bookmark: 'M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z',
  adjustments: 'M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4',
  chevronUp: 'M5 15l7-7 7 7',
  chevronDown: 'M19 9l-7 7-7-7',
  search: 'M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z',
  sun: 'M12 3v2.25m6.364.386l-1.591 1.591M21 12h-2.25m-.386 6.364l-1.591-1.591M12 18.75V21m-4.773-4.227l-1.591 1.591M5.25 12H3m4.227-4.773L5.636 5.636M15.75 12a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0z',
  moon: 'M21.752 15.002A9.718 9.718 0 0118 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 003 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 009.002-5.998z',
  plus: 'M12 4.5v15m7.5-7.5h-15',
  close: 'M6 18L18 6M6 6l12 12'
};

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const themeStore = useThemeStore();
const subscriptionsStore = useSubscriptionsStore();
const mixFeedStore = useMixFeedsStore();

const {user} = storeToRefs(authStore);
const {isDark, label: themeLabel} = storeToRefs(themeStore);

const mobileNavOpen = ref(false);
const isSidebarCollapsed = ref(localStorage.getItem('sidebar-collapsed') === 'true');
const search = ref('');
const searchFocused = ref(false);

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
  items: NavItem[];
};

const baseNavSections: NavSection[] = [
  {
    id: 'primary',
    items: [
      {
        id: 'home',
        label: '首页',
        to: {name: 'home' as const},
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
        id: 'feedsSubscriptions',
        label: '订阅',
        to: {name: 'feedsSubscriptions' as const},
        icon: {
          stroke: true,
          paths: [icons.inbox],
          viewBox: '0 0 24 24'
        },
        activeMatch: (current) => current.name === 'feedsSubscriptions'
      }
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

    return {subscription: s, label, initials, danger};
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
      to: {name: 'feed' as const, params: {feedId: s.feedId}},
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

  // items.push({
  //   id: 'manage-subscriptions',
  //   label: '管理订阅',
  //   to: {name: 'feedChannels' as const},
  //   icon: {
  //     stroke: true,
  //     paths: [icons.adjustments],
  //     viewBox: '0 0 24 24'
  //   },
  //   activeMatch: (current) => current.name === 'feedChannels'
  // });

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
    items
  };
});

const navSections = computed<NavSection[]>(() => {
  return [...baseNavSections, subscriptionNavSection.value, {
    id: 'you',
    title: '我',
    items: [
      {
        id: 'history',
        label: '历史记录',
        to: {name: 'history' as const},
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
        to: {name: 'collections' as const},
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
  if (mobileNavOpen.value) {
    mobileNavOpen.value = false;
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
    router.push({name: 'home', query});
    return;
  }

  const query: Record<string, string> = {q: keyword};
  if (currentType === 'semantic') query.type = currentType;
  if (feedId) query.feedId = feedId;
  if (tag) query.tags = tag;
  if (category) query.category = category;
  router.push({name: 'search', query});
};

const handleLogout = async () => {
  await authStore.logout();
  router.replace({name: 'auth'});
};

const toggleTheme = () => {
  themeStore.toggle();
};

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value;
  localStorage.setItem('sidebar-collapsed', String(isSidebarCollapsed.value));
};

watch(
    () => route.query.q,
    (value) => {
      search.value = typeof value === 'string' ? value : '';
    },
    {immediate: true}
);

onMounted(async () => {
  await subscriptionsStore.fetchSubscriptions();
  mixFeedStore.clearMyMixFeeds();
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
</style>