<template>
  <div class="space-y-3">
    <slot name="header"></slot>
    <!-- Header -->
    <header class="max-w-screen-lg mx-auto flex flex-wrap items-center justify-between gap-4 px-4">
      <div class="flex items-center gap-3">
        <template v-if="title">
          <h1 class="text-xl font-normal text-gray-900 dark:text-gray-100">
            {{ title }}
            <small v-text="subtitle"> </small>
          </h1>
          <button
              type="button"
              class="p-2 hover:bg-surface-container rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              @click="emit('refresh')"
              :disabled="loading"
              aria-label="刷新列表"
              title="刷新">
            <svg
                xmlns="http://www.w3.org/2000/svg"
                class="w-5 h-5 text-text-secondary transition-transform"
                :class="{ 'animate-spin': loading }"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
            >
              <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
            </svg>
          </button>
        </template>
      </div>
      <div class="flex items-center gap-2">
        <slot name="action"></slot>
        <button
            @click="setView('magazine')"
            :class="btnClass(view === 'magazine')"
            title="杂志视图"
            aria-label="杂志视图">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"
               :stroke-width="view === 'magazine' ? 2.5 : 2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h8m-8 4h5m-5 5h8m-8 4h5"/>
            <rect x="15" y="4" width="5" height="5" rx="1"/>
            <rect x="15" y="14" width="5" height="5" rx="1"/>
          </svg>
        </button>
        <button
            @click="setView('social')"
            :class="btnClass(view === 'social')"
            title="社交视图"
            aria-label="社交视图">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"
               :stroke-width="view === 'social' ? 2.5 : 2">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
        </button>
        <button
            @click="setView('card')"
            :class="btnClass(view === 'card')"
            title="卡片视图"
            aria-label="卡片视图">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"
               :stroke-width="view === 'card' ? 2.5 : 2">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/>
          </svg>
        </button>

        <button
            @click="setView('only-title')"
            :class="btnClass(view === 'only-title')"
            title="仅标题视图"
            aria-label="仅标题视图">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"
               :stroke-width="view === 'only-title' ? 2.5 : 2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 8h16M4 16h16"/>
          </svg>
        </button>

      </div>
    </header>

    <!-- Loading skeleton -->
    <section v-if="loading" class="space-y-4">
      <div v-if="view === 'card'" class="grid gap-0 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <div v-for="i in 12" :key="`card-skel-${i}`" class="animate-pulse space-y-4 p-4">
          <div class="aspect-video w-full bg-surface-container rounded-lg"></div>
          <div class="space-y-2">
            <div class="h-4 w-3/4 bg-surface-container rounded"></div>
            <div class="h-3 w-1/4 bg-surface-container rounded"></div>
            <div class="h-4 w-full bg-surface-container rounded"></div>
            <div class="h-4 w-3/4 bg-surface-container rounded"></div>
          </div>
        </div>
      </div>

      <div v-else-if="view === 'magazine'" class="max-w-screen-lg mx-auto space-y-3">
        <div v-for="i in 10" :key="`mag-skel-${i}`" class="flex items-start gap-3 md:gap-6 animate-pulse p-4">
          <div class="flex-1 space-y-2">
            <div class="h-5 w-2/3 bg-surface-container rounded"></div>
            <div class="h-3 w-1/3 bg-surface-container rounded"></div>
            <div class="hidden md:block h-4 w-full bg-surface-container rounded"></div>
            <div class="hidden md:block h-4 w-3/4 bg-surface-container rounded"></div>
          </div>
          <div
              class="w-20 h-20 md:w-48 md:h-32 lg:w-56 lg:h-36 bg-surface-container rounded-lg md:rounded-xl flex-shrink-0"></div>
        </div>
      </div>

      <div v-else-if="view === 'only-title'" class="max-w-screen-lg mx-auto">
        <div v-for="i in 15" :key="`title-skel-${i}`" class="flex items-center gap-3 px-4 py-2.5 animate-pulse">
          <div class="h-3 w-20 md:w-24 bg-surface-container rounded flex-shrink-0"></div>
          <div class="h-4 flex-1 bg-surface-container rounded"></div>
          <div class="h-3 w-12 bg-surface-container rounded flex-shrink-0"></div>
        </div>
      </div>

      <div v-else-if="view === 'social'" class="max-w-[640px] mx-auto">
        <div v-for="i in 8" :key="`tl-skel-${i}`" class="animate-pulse px-4 py-5 space-y-3">
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 rounded-full bg-surface-container flex-shrink-0"></div>
            <div class="h-3 w-24 bg-surface-container rounded"></div>
            <div class="h-3 w-12 bg-surface-container rounded"></div>
          </div>
          <div class="space-y-2 pl-11">
            <div class="h-4 w-4/5 bg-surface-container rounded"></div>
            <div class="h-3 w-full bg-surface-container rounded"></div>
            <div class="h-3 w-2/3 bg-surface-container rounded"></div>
            <div v-if="i % 2 === 0" class="h-48 w-full bg-surface-container rounded-xl mt-2"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- Content -->
    <section v-else>
      <!-- Empty state -->
      <div v-if="!items.length" class="flex flex-col items-center justify-center gap-3 py-16 text-center">
        <slot name="empty">
          <svg class="h-12 w-12 text-gray-400 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          <span class="text-sm text-gray-600 dark:text-gray-400 max-w-xs">{{ emptyMessage }}</span>
        </slot>
      </div>

      <transition name="fade" mode="out-in">
        <!-- Magazine view -->
        <div v-if="view === 'magazine'" key="view-magazine" class="max-w-screen-lg mx-auto space-y-4">
          <article
              v-for="item in items"
              :key="item.id"
              class="group flex flex-row items-start gap-3 md:gap-6
                     p-4 md:p-5 rounded-2xl transition-all duration-300
                     hover:bg-surface-container/60 cursor-pointer"
              @click="handleArticleContainerClick(item, $event)"
          >
            <!-- 文字内容 -->
            <div class="flex-1 min-w-0 order-1 space-y-2 md:space-y-3">
              <!-- 标题 -->
              <h3 class="text-base font-normal text-gray-900 dark:text-gray-100 line-clamp-1 md:line-clamp-2">
                <router-link
                    :to="{ name: 'article', params: { id: item.id } }"
                    class="rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
                  {{ item.title }}
                </router-link>
              </h3>

              <!-- 来源 · 时间 -->
              <div
                  class="flex items-center gap-1.5 text-xs text-gray-500 dark:text-gray-500 whitespace-nowrap max-w-full">
                <router-link
                    v-if="item.feedId"
                    :to="{ name: 'feed', params: { feedId: item.feedId } }"
                    class="flex min-w-0 items-center gap-1.5 truncate rounded hover:text-primary transition-colors
                             focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                    :title="item.feedTitle">
                  <img v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                       :src="item.feedAvatar"
                       :alt="item.feedTitle"
                       class="w-5 h-5 rounded-full object-cover flex-shrink-0"
                       @error="avatarErrorMap[item.id] = true"/>
                  <span v-else
                        class="w-5 h-5 rounded-full bg-primary/10 text-primary flex items-center justify-center
                                 text-[10px] font-semibold flex-shrink-0">
                      {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
                    </span>
                  <span class="truncate">{{ item.feedTitle }}</span>
                </router-link>
                <span v-else class="flex min-w-0 items-center gap-1.5 truncate" :title="item.feedTitle">
                    <img v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                         :src="item.feedAvatar"
                         :alt="item.feedTitle"
                         class="w-3 h-3 rounded-full object-cover flex-shrink-0"
                         @error="avatarErrorMap[item.id] = true"/>
                    <span v-else
                          class="w-3 h-3 rounded-full bg-primary/10 text-primary flex items-center justify-center
                                 text-[10px] font-semibold flex-shrink-0">
                      {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
                    </span>
                    <span class="truncate">{{ item.feedTitle }}</span>
                  </span>
                <span class="flex-shrink-0">· {{ item.timeAgo }}</span>
              </div>

              <!-- 摘要 -->
              <router-link
                  v-if="item.summary"
                  :to="{ name: 'article', params: { id: item.id } }"
                  class="block rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
                <p class="text-sm leading-relaxed text-gray-600 dark:text-gray-400 line-clamp-2 ">
                  {{ item.summary }}
                </p>
              </router-link>

              <!-- 标签 -->
              <div v-if="item.tags?.length" class="hidden md:flex flex-wrap gap-2">
                <button
                    v-for="tag in item.tags"
                    :key="tag"
                    type="button"
                    @click="emit('select-tag', tag)"
                    class="px-3 py-1 text-xs font-medium rounded-full
                   bg-secondary-100 dark:bg-secondary-900/50
                   text-secondary-700 dark:text-secondary-300
                   hover:bg-secondary-200 dark:hover:bg-secondary-800/50
                   transition-colors"
                >
                  #{{ tag }}
                </button>
              </div>
            </div>

            <!-- 缩略图 -->
            <router-link
                v-if="item.thumbnail && !thumbErrorMap[item.id]"
                :to="{ name: 'article', params: { id: item.id } }"
                class="flex-shrink-0 order-2
               w-20 h-20 md:w-48 md:h-32
               overflow-hidden rounded-lg md:rounded-xl
               bg-gray-100 dark:bg-gray-800
               focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              <img
                  :src="item.thumbnail"
                  :alt="item.title"
                  loading="lazy"
                  decoding="async"
                  class="w-full h-full object-cover transition-transform duration-500
                 md:group-hover:scale-105"
                  @error="thumbErrorMap[item.id] = true"
              />
            </router-link>
          </article>
        </div>


        <!-- Card view - 恢复原始紧凑样式 -->
        <div
            v-else-if="view === 'card'"
            key="view-card"
            class="grid gap-0 md:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4">
          <article
              v-for="item in items"
              :key="item.id"
              class="group relative flex h-full flex-col overflow-hidden rounded-xl p-3
                     bg-white dark:bg-surface-container/60 hover:bg-surface-container/60
                     transition-colors duration-200 cursor-pointer"
              @click="handleArticleContainerClick(item, $event)">
            <router-link
                :to="{ name: 'article', params: { id: item.id } }"
                class="relative block aspect-video w-full overflow-hidden bg-gray-100 dark:bg-gray-800 rounded-xl
                         focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
              <img
                  v-if="item.thumbnail && !thumbErrorMap[item.id]"
                  :src="item.thumbnail"
                  :alt="item.title"
                  loading="lazy"
                  decoding="async"
                  class="h-full w-full object-cover transition duration-300 group-hover:scale-[1.02]"
                  @error="thumbErrorMap[item.id] = true"/>
              <div
                  v-else
                  class="flex h-full w-full flex-col items-center justify-center text-gray-400 dark:text-gray-600 gap-2">
                <svg class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                     stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
                <!--                  <span class="text-xs">无图</span>-->
              </div>
            </router-link>

            <div class="relative flex flex-1 flex-col gap-2 py-3">
              <header class="flex items-start gap-2">
                <router-link
                    v-if="item.feedId"
                    :to="{ name: 'feed', params: { feedId: item.feedId } }"
                    class="flex-shrink-0 rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                    :aria-label="`查看 ${item.feedTitle}`">
                  <img v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                       :src="item.feedAvatar"
                       :alt="item.feedTitle"
                       class="w-7 h-7 rounded-full object-cover mt-0.5"
                       @error="avatarErrorMap[item.id] = true"/>
                  <span v-else
                        class="w-7 h-7 rounded-full bg-primary/10 text-primary flex items-center justify-center
                               text-[10px] font-semibold mt-0.5">
                      {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
                    </span>
                </router-link>
                <span v-else class="flex-shrink-0">
                    <img v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                         :src="item.feedAvatar"
                         :alt="item.feedTitle"
                         class="w-7 h-7 rounded-full object-cover mt-0.5"
                         @error="avatarErrorMap[item.id] = true"/>
                    <span v-else
                          class="w-7 h-7 rounded-full bg-primary/10 text-primary flex items-center justify-center
                                 text-[10px] font-semibold mt-0.5">
                      {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
                    </span>
                  </span>
                <div class="min-w-0 flex-1 space-y-1.5">
                  <h3 class="text-base font-medium leading-tight text-gray-900 dark:text-gray-100 line-clamp-2">
                    <router-link
                        :to="{ name: 'article', params: { id: item.id } }"
                        class="rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
                      {{ item.title }}
                    </router-link>
                  </h3>
                  <p class="text-xs text-gray-500 dark:text-gray-500">
                    <router-link v-if="item.feedId"
                                 :to="{ name: 'feed', params: { feedId: item.feedId } }"
                                 class="rounded hover:text-primary transition-colors
                                   focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
                      {{ item.feedTitle }}
                    </router-link>
                    <span v-else>{{ item.feedTitle }}</span>
                    ·
                    <span>
                        {{ item.timeAgo }}
                      </span>
                  </p>
                </div>
              </header>

              <footer v-if="item.tags?.length"
                      class="flex min-h-5 flex-wrap gap-3 mt-auto text-xs text-gray-500 dark:text-gray-500">
                <button
                    v-for="tag in item.tags"
                    :key="tag"
                    type="button"
                    class="hover:text-primary transition-colors"
                    @click="emit('select-tag', tag)">
                  #{{ tag }}
                </button>
              </footer>
            </div>
          </article>
        </div>

        <!-- Title only view -->
        <div v-else-if="view === 'only-title'" key="view-only-title" class=" mx-auto">
          <article
              v-for="item in items"
              :key="item.id"
              class="flex items-start gap-3 px-4 py-2.5
                     transition-colors hover:bg-surface-container/60 rounded-xl cursor-pointer"
              @click="handleArticleContainerClick(item, $event)">
            <!-- 来源 -->
            <router-link v-if="item.feedId"
                         :to="{ name: 'feed', params: { feedId: item.feedId } }"
                         class="flex-shrink-0 w-24 md:w-32 max-w-[35%] text-xs text-gray-500 dark:text-gray-400 truncate pt-0.5
                         rounded hover:text-primary transition-colors
                         focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
              {{ item.feedTitle }}
            </router-link>
            <span v-else class="flex-shrink-0 w-24 md:w-32 max-w-[35%] text-xs text-gray-500 dark:text-gray-400 truncate pt-0.5">
              {{ item.feedTitle }}
            </span>
            <!-- 标题 + 摘要 -->
            <router-link
                :to="{ name: 'article', params: { id: item.id } }"
                class="flex-1 min-w-0 text-sm truncate rounded
                       focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
              <span class="text-gray-900 dark:text-gray-100 font-medium">{{ item.title }}</span>
            </router-link>
            <!-- 时间 -->
            <span class="flex-shrink-0 text-xs text-gray-400 dark:text-gray-500 pt-0.5">
              {{ item.timeAgo }}
            </span>
          </article>
        </div>

        <!-- Social view (X / Reddit style) -->
        <div v-else key="view-social" class="max-w-[600px] mx-auto">
          <article
              v-for="item in items"
              :key="item.id"
              class="social-divider relative block px-4 py-5 rounded-xl
                     last:after:hidden cursor-pointer"
              @click="handleArticleContainerClick(item, $event)"
          >
            <div class="flex gap-3">
              <!-- 头像 -->
              <router-link
                  v-if="item.feedId"
                  :to="{ name: 'feed', params: { feedId: item.feedId } }"
                  class="flex-shrink-0 rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                  :aria-label="`查看 ${item.feedTitle}`">
                <img v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                     :src="item.feedAvatar"
                     :alt="item.feedTitle"
                     class="flex-shrink-0 w-8 h-8 rounded-full object-cover mt-0.5"
                     @error="avatarErrorMap[item.id] = true"
                />
                <span v-else
                      class="flex-shrink-0 w-8 h-8 rounded-full bg-primary/10 text-primary
                          flex items-center justify-center text-xs font-bold select-none mt-0.5">
                  {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
                </span>
              </router-link>
              <span v-else class="flex-shrink-0">
                <img v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                     :src="item.feedAvatar"
                     :alt="item.feedTitle"
                     class="w-8 h-8 rounded-full object-cover mt-0.5"
                     @error="avatarErrorMap[item.id] = true"/>
                <span v-else
                      class="w-8 h-8 rounded-full bg-primary/10 text-primary flex items-center justify-center
                             text-xs font-bold select-none mt-0.5">
                  {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
                </span>
              </span>

              <!-- 主体 -->
              <div class="flex-1 min-w-0 space-y-1.5">
                <!-- 来源、标题和时间 -->
                <div class="flex flex-wrap sm:flex-nowrap items-center gap-x-1.5 gap-y-0.5 text-sm leading-snug">
                  <router-link v-if="item.feedId"
                               :to="{ name: 'feed', params: { feedId: item.feedId } }"
                               class="text-gray-900 dark:text-gray-100 font-semibold truncate flex-shrink-0 max-w-[10rem] sm:max-w-[12rem]
                               rounded hover:text-primary transition-colors
                               focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
                    {{ item.feedTitle }}
                  </router-link>
                  <span v-else
                        class="text-gray-900 dark:text-gray-100 font-semibold truncate flex-shrink-0 max-w-[10rem] sm:max-w-[12rem]">
                    {{ item.feedTitle }}
                  </span>
                  <svg
                      class="w-3 h-3 flex-shrink-0 text-gray-400 dark:text-gray-600"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      aria-hidden="true"
                  >
                    <path d="m9 18 6-6-6-6"/>
                  </svg>
                  <span class="basis-full sm:basis-auto min-w-0 flex items-center gap-1.5">
                    <router-link
                        :to="{ name: 'article', params: { id: item.id } }"
                        class="text-gray-900 dark:text-gray-100 font-semibold truncate min-w-0 flex-1 rounded
                               focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
                      {{ item.title }}
                    </router-link>
                    <span class="text-gray-400 dark:text-gray-500 flex-shrink-0 whitespace-nowrap">
                      {{ item.timeAgo }}
                    </span>
                  </span>
                </div>

                <!-- 摘要 -->
                <router-link
                    v-if="item.summary"
                    :to="{ name: 'article', params: { id: item.id } }"
                    class="block rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
                  <p class="text-sm leading-relaxed text-gray-600 dark:text-gray-400"
                     :class="item.thumbnail && !thumbErrorMap[item.id] ? 'line-clamp-3' : 'line-clamp-6'">
                    {{ item.summary }}
                  </p>
                </router-link>

                <!-- 图片 -->

                <figure v-if="item.thumbnail && !thumbErrorMap[item.id]"
                        class="mt-2 aspect-video overflow-hidden rounded-xl border border-gray-200 dark:border-gray-700/50
                           bg-gray-100 dark:bg-gray-800
                           ">
                  <router-link
                      :to="{ name: 'article', params: { id: item.id } }"
                      class="block h-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
                    <img
                        :src="item.thumbnail"
                        :alt="item.title"
                        loading="lazy"
                        decoding="async"
                        class="w-full h-full object-cover"
                        @error="thumbErrorMap[item.id] = true"
                    />
                  </router-link>
                </figure>

                <!-- 标签 -->
                <div v-if="item.tags?.length" class="flex flex-wrap gap-x-3 gap-y-1 pt-1">
                  <button
                      v-for="tag in item.tags"
                      :key="tag"
                      type="button"
                      @click="emit('select-tag', tag)"
                      class="text-xs text-gray-500 dark:text-gray-400  hover:underline transition-colors">
                    #{{ tag }}
                  </button>
                </div>
              </div>
            </div>
          </article>
        </div>
      </transition>
    </section>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref, watch} from 'vue'
import {useRouter} from 'vue-router'

const router = useRouter()

function handleArticleContainerClick(item: ArticleListItemProps, event: MouseEvent) {
  const target = event.target as HTMLElement | null
  // Preserve native router-link and tag button behavior; only blank areas use the fallback.
  if (target?.closest('a, button')) return
  if (item.id) {
    router.push({name: 'article', params: {id: item.id}})
  }
}

export interface ArticleListItemProps {
  id: string
  feedId?: string
  title: string
  summary: string
  feedTitle: string
  feedAvatar?: string
  timeAgo: string
  tags?: string[]
  thumbnail?: string
}

const props = withDefaults(defineProps<{
  title: string
  subtitle?: string
  items: ArticleListItemProps[]
  loading?: boolean
  emptyMessage?: string
}>(), {
  subtitle: '',
  loading: false,
  emptyMessage: '暂无文章，添加订阅后即可看到推荐内容。'
})

const emit = defineEmits<{
  refresh: []
  'select-tag': [tag: string]
}>()

type ViewMode = 'magazine' | 'card' | 'only-title' | 'social'
const STORAGE_KEY = 'article_feed_view_mode'
const saved = localStorage.getItem(STORAGE_KEY) as ViewMode | null
const view = ref<ViewMode>(saved ?? 'magazine')

function setView(v: ViewMode) {
  view.value = v
  try {
    localStorage.setItem(STORAGE_KEY, v)
  } catch {
  }
}

function btnClass(active: boolean) {
  return [
    'p-2 rounded-full transition-all',
    active
        ? 'bg-primary/10 text-primary shadow-sm'
        : 'text-gray-500 hover:bg-surface-container'
  ].join(' ')
}

const thumbErrorMap = reactive<Record<string, boolean>>({})
const avatarErrorMap = reactive<Record<string, boolean>>({})

watch(() => props.items, (newItems) => {
  const present = new Set(newItems.map(i => i.id))
  Object.keys(thumbErrorMap).forEach(k => {
    if (!present.has(k)) delete thumbErrorMap[k]
  })
  Object.keys(avatarErrorMap).forEach(k => {
    if (!present.has(k)) delete avatarErrorMap[k]
  })
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0
}

.social-divider::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 1rem;
  right: 1rem;
  height: 1px;
  background: rgb(229 231 235 / 0.6);
}

:root.dark .social-divider::after,
.dark .social-divider::after {
  background: rgb(31 41 55 / 0.6);
}
</style>
