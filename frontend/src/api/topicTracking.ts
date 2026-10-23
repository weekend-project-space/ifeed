import { del, get, post, put } from './client';
import type { PageResponse } from '@/types/api';

export type TopicCategory = 'NEWS' | 'KNOWLEDGE' | 'COMMUNITY' | 'ANNOUNCEMENT';

export interface TopicSourceOption {
  sourceId: string;
  name: string;
  url: string;
}

export interface TopicTrackSuggestion {
  includeKeywords: string[];
  blockedKeywords: string[];
  sources: TopicSourceOption[];
  categories: TopicCategory[];
  relevancePrompt: string;
  existingTopic?: TopicTrack | null;
}

export interface TopicTrackConfig {
  name: string;
  keyword: string;
  includeKeywords: string[];
  blockedKeywords: string[];
  sourceIds: string[];
  categories: TopicCategory[];
  relevancePrompt: string;
  enabled: boolean;
}

export interface TopicTrack extends TopicTrackConfig {
  topicId: string;
  sources: TopicSourceOption[];
  lastRunAt?: string;
  createdAt: string;
  updatedAt: string;
  owner: boolean;
  subscribed: boolean;
  configVersion: number;
  scanStatus: 'IDLE' | 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'DISABLED';
  scanStartedAt?: string;
  scanFinishedAt?: string;
  scanError?: string;
}

export interface TopicTrackSummary {
  summaryId: string;
  topicId: string;
  topicName: string;
  keyword: string;
  summary: string;
  articleCount: number;
  titlesOnly: boolean;
  articles: Array<{
    articleId: string;
    title: string;
    url: string;
    feedName: string;
    feedIcon?: string;
    publishedAt: string;
  }>;
  createdAt: string;
}

export type TopicSummaryArticle = TopicTrackSummary['articles'][number];

export interface TopicCursorPage<T> {
  content: T[];
  nextCursor: string | null;
  hasNext: boolean;
}

const basePath = '/api/topic-tracks';

export const suggestTopicTrack = (keyword: string, signal?: AbortSignal) =>
  get<TopicTrackSuggestion>(`${basePath}/suggestions`, { signal, query: { keyword } });

export const getTopicTrack = (topicId: string, signal?: AbortSignal) =>
  get<TopicTrack>(`${basePath}/${encodeURIComponent(topicId)}`, { signal });

export const getTopicTrackLimits = () =>
  get<{ maxCategories: number; maxSources: number; maxTerms: number; maxTermLength: number }>(`${basePath}/limits`);

export const getTopicTrackStates = (topicIds: string[], signal?: AbortSignal) =>
  get<Array<Pick<TopicTrack, 'topicId' | 'configVersion' | 'enabled' | 'scanStatus' | 'scanStartedAt' | 'scanFinishedAt' | 'scanError'>>>(
    `${basePath}/status`, { signal, query: { topicIds: topicIds.join(',') } });

export const listTopicTracks = (page = 0, size = 50, signal?: AbortSignal) =>
  get<PageResponse<TopicTrack>>(`${basePath}/following`, { signal, query: { page, size, sort: 'createdAt,desc' } });

export const listPublicTopicTracks = (page = 0, size = 50, signal?: AbortSignal) =>
  get<PageResponse<TopicTrack>>(basePath, { signal, query: { page, size, sort: 'updatedAt,desc' } });

export const listTopicTrackSummaries = (topicId?: string, signal?: AbortSignal, following = false, cursor?: string) =>
  get<TopicCursorPage<TopicTrackSummary>>(`${basePath}/summaries`, {
    signal,
    query: { size: 20, topicId, following, cursor },
  });

export const listTopicSummaryArticles = (summaryId: string, cursor?: string, signal?: AbortSignal) =>
  get<TopicCursorPage<TopicSummaryArticle>>(`${basePath}/summaries/${encodeURIComponent(summaryId)}/articles`, {
    signal, query: { size: 30, cursor },
  });

export const createTopicTrack = (config: TopicTrackConfig) => post<TopicTrack>(basePath, config);

export const updateTopicTrack = (topicId: string, config: TopicTrackConfig) =>
  put<TopicTrack>(`${basePath}/${encodeURIComponent(topicId)}`, config);

export const subscribeTopicTrack = (topicId: string) =>
  post<TopicTrack>(`${basePath}/${encodeURIComponent(topicId)}/subscribe`);

export const unsubscribeTopicTrack = (topicId: string) =>
  del<TopicTrack>(`${basePath}/${encodeURIComponent(topicId)}/subscribe`);
