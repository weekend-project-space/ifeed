const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

let authToken: string | null = localStorage.getItem('auth-token');

export interface HttpError extends Error {
  status: number;
  payload?: unknown;
}

export type QueryParams = Record<string, string | number | boolean | undefined | null>;

export const setAuthToken = (token: string | null) => {
  authToken = token;
  if (token) {
    localStorage.setItem('auth-token', token)
  }
};

const buildUrl = (path: string, query?: QueryParams) => {
  const normalizedPath = path.startsWith('http') ? path : `${API_BASE_URL}${path}`;
  if (!query) {
    return normalizedPath;
  }

  const searchParams = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null) {
      return;
    }
    searchParams.append(key, String(value));
  });

  const queryString = searchParams.toString();
  return queryString ? `${normalizedPath}?${queryString}` : normalizedPath;
};

export interface RequestOptions extends RequestInit {
  query?: QueryParams;
  skipAuth?: boolean;
  json?: unknown;
}

export async function request<T = unknown>(path: string, options: RequestOptions = {}): Promise<T> {
  const { query, skipAuth, json, headers, body, ...rest } = options;
  const url = buildUrl(path, query);

  const finalHeaders = new Headers(headers ?? {});

  const isJsonPayload = json !== undefined;
  const payload = isJsonPayload ? json : body;

  if (authToken && !skipAuth) {
    finalHeaders.set('Authorization', `Bearer ${authToken}`);
  }

  if (isJsonPayload && !finalHeaders.has('Content-Type')) {
    finalHeaders.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, {
    ...rest,
    body: isJsonPayload ? JSON.stringify(json) : payload,
    headers: finalHeaders
  });

  const contentType = response.headers.get('content-type') ?? '';
  const isJsonResponse = contentType.includes('application/json');
  const data = isJsonResponse ? await response.json().catch(() => null) : await response.text();

  if (!response.ok) {
    const error: Error & { payload: any; status: number } = Object.assign(new Error((response.json()).message), {
      status: response.status,
      payload: data
    });
    throw error;
  }

  return (data as T) ?? (undefined as T);
}

export const get = <T = unknown>(path: string, options: RequestOptions = {}) => request<T>(path, { ...options, method: options.method ?? 'GET' });

export const post = <T = unknown>(path: string, json?: unknown, options: RequestOptions = {}) =>
  request<T>(path, { ...options, method: 'POST', json });

export const put = <T = unknown>(path: string, json?: unknown, options: RequestOptions = {}) =>
  request<T>(path, { ...options, method: 'PUT', json });

export const del = <T = unknown>(path: string, options: RequestOptions = {}) =>
  request<T>(path, { ...options, method: 'DELETE' });
