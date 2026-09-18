import { get } from './client';
import type { SlimPageResponse } from '@/types/api';

export interface RadarTopicDto {
  topicId: string;
  title?: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
  articleCount: number;
  topKeywords?: string[];
}

export interface RadarItemDto {
  articleId: string;
  title?: string;
  summary?: string;
  thumbnail?: string;
  enclosure?: string;
  feedTitle?: string;
  publishedAt?: string;
  tags?: string[];
  score?: number;
  relativeTime?: string;
}

export type PageResponseWithMeta<T> = SlimPageResponse<T> & {
  meta?: Record<string, unknown>;
};

export interface RadarDigestParams {
  page: number;
  size: number;
  snapshotId?: string | null;
  windowHours?: number | null;
}

export const getRadarDigest = (params: RadarDigestParams) =>
  get<PageResponseWithMeta<RadarTopicDto>>('/api/user/radar/digest', {
    query: {
      ...params,
      snapshotId: params.snapshotId ?? undefined,
      windowHours: params.windowHours ?? undefined,
    },
  });

export const getRadarTopicDetail = (args: {
  topicId: string;
  snapshotId: string;
  page: number;
  size: number;
}) =>
  get<PageResponseWithMeta<RadarItemDto>>(
    `/api/user/radar/topics/${encodeURIComponent(args.topicId)}`,
    {
      query: {
        snapshotId: args.snapshotId,
        page: args.page,
        size: args.size,
      },
    },
  );
