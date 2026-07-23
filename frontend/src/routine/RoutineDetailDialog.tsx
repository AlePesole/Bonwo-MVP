import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { catalogApi } from "@/catalog/api";
import { BODY_DIAGRAMS } from "@/catalog/bodyDiagram";
import { displayDuration } from "./api";
import { cn } from "@/lib/utils";
import type {
  ActivationLevel,
  ExerciseSlotResponse,
  MuscleGroupResponse,
  MuscleSubGroupResponse,
  RoutineResponse,
  SetConfigResponse,
  SetType,
} from "@/types/api";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Dumbbell, Layers, Clock, AlertTriangle, Eye } from "lucide-react";
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
              <div className="h-8 w-8 rounded-md bg-muted overflow-hidden shrink-0">
                {slot.exercise.thumbnail?.url ? (
                  <img src={slot.exercise.thumbnail.url} className="h-full w-full object-cover" alt="" />
                ) : (
                  <Dumbbell className="h-4 w-4 text-muted-foreground m-2" />
                )}
              </div>
              <span className="flex-1 text-sm font-medium truncate">{slot.exercise.title}</span>
            </>
          ) : (
            <>
              <AlertTriangle className="h-4 w-4 text-destructive shrink-0" />
              <span className="flex-1 text-sm text-destructive italic">Deleted exercise</span>
            </>
          )}
          <span className="text-xs text-muted-foreground shrink-0">{slot.sets.length} sets</span>
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

export function RoutineDetailDialog({ routine, onClose }: { routine: RoutineResponse | null; onClose: () => void }) {
  const [viewExercise, setViewExercise] = useState<ExerciseResponseType | null>(null);
  const { data: muscleGroups = [] } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: catalogApi.listMuscleGroups,
    staleTime: 60_000,
    enabled: !!routine,
  });

  if (!routine) return null;

  const totalSets = routine.slots.reduce((acc, s) => acc + s.sets.length, 0);

  return (
    <>
      <Dialog open={!!routine} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-4xl max-h-[90vh] flex flex-col overflow-hidden p-0">
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
            <div className="sm:w-[65%] shrink-0 space-y-2">
              <div className="relative w-full aspect-video rounded-xl overflow-hidden bg-muted flex items-center justify-center border border-primary/50">
                {routine.thumbnail?.url ? (
                  <img src={routine.thumbnail.url} alt={routine.title} className="w-full h-full object-cover" />
                ) : (
                  <Dumbbell className="h-12 w-12 text-muted-foreground/30" />
                )}
              </div>
              {/* Stats row below thumbnail */}
              <div className="flex flex-wrap gap-3 text-sm px-1">
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
      </DialogContent>
    </Dialog>

    <ExerciseDetailDialog exercise={viewExercise} onClose={() => setViewExercise(null)} />
    </>
  );
}
