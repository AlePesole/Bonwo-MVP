import { api } from "@/lib/axios";
import type {
  ActivityResponse,
  EquipmentResponse,
  MuscleGroupResponse,
  TrainingGoalResponse,
} from "@/types/api";

export const catalogApi = {
  listActivities: () =>
    api.get<ActivityResponse[]>("/catalog/activities").then((r) => r.data),

  listEquipment: () =>
    api.get<EquipmentResponse[]>("/catalog/equipment").then((r) => r.data),

  listTrainingGoals: () =>
    api.get<TrainingGoalResponse[]>("/catalog/training-goals").then((r) => r.data),

  listMuscleGroups: () =>
    api.get<MuscleGroupResponse[]>("/catalog/muscles").then((r) => r.data),
};
