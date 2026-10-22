<template>
  <div class="min-h-screen p-4 text-text sm:p-6">
    <div class="mx-auto max-w-screen-md">
      <!-- 头部：MD3 标题区 -->
      <div class="mb-8">
        <div class="flex items-center gap-3.5 mb-2">
          <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-primary text-white shadow-xs">
            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"/>
            </svg>
          </div>
          <div>
            <h1 class="text-xl font-bold tracking-tight text-text sm:text-2xl">
              用户订阅计划管理
            </h1>
            <p class="text-xs text-text-secondary sm:text-sm">
              管理用户的会员套餐与权限等级
            </p>
          </div>
        </div>
      </div>

      <!-- 主卡片：MD3 28px 动态表面大卡片 -->
      <div class="overflow-hidden rounded-[28px] bg-surface-container/40 p-6 shadow-xs sm:p-8">
        <!-- 用户名输入：MD3 Filled Text Field -->
        <div class="mb-6">
          <div class="rounded-t-2xl bg-surface px-4 pt-2.5 pb-2 border-b-2 border-outline/40 transition focus-within:border-primary focus-within:bg-surface/80">
            <label for="username" class="block text-[11px] font-medium text-text-secondary">用户名</label>
            <div class="relative mt-0.5 flex items-center">
              <svg class="absolute left-0 h-4 w-4 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
              </svg>
              <input
                  type="text"
                  id="username"
                  v-model="username"
                  placeholder="输入目标账号用户名"
                  class="block w-full border-0 bg-transparent pl-7 pr-0 text-sm text-text outline-none ring-0 placeholder:text-text-secondary/60"
              />
            </div>
          </div>
        </div>

        <!-- 计划选择器：MD3 柔和状态选择网格 -->
        <div class="mb-6">
          <label class="block text-xs font-semibold text-text-secondary mb-3">
            选择套餐
          </label>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <button
                v-for="planOption in plans"
                :key="planOption.value"
                @click="selectedPlan = planOption.value"
                type="button"
                :class="[
                  'relative rounded-2xl p-4 text-left transition-all duration-200 select-none active:scale-[0.98]',
                  selectedPlan === planOption.value
                    ? 'bg-primary/10 border-2 border-primary shadow-xs'
                    : 'bg-surface border-2 border-transparent hover:bg-surface-container/70'
                ]"
            >
              <div class="flex flex-col items-center text-center">
                <div :class="[
                  'mb-3 flex h-11 w-11 items-center justify-center rounded-2xl transition-colors',
                  selectedPlan === planOption.value ? 'bg-primary text-white shadow-xs' : 'bg-surface-container text-text-secondary'
                ]">
                  <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="planOption.icon"/>
                  </svg>
                </div>
                <div class="text-sm font-bold text-text mb-0.5">
                  {{ planOption.label }}
                </div>
                <div class="text-[11px] text-text-secondary">
                  {{ planOption.desc }}
                </div>
              </div>
              <div v-if="selectedPlan === planOption.value" class="absolute top-2.5 right-2.5 flex h-5 w-5 items-center justify-center rounded-full bg-primary text-white">
                <svg class="h-3 w-3 stroke-[2.5]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
                </svg>
              </div>
            </button>
          </div>
        </div>

        <!-- 消息提示条 -->
        <div
            v-if="message"
            :class="[
              'mb-6 flex items-center gap-3 rounded-2xl p-4 text-xs font-medium',
              message.type === 'success'
                ? 'bg-green-500/10 text-green-700 dark:text-green-300'
                : 'bg-red-500/10 text-red-600 dark:text-red-400'
            ]"
        >
          <svg v-if="message.type === 'success'" class="h-4 w-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <svg v-else class="h-4 w-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <span>{{ message.text }}</span>
        </div>

        <!-- 操作按钮：MD3 胶囊风格 -->
        <div class="flex items-center gap-3">
          <button
              @click="updatePlan"
              :disabled="loading || !username.trim()"
              class="inline-flex h-10 flex-1 items-center justify-center gap-2 rounded-full bg-primary px-6 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-40"
          >
            <svg v-if="loading" class="h-4 w-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
            </svg>
            <span>{{ loading ? '更新中...' : '确认更新套餐' }}</span>
          </button>
          <button
              @click="reset"
              type="button"
              class="inline-flex h-10 items-center justify-center rounded-full bg-surface px-5 text-xs font-semibold text-text-secondary transition hover:bg-surface-container active:scale-[0.98]"
          >
            重置
          </button>
        </div>

        <!-- 权限提示 -->
        <div class="mt-6 flex items-center gap-2 border-t border-outline/10 pt-4 text-[11px] text-text-secondary">
          <svg class="h-3.5 w-3.5 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
          </svg>
          <span>此操作需具备管理员特权 (Admin Privileges)</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { updateUserPlan } from '@/api/admin';

export default {
  name: 'UserPlanAdmin',
  data() {
    return {
      username: '',
      selectedPlan: 'FREE',
      loading: false,
      message: null,
      plans: [
        {
          value: 'FREE',
          label: 'Free',
          desc: '基础订阅功能',
          icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
        },
        {
          value: 'STANDARD',
          label: 'Standard',
          desc: '个人深度阅读',
          icon: 'M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z',
        },
        {
          value: 'PRO',
          label: 'Pro',
          desc: '专业与智能推荐',
          icon: 'M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z',
        }
      ]
    };
  },
  methods: {
    async updatePlan() {
      if (!this.username.trim()) {
        this.message = { type: 'error', text: '请输入用户名' };
        return;
      }

      this.loading = true;
      this.message = null;

      try {
        await updateUserPlan(this.username.trim(), this.selectedPlan);

        const planLabel = this.plans.find(p => p.value === this.selectedPlan)?.label;
        this.message = {
          type: 'success',
          text: `成功将 ${this.username} 更新为 ${planLabel} 方案`
        };
        this.username = '';
      } catch (error) {
        if (error.response) {
          if (error.response.status === 401) {
            this.message = { type: 'error', text: '未授权：请先登录' };
          } else if (error.response.status === 403) {
            this.message = { type: 'error', text: '拒绝访问：需要管理员权限' };
          } else {
            this.message = {
              type: 'error',
              text: error.response.data || '更新失败'
            };
          }
        } else {
          this.message = { type: 'error', text: '网络请求错误' };
        }
      } finally {
        this.loading = false;
      }
    },
    reset() {
      this.username = '';
      this.selectedPlan = 'FREE';
      this.message = null;
    }
  }
};
</script>