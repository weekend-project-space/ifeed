<template>
  <div class="min-h-screen p-6">
    <div class="max-w-screen-md mx-auto">
      <!-- Header -->
      <div class="mb-8">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 rounded-lg bg-blue-600 flex items-center justify-center">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"/>
            </svg>
          </div>
          <div>
            <h1 class="text-2xl font-semibold text-gray-900 dark:text-white">
              User Plan Management
            </h1>
            <p class="text-sm text-gray-500 dark:text-gray-400">
              Manage user subscription plans
            </p>
          </div>
        </div>
      </div>

      <!-- Main Card -->
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div class="p-6">
          <!-- Username Input -->
          <div class="mb-6">
            <label for="username" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Username
            </label>
            <div class="relative">
              <input
                  type="text"
                  id="username"
                  v-model="username"
                  placeholder="Enter username"
                  class="w-full pl-10 pr-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-shadow"
              />
              <svg class="absolute left-3 top-3 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
              </svg>
            </div>
          </div>

          <!-- Plan Selection -->
          <div class="mb-6">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
              Select Plan
            </label>
            <div class="grid grid-cols-3 gap-3">
              <button
                  v-for="planOption in plans"
                  :key="planOption.value"
                  @click="selectedPlan = planOption.value"
                  type="button"
                  :class="[
                  'relative p-4 rounded-lg border-2 transition-all duration-200',
                  selectedPlan === planOption.value
                    ? planOption.selectedColor
                    : 'border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 hover:border-gray-300 dark:hover:border-gray-600'
                ]"
              >
                <div class="flex flex-col items-center text-center">
                  <div :class="[
                    'w-12 h-12 rounded-full flex items-center justify-center mb-2 transition-colors',
                    selectedPlan === planOption.value ? planOption.iconBg : 'bg-gray-200 dark:bg-gray-700'
                  ]">
                    <svg class="w-6 h-6" :class="selectedPlan === planOption.value ? 'text-white' : 'text-gray-500'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="planOption.icon"/>
                    </svg>
                  </div>
                  <div class="text-sm font-semibold text-gray-900 dark:text-white mb-1">
                    {{ planOption.label }}
                  </div>
                  <div class="text-xs text-gray-500 dark:text-gray-400">
                    {{ planOption.desc }}
                  </div>
                </div>
                <div v-if="selectedPlan === planOption.value" class="absolute -top-1 -right-1 w-6 h-6 bg-green-500 rounded-full flex items-center justify-center">
                  <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/>
                  </svg>
                </div>
              </button>
            </div>
          </div>

          <!-- Message -->
          <div
              v-if="message"
              :class="[
              'mb-6 px-4 py-3 rounded-lg flex items-center gap-3',
              message.type === 'success'
                ? 'bg-green-50 dark:bg-green-900/20 text-green-800 dark:text-green-200 border border-green-200 dark:border-green-800'
                : 'bg-red-50 dark:bg-red-900/20 text-red-800 dark:text-red-200 border border-red-200 dark:border-red-800'
            ]"
          >
            <svg v-if="message.type === 'success'" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <svg v-else class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <span class="text-sm font-medium">{{ message.text }}</span>
          </div>

          <!-- Actions -->
          <div class="flex gap-3">
            <button
                @click="updatePlan"
                :disabled="loading || !username.trim()"
                :class="[
                'flex-1 px-6 py-2.5 text-sm font-medium rounded-lg transition-all duration-200 flex items-center justify-center gap-2',
                loading || !username.trim()
                  ? 'bg-gray-300 dark:bg-gray-600 text-gray-500 dark:text-gray-400 cursor-not-allowed'
                  : 'bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white shadow-sm hover:shadow'
              ]"
            >
              <svg v-if="loading" class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
              </svg>
              <span>{{ loading ? 'Updating...' : 'Update Plan' }}</span>
            </button>
            <button
                @click="reset"
                type="button"
                class="px-6 py-2.5 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
            >
              Reset
            </button>
          </div>
        </div>

        <!-- Footer -->
        <div class="px-6 py-3 bg-gray-50 dark:bg-gray-900/50 border-t border-gray-200 dark:border-gray-700">
          <div class="flex items-center gap-2 text-xs text-gray-500 dark:text-gray-400">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
            </svg>
            <span>Admin privileges required for this action</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { post } from '@/api/client';

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
          desc: 'Basic features',
          icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
          selectedColor: 'border-gray-400 dark:border-gray-500 bg-gray-100 dark:bg-gray-800',
          iconBg: 'bg-gray-600'
        },
        {
          value: 'STANDARD',
          label: 'Standard',
          desc: 'Most popular',
          icon: 'M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z',
          selectedColor: 'border-blue-500 dark:border-blue-400 bg-blue-50 dark:bg-blue-900/20',
          iconBg: 'bg-blue-600'
        },
        {
          value: 'PRO',
          label: 'Pro',
          desc: 'Full access',
          icon: 'M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z',
          selectedColor: 'border-purple-500 dark:border-purple-400 bg-purple-50 dark:bg-purple-900/20',
          iconBg: 'bg-purple-600'
        }
      ]
    };
  },
  methods: {
    async updatePlan() {
      if (!this.username.trim()) {
        this.message = { type: 'error', text: 'Username is required' };
        return;
      }

      this.loading = true;
      this.message = null;

      try {
        await post('/api/plan', {
          username: this.username.trim(),
          plan: this.selectedPlan
        });

        const planLabel = this.plans.find(p => p.value === this.selectedPlan)?.label;
        this.message = {
          type: 'success',
          text: `Successfully updated ${this.username} to ${planLabel} plan`
        };
        this.username = '';
      } catch (error) {
        if (error.response) {
          if (error.response.status === 401) {
            this.message = { type: 'error', text: 'Unauthorized: Please log in' };
          } else if (error.response.status === 403) {
            this.message = { type: 'error', text: 'Forbidden: Admin access required' };
          } else {
            this.message = {
              type: 'error',
              text: error.response.data || 'Update failed'
            };
          }
        } else {
          this.message = { type: 'error', text: 'Network error occurred' };
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

<style scoped>
/* Custom styles for enhanced visuals */
</style>