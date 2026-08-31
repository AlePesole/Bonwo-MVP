import { useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { catalogApi } from "@/catalog/api";
import { BODY_DIAGRAMS } from "@/catalog/bodyDiagram";
import { routineApi, type RoutinePayload } from "@/routine/api";
import { cn } from "@/lib/utils";
import { getErrorMessage } from "@/lib/axios";
import type {
  ActivationLevel,
  ExerciseResponse,
  MuscleEntryResponse,
  MuscleGroupResponse,
  MuscleSubGroupResponse,
  RoutineResponse,
} from "@/types/api";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
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
import { Spinner } from "@/components/Spinner";
import {
  ChevronDown,
  FolderPlus,
  Layers,
  Play,
  Dumbbell,
  MoreVertical,
  Pencil,
  Trash2,
} from "lucide-react";

// ── Level badge ───────────────────────────────────────────────────────────────

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

// ── Role colors for SVG muscle overlay ───────────────────────────────────────

const ROLE_FILL: Record<ActivationLevel, string> = {
  PRIMARY: "#dc2626",
  SECONDARY: "#fb923c",
  STABILIZER: "#fde047",
};

// ── Catalog icon item ─────────────────────────────────────────────────────────

function CatalogItem({ name, iconUrl }: { name: string; iconUrl?: string | null }) {
  return (
    <div className="flex flex-col items-center gap-1 text-center">
      <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center">
        {iconUrl ? (
          <img src={iconUrl} alt={name} className="h-7 w-7 object-contain" />
        ) : (
          <Layers className="h-5 w-5 text-muted-foreground" />
        )}
      </div>
      <span className="text-[10px] text-muted-foreground leading-tight line-clamp-2">{name}</span>
    </div>
  );
}

function CatalogSection({
  label,
  items,
}: {
  label: string;
  items: { id: number; name: string; icon?: { url: string } | null }[];
}) {
  const [popoverOpen, setPopoverOpen] = useState(false);
  if (items.length === 0) return null;
  const visible = items.slice(0, 3);
  const extra = items.slice(3);

  return (
    <div className="space-y-1.5">
      <p className="text-xs font-semibold text-foreground">{label}</p>
      <div className="grid grid-cols-4 gap-2 items-start">
        {visible.map((item) => (
          <CatalogItem key={item.id} name={item.name} iconUrl={item.icon?.url} />
        ))}
        {extra.length > 0 && (
          <div className="relative">
            <button
              type="button"
              onClick={() => setPopoverOpen((v) => !v)}
              className="flex flex-col items-center gap-1 text-center"
            >
              <div className="h-10 w-10 rounded-lg bg-muted hover:bg-accent transition-colors flex items-center justify-center">
                <span className="text-xs font-semibold text-muted-foreground">+{extra.length}</span>
              </div>
              <span className="text-[10px] text-muted-foreground leading-tight">more</span>
            </button>
            {popoverOpen && (
              <>
                <div className="fixed inset-0 z-30" onClick={() => setPopoverOpen(false)} />
                <div className="absolute right-0 top-full mt-1 z-40 bg-popover border border-border rounded-xl p-3 shadow-lg flex flex-wrap gap-2 w-max max-w-[280px]">
                  {extra.map((item) => (
                    <CatalogItem key={item.id} name={item.name} iconUrl={item.icon?.url} />
                  ))}
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// ── Muscle map with role colors ───────────────────────────────────────────────

function ExerciseBodyPanel({
  view,
  muscles,
  subGroupMap,
}: {
  view: "front" | "back";
  muscles: MuscleEntryResponse[];
  subGroupMap: Map<number, MuscleSubGroupResponse>;
}) {
  const { src, viewBox } = BODY_DIAGRAMS[view];

  const visible = muscles.filter((m) => {
    const sub = subGroupMap.get(m.subGroupId);
    return view === "front" ? !!sub?.svgPathFront : !!sub?.svgPathBack;
  });

  return (
    <div className="flex flex-col items-center gap-1.5 flex-1 min-w-0">
      <span className="text-xs font-medium text-muted-foreground uppercase tracking-widest">
        {view === "front" ? "Front" : "Back"}
      </span>
      <div className="relative w-full rounded-xl bg-white/[0.06] border border-primary/40 p-3">
        <div className="relative">
          <img src={src} alt={`Body ${view}`} className="w-full select-none pointer-events-none" />
          <svg viewBox={viewBox} className="absolute inset-0 h-full w-full" preserveAspectRatio="none">
            {visible.map((m) => {
              const sub = subGroupMap.get(m.subGroupId)!;
              const d = (view === "front" ? sub.svgPathFront : sub.svgPathBack) ?? undefined;
              const color = ROLE_FILL[m.role];
              return (
                <path
                  key={`${m.subGroupId}-${view}`}
                  d={d}
                  fill={color}
                  fillOpacity={0.65}
                  stroke={color}
                  strokeOpacity={0.4}
                  strokeWidth={0.5}
                />
              );
            })}
          </svg>
        </div>
      </div>
    </div>
  );
}

function ExerciseMuscleMap({
  muscles,
  muscleGroups,
}: {
  muscles: MuscleEntryResponse[];
  muscleGroups: MuscleGroupResponse[];
}) {
  const subGroupMap = useMemo(() => {
    const map = new Map<number, MuscleSubGroupResponse>();
    for (const g of muscleGroups) for (const s of g.subGroups) map.set(s.id, s);
    return map;
  }, [muscleGroups]);

  const byRole = useMemo(() => ({
    PRIMARY: muscles.filter((m) => m.role === "PRIMARY"),
    SECONDARY: muscles.filter((m) => m.role === "SECONDARY"),
    STABILIZER: muscles.filter((m) => m.role === "STABILIZER"),
  }), [muscles]);

  const roleLabel: Record<ActivationLevel, string> = {
    PRIMARY: "Primary Muscles",
    SECONDARY: "Secondary Muscles",
    STABILIZER: "Stabilizer Muscles",
  };

  return (
    <div className="flex gap-6 items-start">
      {/* Legend */}
      <div className="shrink-0 space-y-4 min-w-[160px]">
        {(["PRIMARY", "SECONDARY", "STABILIZER"] as ActivationLevel[]).map((role) => {
          const list = byRole[role];
          if (list.length === 0) return null;
          return (
            <div key={role} className="space-y-1.5">
              <p className="text-xs font-semibold text-muted-foreground">{roleLabel[role]}</p>
              <div className="space-y-1">
                {list.map((m) => {
                  const sub = subGroupMap.get(m.subGroupId);
                  return (
                    <div key={m.subGroupId} className="flex items-center gap-2 text-sm">
                      <span
                        className="h-3 w-3 rounded-full shrink-0"
                        style={{ backgroundColor: ROLE_FILL[role] }}
                      />
                      <span className="text-foreground/80 text-xs">{sub?.name ?? "Unknown muscle (removed)"}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
        {muscles.length === 0 && (
          <p className="text-xs text-muted-foreground italic">No muscles defined.</p>
        )}
      </div>

      {/* Body diagrams */}
      <div className="flex gap-3 max-w-[480px]">
        <ExerciseBodyPanel view="front" muscles={muscles} subGroupMap={subGroupMap} />
        <ExerciseBodyPanel view="back" muscles={muscles} subGroupMap={subGroupMap} />
      </div>
    </div>
  );
}

// ── Video / thumbnail media block ─────────────────────────────────────────────

// Derive a thumbnail from a Cloudinary video URL by swapping the extension
function videoThumbnailUrl(video: { url: string; thumbnailUrl: string | null } | null | undefined): string | null {
  if (!video) return null;
  if (video.thumbnailUrl) return video.thumbnailUrl;
  try {
    return video.url.replace(/\.[^.]+$/, ".jpg");
  } catch {
    return null;
  }
}

function MediaBlock({ exercise }: { exercise: ExerciseResponse }) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [playing, setPlaying] = useState(false);

  const togglePlay = () => {
    if (!videoRef.current) return;
    if (playing) { videoRef.current.pause(); setPlaying(false); }
    else { videoRef.current.play(); setPlaying(true); }
  };

  if (exercise.mainVideo?.url) {
    return (
      <div
        className="relative w-full aspect-video rounded-xl overflow-hidden bg-black cursor-pointer border border-primary/50"
        onClick={togglePlay}
      >
        <video
          ref={videoRef}
          src={exercise.mainVideo.url}
          className="w-full h-full object-cover"
          onEnded={() => setPlaying(false)}
          poster={exercise.thumbnail?.url ?? undefined}
        />
        {!playing && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/30">
            <div className="h-14 w-14 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center">
              <Play className="h-6 w-6 text-white fill-white ml-1" />
            </div>
          </div>
        )}
      </div>
    );
  }

  // Square thumbnail — same height as aspect-video, aligned to the right (against catalog)
  return (
    <div className="w-[56.25%] aspect-square ml-auto rounded-xl overflow-hidden bg-muted flex items-center justify-center border border-primary/50">
      {exercise.thumbnail?.url ? (
        <img src={exercise.thumbnail.url} alt={exercise.title} className="h-full w-full object-cover" />
      ) : (
        <Dumbbell className="h-12 w-12 text-muted-foreground/30" />
      )}
    </div>
  );
}

// ── Radar / Spider chart ──────────────────────────────────────────────────────

function RadarChart({
  groups,
  scores,
}: {
  groups: MuscleGroupResponse[];
  scores: Record<string, number>;
}) {
  const N = groups.length;
  if (N < 3) return null;

  const SIZE = 260;
  const cx = SIZE / 2;
  const cy = SIZE / 2;
  const maxR = 90;
  const iconR = maxR + 28;

  const rawValues = groups.map((g) => scores[String(g.id)] ?? 0);
  const maxVal = Math.max(...rawValues, 0.01);
  const normalized = rawValues.map((v) => v / maxVal);

  const angle = (i: number) => (2 * Math.PI * i) / N - Math.PI / 2;

  const toXY = (i: number, r: number) => ({
    x: cx + r * Math.cos(angle(i)),
    y: cy + r * Math.sin(angle(i)),
  });

  // Grid rings
  const rings = [0.25, 0.5, 0.75, 1];

  const ringPath = (frac: number) =>
    groups
      .map((_, i) => {
        const { x, y } = toXY(i, maxR * frac);
        return `${i === 0 ? "M" : "L"} ${x} ${y}`;
      })
      .join(" ") + " Z";

  const dataPath =
    groups
      .map((_, i) => {
        const { x, y } = toXY(i, maxR * normalized[i]);
        return `${i === 0 ? "M" : "L"} ${x} ${y}`;
      })
      .join(" ") + " Z";

  return (
    <div className="relative" style={{ width: SIZE, height: SIZE + 16 }}>
      <svg width={SIZE} height={SIZE} className="overflow-visible">
        {/* Grid rings */}
        {rings.map((frac) => (
          <path
            key={frac}
            d={ringPath(frac)}
            fill="none"
            stroke="currentColor"
            strokeOpacity={0.08}
            strokeWidth={1}
            className="text-foreground"
          />
        ))}

        {/* Axis lines */}
        {groups.map((_, i) => {
          const { x, y } = toXY(i, maxR);
          return (
            <line
              key={i}
              x1={cx} y1={cy}
              x2={x} y2={y}
              stroke="currentColor"
              strokeOpacity={0.1}
              strokeWidth={1}
              className="text-foreground"
            />
          );
        })}

        {/* Data polygon */}
        <path d={dataPath} fill="#ff6a00" fillOpacity={0.25} stroke="#ff6a00" strokeWidth={2} strokeLinejoin="round" />

        {/* Data dots */}
        {groups.map((_, i) => {
          const { x, y } = toXY(i, maxR * normalized[i]);
          return <circle key={i} cx={x} cy={y} r={3.5} fill="#ff6a00" />;
        })}
      </svg>

      {/* Icons at axis tips */}
      {groups.map((g, i) => {
        const { x, y } = toXY(i, iconR);
        return (
          <div
            key={g.id}
            className="absolute flex flex-col items-center gap-0.5"
            style={{
              left: x,
              top: y,
              transform: "translate(-50%, -50%)",
              width: 48,
            }}
          >
            <div className="h-8 w-8 rounded-lg bg-muted/80 flex items-center justify-center">
              {g.icon?.url ? (
                <img src={g.icon.url} alt={g.name} className="h-6 w-6 object-contain" />
              ) : (
                <Layers className="h-4 w-4 text-muted-foreground" />
              )}
            </div>
            <span className="text-[9px] text-muted-foreground text-center leading-tight line-clamp-2">
              {g.name}
            </span>
          </div>
        );
      })}
    </div>
  );
}

function MuscleSummaryTab({
  exercise,
  muscleGroups,
}: {
  exercise: ExerciseResponse;
  muscleGroups: MuscleGroupResponse[];
}) {
  const activeGroups = useMemo(() => {
    const keys = Object.keys(exercise.muscleSummary).map(Number);
    return muscleGroups.filter((g) => keys.includes(g.id));
  }, [exercise.muscleSummary, muscleGroups]);

  if (activeGroups.length === 0) {
    return <p className="text-sm text-muted-foreground italic text-center py-8">No muscle data for this exercise.</p>;
  }

  return (
    <div className="flex flex-col items-center gap-4 py-2">
      <RadarChart groups={activeGroups} scores={exercise.muscleSummary} />
      <div className="flex flex-wrap justify-center gap-x-4 gap-y-1">
        {activeGroups.map((g) => {
          const raw = exercise.muscleSummary[String(g.id)] ?? 0;
          const maxVal = Math.max(...Object.values(exercise.muscleSummary), 0.01);
          const pct = Math.round((raw / maxVal) * 100);
          return (
            <div key={g.id} className="flex items-center gap-1.5 text-xs text-muted-foreground">
              {g.icon?.url ? (
                <img src={g.icon.url} alt={g.name} className="h-3.5 w-3.5 object-contain" />
              ) : (
                <Layers className="h-3.5 w-3.5" />
              )}
              <span>{g.name}</span>
              <span className="text-primary font-medium">{pct}%</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}



function routineToUpdatePayload(r: RoutineResponse, extraExerciseId?: number): RoutinePayload {
  const slots = r.slots.map((s, i) => ({
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
  }));

  if (extraExerciseId != null) {
    slots.push({
      exerciseId: extraExerciseId,
      position: slots.length + 1,
      restBetweenSets: "PT1M",
      sets: [{ type: "REPS", reps: 10, weightMode: "TOTAL", weightKg: undefined, duration: undefined }],
    });
  }

  return {
    title: r.title,
    description: r.description ?? undefined,
    level: r.level,
    restBetweenExercises: r.restBetweenExercises,
    slots,
    equipmentIds: r.equipment.map((e) => e.id),
    activityIds: r.activities.map((a) => a.id),
    trainingGoalIds: r.trainingGoals.map((g) => g.id),
  };
}

function AddToRoutinePicker({
  exercise,
  onBack,
  onDone,
}: {
  exercise: ExerciseResponse;
  onBack: () => void;
  onDone: () => void;
}) {
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["routines-add-exercise-picker", page],
    queryFn: () => routineApi.list({}, page, 12),
    staleTime: 30_000,
  });

  const routines = (data?.content ?? []).filter((r) =>
    search ? r.title.toLowerCase().includes(search.toLowerCase()) : true
  );

  const mutation = useMutation({
    mutationFn: (routine: RoutineResponse) =>
      routineApi.update(routine.id, routineToUpdatePayload(routine, exercise.id)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["routines"] });
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
          placeholder="Search your routines…"
          className="h-8 text-sm flex-1"
          autoFocus
        />
      </div>

      <p className="text-xs text-muted-foreground">
        The exercise will be appended with a default 10-rep set. You can edit sets later.
      </p>

      {error && <ApiError message={error} />}

      {isLoading ? (
        <div className="flex justify-center py-6"><Spinner size="sm" label="" /></div>
      ) : routines.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-6">No routines found.</p>
      ) : (
        <div className="space-y-1.5 overflow-y-auto max-h-[360px]">
          {routines.map((r) => {
            const alreadyIn = r.slots.some((s) => s.exerciseId === exercise.id);
            return (
              <button
                key={r.id}
                type="button"
                disabled={mutation.isPending}
                onClick={() => { setError(null); mutation.mutate(r); }}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-lg border border-border hover:border-primary/60 hover:bg-accent/30 text-left transition-colors disabled:opacity-50"
              >
                <div className="h-9 w-9 rounded-md bg-muted overflow-hidden shrink-0 flex items-center justify-center">
                  {r.thumbnail?.url ? (
                    <img src={r.thumbnail.url} className="h-full w-full object-cover" alt="" />
                  ) : (
                    <Layers className="h-4 w-4 text-muted-foreground" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{r.title}</p>
                  <p className="text-xs text-muted-foreground">
                    {r.slots.length} exercise{r.slots.length !== 1 ? "s" : ""}
                    {alreadyIn ? " · already includes this exercise" : ""}
                  </p>
                </div>
                <span className="text-xs text-primary font-medium shrink-0">
                  {mutation.isPending && mutation.variables?.id === r.id ? "Adding…" : "Add"}
                </span>
              </button>
            );
          })}
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

export function ExerciseDetailDialog({
  exercise,
  onClose,
  onEdit,
  onDelete,
}: {
  exercise: ExerciseResponse | null;
  onClose: () => void;
  onEdit?: () => void;
  onDelete?: () => void;
}) {
  const [addToRoutineOpen, setAddToRoutineOpen] = useState(false);
  const [addedOk, setAddedOk] = useState(false);
  const { data: muscleGroups = [] } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: catalogApi.listMuscleGroups,
    staleTime: 60_000,
    enabled: !!exercise,
  });

  if (!exercise) return null;

  return (
    <Dialog
      open={!!exercise}
      onOpenChange={(v) => {
        if (!v) {
          setAddToRoutineOpen(false);
          setAddedOk(false);
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
            <DropdownMenuContent align="end" className="w-52 z-[60]">
              {onEdit && (
                <DropdownMenuItem
                  onClick={() => {
                    setAddToRoutineOpen(false);
                    onEdit();
                  }}
                >
                  <Pencil className="h-4 w-4 mr-2" /> Edit
                </DropdownMenuItem>
              )}
              <DropdownMenuItem onClick={() => { setAddedOk(false); setAddToRoutineOpen(true); }}>
                <FolderPlus className="h-4 w-4 mr-2" /> Add to routine
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

        {addToRoutineOpen ? (
          <>
            <DialogHeader className="px-6 pt-5 pb-0 shrink-0 items-center text-center">
              <DialogTitle className="text-xl font-bold">Add to routine</DialogTitle>
              <p className="text-sm text-muted-foreground mt-1 truncate max-w-full px-8">{exercise.title}</p>
            </DialogHeader>
            <AddToRoutinePicker
              exercise={exercise}
              onBack={() => setAddToRoutineOpen(false)}
              onDone={() => {
                setAddToRoutineOpen(false);
                setAddedOk(true);
                window.setTimeout(() => setAddedOk(false), 2500);
              }}
            />
          </>
        ) : (
        <>
        <DialogHeader className="px-6 pt-5 pb-0 shrink-0 items-center text-center">
          <DialogTitle className="text-xl font-bold">{exercise.title}</DialogTitle>
          <span className={cn(
            "inline-flex items-center self-center px-3 py-1 rounded-full text-xs font-semibold border mt-2",
            LEVEL_COLOR[exercise.level]
          )}>
            {LEVEL_LABEL[exercise.level]}
          </span>
          {addedOk && (
            <p className="text-xs text-emerald-400 mt-2">Added to routine</p>
          )}
        </DialogHeader>

        <div className="flex-1 overflow-y-auto px-6 pb-6">
          {/* ── Top: media + stats ── */}
          <div className="flex flex-col sm:flex-row gap-5 mt-4 items-start">
            {/* Media */}
            <div className="sm:w-[65%] shrink-0">
              <MediaBlock exercise={exercise} />
            </div>

            {/* Stats */}
            <div className="flex-1 space-y-4 self-stretch flex flex-col justify-center">

              <CatalogSection label="Equipment" items={exercise.equipment} />
              <CatalogSection label="Activity" items={exercise.activities} />
              <CatalogSection label="Training Goal" items={exercise.trainingGoals} />

              {exercise.equipment.length === 0 && exercise.activities.length === 0 && exercise.trainingGoals.length === 0 && (
                <p className="text-xs text-muted-foreground italic">No catalog details added.</p>
              )}
            </div>
          </div>

          {/* ── Tabs: Info / Muscles ── */}
          <Tabs defaultValue="muscles" className="mt-6">
            <div className="relative flex justify-center">
              <TabsList className="border border-primary/40 relative z-10">
                <TabsTrigger value="muscles">
                  Muscles
                  {exercise.muscles.length > 0 && (
                    <Badge variant="secondary" className="ml-1.5 text-[10px] px-1.5 py-0">
                      {exercise.muscles.length}
                    </Badge>
                  )}
                </TabsTrigger>
                <TabsTrigger value="details">Details</TabsTrigger>
                <TabsTrigger value="summary">Summary</TabsTrigger>
              </TabsList>
              <div className="absolute top-full left-0 right-0 h-20 pointer-events-none -z-10" style={{
                background: "radial-gradient(ellipse 70% 100% at 50% 0%, rgba(255,106,0,0.18) 0%, transparent 100%)"
              }} />
            </div>

            {/* Summary tab */}
            <TabsContent value="summary" className="mt-4 relative z-10">
              <MuscleSummaryTab exercise={exercise} muscleGroups={muscleGroups} />
            </TabsContent>

            {/* Details tab */}
            <TabsContent value="details" className="mt-4 space-y-4 relative z-10">
              {exercise.description && (
                <div className="space-y-1.5">
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Description</p>
                  <p className="text-sm text-foreground/80 leading-relaxed">{exercise.description}</p>
                </div>
              )}
              {exercise.instructions && (
                <div className="space-y-1.5">
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Instructions</p>
                  <p className="text-sm text-foreground/80 leading-relaxed whitespace-pre-line">{exercise.instructions}</p>
                </div>
              )}
              {!exercise.description && !exercise.instructions && (
                <p className="text-sm text-muted-foreground italic">No description or instructions added.</p>
              )}
            </TabsContent>

            {/* Muscles tab */}
            <TabsContent value="muscles" className="mt-4 relative z-10">
              <ExerciseMuscleMap muscles={exercise.muscles} muscleGroups={muscleGroups} />
            </TabsContent>
          </Tabs>
        </div>
        </>
        )}
      </DialogContent>
    </Dialog>
  );
}
