import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import { api, storage } from "@/lib/axios";
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

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>(() => {
    const raw = localStorage.getItem("bonwo_user");
    return {
      user: raw ? (JSON.parse(raw) as UserResponse) : null,
      isLoading: false,
    };
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

  const register = useCallback(
    async (payload: RegisterRequest) => {
      await api.post("/auth/register", payload);
    },
    []
  );

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

  // Keep user in sync if token refresh updated it
  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === "bonwo_user") {
        const raw = e.newValue;
        setState((prev) => ({
          ...prev,
          user: raw ? (JSON.parse(raw) as UserResponse) : null,
        }));
      }
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        register,
        logout,
        isAuthenticated: state.user !== null,
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
