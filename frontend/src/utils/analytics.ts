// src/utils/analytics.ts

declare global {
    interface Window {
        gtag?: (...args: any[]) => void
    }
}


export function trackPageView(
    to: {
        fullPath: string
        name?: string | symbol | null
    }
) {
    if (!window.gtag) {
        return
    }

    window.gtag('event', 'page_view', {
        page_title: document.title,
        page_location: window.location.href,
        page_path: to.fullPath
    })
}

export function trackEvent(
    name: string,
    params: Record<string, any> = {}
) {
    if (!window.gtag) {
        return
    }

    window.gtag('event', name, params)
}