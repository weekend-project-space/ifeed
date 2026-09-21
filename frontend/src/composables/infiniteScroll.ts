import {
  computed,
  onActivated,
  onBeforeUnmount,
  onDeactivated,
  onMounted,
  ref,
  shallowRef,
  toValue,
  watch,
  type MaybeRefOrGetter,
} from 'vue';

type InfiniteScrollOptions = {
  hasNextPage: MaybeRefOrGetter<boolean | undefined>;
  isLoading: MaybeRefOrGetter<boolean>;
  hasError?: MaybeRefOrGetter<boolean>;
  onLoadMore: () => Promise<unknown>;
  enabled?: MaybeRefOrGetter<boolean>;
  rootMargin?: string;
};

export const useInfiniteScroll = ({
  hasNextPage,
  isLoading,
  hasError = false,
  onLoadMore,
  enabled = true,
  rootMargin = '600px 0px',
}: InfiniteScrollOptions) => {
  const sentinel = ref<HTMLElement | null>(null);
  const active = ref(false);
  const requestInFlight = ref(false);
  const error = shallowRef<unknown>(null);
  let observer: IntersectionObserver | null = null;

  const canLoad = computed(() => active.value
    && toValue(enabled)
    && toValue(hasNextPage)
    && !toValue(isLoading)
    && !requestInFlight.value);
  const canAutoLoad = computed(() => canLoad.value && !toValue(hasError) && !error.value);

  const disconnect = () => {
    observer?.disconnect();
    observer = null;
  };

  const loadMore = async () => {
    if (!canLoad.value) return;

    requestInFlight.value = true;
    error.value = null;
    try {
      await onLoadMore();
    } catch (cause) {
      error.value = cause;
    } finally {
      requestInFlight.value = false;
    }
  };

  const observe = () => {
    disconnect();
    const target = sentinel.value;
    if (!target || !canAutoLoad.value || typeof IntersectionObserver === 'undefined') return;

    const currentObserver = new IntersectionObserver((entries) => {
      if (observer !== currentObserver || !canAutoLoad.value) return;
      if (entries.some((entry) => entry.isIntersecting)) {
        void loadMore();
      }
    }, {rootMargin});
    observer = currentObserver;
    observer.observe(target);
  };

  watch([sentinel, canAutoLoad], observe, {flush: 'post'});
  watch(() => toValue(hasError), (value) => {
    if (!value) error.value = null;
  });

  const activate = () => {
    active.value = true;
  };
  const deactivate = () => {
    active.value = false;
    disconnect();
  };

  onMounted(activate);
  onActivated(activate);
  onDeactivated(deactivate);
  onBeforeUnmount(deactivate);

  return {sentinel, loadMore, error};
};
