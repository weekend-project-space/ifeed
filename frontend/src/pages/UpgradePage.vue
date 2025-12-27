<template>
  <div class="min-h-screen  transition-colors duration-200">
    <!-- Header -->
    <header class="border-b border-gray-200 dark:border-gray-700">
      <div class="max-w-screen-xl  mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div class="flex items-center justify-between">
          <div  v-if="userInfo" class="flex items-center space-x-3">
            <div class="flex items-center space-x-2">
              <img v-if="userInfo.avatarUrl" :src="userInfo.avatarUrl" class="h-8 w-8 rounded-full bg-primary"/>
              <span class="text-sm text-gray-700 dark:text-gray-300">{{ userInfo.username }}</span>
            </div>
            <span class="bg-green-500 dark:bg-green-600 text-white text-xs px-3 py-1 rounded-full font-medium">
              {{ userInfo.currentPlan }} 套餐
            </span>
          </div>
          <div v-else  class="flex items-center space-x-3" >
            <div class="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-semibold cursor-pointer group-hover:ring-2 group-hover:ring-primary/30 transition-all">U</div>
            <span>访客</span>
          </div>
          <div class="flex items-center space-x-4">
            <router-link to="/feeds/channels"
                         class="text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white flex items-center space-x-1">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
              </svg>
              <span class="text-sm">管理订阅</span>
            </router-link>
            <button @click="handleLogout"
                    class="text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white flex items-center space-x-1">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
              </svg>
              <span class="text-sm">登出</span>
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- Promotion Banner -->
    <div class="bg-gradient-to-r from-purple-600 to-primary dark:from-purple-700 dark:to-primary">
      <div class="max-w-screen-xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div class="flex items-center justify-center space-x-2 text-white text-sm">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 8v13m0-13V6a2 2 0 112 2h-2zm0 0V5.5A2.5 2.5 0 109.5 8H12zm-7 4h14M5 12a2 2 0 110-4h14a2 2 0 110 4M5 12v7a2 2 0 002 2h10a2 2 0 002-2v-7"></path>
          </svg>

          <span class="font-medium">特别优惠！</span>
          <span>我们为中文用户提供特别优惠！请在订阅时使用优惠码</span>

          <span class="bg-white text-primary px-2 py-1 rounded font-bold">
        AIR
      </span>

          <span>，即可额外享受 <strong>85折</strong> 优惠！</span>
        </div>
      </div>
    </div>


    <!-- Main Content -->
    <div class="max-w-screen-xl  mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 class="text-3xl font-bold text-gray-900 dark:text-white mb-2">订阅方案</h1>
      <div class="flex items-center space-x-2 mb-8">
        <span class="text-gray-600 dark:text-gray-400">使用 💰支付宝或 💳微信支付？</span>
        <button  v-if="selectedPlan" class="text-blue-600 dark:text-blue-400 hover:underline" @click="handlePayment">点此付款</button>
      </div>

      <!-- Pricing Cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <!-- Standard Plan -->
        <div
            @click="selectPlan('standard')"
            :class="[
            'bg-white dark:bg-gray-800 rounded-xl border-2 transition-all cursor-pointer',
            selectedPlan === 'standard'
              ? 'border-gray-900 dark:border-gray-300 shadow-xl'
              : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
          ]"
        >
          <div class="p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-2xl font-bold text-gray-900 dark:text-white">Standard</h3>
              <div class="flex items-center space-x-1 text-sm text-gray-600 dark:text-gray-400">
                <svg class="w-4 h-4 fill-current" viewBox="0 0 24 24">
                  <path
                      d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"></path>
                </svg>
                <span>Most popular</span>
              </div>
            </div>

            <p class="text-gray-600 dark:text-gray-400 text-sm mb-6">定期追踪多个RSS源，获取AI智能摘要</p>

            <button
                @click.stop="selectPlan('standard')"
                :class="[
                'w-full py-3 rounded-lg font-medium transition-colors mb-6',
                selectedPlan === 'standard'
                  ? 'bg-gray-900 dark:bg-gray-200 text-white dark:text-gray-900'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-gray-100 hover:bg-gray-200 dark:hover:bg-gray-600'
              ]"
            >
              {{ selectedPlan === 'standard' ? '已选择' : 'Continue with Standard' }}
            </button>

            <div class="mb-6">
              <div class="text-4xl font-bold text-gray-900 dark:text-white">
                ¥36
                <span class="text-base font-normal text-gray-600 dark:text-gray-400">/年</span>
              </div>
            </div>

            <div class="space-y-3">
              <div v-for="(feature, index) in standardFeatures" :key="index" class="flex items-start">
                <svg class="w-5 h-5 text-gray-900 dark:text-gray-300 mr-3 flex-shrink-0 mt-0.5" fill="none"
                     stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                </svg>
                <span class="text-sm text-gray-700 dark:text-gray-300">{{ feature }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Pro Plan -->
        <div
            @click="selectPlan('pro')"
            :class="[
            'bg-white dark:bg-gray-800 rounded-xl border-2 transition-all cursor-pointer',
            selectedPlan === 'pro'
              ? 'border-gray-900 dark:border-gray-300 shadow-xl'
              : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
          ]"
        >
          <div class="p-6">
            <h3 class="text-2xl font-bold text-gray-900 dark:text-white mb-4">Pro</h3>

            <p class="text-gray-600 dark:text-gray-400 text-sm mb-6">重度信息获取者，需要追踪大量RSS源和深度分析</p>

            <button
                @click.stop="selectPlan('pro')"
                :class="[
                'w-full py-3 rounded-lg font-medium transition-colors mb-6',
                selectedPlan === 'pro'
                  ? 'bg-gray-900 dark:bg-gray-200 text-white dark:text-gray-900'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-gray-100 hover:bg-gray-200 dark:hover:bg-gray-600'
              ]"
            >
              {{ selectedPlan === 'pro' ? '已选择' : 'Continue with Pro' }}
            </button>

            <div class="mb-6">
              <div class="text-4xl font-bold text-gray-900 dark:text-white">
                ¥68
                <span class="text-base font-normal text-gray-600 dark:text-gray-400">/年</span>
              </div>
            </div>

            <div class="space-y-3">
              <div v-for="(feature, index) in proFeatures" :key="index" class="flex items-start">
                <svg class="w-5 h-5 text-gray-900 dark:text-gray-300 mr-3 flex-shrink-0 mt-0.5" fill="none"
                     stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                </svg>
                <span class="text-sm text-gray-700 dark:text-gray-300">{{ feature }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Free Plan -->
        <div class="bg-white dark:bg-gray-800 rounded-xl border-2 border-gray-200 dark:border-gray-700">
          <div class="p-6">
            <h3 class="text-2xl font-bold text-gray-900 dark:text-white mb-4">Free</h3>

            <p class="text-gray-600 dark:text-gray-400 text-sm mb-6">试用版，体验RSS订阅和AI摘要功能</p>

            <button
                class="w-full py-3 rounded-lg font-medium bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 cursor-not-allowed mb-6">
              Current Plan
            </button>

            <div class="text-4xl font-bold text-gray-900 dark:text-white mb-6">Free</div>

            <div class="space-y-3">
              <div v-for="(feature, index) in freeFeatures" :key="index" class="flex items-start">
                <svg class="w-5 h-5 text-gray-900 dark:text-gray-300 mr-3 flex-shrink-0 mt-0.5" fill="none"
                     stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                </svg>
                <span class="text-sm text-gray-700 dark:text-gray-300">{{ feature }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Payment Button -->
      <div v-if="selectedPlan" class="mt-8 text-center">
        <button
            @click="handlePayment"
            class="px-12 py-4 bg-gray-900 dark:bg-gray-200 text-white dark:text-gray-900 rounded-lg font-medium text-lg hover:bg-gray-800 dark:hover:bg-gray-300 shadow-lg hover:shadow-xl transition-all"
        >
          继续支付
        </button>
        <p class="mt-4 text-sm text-gray-600 dark:text-gray-400">可随时取消订阅，无需承诺</p>
      </div>

      <!-- Feature Comparison Table -->
      <div class="mt-12 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-8">
        <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-6 text-center">功能对比</h2>
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
            <tr class="border-b border-gray-200 dark:border-gray-700">
              <th class="text-left py-3 px-4 text-gray-600 dark:text-gray-400 font-medium">功能</th>
              <th class="text-center py-3 px-4 text-gray-900 dark:text-white font-semibold">Free</th>
              <th class="text-center py-3 px-4 text-gray-900 dark:text-white font-semibold">Standard</th>
              <th class="text-center py-3 px-4 text-gray-900 dark:text-white font-semibold">Pro</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="(row, index) in comparisonData" :key="index"
                class="border-b border-gray-100 dark:border-gray-700">
              <td class="py-4 px-4 text-gray-700 dark:text-gray-300">{{ row.feature }}</td>
              <td class="py-4 px-4 text-center text-gray-600 dark:text-gray-400">{{ row.free }}</td>
              <td class="py-4 px-4 text-center text-gray-900 dark:text-white font-medium">{{ row.standard }}</td>
              <td class="py-4 px-4 text-center text-gray-900 dark:text-white font-medium">{{ row.pro }}</td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Payment Modal -->
    <div v-if="showPaymentModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <!-- Backdrop -->
      <div
          class="absolute inset-0 bg-black bg-opacity-50 transition-opacity"
          @click="closePaymentModal"
      ></div>

      <!-- Modal Content -->
      <div class="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-md w-full p-6 z-10">
        <!-- Close Button -->
        <button
            @click="closePaymentModal"
            class="absolute top-4 right-4 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>

        <!-- Modal Header -->
        <div class="text-center mb-6">
          <h3 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">
            扫码支付
          </h3>
          <p class="text-gray-600 dark:text-gray-400">
            {{ selectedPlanName }} - {{ selectedPlanPrice }}
          </p>
        </div>

        <!-- QR Code Image Placeholder -->
        <div class="bg-gray-100 dark:bg-gray-700 rounded-lg p-8 flex items-center justify-center mb-6">
          <div class="text-center">
            <div class="w-80 h-80 bg-white dark:bg-gray-600 rounded-lg flex items-center justify-center mb-4">
              <!-- 这里放置实际的二维码图片 -->
              <img
                  src="/weixin-pay-qrcode.png"
                  alt="支付二维码"
                  class="w-full h-full object-contain"
              />
              <!-- 如果没有图片，显示占位文字 -->
              <span class="text-gray-400 dark:text-gray-500 text-sm">
                支付二维码
              </span>
            </div>
            <p class="text-sm text-gray-600 dark:text-gray-400">
              请使用微信扫描二维码完成支付
            </p>
          </div>
        </div>

        <!-- Payment Tips -->
        <div class="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-4">
          <div class="flex items-start space-x-3">
            <svg class="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor"
                 viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <div class="text-sm  ">
              <p class="font-medium mb-1">支付提示：</p>
              <ul class="list-disc list-inside space-y-1 ">
                <li>付款留言处填写你在本站的账号：<strong>{{ userInfo.username }}</strong> <br><small> （受字数限制没填写完整也可）</small></li>
                <li>支付成功后，套餐将2小时内自动激活, </li>
                <li>记得使用优惠码 <strong>AIR</strong> 享受85折优惠</li>
                <small> 如有问题，<a class="text-blue-900" href="https://zhidayingxiao.cn/to/06g6yb">请联系客服</a></small>
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
const {user: userInfo} = storeToRefs(authStore);

const standardFeatures = [
  '订阅 300 个RSS源',
  '每天 AI 处理 30 篇文章',
  '创建 2 个订阅源',
  '2 个 Webhook 推送',
  '智能摘要和关键信息提取',
  '内容分类和标签管理'
];

const proFeatures = [
  '订阅 2000 个RSS源',
  '每天 AI 处理 100 篇文章',
  '创建 6 个订阅源',
  '6 个 Webhook 推送',
  'API访问权限',
  '优先客户支持',
  '多语言翻译',
  '自定义AI提示词',
  '导出功能（PDF/Markdown）'
];

const freeFeatures = [
  '订阅 30 个RSS源',
  '每天 AI 处理 3 篇文章',
  '1 个 Webhook 推送',
  '基础摘要功能',
  '标准更新频率（每2小时）'
];

const comparisonData = [
  {feature: 'RSS订阅源数量', free: '30个', standard: '300个', pro: '2000个'},
  {feature: 'AI处理文章数/天', free: '3篇', standard: '30篇', pro: '100篇'},
  {feature: '创建订阅源', free: '—', standard: '2个', pro: '6个'},
  {feature: 'Webhook推送', free: '1个', standard: '2个', pro: '6个'},
  {feature: '智能摘要', free: '✓', standard: '✓', pro: '✓'},
  {feature: '关键信息提取', free: '—', standard: '✓', pro: '✓'},
  {feature: '多语言翻译', free: '—', standard: '—', pro: '✓'},
  {feature: 'API访问', free: '—', standard: '—', pro: '✓'},
  {feature: '自定义AI提示词', free: '—', standard: '—', pro: '✓'}
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
  handlePayment()
};

const handlePayment = () => {
  if (selectedPlan.value) {
    showPaymentModal.value = true;
  }
};

const closePaymentModal = () => {
  showPaymentModal.value = false;
};

const handleLogout = async () => {
  await authStore.logout();
  await router.replace({name: 'auth'});
};
</script>

<style scoped>
/* Tailwind CSS dark mode classes are used inline */
</style>