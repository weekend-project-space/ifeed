<template>
  <div class="">

    <!-- Main Content -->
    <div class="max-w-screen-lg mx-auto">
      <!-- User Profile Header -->
      <div class=" px-4 py-6">
        <div class="flex items-center space-x-4 mb-4">
          <img v-if="userInfo?.avatarUrl"
               :src="userInfo.avatarUrl"
               class="h-20 w-20 sm:h-24 sm:w-24 rounded-full bg-primary"/>
          <div v-else class="flex h-20 w-20 sm:h-24 sm:w-24 items-center justify-center rounded-full bg-primary text-primary-foreground text-2xl sm:text-3xl font-medium">
            {{ userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <div class="flex-1 min-w-0">
            <h1 class="text-xl sm:text-2xl font-medium text-text truncate">
              {{ userInfo?.username || '访客' }}
            </h1>
            <div class="flex items-center mt-2 text-xs text-text-secondary">
              <svg class="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
              </svg>
              <span>{{ userInfo?.currentPlan || 'Free' }}</span>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex space-x-2">
          <router-link to="/feeds/channels"
                       class="flex-1 flex items-center justify-center px-4 py-2.5 rounded-full bg-surface-container hover:bg-surface-container/70 transition-colors">
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
            </svg>
            <span class="text-sm font-medium text-text">管理订阅</span>
          </router-link>
          <button @click="handleLogout"
                  class="flex-1 flex items-center justify-center px-4 py-2.5 rounded-full bg-surface-container hover:bg-surface-container/70 transition-colors">
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
            </svg>
            <span class="text-sm font-medium text-text">退出登录</span>
          </button>
        </div>
      </div>

      <!-- Divider -->
      <div class="h-2"></div>

      <!-- Section List -->
      <div class="">
        <!-- History Section -->
        <router-link to="/history" class="flex items-center px-4 py-3.5 hover:bg-surface-container transition-colors">
          <svg class="w-6 h-6 text-text mr-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <span class="flex-1 text-sm font-normal text-text">历史记录</span>
          <svg class="w-5 h-5 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </router-link>

        <!-- Collections Section -->
        <router-link to="/likes" class="flex items-center px-4 py-3.5 hover:bg-surface-container transition-colors">
          <svg class="w-6 h-6 text-text mr-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z" /></svg>
          <span class="flex-1 text-left text-sm text-text">我的喜欢</span>
        </router-link>
        <router-link to="/collections" class="flex items-center px-4 py-3.5 hover:bg-surface-container transition-colors">
          <svg class="w-6 h-6 text-text mr-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"/>
          </svg>
          <span class="flex-1 text-sm font-normal text-text">收藏夹</span>
          <svg class="w-5 h-5 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </router-link>

        <!-- Install App Section -->
        <button v-if="canInstall" @click="handleInstallPWA" class="w-full flex items-center px-4 py-3.5 hover:bg-surface-container transition-colors">
          <svg class="w-6 h-6 text-text mr-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
          </svg>
          <span class="flex-1 text-left text-sm font-normal text-text">安装应用</span>
          <svg class="w-5 h-5 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </button>

        <!-- Upgrade Section -->
        <button @click="showUpgradePlans = !showUpgradePlans" class="w-full flex items-center px-4 py-3.5 hover:bg-surface-container transition-colors">
          <svg class="w-6 h-6 text-text mr-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"/>
          </svg>
          <span class="flex-1 text-left text-sm font-normal text-text">升级套餐</span>
          <svg
              class="w-5 h-5 text-text-secondary transition-transform duration-200"
              :class="showUpgradePlans ? 'rotate-180' : ''"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
          </svg>
        </button>
      </div>

      <!-- Divider -->
      <div class="h-2"></div>

      <!-- Upgrade Plans Section (Collapsible) -->
      <transition name="expand">
        <div v-if="showUpgradePlans" class="space-y-6 sm:space-y-8 mb-6 sm:mb-8 mt-5">
          <!-- Section Header -->
          <div class="text-center">
            <h2 class="text-2xl sm:text-3xl font-bold text-text mb-2">选择你的套餐</h2>
            <p class="text-sm sm:text-base text-text-secondary">解锁全部功能，提升使用体验</p>
          </div>

          <!-- Pricing Cards -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 sm:gap-6">
            <!-- Free Plan -->
            <div class="bg-surface rounded-xl p-5 sm:p-6 border border-outline/20">
              <div class="mb-4">
                <h3 class="text-lg sm:text-xl font-medium text-text mb-1">Free</h3>
                <p class="text-xs sm:text-sm text-text-secondary">体验基础功能</p>
              </div>

              <div class="mb-4">
                <div class="text-3xl sm:text-4xl font-medium text-text">免费</div>
              </div>

              <button disabled class="w-full py-2.5 rounded-full text-sm font-medium bg-surface-container text-text-secondary cursor-not-allowed disabled:opacity-60 mb-5">
                {{ currentPlan === 'free' ? '当前方案' : '免费版本' }}
              </button>

              <div class="space-y-2">
                <div v-for="(feature, index) in freeFeatures" :key="index" class="flex items-start">
                  <svg class="w-4 h-4 text-primary mr-2 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                  </svg>
                  <span class="text-xs sm:text-sm text-text">{{ feature }}</span>
                </div>
              </div>
            </div>

            <!-- Standard Plan -->
            <div
                @click="currentPlan !== 'standard' && selectPlan('standard')"
                :class="[
                'bg-surface rounded-xl p-5 sm:p-6 cursor-pointer transition-all relative border-2',
                selectedPlan === 'standard'
                  ? 'border-primary'
                  : 'border-outline/20 hover:border-outline/60'
              ]"
            >
              <div class="absolute -top-2.5 right-4">
                <span class="bg-primary text-primary-foreground text-xs font-medium px-2.5 py-0.5 rounded-full">推荐</span>
              </div>

              <div class="mb-4">
                <h3 class="text-lg sm:text-xl font-medium text-text mb-1">Standard</h3>
                <p class="text-xs sm:text-sm text-text-secondary">适合个人使用</p>
              </div>

              <div class="mb-4">
                <div class="text-3xl sm:text-4xl font-medium text-text">
                  ¥36
                  <span class="text-base font-normal text-text-secondary">/年</span>
                </div>
              </div>

              <button
                  v-if="currentPlan === 'standard'"
                  disabled
                  class="w-full py-2.5 rounded-full text-sm font-medium bg-surface-container text-text-secondary cursor-not-allowed disabled:opacity-60 mb-5"
              >
                当前方案
              </button>
              <button
                  v-else
                  @click.stop="selectPlan('standard')"
                  :class="[
                  'w-full py-2.5 rounded-full text-sm font-medium transition-all mb-5',
                  selectedPlan === 'standard'
                    ? 'bg-primary text-primary-foreground'
                    : 'bg-surface-container text-text hover:bg-surface-container/70'
                ]"
              >
                {{ selectedPlan === 'standard' ? '已选择' : '选择 Standard' }}
              </button>

              <div class="space-y-2">
                <div v-for="(feature, index) in standardFeatures" :key="index" class="flex items-start">
                  <svg class="w-4 h-4 text-primary mr-2 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                  </svg>
                  <span class="text-xs sm:text-sm text-text">{{ feature }}</span>
                </div>
              </div>
            </div>

            <!-- Pro Plan -->
            <div
                @click="currentPlan !== 'pro' && selectPlan('pro')"
                :class="[
                'bg-surface rounded-xl p-5 sm:p-6 cursor-pointer transition-all border-2',
                selectedPlan === 'pro'
                  ? 'border-primary'
                  : 'border-outline/20 hover:border-outline/60'
              ]"
            >
              <div class="mb-4">
                <h3 class="text-lg sm:text-xl font-medium text-text mb-1">Pro</h3>
                <p class="text-xs sm:text-sm text-text-secondary">适合专业用户</p>
              </div>

              <div class="mb-4">
                <div class="text-3xl sm:text-4xl font-medium text-text">
                  ¥68
                  <span class="text-base font-normal text-text-secondary">/年</span>
                </div>
              </div>

              <button
                  v-if="currentPlan === 'pro'"
                  disabled
                  class="w-full py-2.5 rounded-full text-sm font-medium bg-surface-container text-text-secondary cursor-not-allowed disabled:opacity-60 mb-5"
              >
                当前方案
              </button>
              <button
                  v-else
                  @click.stop="selectPlan('pro')"
                  :class="[
                  'w-full py-2.5 rounded-full text-sm font-medium transition-all mb-5',
                  selectedPlan === 'pro'
                    ? 'bg-primary text-primary-foreground'
                    : 'bg-surface-container text-text hover:bg-surface-container/70'
                ]"
              >
                {{ selectedPlan === 'pro' ? '已选择' : '选择 Pro' }}
              </button>

              <div class="space-y-2">
                <div v-for="(feature, index) in proFeatures" :key="index" class="flex items-start">
                  <svg class="w-4 h-4 text-primary mr-2 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                  </svg>
                  <span class="text-xs sm:text-sm text-text">{{ feature }}</span>
                </div>
              </div>
            </div>
          </div>
          <!-- Payment Button -->
          <div v-if="selectedPlan" class="text-center pt-4">
            <button
                @click="handlePayment"
                class="w-full sm:w-auto px-12 sm:px-16 py-3 bg-primary text-primary-foreground rounded-full font-medium text-sm hover:bg-primary/90 transition-all">
              继续支付
            </button>
            <p class="mt-3 text-xs text-text-secondary">可随时取消订阅，无需承诺</p>
          </div>

          <!-- Feature Comparison -->
          <div class="bg-surface rounded-xl p-5 sm:p-6 border border-outline/20 mt-8">
            <h3 class="text-lg sm:text-xl font-medium text-text mb-4 text-center">功能对比</h3>
            <div class="overflow-x-auto -mx-5 sm:mx-0 px-5 sm:px-0">
              <table class="w-full min-w-[600px] sm:min-w-0">
                <thead>
                <tr class="border-b border-outline/20">
                  <th class="text-left py-3 px-3 sm:px-4 text-xs text-text-secondary font-medium">功能</th>
                  <th class="text-center py-3 px-2 sm:px-4 text-xs text-text font-medium">Free</th>
                  <th class="text-center py-3 px-2 sm:px-4 text-xs text-text font-medium">Standard</th>
                  <th class="text-center py-3 px-2 sm:px-4 text-xs text-text font-medium">Pro</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="(row, index) in comparisonData" :key="index" class="border-b border-outline/10 last:border-b-0">
                  <td class="py-3 px-3 sm:px-4 text-xs text-text">{{ row.feature }}</td>
                  <td class="py-3 px-2 sm:px-4 text-center text-xs text-text-secondary">{{ row.free }}</td>
                  <td class="py-3 px-2 sm:px-4 text-center text-xs text-text">{{ row.standard }}</td>
                  <td class="py-3 px-2 sm:px-4 text-center text-xs text-text">{{ row.pro }}</td>
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

<script setup>
import {ref, computed} from 'vue';
import {storeToRefs} from "pinia";
import {useAuthStore} from "../stores/auth.ts";
import {usePWAStore} from "../stores/pwa.ts";
import {useRouter} from "vue-router";

const router = useRouter();
const authStore = useAuthStore();
const pwaStore = usePWAStore();

const selectedPlan = ref(null);
const showPaymentModal = ref(false);
const showUpgradePlans = ref(true);
const {user: userInfo} = storeToRefs(authStore);
const {canInstall} = storeToRefs(pwaStore);

const standardFeatures = [
  '订阅 300 个 RSS 源',
  '每天 AI 处理 30 篇文章',
  '创建 2 个订阅源',
  '智能摘要和关键信息提取',
  '内容分类和标签管理',
  '文章思维导图',
  '语义搜索',
  // '2 个 Webhook 推送',       // TODO: Webhook 功能未实现
];

const proFeatures = [
  '订阅 2000 个 RSS 源',
  '每天 AI 处理 100 篇文章',
  '创建 6 个订阅源',
  '智能推荐（多路召回+重排）',
  '文章思维导图',
  '语义搜索',
  '优先客户支持',
  // '每天 AI 处理 100 篇文章', // TODO: 后端未实现每日配额限制
  // '6 个 Webhook 推送',        // TODO: Webhook 功能未实现
  // 'API 访问权限',             // TODO: 未实现
  // '多语言翻译',               // TODO: 未实现
  // '自定义 AI 提示词',         // TODO: 未实现
  // '导出功能（PDF/Markdown）',  // TODO: 未实现
];

const freeFeatures = [
  '订阅 60 个 RSS 源',
  '每天 AI 处理 3 篇文章',
  '基础摘要功能',
  '内容分类和标签',
  '关键词搜索',
  // '1 个 Webhook 推送',       // TODO: Webhook 功能未实现
  // '标准更新频率（每 2 小时）', // TODO: 更新频率未按套餐区分
];

const comparisonData = [
  {feature: 'RSS 订阅源数量', free: '60 个', standard: '300 个', pro: '2000 个'},
  {feature: 'AI 处理文章数/天', free: '3 篇', standard: '30 篇', pro: '100 篇'},
  {feature: '创建订阅源', free: '—', standard: '2 个', pro: '6 个'},
  {feature: '智能摘要', free: '基础', standard: '✓', pro: '✓'},
  {feature: '内容分类和标签', free: '✓', standard: '✓', pro: '✓'},
  {feature: '文章思维导图', free: '—', standard: '✓', pro: '✓'},
  {feature: '搜索', free: '关键词', standard: '关键词 + 语义', pro: '关键词 + 语义'},
  {feature: '智能推荐', free: '基础', standard: '基础', pro: '多路召回+重排'},
  // {feature: 'AI 处理文章数/天', free: '3 篇', standard: '30 篇', pro: '100 篇'},  // TODO: 未实现
  // {feature: 'Webhook 推送', free: '1 个', standard: '2 个', pro: '6 个'},          // TODO: 未实现
  // {feature: '多语言翻译', free: '—', standard: '—', pro: '✓'},                     // TODO: 未实现
  // {feature: 'API 访问', free: '—', standard: '—', pro: '✓'},                       // TODO: 未实现
  // {feature: '自定义 AI 提示词', free: '—', standard: '—', pro: '✓'},               // TODO: 未实现
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

const selectPlan = (planId) => {
  selectedPlan.value = planId;
  showPaymentModal.value=true;
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

// PWA 安装处理
const handleInstallPWA = async () => {
  await pwaStore.installPWA();
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
