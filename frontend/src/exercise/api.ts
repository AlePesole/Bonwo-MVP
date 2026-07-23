import { api } from "@/lib/axios";
import type { ExerciseResponse, PageResponse } from "@/types/api";

export interface ExerciseFilter {
  title?: string;
  level?: string;
  muscleGroupId?: number;
  muscleSubGroupId?: number;
  equipmentIds?: number[];
  activityIds?: number[];
  trainingGoalIds?: number[];
}

export interface ExercisePayload {
  title: string;
  level?: string;
  thumbnailUploadToken?: string;
  description?: string;
  instructions?: string;
  muscles?: Array<{ subGroupId: number; activation: number }>;
  equipmentIds?: number[];
  activityIds?: number[];
  trainingGoalIds?: number[];
}

function buildParams(filter: ExerciseFilter, page: number, size: number): URLSearchParams {
  const p = new URLSearchParams();
  if (filter.title) p.set("title", filter.title);
  if (filter.level) p.set("level", filter.level);
  if (filter.muscleGroupId) p.set("muscleGroupId", String(filter.muscleGroupId));
  if (filter.muscleSubGroupId) p.set("muscleSubGroupId", String(filter.muscleSubGroupId));
  filter.equipmentIds?.forEach((id) => p.append("equipmentIds", String(id)));
  filter.activityIds?.forEach((id) => p.append("activityIds", String(id)));
  filter.trainingGoalIds?.forEach((id) => p.append("trainingGoalIds", String(id)));
  p.set("page", String(page));
  p.set("size", String(size));
  return p;
}

export const exerciseApi = {
  list: (filter: ExerciseFilter = {}, page = 0, size = 12) =>
    api
      .get<PageResponse<ExerciseResponse>>(`/exercises?${buildParams(filter, page, size)}`)
      .then((r) => r.data),

  getById: (id: number) =>
    api.get<ExerciseResponse>(`/exercises/${id}`).then((r) => r.data),

  create: (body: ExercisePayload) =>
    api.post<ExerciseResponse>("/exercises", body).then((r) => r.data),

  update: (id: number, body: Partial<ExercisePayload>) =>
    api.put<ExerciseResponse>(`/exercises/${id}`, body).then((r) => r.data),

  delete: (id: number) => api.delete(`/exercises/${id}`),
};
