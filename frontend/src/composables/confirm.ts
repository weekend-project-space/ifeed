import { createApp, h, ref } from 'vue';
import ConfirmModal from '@/components/ConfirmDialog.vue';

interface ConfirmOptions {
    title: string;
    description?: string;
    confirmText?: string;
    cancelText?: string;
    confirmLoadingText?: string;
    closeOnBackdrop?: boolean;
}

export function confirm(options: ConfirmOptions): Promise<boolean> {
    return new Promise((resolve, reject) => {
        const div = document.createElement('div');
        document.body.appendChild(div);

        // 使用 ref 管理动态状态（如 loading/disabled，如果需要异步更新）
        const visible = ref(true);
        const confirmLoading = ref(false);
        const confirmDisabled = ref(false);
        const app = createApp({
            render() {
                return h(ConfirmModal, {
                    visible: visible.value,
                    title: options.title,
                    description: options.description,
                    confirmText: options.confirmText,
                    confirmLoadingText: options.confirmLoadingText,
                    cancelText: options.cancelText,
                    confirmLoading: confirmLoading.value,
                    confirmDisabled: confirmDisabled.value,
                    closeOnBackdrop: options.closeOnBackdrop,
                    'onUpdate:visible': (value: boolean) => { visible.value = value; },
                    onConfirm: () => {
                        resolve(true);
                        cleanup();
                    },
                    onCancel: () => {
                        reject(false);
                        cleanup();
                    },
                    onClose: () => {
                        reject(false);
                        cleanup();
                    },
                });
            },
        });

        app.mount(div);

        function cleanup() {
            visible.value = false;
            app.unmount();
            document.body.removeChild(div);
        }
    });
}