import { formatRelativeTime } from '@/utils/datetime';
import { md2html } from '@/utils/markdown';
import type { ArticleDto } from '@/api/articles';

export type { ArticleDto } from '@/api/articles';

export interface ArticleListItem {
    id: string;
    feedId?: string;
    title: string;
    summary: string;
    link?: string;
    thumbnail?: string;
    enclosure?: string;
    feedTitle: string;
    feedAvatar: string;
    publishedAt?: string;
    timeAgo: string;
    tags: string[];
    collected?: boolean;
}


export interface ArticleDetail extends ArticleListItem {
    liked: boolean;
    folderId: string | null;
    content: string;
    feedId?: string;
    feedAvatar?: string;
    mindMap?: string;
    requiresUpgrade: string;
}


export const normalizeArticle = (article: ArticleDto): ArticleListItem => {
    const feedTitle = article.feedTitle ?? '未知来源';
    const publishedAt = article.publishedAt;
    const tags = article.tags ?? [];
    return {
        id: String(article.id),
        feedId: article.feedId,
        title: article.title ?? '未命名文章',
        summary: article.summary ?? '暂无摘要。',
        link: article.link,
        thumbnail: article.thumbnail,
        enclosure: article.enclosure,
        feedTitle,
        feedAvatar: article.feedAvatar ?? '',
        publishedAt,
        timeAgo: formatRelativeTime(publishedAt ?? Date.now()),
        tags: Array.from(new Set(tags)).slice(0, 6),
        collected: article.collected ?? false
    };
};

export const normalizeArticleDetail = (article: ArticleDto): ArticleDetail => {
    const feedTitle = article.feedTitle ?? '未知来源';
    const publishedAt = article.publishedAt;
    const tags = article.tags ?? [];
    const rawContent = article.content ?? article.summary ?? '';

    return {
        ...article,
        id: String(article.id),
        title: article.title ?? '未命名文章',
        summary:article.summary? md2html(article.summary) : '',
        content: md2html(rawContent),
        link: article.link,
        thumbnail: article.thumbnail,
        enclosure: article.enclosure,
        enclosureType: article.enclosureType,
        feedId: article.feedId,
        feedTitle,
        author: article.author,
        publishedAt,
        timeAgo: formatRelativeTime(publishedAt ?? Date.now()),
        tags: Array.from(new Set(tags)).slice(0, 6),
        collected: article.collected ?? false,
        liked: article.liked ?? false,
        folderId: article.folderId ?? null,
    };
};
