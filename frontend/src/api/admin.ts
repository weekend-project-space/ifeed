import { post } from './client';

export type UserPlan = 'FREE' | 'STANDARD' | 'PRO';

export const updateUserPlan = (username: string, plan: UserPlan) =>
  post('/api/plan', { username, plan });
