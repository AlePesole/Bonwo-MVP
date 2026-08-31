import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { catalogApi } from "@/catalog/api";
import { BODY_DIAGRAMS } from "@/catalog/bodyDiagram";
import { cn } from "@/lib/utils";
import type {
  ActivationLevel,
  MuscleGroupResponse,
  MuscleSubGroupResponse,
  RoutineResponse,
  TrainingProgramResponse,
} from "@/types/api";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { AlertTriangle, CalendarDays, Copy, Dumbbell, Eye, Layers, MoreVertical, Pencil, Trash2 } from "lucide-react";
import { RoutineDetailDialog } from "@/routine/RoutineDetailDialog";

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

const ROLE_FILL: Record<ActivationLevel, string> = {
  PRIMARY: "#dc2626",
  SECONDARY: "#fb923c",
  STABILIZER: "#fde047",
};

// ── Catalog section (same as RoutineDetailDialog) ─────────────────────────────

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

// ── Routine entry list item ───────────────────────────────────────────────────

function RoutineEntryItem({
  routine,
  position,
  onView,
}: {
  routine: RoutineResponse;
  position: number;
  onView: (r: RoutineResponse) => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const sortedSlots = [...routine.slots].sort((a, b) => a.position - b.position);

  return (
    <div className="rounded-lg border border-primary/40 bg-card/60 overflow-hidden hover:border-primary transition-colors">
      <div className="flex items-center gap-3 p-3">
        <button
          type="button"
          className="flex items-center gap-3 flex-1 hover:bg-accent/20 transition-colors text-left rounded min-w-0"
          onClick={() => setExpanded((v) => !v)}
        >
          <span className="text-xs text-muted-foreground w-5 shrink-0 text-right">{position}.</span>
          <div className="h-12 w-12 rounded-md bg-muted overflow-hidden shrink-0 flex items-center justify-center">
            {routine.thumbnail?.url ? (
              <img src={routine.thumbnail.url} className="h-full w-full object-cover" alt="" />
            ) : (
              <Layers className="h-5 w-5 text-muted-foreground" />
            )}
          </div>
          <div className="flex-1 min-w-0">
            <span className="text-sm font-medium truncate block">{routine.title}</span>
            <span className="text-xs text-muted-foreground">
              {routine.slots.length} exercise{routine.slots.length !== 1 ? "s" : ""}
            </span>
          </div>
        </button>
        <button
          type="button"
          onClick={() => onView(routine)}
          className="shrink-0 p-1.5 rounded-md text-muted-foreground hover:text-primary hover:bg-primary/10 transition-colors"
          title="View routine"
        >
          <Eye className="h-4 w-4" />
        </button>
      </div>

      {expanded && (
        <div className="pl-8 pr-3 pb-3 space-y-1.5 border-t border-border pt-2">
          {sortedSlots.length === 0 ? (
            <p className="text-xs text-muted-foreground text-center py-2">No exercises.</p>
          ) : (
            sortedSlots.map((slot) => (
              <div key={`${slot.exerciseId}-${slot.position}`} className="flex items-center gap-2.5 px-1 py-1">
                <span className="text-[10px] text-muted-foreground w-4 shrink-0 text-right">{slot.position}.</span>
                {slot.exercise ? (
                  <>
                    <div className="h-8 w-8 rounded-md bg-muted overflow-hidden shrink-0 flex items-center justify-center">
                      {slot.exercise.thumbnail?.url ? (
                        <img src={slot.exercise.thumbnail.url} className="h-full w-full object-cover" alt="" />
                      ) : (
                        <Dumbbell className="h-3.5 w-3.5 text-muted-foreground" />
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <span className="text-xs font-medium truncate block">{slot.exercise.title}</span>
                      <span className="text-[10px] text-muted-foreground">
                        {slot.sets.length} set{slot.sets.length !== 1 ? "s" : ""}
                      </span>
                    </div>
                  </>
                ) : (
                  <>
                    <AlertTriangle className="h-3.5 w-3.5 text-destructive shrink-0" />
                    <span className="flex-1 text-xs text-destructive italic">Deleted exercise</span>
                  </>
                )}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}

// ── Muscle map ────────────────────────────────────────────────────────────────

function ProgramBodyPanel({ view, muscles, subGroupMap }: {
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

// ── Muscle map helpers ────────────────────────────────────────────────────────

function useAggregatedMuscles(routines: RoutineResponse[]) {
  return useMemo(() => {
    const map = new Map<number, { subGroupId: number; activation: number; role: ActivationLevel }>();
    for (const routine of routines) {
      for (const slot of routine.slots) {
        if (!slot.exercise) continue;
        for (const m of slot.exercise.muscles) {
          const existing = map.get(m.subGroupId);
          if (!existing || m.activation > existing.activation) {
            map.set(m.subGroupId, { subGroupId: m.subGroupId, activation: m.activation, role: m.role });
          }
        }
      }
    }
    return Array.from(map.values());
  }, [routines]);
}

// Muscles tab: body diagrams + role legend
function ProgramMusclesTab({ routines, muscleGroups }: {
  routines: RoutineResponse[];
  muscleGroups: MuscleGroupResponse[];
}) {
  const subGroupMap = useMemo(() => {
    const map = new Map<number, MuscleSubGroupResponse>();
    for (const g of muscleGroups) for (const s of g.subGroups) map.set(s.id, s);
    return map;
  }, [muscleGroups]);

  const allMuscles = useAggregatedMuscles(routines);

  if (allMuscles.length === 0) {
    return <p className="text-sm text-muted-foreground text-center py-8">No muscle data available.</p>;
  }

  return (
    <div className="space-y-5">
      <div className="flex gap-4 max-w-[480px] mx-auto">
        <ProgramBodyPanel view="front" muscles={allMuscles} subGroupMap={subGroupMap} />
        <ProgramBodyPanel view="back" muscles={allMuscles} subGroupMap={subGroupMap} />
      </div>
      <div className="flex gap-6 items-start">
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
      </div>
    </div>
  );
}

// Summary tab: radar chart only
function ProgramSummaryTab({ muscleSummary, muscleGroups }: {
  muscleSummary: Record<string, number>;
  muscleGroups: MuscleGroupResponse[];
}) {
  const groupScoreMap = Object.fromEntries(Object.entries(muscleSummary).map(([k, v]) => [Number(k), v]));
  const activeGroups = muscleGroups.filter((g) => (groupScoreMap[g.id] ?? 0) > 0);
  const N = activeGroups.length;

  if (N < 3) {
    return <p className="text-sm text-muted-foreground italic text-center py-8">Not enough muscle data to display chart.</p>;
  }

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

export function ProgramDetailDialog({
  program,
  onClose,
  onEdit,
  onDuplicate,
  onDelete,
}: {
  program: TrainingProgramResponse | null;
  onClose: () => void;
  onEdit?: () => void;
  onDuplicate?: () => void;
  onDelete?: () => void;
}) {
  const [viewRoutine, setViewRoutine] = useState<RoutineResponse | null>(null);

  const { data: muscleGroups = [] } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: catalogApi.listMuscleGroups,
    staleTime: 60_000,
    enabled: !!program,
  });

  if (!program) return null;

  const sortedRoutines = [...program.routines].sort((a, b) => (a.position ?? 0) - (b.position ?? 0));
  const hasActions = !!(onEdit || onDuplicate || onDelete);

  return (
    <>
      <Dialog open={!!program} onOpenChange={(v) => !v && onClose()}>
        <DialogContent className="sm:max-w-4xl max-h-[90vh] flex flex-col overflow-hidden p-0">
          {hasActions && (
            <div className="absolute right-12 top-4 z-10">
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="icon" className="h-8 w-8 opacity-70 hover:opacity-100">
                    <MoreVertical className="h-4 w-4" />
                    <span className="sr-only">Actions</span>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-52 z-[60]">
                  {onEdit && (
                    <DropdownMenuItem onClick={onEdit}>
                      <Pencil className="h-4 w-4 mr-2" /> Edit
                    </DropdownMenuItem>
                  )}
                  {onDuplicate && (
                    <DropdownMenuItem onClick={onDuplicate}>
                      <Copy className="h-4 w-4 mr-2" /> Duplicate
                    </DropdownMenuItem>
                  )}
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
          )}

          <DialogHeader className="px-6 pt-5 pb-0 shrink-0 items-center text-center">
            <DialogTitle className="text-xl font-bold">{program.title}</DialogTitle>
            <span className={cn("inline-flex items-center self-center px-3 py-1 rounded-full text-xs font-semibold border mt-2", LEVEL_COLOR[program.level])}>
              {LEVEL_LABEL[program.level]}
            </span>
          </DialogHeader>

          <div className="flex-1 overflow-y-auto px-6 pb-6">
            {/* Top: thumbnail + catalog stats */}
            <div className="flex flex-col sm:flex-row gap-5 mt-4 items-start">
              <div className="sm:w-[65%] shrink-0 space-y-2 flex flex-col items-end">
                {/* Square thumbnail — same height as aspect-video, glued to the right */}
                <div className="w-[56.25%] aspect-square rounded-xl overflow-hidden bg-muted flex items-center justify-center border border-primary/50">
                  {program.thumbnail?.url ? (
                    <img src={program.thumbnail.url} alt={program.title} className="h-full w-full object-cover" />
                  ) : (
                    <CalendarDays className="h-12 w-12 text-muted-foreground/30" />
                  )}
                </div>
                {/* Stats */}
                <div className="flex flex-wrap gap-3 text-sm px-1 w-[56.25%]">
                  <div className="flex items-center gap-1.5">
                    <Layers className="h-4 w-4 text-muted-foreground" />
                    <span>{program.routines.length} routine{program.routines.length !== 1 ? "s" : ""}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <CalendarDays className="h-4 w-4 text-muted-foreground" />
                    <span>{program.daysPerWeek} day{program.daysPerWeek !== 1 ? "s" : ""}/week</span>
                  </div>
                </div>
              </div>

              {/* Catalog sections */}
              <div className="flex-1 space-y-3 self-stretch flex flex-col justify-center">
                <CatalogSection label="Equipment" items={program.equipment} />
                <CatalogSection label="Activity" items={program.activities} />
                <CatalogSection label="Training Goal" items={program.trainingGoals} />
              </div>
            </div>

            {/* Description */}
            {program.description && (
              <div className="mt-4 rounded-xl border border-primary/40 bg-card/50 px-4 py-3">
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1.5">Description</p>
                <p className="text-sm text-foreground/80 whitespace-pre-line">{program.description}</p>
              </div>
            )}

            {/* Tabs */}
            <Tabs defaultValue="routines" className="mt-5">
              <div className="flex justify-center mb-1">
                <TabsList className="border border-primary/40">
                  <TabsTrigger value="routines">Routines</TabsTrigger>
                  <TabsTrigger value="muscles">Muscles</TabsTrigger>
                  <TabsTrigger value="summary">Summary</TabsTrigger>
                </TabsList>
              </div>
              <div className="h-1 w-full bg-gradient-to-b from-primary/20 to-transparent rounded-full mb-4" />

              <TabsContent value="routines" className="mt-4 relative z-10">
                <div className="space-y-2">
                  {sortedRoutines.length === 0 ? (
                    <p className="text-sm text-muted-foreground text-center py-8">No routines in this program.</p>
                  ) : (
                    sortedRoutines.map((routine, i) => (
                      <RoutineEntryItem
                        key={routine.id}
                        routine={routine}
                        position={i + 1}
                        onView={(r) => setViewRoutine(r)}
                      />
                    ))
                  )}
                </div>
              </TabsContent>

              <TabsContent value="muscles" className="mt-4 relative z-10">
                <ProgramMusclesTab
                  routines={program.routines}
                  muscleGroups={muscleGroups}
                />
              </TabsContent>

              <TabsContent value="summary" className="mt-4 relative z-10">
                <ProgramSummaryTab
                  muscleSummary={program.muscleSummary}
                  muscleGroups={muscleGroups}
                />
              </TabsContent>
            </Tabs>
          </div>
        </DialogContent>
      </Dialog>

      <RoutineDetailDialog routine={viewRoutine} onClose={() => setViewRoutine(null)} />
    </>
  );
}
