import { api } from "@/lib/axios";
import type { PageResponse, ProgramRoutineDto, TrainingProgramResponse } from "@/types/api";

export interface ProgramFilter {
  title?: string;
  equipmentIds?: number[];
  activityIds?: number[];
  trainingGoalIds?: number[];
}

export interface ProgramPayload {
  title: string;
  description?: string;
  level?: string;
  thumbnailUploadToken?: string;
  /** Reuse an owned image on create/duplicate; ignored if thumbnailUploadToken is set. */
  thumbnailId?: number;
  removeThumbnail?: boolean;
  daysPerWeek?: number;
  routines?: ProgramRoutineDto[];
  equipmentIds?: number[];
  activityIds?: number[];
  trainingGoalIds?: number[];
}

function buildParams(filter: ProgramFilter, page: number, size: number): URLSearchParams {
  const p = new URLSearchParams();
  if (filter.title) p.set("title", filter.title);
  filter.equipmentIds?.forEach((id) => p.append("equipmentIds", String(id)));
  filter.activityIds?.forEach((id) => p.append("activityIds", String(id)));
  filter.trainingGoalIds?.forEach((id) => p.append("trainingGoalIds", String(id)));
  p.set("page", String(page));
  p.set("size", String(size));
  p.set("sort", "createdAt,desc");
  return p;
}

export const programApi = {
  list: (filter: ProgramFilter = {}, page = 0, size = 12, signal?: AbortSignal) =>
    api
      .get<PageResponse<TrainingProgramResponse>>(`/training-programs?${buildParams(filter, page, size)}`, {
        signal,
      })
      .then((r) => r.data),

  getById: (id: number, signal?: AbortSignal) =>
    api.get<TrainingProgramResponse>(`/training-programs/${id}`, { signal }).then((r) => r.data),

  create: (body: ProgramPayload) =>
    api.post<TrainingProgramResponse>("/training-programs", body).then((r) => r.data),

  update: (id: number, body: Partial<ProgramPayload>) =>
    api.put<TrainingProgramResponse>(`/training-programs/${id}`, body).then((r) => r.data),

  delete: (id: number) => api.delete(`/training-programs/${id}`),
};
