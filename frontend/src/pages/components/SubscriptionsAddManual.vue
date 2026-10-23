<template>
  <div class="text-text">
    <p class="mb-4 text-xs leading-relaxed text-text-secondary sm:text-sm">
      {{ tr('subscriptionsAddManual.manualHint') }}
    </p>

    <div class="mb-5">
      <form @submit.prevent="handleAdd" class="relative flex items-center">
        <!-- RSS 图标 -->
        <div class="pointer-events-none absolute left-4 flex items-center text-text-secondary">
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="5" cy="19" r="1.5" fill="currentColor" stroke="none" />
            <path d="M 4 11 A 8 8 0 0 1 13 20" />
            <path d="M 4 4 A 15 15 0 0 1 20 20" />
          </svg>
        </div>

        <!-- MD3 药丸型搜索输入框 -->
        <input
            v-model.trim="newFeedUrl"
            type="search"
            :placeholder="tr('subscriptionsAddManual.urlPlaceholder')"
            class="w-full rounded-full border border-outline/25 bg-surface-container/40 py-3 pl-11 pr-24 text-xs text-text outline-none transition placeholder:text-text-secondary/60 focus:border-primary focus:bg-surface focus:ring-2 focus:ring-primary/20 sm:text-sm"
        />

        <!-- 固定宽度防抖添加按钮 -->
        <button
            type="submit"
            class="absolute right-1.5 top-1.5 bottom-1.5 inline-flex w-18 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-95 disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="isPending || !newFeedUrl">
          <span v-if="isPending" class="inline-flex items-center gap-1">
            <svg class="h-3 w-3 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M21 12a9 9 0 1 1-6.219-8.56" />
            </svg>
          </span>
          <span v-else>{{ tr('common.add') }}</span>
        </button>
      </form>
    </div>

    <!-- 错误反馈 -->
    <p v-if="error" class="rounded-2xl bg-red-500/10 px-4 py-3 text-xs text-red-600 dark:text-red-400 leading-relaxed">
      {{ error.message }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import { ref } from 'vue';
import { useSubscriptionMutation } from '@/queries/subscriptions';

defineOptions({ name: 'SubscriptionsAddManual' });

const emit = defineEmits<{ success: [] }>();
const { mutateAsync, isPending, error } = useSubscriptionMutation();
const newFeedUrl = ref('');

const handleAdd = async () => {
  if (!newFeedUrl.value || isPending.value) return;
  try {
    await mutateAsync({ feedUrl: newFeedUrl.value, subscribed: false });
    newFeedUrl.value = '';
    emit('success');
  } catch {
    // 错误状态已通过 mutation error 暴露
  }
};
</script>