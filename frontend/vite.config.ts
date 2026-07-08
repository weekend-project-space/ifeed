import {defineConfig, loadEnv} from 'vite';
import vue from '@vitejs/plugin-vue';
import {VitePWA} from 'vite-plugin-pwa';
import path from 'path'

export default defineConfig(({mode}) => {
    // / loadEnv 会把 .env* 文件合并到 process.env 中
    const env = loadEnv(mode, process.cwd(), '')
    const backendProxyTarget = env.VITE_BACKEND_PROXY_TARGET || 'http://localhost:8080';
    return {

        plugins: [
            vue(),
            VitePWA({
                registerType: 'autoUpdate',
                includeAssets: ['favicon.ico', 'logo.svg', 'loading.svg'],
                manifest: {
                    name: 'IFeed - RSS阅读器',
                    short_name: 'IFeed',
                    description: '简单、高效、全平台、网页版RSS 阅读器、信息助手',
                    theme_color: '#ffffff',
                    background_color: '#ffffff',
                    display: 'standalone',
                    start_url: '/',
                    scope: '/',
                    icons: [
                        {
                            src: '/logo.svg',
                            sizes: '512x512',
                            type: 'image/svg+xml'
                        },
                        {
                            src: '/logo.svg',
                            sizes: '512x512',
                            type: 'image/svg+xml',
                            purpose: 'any maskable'
                        },
                        {
                            src: '/favicon.ico',
                            sizes: '64x64 32x32 24x24 16x16',
                            type: 'image/x-icon'
                        }
                    ]
                },
                workbox: {
                    cleanupOutdatedCaches: true,
                    globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
                    runtimeCaching: [
                        {
                            urlPattern: /^https:\/\/www\.ifeed\.cc\/api\/.*/i,
                            handler: 'NetworkFirst',
                            options: {
                                cacheName: 'api-cache',
                                expiration: {
                                    maxEntries: 100,
                                    maxAgeSeconds: 60 * 60 * 24 // 24小时
                                },
                                cacheableResponse: {
                                    statuses: [0, 200]
                                }
                            }
                        },
                        {
                            urlPattern: /\.(png|jpg|jpeg|svg|gif|webp)$/i,
                            handler: 'CacheFirst',
                            options: {
                                cacheName: 'images-cache',
                                expiration: {
                                    maxEntries: 200,
                                    maxAgeSeconds: 60 * 60 * 24 * 30 // 30天
                                }
                            }
                        }
                    ]
                },
                devOptions: {
                    enabled: true,
                    type: 'module'
                }
            })
        ],
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
