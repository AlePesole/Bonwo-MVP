import { api } from "@/lib/axios";
import type { ActivityResponse, EquipmentResponse, TrainingGoalResponse } from "@/types/api";

// ── Activities ────────────────────────────────────────────────────────────────

export interface ActivityRequest {
  name: string;
  detail: string;
  iconUploadToken?: string;
}

export const adminActivityApi = {
  list: () => api.get<ActivityResponse[]>("/catalog/activities").then((r) => r.data),
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
  list: () => api.get<EquipmentResponse[]>("/catalog/equipment").then((r) => r.data),
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
  list: () => api.get<TrainingGoalResponse[]>("/catalog/training-goals").then((r) => r.data),
  create: (body: TrainingGoalRequest) =>
    api.post<TrainingGoalResponse>("/catalog/training-goals", body).then((r) => r.data),
  update: (id: number, body: TrainingGoalRequest) =>
    api.put<TrainingGoalResponse>(`/catalog/training-goals/${id}`, body).then((r) => r.data),
  delete: (id: number) => api.delete(`/catalog/training-goals/${id}`),
};
