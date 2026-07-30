import { api } from "@/lib/axios";
import type { PageResponse, ProgramRoutineDto, TrainingProgramResponse } from "@/types/api";

export interface ProgramFilter {
  equipmentIds?: number[];
  activityIds?: number[];
  trainingGoalIds?: number[];
}

export interface ProgramPayload {
  title: string;
  description?: string;
  level?: string;
  thumbnailUploadToken?: string;
  removeThumbnail?: boolean;
  daysPerWeek?: number;
  routines?: ProgramRoutineDto[];
  equipmentIds?: number[];
  activityIds?: number[];
  trainingGoalIds?: number[];
}

function buildParams(filter: ProgramFilter, page: number, size: number): URLSearchParams {
  const p = new URLSearchParams();
  filter.equipmentIds?.forEach((id) => p.append("equipmentIds", String(id)));
  filter.activityIds?.forEach((id) => p.append("activityIds", String(id)));
  filter.trainingGoalIds?.forEach((id) => p.append("trainingGoalIds", String(id)));
  p.set("page", String(page));
  p.set("size", String(size));
  p.set("sort", "createdAt,desc");
  return p;
}

export const programApi = {
  list: (filter: ProgramFilter = {}, page = 0, size = 12) =>
    api
      .get<PageResponse<TrainingProgramResponse>>(`/training-programs?${buildParams(filter, page, size)}`)
      .then((r) => r.data),

  getById: (id: number) =>
    api.get<TrainingProgramResponse>(`/training-programs/${id}`).then((r) => r.data),

  create: (body: ProgramPayload) =>
    api.post<TrainingProgramResponse>("/training-programs", body).then((r) => r.data),

  update: (id: number, body: Partial<ProgramPayload>) =>
    api.put<TrainingProgramResponse>(`/training-programs/${id}`, body).then((r) => r.data),

  delete: (id: number) => api.delete(`/training-programs/${id}`),
};
