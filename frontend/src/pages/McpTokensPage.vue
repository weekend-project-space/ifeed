<template>
  <main class="mx-auto w-full max-w-3xl px-4 py-8 sm:px-6 sm:py-10">
    <RouterLink to="/feed/you" class="mb-5 inline-flex items-center gap-2 text-sm text-text-secondary hover:text-primary">
      <span aria-hidden="true">←</span>{{ labels.back }}
    </RouterLink>

    <header class="mb-7">
      <h1 class="text-2xl font-bold tracking-tight text-text">{{ labels.title }}</h1>
      <p class="mt-2 text-sm leading-6 text-text-secondary">{{ labels.description }}</p>
    </header>

    <section class="mb-6 rounded-2xl border border-outline/15 bg-surface p-4 sm:p-5">
      <h2 class="text-base font-semibold text-text">{{ labels.createTitle }}</h2>
      <p class="mt-1 text-sm text-text-secondary">{{ labels.createDescription }}</p>
      <form class="mt-4 flex flex-col gap-3 sm:flex-row" @submit.prevent="createToken">
        <label class="sr-only" for="mcp-token-name">{{ labels.name }}</label>
        <input id="mcp-token-name" v-model="name" maxlength="64" required :placeholder="labels.namePlaceholder"
               class="min-w-0 flex-1 rounded-xl border border-outline/20 bg-surface px-3.5 py-2.5 text-sm text-text placeholder:text-text-secondary/60 focus:border-primary focus:ring-primary/20" />
        <button type="submit" :disabled="creating || !name.trim()"
                class="inline-flex min-h-10 items-center justify-center rounded-xl bg-primary px-5 text-sm font-semibold text-primary-foreground transition hover:brightness-95 disabled:cursor-not-allowed disabled:opacity-50">
          {{ creating ? labels.creating : labels.create }}
        </button>
      </form>
      <p v-if="error" role="alert" class="mt-3 text-sm text-red-600 dark:text-red-400">{{ error }}</p>
    </section>

    <section v-if="issuedToken" class="mb-6 rounded-2xl border border-primary/20 bg-primary/5 p-4 sm:p-5" aria-live="polite">
      <div class="flex items-start justify-between gap-3">
        <div>
          <h2 class="font-semibold text-text">{{ labels.tokenCreated }}</h2>
          <p class="mt-1 text-sm text-text-secondary">{{ labels.tokenWarning }}</p>
        </div>
        <button type="button" :aria-label="labels.dismiss" class="text-lg leading-none text-text-secondary hover:text-text" @click="issuedToken = ''">×</button>
      </div>
      <div class="mt-4 flex flex-col gap-2 sm:flex-row">
        <input readonly :value="issuedToken" :aria-label="labels.tokenValue"
               class="min-w-0 flex-1 rounded-xl border border-outline/15 bg-surface px-3 py-2.5 font-mono text-xs text-text" />
        <button type="button" class="min-h-10 rounded-xl bg-primary px-4 text-sm font-semibold text-primary-foreground hover:brightness-95" @click="copyToken">
          {{ copied ? labels.copied : labels.copy }}
        </button>
      </div>
    </section>

    <section class="mb-6 overflow-hidden rounded-2xl border border-outline/15 bg-surface">
      <div class="flex items-center justify-between gap-3 border-b border-outline/10 px-4 py-4 sm:px-5">
        <div>
          <h2 class="font-semibold text-text">{{ labels.listTitle }} <span class="font-normal text-text-secondary">({{ tokens.length }})</span></h2>
          <p class="mt-1 text-xs text-text-secondary">{{ labels.listDescription }}</p>
        </div>
        <button type="button" class="rounded-lg px-3 py-2 text-xs font-semibold text-primary hover:bg-primary/5 disabled:opacity-50" :disabled="loading" @click="loadTokens">
          {{ loading ? labels.loading : labels.refresh }}
        </button>
      </div>
      <div v-if="loading && !tokens.length" class="px-4 py-10 text-center text-sm text-text-secondary">{{ labels.loading }}</div>
      <div v-else-if="!tokens.length" class="px-4 py-10 text-center text-sm text-text-secondary">{{ labels.empty }}</div>
      <ul v-else class="divide-y divide-outline/10">
        <li v-for="token in tokens" :key="token.id" class="flex items-center gap-3 px-4 py-4 sm:px-5">
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-medium text-text">{{ token.name }}</p>
            <p class="mt-1 text-xs text-text-secondary">{{ labels.expires }} {{ formatDate(token.expiresAt) }}</p>
          </div>
          <button type="button" class="rounded-lg px-3 py-2 text-xs font-semibold text-red-600 hover:bg-red-500/10 dark:text-red-400"
                  :disabled="revoking === token.id" @click="revokeToken(token)">
            {{ revoking === token.id ? labels.revoking : labels.revoke }}
          </button>
        </li>
      </ul>
    </section>

    <section class="rounded-2xl border border-outline/15 bg-surface p-4 sm:p-5">
      <div class="flex items-center justify-between gap-3">
        <div>
          <h2 class="font-semibold text-text">{{ labels.usageTitle }}</h2>
          <p class="mt-1 text-sm text-text-secondary">{{ labels.usageDescription }}</p>
        </div>
        <button type="button" class="shrink-0 text-xs font-semibold text-primary hover:underline" @click="copyConfig">{{ configCopied ? labels.copied : labels.copyConfig }}</button>
      </div>
      <pre class="mt-4 overflow-x-auto rounded-xl bg-surface-container/50 p-3 text-xs leading-5 text-text"><code>{{ clientConfig }}</code></pre>
      <p class="mt-3 text-xs leading-5 text-text-secondary">{{ labels.tools }}</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { del, get, post } from '@/api/client';
import { useAuthStore } from '@/stores/auth';
import { useI18n } from 'vue-i18n';

interface McpTokenInfo { id: string; name: string; createdAt: string; expiresAt: string }
interface CreatedMcpToken extends McpTokenInfo { token: string }

const router = useRouter();
const auth = useAuthStore();
const { locale } = useI18n();
const name = ref('');
const tokens = ref<McpTokenInfo[]>([]);
const issuedToken = ref('');
const loading = ref(false);
const creating = ref(false);
const revoking = ref('');
const copied = ref(false);
const configCopied = ref(false);
const error = ref('');
const isEnglish = computed(() => locale.value.toLowerCase().startsWith('en'));
const endpoint = computed(() => `${window.location.origin}/api/mcp`);
const labels = computed(() => isEnglish.value ? {
  back: 'Back to account', title: 'MCP access', description: 'Connect an AI client to browse articles and manage your own saved articles and likes. Each credential can be revoked independently.',
  activeCount: 'Active credentials',
  createTitle: 'Create credential', createDescription: 'The full credential is shown only once. Give each client its own name so you can revoke it separately.',
  name: 'Credential name', namePlaceholder: 'e.g. Claude Desktop', create: 'Create credential', creating: 'Creating…',
  tokenCreated: 'Credential created', tokenWarning: 'Copy and save it now. For security, the full credential cannot be viewed again.',
  tokenValue: 'MCP access token', copy: 'Copy token', copied: 'Copied', configureHint: 'Configure your MCP client with this endpoint:', dismiss: 'Hide credential',
  listTitle: 'Your credentials', listDescription: 'Revoking a credential blocks it immediately.', refresh: 'Refresh', loading: 'Loading…', empty: 'No MCP credentials yet.', emptyHint: 'Create one above to connect your first client.',
  active: 'Active', expires: 'Expires', revoke: 'Revoke', revoking: 'Revoking…', confirmRevoke: 'Revoke this credential? Connected clients will stop working.',
  usageTitle: 'Connect an AI client', usageDescription: 'Add this server configuration to an MCP client that supports Streamable HTTP and custom Authorization headers.',
  copyConfig: 'Copy config', availableTools: 'Available tools', tools: 'Search and read articles, get personalized recommendations and trending topics, browse your subscriptions, saved articles, likes and folders. Clients can also save or unsave articles and like or unlike them for your account.', error: 'Could not complete the request.',
} : {
  back: '返回账户', title: 'MCP 访问', description: '将 AI 客户端连接到 iFeed，阅读文章并管理自己的收藏和喜欢。每个凭证都可以单独撤销。',
  activeCount: '有效凭证',
  createTitle: '创建访问凭证', createDescription: '完整凭证仅显示一次。建议为每个客户端单独命名，方便管理和撤销。',
  name: '凭证名称', namePlaceholder: '例如：Claude Desktop', create: '创建凭证', creating: '正在创建…',
  tokenCreated: '凭证已创建', tokenWarning: '请现在复制并妥善保存。出于安全考虑，之后无法再次查看完整凭证。',
  tokenValue: 'MCP 访问凭证', copy: '复制凭证', copied: '已复制', configureHint: '在 MCP 客户端中使用此服务地址：', dismiss: '隐藏凭证',
  listTitle: '你的凭证', listDescription: '撤销后凭证立即失效。', refresh: '刷新', loading: '加载中…', empty: '还没有 MCP 凭证。', emptyHint: '创建一个凭证，即可连接你的第一个 AI 客户端。',
  active: '有效', expires: '有效期至', revoke: '撤销', revoking: '正在撤销…', confirmRevoke: '确定撤销此凭证吗？对应客户端将无法继续连接。',
  usageTitle: '连接 AI 客户端', usageDescription: '将以下配置添加到支持 Streamable HTTP 和自定义 Authorization 请求头的 MCP 客户端。',
  copyConfig: '复制配置', availableTools: '可用工具', tools: '支持搜索和阅读文章、获取个性化推荐和热点、查看我的订阅、收藏、喜欢和收藏夹，也可为当前账户收藏、喜欢文章或取消这些操作。', error: '请求未能完成，请稍后重试。',
});
const clientConfig = computed(() => JSON.stringify({ mcpServers: { ifeed: { url: endpoint.value, headers: { Authorization: 'Bearer 粘贴创建时获得的凭证' } } } }, null, 2));

async function loadTokens() {
  loading.value = true;
  error.value = '';
  try { tokens.value = await get<McpTokenInfo[]>('/api/user/mcp-tokens'); }
  catch { error.value = labels.value.error; }
  finally { loading.value = false; }
}

async function createToken() {
  if (!name.value.trim() || creating.value) return;
  creating.value = true;
  error.value = '';
  try {
    const created = await post<CreatedMcpToken>('/api/user/mcp-tokens', { name: name.value.trim() });
    issuedToken.value = created.token;
    copied.value = false;
    name.value = '';
    await loadTokens();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } catch { error.value = labels.value.error; }
  finally { creating.value = false; }
}

async function copyToken() {
  try {
    await navigator.clipboard.writeText(issuedToken.value);
    copied.value = true;
    window.setTimeout(() => { copied.value = false; }, 1800);
  } catch { error.value = labels.value.error; }
}

async function copyConfig() {
  try {
    await navigator.clipboard.writeText(clientConfig.value);
    configCopied.value = true;
    window.setTimeout(() => { configCopied.value = false; }, 1800);
  } catch { error.value = labels.value.error; }
}

async function revokeToken(token: McpTokenInfo) {
  if (!window.confirm(labels.value.confirmRevoke)) return;
  revoking.value = token.id;
  error.value = '';
  try {
    await del(`/api/user/mcp-tokens/${encodeURIComponent(token.id)}`);
    tokens.value = tokens.value.filter(item => item.id !== token.id);
    if (issuedToken.value) issuedToken.value = '';
  } catch { error.value = labels.value.error; }
  finally { revoking.value = ''; }
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(isEnglish.value ? 'en' : 'zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

onMounted(async () => {
  if (!auth.isAuthenticated) {
    await router.replace({ name: 'auth', query: { redirect: '/settings/mcp' } });
    return;
  }
  await loadTokens();
});
</script>
