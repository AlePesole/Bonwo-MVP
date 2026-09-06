import { api } from "@/lib/axios";
import type {
  PageResponse,
  TrainingSessionResponse,
  TrainingSlotDto,
} from "@/types/api";

export interface TrainingSessionUpdatePayload {
  slots?: TrainingSlotDto[];
  finalNote?: string;
}

export const sessionApi = {
  listMine: (page = 0, size = 12, signal?: AbortSignal) =>
    api
      .get<PageResponse<TrainingSessionResponse>>(
        `/training-sessions?page=${page}&size=${size}&sort=startedAt,desc`,
        { signal }
      )
      .then((r) => r.data),

  getById: (id: number, signal?: AbortSignal) =>
    api
      .get<TrainingSessionResponse>(`/training-sessions/${id}`, { signal })
      .then((r) => r.data),

  start: (routineId: number) =>
    api
      .post<TrainingSessionResponse>("/training-sessions", { routineId })
      .then((r) => r.data),

  update: (id: number, body: TrainingSessionUpdatePayload) =>
    api
      .put<TrainingSessionResponse>(`/training-sessions/${id}`, body)
      .then((r) => r.data),

  complete: (id: number, finalNote?: string) =>
    api
      .post<TrainingSessionResponse>(`/training-sessions/${id}/complete`, {
        ...(finalNote != null && finalNote !== "" ? { finalNote } : {}),
      })
      .then((r) => r.data),

  delete: (id: number) => api.delete(`/training-sessions/${id}`),
};
