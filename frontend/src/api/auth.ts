import { get, post } from './client';

export interface AuthResponse {
  token: string;
  userId: string;
}

export interface UserProfile {
  userId: string;
  username: string;
  avatarUrl: string;
  currentPlan: string;
}

export interface Credentials {
  username: string;
  password: string;
}

export const login = (credentials: Credentials) =>
  post<AuthResponse>('/api/auth/login', credentials, { skipAuth: true });

export const register = (credentials: Credentials) =>
  post<AuthResponse>('/api/auth/register', credentials, { skipAuth: true });

export const getCurrentUser = () => get<UserProfile>('/api/user');

export const logout = () => post('/api/auth/logout');

export const getLinuxDoAuthUrl = () =>
  get<string>('/api/auth/linuxdo', { skipAuth: true });

export const loginWithLinuxDo = (code: string) =>
  get<AuthResponse>('/api/auth/linuxdo/callback', {
    query: { code },
    skipAuth: true,
  });
