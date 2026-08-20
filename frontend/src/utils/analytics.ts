// src/utils/analytics.ts

declare global {
    interface Window {
        gtag?: (...args: any[]) => void
    }
}

export function trackPageView(path: string) {
    window.gtag?.('event', 'page_view', {
        page_path: path,
        page_location: window.location.href,
        page_title: document.title
    })
}

export function trackEvent(
    name: string,
    params: Record<string, any> = {}
) {
    window.gtag?.('event', name, params)
}