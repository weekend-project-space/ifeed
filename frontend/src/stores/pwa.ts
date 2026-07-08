import { ref } from 'vue';
import { defineStore } from 'pinia';

export const usePWAStore = defineStore('pwa', () => {
    const deferredPrompt = ref<any>(null);
    const canInstall = ref(false);

    // 检查是否已安装
    const checkIsInstalled = () => {
        const isStandalone = window.matchMedia('(display-mode: standalone)').matches;
        const isIOSStandalone = (window.navigator as any).standalone === true;
        return isStandalone || isIOSStandalone;
    };

    // 初始化PWA监听
    const initPWA = () => {
        // 如果已安装，不显示安装按钮
        if (checkIsInstalled()) {
            canInstall.value = false;
            return;
        }

        // 监听 beforeinstallprompt 事件
        const beforeInstallPromptHandler = (e: Event) => {
            e.preventDefault();
            deferredPrompt.value = e;
            canInstall.value = true;
            console.log('PWA可以安装');
        };

        // 监听应用安装完成事件
        const appInstalledHandler = () => {
            console.log('PWA已成功安装');
            canInstall.value = false;
            deferredPrompt.value = null;
        };

        window.addEventListener('beforeinstallprompt', beforeInstallPromptHandler);
        window.addEventListener('appinstalled', appInstalledHandler);
    };

    // 触发安装
    const installPWA = async () => {
        if (!deferredPrompt.value) {
            console.warn('无法安装PWA：未捕获到安装提示事件');
            return false;
        }

        try {
            await deferredPrompt.value.prompt();
            const choiceResult = await deferredPrompt.value.userChoice;

            if (choiceResult.outcome === 'accepted') {
                console.log('用户接受了安装');
                canInstall.value = false;
                deferredPrompt.value = null;
                return true;
            } else {
                console.log('用户拒绝了安装');
                deferredPrompt.value = null;
                return false;
            }
        } catch (error) {
            console.error('PWA安装失败:', error);
            deferredPrompt.value = null;
            return false;
        }
    };

    return {
        canInstall,
        initPWA,
        installPWA,
        checkIsInstalled
    };
});
