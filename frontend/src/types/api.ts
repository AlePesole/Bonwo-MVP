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
