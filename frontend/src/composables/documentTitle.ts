import {onActivated, onBeforeUnmount, onDeactivated, ref, toValue, watch} from 'vue';
import type {MaybeRefOrGetter} from 'vue';

export const DEFAULT_DOCUMENT_TITLE = 'IFeed';
const DOCUMENT_TITLE_SUFFIX = 'Ifeed';

const formatDocumentTitle = (title: string | null | undefined) => {
  const normalizedTitle = title?.trim();
  return normalizedTitle
    ? `${normalizedTitle} - ${DOCUMENT_TITLE_SUFFIX}`
    : DEFAULT_DOCUMENT_TITLE;
};

export const useDocumentTitle = (title: MaybeRefOrGetter<string | null | undefined>) => {
  const isActive = ref(true);
  const updateTitle = () => {
    if (!isActive.value) return;
    document.title = formatDocumentTitle(toValue(title));
  };

  watch(() => toValue(title), updateTitle, {immediate: true});
  onActivated(() => {
    isActive.value = true;
    updateTitle();
  });
  onDeactivated(() => {
    isActive.value = false;
    document.title = DEFAULT_DOCUMENT_TITLE;
  });
  onBeforeUnmount(() => {
    isActive.value = false;
    document.title = DEFAULT_DOCUMENT_TITLE;
  });
};
