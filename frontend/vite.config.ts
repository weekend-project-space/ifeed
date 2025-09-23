import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

const backendProxyTarget = process.env.VITE_BACKEND_PROXY_TARGET || 'http://localhost:8080';

export default defineConfig(() => ({
  plugins: [vue()],
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
}));
