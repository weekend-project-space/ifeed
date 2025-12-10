import {createApp} from 'vue';
import {createPinia} from 'pinia';
import App from './App.vue';
import router from './router';
import './styles/tailwind.css';
import Pagination from "./components/Pagination.vue";
import ArticleList from "./components/ArticleList.vue";
import {useThemeStore} from './stores/theme';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.component('pagination',Pagination)
app.component('article-list',ArticleList)
const themeStore = useThemeStore(pinia);
themeStore.init();
app.mount('#app');
