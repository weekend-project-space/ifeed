import { createRouter, createWebHistory } from 'vue-router';
import { onUnauthorized } from '@/api/auth-events';
import { setAuthToken } from '@/api/client'
import AuthPage from '../pages/AuthPage.vue';
// import HomePage from '../pages/HomePage.vue';
import SearchPage from '../pages/SearchPage.vue';
import MainLayout from '../layouts/MainLayout.vue';
import SubscriptionDiscoveryPage from '../pages/SubscriptionDiscoveryPage.vue';
import CollectionsPage from '../pages/CollectionsPage.vue';
import HistoryPage from '../pages/HistoryPage.vue';
import LikesPage from '../pages/LikesPage.vue';
import ArticleDetailPage from '../pages/ArticleDetailPage.vue';
import FeedDetailPage from '../pages/FeedDetailPage.vue';
import HomePage from '../pages/HomePage.vue';
import FeedSubscriptionsPage from '../pages/FeedSubscriptionsPage.vue';
import FeedChannelsPage from '../pages/SubscriptionsListPage.vue';
import AdminPage from "../pages/AdminPage.vue";
import UpgradePage from "../pages/UpgradePage.vue";
import RadarPage from '../pages/RadarPage.vue';
import RadarTopicPage from '../pages/RadarTopicPage.vue';
import { useAuthStore } from '../stores/auth';
// router/index.ts

import { trackPageView } from '@/utils/analytics'

// Route values are decoded once here; pages consume reactive, typed props.
const routeText = (value: unknown): string => {
    const first = Array.isArray(value) ? value[0] : value;
    return typeof first === 'string' ? first.trim() : '';
};

const routePage = (value: unknown): number => {
    const parsed = Number(routeText(value));
    return Number.isFinite(parsed) && parsed > 0 ? Math.max(1, Math.floor(parsed)) : 1;
};

const router = createRouter({
    history: createWebHistory(),
    scrollBehavior(to, from, savedPosition) {
        // Browser back/forward should restore the exact list scroll position.
        if (savedPosition) {
            return savedPosition;
        }
        // New navigations start at the top of the target page.
        return { top: 0, left: 0, behavior: 'auto' };
    },
    routes: [
        {
            path: '/auth',
            name: 'auth',
            component: AuthPage
        },
        {
            path: '/',
            component: MainLayout,
            children: [
                {
                    path: '',
                    name: 'home',
                    component: HomePage,
                },
                {
                    path: 'search',
                    name: 'search',
                    component: SearchPage,
                    props: route => ({
                        query: routeText(route.query.q),
                        type: routeText(route.query.type) === 'keyword' ? 'keyword' : 'semantic',
                        source: ['owner', 'global'].includes(routeText(route.query.source))
                            ? routeText(route.query.source) : undefined,
                        page: routePage(route.query.page),
                        feedId: routeText(route.query.feedId) || null,
                        tags: routeText(route.query.tags) || null,
                        category: routeText(route.query.category) || null,
                    })
                },
                {
                    path: 'discover',
                    name: 'discover',
                    component: SubscriptionDiscoveryPage,
                    props: route => ({
                        query: routeText(route.query.q),
                        category: routeText(route.query.category) || 'all',
                    })
                },
                // {
                //     path: 'subscriptions',
                //     name: 'subscriptions',
                //     component: SubscriptionsAddPage
                // },
                {
                    path: 'collections',
                    name: 'collections',
                    component: CollectionsPage,
                    props: route => ({ folderId: routeText(route.query.folderId) || undefined })
                },
                {
                    path: 'likes',
                    name: 'likes',
                    component: LikesPage,
                },
                {
                    path: 'history',
                    name: 'history',
                    component: HistoryPage,
                },
                {
                    path: 'feed/subscriptions',
                    name: 'feedsSubscriptions',
                    component: FeedSubscriptionsPage,
                    props: route => ({
                        tags: routeText(route.query.tags) || null,
                        category: routeText(route.query.category).toLowerCase() || null,
                        feedId: routeText(route.query.feedId) || null,
                    })
                },
                {
                    path: 'feed/channels',
                    name: 'feedChannels',
                    component: FeedChannelsPage,
                },
                {
                    path: 'feed/:feedId',
                    name: 'feed',
                    component: FeedDetailPage,
                    props: route => ({
                        feedId: routeText(route.params.feedId),
                        tags: routeText(route.query.tags) || null,
                    })
                },
                {
                    path: 'articles/:id',
                    name: 'article',
                    component: ArticleDetailPage,
                    props: route => ({
                        id: routeText(route.params.id),
                        tab: ['summary', 'mindmap'].includes(routeText(route.query.tab))
                            ? routeText(route.query.tab) : 'content',
                    })
                },
                {
                    path: 'feed/you',
                    name: 'upgrade',
                    component: UpgradePage,
                },
                {
                    path: 'admin',
                    name: 'admin',
                    component: AdminPage,
                },
                {
                    path: 'radar',
                    name: 'radar',
                    component: RadarPage,
                },
                {
                    path: 'radar/topics/:topicId',
                    name: 'radarTopic',
                    component: RadarTopicPage,
                    props: true
                }
            ]
        }
    ]
});

router.beforeEach(async (to: any, from: any) => {
    if (['collections', 'likes', 'history'].includes(to.name) && 'page' in to.query) {
        const query = { ...to.query };
        delete query.page;
        return { path: to.path, query, hash: to.hash, replace: true };
    }
    const auth = useAuthStore();
    if (!auth.initialized && auth.token) {
        try {
            await auth.fetchUser();
        } catch (err) {
            console.warn('用户信息初始化失败', err);
        }
    }
    // if (to.name !== 'auth' && !auth.isAuthenticated) {
    //     return {name: 'auth', query: {redirect: to.fullPath}};
    // }
    //
    // if (to.name === 'auth' && auth.isAuthenticated) {
    //     return {name: 'home'};
    // }

    return true;
});

router.afterEach((to) => {
    trackPageView(to)
})

// ⭐ 只注册一次
onUnauthorized(async () => {
    setAuthToken(null);

    if (router.currentRoute.value.path !== '/auth') {
        await router.replace({
            path: '/auth',
            query: {
                redirect: router.currentRoute.value.fullPath
            }
        });
    }
});

export default router;
