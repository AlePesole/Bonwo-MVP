import { api } from "@/lib/axios";
import type {
  ActivityResponse,
  EquipmentResponse,
  MuscleGroupResponse,
  TrainingGoalResponse,
} from "@/types/api";

export const catalogApi = {
  listActivities: (signal?: AbortSignal) =>
    api.get<ActivityResponse[]>("/catalog/activities", { signal }).then((r) => r.data),

  listEquipment: (signal?: AbortSignal) =>
    api.get<EquipmentResponse[]>("/catalog/equipment", { signal }).then((r) => r.data),

  listTrainingGoals: (signal?: AbortSignal) =>
    api.get<TrainingGoalResponse[]>("/catalog/training-goals", { signal }).then((r) => r.data),

  listMuscleGroups: (signal?: AbortSignal) =>
    api.get<MuscleGroupResponse[]>("/catalog/muscles", { signal }).then((r) => r.data),
};
