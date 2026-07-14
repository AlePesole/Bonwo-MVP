import { api } from "@/lib/axios";
import type {
  AdminUpdateUserRequest,
  ChangeRoleRequest,
  PageResponse,
  UserResponse,
  UserRole,
} from "@/types/api";

export const adminApi = {
  listUsers: (page = 0, size = 20) =>
    api
      .get<PageResponse<UserResponse>>("/admin/users", { params: { page, size, sort: "createdAt,desc" } })
      .then((r) => r.data),

  getUser: (id: number) =>
    api.get<UserResponse>(`/admin/users/${id}`).then((r) => r.data),

  updateUser: (id: number, body: AdminUpdateUserRequest) =>
    api.patch<UserResponse>(`/admin/users/${id}`, body).then((r) => r.data),

  banUser: (id: number) =>
    api.post<void>(`/admin/users/${id}/ban`).then((r) => r.data),

  unbanUser: (id: number) =>
    api.post<void>(`/admin/users/${id}/unban`).then((r) => r.data),

  deleteUser: (id: number) =>
    api.delete<void>(`/admin/users/${id}`).then((r) => r.data),

  changeRole: (id: number, role: UserRole) =>
    api.patch<void>(`/admin/users/${id}/role`, { role } as ChangeRoleRequest).then((r) => r.data),
};
