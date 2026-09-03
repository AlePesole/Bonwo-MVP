import type { PublicationType } from "@/types/api";

export type ExploreScope = "official" | "community";
export type ExploreKind = "exercises" | "routines" | "programs";

export function parseExploreScope(value: string | null | undefined): ExploreScope {
  return value === "community" ? "community" : "official";
}

export function parseExploreKind(value: string | null | undefined): ExploreKind {
  if (value === "routines" || value === "programs") return value;
  return "exercises";
}

export function exploreScopeToType(scope: ExploreScope): PublicationType {
  return scope === "official" ? "OFFICIAL" : "COMMUNITY";
}

export function exploreScopeLabel(scope: ExploreScope): string {
  return scope === "official" ? "Official" : "Community";
}
