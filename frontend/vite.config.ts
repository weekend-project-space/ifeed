import {defineConfig, loadEnv} from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path'

export default defineConfig(({mode}) => {
    // / loadEnv 会把 .env* 文件合并到 process.env 中
    const env = loadEnv(mode, process.cwd(), '')
    const backendProxyTarget = env.VITE_BACKEND_PROXY_TARGET || 'http://localhost:8080';
    return {

        plugins: [vue()],
        resolve: {
            alias: {
                '@': path.resolve(__dirname, './src')
            }
        },
        server: {
            host: '0.0.0.0',
            port: 5173,
            proxy: {
                '/api': {
                    target: backendProxyTarget,
                    changeOrigin: true,
                    secure: false
                }
            }
        }
    }
});
