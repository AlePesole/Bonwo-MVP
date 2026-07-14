import { api } from "@/lib/axios";
import type { UpdateProfileRequest, UserProfileResponse } from "@/types/api";

export const profileApi = {
  getMe: () => api.get<UserProfileResponse>("/users/me").then((r) => r.data),

  getPublic: (username: string) =>
    api.get<UserProfileResponse>(`/users/${username}`).then((r) => r.data),

  patchMe: (body: UpdateProfileRequest) =>
    api.patch<UserProfileResponse>("/users/me", body).then((r) => r.data),
};
