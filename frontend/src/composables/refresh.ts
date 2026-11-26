import {onDeactivated, onScopeDispose, ref, shallowRef, toValue, watch, type MaybeRefOrGetter} from 'vue';

type RefreshOptions = {
  onRefresh: (signal: AbortSignal) => Promise<unknown>;
  enabled?: MaybeRefOrGetter<boolean>;
  resetOn?: MaybeRefOrGetter<unknown>;
};

export const useRefresh = ({onRefresh, enabled = true, resetOn}: RefreshOptions) => {
  const isRefreshing = ref(false);
  const refreshError = shallowRef<unknown>(null);
  let controller: AbortController | null = null;

  const cancelRefresh = () => {
    controller?.abort();
    controller = null;
    isRefreshing.value = false;
    refreshError.value = null;
  };

  const refresh = async () => {
    if (isRefreshing.value || !toValue(enabled)) return;
    const request = new AbortController();
    controller = request;
    isRefreshing.value = true;
    refreshError.value = null;
    try {
      await onRefresh(request.signal);
    } catch (error) {
      if (!request.signal.aborted) refreshError.value = error;
    } finally {
      if (controller === request) {
        controller = null;
        isRefreshing.value = false;
      }
    }
  };

  if (resetOn !== undefined) watch(() => toValue(resetOn), cancelRefresh, {flush: 'sync'});
  onDeactivated(cancelRefresh);
  onScopeDispose(cancelRefresh);

  return {refresh, isRefreshing, refreshError};
};
