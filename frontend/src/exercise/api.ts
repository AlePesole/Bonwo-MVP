import { api } from "@/lib/axios";
import type { ExerciseResponse, PageResponse } from "@/types/api";

export interface ExerciseFilter {
  title?: string;
  level?: string;
  muscleGroupIds?: number[];
  muscleSubGroupIds?: number[];
  equipmentIds?: number[];
  activityIds?: number[];
  trainingGoalIds?: number[];
}

export interface ExercisePayload {
  title: string;
  level?: string;
  thumbnailUploadToken?: string;
  removeThumbnail?: boolean;
  mainVideoUploadToken?: string;
  removeMainVideo?: boolean;
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
  // Backend only accepts a single muscleGroupId / muscleSubGroupId.
  // Multiple selections are filtered client-side.
  if (filter.muscleSubGroupIds?.length === 1) {
    p.set("muscleSubGroupId", String(filter.muscleSubGroupIds[0]));
  } else if (filter.muscleGroupIds?.length === 1 && !filter.muscleSubGroupIds?.length) {
    p.set("muscleGroupId", String(filter.muscleGroupIds[0]));
  }
  filter.equipmentIds?.forEach((id) => p.append("equipmentIds", String(id)));
  filter.activityIds?.forEach((id) => p.append("activityIds", String(id)));
  filter.trainingGoalIds?.forEach((id) => p.append("trainingGoalIds", String(id)));
  p.set("page", String(page));
  p.set("size", String(size));
  return p;
}

export const exerciseApi = {
  list: (filter: ExerciseFilter = {}, page = 0, size = 12, signal?: AbortSignal) =>
    api
      .get<PageResponse<ExerciseResponse>>(`/exercises?${buildParams(filter, page, size)}`, {
        signal,
      })
      .then((r) => r.data),

  getById: (id: number, signal?: AbortSignal) =>
    api.get<ExerciseResponse>(`/exercises/${id}`, { signal }).then((r) => r.data),

  create: (body: ExercisePayload) =>
    api.post<ExerciseResponse>("/exercises", body).then((r) => r.data),

  update: (id: number, body: Partial<ExercisePayload>) =>
    api.put<ExerciseResponse>(`/exercises/${id}`, body).then((r) => r.data),

  delete: (id: number) => api.delete(`/exercises/${id}`),
};
