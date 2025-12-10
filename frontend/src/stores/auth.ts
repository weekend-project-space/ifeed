import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { request, setAuthToken } from '../api/client';

interface AuthResponse {
  token: string;
  userId: string;
}

interface UserProfile {
  userId: string;
  username: string;
}

interface Credentials {
  username: string;
  password: string;
}

const TOKEN_STORAGE_KEY = 'ifeed_token';

const extractMessage = (err: unknown, fallback: string) => {
  if (err && typeof err === 'object') {
    const maybeError = err as { status?: number; payload?: unknown; message?: string };
    if (maybeError.payload && typeof maybeError.payload === 'object') {
      const payload = maybeError.payload as Record<string, unknown>;
      const message = payload.message;
      if (typeof message === 'string' && message.trim()) {
        return message;
      }
    }
    if (typeof maybeError.message === 'string' && maybeError.message.trim()) {
      return maybeError.message;
    }
  }
  return fallback;
};

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_STORAGE_KEY));
  const user = ref<UserProfile | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const initialized = ref(false);

  if (token.value) {
    setAuthToken(token.value);
  }

  const isAuthenticated = computed(() => Boolean(token.value));

  const setToken = (newToken: string | null) => {
    token.value = newToken;
    if (newToken) {
      localStorage.setItem(TOKEN_STORAGE_KEY, newToken);
    } else {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
    }
    setAuthToken(newToken);
  };

  const login = async (credentials: Credentials) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await request<AuthResponse>('/api/auth/login', {
        method: 'POST',
        json: credentials,
        skipAuth: true
      });
      setToken(response.token);
      await fetchUser();
      return response;
    } catch (err) {
      const message = extractMessage(err, '登录失败');
      error.value = message;
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const register = async (credentials: Credentials) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await request<AuthResponse>('/api/auth/register', {
        method: 'POST',
        json: credentials,
        skipAuth: true
      });
      setToken(response.token);
      await fetchUser();
      return response;
    } catch (err) {
      const message = extractMessage(err, '注册失败');
      error.value = message;
      console.log(err)
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const fetchUser = async () => {
    if (!token.value) {
      user.value = null;
      return null;
    }

    try {
      const profile = await request<UserProfile>('/api/user');
      user.value = profile;
      initialized.value = true;
      return profile;
    } catch (err) {
      setToken(null);
      user.value = null;
      initialized.value = true;
      throw err;
    }
  };

  const logout = async () => {
    error.value = null;
    try {
      await request('/api/auth/logout', {
        method: 'POST'
      });
    } catch {
    } finally {
      setToken(null);
      user.value = null;
    }
  };

  const getLinuxDoAuthUrl = async (): Promise<string> => {
    try {
      const authUrl = await request<string>('/api/auth/linuxdo', {
        skipAuth: true
      });
      return authUrl;
    } catch (err) {
      const message = extractMessage(err, '获取授权地址失败');
      error.value = message;
      throw err;
    }
  };

  const linuxDoLogin = async (code: string) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await request<AuthResponse>('/api/auth/linuxdo/callback', {
        method: 'GET',
        query: { code },
        skipAuth: true
      });
      setToken(response.token);
      await fetchUser();
      return response;
    } catch (err) {
      const message = extractMessage(err, 'Linux.do 登录失败');
      error.value = message;
      throw err;
    } finally {
      loading.value = false;
    }
  };

  return {
    token,
    user,
    loading,
    error,
    initialized,
    isAuthenticated,
    login,
    register,
    fetchUser,
    logout,
    setToken,
    getLinuxDoAuthUrl,
    linuxDoLogin
  };
});
