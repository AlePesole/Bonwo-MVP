import { api } from "@/lib/axios";
import type {
  ExercisePublicationResponse,
  PageResponse,
  PublicationSort,
  PublicationType,
  Visibility,
} from "@/types/api";

export interface PublicationFilter {
  title?: string;
  type?: PublicationType;
  sort?: PublicationSort;
  muscleGroupIds?: number[];
  muscleSubGroupIds?: number[];
  equipmentIds?: number[];
  activityIds?: number[];
  trainingGoalIds?: number[];
}

export interface CreatePublicationPayload {
  title: string;
  level?: string;
  thumbnailUploadToken: string;
  mainVideoUploadToken: string;
  description?: string;
  instructions?: string;
  muscles?: Array<{ subGroupId: number; activation: number }>;
  equipmentIds: number[];
  activityIds: number[];
  trainingGoalIds: number[];
  type: PublicationType;
  visibility?: Visibility;
}

export interface UpdatePublicationPayload {
  title?: string;
  level?: string;
  thumbnailUploadToken?: string;
  description?: string;
  instructions?: string;
  muscles?: Array<{ subGroupId: number; activation: number }>;
  equipmentIds?: number[];
  activityIds?: number[];
  trainingGoalIds?: number[];
  visibility?: Visibility;
}

function buildParams(filter: PublicationFilter, page: number, size: number): URLSearchParams {
  const p = new URLSearchParams();
  if (filter.title) p.set("title", filter.title);
  if (filter.type) p.set("type", filter.type);
  if (filter.sort) p.set("sort", filter.sort);
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

export const publicationApi = {
  listFeed: (filter: PublicationFilter = {}, page = 0, size = 12, signal?: AbortSignal) =>
    api
      .get<PageResponse<ExercisePublicationResponse>>(
        `/exercise-publications?${buildParams(filter, page, size)}`,
        { signal }
      )
      .then((r) => r.data),

  listMine: (filter: PublicationFilter = {}, page = 0, size = 12, signal?: AbortSignal) =>
    api
      .get<PageResponse<ExercisePublicationResponse>>(
        `/exercise-publications/mine?${buildParams(filter, page, size)}`,
        { signal }
      )
      .then((r) => r.data),

  listLiked: (filter: PublicationFilter = {}, page = 0, size = 12, signal?: AbortSignal) =>
    api
      .get<PageResponse<ExercisePublicationResponse>>(
        `/exercise-publications/liked?${buildParams(filter, page, size)}`,
        { signal }
      )
      .then((r) => r.data),

  listSaved: (filter: PublicationFilter = {}, page = 0, size = 12, signal?: AbortSignal) =>
    api
      .get<PageResponse<ExercisePublicationResponse>>(
        `/exercise-publications/saved?${buildParams(filter, page, size)}`,
        { signal }
      )
      .then((r) => r.data),

  getById: (id: number, signal?: AbortSignal) =>
    api.get<ExercisePublicationResponse>(`/exercise-publications/${id}`, { signal }).then((r) => r.data),

  create: (body: CreatePublicationPayload) =>
    api.post<ExercisePublicationResponse>("/exercise-publications", body).then((r) => r.data),

  update: (id: number, body: UpdatePublicationPayload) =>
    api.put<ExercisePublicationResponse>(`/exercise-publications/${id}`, body).then((r) => r.data),

  delete: (id: number) => api.delete(`/exercise-publications/${id}`),

  like: (id: number) =>
    api.post<ExercisePublicationResponse>(`/exercise-publications/${id}/like`).then((r) => r.data),

  unlike: (id: number) =>
    api.delete<ExercisePublicationResponse>(`/exercise-publications/${id}/like`).then((r) => r.data),

  save: (id: number) =>
    api.post<ExercisePublicationResponse>(`/exercise-publications/${id}/save`).then((r) => r.data),

  unsave: (id: number) =>
    api
      .delete<ExercisePublicationResponse>(`/exercise-publications/${id}/save`)
      .then((r) => r.data),
};
