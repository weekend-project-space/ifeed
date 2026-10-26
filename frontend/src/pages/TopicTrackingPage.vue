<template>
  <div class="mx-auto max-w-screen-2xl px-4 pb-24 pt-5 text-text sm:px-6 lg:px-8">
    <!-- 顶部栏：MD3 干净利落 -->
    <header class="mb-5 flex items-center justify-between gap-4 border-b border-outline/15 pb-4">
      <div class="flex items-center gap-2.5">
        <h1 class="text-lg font-bold tracking-tight text-text sm:text-xl">{{ tr('topicTrackingPage.title') }}</h1>
        <span class="hidden rounded-full bg-surface-container px-3 py-0.5 text-[11px] font-semibold text-text-secondary sm:inline-block">
          Intelligence
        </span>
      </div>
      <!-- MD3 标准 Filled Button -->
      <button v-if="authStore.isAuthenticated" type="button"
              class="inline-flex h-10 items-center justify-center gap-2 rounded-full bg-primary px-5 text-xs font-semibold text-white shadow-xs transition hover:shadow-md hover:brightness-105 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-40"
              :disabled="!limitsReady" @click="openCreate">
        <svg class="h-4 w-4 stroke-[2.2]" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path stroke-linecap="round" d="M12 5v14M5 12h14" />
        </svg>
        <span>{{ tr('topicTrackingPage.createTopic') }}</span>
      </button>
    </header>

    <!-- 未登录提示条：MD3 Surface Tint 柔和横幅 -->
    <div v-if="!authStore.isAuthenticated" class="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-3xl bg-surface-container/70 p-4 sm:px-6">
      <div class="flex flex-wrap items-baseline gap-x-3 gap-y-1">
        <h2 class="text-sm font-bold text-text">{{ tr('topicTrackingPage.guestTitle') }}</h2>
        <p class="text-xs text-text-secondary">{{ tr('topicTrackingPage.guestDescription') }}</p>
      </div>
      <router-link :to="{ name: 'auth', query: { redirect: route.fullPath } }"
                   class="inline-flex h-9 items-center justify-center rounded-full bg-primary px-5 text-xs font-semibold text-white transition hover:shadow-xs active:scale-[0.98]">
        {{ tr('topicTrackingPage.signInNow') }}
      </router-link>
    </div>

    <div class="grid grid-cols-1 gap-6 lg:grid-cols-[256px_minmax(0,1fr)] lg:gap-8">
      <!-- 左侧导航：MD3 Filter Chips & Navigation Item -->
      <aside class="min-w-0 lg:sticky lg:top-5 lg:self-start">
        <!-- MD3 Filter Chips -->
        <div class="mb-3.5 flex items-center gap-1.5">
          <button v-if="authStore.isAuthenticated" type="button"
                  class="inline-flex h-8 items-center rounded-full px-3.5 text-xs font-medium transition"
                  :class="scope === 'following'
                    ? 'bg-primary/15 font-semibold text-primary'
                    : 'bg-surface-container/60 text-text-secondary hover:bg-surface-container hover:text-text'"
                  @click="changeScope('following')">{{ tr('topicTrackingPage.followingTopics') }}</button>
          <button type="button"
                  class="inline-flex h-8 items-center rounded-full px-3.5 text-xs font-medium transition"
                  :class="scope === 'public'
                    ? 'bg-primary/15 font-semibold text-primary'
                    : 'bg-surface-container/60 text-text-secondary hover:bg-surface-container hover:text-text'"
                  @click="changeScope('public')">{{ tr('topicTrackingPage.publicTopics') }}</button>
          <span class="ml-auto rounded-full bg-surface-container px-2.5 py-0.5 text-[11px] font-semibold text-text-secondary">
            {{ topics.length }}
          </span>
        </div>

        <div v-if="topics.length > 5" class="mb-3">
          <input v-model.trim="topicFilter" type="search" :placeholder="tr('topicTrackingPage.filterPlaceholder')"
                 class="w-full rounded-full border border-outline/20 bg-surface-container/40 px-4 py-2 text-xs text-text outline-none transition placeholder:text-text-secondary/60 focus:border-primary focus:bg-surface" />
        </div>

        <!-- 话题导航项 -->
        <div class="flex gap-1.5 overflow-x-auto pb-2 [scrollbar-width:none] lg:max-h-[calc(100vh-9.5rem)] lg:flex-col lg:gap-1 lg:overflow-y-auto [&::-webkit-scrollbar]:hidden">
          <button type="button"
                  class="flex h-11 w-44 shrink-0 items-center gap-3 rounded-full px-3 text-sm transition lg:w-full"
                  :class="selectedTopicId === null
                    ? 'bg-surface-container font-semibold text-text'
                    : 'text-text-secondary hover:bg-surface-container/50 hover:text-text'"
                  @click="selectTopic(null)">
            <span class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold transition-colors"
                  :class="selectedTopicId === null ? 'bg-primary text-white' : 'bg-surface-container-high text-text-secondary'">{{ tr('topicTrackingPage.allInitial') }}</span>
            <span class="min-w-0 flex-1 truncate text-left">{{ tr('topicTrackingPage.allSummaries') }}</span>
          </button>

          <div v-for="topic in filteredTopics" :key="topic.topicId" class="group relative flex shrink-0 items-center lg:w-full">
            <button type="button"
                    class="flex h-11 w-44 shrink-0 items-center gap-3 rounded-full px-3 pr-8 text-sm transition lg:w-full"
                    :class="selectedTopicId === topic.topicId
                      ? 'bg-surface-container font-semibold text-text'
                      : 'text-text-secondary hover:bg-surface-container/50 hover:text-text'"
                    @click="selectTopic(topic.topicId)">
              <span class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold transition-colors"
                    :class="selectedTopicId === topic.topicId ? 'bg-primary text-white' : 'bg-surface-container-high text-text-secondary'">
                {{ topic.name.slice(0, 1) }}
              </span>
              <span class="min-w-0 flex-1 truncate text-left">{{ topic.name }}</span>
              <span v-if="['QUEUED', 'RUNNING'].includes(topic.scanStatus)" class="h-1.5 w-1.5 shrink-0 animate-pulse rounded-full bg-primary" :title="tr('topicTrackingPage.analyzing')" />
              <span v-else-if="!topic.enabled" class="h-1.5 w-1.5 shrink-0 rounded-full bg-outline/40" :title="tr('topicTrackingPage.disabled')" />
            </button>
            <button v-if="authStore.isAuthenticated && topic.owner" type="button"
                    class="absolute right-1.5 flex h-7 w-7 items-center justify-center rounded-full text-text-secondary opacity-70 transition hover:bg-surface hover:text-text lg:opacity-0 lg:focus:opacity-100 lg:group-hover:opacity-100"
                    :aria-label="tr('topicTrackingPage.editNamedTopic', { value0: topic.name })" :title="tr('topicTrackingPage.editTopic')" @click.stop="openEdit(topic)">
              <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path stroke-linecap="round" stroke-linejoin="round" d="m16.862 4.487 2.651 2.651M4 20l4.5-.9L19.3 8.3a1.875 1.875 0 0 0-2.65-2.65L5.85 16.45 4 20Z" />
              </svg>
            </button>
          </div>
          <!-- 话题无限滚动哨兵：兼顾移动端水平滚动与桌面端垂直滚动 -->
          <div ref="topicSentinel" class="h-1 w-1 shrink-0" aria-hidden="true" />
        </div>

        <p v-if="topics.length === 0 && !topicsLoading && !topicsError" class="px-3 py-4 text-xs text-text-secondary">
          {{ scope === 'following' ? tr('topicTrackingPage.noFollowing') : tr('topicTrackingPage.noPublic') }}
        </p>
        <p v-if="topicsError" class="px-2 py-2 text-xs text-red-600">
          {{ topicsError }} <button type="button" class="underline" :disabled="topicsLoading" @click="retryTopics">{{ tr('common.retry') }}</button>
        </p>
        <p v-if="topicsLoading" class="px-2 py-2 text-xs text-text-secondary" role="status">{{ tr('topicTrackingPage.loadingTopics') }}</p>
        <button v-else-if="hasMoreTopics && !topicsError" type="button"
                class="mt-2 inline-flex h-8 w-full items-center justify-center rounded-full bg-surface-container px-4 text-xs font-medium text-text transition hover:bg-surface-container/70 active:scale-[0.98]"
                @click="loadMoreTopics">
          {{ tr('topicTrackingPage.loadMoreTopics') }}
        </button>
      </aside>

      <!-- 右侧主内容区 -->
      <main class="min-w-0">
        <!-- MD3 一体化话题看板头部 -->
        <div class="mb-5 rounded-[28px] bg-surface-container/50 p-5 sm:p-6">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div class="flex min-w-0 items-center gap-3.5">
              <span class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-base font-bold text-primary shadow-xs">
                {{ selectedTopic ? selectedTopic.name.slice(0, 1) : tr('topicTrackingPage.allInitial') }}
              </span>
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <h2 class="truncate text-lg font-bold tracking-tight sm:text-xl">{{ selectedTopic?.name || tr('topicTrackingPage.allSummaries') }}</h2>
                  <span v-if="scanMessage" role="status"
                        class="inline-flex items-center gap-1.5 rounded-full bg-surface px-3 py-0.5 text-[11px] font-medium text-text-secondary shadow-xs">
                    <span v-if="selectedTopic && ['QUEUED', 'RUNNING'].includes(selectedTopic.scanStatus)"
                          class="h-1.5 w-1.5 animate-ping rounded-full bg-primary" />
                    <span v-else class="h-1.5 w-1.5 rounded-full bg-text-secondary/60" />
                    {{ scanMessage }}
                  </span>
                </div>
                <p v-if="!selectedTopic" class="mt-0.5 text-xs text-text-secondary">
                  {{ scope === 'following' ? tr('topicTrackingPage.followingSummaryHint') : tr('topicTrackingPage.publicSummaryHint') }}
                </p>
              </div>
            </div>

            <!-- MD3 规范操作按钮群 -->
            <div v-if="selectedTopic" class="flex items-center gap-2">
              <button v-if="authStore.isAuthenticated && !selectedTopic.subscribed && selectedTopic.enabled"
                      type="button"
                      class="inline-flex h-9 items-center justify-center rounded-full bg-primary px-5 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-40"
                      :disabled="subscribing" @click="subscribeSelected">
                {{ subscribing ? tr('topicTrackingPage.followingPending') : tr('topicTrackingPage.follow') }}
              </button>
              <button v-if="authStore.isAuthenticated && selectedTopic.subscribed" type="button"
                      class="inline-flex h-9 items-center justify-center rounded-full bg-surface-container px-5 text-xs font-medium text-text transition hover:bg-surface-container-high active:scale-[0.98] disabled:opacity-40"
                      :disabled="subscribing" @click="subscribeSelected">
                {{ subscribing ? tr('common.processingEllipsis') : tr('topicTrackingPage.following') }}
              </button>
              <button v-if="selectedTopic.owner" type="button"
                      class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-surface text-text-secondary transition hover:bg-surface-container hover:text-text"
                      :aria-label="tr('topicTrackingPage.editNamedTopic', { value0: selectedTopic.name })" :title="tr('topicTrackingPage.editTopic')" @click="openEdit(selectedTopic)">
                <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path stroke-linecap="round" stroke-linejoin="round" d="m16.862 4.487 2.651 2.651M4 20l4.5-.9L19.3 8.3a1.875 1.875 0 0 0-2.65-2.65L5.85 16.45 4 20Z" />
                </svg>
              </button>
            </div>
          </div>

          <div v-if="selectedTopic" class="mt-4 flex flex-wrap items-center gap-1.5 border-t border-outline/10 pt-3 text-xs">
            <span class="font-medium text-text-secondary">{{ tr('topicTrackingPage.keywords') }}</span>
            <span v-for="kw in (selectedTopic.includeKeywords.length ? selectedTopic.includeKeywords : [selectedTopic.keyword])" :key="kw"
                  class="rounded-lg bg-surface px-2.5 py-1 font-medium text-text shadow-xs">{{ kw }}</span>

            <template v-if="selectedTopic.blockedKeywords.length">
              <span class="ml-2 font-medium text-text-secondary">{{ tr('topicTrackingPage.blocked') }}</span>
              <span v-for="bkw in selectedTopic.blockedKeywords" :key="bkw"
                    class="rounded-lg bg-surface px-2.5 py-1 font-medium text-text shadow-xs line-through">{{ bkw }}</span>
            </template>

            <details class="group ml-auto">
              <summary class="cursor-pointer rounded-lg bg-surface px-3 py-1 font-medium text-text-secondary transition hover:text-text select-none">
                {{ tr('topicTrackingPage.monitoringSites', { count: selectedTopic.sources.length }) }}
              </summary>
              <div class="mt-2.5 flex flex-wrap gap-1.5 rounded-2xl bg-surface p-3 shadow-xs">
                <span v-for="source in selectedTopic.sources" :key="source.sourceId"
                      class="rounded-md bg-surface-container px-2.5 py-0.5 text-[11px] text-text-secondary">{{ source.name }}</span>
                <p v-if="selectedTopic.relevancePrompt" class="mt-2 w-full border-t border-outline/10 pt-2 text-[11px] leading-relaxed text-text-secondary">
                  <strong class="font-semibold text-text">{{ tr('topicTrackingPage.summaryInstructions') }}</strong>{{ selectedTopic.relevancePrompt }}
                </p>
              </div>
            </details>
          </div>
        </div>

        <!-- 总结流：MD3 卡片式呈现 -->
        <div v-if="summaries.length" class="divide-y divide-outline/10">
          <article v-for="summary in summaries" :key="summary.summaryId" class="py-6 first:pt-1">
            <div class="flex items-start gap-3.5">
              <button type="button"
                      class="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-surface-container text-sm font-bold text-text transition hover:opacity-80"
                      @click="selectTopic(summary.topicId)">
                {{ summary.topicName.slice(0, 1) }}
              </button>

              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-text-secondary">
                  <button type="button" class="text-sm font-bold text-text hover:underline" @click="selectTopic(summary.topicId)">
                    {{ summary.topicName }}
                  </button>
                  <span aria-hidden="true">·</span>
                  <time :datetime="summary.createdAt">{{ formatRelativeTime(summary.createdAt) }}</time>
                  <span aria-hidden="true">·</span>
                  <span>{{ summary.titlesOnly ? tr('topicTrackingPage.titlesOnly') : tr('topicTrackingPage.fullSummary') }}</span>
                  <span class="ml-auto rounded-full bg-surface-container px-2.5 py-0.5 text-xs font-semibold text-text">
                    {{ tr('common.articleCount', { count: summary.articleCount }) }}
                  </span>
                </div>

                <p class="mt-2.5 whitespace-pre-line text-[15px] leading-relaxed text-text">{{ summary.summary }}</p>

                <!-- 关联文章 3 列网格 -->
                <div v-if="summary.articles.length" class="mt-3.5 grid grid-cols-1 gap-2.5 sm:grid-cols-2 xl:grid-cols-3">
                  <router-link v-for="article in visibleArticles(summary)" :key="article.articleId" :to="`/articles/${article.articleId}`" target="_blank"
                               class="group flex min-w-0 items-start gap-2.5 rounded-2xl bg-surface-container/45 p-3 transition hover:bg-surface-container">
                    <img v-if="article.feedIcon" :src="article.feedIcon" loading="lazy" alt="" class="mt-0.5 h-5 w-5 shrink-0 rounded-full object-cover" />
                    <span v-else class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-surface-container text-[10px] font-bold">
                      {{ article.feedName.slice(0, 1) || tr('topicTrackingPage.sourceInitial') }}
                    </span>
                    <span class="min-w-0 flex-1">
                      <span class="line-clamp-2 text-xs font-medium leading-snug text-text group-hover:underline">{{ article.title }}</span>
                      <span class="mt-1 flex items-center gap-1 text-[11px] text-text-secondary">
                        <span class="truncate">{{ article.feedName }}</span>
                        <template v-if="article.publishedAt">
                          <span aria-hidden="true" class="shrink-0">·</span>
                          <time :datetime="article.publishedAt" class="shrink-0">{{ formatRelativeTime(article.publishedAt) }}</time>
                        </template>
                      </span>
                    </span>
                    <svg class="mt-0.5 h-3.5 w-3.5 shrink-0 text-text-secondary opacity-0 transition group-hover:opacity-100" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M7 17 17 7M8 7h9v9" />
                    </svg>
                  </router-link>
                </div>

                <div v-if="summary.articleCount > summary.articles.length" class="mt-2.5 flex items-center gap-3">
                  <button type="button"
                          class="inline-flex h-7 items-center rounded-full bg-surface-container/60 px-3 text-xs font-semibold text-text transition hover:bg-surface-container"
                          @click="toggleArticles(summary)">
                    {{ expandedSummaries.has(summary.summaryId) ? tr('topicTrackingPage.collapseList') : tr('topicTrackingPage.expandArticles', { value0: summary.articleCount }) }}
                  </button>
                </div>
              </div>
            </div>
          </article>
        </div>

        <!-- 总结流的无限滚动哨兵与加载状态反馈 -->
        <div ref="summarySentinel" class="h-4" aria-hidden="true" />

        <div v-if="summariesLoading" class="py-6 text-center text-xs text-text-secondary" role="status">
          {{ tr('common.loading') }}
        </div>
        <div v-else-if="summariesError" class="py-6 text-center text-xs text-red-600">
          {{ summariesError }}
          <button type="button" class="ml-1 underline" @click="retrySummaries">{{ tr('common.retry') }}</button>
        </div>
        <div v-else-if="!hasMoreSummaries && summaries.length" class="py-6 text-center text-xs text-text-secondary/50">
          {{ tr('common.noMore') || '已加载全部总结' }}
        </div>
        <div v-else-if="!summaries.length && !summariesLoading" class="py-12 text-center text-xs text-text-secondary">
          {{ tr('topicTrackingPage.noSummaries') || '暂无相关追踪总结' }}
        </div>
      </main>
    </div>

    <!-- =========================================================================
         模态框：Google Material You (MD3) 双栏工作台
         ========================================================================= -->
    <div v-if="showEditor"
         class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-0 sm:p-5 backdrop-blur-sm"
         @click.self="closeEditor" @keydown.esc.stop.prevent="closeEditor">
      <section role="dialog" aria-modal="true" aria-labelledby="topic-editor-title" :aria-busy="saving"
               class="flex h-full w-full flex-col overflow-hidden bg-surface transition-all sm:h-auto sm:max-h-[min(92dvh,880px)] sm:rounded-[28px] sm:shadow-2xl"
               :class="!editingTopicId && createStage === 'prompt' ? 'sm:max-w-xl' : 'sm:max-w-5xl lg:h-[730px]'">

        <!-- MD3 Header -->
        <header class="flex shrink-0 items-center justify-between px-6 pt-6 pb-4 sm:px-8">
          <div>
            <h2 id="topic-editor-title" class="text-xl font-normal tracking-tight text-text">
              {{ editingTopicId ? tr('topicTrackingPage.editTrackingTopic') : createStage === 'prompt' ? tr('topicTrackingPage.newTrackingTopic') : tr('topicTrackingPage.confirmConfiguration') }}
            </h2>
            <p class="mt-1 text-xs text-text-secondary">
              {{ !editingTopicId && createStage === 'prompt' ? tr('topicTrackingPage.suggestHint') : tr('topicTrackingPage.editHint') }}
            </p>
          </div>
          <button type="button"
                  class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
                  :aria-label="tr('common.close')" :disabled="saving" @click="closeEditor">
            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </header>

        <!-- 阶段 1：AI 引导式创建 -->
        <form v-if="!editingTopicId && createStage === 'prompt'" class="flex min-h-0 flex-1 flex-col justify-between overflow-y-auto px-6 pb-6 sm:px-8" @submit.prevent="generateSuggestions">
          <div class="space-y-4">
            <!-- M3 Filled Text Field -->
            <div class="rounded-t-2xl bg-surface-container/60 px-4 pt-3 pb-2 border-b-2 border-outline/40 transition focus-within:border-primary focus-within:bg-surface-container/80">
              <span class="block text-[11px] font-medium text-text-secondary">{{ tr('topicTrackingPage.trackingPrompt') }}</span>
              <textarea v-model.trim="form.keyword"
                        class="mt-1 block min-h-32 w-full min-w-0 resize-y border-0 bg-transparent p-0 text-sm text-text outline-none ring-0 placeholder:text-text-secondary/60"
                        maxlength="240" required autofocus :placeholder="tr('topicTrackingPage.promptPlaceholder')" />
            </div>
            <p v-if="editorError" class="rounded-xl bg-red-500/10 px-4 py-2.5 text-xs text-red-600 dark:text-red-400">{{ editorError }}</p>
          </div>
          <footer class="mt-6 flex justify-end gap-3 pt-4">
            <button type="button"
                    class="inline-flex h-10 items-center justify-center rounded-full px-5 text-xs font-semibold text-text-secondary transition hover:bg-surface-container"
                    @click="closeEditor">{{ tr('common.cancel') }}</button>
            <button type="submit"
                    class="inline-flex h-10 items-center justify-center rounded-full bg-primary px-6 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 disabled:opacity-40"
                    :disabled="suggesting || !form.keyword">
              {{ suggesting ? tr('topicTrackingPage.analyzingPending') : tr('topicTrackingPage.findOrGenerate') }}
            </button>
          </footer>
        </form>

        <!-- 阶段 2：MD3 双栏工作台 -->
        <form v-else class="flex min-h-0 flex-1 flex-col overflow-hidden" @submit.prevent="saveTopic">
          <fieldset :disabled="saving"
                    class="flex min-h-0 flex-1 flex-col divide-y divide-outline/10 overflow-y-auto overscroll-contain lg:grid lg:grid-cols-12 lg:divide-x lg:divide-y-0 lg:overflow-hidden">

            <!-- 左栏 (占 5 列)：规则与 Prompt -->
            <div class="flex shrink-0 flex-col justify-between gap-5 p-6 sm:px-8 lg:col-span-5 lg:h-full lg:overflow-y-auto">
              <div class="space-y-4">
                <span class="text-xs font-semibold tracking-wide text-text-secondary">{{ tr('topicTrackingPage.basicRules') }}</span>

                <!-- M3 Filled Text Fields -->
                <div class="space-y-3">
                  <div class="rounded-t-2xl bg-surface-container/60 px-4 pt-2.5 pb-2 border-b-2 border-outline/40 transition focus-within:border-primary focus-within:bg-surface-container/80">
                    <span class="block text-[11px] font-medium text-text-secondary">{{ tr('topicTrackingPage.topicName') }}</span>
                    <input v-model.trim="form.name"
                           class="mt-0.5 block w-full min-w-0 border-0 bg-transparent p-0 text-sm text-text outline-none ring-0 placeholder:text-text-secondary/60"
                           maxlength="120" required :placeholder="tr('topicTrackingPage.namePlaceholder')" />
                  </div>

                  <div class="rounded-t-2xl bg-surface-container/60 px-4 pt-2.5 pb-2 border-b-2 border-outline/40 transition focus-within:border-primary focus-within:bg-surface-container/80">
                    <span class="block text-[11px] font-medium text-text-secondary">{{ tr('topicTrackingPage.mainKeyword') }}</span>
                    <input v-model.trim="form.keyword"
                           class="mt-0.5 block w-full min-w-0 border-0 bg-transparent p-0 text-sm text-text outline-none ring-0 placeholder:text-text-secondary/60"
                           maxlength="240" required :placeholder="tr('topicTrackingPage.keywordPlaceholder')" />
                  </div>
                </div>

                <div class="space-y-3">
                  <KeywordTagInput id="topic-include-keywords" ref="includeKeywordInput" v-model="form.includeKeywords"
                                   :label="tr('topicTrackingPage.expandedKeywords')" :max-terms="limits.maxTerms" :max-term-length="limits.maxTermLength" />
                  <KeywordTagInput id="topic-blocked-keywords" ref="blockedKeywordInput" v-model="form.blockedKeywords"
                                   :label="tr('topicTrackingPage.blockedKeywords')" blocked :max-terms="limits.maxTerms" :max-term-length="limits.maxTermLength" />
                </div>

                <div class="rounded-t-2xl bg-surface-container/60 px-4 pt-2.5 pb-2 border-b-2 border-outline/40 transition focus-within:border-primary focus-within:bg-surface-container/80">
                  <div class="flex items-center justify-between">
                    <span class="block text-[11px] font-medium text-text-secondary">{{ tr('topicTrackingPage.summaryPrompt') }}</span>
                    <span class="text-[11px] text-text-secondary">{{ (form.relevancePrompt || '').length }} / 3000</span>
                  </div>
                  <textarea v-model="form.relevancePrompt"
                            class="mt-1 block min-h-20 w-full min-w-0 resize-y border-0 bg-transparent p-0 text-xs leading-relaxed text-text outline-none ring-0 placeholder:text-text-secondary/60"
                            maxlength="3000" :placeholder="tr('topicTrackingPage.summaryPlaceholder')" />
                </div>
              </div>

              <!-- M3 Switch 硬件级触控开关 -->
              <div class="flex items-center justify-between rounded-3xl bg-surface-container/50 p-4 transition">
                <div>
                  <div class="text-xs font-semibold text-text">{{ tr('topicTrackingPage.enableScheduled') }}</div>
                  <div class="text-[11px] text-text-secondary mt-0.5">{{ tr('topicTrackingPage.scheduledHint') }}</div>
                </div>
                <button type="button" role="switch" :aria-checked="form.enabled"
                        class="relative inline-flex h-7 w-12 shrink-0 cursor-pointer rounded-full p-1 transition-colors duration-200 ease-in-out focus:outline-none"
                        :class="form.enabled ? 'bg-primary' : 'bg-surface-container-high border-2 border-outline/40'"
                        @click="form.enabled = !form.enabled">
                  <span class="pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-xs transition duration-200 ease-in-out"
                        :class="form.enabled ? 'translate-x-5' : 'translate-x-0 bg-outline/60'" />
                </button>
              </div>
            </div>

            <!-- 右栏 (占 7 列)：MD3 站点选择工作区 -->
            <div class="flex shrink-0 flex-col justify-between bg-surface-container/20 p-6 sm:px-8 lg:col-span-7 lg:h-full lg:min-h-0 lg:overflow-hidden">
              <div class="flex min-h-0 flex-1 flex-col">
                <div class="mb-4 flex shrink-0 flex-wrap items-center justify-between gap-3">
                  <span class="text-xs font-semibold tracking-wide text-text-secondary">{{ tr('topicTrackingPage.sourceConfiguration') }}</span>
                  <div class="flex items-center gap-3">
                    <button v-if="displayedEditorSources.length" type="button"
                            class="text-xs font-medium text-primary hover:underline"
                            @click="toggleBatchSources">
                      {{ isAllDisplayedSelected ? tr('topicTrackingPage.deselectAll') : tr('topicTrackingPage.selectDisplayed') }}
                    </button>
                    <!-- MD3 进度胶囊指示条 -->
                    <div class="flex items-center gap-2.5 rounded-full bg-surface px-3.5 py-1 shadow-xs">
                      <div class="w-16 h-1.5 rounded-full bg-surface-container overflow-hidden">
                        <div class="h-full bg-primary transition-all duration-300"
                             :style="{ width: `${Math.min(100, (form.sourceIds.length / limits.maxSources) * 100)}%` }" />
                      </div>
                      <span class="text-[11px] font-semibold text-text">
                        {{ form.sourceIds.length }} / {{ limits.maxSources }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- MD3 搜索胶囊 -->
                <div class="mb-3.5 flex shrink-0 items-center gap-2">
                  <div class="flex min-w-0 flex-1 items-center gap-3 rounded-full bg-surface px-4 py-2 shadow-xs transition focus-within:ring-2 focus-within:ring-primary/20">
                    <svg class="h-4 w-4 shrink-0 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                    <input v-model="sourceSearchTerm"
                           class="block w-full min-w-0 border-0 bg-transparent p-0 text-xs text-text outline-none ring-0 placeholder:text-text-secondary/60"
                           :aria-label="tr('topicTrackingPage.searchSources')" :placeholder="tr('topicTrackingPage.sourcePlaceholder')"
                           @keydown.enter.prevent="searchSources()" />
                  </div>
                  <button type="button"
                          class="inline-flex h-9 shrink-0 items-center justify-center rounded-full bg-surface px-4 text-xs font-medium text-text shadow-xs transition hover:bg-surface-container active:scale-[0.98] disabled:opacity-40"
                          :disabled="searchingSources" @click="searchSources()">
                    {{ searchingSources ? tr('topicTrackingPage.searching') : sourceSearchTerm.trim() ? tr('topicTrackingPage.search') : tr('topicTrackingPage.browseSources') }}
                  </button>
                </div>

                <!-- MD3 Filter Chips 标签分段 -->
                <div class="mb-3.5 flex shrink-0 items-center gap-2 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
                  <button v-for="tab in sourceFilterTabs" :key="tab.id" type="button"
                          class="inline-flex h-8 shrink-0 items-center gap-1.5 rounded-full px-3.5 text-xs font-medium transition"
                          :class="editorSourceTab === tab.id
                            ? 'bg-primary/15 font-semibold text-primary'
                            : 'bg-surface text-text-secondary hover:bg-surface-container hover:text-text'"
                          @click="editorSourceTab = tab.id">
                    <span>{{ tab.label }}</span>
                    <span class="rounded-full bg-surface-container px-1.5 py-0.2 text-[10px]">{{ tab.count }}</span>
                  </button>
                </div>

                <div v-if="sourceSearchError" class="mb-2 shrink-0 rounded-2xl bg-red-500/10 px-3.5 py-2 text-xs text-red-600 dark:text-red-400">
                  {{ sourceSearchError }}
                </div>

                <!-- 站点卡片列表：MD3 柔和卡片态 -->
                <div class="min-h-[220px] max-h-[360px] w-full min-w-0 overflow-y-auto pr-1 lg:h-[380px] lg:max-h-none">
                  <div v-if="displayedEditorSources.length" class="grid grid-cols-1 content-start gap-2.5 sm:grid-cols-2">
                    <div v-for="source in displayedEditorSources" :key="source.sourceId"
                         @click="!isSourceDisabled(source.sourceId) && toggleSource(source.sourceId)"
                         class="group flex min-w-0 cursor-pointer items-start gap-3 rounded-2xl p-3 border transition select-none"
                         :class="[
                           form.sourceIds.includes(source.sourceId)
                             ? 'border-primary/40 bg-primary/8 shadow-xs'
                             : 'border-transparent bg-surface hover:border-outline/30 hover:bg-surface-container/60',
                           isSourceDisabled(source.sourceId) ? 'cursor-not-allowed opacity-50' : ''
                         ]">
                      <div class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-lg transition"
                           :class="form.sourceIds.includes(source.sourceId) ? 'bg-primary text-white' : 'border-2 border-outline/50'">
                        <svg v-if="form.sourceIds.includes(source.sourceId)" class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                        </svg>
                      </div>
                      <div class="min-w-0 flex-1">
                        <span class="block truncate text-xs font-semibold text-text">{{ source.name }}</span>
                        <span class="mt-0.5 block truncate text-[11px] text-text-secondary">{{ source.url }}</span>
                      </div>
                    </div>
                  </div>

                  <div v-else class="flex h-[220px] w-full flex-col items-center justify-center rounded-3xl border-2 border-dashed border-outline/20 p-6 text-center lg:h-full">
                    <p class="text-xs text-text-secondary">
                      {{ editorSourceTab === 'selected' ? tr('topicTrackingPage.noSelectedSources') : tr('topicTrackingPage.noMatchingSources') }}
                    </p>
                    <button v-if="editorSourceTab !== 'all'" type="button"
                            class="mt-3 inline-flex h-8 items-center justify-center rounded-full bg-surface px-4 text-xs font-medium text-primary shadow-xs transition hover:bg-surface-container"
                            @click="editorSourceTab = 'all'">
                      {{ tr('topicTrackingPage.backToAllSources') }}
                    </button>
                  </div>
                </div>

                <div class="mt-3 flex shrink-0 items-center justify-between">
                  <p v-if="!form.sourceIds.length" class="text-xs font-medium text-red-600 dark:text-red-400">
                    {{ tr('topicTrackingPage.selectSourceHint') }}
                  </p>
                  <span v-else class="text-[11px] text-text-secondary">
                    {{ tr('topicTrackingPage.selectedFirstHint') }}
                  </span>
                  <button v-if="sourcePage + 1 < sourceTotalPages" type="button"
                          class="ml-auto inline-flex h-8 items-center justify-center rounded-full bg-surface px-4 text-xs font-medium text-text shadow-xs transition hover:bg-surface-container disabled:opacity-40"
                          :disabled="searchingSources" @click="searchSources(sourcePage + 1, true)">
                    {{ searchingSources ? tr('common.loading') : tr('topicTrackingPage.moreSources') }}
                  </button>
                </div>
              </div>

              <!-- 底部操作栏 -->
              <div class="mt-5 shrink-0 pt-3">
                <p v-if="editorError" class="mb-3 rounded-2xl bg-red-500/10 px-4 py-2.5 text-xs text-red-600 dark:text-red-400">
                  {{ editorError }}
                </p>
                <footer class="flex flex-wrap items-center justify-end gap-3">
                  <button v-if="!editingTopicId" type="button"
                          class="mr-auto inline-flex h-10 items-center justify-center rounded-full px-4 text-xs font-semibold text-text-secondary transition hover:bg-surface-container/60 disabled:opacity-40"
                          :disabled="saving" @click="createStage = 'prompt'; editorError = ''">
                    {{ tr('topicTrackingPage.backToPrompt') }}
                  </button>
                  <button type="button"
                          class="inline-flex h-10 items-center justify-center rounded-full px-5 text-xs font-semibold text-text-secondary transition hover:bg-surface-container disabled:opacity-40"
                          :disabled="saving" @click="closeEditor">{{ tr('common.cancel') }}</button>
                  <button type="submit"
                          class="inline-flex h-10 items-center justify-center rounded-full bg-primary px-7 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-[0.98] disabled:opacity-40"
                          :disabled="saving">
                    {{ saving ? tr('topicTrackingPage.saving') : editingTopicId ? tr('topicTrackingPage.saveChanges') : tr('topicTrackingPage.startTracking') }}
                  </button>
                </footer>
              </div>
            </div>

          </fieldset>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { formatRelativeTime } from '@/utils/datetime';
import { latestRequest } from '@/utils/latestRequest';
import { useDocumentTitle } from '@/composables/documentTitle';
import { useInfiniteScroll } from '@/composables/infiniteScroll';
import { listDiscoveryFeeds } from '@/api/discovery';
import KeywordTagInput from '@/components/KeywordTagInput.vue';
import {
  createTopicTrack, getTopicTrack, getTopicTrackLimits, getTopicTrackStates, listTopicTrackSummaries, listTopicSummaryArticles,
  listPublicTopicTracks, listTopicTracks, suggestTopicTrack, subscribeTopicTrack,
  unsubscribeTopicTrack, updateTopicTrack,
  type TopicSourceOption, type TopicTrack,
  type TopicTrackConfig, type TopicTrackSummary, type TopicSummaryArticle,
} from '@/api/topicTracking';

defineOptions({ name: 'TopicTrackingPage' });

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const parseQueryString = (val: unknown): string | null =>
    typeof val === 'string' && val.trim() ? val.trim() : Array.isArray(val) && typeof val[0] === 'string' && val[0].trim() ? val[0].trim() : null;

const getQueryTopicId = () => parseQueryString(route.query.topic) ?? parseQueryString(route.query.topicId);
const getQueryScope = (): 'following' | 'public' | null => {
  const s = parseQueryString(route.query.scope);
  if (s === 'public') return 'public';
  if (s === 'following' && authStore.isAuthenticated) return 'following';
  return null;
};

const scope = ref<'following' | 'public'>(getQueryScope() ?? (authStore.isAuthenticated ? 'following' : 'public'));
const topics = ref<TopicTrack[]>([]);
const topicFilter = ref('');
const summaries = ref<TopicTrackSummary[]>([]);
const topicDetail = ref<TopicTrack | null>(null);
const selectedTopicId = ref<string | null>(getQueryTopicId());
const reusedTopicId = ref<string | null>(null);
const subscribing = ref(false);
const topicsLoading = ref(false);
const summariesLoading = ref(false);
const error = ref('');
const detailError = ref('');
const summariesError = ref('');
const pollError = ref('');
const limitsError = ref('');
const topicsError = ref('');
const summaryCursor = ref<string>();
const hasMoreSummaries = ref(false);
const newSummariesAvailable = ref(false);
const topicPage = ref(0);
const topicTotalPages = ref(0);
const expandedSummaries = ref(new Set<string>());
type ArticlePage = { articles: TopicSummaryArticle[]; cursor?: string; hasNext: boolean; loading: boolean; error: string };
const articlePages = reactive<Record<string, ArticlePage>>({});
const articleRequests = new Map<string, ReturnType<typeof latestRequest>>();
const showEditor = ref(false);
const editingTopicId = ref<string | null>(null);
const createStage = ref<'prompt' | 'config'>('prompt');
const suggesting = ref(false);
const saving = ref(false);
const editorError = ref('');
const sourceOptions = ref<TopicSourceOption[]>([]);
const sourceSearchTerm = ref('');
const sourceSearchResults = ref<TopicSourceOption[]>([]);
const sourceSearchError = ref('');
const searchingSources = ref(false);
const sourcePage = ref(0);
const sourceTotalPages = ref(0);
const limits = ref({ maxCategories: 3, maxSources: 30, maxTerms: 30, maxTermLength: 80 });
const limitsReady = ref(false);

const editorSourceTab = ref<'all' | 'selected' | 'unselected'>('all');

const topicsRequest = latestRequest();
const summariesRequest = latestRequest();
const detailRequest = latestRequest();
const sourceRequest = latestRequest();
const suggestionRequest = latestRequest();
const pollRequest = latestRequest();
let pollTimer: ReturnType<typeof setTimeout> | undefined;
let disposed = false;
let authGeneration = 0;
let failedTopicRequest = { page: 0, append: false };
let failedSummaryAppend = false;
let failedSummaryReplace = false;

const syncRouteQuery = (topicId: string | null = selectedTopicId.value, currentScope: 'following' | 'public' = scope.value) => {
  const nextQuery: Record<string, string> = {};
  Object.entries(route.query).forEach(([key, value]) => {
    if (key !== 'topic' && key !== 'topicId' && key !== 'scope' && typeof value === 'string') {
      nextQuery[key] = value;
    }
  });
  if (topicId) nextQuery.topic = topicId;
  const defaultScope = authStore.isAuthenticated ? 'following' : 'public';
  if (currentScope !== defaultScope) nextQuery.scope = currentScope;

  const currentTopic = getQueryTopicId();
  const currentQueryScope = getQueryScope() ?? defaultScope;
  if (currentTopic === topicId && currentQueryScope === currentScope && !route.query.topicId) return;

  void router.replace({ query: nextQuery });
};

const blankConfig = (): TopicTrackConfig => ({
  name: '', keyword: '', includeKeywords: [], blockedKeywords: [], sourceIds: [],
  categories: [],
  relevancePrompt: '', enabled: true,
});
const form = ref<TopicTrackConfig>(blankConfig());
const includeKeywordInput = ref<InstanceType<typeof KeywordTagInput> | null>(null);
const blockedKeywordInput = ref<InstanceType<typeof KeywordTagInput> | null>(null);
const selectedTopic = computed(() => topicDetail.value?.topicId === selectedTopicId.value
    ? topicDetail.value : topics.value.find(topic => topic.topicId === selectedTopicId.value) ?? null);
const hasMoreTopics = computed(() => topicPage.value + 1 < topicTotalPages.value);

useDocumentTitle(() => (
    selectedTopic.value?.name?.trim()
        ? tr('topicTrackingPage.topicDocumentTitle', { value0: selectedTopic.value.name.trim() })
        : tr('topicTrackingPage.title')
));

const filteredTopics = computed(() => {
  const q = topicFilter.value.toLowerCase();
  if (!q) return topics.value;
  return topics.value.filter(t => t.name.toLowerCase().includes(q) || t.keyword.toLowerCase().includes(q));
});

const availableSources = computed(() => {
  const searchIds = new Set(sourceSearchResults.value.map(s => s.sourceId));
  const sources = new Map(sourceOptions.value.map(source => [source.sourceId, source]));
  sourceSearchResults.value.forEach(source => sources.set(source.sourceId, source));
  return [...sources.values()].sort((first, second) => {
    const inSearchDiff = Number(searchIds.has(second.sourceId)) - Number(searchIds.has(first.sourceId));
    if (inSearchDiff !== 0) return inSearchDiff;
    return Number(form.value.sourceIds.includes(second.sourceId)) - Number(form.value.sourceIds.includes(first.sourceId));
  });
});

const sourceFilterTabs = computed(() => {
  const allCount = availableSources.value.length;
  const selectedCount = availableSources.value.filter(s => form.value.sourceIds.includes(s.sourceId)).length;
  return [
    { id: 'all' as const, label: tr('topicTrackingPage.allSources'), count: allCount },
    { id: 'selected' as const, label: tr('topicTrackingPage.selected'), count: selectedCount },
    { id: 'unselected' as const, label: tr('topicTrackingPage.unselected'), count: Math.max(0, allCount - selectedCount) },
  ];
});

const displayedEditorSources = computed(() => {
  if (editorSourceTab.value === 'selected') {
    return availableSources.value.filter(s => form.value.sourceIds.includes(s.sourceId));
  }
  if (editorSourceTab.value === 'unselected') {
    return availableSources.value.filter(s => !form.value.sourceIds.includes(s.sourceId));
  }
  return availableSources.value;
});

const isAllDisplayedSelected = computed(() =>
    displayedEditorSources.value.length > 0 &&
    displayedEditorSources.value.every(s => form.value.sourceIds.includes(s.sourceId))
);

const isSourceDisabled = (sourceId: string) =>
    !form.value.sourceIds.includes(sourceId) && form.value.sourceIds.length >= limits.value.maxSources;

const toggleSource = (sourceId: string) => {
  const idx = form.value.sourceIds.indexOf(sourceId);
  if (idx > -1) {
    form.value.sourceIds.splice(idx, 1);
  } else if (form.value.sourceIds.length < limits.value.maxSources) {
    form.value.sourceIds.push(sourceId);
  }
};

const toggleBatchSources = () => {
  if (isAllDisplayedSelected.value) {
    const displayedIds = new Set(displayedEditorSources.value.map(s => s.sourceId));
    form.value.sourceIds = form.value.sourceIds.filter(id => !displayedIds.has(id));
  } else {
    const nextIds = new Set(form.value.sourceIds);
    for (const source of displayedEditorSources.value) {
      if (nextIds.size >= limits.value.maxSources) break;
      nextIds.add(source.sourceId);
    }
    form.value.sourceIds = [...nextIds];
  }
};

const scanMessage = computed(() => {
  const topic = selectedTopic.value;
  if (!topic) return '';
  const labels: Record<string, string> = {
    IDLE: tr('topicTrackingPage.scanIdle'), QUEUED: tr('topicTrackingPage.scanQueued'),
    RUNNING: tr('topicTrackingPage.scanRunning'),
    FAILED: topic.scanError || tr('topicTrackingPage.scanFailed'),
    SUCCEEDED: tr('topicTrackingPage.scanSucceeded'), DISABLED: tr('topicTrackingPage.scanDisabled'),
  };
  return labels[topic.scanStatus] || '';
});

const mergeTopics = (rows: TopicTrack[]) => {
  const byId = new Map(topics.value.map(topic => [topic.topicId, topic]));
  rows.forEach(topic => byId.set(topic.topicId, topic));
  topics.value = [...byId.values()];
};

const loadTopics = async (page = 0, append = false) => {
  const request = topicsRequest.begin();
  topicsLoading.value = true;
  topicsError.value = '';
  try {
    const result = scope.value === 'following'
        ? await listTopicTracks(page, 30, request.signal)
        : await listPublicTopicTracks(page, 30, request.signal);
    if (!request.current()) return;
    if (append) mergeTopics(result.content);
    else {
      topics.value = result.content;
      if (topicDetail.value && topicDetail.value.topicId === selectedTopicId.value) {
        mergeTopics([topicDetail.value]);
      }
    }
    topicPage.value = result.number;
    topicTotalPages.value = result.totalPages;
  } catch (reason) {
    if (request.current()) {
      topicsError.value = topicErrorMessage(reason, tr('topicTrackingPage.topicsFailed'));
      failedTopicRequest = { page, append };
    }
  } finally {
    if (request.current()) topicsLoading.value = false;
  }
};

const loadMoreTopics = async () => {
  if (!topicsLoading.value && hasMoreTopics.value) await loadTopics(topicPage.value + 1, true);
};

const retryTopics = () => loadTopics(failedTopicRequest.page, failedTopicRequest.append);

const resetArticles = () => {
  articleRequests.forEach(request => request.cancel());
  articleRequests.clear();
  Object.keys(articlePages).forEach(id => delete articlePages[id]);
  expandedSummaries.value.clear();
};

const loadSummaries = async (append = false, reset = false, replace = false) => {
  if (append && summariesLoading.value) return;
  const request = summariesRequest.begin();
  const topicId = selectedTopicId.value;
  summariesLoading.value = true;
  summariesError.value = '';
  if (reset) {
    newSummariesAvailable.value = false;
    summaries.value = [];
    summaryCursor.value = undefined;
    hasMoreSummaries.value = false;
    resetArticles();
  }
  try {
    const result = await listTopicTrackSummaries(topicId ?? undefined,
        request.signal, scope.value === 'following', append ? summaryCursor.value : undefined);
    if (!request.current() || selectedTopicId.value !== topicId) return;
    const preserveCursor = !append && !replace && summaries.value.length > 0;
    const byId = new Map((append ? summaries.value : result.content).map(summary => [summary.summaryId, summary]));
    if (!append && !replace) summaries.value.forEach(summary => { if (!byId.has(summary.summaryId)) byId.set(summary.summaryId, summary); });
    result.content.forEach(summary => byId.set(summary.summaryId, summary));
    summaries.value = [...byId.values()];
    if (replace) {
      resetArticles();
      newSummariesAvailable.value = false;
    }
    if (!preserveCursor) {
      summaryCursor.value = result.nextCursor ?? undefined;
      hasMoreSummaries.value = Boolean(result.hasNext && result.nextCursor);
    }
  } catch (reason) {
    if (request.current()) {
      summariesError.value = topicErrorMessage(reason, tr('topicTrackingPage.summariesFailed'));
      failedSummaryAppend = append;
      failedSummaryReplace = replace;
    }
  } finally {
    if (request.current()) summariesLoading.value = false;
  }
};

const retrySummaries = () => loadSummaries(failedSummaryAppend, false, failedSummaryReplace);

const visibleArticles = (summary: TopicTrackSummary) => expandedSummaries.value.has(summary.summaryId)
    ? articlePages[summary.summaryId]?.articles ?? summary.articles : summary.articles;

const loadArticles = async (summary: TopicTrackSummary) => {
  articlePages[summary.summaryId] ??= {
    articles: [...summary.articles],
    cursor: summary.articles[summary.articles.length - 1]?.articleId,
    hasNext: summary.articleCount > summary.articles.length,
    loading: false,
    error: '',
  };
  const state = articlePages[summary.summaryId];
  if (state.loading || !state.hasNext) return;
  let requests = articleRequests.get(summary.summaryId);
  if (!requests) { requests = latestRequest(); articleRequests.set(summary.summaryId, requests); }
  const request = requests.begin();
  state.loading = true;
  state.error = '';
  try {
    const page = await listTopicSummaryArticles(summary.summaryId, state.cursor, request.signal);
    if (!request.current()) return;
    const articles = new Map(state.articles.map(article => [article.articleId, article]));
    page.content.forEach(article => articles.set(article.articleId, article));
    state.articles = [...articles.values()];
    state.cursor = page.nextCursor ?? undefined;
    state.hasNext = page.hasNext;
  } catch (reason) {
    if (request.current()) state.error = topicErrorMessage(reason, tr('topicTrackingPage.relatedArticlesFailed'));
  } finally {
    if (request.current()) state.loading = false;
  }
};

const toggleArticles = (summary: TopicTrackSummary) => {
  if (expandedSummaries.value.has(summary.summaryId)) expandedSummaries.value.delete(summary.summaryId);
  else {
    expandedSummaries.value.add(summary.summaryId);
    if (!articlePages[summary.summaryId]) void loadArticles(summary);
  }
};

const selectTopic = async (topicId: string | null, updateUrl = true) => {
  detailRequest.cancel();
  pollRequest.cancel();
  selectedTopicId.value = topicId;
  reusedTopicId.value = null;
  error.value = '';
  detailError.value = '';
  pollError.value = '';
  topicDetail.value = topicId ? topics.value.find(topic => topic.topicId === topicId) ?? null : null;
  if (updateUrl) syncRouteQuery(topicId, scope.value);
  void loadSummaries(false, true);
  if (!topicId) return;
  const request = detailRequest.begin();
  const version = topicDetail.value?.configVersion;
  try {
    const detail = await getTopicTrack(topicId, request.signal);
    if (request.current() && selectedTopicId.value === topicId) {
      topicDetail.value = detail;
      if (scope.value === 'following' && !detail.subscribed) {
        scope.value = 'public';
        if (updateUrl) syncRouteQuery(topicId, 'public');
        void loadTopics();
      }
      mergeTopics([detail]);
      if (version !== undefined && version !== detail.configVersion) void loadSummaries(false, true);
    }
  } catch (reason) {
    if (request.current()) detailError.value = topicErrorMessage(reason, tr('topicTrackingPage.topicsFailed'));
  }
};

const changeScope = (next: 'following' | 'public') => {
  if (next === scope.value || next === 'following' && !authStore.isAuthenticated) return;
  scope.value = next;
  topicFilter.value = '';
  pollRequest.cancel();
  topics.value = [];
  topicTotalPages.value = 0;
  void selectTopic(null);
  void loadTopics();
};

const loadMoreSummaries = async () => {
  if (!summariesLoading.value && hasMoreSummaries.value) await loadSummaries(true);
};

// 1. 话题列表无限滚动挂载
const { sentinel: topicSentinel } = useInfiniteScroll({
  hasNextPage: hasMoreTopics, isLoading: topicsLoading, hasError: () => !!topicsError.value,
  onLoadMore: loadMoreTopics, rootMargin: '100px 0px',
});

// 2. 核心主内容区：总结流无限滚动挂载
const { sentinel: summarySentinel } = useInfiniteScroll({
  hasNextPage: hasMoreSummaries,
  isLoading: summariesLoading,
  hasError: () => !!summariesError.value,
  onLoadMore: loadMoreSummaries,
  rootMargin: '250px 0px',
});

const subscribeSelected = async () => {
  const topic = selectedTopic.value;
  if (!topic || subscribing.value || !authStore.isAuthenticated) return;
  const generation = authGeneration;
  subscribing.value = true;
  error.value = '';
  try {
    const updated = topic.subscribed
        ? await unsubscribeTopicTrack(topic.topicId) : await subscribeTopicTrack(topic.topicId);
    if (disposed || generation !== authGeneration) return;
    if (selectedTopicId.value === updated.topicId) topicDetail.value = updated;
    if (!updated.subscribed && scope.value === 'following') {
      topics.value = topics.value.filter(item => item.topicId !== updated.topicId);
      if (selectedTopicId.value === updated.topicId) void selectTopic(null);
    } else mergeTopics([updated]);
  } catch (reason) {
    if (!disposed && generation === authGeneration) error.value = topicErrorMessage(reason, tr('topicTrackingPage.followFailed'));
  } finally {
    if (generation === authGeneration) subscribing.value = false;
  }
};

const resetSourceSearch = () => {
  sourceRequest.cancel();
  searchingSources.value = false;
  sourceSearchTerm.value = '';
  sourceSearchResults.value = [];
  sourceSearchError.value = '';
  sourcePage.value = 0;
  sourceTotalPages.value = 0;
  editorSourceTab.value = 'all';
};

const resetEditorRequests = () => {
  suggestionRequest.cancel();
  suggesting.value = false;
  saving.value = false;
  resetSourceSearch();
};

const closeEditor = () => { if (!saving.value) showEditor.value = false; };

const openCreate = () => {
  if (!authStore.isAuthenticated || !limitsReady.value) return;
  resetEditorRequests();
  reusedTopicId.value = null;
  editingTopicId.value = null;
  createStage.value = 'prompt';
  form.value = blankConfig();
  sourceOptions.value = [];
  editorError.value = '';
  showEditor.value = true;
};

const openEdit = (topic: TopicTrack) => {
  if (!authStore.isAuthenticated || !topic.owner) return;
  if (!limitsReady.value) {
    error.value = tr('topicTrackingPage.limitsNotReady');
    return;
  }
  resetEditorRequests();
  reusedTopicId.value = null;
  editingTopicId.value = topic.topicId;
  createStage.value = 'config';
  form.value = {
    name: topic.name, keyword: topic.keyword, includeKeywords: [...topic.includeKeywords],
    blockedKeywords: [...topic.blockedKeywords], sourceIds: topic.sources.map(source => source.sourceId),
    categories: [], relevancePrompt: topic.relevancePrompt, enabled: topic.enabled,
  };
  sourceOptions.value = [...topic.sources];
  editorError.value = '';
  showEditor.value = true;
};

const generateSuggestions = async () => {
  if (!form.value.keyword.trim() || suggesting.value) return;
  const currentForm = form.value;
  const keyword = currentForm.keyword.trim();
  const request = suggestionRequest.begin();
  suggesting.value = true;
  editorError.value = '';
  try {
    const suggestions = await suggestTopicTrack(keyword, request.signal);
    if (!request.current() || !showEditor.value || currentForm !== form.value || keyword !== form.value.keyword.trim()) return;
    if (suggestions.existingTopic) {
      const existing = suggestions.existingTopic;
      if (scope.value === 'following' && !existing.subscribed) {
        scope.value = 'public';
        await loadTopics();
        if (!request.current() || currentForm !== form.value || !showEditor.value) return;
      }
      mergeTopics([existing]);
      void selectTopic(existing.topicId);
      reusedTopicId.value = existing.topicId;
      showEditor.value = false;
      return;
    }
    form.value.name ||= keyword.slice(0, 120);
    form.value.includeKeywords = suggestions.includeKeywords;
    form.value.blockedKeywords = suggestions.blockedKeywords;
    form.value.categories = [];
    form.value.relevancePrompt = suggestions.relevancePrompt;
    sourceOptions.value = [...new Map(suggestions.sources.map(source => [source.sourceId, source])).values()];
    form.value.sourceIds = sourceOptions.value.map(source => source.sourceId);
    createStage.value = 'config';
  } catch (reason) {
    if (request.current() && showEditor.value && currentForm === form.value && keyword === form.value.keyword.trim()) {
      editorError.value = topicErrorMessage(reason, tr('topicTrackingPage.suggestionFailed'));
    }
  } finally {
    if (request.current()) suggesting.value = false;
  }
};

const searchSources = async (page = 0, append = false) => {
  if (searchingSources.value) return;
  const currentForm = form.value;
  const term = sourceSearchTerm.value;
  const request = sourceRequest.begin();
  searchingSources.value = true;
  sourceSearchError.value = '';
  if (!append) sourceSearchResults.value = [];
  try {
    const result = await listDiscoveryFeeds({ page, size: 20, query: term, signal: request.signal });
    if (!request.current() || !showEditor.value || currentForm !== form.value || sourceSearchTerm.value !== term) return;
    const rows = result.content.map(feed => ({ sourceId: feed.feedId, name: feed.name, url: feed.siteUrl || feed.url }));
    sourceSearchResults.value = [...new Map([...(append ? sourceSearchResults.value : []), ...rows]
        .map(source => [source.sourceId, source])).values()];
    sourcePage.value = result.number;
    sourceTotalPages.value = result.totalPages;
    if (!result.content.length && !append) sourceSearchError.value = tr('topicTrackingPage.sourcesNotFound');
  } catch (reason) {
    if (request.current()) sourceSearchError.value = topicErrorMessage(reason, tr('topicTrackingPage.sourceSearchFailed'));
  } finally {
    if (request.current()) searchingSources.value = false;
  }
};

const saveTopic = async () => {
  if (saving.value || !authStore.isAuthenticated) return;
  const includeValid = includeKeywordInput.value?.commit() ?? true;
  const blockedValid = blockedKeywordInput.value?.commit() ?? true;
  if (!includeValid || !blockedValid) {
    editorError.value = tr('topicTrackingPage.keywordsOverLimit');
    return;
  }
  const currentForm = form.value;
  const topicId = editingTopicId.value;
  const generation = authGeneration;
  const config: TopicTrackConfig = {
    ...currentForm, sourceIds: [...currentForm.sourceIds], categories: [...currentForm.categories],
    includeKeywords: [...currentForm.includeKeywords], blockedKeywords: [...currentForm.blockedKeywords],
  };
  if (!config.name || !config.keyword || config.name.length > 120 || config.keyword.length > 240) {
    editorError.value = tr('topicTrackingPage.nameAndPromptRequired'); return;
  }
  if (!config.sourceIds.length || config.sourceIds.length > limits.value.maxSources) {
    editorError.value = tr('topicTrackingPage.sourceCountLimit', { value0: limits.value.maxSources }); return;
  }
  if ([config.includeKeywords, config.blockedKeywords].some(terms =>
      terms.length > limits.value.maxTerms || terms.some(term => term.length > limits.value.maxTermLength))) {
    editorError.value = tr('topicTrackingPage.keywordLimits', { value0: limits.value.maxTerms, value1: limits.value.maxTermLength }); return;
  }
  saving.value = true;
  editorError.value = '';
  const current = () => !disposed && generation === authGeneration && currentForm === form.value && showEditor.value;
  try {
    const saved = topicId ? await updateTopicTrack(topicId, config) : await createTopicTrack(config);
    if (!current()) return;
    detailRequest.cancel();
    pollRequest.cancel();
    showEditor.value = false;
    topicDetail.value = saved;
    selectedTopicId.value = saved.topicId;
    syncRouteQuery(saved.topicId, scope.value);
    if (scope.value === 'public' || saved.subscribed) mergeTopics([saved]);
    reusedTopicId.value = null;
    await loadSummaries(false, true);
  } catch (reason) {
    if (current()) editorError.value = topicErrorMessage(reason, tr('topicTrackingPage.saveFailed'));
  } finally {
    if (currentForm === form.value && generation === authGeneration) saving.value = false;
  }
};

const topicErrorMessage = (reason: unknown, fallback: string) => {
  if (!(reason instanceof Error)) return fallback;
  const messages: Record<string, string> = {
    TOPIC_SOURCE_REQUIRED: tr('topicTrackingPage.sourceRequired'),
    TOPIC_KEYWORD_EXISTS: tr('topicTrackingPage.duplicateTopic'),
    TOPIC_TRACK_DISABLED: tr('topicTrackingPage.existingTopicDisabled'),
    TOPIC_SUGGESTION_AI_FAILED: tr('topicTrackingPage.suggestionUnavailable'),
    INVALID_TOPIC_TERMS: tr('topicTrackingPage.invalidKeywords'),
    INVALID_TOPIC_CURSOR: tr('topicTrackingPage.listChanged'),
    TOPIC_SUMMARY_NOT_FOUND: tr('topicTrackingPage.summaryExpired'),
  };
  return messages[reason.message] || reason.message || fallback;
};

const poll = async () => {
  if (disposed) return;
  const request = pollRequest.begin();
  const topicId = selectedTopicId.value;
  const ids = [...new Set([...(topicId ? [topicId] : []), ...topics.value.map(topic => topic.topicId)])];
  try {
    const batches: string[][] = [];
    for (let start = 0; start < ids.length; start += 100) batches.push(ids.slice(start, start + 100));
    const results = (await Promise.all(batches.map(batch => getTopicTrackStates(batch, request.signal)))).flat();
    if (!request.current()) return;
    let changed = false;
    let configChanged = false;
    for (const updated of results) {
      const previous = updated.topicId === selectedTopicId.value ? selectedTopic.value
          : topics.value.find(topic => topic.topicId === updated.topicId);
      const relevant = topicId === null || topicId === updated.topicId;
      if (previous && relevant && (previous.scanFinishedAt !== updated.scanFinishedAt || previous.configVersion !== updated.configVersion)) changed = true;
      if (previous && relevant && previous.configVersion !== updated.configVersion) configChanged = true;
      const index = topics.value.findIndex(topic => topic.topicId === updated.topicId);
      if (index >= 0) topics.value[index] = { ...topics.value[index], ...updated };
      if (selectedTopicId.value === updated.topicId) {
        detailRequest.cancel();
        if (previous?.configVersion !== updated.configVersion) {
          const detail = await getTopicTrack(updated.topicId, request.signal);
          if (!request.current()) return;
          topicDetail.value = detail;
        } else if (topicDetail.value) topicDetail.value = { ...topicDetail.value, ...updated };
      }
    }
    pollError.value = '';
    if (changed && selectedTopicId.value === topicId) {
      if (configChanged || summaries.value.length === 0) await loadSummaries(false, configChanged);
      else newSummariesAvailable.value = true;
    }
  } catch {
    if (request.current()) pollError.value = tr('topicTrackingPage.statusUpdateFailed');
  } finally {
    if (!disposed) pollTimer = setTimeout(poll, selectedTopic.value && ['QUEUED', 'RUNNING'].includes(selectedTopic.value.scanStatus) ? 5000 : 15000);
  }
};

watch(showEditor, visible => {
  if (!visible) {
    suggestionRequest.cancel();
    sourceRequest.cancel();
    suggesting.value = false;
    searchingSources.value = false;
  }
});
watch(sourceSearchTerm, () => {
  sourceRequest.cancel();
  searchingSources.value = false;
  sourceSearchResults.value = [];
  sourceTotalPages.value = 0;
});
watch(() => [...form.value.sourceIds], ids => {
  sourceSearchResults.value.filter(source => ids.includes(source.sourceId)).forEach(source => {
    if (!sourceOptions.value.some(option => option.sourceId === source.sourceId)) sourceOptions.value.push(source);
  });
});

watch(() => [route.query.topic, route.query.topicId, route.query.scope], () => {
  const nextScope = getQueryScope();
  const nextTopicId = getQueryTopicId();
  let scopeChanged = false;
  if (nextScope && nextScope !== scope.value) {
    scope.value = nextScope;
    topicFilter.value = '';
    topics.value = [];
    topicTotalPages.value = 0;
    scopeChanged = true;
    void loadTopics();
  }
  if (nextTopicId !== selectedTopicId.value || scopeChanged) {
    void selectTopic(nextTopicId, false);
  }
});

watch(() => [authStore.isAuthenticated, authStore.user?.userId], () => {
  authGeneration += 1;
  topicsRequest.cancel(); summariesRequest.cancel(); detailRequest.cancel(); pollRequest.cancel();
  showEditor.value = false;
  subscribing.value = false;
  scope.value = getQueryScope() ?? (authStore.isAuthenticated ? 'following' : 'public');
  topics.value = [];
  void selectTopic(selectedTopicId.value);
  void loadTopics();
});

const loadLimits = async () => {
  limitsError.value = '';
  await getTopicTrackLimits().then(value => {
    if (!disposed) { limits.value = value; limitsReady.value = true; }
  })
      .catch(() => { if (!disposed) limitsError.value = tr('topicTrackingPage.limitsFailed'); });
};

onMounted(async () => {
  void loadLimits();
  const initialTopicId = selectedTopicId.value;
  if (initialTopicId) {
    await Promise.all([loadTopics(), selectTopic(initialTopicId, false)]);
  } else {
    await Promise.all([loadTopics(), loadSummaries()]);
  }
  if (!disposed) pollTimer = setTimeout(poll, 5000);
});

onUnmounted(() => {
  disposed = true;
  authGeneration += 1;
  clearTimeout(pollTimer);
  resetArticles();
  [topicsRequest, summariesRequest, detailRequest, sourceRequest, suggestionRequest, pollRequest].forEach(request => request.cancel());
});
</script>