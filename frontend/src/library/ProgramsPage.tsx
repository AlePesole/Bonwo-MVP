import { useState, useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient, keepPreviousData } from "@tanstack/react-query";
import { programApi, type ProgramFilter } from "@/program/api";
import { catalogApi } from "@/catalog/api";
import { ProgramDialog } from "@/program/ProgramDialog";
import { ProgramDetailDialog } from "@/program/ProgramDetailDialog";
import { cn } from "@/lib/utils";
import { getErrorMessage } from "@/lib/axios";
import type { Level, MuscleGroupResponse, TrainingProgramResponse } from "@/types/api";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/components/ApiError";
import { PageSpinner } from "@/components/Spinner";
import { CalendarDays, ChevronLeft, Dumbbell, Layers, Pencil, Plus, SlidersHorizontal, Trash2, X } from "lucide-react";

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

// ── Program card ──────────────────────────────────────────────────────────────

function ProgramCard({
  program,
  muscleGroupMap,
  onView,
  onEdit,
  onDelete,
}: {
  program: TrainingProgramResponse;
  muscleGroupMap: Map<number, MuscleGroupResponse>;
  onView: () => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  // Primary muscle groups from all routines' slots
  const primaryGroups = useMemo(() => {
    const seen = new Set<number>();
    const groups: MuscleGroupResponse[] = [];
    for (const routine of program.routines) {
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
    }
    return groups.slice(0, 3);
  }, [program.routines, muscleGroupMap]);

  return (
    <div className="group rounded-xl border border-primary/40 bg-card overflow-hidden hover:border-primary transition-colors">
      {/* Thumbnail */}
      <div
        className="relative aspect-square bg-muted flex items-center justify-center overflow-hidden cursor-pointer"
        onClick={onView}
      >
        {program.thumbnail?.url ? (
          <img src={program.thumbnail.url} alt={program.title} className="w-full h-full object-cover" />
        ) : (
          <CalendarDays className="h-8 w-8 text-muted-foreground/30" />
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
          <LevelBadge level={program.level} />
        </div>
      </div>

      {/* Info */}
      <div className="p-2.5 space-y-1.5">
        <div className="flex items-center justify-between gap-2">
          <h3
            className="font-semibold text-sm leading-tight line-clamp-2 flex-1 cursor-pointer hover:text-primary transition-colors"
            onClick={onView}
          >
            {program.title}
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
          <span>{program.routines.length} routine{program.routines.length !== 1 ? "s" : ""}</span>
          <span>·</span>
          <span className="flex items-center gap-0.5">
            <CalendarDays className="h-3 w-3" />{program.daysPerWeek}d/week
          </span>
        </div>
      </div>
    </div>
  );
}

// ── Filter panel ──────────────────────────────────────────────────────────────

function FilterPanel({ filter, onChange }: { filter: ProgramFilter; onChange: (f: ProgramFilter) => void }) {
  const { data: equipment = [] } = useQuery({ queryKey: ["catalog", "equipment"], queryFn: catalogApi.listEquipment });
  const { data: activities = [] } = useQuery({ queryKey: ["catalog", "activities"], queryFn: catalogApi.listActivities });
  const { data: trainingGoals = [] } = useQuery({ queryKey: ["catalog", "training-goals"], queryFn: catalogApi.listTrainingGoals });

  const toggle = (field: "equipmentIds" | "activityIds" | "trainingGoalIds", id: number) => {
    const cur = filter[field] ?? [];
    onChange({ ...filter, [field]: cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id] });
  };

  const chipClass = (active: boolean) => cn(
    "px-2.5 py-1 rounded-full text-xs border transition-colors",
    active ? "bg-primary/10 border-primary text-primary" : "border-border text-muted-foreground hover:border-primary/50"
  );

  return (
    <div className="rounded-xl border border-primary/40 bg-card/50 p-4 space-y-3 mb-4">
      {equipment.length > 0 && (
        <div className="space-y-1.5">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Equipment</p>
          <div className="flex flex-wrap gap-1.5">
            {equipment.map((e) => (
              <button key={e.id} onClick={() => toggle("equipmentIds", e.id)} className={chipClass((filter.equipmentIds ?? []).includes(e.id))}>
                {e.name}
              </button>
            ))}
          </div>
        </div>
      )}
      {activities.length > 0 && (
        <div className="space-y-1.5">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Activities</p>
          <div className="flex flex-wrap gap-1.5">
            {activities.map((a) => (
              <button key={a.id} onClick={() => toggle("activityIds", a.id)} className={chipClass((filter.activityIds ?? []).includes(a.id))}>
                {a.name}
              </button>
            ))}
          </div>
        </div>
      )}
      {trainingGoals.length > 0 && (
        <div className="space-y-1.5">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Training Goals</p>
          <div className="flex flex-wrap gap-1.5">
            {trainingGoals.map((g) => (
              <button key={g.id} onClick={() => toggle("trainingGoalIds", g.id)} className={chipClass((filter.trainingGoalIds ?? []).includes(g.id))}>
                {g.name}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ── Programs tab ──────────────────────────────────────────────────────────────

function ProgramsTab() {
  const qc = useQueryClient();
  const [filter, setFilter] = useState<ProgramFilter>({});
  const [page, setPage] = useState(0);
  const [filterOpen, setFilterOpen] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<TrainingProgramResponse | null>(null);
  const [seedFrom, setSeedFrom] = useState<TrainingProgramResponse | null>(null);
  const [detailProgram, setDetailProgram] = useState<TrainingProgramResponse | null>(null);

  const { data: muscleGroups = [] } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: catalogApi.listMuscleGroups,
  });

  const muscleGroupMap = useMemo(() => new Map(muscleGroups.map((g) => [g.id, g])), [muscleGroups]);

  const handleFilterChange = (f: ProgramFilter) => { setFilter(f); setPage(0); };

  const activeFilters =
    (filter.equipmentIds?.length ?? 0) +
    (filter.activityIds?.length ?? 0) +
    (filter.trainingGoalIds?.length ?? 0);

  const { data, isLoading, error } = useQuery({
    queryKey: ["programs", filter, page],
    queryFn: () => programApi.list(filter, page),
    placeholderData: keepPreviousData,
  });

  const deleteMutation = useMutation({
    mutationFn: programApi.delete,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["programs"] }),
  });

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex items-center gap-2 flex-wrap">
        <Button
          variant="outline"
          size="sm"
          className="gap-1.5"
          onClick={() => setFilterOpen((v) => !v)}
        >
          <SlidersHorizontal className="h-3.5 w-3.5" />
          Filters
          {activeFilters > 0 && (
            <span className="ml-0.5 bg-primary-foreground text-primary rounded-full text-[10px] font-bold w-4 h-4 flex items-center justify-center">
              {activeFilters}
            </span>
          )}
        </Button>
        {activeFilters > 0 && (
          <Button variant="ghost" size="sm" className="gap-1 text-muted-foreground" onClick={() => { setFilter({}); setPage(0); }}>
            <X className="h-3.5 w-3.5" /> Clear
          </Button>
        )}
        <div className="flex-1" />
        <Button size="sm" className="gap-1.5" onClick={() => { setEditing(null); setSeedFrom(null); setDialogOpen(true); }}>
          <Plus className="h-4 w-4" /> New Program
        </Button>
      </div>

      {filterOpen && <FilterPanel filter={filter} onChange={handleFilterChange} />}

      {isLoading ? (
        <PageSpinner />
      ) : error ? (
        <ApiError message={getErrorMessage(error)} />
      ) : (
        <>
          {!data?.content.length ? (
            <div className="text-center py-16 text-muted-foreground">
              <CalendarDays className="h-10 w-10 mx-auto mb-3 opacity-30" />
              <p className="text-sm">No programs yet. Create your first one!</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
              {data.content.map((program) => (
                <ProgramCard
                  key={program.id}
                  program={program}
                  muscleGroupMap={muscleGroupMap}
                  onView={() => setDetailProgram(program)}
                  onEdit={() => { setSeedFrom(null); setEditing(program); setDialogOpen(true); }}
                  onDelete={() => window.confirm(`Delete "${program.title}"?`) && deleteMutation.mutate(program.id)}
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

      <ProgramDialog
        open={dialogOpen}
        editing={editing}
        seedFrom={seedFrom}
        onClose={() => { setDialogOpen(false); setSeedFrom(null); }}
      />
      <ProgramDetailDialog
        program={detailProgram}
        onClose={() => setDetailProgram(null)}
        onEdit={() => {
          if (!detailProgram) return;
          const p = detailProgram;
          setDetailProgram(null);
          setSeedFrom(null);
          setEditing(p);
          setDialogOpen(true);
        }}
        onDuplicate={() => {
          if (!detailProgram) return;
          const p = detailProgram;
          setDetailProgram(null);
          setEditing(null);
          setSeedFrom(p);
          setDialogOpen(true);
        }}
        onDelete={() => {
          if (!detailProgram) return;
          if (!window.confirm(`Delete "${detailProgram.title}"?`)) return;
          deleteMutation.mutate(detailProgram.id, {
            onSuccess: () => setDetailProgram(null),
          });
        }}
      />
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function ProgramsPage() {
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
        <h1 className="text-3xl font-bold">My Programs</h1>
      </div>

      <Tabs defaultValue="programs">
        <div className="flex justify-center mb-6">
          <TabsList>
            <TabsTrigger value="exercises" className="gap-1.5" onMouseDown={() => navigate("/library/workouts")}>
              Exercises
            </TabsTrigger>
            <TabsTrigger value="routines" className="gap-1.5" onMouseDown={() => navigate("/library/routines")}>
              Routines
            </TabsTrigger>
            <TabsTrigger value="programs" className="gap-1.5">
              Programs
            </TabsTrigger>
          </TabsList>
        </div>
        <TabsContent value="programs" className="mt-6">
          <ProgramsTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}
