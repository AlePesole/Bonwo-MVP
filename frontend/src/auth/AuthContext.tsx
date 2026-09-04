import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import { api, refreshAccessToken, storage } from "@/lib/axios";
import { isAccessTokenExpiringSoon } from "@/lib/jwt";
import type { AuthRequest, AuthResponse, RegisterRequest, UserResponse } from "@/types/api";

interface AuthState {
  user: UserResponse | null;
  isLoading: boolean;
}

interface AuthContextValue extends AuthState {
  login: (data: AuthRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const PROACTIVE_REFRESH_MS = 90_000;
const PROACTIVE_CHECK_INTERVAL_MS = 30_000;

function hasSessionTokens(): boolean {
  return !!(storage.getAccess() || storage.getRefresh());
}

function readStoredUser(): UserResponse | null {
  const raw = localStorage.getItem("bonwo_user");
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserResponse;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>(() => {
    const user = readStoredUser();
    // User without tokens is not a valid session
    if (user && !hasSessionTokens()) {
      storage.clear();
      return { user: null, isLoading: false };
    }
    return { user, isLoading: false };
  });

  const setUser = useCallback((user: UserResponse | null) => {
    if (user) {
      localStorage.setItem("bonwo_user", JSON.stringify(user));
    } else {
      localStorage.removeItem("bonwo_user");
    }
    setState((prev) => ({ ...prev, user }));
  }, []);

  const login = useCallback(
    async (credentials: AuthRequest) => {
      const { data } = await api.post<AuthResponse>("/auth/login", credentials);
      storage.setTokens(data.accessToken, data.refreshToken);
      setUser(data.user);
    },
    [setUser]
  );

  const register = useCallback(async (payload: RegisterRequest) => {
    await api.post("/auth/register", payload);
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = storage.getRefresh();
    try {
      if (refreshToken) {
        await api.post(`/auth/logout?refreshToken=${encodeURIComponent(refreshToken)}`);
      }
    } catch {
      // Ignore logout errors — clear local state regardless
    } finally {
      storage.clear();
      setUser(null);
    }
  }, [setUser]);

  // Proactive refresh only when we still have an access token that is near expiry
  useEffect(() => {
    let cancelled = false;

    const ensureFresh = async () => {
      const access = storage.getAccess();
      const refresh = storage.getRefresh();
      if (!access || !refresh) return;
      if (!isAccessTokenExpiringSoon(access, PROACTIVE_REFRESH_MS)) return;
      try {
        await refreshAccessToken();
        if (cancelled) return;
        const raw = localStorage.getItem("bonwo_user");
        if (raw) {
          setState((prev) => ({
            ...prev,
            user: JSON.parse(raw) as UserResponse,
          }));
        }
      } catch {
        // Interceptor / next API call will force logout if refresh is truly dead
      }
    };

    void ensureFresh();
    const id = window.setInterval(() => void ensureFresh(), PROACTIVE_CHECK_INTERVAL_MS);
    return () => {
      cancelled = true;
      window.clearInterval(id);
    };
  }, []);

  // Sync user + session across tabs
  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (
        e.key === "bonwo_user" ||
        e.key === "bonwo_access_token" ||
        e.key === "bonwo_refresh_token"
      ) {
        const user = readStoredUser();
        if (user && !hasSessionTokens()) {
          setState((prev) => ({ ...prev, user: null }));
          return;
        }
        if (!user || !hasSessionTokens()) {
          setState((prev) => ({ ...prev, user: null }));
          return;
        }
        setState((prev) => ({ ...prev, user }));
      }
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const isAuthenticated = state.user !== null && hasSessionTokens();

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        register,
        logout,
        isAuthenticated,
        isAdmin: state.user?.role === "ADMIN",
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
