import {createApp} from 'vue';
import {createPinia} from 'pinia';
import {VueQueryPlugin} from '@tanstack/vue-query';
import App from './App.vue';
import router from './router';
import './styles/tailwind.css';
import Pagination from "./components/Pagination.vue";
import ArticleList from "./components/ArticleList.vue";
import ArticleCardList from "./components/ArticleCardList.vue";
import {useThemeStore} from './stores/theme';
import {usePWAStore} from './stores/pwa';
import {registerSW} from 'virtual:pwa-register';
import {queryClient} from './queryClient';

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);
app.use(router);
app.use(VueQueryPlugin, {queryClient});
app.component('pagination',Pagination)
app.component('article-list',ArticleList)
app.component('article-card-list',ArticleCardList)
const themeStore = useThemeStore(pinia);
themeStore.init();

// 初始化PWA
const pwaStore = usePWAStore(pinia);
pwaStore.initPWA();

app.mount('#app');

// 注册 Service Worker
const updateSW = registerSW({
    onNeedRefresh() {
        if (confirm('新版本可用，是否立即更新？')) {
            updateSW(true);
        }
    },
    onOfflineReady() {
        console.log('应用已可离线使用');
    },
});
