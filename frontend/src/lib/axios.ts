import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import type { AuthResponse, ProblemDetail } from "@/types/api";

const STORAGE_ACCESS = "bonwo_access_token";
const STORAGE_REFRESH = "bonwo_refresh_token";

export const storage = {
  getAccess: () => localStorage.getItem(STORAGE_ACCESS),
  getRefresh: () => localStorage.getItem(STORAGE_REFRESH),
  setTokens: (access: string, refresh: string) => {
    localStorage.setItem(STORAGE_ACCESS, access);
    localStorage.setItem(STORAGE_REFRESH, refresh);
  },
  clear: () => {
    localStorage.removeItem(STORAGE_ACCESS);
    localStorage.removeItem(STORAGE_REFRESH);
    localStorage.removeItem("bonwo_user");
  },
};

export const api = axios.create({
  baseURL: "/api/v1",
});

// Attach Bearer token to every request
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = storage.getAccess();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Flag to avoid infinite retry loops
let isRefreshing = false;
let pendingRequests: Array<(token: string) => void> = [];

function onRefreshed(token: string) {
  pendingRequests.forEach((cb) => cb(token));
  pendingRequests = [];
}

// 401 → refresh once → retry; if refresh fails → logout
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ProblemDetail>) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error);
    }

    // Don't try to refresh if the failing request IS the refresh endpoint
    if (original.url?.includes("/auth/refresh") || original.url?.includes("/auth/login")) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise<string>((resolve) => {
        pendingRequests.push(resolve);
      }).then((token) => {
        original.headers.Authorization = `Bearer ${token}`;
        return api(original);
      });
    }

    original._retry = true;
    isRefreshing = true;

    const refreshToken = storage.getRefresh();
    if (!refreshToken) {
      storage.clear();
      window.location.href = "/login";
      return Promise.reject(error);
    }

    try {
      const { data } = await axios.post<AuthResponse>(
        `/api/v1/auth/refresh?refreshToken=${encodeURIComponent(refreshToken)}`
      );
      storage.setTokens(data.accessToken, data.refreshToken);
      localStorage.setItem("bonwo_user", JSON.stringify(data.user));
      onRefreshed(data.accessToken);
      original.headers.Authorization = `Bearer ${data.accessToken}`;
      return api(original);
    } catch {
      storage.clear();
      window.location.href = "/login";
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  }
);

/** Extract the human-readable message from a backend error */
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ProblemDetail | undefined;
    if (data?.detail) return data.detail;
    if (data?.errors) {
      return Object.entries(data.errors)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join(", ");
    }
    return error.message;
  }
  if (error instanceof Error) return error.message;
  return "An unexpected error occurred";
}
