import { useState, useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient, keepPreviousData } from "@tanstack/react-query";
import { routineApi, displayDuration, type RoutineFilter } from "@/routine/api";
import { catalogApi } from "@/catalog/api";
import { RoutineDialog } from "@/routine/RoutineDialog";
import { RoutineDetailDialog } from "@/routine/RoutineDetailDialog";
import { MuscleGroupFilterRow } from "@/library/MuscleGroupFilterRow";
import { cn } from "@/lib/utils";
import { getErrorMessage } from "@/lib/axios";
import type { Level, MuscleGroupResponse, RoutineResponse } from "@/types/api";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/components/ApiError";
import { PageSpinner } from "@/components/Spinner";
import { ChevronLeft, Clock, Dumbbell, Layers, Pencil, Plus, SlidersHorizontal, Trash2, X } from "lucide-react";

// ── Level badge ───────────────────────────────────────────────────────────────

const LEVEL_COLOR: Record<Level, string> = {
  BEGINNER: "text-emerald-400 border-emerald-500/50",
  INTERMEDIATE: "text-amber-400 border-amber-500/50",
  ADVANCED: "text-red-400 border-red-500/50",
};

function LevelBadge({ level }: { level: Level }) {
  return (
    <span className={cn("text-[10px] font-bold px-1.5 py-0.5 rounded-full border-2 bg-zinc-800/90", LEVEL_COLOR[level])}>
      {level[0] + level.slice(1).toLowerCase()}
    </span>
  );
}

// ── Routine card ──────────────────────────────────────────────────────────────

function RoutineCard({
  routine,
  muscleGroupMap,
  onView,
  onEdit,
  onDelete,
}: {
  routine: RoutineResponse;
  muscleGroupMap: Map<number, MuscleGroupResponse>;
  onView: () => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const primaryGroups = useMemo(() => {
    const seen = new Set<number>();
    const groups: MuscleGroupResponse[] = [];
    for (const slot of routine.slots) {
      if (!slot.exercise) continue;
      for (const m of slot.exercise.muscles) {
        if (m.role === "PRIMARY") {
          const g = muscleGroupMap.get(m.subGroup.groupId);
          if (g && !seen.has(g.id)) {
            seen.add(g.id);
            groups.push(g);
          }
        }
      }
    }
    return groups.slice(0, 3);
  }, [routine.slots, muscleGroupMap]);

  return (
    <div className="group rounded-xl border border-primary/40 bg-card overflow-hidden hover:border-primary transition-colors">
      {/* Thumbnail */}
      <div
        className="relative aspect-square bg-muted flex items-center justify-center overflow-hidden cursor-pointer"
        onClick={onView}
      >
        {routine.thumbnail?.url ? (
          <img src={routine.thumbnail.url} alt={routine.title} className="w-full h-full object-cover" />
        ) : (
          <Layers className="h-8 w-8 text-muted-foreground/30" />
        )}

        {/* Primary muscle icons — top left */}
        {primaryGroups.length > 0 && (
          <div className="absolute top-2 left-2 flex flex-col items-center gap-1">
            {primaryGroups.map((g) =>
              g.icon?.url ? (
                <img key={g.id} src={g.icon.url} alt={g.name} title={g.name} className="h-9 w-9 object-contain drop-shadow" />
              ) : (
                <Dumbbell key={g.id} className="h-9 w-9 text-white drop-shadow" title={g.name} />
              )
            )}
          </div>
        )}

        {/* Level badge — bottom right */}
        <div className="absolute bottom-2 right-2">
          <LevelBadge level={routine.level} />
        </div>
      </div>

      {/* Info */}
      <div className="p-2.5 space-y-1.5">
        <div className="flex items-center justify-between gap-2">
          <h3
            className="font-semibold text-sm leading-tight line-clamp-2 flex-1 cursor-pointer hover:text-primary transition-colors"
            onClick={onView}
          >
            {routine.title}
          </h3>
          <div className="flex gap-1 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
            <button onClick={onEdit} className="p-1 rounded hover:bg-accent text-muted-foreground hover:text-foreground transition-colors">
              <Pencil className="h-3.5 w-3.5" />
            </button>
            <button onClick={onDelete} className="p-1 rounded text-muted-foreground hover:text-destructive transition-colors">
              <Trash2 className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>

        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <span>{routine.slots.length} exercises</span>
          {routine.estimatedDuration && (
            <>
              <span>·</span>
              <span className="flex items-center gap-0.5"><Clock className="h-3 w-3" />{displayDuration(routine.estimatedDuration)}</span>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Filter panel ──────────────────────────────────────────────────────────────

function FilterPanel({ filter, onChange }: { filter: RoutineFilter; onChange: (f: RoutineFilter) => void }) {
  const { data: equipment = [] } = useQuery({ queryKey: ["catalog", "equipment"], queryFn: catalogApi.listEquipment });
  const { data: activities = [] } = useQuery({ queryKey: ["catalog", "activities"], queryFn: catalogApi.listActivities });
  const { data: trainingGoals = [] } = useQuery({ queryKey: ["catalog", "training-goals"], queryFn: catalogApi.listTrainingGoals });

  const toggle = (field: "equipmentIds" | "activityIds" | "trainingGoalIds", id: number) => {
    const cur = filter[field] ?? [];
    onChange({ ...filter, [field]: cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id] });
  };

  const chipClass = (active: boolean) => cn(
    "px-2.5 py-1 rounded-full text-xs border transition-colors",
    active ? "border-primary bg-primary/10 text-primary" : "border-border hover:border-primary/40 text-muted-foreground"
  );

  return (
    <div className="rounded-xl border border-primary/40 bg-card p-4 space-y-4">
      {([
        { label: "Equipment", field: "equipmentIds" as const, items: equipment },
        { label: "Activities", field: "activityIds" as const, items: activities },
        { label: "Training Goals", field: "trainingGoalIds" as const, items: trainingGoals },
      ]).map(({ label, field, items }) => (
        <div key={field} className="space-y-1.5">
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">{label}</p>
          <div className="flex flex-wrap gap-1.5">
            {items.map((item) => (
              <button key={item.id} type="button" onClick={() => toggle(field, item.id)} className={chipClass((filter[field] ?? []).includes(item.id))}>
                {item.name}
              </button>
            ))}
            {items.length === 0 && <span className="text-xs text-muted-foreground italic">No items</span>}
          </div>
        </div>
      ))}
    </div>
  );
}

// ── Routines tab ──────────────────────────────────────────────────────────────

function RoutinesTab() {
  const qc = useQueryClient();
  const [filter, setFilter] = useState<RoutineFilter>({});
  const [page, setPage] = useState(0);
  const [filterOpen, setFilterOpen] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<RoutineResponse | null>(null);
  const [seedFrom, setSeedFrom] = useState<RoutineResponse | null>(null);
  const [detailRoutine, setDetailRoutine] = useState<RoutineResponse | null>(null);

  const { data: muscleGroups = [] } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: catalogApi.listMuscleGroups,
  });

  const muscleGroupMap = useMemo(() => new Map(muscleGroups.map((g) => [g.id, g])), [muscleGroups]);

  const muscleIds = filter.muscleGroupIds ?? [];
  const subIds = filter.muscleSubGroupIds ?? [];
  const panelFilters =
    (filter.equipmentIds?.length ?? 0) +
    (filter.activityIds?.length ?? 0) +
    (filter.trainingGoalIds?.length ?? 0);
  const hasAnyFilter = panelFilters > 0 || muscleIds.length > 0 || subIds.length > 0;

  // Don't send muscle ids to the API (backend has no muscle filter for routines)
  const { data, isLoading, error } = useQuery({
    queryKey: ["routines", { equipmentIds: filter.equipmentIds, activityIds: filter.activityIds, trainingGoalIds: filter.trainingGoalIds }, page],
    queryFn: () =>
      routineApi.list(
        {
          equipmentIds: filter.equipmentIds,
          activityIds: filter.activityIds,
          trainingGoalIds: filter.trainingGoalIds,
        },
        page
      ),
    placeholderData: keepPreviousData,
  });

  const routines = useMemo(() => {
    const list = data?.content ?? [];
    if (subIds.length > 0) {
      return list.filter((r) =>
        r.slots.some(
          (slot) =>
            slot.exercise != null &&
            slot.exercise.muscles.some((m) => subIds.includes(m.subGroupId))
        )
      );
    }
    if (muscleIds.length === 0) return list;
    return list.filter((r) =>
      muscleIds.some((id) => {
        const score = r.muscleSummary[String(id)];
        return score != null && score > 0;
      })
    );
  }, [data?.content, muscleIds, subIds]);

  const deleteMutation = useMutation({
    mutationFn: routineApi.delete,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["routines"] }),
  });

  const handleFilterChange = (f: RoutineFilter) => { setFilter(f); setPage(0); };

  const setMuscleGroupIds = (ids: number[]) => {
    setFilter((prev) => {
      const allowed = new Set(
        muscleGroups
          .filter((g) => ids.includes(g.id))
          .flatMap((g) => g.subGroups.map((s) => s.id))
      );
      const nextSubs = (prev.muscleSubGroupIds ?? []).filter((sid) => allowed.has(sid));
      return {
        ...prev,
        muscleGroupIds: ids.length ? ids : undefined,
        muscleSubGroupIds: nextSubs.length ? nextSubs : undefined,
      };
    });
    setPage(0);
  };

  const setMuscleSubGroupIds = (ids: number[]) => {
    setFilter((prev) => ({
      ...prev,
      muscleSubGroupIds: ids.length ? ids : undefined,
    }));
    setPage(0);
  };

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex items-center gap-2">
        <Button variant={filterOpen ? "default" : "outline"} size="sm" className="gap-1.5" onClick={() => setFilterOpen((v) => !v)}>
          <SlidersHorizontal className="h-4 w-4" />
          Filters
          {panelFilters > 0 && (
            <span className="ml-0.5 bg-primary-foreground text-primary rounded-full text-[10px] font-bold w-4 h-4 flex items-center justify-center">
              {panelFilters}
            </span>
          )}
        </Button>
        {hasAnyFilter && (
          <Button variant="ghost" size="sm" className="gap-1 text-muted-foreground" onClick={() => { setFilter({}); setPage(0); }}>
            <X className="h-3.5 w-3.5" /> Clear
          </Button>
        )}
        <div className="flex-1" />
        <Button size="sm" className="gap-1.5" onClick={() => { setEditing(null); setSeedFrom(null); setDialogOpen(true); }}>
          <Plus className="h-4 w-4" /> New Routine
        </Button>
      </div>

      <MuscleGroupFilterRow
        muscleGroups={muscleGroups}
        selectedGroupIds={muscleIds}
        selectedSubGroupIds={subIds}
        onGroupChange={setMuscleGroupIds}
        onSubGroupChange={setMuscleSubGroupIds}
      />

      {filterOpen && <FilterPanel filter={filter} onChange={handleFilterChange} />}

      {isLoading ? (
        <PageSpinner />
      ) : error ? (
        <ApiError message={getErrorMessage(error)} />
      ) : (
        <>
          {!routines.length ? (
            <div className="text-center py-16 text-muted-foreground">
              <Layers className="h-10 w-10 mx-auto mb-3 opacity-30" />
              <p className="text-sm">
                {hasAnyFilter
                  ? "No routines match the current filters."
                  : "No routines yet. Create your first one!"}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
              {routines.map((routine) => (
                <RoutineCard
                  key={routine.id}
                  routine={routine}
                  muscleGroupMap={muscleGroupMap}
                  onView={() => setDetailRoutine(routine)}
                  onEdit={() => { setSeedFrom(null); setEditing(routine); setDialogOpen(true); }}
                  onDelete={() => window.confirm(`Delete "${routine.title}"?`) && deleteMutation.mutate(routine.id)}
                />
              ))}
            </div>
          )}

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-2">
              <Button variant="outline" size="sm" disabled={data.first} onClick={() => setPage((p) => p - 1)}>Previous</Button>
              <span className="text-sm text-muted-foreground flex items-center px-2">{page + 1} / {data.totalPages}</span>
              <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage((p) => p + 1)}>Next</Button>
            </div>
          )}
        </>
      )}

      <RoutineDialog
        open={dialogOpen}
        editing={editing}
        seedFrom={seedFrom}
        onClose={() => { setDialogOpen(false); setSeedFrom(null); }}
      />
      <RoutineDetailDialog
        routine={detailRoutine}
        onClose={() => setDetailRoutine(null)}
        onEdit={() => {
          if (!detailRoutine) return;
          const r = detailRoutine;
          setDetailRoutine(null);
          setSeedFrom(null);
          setEditing(r);
          setDialogOpen(true);
        }}
        onDuplicate={() => {
          if (!detailRoutine) return;
          const r = detailRoutine;
          setDetailRoutine(null);
          setEditing(null);
          setSeedFrom(r);
          setDialogOpen(true);
        }}
        onDelete={() => {
          if (!detailRoutine) return;
          if (!window.confirm(`Delete "${detailRoutine.title}"?`)) return;
          deleteMutation.mutate(detailRoutine.id, {
            onSuccess: () => setDetailRoutine(null),
          });
        }}
      />
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function RoutinesPage() {
  const navigate = useNavigate();
  return (
    <div className="max-w-5xl mx-auto">
      <Link
        to="/library"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors mb-4"
      >
        <ChevronLeft className="h-5 w-5" /> My Library
      </Link>

      <div className="mb-6 text-center">
        <h1 className="text-3xl font-bold">My Routines</h1>
      </div>

      <Tabs defaultValue="routines">
        <div className="flex justify-center mb-6">
          <TabsList>
            <TabsTrigger value="exercises" className="gap-1.5" onMouseDown={() => navigate("/library/workouts")}>
              Exercises
            </TabsTrigger>
            <TabsTrigger value="routines" className="gap-1.5">
              Routines
            </TabsTrigger>
            <TabsTrigger value="programs" className="gap-1.5" onMouseDown={() => navigate("/library/programs")}>
              Programs
            </TabsTrigger>
          </TabsList>
        </div>
        <TabsContent value="routines" className="mt-6">
          <RoutinesTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}
