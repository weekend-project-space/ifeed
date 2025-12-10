import { PageResponse } from './api';

export interface Category {
    id: string;
    name: string;
    icon: string;
    feedCount: number;
    description: string;
}

export interface CategoriesResponse {
    categories: Category[];
}

export interface DiscoveryFeed {
    feedId: string;
    name: string;
    description: string;
    url: string;
    siteUrl: string;
    favicon: string;
    category: string;
    categoryName: string;
    subscriberCount: number;
    articleCount: number;
    lastUpdated: string;
    updateFrequency: string;
    subscribed: boolean;
    featured: boolean;
}
