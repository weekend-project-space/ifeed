export const latestRequest = () => {
  let controller: AbortController | undefined;
  let generation = 0;
  const cancel = () => {
    generation += 1;
    controller?.abort();
  };
  return {
    cancel,
    begin: () => {
      cancel();
      controller = new AbortController();
      const currentGeneration = generation;
      return { signal: controller.signal, current: () => generation === currentGeneration };
    },
  };
};
