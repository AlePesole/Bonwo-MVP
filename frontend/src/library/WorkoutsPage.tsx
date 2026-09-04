import { useState, useMemo, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient, keepPreviousData } from "@tanstack/react-query";
import { exerciseApi, type ExerciseFilter } from "@/exercise/api";
import { catalogApi } from "@/catalog/api";
import { ExerciseDialog } from "@/exercise/ExerciseDialog";
import { ExerciseDetailDialog } from "@/exercise/ExerciseDetailDialog";
import { MuscleGroupFilterRow } from "@/library/MuscleGroupFilterRow";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { cn } from "@/lib/utils";
import { getErrorMessage } from "@/lib/axios";
import type { ExerciseResponse, Level, MuscleGroupResponse } from "@/types/api";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ApiError } from "@/components/ApiError";
import { PageSpinner } from "@/components/Spinner";
import { ChevronLeft, Dumbbell, MoreVertical, Pencil, Plus, Search, SlidersHorizontal, Trash2, X } from "lucide-react";

// ── Level badge ───────────────────────────────────────────────────────────────

const LEVEL_COLOR: Record<Level, string> = {
  BEGINNER: "text-emerald-400 border-emerald-500/50",
  INTERMEDIATE: "text-amber-400 border-amber-500/50",
  ADVANCED: "text-red-400 border-red-500/50",
};

const LEVEL_LABEL: Record<Level, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

function LevelBadge({ level }: { level: Level }) {
  return (
    <span
      className={cn(
        "text-[10px] font-semibold px-2 py-0.5 rounded-full border-2 bg-zinc-800/90",
        LEVEL_COLOR[level]
      )}
    >
      {LEVEL_LABEL[level]}
    </span>
  );
}

// ── Exercise card ─────────────────────────────────────────────────────────────

function ExerciseCard({
  exercise,
  muscleGroupMap,
  onView,
  onEdit,
  onDelete,
}: {
  exercise: ExerciseResponse;
  muscleGroupMap: Map<number, MuscleGroupResponse>;
  onView: () => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const primaryGroups = useMemo(() => {
    const seen = new Set<number>();
    const groups: MuscleGroupResponse[] = [];
    for (const m of exercise.muscles) {
      if (m.role === "PRIMARY") {
        const g = muscleGroupMap.get(m.subGroup.groupId);
        if (g && !seen.has(g.id)) {
          seen.add(g.id);
          groups.push(g);
        }
      }
    }
    return groups;
  }, [exercise.muscles, muscleGroupMap]);

  return (
    <div className="group rounded-xl border border-primary/40 bg-card overflow-hidden hover:border-primary transition-colors">
      {/* Thumbnail with overlays — click to view detail */}
      <div
        className="relative aspect-square bg-muted flex items-center justify-center overflow-hidden cursor-pointer"
        onClick={onView}
      >
        {exercise.thumbnail ? (
          <img
            src={exercise.thumbnail.url}
            alt={exercise.title}
            className="w-full h-full object-cover"
          />
        ) : (
          <Dumbbell className="h-8 w-8 text-muted-foreground/40" />
        )}

        {/* Primary muscle group icons — top left */}
        {primaryGroups.length > 0 && (
          <div className="absolute top-2 left-2 flex flex-col items-center gap-1">
            {primaryGroups.map((g) =>
              g.icon?.url ? (
                <img
                  key={g.id}
                  src={g.icon.url}
                  alt={g.name}
                  title={g.name}
                  className="h-9 w-9 object-contain drop-shadow"
                />
              ) : (
                <Dumbbell key={g.id} className="h-9 w-9 text-white drop-shadow" title={g.name} />
              )
            )}
          </div>
        )}

        {/* Level badge — bottom right */}
        <div className="absolute bottom-2 right-2">
          <LevelBadge level={exercise.level} />
        </div>
      </div>

      {/* Title + actions */}
      <div className="p-2.5">
        <div className="flex items-start justify-between gap-1">
          <h3
            className="font-semibold text-sm leading-tight line-clamp-2 flex-1 cursor-pointer hover:text-primary transition-colors"
            onClick={onView}
          >
            {exercise.title}
          </h3>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7 shrink-0 -mr-1 text-muted-foreground hover:text-foreground"
                onClick={(e) => e.stopPropagation()}
              >
                <MoreVertical className="h-4 w-4" />
                <span className="sr-only">Actions</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-44">
              <DropdownMenuItem onClick={onEdit}>
                <Pencil className="h-4 w-4 mr-2" /> Edit
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                className="text-destructive focus:text-destructive"
                onClick={onDelete}
              >
                <Trash2 className="h-4 w-4 mr-2" /> Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </div>
  );
}

// ── Filter panel ──────────────────────────────────────────────────────────────

const chipClass = (active: boolean) =>
  cn(
    "px-2.5 py-1 rounded-full text-xs font-medium border transition-colors",
    active
      ? "bg-primary text-primary-foreground border-primary"
      : "text-muted-foreground border-border hover:border-primary/50"
  );

function FilterPanel({
  filter,
  onChange,
}: {
  filter: ExerciseFilter;
  onChange: (f: ExerciseFilter) => void;
}) {
  const { data: equipment = [] } = useQuery({
    queryKey: ["catalog", "equipment"],
    queryFn: catalogApi.listEquipment,
  });
  const { data: activities = [] } = useQuery({
    queryKey: ["catalog", "activities"],
    queryFn: catalogApi.listActivities,
  });
  const { data: trainingGoals = [] } = useQuery({
    queryKey: ["catalog", "training-goals"],
    queryFn: catalogApi.listTrainingGoals,
  });

  const toggle = (
    field: "equipmentIds" | "activityIds" | "trainingGoalIds",
    id: number
  ) => {
    const cur = filter[field] ?? [];
    onChange({
      ...filter,
      [field]: cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id],
    });
  };

  return (
    <div className="rounded-xl border border-primary/40 bg-card p-4 space-y-4">
      {(
        [
          { label: "Equipment", field: "equipmentIds", items: equipment },
          { label: "Activities", field: "activityIds", items: activities },
          { label: "Training Goals", field: "trainingGoalIds", items: trainingGoals },
        ] as const
      ).map(({ label, field, items }) => (
        <div key={field} className="space-y-1.5">
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
            {label}
          </p>
          <div className="flex flex-wrap gap-1.5">
            {items.map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => toggle(field, item.id)}
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
  );
}

// ── Exercises tab ─────────────────────────────────────────────────────────────

function ExercisesTab() {
  const qc = useQueryClient();
  const [filter, setFilter] = useState<ExerciseFilter>({});
  const [titleDraft, setTitleDraft] = useState("");
  const debouncedTitle = useDebouncedValue(titleDraft.trim(), 350);
  const [page, setPage] = useState(0);
  const [filterOpen, setFilterOpen] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<ExerciseResponse | null>(null);
  const [detailExercise, setDetailExercise] = useState<ExerciseResponse | null>(null);

  useEffect(() => {
    setFilter((prev) => {
      const next = debouncedTitle || undefined;
      if (prev.title === next) return prev;
      setPage(0);
      return { ...prev, title: next };
    });
  }, [debouncedTitle]);

  const { data: muscleGroups = [] } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: catalogApi.listMuscleGroups,
  });

  const muscleIds = filter.muscleGroupIds ?? [];
  const subIds = filter.muscleSubGroupIds ?? [];
  const panelFilters =
    (filter.equipmentIds?.length ?? 0) +
    (filter.activityIds?.length ?? 0) +
    (filter.trainingGoalIds?.length ?? 0);
  const hasAnyFilter = panelFilters > 0 || muscleIds.length > 0 || subIds.length > 0 || !!filter.title;

  const muscleGroupMap = useMemo(
    () => new Map(muscleGroups.map((g) => [g.id, g])),
    [muscleGroups]
  );

  const { data, isLoading, error } = useQuery({
    queryKey: ["exercises", filter, page],
    queryFn: () => exerciseApi.list(filter, page),
    placeholderData: keepPreviousData,
  });

  // Client-side filter when multi-select exceeds what the backend supports (single id).
  const exercises = useMemo(() => {
    const list = data?.content ?? [];
    if (subIds.length > 1) {
      return list.filter((ex) =>
        ex.muscles.some((m) => subIds.includes(m.subGroupId))
      );
    }
    if (subIds.length === 1) return list; // already filtered by API
    if (muscleIds.length > 1) {
      return list.filter((ex) =>
        ex.muscles.some((m) => muscleIds.includes(m.subGroup.groupId))
      );
    }
    return list;
  }, [data?.content, muscleIds, subIds]);

  const deleteMutation = useMutation({
    mutationFn: exerciseApi.delete,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["exercises"] }),
  });

  const handleFilterChange = (f: ExerciseFilter) => {
    setFilter(f);
    setPage(0);
  };

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
      <div className="flex items-center gap-2 flex-wrap">
        <div className="relative flex-1 min-w-[160px] max-w-xs">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
          <Input
            value={titleDraft}
            onChange={(e) => setTitleDraft(e.target.value)}
            placeholder="Search by title…"
            aria-label="Search by title"
            className="h-8 pl-8 text-sm"
          />
        </div>
        <Button
          variant={filterOpen ? "default" : "outline"}
          size="sm"
          className="gap-1.5 h-8"
          onClick={() => setFilterOpen((v) => !v)}
        >
          <SlidersHorizontal className="h-4 w-4" />
          Filters
          {panelFilters > 0 && (
            <span className="ml-0.5 bg-primary-foreground text-primary rounded-full text-[10px] font-bold w-4 h-4 flex items-center justify-center">
              {panelFilters}
            </span>
          )}
        </Button>

        {hasAnyFilter && (
          <Button
            variant="ghost"
            size="sm"
            className="gap-1 text-muted-foreground h-8"
            onClick={() => { setFilter({}); setTitleDraft(""); setPage(0); }}
          >
            <X className="h-3.5 w-3.5" /> Clear
          </Button>
        )}

        <div className="flex-1" />

        <Button
          size="sm"
          className="gap-1.5 h-8"
          onClick={() => { setEditing(null); setDialogOpen(true); }}
        >
          <Plus className="h-4 w-4" /> New exercise
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

      {isLoading && <PageSpinner />}
      {error && <ApiError message={getErrorMessage(error)} />}

      {!isLoading && !error && (
        <>
          {exercises.length === 0 ? (
            <div className="text-center py-16 space-y-3">
              <Dumbbell className="h-10 w-10 text-muted-foreground/30 mx-auto" />
              <p className="text-muted-foreground">
                {hasAnyFilter
                  ? "No exercises match the current filters."
                  : "No exercises yet. Create your first one!"}
              </p>
              {!hasAnyFilter && (
                <Button size="sm" onClick={() => { setEditing(null); setDialogOpen(true); }}>
                  <Plus className="h-4 w-4" /> New exercise
                </Button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
              {exercises.map((exercise) => (
                <ExerciseCard
                  key={exercise.id}
                  exercise={exercise}
                  muscleGroupMap={muscleGroupMap}
                  onView={() => setDetailExercise(exercise)}
                  onEdit={() => { setEditing(exercise); setDialogOpen(true); }}
                  onDelete={() =>
                    window.confirm(`Delete "${exercise.title}"?`) &&
                    deleteMutation.mutate(exercise.id)
                  }
                />
              ))}
            </div>
          )}

          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-center gap-3 pt-2">
              <Button
                variant="outline"
                size="sm"
                disabled={data.first}
                onClick={() => setPage((p) => p - 1)}
              >
                Previous
              </Button>
              <span className="text-sm text-muted-foreground">
                {data.number + 1} / {data.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={data.last}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </>
      )}

      <ExerciseDialog
        open={dialogOpen}
        editing={editing}
        onClose={() => setDialogOpen(false)}
      />

      <ExerciseDetailDialog
        exercise={detailExercise}
        onClose={() => setDetailExercise(null)}
        onEdit={() => {
          if (!detailExercise) return;
          const ex = detailExercise;
          setDetailExercise(null);
          setEditing(ex);
          setDialogOpen(true);
        }}
        onDelete={() => {
          if (!detailExercise) return;
          if (!window.confirm(`Delete "${detailExercise.title}"?`)) return;
          deleteMutation.mutate(detailExercise.id, {
            onSuccess: () => setDetailExercise(null),
          });
        }}
      />
    </div>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────

export function WorkoutsPage() {
  const navigate = useNavigate();
  return (
    <div className="max-w-5xl mx-auto">
      <Link
        to="/library"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors mb-4"
      >
        <ChevronLeft className="h-4 w-4" /> My Library
      </Link>

      <div className="mb-6 text-center">
        <h1 className="text-3xl font-bold">My Exercises</h1>
      </div>

      <Tabs defaultValue="exercises">
        <div className="flex justify-center mb-6">
          <TabsList>
            <TabsTrigger value="exercises" className="gap-1.5">
              <Dumbbell className="h-4 w-4" /> Exercises
            </TabsTrigger>
            <TabsTrigger value="routines" className="gap-1.5" onMouseDown={() => navigate("/library/routines")}>
              Routines
            </TabsTrigger>
            <TabsTrigger value="programs" className="gap-1.5" onMouseDown={() => navigate("/library/programs")}>
              Programs
            </TabsTrigger>
          </TabsList>
        </div>

        <TabsContent value="exercises">
          <ExercisesTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}
