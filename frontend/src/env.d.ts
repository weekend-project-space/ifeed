// vite-env.d.ts
/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_BACKEND_PROXY_TARGET: string
    readonly VITE_DEBUG_MODE: string
    // 更多...
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}