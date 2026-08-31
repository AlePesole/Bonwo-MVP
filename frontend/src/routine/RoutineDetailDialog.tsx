import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { catalogApi } from "@/catalog/api";
import { BODY_DIAGRAMS } from "@/catalog/bodyDiagram";
import { displayDuration, routineApi, type RoutinePayload } from "./api";
import { programApi } from "@/program/api";
import { cn } from "@/lib/utils";
import { getErrorMessage } from "@/lib/axios";
import type {
  ActivationLevel,
  ExerciseSlotResponse,
  MuscleGroupResponse,
  MuscleSubGroupResponse,
  ProgramRoutineDto,
  RoutineResponse,
  SetConfigResponse,
  SetType,
  TrainingProgramResponse,
} from "@/types/api";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ApiError } from "@/components/ApiError";
import { Spinner } from "@/components/Spinner";
import {
  ChevronDown,
  Copy,
  Dumbbell,
  Layers,
  Clock,
  AlertTriangle,
  Eye,
  MoreVertical,
  Pencil,
  Trash2,
  FolderPlus,
} from "lucide-react";
import { ExerciseDetailDialog } from "@/exercise/ExerciseDetailDialog";
import type { ExerciseResponse as ExerciseResponseType } from "@/types/api";

// ── Helpers ───────────────────────────────────────────────────────────────────

const LEVEL_COLOR: Record<string, string> = {
  BEGINNER: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
  INTERMEDIATE: "bg-amber-500/20 text-amber-400 border-amber-500/30",
  ADVANCED: "bg-red-500/20 text-red-400 border-red-500/30",
};
const LEVEL_LABEL: Record<string, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

const SET_LABEL: Record<SetType, string> = {
  REPS: "Reps",
  TIMED: "Timed",
  AMRAP: "AMRAP",
  FAILURE: "To failure",
};

function setDescription(set: SetConfigResponse): string {
  const parts: string[] = [];
  if (set.type === "REPS" || set.type === "AMRAP" || set.type === "FAILURE") {
    if (set.reps) parts.push(`${set.reps} reps`);
  }
  if (set.type === "TIMED" || set.type === "AMRAP") {
    if (set.duration) parts.push(displayDuration(set.duration));
  }
  if (set.weightKg && set.weightMode === "PER_SIDE") {
    parts.push(`${set.weightKg}kg x2`);
  } else if (set.totalWeightKg) {
    parts.push(`${set.totalWeightKg}kg`);
  }
  return parts.join(" · ") || SET_LABEL[set.type];
}

// ── Catalog section ───────────────────────────────────────────────────────────

function CatalogItem({ name, iconUrl }: { name: string; iconUrl?: string | null }) {
  return (
    <div className="flex flex-col items-center gap-1 text-center">
      <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center">
        {iconUrl ? <img src={iconUrl} alt={name} className="h-7 w-7 object-contain" /> : <Layers className="h-5 w-5 text-muted-foreground" />}
      </div>
      <span className="text-[10px] text-muted-foreground leading-tight line-clamp-2">{name}</span>
    </div>
  );
}

function CatalogSection({ label, items }: { label: string; items: { id: number; name: string; icon?: { url: string } | null }[] }) {
  const [popoverOpen, setPopoverOpen] = useState(false);
  if (items.length === 0) return null;
  const visible = items.slice(0, 3);
  const extra = items.slice(3);
  return (
    <div className="space-y-1.5">
      <p className="text-xs font-semibold text-foreground">{label}</p>
      <div className="grid grid-cols-4 gap-2 items-start">
        {visible.map((item) => <CatalogItem key={item.id} name={item.name} iconUrl={item.icon?.url} />)}
        {extra.length > 0 && (
          <div className="relative">
            <button type="button" onClick={() => setPopoverOpen((v) => !v)} className="flex flex-col items-center gap-1 text-center">
              <div className="h-10 w-10 rounded-lg bg-muted hover:bg-accent transition-colors flex items-center justify-center">
                <span className="text-xs font-semibold text-muted-foreground">+{extra.length}</span>
              </div>
              <span className="text-[10px] text-muted-foreground leading-tight">more</span>
            </button>
            {popoverOpen && (
              <>
                <div className="fixed inset-0 z-30" onClick={() => setPopoverOpen(false)} />
                <div className="absolute right-0 top-full mt-1 z-40 bg-popover border border-border rounded-xl p-3 shadow-lg flex flex-wrap gap-2 w-max max-w-[280px]">
                  {extra.map((item) => <CatalogItem key={item.id} name={item.name} iconUrl={item.icon?.url} />)}
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// ── Slot list ─────────────────────────────────────────────────────────────────

function SlotListItem({ slot, onView }: { slot: ExerciseSlotResponse; onView: (ex: ExerciseSlotResponse["exercise"]) => void }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="rounded-lg border border-primary/40 bg-card/60 overflow-hidden hover:border-primary transition-colors">
      <div className="flex items-center gap-3 p-3">
        <button
          type="button"
          className="flex items-center gap-3 flex-1 hover:bg-accent/20 transition-colors text-left rounded min-w-0"
          onClick={() => setExpanded((v) => !v)}
        >
          <span className="text-xs text-muted-foreground w-5 shrink-0 text-right">{slot.position}.</span>
          {slot.exercise ? (
            <>
              <div className="h-12 w-12 rounded-md bg-muted overflow-hidden shrink-0">
                {slot.exercise.thumbnail?.url ? (
                  <img src={slot.exercise.thumbnail.url} className="h-full w-full object-cover" alt="" />
                ) : (
                  <Dumbbell className="h-5 w-5 text-muted-foreground m-3.5" />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <span className="text-sm font-medium truncate block">{slot.exercise.title}</span>
                <span className="text-xs text-muted-foreground">{slot.sets.length} set{slot.sets.length !== 1 ? "s" : ""}</span>
              </div>
            </>
          ) : (
            <>
              <AlertTriangle className="h-4 w-4 text-destructive shrink-0" />
              <span className="flex-1 text-sm text-destructive italic">Deleted exercise</span>
            </>
          )}
        </button>
        {slot.exercise && (
          <button
            type="button"
            onClick={() => onView(slot.exercise)}
            className="shrink-0 p-1.5 rounded-md text-muted-foreground hover:text-primary hover:bg-primary/10 transition-colors"
            title="View exercise"
          >
            <Eye className="h-4 w-4" />
          </button>
        )}
      </div>

      {expanded && (
        <div className="px-4 pb-3 space-y-1 border-t border-border pt-2">
          {slot.sets.map((set, i) => (
            <div key={i} className="flex items-center gap-2 text-xs">
              <span className="text-muted-foreground w-4 text-right">{i + 1}.</span>
              <span className="bg-muted px-1.5 py-0.5 rounded text-[10px] font-medium uppercase">{set.type}</span>
              <span className="text-foreground/80">{setDescription(set)}</span>
            </div>
          ))}
          {slot.restBetweenSets && (
            <p className="text-xs text-muted-foreground mt-1 flex items-center gap-1">
              <Clock className="h-3 w-3" /> Rest {displayDuration(slot.restBetweenSets)} between sets
            </p>
          )}
        </div>
      )}
    </div>
  );
}

// ── Muscle map ────────────────────────────────────────────────────────────────

const ROLE_FILL: Record<ActivationLevel, string> = {
  PRIMARY: "#dc2626",
  SECONDARY: "#fb923c",
  STABILIZER: "#fde047",
};

function ExerciseBodyPanel({ view, muscles, subGroupMap }: {
  view: "front" | "back";
  muscles: Array<{ subGroupId: number; activation: number; role: ActivationLevel }>;
  subGroupMap: Map<number, MuscleSubGroupResponse>;
}) {
  const { src, viewBox } = BODY_DIAGRAMS[view];
  const visible = muscles.filter((m) => {
    const sub = subGroupMap.get(m.subGroupId);
    return view === "front" ? !!sub?.svgPathFront : !!sub?.svgPathBack;
  });
  return (
    <div className="flex flex-col items-center gap-1.5 flex-1 min-w-0">
      <span className="text-xs font-medium text-muted-foreground uppercase tracking-widest">{view}</span>
      <div className="relative w-full rounded-xl bg-white/[0.06] border border-primary/40 p-3">
        <div className="relative">
          <img src={src} alt={`Body ${view}`} className="w-full select-none pointer-events-none" />
          <svg viewBox={viewBox} className="absolute inset-0 h-full w-full" preserveAspectRatio="none">
            {visible.map((m) => {
              const sub = subGroupMap.get(m.subGroupId)!;
              const d = (view === "front" ? sub.svgPathFront : sub.svgPathBack) ?? undefined;
              return (
                <path key={`${m.subGroupId}-${view}`} d={d} fill={ROLE_FILL[m.role]} fillOpacity={0.65}
                  stroke={ROLE_FILL[m.role]} strokeOpacity={0.4} strokeWidth={0.5} />
              );
            })}
          </svg>
        </div>
      </div>
    </div>
  );
}

function RoutineMuscleMap({ muscleSummary, slots, muscleGroups }: {
  muscleSummary: Record<string, number>;
  slots: ExerciseSlotResponse[];
  muscleGroups: MuscleGroupResponse[];
}) {
  const subGroupMap = useMemo(() => {
    const map = new Map<number, MuscleSubGroupResponse>();
    for (const g of muscleGroups) for (const s of g.subGroups) map.set(s.id, s);
    return map;
  }, [muscleGroups]);

  // Aggregate all muscles from all slots
  const allMuscles = useMemo(() => {
    const map = new Map<number, { subGroupId: number; activation: number; role: ActivationLevel }>();
    for (const slot of slots) {
      if (!slot.exercise) continue;
      for (const m of slot.exercise.muscles) {
        const existing = map.get(m.subGroupId);
        if (!existing || m.activation > existing.activation) {
          map.set(m.subGroupId, { subGroupId: m.subGroupId, activation: m.activation, role: m.role });
        }
      }
    }
    return Array.from(map.values());
  }, [slots]);

  const groupScoreMap = Object.fromEntries(
    Object.entries(muscleSummary).map(([k, v]) => [Number(k), v])
  );
  const activeGroups = muscleGroups.filter((g) => groupScoreMap[g.id] != null && groupScoreMap[g.id] > 0);

  // Radar chart
  const N = activeGroups.length;
  const SIZE = 260, cx = 130, cy = 130, maxR = 90, iconR = maxR + 28;
  const rawValues = activeGroups.map((g) => groupScoreMap[g.id] ?? 0);
  const maxVal = Math.max(...rawValues, 0.01);
  const normalized = rawValues.map((v) => v / maxVal);
  const angle = (i: number) => (2 * Math.PI * i) / N - Math.PI / 2;
  const toXY = (i: number, r: number) => ({ x: cx + r * Math.cos(angle(i)), y: cy + r * Math.sin(angle(i)) });
  const ringPath = (frac: number) => activeGroups.map((_, i) => { const { x, y } = toXY(i, maxR * frac); return `${i === 0 ? "M" : "L"} ${x} ${y}`; }).join(" ") + " Z";
  const dataPath = activeGroups.map((_, i) => { const { x, y } = toXY(i, maxR * normalized[i]); return `${i === 0 ? "M" : "L"} ${x} ${y}`; }).join(" ") + " Z";

  return (
    <div className="flex gap-6 items-start">
      {/* Legend */}
      <div className="shrink-0 space-y-3 min-w-[140px]">
        {(["PRIMARY", "SECONDARY", "STABILIZER"] as ActivationLevel[]).map((role) => {
          const list = allMuscles.filter((m) => m.role === role);
          if (!list.length) return null;
          const labels = { PRIMARY: "Primary", SECONDARY: "Secondary", STABILIZER: "Stabilizer" };
          return (
            <div key={role} className="space-y-1">
              <p className="text-xs font-semibold text-muted-foreground">{labels[role]}</p>
              {list.map((m) => (
                <div key={m.subGroupId} className="flex items-center gap-2 text-xs">
                  <span className="h-2.5 w-2.5 rounded-full shrink-0" style={{ backgroundColor: ROLE_FILL[role] }} />
                  <span className="text-foreground/80">{subGroupMap.get(m.subGroupId)?.name ?? "Unknown muscle (removed)"}</span>
                </div>
              ))}
            </div>
          );
        })}
        {allMuscles.length === 0 && <p className="text-xs text-muted-foreground italic">No muscle data.</p>}
      </div>

      {/* Body diagrams */}
      <div className="flex gap-3 max-w-[480px]">
        <ExerciseBodyPanel view="front" muscles={allMuscles} subGroupMap={subGroupMap} />
        <ExerciseBodyPanel view="back" muscles={allMuscles} subGroupMap={subGroupMap} />
      </div>
    </div>
  );
}

// ── Summary tab with radar ────────────────────────────────────────────────────

function SummaryTab({ muscleSummary, muscleGroups }: { muscleSummary: Record<string, number>; muscleGroups: MuscleGroupResponse[] }) {
  const groupScoreMap = Object.fromEntries(Object.entries(muscleSummary).map(([k, v]) => [Number(k), v]));
  const activeGroups = muscleGroups.filter((g) => (groupScoreMap[g.id] ?? 0) > 0);
  const N = activeGroups.length;

  if (N < 3) return <p className="text-sm text-muted-foreground italic text-center py-8">Not enough muscle data to display chart.</p>;

  const SIZE = 260, cx = 130, cy = 130, maxR = 90, iconR = maxR + 28;
  const rawValues = activeGroups.map((g) => groupScoreMap[g.id] ?? 0);
  const maxVal = Math.max(...rawValues, 0.01);
  const normalized = rawValues.map((v) => v / maxVal);
  const angle = (i: number) => (2 * Math.PI * i) / N - Math.PI / 2;
  const toXY = (i: number, r: number) => ({ x: cx + r * Math.cos(angle(i)), y: cy + r * Math.sin(angle(i)) });
  const ringPath = (frac: number) => activeGroups.map((_, i) => { const { x, y } = toXY(i, maxR * frac); return `${i === 0 ? "M" : "L"} ${x} ${y}`; }).join(" ") + " Z";
  const dataPath = activeGroups.map((_, i) => { const { x, y } = toXY(i, maxR * normalized[i]); return `${i === 0 ? "M" : "L"} ${x} ${y}`; }).join(" ") + " Z";

  return (
    <div className="flex flex-col items-center gap-4 py-2">
      <div className="relative" style={{ width: SIZE, height: SIZE + 16 }}>
        <svg width={SIZE} height={SIZE} className="overflow-visible">
          {[0.25, 0.5, 0.75, 1].map((frac) => (
            <path key={frac} d={ringPath(frac)} fill="none" stroke="currentColor" strokeOpacity={0.08} strokeWidth={1} className="text-foreground" />
          ))}
          {activeGroups.map((_, i) => { const { x, y } = toXY(i, maxR); return <line key={i} x1={cx} y1={cy} x2={x} y2={y} stroke="currentColor" strokeOpacity={0.1} strokeWidth={1} className="text-foreground" />; })}
          <path d={dataPath} fill="#ff6a00" fillOpacity={0.25} stroke="#ff6a00" strokeWidth={2} strokeLinejoin="round" />
          {activeGroups.map((_, i) => { const { x, y } = toXY(i, maxR * normalized[i]); return <circle key={i} cx={x} cy={y} r={3.5} fill="#ff6a00" />; })}
        </svg>
        {activeGroups.map((g, i) => {
          const { x, y } = toXY(i, iconR);
          return (
            <div key={g.id} className="absolute flex flex-col items-center gap-0.5" style={{ left: x, top: y, transform: "translate(-50%,-50%)", width: 48 }}>
              <div className="h-8 w-8 rounded-lg bg-muted/80 flex items-center justify-center">
                {g.icon?.url ? <img src={g.icon.url} alt={g.name} className="h-6 w-6 object-contain" /> : <Layers className="h-4 w-4 text-muted-foreground" />}
              </div>
              <span className="text-[9px] text-muted-foreground text-center leading-tight line-clamp-2">{g.name}</span>
            </div>
          );
        })}
      </div>
      <div className="flex flex-wrap justify-center gap-x-4 gap-y-1">
        {activeGroups.map((g) => {
          const pct = Math.round((groupScoreMap[g.id] / maxVal) * 100);
          return (
            <div key={g.id} className="flex items-center gap-1.5 text-xs text-muted-foreground">
              {g.icon?.url ? <img src={g.icon.url} alt={g.name} className="h-3.5 w-3.5 object-contain" /> : <Layers className="h-3.5 w-3.5" />}
              <span>{g.name}</span>
              <span className="text-primary font-medium">{pct}%</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── Main dialog ───────────────────────────────────────────────────────────────

function toProgramRoutineDto(r: RoutineResponse, position: number, keepId: boolean): ProgramRoutineDto {
  return {
    ...(keepId ? { id: r.id } : {}),
    title: r.title,
    description: r.description ?? undefined,
    level: r.level,
    ...(!keepId && r.thumbnail?.id ? { thumbnailId: r.thumbnail.id } : {}),
    removeThumbnail: false,
    position,
    restBetweenExercises: r.restBetweenExercises,
    slots: r.slots.map((s, i) => ({
      exerciseId: s.exerciseId,
      position: s.position ?? i + 1,
      restBetweenSets: s.restBetweenSets,
      sets: s.sets.map((set) => ({
        type: set.type,
        reps: set.reps || undefined,
        weightKg: set.weightKg ?? undefined,
        weightMode: set.weightMode ?? "TOTAL",
        duration: set.duration ?? undefined,
      })),
    })),
    equipmentIds: r.equipment.map((e) => e.id),
    activityIds: r.activities.map((a) => a.id),
    trainingGoalIds: r.trainingGoals.map((g) => g.id),
  };
}

/** Build a create payload for a standalone My Routines copy (no program id). */
function toStandaloneRoutinePayload(r: RoutineResponse): RoutinePayload {
  return {
    title: `${r.title} (copy)`,
    description: r.description ?? undefined,
    level: r.level,
    thumbnailId: r.thumbnail?.id,
    restBetweenExercises: r.restBetweenExercises,
    slots: r.slots.map((s, i) => ({
      exerciseId: s.exerciseId,
      position: s.position ?? i + 1,
      restBetweenSets: s.restBetweenSets ?? undefined,
      sets: s.sets.map((set) => ({
        type: set.type,
        reps: set.reps || undefined,
        weightKg: set.weightKg ?? undefined,
        weightMode: set.weightMode ?? "TOTAL",
        duration: set.duration ?? undefined,
      })),
    })),
    equipmentIds: r.equipment.map((e) => e.id),
    activityIds: r.activities.map((a) => a.id),
    trainingGoalIds: r.trainingGoals.map((g) => g.id),
  };
}

function AddToProgramPicker({
  routine,
  onBack,
  onDone,
}: {
  routine: RoutineResponse;
  onBack: () => void;
  onDone: () => void;
}) {
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["programs-add-routine-picker", page],
    queryFn: () => programApi.list({}, page, 12),
    staleTime: 30_000,
  });

  const programs = (data?.content ?? []).filter((p) =>
    search ? p.title.toLowerCase().includes(search.toLowerCase()) : true
  );

  const mutation = useMutation({
    mutationFn: async (program: TrainingProgramResponse) => {
      const sorted = [...program.routines].sort((a, b) => (a.position ?? 0) - (b.position ?? 0));
      const existing = sorted.map((r, i) => toProgramRoutineDto(r, i + 1, true));
      const copied = toProgramRoutineDto(routine, existing.length + 1, false);
      return programApi.update(program.id, { routines: [...existing, copied] });
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["programs"] });
      onDone();
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  return (
    <div className="space-y-3 px-6 py-4">
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={onBack}
          disabled={mutation.isPending}
          className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors shrink-0"
        >
          <ChevronDown className="h-4 w-4 rotate-90" /> Back
        </button>
        <Input
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          placeholder="Search your programs…"
          className="h-8 text-sm flex-1"
          autoFocus
        />
      </div>

      <p className="text-xs text-muted-foreground">
        A copy of this routine will be added to the selected program.
      </p>

      {error && <ApiError message={error} />}

      {isLoading ? (
        <div className="flex justify-center py-6"><Spinner size="sm" label="" /></div>
      ) : programs.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-6">No programs found.</p>
      ) : (
        <div className="space-y-1.5 overflow-y-auto max-h-[360px]">
          {programs.map((p) => (
            <button
              key={p.id}
              type="button"
              disabled={mutation.isPending}
              onClick={() => { setError(null); mutation.mutate(p); }}
              className="w-full flex items-center gap-3 px-3 py-2 rounded-lg border border-border hover:border-primary/60 hover:bg-accent/30 text-left transition-colors disabled:opacity-50"
            >
              <div className="h-9 w-9 rounded-md bg-muted overflow-hidden shrink-0 flex items-center justify-center">
                {p.thumbnail?.url ? (
                  <img src={p.thumbnail.url} className="h-full w-full object-cover" alt="" />
                ) : (
                  <Layers className="h-4 w-4 text-muted-foreground" />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{p.title}</p>
                <p className="text-xs text-muted-foreground">
                  {p.routines.length} routine{p.routines.length !== 1 ? "s" : ""} · {p.daysPerWeek}d/wk
                </p>
              </div>
              <span className="text-xs text-primary font-medium shrink-0">
                {mutation.isPending && mutation.variables?.id === p.id ? "Adding…" : "Add"}
              </span>
            </button>
          ))}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex justify-center items-center gap-2">
          <button type="button" disabled={data.first} onClick={() => setPage((p) => p - 1)} className="text-xs text-muted-foreground hover:text-foreground disabled:opacity-40 px-2 py-1 rounded border border-border">← Prev</button>
          <span className="text-xs text-muted-foreground">{page + 1} / {data.totalPages}</span>
          <button type="button" disabled={data.last} onClick={() => setPage((p) => p + 1)} className="text-xs text-muted-foreground hover:text-foreground disabled:opacity-40 px-2 py-1 rounded border border-border">Next →</button>
        </div>
      )}
    </div>
  );
}

export function RoutineDetailDialog({
  routine,
  onClose,
  onEdit,
  onDelete,
  onDuplicate,
}: {
  routine: RoutineResponse | null;
  onClose: () => void;
  onEdit?: () => void;
  onDelete?: () => void;
  onDuplicate?: () => void;
}) {
  const qc = useQueryClient();
  const [viewExercise, setViewExercise] = useState<ExerciseResponseType | null>(null);
  const [addToProgramOpen, setAddToProgramOpen] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [copiedOk, setCopiedOk] = useState(false);
  const { data: muscleGroups = [] } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: catalogApi.listMuscleGroups,
    staleTime: 60_000,
    enabled: !!routine,
  });

  const isProgramRoutine = !!routine?.trainingProgramId;

  const duplicateToMyRoutines = useMutation({
    mutationFn: (r: RoutineResponse) => routineApi.create(toStandaloneRoutinePayload(r)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["routines"] });
      setActionError(null);
      setCopiedOk(true);
      window.setTimeout(() => setCopiedOk(false), 2500);
    },
    onError: (err) => {
      setCopiedOk(false);
      setActionError(getErrorMessage(err));
    },
  });

  if (!routine) return null;

  const totalSets = routine.slots.reduce((acc, s) => acc + s.sets.length, 0);

  return (
    <>
      <Dialog
        open={!!routine}
        onOpenChange={(v) => {
          if (!v) {
            setAddToProgramOpen(false);
            setActionError(null);
            setCopiedOk(false);
            onClose();
          }
        }}
      >
      <DialogContent className="sm:max-w-4xl max-h-[90vh] flex flex-col overflow-hidden p-0">
        <div className="absolute right-12 top-4 z-10">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-8 w-8 opacity-70 hover:opacity-100">
                <MoreVertical className="h-4 w-4" />
                <span className="sr-only">Actions</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56 z-[60]">
              {onEdit && (
                <DropdownMenuItem
                  onClick={() => {
                    setAddToProgramOpen(false);
                    onEdit();
                  }}
                >
                  <Pencil className="h-4 w-4 mr-2" /> Edit
                </DropdownMenuItem>
              )}
              {isProgramRoutine ? (
                <DropdownMenuItem
                  disabled={duplicateToMyRoutines.isPending}
                  onClick={() => {
                    setAddToProgramOpen(false);
                    setActionError(null);
                    duplicateToMyRoutines.mutate(routine);
                  }}
                >
                  <Copy className="h-4 w-4 mr-2" />
                  {duplicateToMyRoutines.isPending ? "Copying…" : "Duplicate to My Routines"}
                </DropdownMenuItem>
              ) : onDuplicate ? (
                <DropdownMenuItem
                  onClick={() => {
                    setAddToProgramOpen(false);
                    onDuplicate();
                  }}
                >
                  <Copy className="h-4 w-4 mr-2" /> Duplicate
                </DropdownMenuItem>
              ) : null}
              <DropdownMenuItem onClick={() => setAddToProgramOpen(true)}>
                <FolderPlus className="h-4 w-4 mr-2" /> Add to program
              </DropdownMenuItem>
              {onDelete && (
                <>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem
                    className="text-destructive focus:text-destructive"
                    onClick={onDelete}
                  >
                    <Trash2 className="h-4 w-4 mr-2" /> Delete
                  </DropdownMenuItem>
                </>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {actionError && (
          <div className="px-6 pt-2">
            <ApiError message={actionError} />
          </div>
        )}
        {copiedOk && (
          <p className="px-6 pt-2 text-xs text-center text-emerald-400">
            Copied to My Routines as “{routine.title} (copy)”
          </p>
        )}

        {addToProgramOpen ? (
          <>
            <DialogHeader className="px-6 pt-5 pb-0 shrink-0 items-center text-center">
              <DialogTitle className="text-xl font-bold">Add to program</DialogTitle>
              <p className="text-sm text-muted-foreground mt-1 truncate max-w-full px-8">{routine.title}</p>
            </DialogHeader>
            <AddToProgramPicker
              routine={routine}
              onBack={() => setAddToProgramOpen(false)}
              onDone={() => setAddToProgramOpen(false)}
            />
          </>
        ) : (
        <>
        <DialogHeader className="px-6 pt-5 pb-0 shrink-0 items-center text-center">
          <DialogTitle className="text-xl font-bold">{routine.title}</DialogTitle>
          <span className={cn("inline-flex items-center self-center px-3 py-1 rounded-full text-xs font-semibold border mt-2", LEVEL_COLOR[routine.level])}>
            {LEVEL_LABEL[routine.level]}
          </span>
        </DialogHeader>

        <div className="flex-1 overflow-y-auto px-6 pb-6">
          {/* ── Top: thumbnail + catalog stats ── */}
          <div className="flex flex-col sm:flex-row gap-5 mt-4 items-start">
            {/* Thumbnail + quick stats below */}
            <div className="sm:w-[65%] shrink-0 space-y-2 flex flex-col items-end">
              {/* Square thumbnail — same height as aspect-video, glued to the right */}
              <div className="w-[56.25%] aspect-square rounded-xl overflow-hidden bg-muted flex items-center justify-center border border-primary/50">
                {routine.thumbnail?.url ? (
                  <img src={routine.thumbnail.url} alt={routine.title} className="h-full w-full object-cover" />
                ) : (
                  <Dumbbell className="h-12 w-12 text-muted-foreground/30" />
                )}
              </div>
              {/* Stats row below thumbnail */}
              <div className="flex flex-wrap gap-3 text-sm px-1 w-[56.25%]">
                <div className="flex items-center gap-1.5">
                  <Dumbbell className="h-4 w-4 text-muted-foreground" />
                  <span>{routine.slots.length} exercise{routine.slots.length !== 1 ? "s" : ""}</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="text-muted-foreground text-xs">Sets:</span>
                  <span>{totalSets}</span>
                </div>
                {routine.estimatedDuration && (
                  <div className="flex items-center gap-1.5">
                    <Clock className="h-4 w-4 text-muted-foreground" />
                    <span>{displayDuration(routine.estimatedDuration)}</span>
                  </div>
                )}
                {routine.restBetweenExercises && (
                  <div className="flex items-center gap-1.5">
                    <span className="text-muted-foreground text-xs">Rest:</span>
                    <span>{displayDuration(routine.restBetweenExercises)}</span>
                  </div>
                )}
              </div>
            </div>

            {/* Catalog sections */}
            <div className="flex-1 space-y-3 self-stretch flex flex-col justify-center">
              <CatalogSection label="Equipment" items={routine.equipment} />
              <CatalogSection label="Activity" items={routine.activities} />
              <CatalogSection label="Training Goal" items={routine.trainingGoals} />
            </div>
          </div>

          {/* ── Description ── */}
          {routine.description && (
            <div className="mt-4 rounded-xl border border-primary/40 bg-card/50 px-4 py-3">
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1.5">Description</p>
              <p className="text-sm text-foreground/80 whitespace-pre-line">{routine.description}</p>
            </div>
          )}

          {/* ── Tabs ── */}
          <Tabs defaultValue="exercises" className="mt-6">
            <div className="relative flex justify-center">
              <TabsList className="border border-primary/40 relative z-10">
                <TabsTrigger value="exercises">Exercises</TabsTrigger>
                <TabsTrigger value="muscles">Muscles</TabsTrigger>
                <TabsTrigger value="summary">Summary</TabsTrigger>
              </TabsList>
              <div className="absolute top-full left-0 right-0 h-20 pointer-events-none -z-10" style={{
                background: "radial-gradient(ellipse 70% 100% at 50% 0%, rgba(255,106,0,0.18) 0%, transparent 100%)"
              }} />
            </div>

            {/* Exercises tab */}
            <TabsContent value="exercises" className="mt-4 relative z-10">
              <div className="space-y-2">
                {routine.slots.length === 0 ? (
                  <p className="text-sm text-muted-foreground italic text-center py-6">No exercises added.</p>
                ) : (
                  routine.slots
                    .slice()
                    .sort((a, b) => a.position - b.position)
                    .map((slot) => <SlotListItem key={slot.exerciseId} slot={slot} onView={(ex) => ex && setViewExercise(ex)} />)
                )}
              </div>
            </TabsContent>

            {/* Muscles tab */}
            <TabsContent value="muscles" className="mt-4 relative z-10">
              <RoutineMuscleMap muscleSummary={routine.muscleSummary} slots={routine.slots} muscleGroups={muscleGroups} />
            </TabsContent>

            {/* Summary tab */}
            <TabsContent value="summary" className="mt-4 relative z-10">
              <SummaryTab muscleSummary={routine.muscleSummary} muscleGroups={muscleGroups} />
            </TabsContent>
          </Tabs>
        </div>
        </>
        )}
      </DialogContent>
    </Dialog>

    <ExerciseDetailDialog exercise={viewExercise} onClose={() => setViewExercise(null)} />
    </>
  );
}
