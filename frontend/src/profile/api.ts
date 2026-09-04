import { api } from "@/lib/axios";
import type { UpdateProfileRequest, UserProfileResponse } from "@/types/api";

export const profileApi = {
  getMe: (signal?: AbortSignal) =>
    api.get<UserProfileResponse>("/users/me", { signal }).then((r) => r.data),

  getPublic: (username: string, signal?: AbortSignal) =>
    api.get<UserProfileResponse>(`/users/${username}`, { signal }).then((r) => r.data),

  patchMe: (body: UpdateProfileRequest) =>
    api.patch<UserProfileResponse>("/users/me", body).then((r) => r.data),
};
