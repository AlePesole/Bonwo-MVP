import { api } from "@/lib/axios";
import type { PageResponse, RoutineResponse } from "@/types/api";

export interface RoutineFilter {
  muscleGroupIds?: number[];
  muscleSubGroupIds?: number[];
  equipmentIds?: number[];
  activityIds?: number[];
  trainingGoalIds?: number[];
}

export interface RoutinePayload {
  title: string;
  description?: string;
  level?: string;
  thumbnailUploadToken?: string;
  /** Reuse an owned image on create/duplicate; ignored if thumbnailUploadToken is set. */
  thumbnailId?: number;
  removeThumbnail?: boolean;
  slots?: Array<{
    exerciseId: number;
    position: number;
    sets: Array<{
      type: string;
      reps?: number;
      weightKg?: number | null;
      weightMode?: string;
      duration?: string | null;
    }>;
    restBetweenSets?: string | null;
  }>;
  restBetweenExercises?: string | null;
  equipmentIds?: number[];
  activityIds?: number[];
  trainingGoalIds?: number[];
}

function buildParams(filter: RoutineFilter, page: number, size: number): URLSearchParams {
  const p = new URLSearchParams();
  filter.equipmentIds?.forEach((id) => p.append("equipmentIds", String(id)));
  filter.activityIds?.forEach((id) => p.append("activityIds", String(id)));
  filter.trainingGoalIds?.forEach((id) => p.append("trainingGoalIds", String(id)));
  p.set("page", String(page));
  p.set("size", String(size));
  p.set("sort", "createdAt,desc");
  return p;
}

export const routineApi = {
  list: (filter: RoutineFilter = {}, page = 0, size = 12, signal?: AbortSignal) =>
    api
      .get<PageResponse<RoutineResponse>>(`/routines?${buildParams(filter, page, size)}`, {
        signal,
      })
      .then((r) => r.data),

  getById: (id: number, signal?: AbortSignal) =>
    api.get<RoutineResponse>(`/routines/${id}`, { signal }).then((r) => r.data),

  create: (body: RoutinePayload) =>
    api.post<RoutineResponse>("/routines", body).then((r) => r.data),

  update: (id: number, body: Partial<RoutinePayload>) =>
    api.put<RoutineResponse>(`/routines/${id}`, body).then((r) => r.data),

  delete: (id: number) => api.delete(`/routines/${id}`),
};

// ── Duration helpers ──────────────────────────────────────────────────────────

/** Parse ISO 8601 duration string to total seconds */
export function parseDuration(iso: string | null | undefined): number {
  if (!iso) return 0;
  const m = iso.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/);
  if (!m) return 0;
  return Number(m[1] || 0) * 3600 + Number(m[2] || 0) * 60 + Math.round(Number(m[3] || 0));
}

/** Format total seconds to ISO 8601 duration string */
export function formatDuration(seconds: number): string | null {
  if (!seconds || seconds <= 0) return null;
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  let r = "PT";
  if (h > 0) r += `${h}H`;
  if (m > 0) r += `${m}M`;
  if (s > 0) r += `${s}S`;
  return r === "PT" ? null : r;
}

/** Display a duration string as "Xh Xmin" or "Xs" */
export function displayDuration(iso: string | null | undefined): string {
  const s = parseDuration(iso);
  if (s <= 0) return "—";
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  if (h > 0 && m > 0) return `${h}h ${m}min`;
  if (h > 0) return `${h}h`;
  if (m > 0 && sec > 0) return `${m}min ${sec}s`;
  if (m > 0) return `${m}min`;
  return `${sec}s`;
}
