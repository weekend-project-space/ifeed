// auth-events.ts
type UnauthorizedHandler = () => void;

const handlers = new Set<UnauthorizedHandler>();

export const onUnauthorized = (handler: UnauthorizedHandler) => {
    handlers.add(handler);
    return () => handlers.delete(handler);
};

export const emitUnauthorized = () => {
    handlers.forEach(h => h());
};
