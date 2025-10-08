import { createRouter, createWebHistory } from 'vue-router';
import AuthPage from '../pages/AuthPage.vue';
import HomePage from '../pages/HomePage.vue';
import MainLayout from '../layouts/MainLayout.vue';
import SubscriptionsPage from '../pages/SubscriptionsPage.vue';
import CollectionsPage from '../pages/CollectionsPage.vue';
import HistoryPage from '../pages/HistoryPage.vue';
import ArticleDetailPage from '../pages/ArticleDetailPage.vue';
import FeedPage from '../pages/FeedPage.vue';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory(),
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
          component: HomePage
        },
        {
          path: 'subscriptions',
          name: 'subscriptions',
          component: SubscriptionsPage
        },
        {
          path: 'collections',
          name: 'collections',
          component: CollectionsPage
        },
        {
          path: 'history',
          name: 'history',
          component: HistoryPage
        },
        {
          path: 'feeds/:feedId',
          name: 'feed',
          component: FeedPage,
          props: true
        },
        {
          path: 'articles/:id',
          name: 'article-detail',
          component: ArticleDetailPage,
          props: true
        }
      ]
    }
  ]
});

router.beforeEach(async (to: any) => {
  const auth = useAuthStore();
  // console.log(auth)
  if (!auth.initialized && auth.token) {
    try {
      await auth.fetchUser();
    } catch (err) {
      console.warn('用户信息初始化失败', err);
    }
  }

  if (to.name !== 'auth' && !auth.isAuthenticated) {
    return { name: 'auth', query: { redirect: to.fullPath } };
  }

  if (to.name === 'auth' && auth.isAuthenticated) {
    return { name: 'home' };
  }

  return true;
});

export default router;
