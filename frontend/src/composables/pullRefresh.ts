import {
  computed,
  onActivated,
  onBeforeUnmount,
  onDeactivated,
  onMounted,
  ref,
  toValue,
  watch,
  type MaybeRefOrGetter,
} from 'vue';

type PullRefreshOptions = {
  enabled?: MaybeRefOrGetter<boolean>;
  refreshing: MaybeRefOrGetter<boolean>;
  onRefresh: () => void | Promise<unknown>;
};

const PULL_THRESHOLD = 60;
const MAX_PULL_DISTANCE = 100;

let overscrollUsers = 0;
let previousOverscroll = '';
let previousOverscrollPriority = '';
const lockViewportOverscroll = () => {
  const style = document.documentElement.style;
  if (overscrollUsers++ === 0) {
    previousOverscroll = style.getPropertyValue('overscroll-behavior-y');
    previousOverscrollPriority = style.getPropertyPriority('overscroll-behavior-y');
    style.setProperty('overscroll-behavior-y', 'none');
  }
  return () => {
    if (--overscrollUsers > 0) return;
    if (previousOverscroll) style.setProperty('overscroll-behavior-y', previousOverscroll, previousOverscrollPriority);
    else style.removeProperty('overscroll-behavior-y');
  };
};

export const usePullRefresh = ({enabled = true, refreshing, onRefresh}: PullRefreshOptions) => {
  const target = ref<HTMLElement | null>(null);
  const active = ref(false);
  const smallScreen = ref(false);
  const distance = ref(0);
  const isPulling = ref(false);
  const isReady = computed(() => distance.value >= PULL_THRESHOLD);
  const busy = computed(() => isPulling.value || toValue(refreshing));
  const indicator = computed(() => ({
    height: toValue(refreshing) ? PULL_THRESHOLD : distance.value,
    pulling: isPulling.value,
    ready: isReady.value,
    refreshing: toValue(refreshing),
  }));
  const canListen = computed(() => active.value && smallScreen.value && toValue(enabled));
  let mediaQuery: MediaQueryList | null = null;
  let touchId: number | null = null;
  let startX = 0;
  let startY = 0;

  const reset = () => {
    touchId = null;
    distance.value = 0;
    isPulling.value = false;
  };

  const isAtTop = () => (window.scrollY || document.documentElement.scrollTop) <= 1;

  const onTouchStart = (event: TouchEvent) => {
    if (toValue(refreshing)) return;
    reset();
    if (event.touches.length !== 1 || !isAtTop() || event.defaultPrevented) return;

    const origin = event.target instanceof Element ? event.target : null;
    if (origin?.closest('input, textarea, select, video, audio, iframe, [contenteditable]:not([contenteditable="false"]), [data-no-pull-refresh]')) return;

    // Leave nested scrollable content in control of its own gestures.
    for (let element = origin; element && element !== target.value; element = element.parentElement) {
      if (element.scrollHeight > element.clientHeight && /auto|scroll/.test(getComputedStyle(element).overflowY)) return;
    }

    const touch = event.touches[0];
    touchId = touch.identifier;
    startX = touch.clientX;
    startY = touch.clientY;
  };

  const onTouchMove = (event: TouchEvent) => {
    if (touchId === null || toValue(refreshing)) return;
    if (event.touches.length !== 1 || !isAtTop() || !event.cancelable) {
      reset();
      return;
    }

    const touch = event.touches[0];
    if (touch.identifier !== touchId) {
      reset();
      return;
    }

    const deltaX = touch.clientX - startX;
    const deltaY = touch.clientY - startY;
    if (!isPulling.value) {
      if (Math.max(Math.abs(deltaX), Math.abs(deltaY)) < 8) return;
      if (deltaY <= 0 || deltaY <= Math.abs(deltaX)) {
        reset();
        return;
      }
      isPulling.value = true;
    }

    event.preventDefault();
    distance.value = Math.min(MAX_PULL_DISTANCE, Math.max(0, deltaY * 0.5));
  };

  const onTouchEnd = () => {
    const shouldRefresh = touchId !== null && isReady.value && !toValue(refreshing);
    reset();
    if (shouldRefresh) void onRefresh();
  };

  watch([target, canListen], ([element, listen], _, onCleanup) => {
    reset();
    if (!element || !listen) return;

    // The viewport scrolls, so overscroll must be controlled on its root.
    const unlockOverscroll = lockViewportOverscroll();
    element.addEventListener('touchstart', onTouchStart, {passive: true});
    element.addEventListener('touchmove', onTouchMove, {passive: false});
    element.addEventListener('touchend', onTouchEnd, {passive: true});
    element.addEventListener('touchcancel', reset, {passive: true});

    onCleanup(() => {
      element.removeEventListener('touchstart', onTouchStart);
      element.removeEventListener('touchmove', onTouchMove);
      element.removeEventListener('touchend', onTouchEnd);
      element.removeEventListener('touchcancel', reset);
      unlockOverscroll();
      reset();
    });
  }, {flush: 'post'});

  watch(() => toValue(refreshing), reset);

  const updateScreen = () => {
    smallScreen.value = mediaQuery?.matches ?? false;
  };
  const activate = () => {
    if (active.value || typeof window === 'undefined') return;
    mediaQuery = window.matchMedia('(max-width: 1023px)');
    updateScreen();
    mediaQuery.addEventListener('change', updateScreen);
    active.value = true;
  };
  const deactivate = () => {
    active.value = false;
    mediaQuery?.removeEventListener('change', updateScreen);
    mediaQuery = null;
    reset();
  };

  onMounted(activate);
  onActivated(activate);
  onDeactivated(deactivate);
  onBeforeUnmount(deactivate);

  return {target, distance, isPulling, isReady, busy, indicator, threshold: PULL_THRESHOLD};
};
