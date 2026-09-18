import {computed, ref} from 'vue';
import {defineStore} from 'pinia';
import {setAuthToken} from '../api/client';
import {
    getCurrentUser,
    getLinuxDoAuthUrl as fetchLinuxDoAuthUrl,
    login as loginRequest,
    loginWithLinuxDo,
    logout as logoutRequest,
    register as registerRequest,
    type AuthResponse,
    type Credentials,
    type UserProfile,
} from '@/api/auth';
import {queryClient} from '../queryClient';

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
        if (token.value !== newToken) {
            queryClient.clear();
        }
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
            const response = await loginRequest(credentials);
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
            const response = await registerRequest(credentials);
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
            const profile = await getCurrentUser();
            if (user.value && user.value.userId !== profile.userId) {
                queryClient.clear();
            }
            user.value = profile;
            initialized.value = true;
            return profile;
        } catch (err) {
            setToken(null);
            queryClient.clear();
            user.value = null;
            initialized.value = true;
            throw err;
        }
    };

    const logout = async () => {
        error.value = null;
        try {
            await logoutRequest();
        } catch {
        } finally {
            setToken(null);
            queryClient.clear();
            user.value = null;
        }
    };

    const getLinuxDoAuthUrl = async (): Promise<string> => {
        try {
            const authUrl = await fetchLinuxDoAuthUrl();
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
            const response = await loginWithLinuxDo(code);
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
