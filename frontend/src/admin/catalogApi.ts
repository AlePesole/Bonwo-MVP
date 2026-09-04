import { api } from "@/lib/axios";
import type { ActivityResponse, EquipmentResponse, MuscleGroupResponse, MuscleSubGroupResponse, TrainingGoalResponse } from "@/types/api";

// ── Activities ────────────────────────────────────────────────────────────────

export interface ActivityRequest {
  name: string;
  detail: string;
  iconUploadToken?: string;
}

export const adminActivityApi = {
  list: (signal?: AbortSignal) =>
    api.get<ActivityResponse[]>("/catalog/activities", { signal }).then((r) => r.data),
  create: (body: ActivityRequest) =>
    api.post<ActivityResponse>("/catalog/activities", body).then((r) => r.data),
  update: (id: number, body: ActivityRequest) =>
    api.put<ActivityResponse>(`/catalog/activities/${id}`, body).then((r) => r.data),
  delete: (id: number) => api.delete(`/catalog/activities/${id}`),
};

// ── Equipment ─────────────────────────────────────────────────────────────────

export interface EquipmentRequest {
  name: string;
  iconUploadToken?: string;
}

export const adminEquipmentApi = {
  list: (signal?: AbortSignal) =>
    api.get<EquipmentResponse[]>("/catalog/equipment", { signal }).then((r) => r.data),
  create: (body: EquipmentRequest) =>
    api.post<EquipmentResponse>("/catalog/equipment", body).then((r) => r.data),
  update: (id: number, body: EquipmentRequest) =>
    api.put<EquipmentResponse>(`/catalog/equipment/${id}`, body).then((r) => r.data),
  delete: (id: number) => api.delete(`/catalog/equipment/${id}`),
};

// ── Training Goals ────────────────────────────────────────────────────────────

export interface TrainingGoalRequest {
  name: string;
  detail?: string;
  iconUploadToken?: string;
}

export const adminTrainingGoalApi = {
  list: (signal?: AbortSignal) =>
    api.get<TrainingGoalResponse[]>("/catalog/training-goals", { signal }).then((r) => r.data),
  create: (body: TrainingGoalRequest) =>
    api.post<TrainingGoalResponse>("/catalog/training-goals", body).then((r) => r.data),
  update: (id: number, body: TrainingGoalRequest) =>
    api.put<TrainingGoalResponse>(`/catalog/training-goals/${id}`, body).then((r) => r.data),
  delete: (id: number) => api.delete(`/catalog/training-goals/${id}`),
};

// ── Muscle Groups ─────────────────────────────────────────────────────────────

export interface MuscleGroupRequest {
  name: string;
  iconUploadToken?: string;
}

export const adminMuscleGroupApi = {
  list: (signal?: AbortSignal) =>
    api.get<MuscleGroupResponse[]>("/catalog/muscles", { signal }).then((r) => r.data),
  create: (body: MuscleGroupRequest) =>
    api.post<MuscleGroupResponse>("/catalog/muscles", body).then((r) => r.data),
  update: (id: number, body: MuscleGroupRequest) =>
    api.put<MuscleGroupResponse>(`/catalog/muscles/${id}`, body).then((r) => r.data),
  delete: (id: number) => api.delete(`/catalog/muscles/${id}`),
};

// ── Muscle SubGroups ──────────────────────────────────────────────────────────

export interface CreateMuscleSubGroupRequest {
  groupId: number;
  name: string;
  detail?: string;
  svgPathFront?: string;
  svgPathBack?: string;
  iconUploadToken?: string;
}

export interface UpdateMuscleSubGroupRequest {
  name?: string;
  detail?: string | null;
  svgPathFront?: string | null;
  svgPathBack?: string | null;
  iconUploadToken?: string;
}

export const adminMuscleSubGroupApi = {
  create: (body: CreateMuscleSubGroupRequest) =>
    api.post<MuscleSubGroupResponse>("/catalog/muscles/sub-groups", body).then((r) => r.data),
  update: (id: number, body: UpdateMuscleSubGroupRequest) =>
    api.put<MuscleSubGroupResponse>(`/catalog/muscles/sub-groups/${id}`, body).then((r) => r.data),
  delete: (id: number) => api.delete(`/catalog/muscles/sub-groups/${id}`),
};
