import { useState, useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { exerciseApi, type ExerciseFilter } from "@/exercise/api";
import { catalogApi } from "@/catalog/api";
import { ExerciseDialog } from "@/exercise/ExerciseDialog";
import { ExerciseDetailDialog } from "@/exercise/ExerciseDetailDialog";
import { cn } from "@/lib/utils";
import { getErrorMessage } from "@/lib/axios";
import type { ExerciseResponse, Level, MuscleGroupResponse } from "@/types/api";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/components/ApiError";
import { PageSpinner } from "@/components/Spinner";
import { ChevronLeft, Dumbbell, Pencil, Plus, SlidersHorizontal, Trash2, X } from "lucide-react";

// ── Level badge ───────────────────────────────────────────────────────────────

const LEVEL_COLOR: Record<Level, string> = {
  BEGINNER: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
  INTERMEDIATE: "bg-amber-500/20 text-amber-400 border-amber-500/30",
  ADVANCED: "bg-red-500/20 text-red-400 border-red-500/30",
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
        "text-[10px] font-semibold px-2 py-0.5 rounded-full border",
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
        className="relative aspect-video bg-muted flex items-center justify-center overflow-hidden cursor-pointer"
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
          <div className="absolute top-2 left-2 flex items-center gap-1">
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

      {/* Title + action buttons */}
      <div className="p-2.5">
        <div className="flex items-center justify-between gap-2">
          <h3
            className="font-semibold text-sm leading-tight line-clamp-2 flex-1 cursor-pointer hover:text-primary transition-colors"
            onClick={onView}
          >
            {exercise.title}
          </h3>
          <div className="flex gap-1 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
            <button
              onClick={onEdit}
              className="p-1 rounded hover:bg-accent text-muted-foreground hover:text-foreground transition-colors"
            >
              <Pencil className="h-3.5 w-3.5" />
            </button>
            <button
              onClick={onDelete}
              className="p-1 rounded text-muted-foreground hover:text-destructive transition-colors"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </button>
          </div>
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
  const { data: muscleGroups = [] } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: catalogApi.listMuscleGroups,
    staleTime: 60_000,
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

  const selectClass =
    "flex h-8 w-full rounded-md border border-input bg-background px-3 py-1 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring";

  return (
    <div className="rounded-xl border border-primary/40 bg-card p-4 space-y-4">
      <div className="space-y-1.5">
        <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
          Muscle Group
        </p>
        <select
          className={selectClass}
          value={filter.muscleGroupId ?? ""}
          onChange={(e) =>
            onChange({
              ...filter,
              muscleGroupId: e.target.value ? Number(e.target.value) : undefined,
              muscleSubGroupId: undefined,
            })
          }
        >
          <option value="">All muscles</option>
          {muscleGroups.map((g) => (
            <option key={g.id} value={g.id}>
              {g.name}
            </option>
          ))}
        </select>
      </div>

      {/* Subgroup selector — only when a group is selected */}
      {filter.muscleGroupId && (() => {
        const subGroups = muscleGroups.find((g) => g.id === filter.muscleGroupId)?.subGroups ?? [];
        if (subGroups.length === 0) return null;
        return (
          <div className="space-y-1.5">
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
              Muscle Subgroup
            </p>
            <select
              className={selectClass}
              value={filter.muscleSubGroupId ?? ""}
              onChange={(e) =>
                onChange({
                  ...filter,
                  muscleSubGroupId: e.target.value ? Number(e.target.value) : undefined,
                })
              }
            >
              <option value="">All subgroups</option>
              {subGroups.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          </div>
        );
      })()}

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
  const [page, setPage] = useState(0);
  const [filterOpen, setFilterOpen] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<ExerciseResponse | null>(null);
  const [detailExercise, setDetailExercise] = useState<ExerciseResponse | null>(null);

  const { data: muscleGroups = [] } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: catalogApi.listMuscleGroups,
    staleTime: 60_000,
  });

  const activeFilters =
    (filter.muscleGroupId ? 1 : 0) +
    (filter.muscleSubGroupId ? 1 : 0) +
    (filter.equipmentIds?.length ?? 0) +
    (filter.activityIds?.length ?? 0) +
    (filter.trainingGoalIds?.length ?? 0);

  const muscleGroupMap = useMemo(
    () => new Map(muscleGroups.map((g) => [g.id, g])),
    [muscleGroups]
  );

  const { data, isLoading, error } = useQuery({
    queryKey: ["exercises", filter, page],
    queryFn: () => exerciseApi.list(filter, page),
  });

  const deleteMutation = useMutation({
    mutationFn: exerciseApi.delete,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["exercises"] }),
  });

  const handleFilterChange = (f: ExerciseFilter) => {
    setFilter(f);
    setPage(0);
  };

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex items-center gap-2">
        <Button
          variant={filterOpen ? "default" : "outline"}
          size="sm"
          className="gap-1.5"
          onClick={() => setFilterOpen((v) => !v)}
        >
          <SlidersHorizontal className="h-4 w-4" />
          Filters
          {activeFilters > 0 && (
            <span className="ml-0.5 bg-primary-foreground text-primary rounded-full text-[10px] font-bold w-4 h-4 flex items-center justify-center">
              {activeFilters}
            </span>
          )}
        </Button>

        {activeFilters > 0 && (
          <Button
            variant="ghost"
            size="sm"
            className="gap-1 text-muted-foreground"
            onClick={() => { setFilter({}); setPage(0); }}
          >
            <X className="h-3.5 w-3.5" /> Clear
          </Button>
        )}

        <div className="flex-1" />

        <Button
          size="sm"
          className="gap-1.5"
          onClick={() => { setEditing(null); setDialogOpen(true); }}
        >
          <Plus className="h-4 w-4" /> New exercise
        </Button>
      </div>

      {filterOpen && <FilterPanel filter={filter} onChange={handleFilterChange} />}

      {isLoading && <PageSpinner />}
      {error && <ApiError message={getErrorMessage(error)} />}

      {!isLoading && !error && (
        <>
          {data?.content.length === 0 ? (
            <div className="text-center py-16 space-y-3">
              <Dumbbell className="h-10 w-10 text-muted-foreground/30 mx-auto" />
              <p className="text-muted-foreground">
                {activeFilters > 0
                  ? "No exercises match the current filters."
                  : "No exercises yet. Create your first one!"}
              </p>
              {activeFilters === 0 && (
                <Button size="sm" onClick={() => { setEditing(null); setDialogOpen(true); }}>
                  <Plus className="h-4 w-4" /> New exercise
                </Button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {data?.content.map((exercise) => (
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
        <p className="text-muted-foreground mt-1">
          Exercises · Routines · Programs (soon)
        </p>
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
            <TabsTrigger value="programs" disabled className="gap-1.5 opacity-40">
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
