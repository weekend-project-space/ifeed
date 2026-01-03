<template>
  <div class="">

    <!-- Main Content -->
    <div class="max-w-screen-lg mx-auto">
      <!-- User Profile Header -->
      <div class=" px-4 py-6">
        <div class="flex items-center space-x-4 mb-4">
          <img v-if="userInfo?.avatarUrl"
               :src="userInfo.avatarUrl"
               class="h-20 w-20 sm:h-24 sm:w-24 rounded-full"/>
          <div v-else class="flex h-20 w-20 sm:h-24 sm:w-24 items-center justify-center rounded-full bg-blue-600 dark:bg-blue-500 text-white text-2xl sm:text-3xl font-medium">
            {{ userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <div class="flex-1 min-w-0">
            <h1 class="text-xl sm:text-2xl font-medium text-gray-900 dark:text-white truncate">
              {{ userInfo?.username || '访客' }}
            </h1>
<!--            <p class="text-xs sm:text-sm text-gray-600 dark:text-gray-400 mt-0.5">-->
<!--              {{ userInfo?.email || '未登录' }}-->
<!--            </p>-->
            <div class="flex items-center mt-2 text-xs text-gray-600 dark:text-gray-400">
              <svg class="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
              </svg>
              <span>{{ userInfo?.currentPlan || 'Free' }} 会员</span>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex space-x-2">
          <router-link to="/feeds/channels"
                       class="flex-1 flex items-center justify-center px-4 py-2.5 rounded-full bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors">
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
            </svg>
            <span class="text-sm font-medium text-gray-900 dark:text-white">管理订阅</span>
          </router-link>
          <button @click="handleLogout"
                  class="flex-1 flex items-center justify-center px-4 py-2.5 rounded-full bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors">
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
            </svg>
            <span class="text-sm font-medium text-gray-900 dark:text-white">退出登录</span>
          </button>
        </div>
      </div>

      <!-- Divider -->
      <div class="h-2"></div>

      <!-- Section List -->
      <div class="">
        <!-- History Section -->
        <router-link to="/history" class="flex items-center px-4 py-3.5 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
          <svg class="w-6 h-6 text-gray-900 dark:text-white mr-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <span class="flex-1 text-sm font-normal text-gray-900 dark:text-white">历史记录</span>
          <svg class="w-5 h-5 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </router-link>

        <!-- Collections Section -->
        <router-link to="/collections" class="flex items-center px-4 py-3.5 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
          <svg class="w-6 h-6 text-gray-900 dark:text-white mr-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"/>
          </svg>
          <span class="flex-1 text-sm font-normal text-gray-900 dark:text-white">收藏夹</span>
          <svg class="w-5 h-5 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </router-link>

        <!-- Upgrade Section -->
        <button @click="showUpgradePlans = !showUpgradePlans" class="w-full flex items-center px-4 py-3.5 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
          <svg class="w-6 h-6 text-gray-900 dark:text-white mr-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"/>
          </svg>
          <span class="flex-1 text-left text-sm font-normal text-gray-900 dark:text-white">升级套餐</span>
          <svg
              class="w-5 h-5 text-gray-600 dark:text-gray-400 transition-transform duration-200"
              :class="showUpgradePlans ? 'rotate-180' : ''"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
          </svg>
        </button>
      </div>

      <!-- Divider -->
      <div class="h-2 "></div>

      <!-- Upgrade Plans Section (Collapsible) -->
      <transition name="expand">
        <div v-if="showUpgradePlans" class="space-y-6 sm:space-y-8 mb-6 sm:mb-8 mt-5">
          <!-- Section Header -->
          <div class="text-center">
            <h2 class="text-2xl sm:text-3xl font-medium text-[#030303] dark:text-white mb-2">选择你的套餐</h2>
            <p class="text-sm sm:text-base text-[#606060] dark:text-[#aaaaaa]">解锁全部功能，提升使用体验</p>
          </div>

          <!-- Pricing Cards -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 sm:gap-6">
            <!-- Free Plan -->
            <div class="bg-white dark:bg-[#212121] rounded-xl p-5 sm:p-6 border border-[#e5e5e5] dark:border-[#3f3f3f]">
              <div class="mb-4">
                <h3 class="text-lg sm:text-xl font-medium text-[#030303] dark:text-white mb-1">Free</h3>
                <p class="text-xs sm:text-sm text-[#606060] dark:text-[#aaaaaa]">体验基础功能</p>
              </div>

              <div class="mb-4">
                <div class="text-3xl sm:text-4xl font-medium text-[#030303] dark:text-white">免费</div>
              </div>

              <button class="w-full py-2.5 rounded-full text-sm font-medium bg-[#f2f2f2] dark:bg-[#3f3f3f] text-[#606060] dark:text-[#aaaaaa] cursor-not-allowed mb-5">
                当前方案
              </button>

              <div class="space-y-2">
                <div v-for="(feature, index) in freeFeatures" :key="index" class="flex items-start">
                  <svg class="w-4 h-4 text-[#065fd4] dark:text-[#3ea6ff] mr-2 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                  </svg>
                  <span class="text-xs sm:text-sm text-[#030303] dark:text-white">{{ feature }}</span>
                </div>
              </div>
            </div>

            <!-- Standard Plan -->
            <div
                @click="selectPlan('standard')"
                :class="[
                'bg-white dark:bg-[#212121] rounded-xl p-5 sm:p-6 cursor-pointer transition-all relative border-2',
                selectedPlan === 'standard'
                  ? 'border-[#065fd4] dark:border-[#3ea6ff]'
                  : 'border-[#e5e5e5] dark:border-[#3f3f3f] hover:border-[#c0c0c0] dark:hover:border-[#5f5f5f]'
              ]"
            >
              <div class="absolute -top-2.5 right-4">
                <span class="bg-[#065fd4] dark:bg-[#3ea6ff] text-white text-xs font-medium px-2.5 py-0.5 rounded-full">推荐</span>
              </div>

              <div class="mb-4">
                <h3 class="text-lg sm:text-xl font-medium text-[#030303] dark:text-white mb-1">Standard</h3>
                <p class="text-xs sm:text-sm text-[#606060] dark:text-[#aaaaaa]">适合个人使用</p>
              </div>

              <div class="mb-4">
                <div class="text-3xl sm:text-4xl font-medium text-[#030303] dark:text-white">
                  ¥36
                  <span class="text-base font-normal text-[#606060] dark:text-[#aaaaaa]">/年</span>
                </div>
              </div>

              <button
                  @click.stop="selectPlan('standard')"
                  :class="[
                  'w-full py-2.5 rounded-full text-sm font-medium transition-all mb-5',
                  selectedPlan === 'standard'
                    ? 'bg-[#065fd4] dark:bg-[#3ea6ff] text-white'
                    : 'bg-[#f2f2f2] dark:bg-[#3f3f3f] text-[#030303] dark:text-white hover:bg-[#e5e5e5] dark:hover:bg-[#4f4f4f]'
                ]"
              >
                {{ selectedPlan === 'standard' ? '已选择' : '选择 Standard' }}
              </button>

              <div class="space-y-2">
                <div v-for="(feature, index) in standardFeatures" :key="index" class="flex items-start">
                  <svg class="w-4 h-4 text-[#065fd4] dark:text-[#3ea6ff] mr-2 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                  </svg>
                  <span class="text-xs sm:text-sm text-[#030303] dark:text-white">{{ feature }}</span>
                </div>
              </div>
            </div>

            <!-- Pro Plan -->
            <div
                @click="selectPlan('pro')"
                :class="[
                'bg-white dark:bg-[#212121] rounded-xl p-5 sm:p-6 cursor-pointer transition-all border-2',
                selectedPlan === 'pro'
                  ? 'border-[#065fd4] dark:border-[#3ea6ff]'
                  : 'border-[#e5e5e5] dark:border-[#3f3f3f] hover:border-[#c0c0c0] dark:hover:border-[#5f5f5f]'
              ]"
            >
              <div class="mb-4">
                <h3 class="text-lg sm:text-xl font-medium text-[#030303] dark:text-white mb-1">Pro</h3>
                <p class="text-xs sm:text-sm text-[#606060] dark:text-[#aaaaaa]">适合专业用户</p>
              </div>

              <div class="mb-4">
                <div class="text-3xl sm:text-4xl font-medium text-[#030303] dark:text-white">
                  ¥68
                  <span class="text-base font-normal text-[#606060] dark:text-[#aaaaaa]">/年</span>
                </div>
              </div>

              <button
                  @click.stop="selectPlan('pro')"
                  :class="[
                  'w-full py-2.5 rounded-full text-sm font-medium transition-all mb-5',
                  selectedPlan === 'pro'
                    ? 'bg-[#065fd4] dark:bg-[#3ea6ff] text-white'
                    : 'bg-[#f2f2f2] dark:bg-[#3f3f3f] text-[#030303] dark:text-white hover:bg-[#e5e5e5] dark:hover:bg-[#4f4f4f]'
                ]"
              >
                {{ selectedPlan === 'pro' ? '已选择' : '选择 Pro' }}
              </button>

              <div class="space-y-2">
                <div v-for="(feature, index) in proFeatures" :key="index" class="flex items-start">
                  <svg class="w-4 h-4 text-[#065fd4] dark:text-[#3ea6ff] mr-2 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                  </svg>
                  <span class="text-xs sm:text-sm text-[#030303] dark:text-white">{{ feature }}</span>
                </div>
              </div>
            </div>
          </div>
          <!-- Payment Button -->
          <div v-if="selectedPlan" class="text-center pt-4">
            <button
                @click="handlePayment"
                class="w-full sm:w-auto px-12 sm:px-16 py-3 bg-[#065fd4] dark:bg-[#3ea6ff] text-white rounded-full font-medium text-sm hover:bg-[#0850b5] dark:hover:bg-[#65b8ff] transition-all">
              继续支付
            </button>
            <p class="mt-3 text-xs text-[#606060] dark:text-[#aaaaaa]">可随时取消订阅，无需承诺</p>
          </div>

          <!-- Feature Comparison -->
          <div class="bg-white dark:bg-[#212121] rounded-xl p-5 sm:p-6 border border-[#e5e5e5] dark:border-[#3f3f3f] mt-8">
            <h3 class="text-lg sm:text-xl font-medium text-[#030303] dark:text-white mb-4 text-center">功能对比</h3>
            <div class="overflow-x-auto -mx-5 sm:mx-0 px-5 sm:px-0">
              <table class="w-full min-w-[600px] sm:min-w-0">
                <thead>
                <tr class="border-b border-[#e5e5e5] dark:border-[#3f3f3f]">
                  <th class="text-left py-3 px-3 sm:px-4 text-xs text-[#606060] dark:text-[#aaaaaa] font-medium">功能</th>
                  <th class="text-center py-3 px-2 sm:px-4 text-xs text-[#030303] dark:text-white font-medium">Free</th>
                  <th class="text-center py-3 px-2 sm:px-4 text-xs text-[#030303] dark:text-white font-medium">Standard</th>
                  <th class="text-center py-3 px-2 sm:px-4 text-xs text-[#030303] dark:text-white font-medium">Pro</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="(row, index) in comparisonData" :key="index" class="border-b border-[#f2f2f2] dark:border-[#2f2f2f] last:border-b-0">
                  <td class="py-3 px-3 sm:px-4 text-xs text-[#030303] dark:text-white">{{ row.feature }}</td>
                  <td class="py-3 px-2 sm:px-4 text-center text-xs text-[#606060] dark:text-[#aaaaaa]">{{ row.free }}</td>
                  <td class="py-3 px-2 sm:px-4 text-center text-xs text-[#030303] dark:text-white">{{ row.standard }}</td>
                  <td class="py-3 px-2 sm:px-4 text-center text-xs text-[#030303] dark:text-white">{{ row.pro }}</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- Payment Modal -->
    <div v-if="showPaymentModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black bg-opacity-60" @click="closePaymentModal"></div>

      <div class="relative bg-white dark:bg-[#212121] rounded-2xl sm:rounded-3xl shadow-2xl max-w-md w-full p-6 sm:p-8 z-10 max-h-[90vh] overflow-y-auto">
        <button @click="closePaymentModal" class="absolute top-4 right-4 sm:top-6 sm:right-6 text-[#606060] hover:text-[#030303] dark:hover:text-white transition-colors">
          <svg class="w-5 h-5 sm:w-6 sm:h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>

        <div class="text-center mb-5 sm:mb-6">
          <h3 class="text-xl sm:text-2xl font-medium text-[#030303] dark:text-white mb-1 sm:mb-2">扫码支付</h3>
          <p class="text-sm sm:text-base text-[#606060] dark:text-[#aaaaaa]">{{ selectedPlanName }} - {{ selectedPlanPrice }}</p>
        </div>

        <div class="bg-[#f9f9f9] dark:bg-[#0f0f0f] rounded-xl sm:rounded-2xl p-6 sm:p-8 flex items-center justify-center mb-5 sm:mb-6">
          <div class="text-center">
            <div class="w-64 h-64 sm:w-80 sm:h-80 bg-white dark:bg-[#2f2f2f] rounded-xl sm:rounded-2xl flex items-center justify-center mb-3 sm:mb-4">
              <img
                  v-if="selectedPlan"
                  src="/weixin-pay-qrcode.png"
                  alt="支付二维码"
                  class="w-full h-full object-contain"
                  @error="handleImageError"
              />
              <span v-else class="text-[#606060] dark:text-[#aaaaaa] text-xs sm:text-sm">支付二维码</span>
            </div>
            <p class="text-xs sm:text-sm text-[#606060] dark:text-[#aaaaaa]">请使用微信扫描二维码完成支付</p>
          </div>
        </div>

        <div class="bg-[#e7f3ff] dark:bg-[#1a2634] rounded-xl sm:rounded-2xl p-3.5 sm:p-4">
          <div class="flex items-start space-x-2.5 sm:space-x-3">
            <svg class="w-4 h-4 sm:w-5 sm:h-5 text-[#065fd4] dark:text-[#3ea6ff] flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <div class="text-xs sm:text-sm text-[#030303] dark:text-white">
              <p class="font-medium mb-1.5 sm:mb-2">支付提示：</p>
              <ul class="space-y-1 sm:space-y-1.5 text-[#606060] dark:text-[#aaaaaa]">
                <li v-if="userInfo">• 付款留言处填写账号：<strong class="text-[#030303] dark:text-white">{{ userInfo.username }}</strong></li>
                <li>• 支付成功后，套餐将在 2 小时内自动激活</li>
                <li>• 记得使用优惠码 <strong class="text-[#030303] dark:text-white">AIR</strong> 享受 85 折优惠</li>
                <li>• 如有问题，<a class="text-[#065fd4] dark:text-[#3ea6ff] hover:underline" href="https://zhidayingxiao.cn/to/06g6yb">请联系客服</a></li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, computed} from 'vue';
import {storeToRefs} from "pinia";
import {useAuthStore} from "../stores/auth.ts";
import {useRouter} from "vue-router";

const router = useRouter();
const authStore = useAuthStore();

const selectedPlan = ref(null);
const showPaymentModal = ref(false);
const showUpgradePlans = ref(false);
const {user: userInfo} = storeToRefs(authStore);

const standardFeatures = [
  '订阅 300 个 RSS 源',
  '每天 AI 处理 30 篇文章',
  '创建 2 个订阅源',
  '2 个 Webhook 推送',
  '智能摘要和关键信息提取',
  '内容分类和标签管理'
];

const proFeatures = [
  '订阅 2000 个 RSS 源',
  '每天 AI 处理 100 篇文章',
  '创建 6 个订阅源',
  '6 个 Webhook 推送',
  'API 访问权限',
  '优先客户支持',
  '多语言翻译',
  '自定义 AI 提示词',
  '导出功能（PDF/Markdown）'
];

const freeFeatures = [
  '订阅 60 个 RSS 源',
  '每天 AI 处理 3 篇文章',
  '1 个 Webhook 推送',
  '基础摘要功能',
  '标准更新频率（每 2 小时）'
];

const comparisonData = [
  {feature: 'RSS 订阅源数量', free: '60 个', standard: '300 个', pro: '2000 个'},
  {feature: 'AI 处理文章数/天', free: '3 篇', standard: '30 篇', pro: '100 篇'},
  {feature: '创建订阅源', free: '—', standard: '2 个', pro: '6 个'},
  {feature: 'Webhook 推送', free: '1 个', standard: '2 个', pro: '6 个'},
  {feature: '智能摘要', free: '✓', standard: '✓', pro: '✓'},
  {feature: '关键信息提取', free: '—', standard: '✓', pro: '✓'},
  {feature: '多语言翻译', free: '—', standard: '—', pro: '✓'},
  {feature: 'API 访问', free: '—', standard: '—', pro: '✓'},
  {feature: '自定义 AI 提示词', free: '—', standard: '—', pro: '✓'}
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

const selectPlan = (planId) => {
  selectedPlan.value = planId;
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
  await router.replace({name: 'auth'});
};
</script>

<style scoped>
/* Smooth transitions */
* {
  transition-property: background-color, border-color, color, fill, stroke, opacity, box-shadow, transform;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 150ms;
}

/* Expand/collapse animation */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
  transform: translateY(-10px);
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 3000px;
  transform: translateY(0);
}
</style>
