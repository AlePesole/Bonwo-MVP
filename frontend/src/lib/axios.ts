import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import type { AuthResponse, ProblemDetail } from "@/types/api";

const STORAGE_ACCESS = "bonwo_access_token";
const STORAGE_REFRESH = "bonwo_refresh_token";
const API_TIMEOUT_MS = 20_000;
const REFRESH_TIMEOUT_MS = 15_000;

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
  timeout: API_TIMEOUT_MS,
});

// Attach Bearer token to every request
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = storage.getAccess();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** Same-tab single-flight for refresh (shared by interceptor + AuthContext). */
let inFlightRefresh: Promise<string> | null = null;

function forceLogout() {
  storage.clear();
  if (!window.location.pathname.startsWith("/login")) {
    window.location.href = "/login";
  }
}

function withTimeout<T>(promise: Promise<T>, ms: number, label: string): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const id = window.setTimeout(() => {
      reject(new Error(`${label} timed out after ${ms}ms`));
    }, ms);
    promise.then(
      (value) => {
        window.clearTimeout(id);
        resolve(value);
      },
      (err) => {
        window.clearTimeout(id);
        reject(err);
      }
    );
  });
}

async function postRefresh(): Promise<string> {
  const refreshToken = storage.getRefresh();
  if (!refreshToken) {
    throw new Error("No refresh token");
  }

  const { data } = await axios.post<AuthResponse>(
    `/api/v1/auth/refresh?refreshToken=${encodeURIComponent(refreshToken)}`,
    null,
    { timeout: REFRESH_TIMEOUT_MS }
  );
  storage.setTokens(data.accessToken, data.refreshToken);
  localStorage.setItem("bonwo_user", JSON.stringify(data.user));
  return data.accessToken;
}

/**
 * Refresh access (+ refresh) tokens. Same-tab single-flight only (no navigator.locks).
 * Always settles within REFRESH_TIMEOUT_MS so callers never hang forever.
 */
export function refreshAccessToken(): Promise<string> {
  if (inFlightRefresh) return inFlightRefresh;

  inFlightRefresh = withTimeout(postRefresh(), REFRESH_TIMEOUT_MS + 1_000, "Token refresh").finally(
    () => {
      inFlightRefresh = null;
    }
  );
  return inFlightRefresh;
}

function tryRecoverFromOtherTab(failedAccess: string | null): string | null {
  const access = storage.getAccess();
  const refresh = storage.getRefresh();
  if (access && access !== failedAccess && refresh) {
    return access;
  }
  return null;
}

function retryWithToken(
  original: InternalAxiosRequestConfig & { _retry?: boolean },
  token: string
) {
  original._retry = true;
  original.headers.Authorization = `Bearer ${token}`;
  return api(original);
}

// 401 → refresh once → retry; if refresh fails → logout (unless another tab already refreshed)
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ProblemDetail>) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status !== 401 || !original || original._retry) {
      return Promise.reject(error);
    }

    if (original.url?.includes("/auth/refresh") || original.url?.includes("/auth/login")) {
      return Promise.reject(error);
    }

    const failedAccess = storage.getAccess();

    if (inFlightRefresh) {
      try {
        const token = await inFlightRefresh;
        return retryWithToken(original, token);
      } catch {
        const recovered = tryRecoverFromOtherTab(failedAccess);
        if (recovered) return retryWithToken(original, recovered);
        return Promise.reject(sessionExpiredError(error));
      }
    }

    if (!storage.getRefresh()) {
      forceLogout();
      return Promise.reject(sessionExpiredError(error));
    }

    try {
      const accessToken = await refreshAccessToken();
      return retryWithToken(original, accessToken);
    } catch {
      const recovered = tryRecoverFromOtherTab(failedAccess);
      if (recovered) return retryWithToken(original, recovered);
      forceLogout();
      return Promise.reject(sessionExpiredError(error));
    }
  }
);

function sessionExpiredError(original: AxiosError): AxiosError {
  if (original.response?.data && typeof original.response.data === "object") {
    const data = original.response.data as { detail?: string; message?: string };
    data.detail = "Session expired";
    data.message = "Session expired";
  }
  return original;
}

type ErrorBody = ProblemDetail & { message?: string };

/** Extract the human-readable message from a backend error */
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (error.code === "ECONNABORTED" || /timeout/i.test(error.message)) {
      return "Request timed out. Please try again.";
    }

    const status = error.response?.status;
    const data = error.response?.data as ErrorBody | undefined;

    if (status === 401) {
      const msg = data?.detail || data?.message || "";
      if (
        !msg ||
        msg.includes("Full authentication") ||
        msg.includes("Unauthorized") ||
        msg === "Session expired"
      ) {
        return "Session expired";
      }
    }

    if (data?.detail) return data.detail;
    if (data?.message) return data.message;
    if (data?.errors) {
      return Object.entries(data.errors)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join(", ");
    }
    return error.message;
  }
  if (error instanceof Error) {
    if (/timed out/i.test(error.message)) {
      return "Request timed out. Please try again.";
    }
    return error.message;
  }
  return "An unexpected error occurred";
}
