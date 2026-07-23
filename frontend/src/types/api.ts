// ── Auth ──────────────────────────────────────────────────────────────────────

export type UserRole = "USER" | "ADMIN";
export type AccountStatus = "ACTIVE" | "BANNED" | "DELETED";

export interface UserResponse {
  id: number;
  email: string;
  username: string;
  role: UserRole;
  status: AccountStatus;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: UserResponse;
}

export interface RegisterRequest {
  email: string;
  password: string;
  username: string;
}

export interface AuthRequest {
  email: string;
  password: string;
}

// ── Media ─────────────────────────────────────────────────────────────────────

export interface ImageResponse {
  id: number;
  url: string;
  createdAt: string;
}

export interface ImageUploadResponse {
  uploadToken: string;
  url: string;
  expiresAt: string;
}

// ── Catalog ───────────────────────────────────────────────────────────────────

export interface ActivityResponse {
  id: number;
  name: string;
  detail: string;
  icon: ImageResponse | null;
}

export interface EquipmentResponse {
  id: number;
  name: string;
  icon: ImageResponse | null;
}

export interface TrainingGoalResponse {
  id: number;
  name: string;
  detail: string | null;
  icon: ImageResponse | null;
}

export interface MuscleSubGroupResponse {
  id: number;
  groupId: number;
  name: string;
  detail: string | null;
  svgPathFront: string | null;
  svgPathBack: string | null;
  icon: ImageResponse | null;
}

export interface MuscleGroupResponse {
  id: number;
  name: string;
  icon: ImageResponse | null;
  subGroups: MuscleSubGroupResponse[];
}

// ── Exercise ──────────────────────────────────────────────────────────────────

export type Level = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
export type ActivationLevel = "PRIMARY" | "SECONDARY" | "STABILIZER";

export interface VideoResponse {
  id: number;
  url: string;
  thumbnailUrl: string | null;
  durationSeconds: number | null;
  createdAt: string;
}

export interface MuscleEntryDto {
  subGroupId: number;
  activation: number;
}

export interface MuscleEntryResponse {
  subGroupId: number;
  subGroup: MuscleSubGroupResponse;
  activation: number;
  role: ActivationLevel;
}

export interface ExerciseResponse {
  id: number;
  ownerId: number;
  title: string;
  level: Level;
  thumbnail: ImageResponse | null;
  mainVideo: VideoResponse | null;
  description: string | null;
  instructions: string | null;
  muscleSummary: Record<string, number>;
  muscles: MuscleEntryResponse[];
  equipment: EquipmentResponse[];
  activities: ActivityResponse[];
  trainingGoals: TrainingGoalResponse[];
  createdAt: string;
}

// ── Routine ───────────────────────────────────────────────────────────────────

export type SetType = "REPS" | "TIMED" | "AMRAP" | "FAILURE";
export type WeightMode = "TOTAL" | "PER_SIDE";

export interface SetConfigDto {
  type: SetType;
  reps?: number;
  weightKg?: number | null;
  weightMode?: WeightMode;
  duration?: string | null; // ISO 8601 e.g. "PT30S"
}

export interface SetConfigResponse {
  type: SetType;
  reps: number;
  weightKg: number | null;
  weightMode: WeightMode | null;
  totalWeightKg: number | null;
  duration: string | null;
}

export interface ExerciseSlotDto {
  exerciseId: number;
  position: number;
  sets: SetConfigDto[];
  restBetweenSets?: string | null;
}

export interface ExerciseSlotResponse {
  exerciseId: number;
  exercise: ExerciseResponse | null; // null if exercise was deleted
  position: number;
  sets: SetConfigResponse[];
  restBetweenSets: string | null;
}

export interface RoutineResponse {
  id: number;
  ownerId: number;
  title: string;
  description: string | null;
  level: Level;
  thumbnail: ImageResponse | null;
  estimatedDuration: string | null;
  restBetweenExercises: string | null;
  slots: ExerciseSlotResponse[];
  muscleSummary: Record<string, number>;
  equipment: EquipmentResponse[];
  activities: ActivityResponse[];
  trainingGoals: TrainingGoalResponse[];
  createdAt: string;
}



export type LibraryItemType =
  | "EXERCISE"
  | "ROUTINE"
  | "PROGRAM"
  | "PUBLICATION_EXERCISE"
  | "PUBLICATION_ROUTINE"
  | "PUBLICATION_PROGRAM";

export interface LibraryFolderSummary {
  id: number;
  name: string;
  defaultFolder: boolean;
  itemCount: number;
  createdAt: string;
}

export interface LibraryItemResponse {
  referenceId: number;
  type: LibraryItemType;
  detail: ExerciseResponse | null;
}

export interface LibraryFolderDetail {
  id: number;
  name: string;
  defaultFolder: boolean;
  items: LibraryItemResponse[];
  createdAt: string;
}

// ── User Profile ──────────────────────────────────────────────────────────────

export interface UserProfileResponse {
  userId: number;
  username: string;
  avatar: ImageResponse | null;
  bio: string | null;
  ageYears: number | null;
  heightCm: number | null;
  weightKg: number | null;
  activities: ActivityResponse[];
}

export interface UpdateProfileRequest {
  avatarUploadToken?: string;
  bio?: string | null;
  ageYears?: number | null;
  heightCm?: number | null;
  weightKg?: number | null;
  activityIds?: number[];
}

// ── Admin ─────────────────────────────────────────────────────────────────────

export interface AdminUpdateUserRequest {
  username?: string;
  bio?: string;
}

export interface ChangeRoleRequest {
  role: UserRole;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

// ── Errors (RFC 7807 ProblemDetail) ───────────────────────────────────────────

export interface ProblemDetail {
  status: number;
  title?: string;
  detail: string;
  type?: string;
  timestamp?: string;
  errors?: Record<string, string>;
}
