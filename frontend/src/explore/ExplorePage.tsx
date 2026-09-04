import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { publicationApi, type PublicationFilter } from "@/publication/api";
import { PublicationSortSelect } from "@/publication/PublicationSortSelect";
import { catalogApi } from "@/catalog/api";
import { ExerciseDetailDialog } from "@/exercise/ExerciseDetailDialog";
import { MuscleGroupFilterRow } from "@/library/MuscleGroupFilterRow";
import { cn, formatTimeAgo } from "@/lib/utils";
import { getErrorMessage } from "@/lib/axios";
import type {
  ExercisePublicationResponse,
  Level,
  MuscleGroupResponse,
  PublicationType,
} from "@/types/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { ApiError } from "@/components/ApiError";
import { PageSpinner } from "@/components/Spinner";
import {
  BadgeCheck,
  CalendarDays,
  Dumbbell,
  Eye,
  Layers,
  Search,
  SlidersHorizontal,
  Users,
  X,
} from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { useEffect, useMemo, useState } from "react";
import {
  exploreScopeLabel,
  exploreScopeToType,
  parseExploreKind,
  parseExploreScope,
  type ExploreKind,
  type ExploreScope,
} from "./scope";

const LEVEL_COLOR: Record<Level, string> = {
  BEGINNER: "text-emerald-400 border-emerald-500/50",
  INTERMEDIATE: "text-amber-400 border-amber-500/50",
  ADVANCED: "text-red-400 border-red-500/50",
};

const TYPE_BADGE: Record<PublicationType, string> = {
  COMMUNITY: "text-sky-300 border-sky-500/40",
  OFFICIAL: "text-violet-300 border-violet-500/40",
};

function SegmentedControl<T extends string>({
  value,
  options,
  onChange,
}: {
  value: T;
  options: Array<{ value: T; label: string; icon?: React.ReactNode }>;
  onChange: (value: T) => void;
}) {
  return (
    <div className="inline-flex items-center rounded-lg border border-border bg-muted/40 p-1">
      {options.map((opt) => (
        <button
          key={opt.value}
          type="button"
          onClick={() => onChange(opt.value)}
          className={cn(
            "inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
            value === opt.value
              ? "bg-background text-foreground shadow-sm"
              : "text-muted-foreground hover:text-foreground"
          )}
        >
          {opt.icon}
          {opt.label}
        </button>
      ))}
    </div>
  );
}

function ExplorePublicationCard({
  publication,
  muscleGroupMap,
  onView,
}: {
  publication: ExercisePublicationResponse;
  muscleGroupMap: Map<number, MuscleGroupResponse>;
  onView: () => void;
}) {
  const { user } = useAuth();
  const ex = publication.exercise;
  const username =
    publication.authorUsername?.trim() ||
    (user?.id === publication.authorId ? user.username : "") ||
    "Unknown";
  const initial = username[0]?.toUpperCase() ?? "?";
  const primaryGroups = useMemo(() => {
    const seen = new Set<number>();
    const groups: MuscleGroupResponse[] = [];
    for (const m of ex.muscles) {
      if (m.role === "PRIMARY") {
        const g = muscleGroupMap.get(m.subGroup.groupId);
        if (g && !seen.has(g.id)) {
          seen.add(g.id);
          groups.push(g);
        }
      }
    }
    return groups.slice(0, 3);
  }, [ex.muscles, muscleGroupMap]);

  return (
    <div className="group rounded-xl border border-primary/40 bg-card overflow-hidden hover:border-primary transition-colors">
      <div
        className="relative aspect-square bg-muted flex items-center justify-center overflow-hidden cursor-pointer"
        onClick={onView}
      >
        {ex.thumbnail?.url ? (
          <img src={ex.thumbnail.url} alt={ex.title} className="w-full h-full object-cover" />
        ) : (
          <Dumbbell className="h-8 w-8 text-muted-foreground/40" />
        )}

        {primaryGroups.length > 0 && (
          <div className="absolute top-2 left-2 flex flex-col items-center gap-1">
            {primaryGroups.map((g) =>
              g.icon?.url ? (
                <img key={g.id} src={g.icon.url} alt={g.name} title={g.name} className="h-9 w-9 object-contain drop-shadow" />
              ) : (
                <Dumbbell key={g.id} className="h-9 w-9 text-white drop-shadow" />
              )
            )}
          </div>
        )}

        <div className="absolute bottom-2 right-2 flex flex-col items-end gap-1">
          <span className={cn("text-[10px] font-semibold px-2 py-0.5 rounded-full border-2 bg-zinc-800/90", TYPE_BADGE[publication.type])}>
            {publication.type[0] + publication.type.slice(1).toLowerCase()}
          </span>
          <span className={cn("text-[10px] font-semibold px-2 py-0.5 rounded-full border-2 bg-zinc-800/90", LEVEL_COLOR[ex.level])}>
            {ex.level[0] + ex.level.slice(1).toLowerCase()}
          </span>
        </div>
      </div>

      <div className="p-2.5">
        <div className="flex gap-2 items-start">
          <Avatar className="h-8 w-8 shrink-0 mt-0.5 cursor-pointer" onClick={onView}>
            {publication.authorAvatar?.url && (
              <AvatarImage src={publication.authorAvatar.url} alt={username} />
            )}
            <AvatarFallback className="text-[10px] font-semibold bg-primary/15 text-primary">
              {initial}
            </AvatarFallback>
          </Avatar>

          <div className="min-w-0 flex-1 space-y-0.5">
            <p
              className="text-sm font-medium leading-snug line-clamp-2 cursor-pointer"
              onClick={onView}
            >
              {ex.title}
            </p>
            <p className="text-xs text-muted-foreground truncate leading-snug">
              {username}
            </p>
            <p className="text-[10px] text-muted-foreground inline-flex items-center gap-0.5 truncate">
              <Eye className="h-3 w-3 shrink-0" />
              {publication.viewsCount}{" "}
              {publication.viewsCount === 1 ? "view" : "views"}
              {publication.publishedAt
                ? ` · ${formatTimeAgo(publication.publishedAt)}`
                : ""}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

function ComingSoonPanel({ label }: { label: string }) {
  return (
    <div className="text-center py-16 space-y-3 rounded-xl border border-border/60 bg-card/40">
      <p className="text-muted-foreground text-sm">
        {label} publications are coming soon.
      </p>
    </div>
  );
}

function ExploreExercisesFeed({ scope }: { scope: ExploreScope }) {
  const scopeLabel = exploreScopeLabel(scope);
  const publicationType = exploreScopeToType(scope);

  const [filter, setFilter] = useState<PublicationFilter>({});
  const [titleDraft, setTitleDraft] = useState("");
  const debouncedTitle = useDebouncedValue(titleDraft.trim(), 350);
  const [page, setPage] = useState(0);
  const [filterOpen, setFilterOpen] = useState(false);
  const [detail, setDetail] = useState<ExercisePublicationResponse | null>(null);

  useEffect(() => {
    setFilter((prev) => {
      const next = debouncedTitle || undefined;
      if (prev.title === next) return prev;
      return { ...prev, title: next };
    });
    setPage(0);
  }, [debouncedTitle]);

  useEffect(() => {
    setPage(0);
    setFilter({});
    setTitleDraft("");
    setFilterOpen(false);
    setDetail(null);
  }, [scope]);

  const { data: muscleGroups = [] } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: catalogApi.listMuscleGroups,
    staleTime: 60_000,
  });
  const muscleGroupMap = useMemo(() => new Map(muscleGroups.map((g) => [g.id, g])), [muscleGroups]);

  const muscleIds = filter.muscleGroupIds ?? [];
  const subIds = filter.muscleSubGroupIds ?? [];
  const panelFilters =
    (filter.equipmentIds?.length ?? 0) +
    (filter.activityIds?.length ?? 0) +
    (filter.trainingGoalIds?.length ?? 0);
  const hasAnyFilter =
    !!filter.title ||
    panelFilters > 0 ||
    muscleIds.length > 0 ||
    subIds.length > 0;

  const feedFilter: PublicationFilter = { ...filter, type: publicationType };

  const { data, isLoading, error } = useQuery({
    queryKey: ["publications", "feed", scope, feedFilter, page],
    queryFn: () => publicationApi.listFeed(feedFilter, page),
  });

  const { data: equipment = [] } = useQuery({
    queryKey: ["catalog", "equipment"],
    queryFn: catalogApi.listEquipment,
    staleTime: 60_000,
  });
  const { data: activities = [] } = useQuery({
    queryKey: ["catalog", "activities"],
    queryFn: catalogApi.listActivities,
    staleTime: 60_000,
  });
  const { data: trainingGoals = [] } = useQuery({
    queryKey: ["catalog", "training-goals"],
    queryFn: catalogApi.listTrainingGoals,
    staleTime: 60_000,
  });

  const toggleCatalog = (
    field: "equipmentIds" | "activityIds" | "trainingGoalIds",
    id: number
  ) => {
    setFilter((prev) => {
      const cur = prev[field] ?? [];
      const next = cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id];
      return { ...prev, [field]: next.length ? next : undefined };
    });
    setPage(0);
  };

  const chipClass = (active: boolean) =>
    cn(
      "px-2.5 py-1 rounded-full text-xs font-medium border transition-colors",
      active
        ? "bg-primary text-primary-foreground border-primary"
        : "text-muted-foreground border-border hover:border-primary/50"
    );

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 flex-wrap">
        <div className="relative flex-1 min-w-[160px] max-w-xs">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
          <Input
            value={titleDraft}
            onChange={(e) => setTitleDraft(e.target.value)}
            placeholder="Search by title…"
            className="h-8 pl-8 text-sm"
          />
        </div>
        <Button
          variant={filterOpen ? "default" : "outline"}
          size="sm"
          className="gap-1.5 h-8"
          onClick={() => setFilterOpen((v) => !v)}
        >
          <SlidersHorizontal className="h-3.5 w-3.5" /> Filters
          {panelFilters > 0 && (
            <span className="ml-0.5 bg-primary-foreground text-primary rounded-full text-[10px] font-bold w-4 h-4 flex items-center justify-center">
              {panelFilters}
            </span>
          )}
        </Button>
        <PublicationSortSelect
          value={filter.sort}
          onChange={(sort) => {
            setFilter((prev) => ({ ...prev, sort }));
            setPage(0);
          }}
        />
        {hasAnyFilter && (
          <Button
            variant="ghost"
            size="sm"
            className="gap-1 text-muted-foreground h-8"
            onClick={() => {
              setFilter((prev) => ({ sort: prev.sort }));
              setTitleDraft("");
              setPage(0);
            }}
          >
            <X className="h-3.5 w-3.5" /> Clear
          </Button>
        )}
      </div>

      <MuscleGroupFilterRow
        muscleGroups={muscleGroups}
        selectedGroupIds={muscleIds}
        selectedSubGroupIds={subIds}
        onGroupChange={(ids) => {
          setFilter((prev) => ({
            ...prev,
            muscleGroupIds: ids.length ? ids : undefined,
            muscleSubGroupIds: undefined,
          }));
          setPage(0);
        }}
        onSubGroupChange={(ids) => {
          setFilter((prev) => ({
            ...prev,
            muscleSubGroupIds: ids.length ? ids : undefined,
          }));
          setPage(0);
        }}
      />

      {filterOpen && (
        <div className="rounded-xl border border-primary/40 bg-card p-4 space-y-4">
          {(
            [
              { label: "Equipment", field: "equipmentIds" as const, items: equipment },
              { label: "Activities", field: "activityIds" as const, items: activities },
              { label: "Training Goals", field: "trainingGoalIds" as const, items: trainingGoals },
            ]
          ).map(({ label, field, items }) => (
            <div key={field} className="space-y-1.5">
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">{label}</p>
              <div className="flex flex-wrap gap-1.5">
                {items.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => toggleCatalog(field, item.id)}
                    className={chipClass((filter[field] ?? []).includes(item.id))}
                  >
                    {item.name}
                  </button>
                ))}
                {items.length === 0 && (
                  <span className="text-xs text-muted-foreground italic">No items</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {isLoading && <PageSpinner />}
      {error && <ApiError message={getErrorMessage(error)} />}

      {!isLoading && !error && (
        <>
          {!data?.content.length ? (
            <div className="text-center py-16 space-y-3">
              <Dumbbell className="h-10 w-10 text-muted-foreground/30 mx-auto" />
              <p className="text-muted-foreground text-sm">
                {hasAnyFilter
                  ? "No publications match the current filters."
                  : `No ${scopeLabel.toLowerCase()} exercise publications yet.`}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
              {data.content.map((pub) => (
                <ExplorePublicationCard
                  key={pub.id}
                  publication={pub}
                  muscleGroupMap={muscleGroupMap}
                  onView={() => setDetail(pub)}
                />
              ))}
            </div>
          )}

          {data && data.totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-2">
              <Button variant="outline" size="sm" disabled={data.first} onClick={() => setPage((p) => p - 1)}>Previous</Button>
              <span className="text-sm text-muted-foreground flex items-center px-2">{page + 1} / {data.totalPages}</span>
              <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage((p) => p + 1)}>Next</Button>
            </div>
          )}
        </>
      )}

      <ExerciseDetailDialog
        exercise={detail?.exercise ?? null}
        publication={detail}
        onClose={() => setDetail(null)}
      />
    </div>
  );
}

export function ExplorePage() {
  const [params, setParams] = useSearchParams();
  const scope = parseExploreScope(params.get("scope"));
  const kind = parseExploreKind(params.get("kind"));

  const setScope = (next: ExploreScope) => {
    const nextParams = new URLSearchParams(params);
    if (next === "official") nextParams.delete("scope");
    else nextParams.set("scope", next);
    setParams(nextParams, { replace: true });
  };

  const setKind = (next: ExploreKind) => {
    const nextParams = new URLSearchParams(params);
    if (next === "exercises") nextParams.delete("kind");
    else nextParams.set("kind", next);
    setParams(nextParams, { replace: true });
  };

  return (
    <div className="max-w-5xl mx-auto">
      <div className="mb-6 text-center space-y-4">
        <div>
          <h1 className="text-3xl font-bold">Explore</h1>
          <p className="text-muted-foreground mt-1">
            Browse official and community publications.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
          <SegmentedControl
            value={scope}
            onChange={setScope}
            options={[
              { value: "official", label: "Official", icon: <BadgeCheck className="h-3.5 w-3.5" /> },
              { value: "community", label: "Community", icon: <Users className="h-3.5 w-3.5" /> },
            ]}
          />
          <SegmentedControl
            value={kind}
            onChange={setKind}
            options={[
              { value: "exercises", label: "Exercises", icon: <Dumbbell className="h-3.5 w-3.5" /> },
              { value: "routines", label: "Routines", icon: <Layers className="h-3.5 w-3.5" /> },
              { value: "programs", label: "Programs", icon: <CalendarDays className="h-3.5 w-3.5" /> },
            ]}
          />
        </div>
      </div>

      {kind === "exercises" && <ExploreExercisesFeed scope={scope} />}
      {kind === "routines" && <ComingSoonPanel label="Routine" />}
      {kind === "programs" && <ComingSoonPanel label="Program" />}
    </div>
  );
}
