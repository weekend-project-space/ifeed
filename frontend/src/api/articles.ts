import { get } from './client';

export type RecommendationSource = 'owner' | 'global';

export interface ArticleRecommendation {
  id: string;
  title: string;
  link: string;
  summary: string;
  thumbnail?: string;
  enclosure?: string;
  feedTitle: string;
  publishedAt?: string;
  tags: string[];
  timeAgo: string;
  reason: 'collaborative' | 'content' | 'popular';
}

export interface RecommendationQuery {
  size?: number;
  source?: RecommendationSource;
}

export const fetchRecommendations = (query: RecommendationQuery = {}) =>
  get<ArticleRecommendation[]>('/api/articles/recommendations', { query });
