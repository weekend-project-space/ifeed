import { get, post, type RequestOptions } from './client';
import type { PageResponse } from '@/types/api';

export interface ArticleDto {
  id: string;
  title: string;
  summary?: string;
  content?: string;
  link?: string;
  thumbnail?: string;
  enclosure?: string;
  enclosureType?: string;
  feedId?: string;
  feedTitle?: string;
  feedAvatar?: string;
  author?: string;
  publishedAt?: string;
  tags?: string[];
  collected?: boolean;
  liked?: boolean;
  folderId?: string | null;
}

export interface ArticleEnrichmentResult {
  summary: string;
  mindMap: string;
}

type RequestSignal = Pick<RequestOptions, 'signal'>;

export const getArticle = (articleId: string, options?: RequestSignal) =>
  get<ArticleDto>(`/api/articles/${encodeURIComponent(articleId)}`, options);

export const getArticleRecommendations = (
  articleId: string,
  topK: number,
  options?: RequestSignal,
) =>
  get<ArticleDto[]>(
    `/api/articles/${encodeURIComponent(articleId)}/recommendations`,
    { ...options, query: { topK } },
  );

export const enrichArticle = (articleId: string) =>
  post<ArticleEnrichmentResult>(
    `/api/articles/${encodeURIComponent(articleId)}/enrich`,
  );

export const recordArticleHistory = (articleId: string) =>
  post('/api/user/history', { articleId });

export interface ArticleListParams {
  page: number;
  size: number;
  sort?: string;
  feedId?: string | null;
  tags?: string | null;
  category?: string | null;
  signal?: AbortSignal;
}

export const listArticles = ({ signal, ...params }: ArticleListParams) =>
  get<PageResponse<ArticleDto>>('/api/articles', {
    signal,
    query: {
      ...params,
      feedId: params.feedId ?? undefined,
      tags: params.tags ?? undefined,
      category: params.category ?? undefined,
    },
  });

export interface SubscriptionInsights {
  categories: { category: string; count: number }[];
  hotTags: { tag: string; count: number }[];
}

export const getSubscriptionInsights = (top: number, options?: RequestSignal) =>
  get<SubscriptionInsights>('/api/articles/insights', {
    ...options,
    query: { top },
  });
