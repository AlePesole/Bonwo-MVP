import React, { useEffect, useMemo, useRef, useState } from "react";
import { useFieldArray, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  DndContext,
  closestCenter,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  arrayMove,
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { programApi } from "./api";
import { routineApi, formatDuration, parseDuration } from "@/routine/api";
import { exerciseApi } from "@/exercise/api";
import { publicationApi } from "@/publication/api";
import { catalogApi } from "@/catalog/api";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { api, getErrorMessage } from "@/lib/axios";
import { cn } from "@/lib/utils";
import type {
  ExerciseResponse,
  ImageUploadResponse,
  Level,
  RoutineResponse,
  SetType,
  TrainingProgramResponse,
  WeightMode,
} from "@/types/api";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { ApiError } from "@/components/ApiError";
import { Spinner } from "@/components/Spinner";
import {
  ChevronDown,
  Copy,
  Dumbbell,
  GripVertical,
  ImagePlus,
  Layers,
  Pencil,
  Plus,
  Trash2,
  X,
} from "lucide-react";

// ── Constants ─────────────────────────────────────────────────────────────────

const LEVELS: { value: Level; label: string }[] = [
  { value: "BEGINNER", label: "Beginner" },
  { value: "INTERMEDIATE", label: "Intermediate" },
  { value: "ADVANCED", label: "Advanced" },
];

const SET_TYPES: { value: SetType; label: string }[] = [
  { value: "REPS", label: "Reps" },
  { value: "TIMED", label: "Timed" },
  { value: "AMRAP", label: "AMRAP" },
  { value: "FAILURE", label: "To failure" },
];

const WEIGHT_MODES: { value: WeightMode; label: string }[] = [
  { value: "TOTAL", label: "Total" },
  { value: "PER_SIDE", label: "Per side" },
];

const LEVEL_BADGE: Record<string, string> = {
  BEGINNER: "bg-emerald-500/20 text-emerald-400",
  INTERMEDIATE: "bg-amber-500/20 text-amber-400",
  ADVANCED: "bg-red-500/20 text-red-400",
};

// ── Internal form types ────────────────────────────────────────────────────────

const setSubSchema = z.object({
  type: z.enum(["REPS", "TIMED", "AMRAP", "FAILURE"]),
  reps: z.coerce.number().int().min(0).optional(),
  weightKg: z.coerce.number().min(0).nullable().optional(),
  weightMode: z.enum(["TOTAL", "PER_SIDE"]).optional(),
  durationSecs: z.coerce.number().int().min(0).optional(),
});

const slotSubSchema = z.object({
  exerciseId: z.coerce.number().min(1, "Select an exercise"),
  restBetweenSetsSecs: z.coerce.number().int().min(0).optional(),
  sets: z.array(setSubSchema).min(1, "At least one set required"),
});

const routineSubSchema = z.object({
  title: z.string().min(1, "Title is required").max(200),
  level: z.enum(["BEGINNER", "INTERMEDIATE", "ADVANCED"]),
  description: z.string().optional(),
  restBetweenExercisesSecs: z.coerce.number().int().min(0).optional(),
  slots: z.array(slotSubSchema).optional(),
  equipmentIds: z.array(z.number()).optional(),
  activityIds: z.array(z.number()).optional(),
  trainingGoalIds: z.array(z.number()).optional(),
});

type RoutineSubForm = z.infer<typeof routineSubSchema>;

// ── Routine entry type (local state in ProgramDialog) ─────────────────────────

type RoutineSlotForm = {
  exerciseId: number;
  restBetweenSetsSecs: number;
  sets: {
    type: SetType;
    reps?: number;
    weightKg?: number | null;
    weightMode?: WeightMode;
    durationSecs?: number;
  }[];
};

export type RoutineEntry = {
  localId: string;
  id?: number;
  title: string;
  level: Level;
  description: string;
  thumbnailPreviewUrl: string | null;
  thumbnailToken: string | undefined;
  /** Owned image id to reuse when creating a copy (no new upload). */
  thumbnailId?: number;
  removeThumbnail: boolean;
  restBetweenExercisesSecs: number;
  slots: RoutineSlotForm[];
  resolvedExercises: ExerciseResponse[];
  equipmentIds: number[];
  activityIds: number[];
  trainingGoalIds: number[];
};

// ── Zod schema (program-level fields only) ────────────────────────────────────

const programSchema = z.object({
  title: z.string().min(1, "Title is required").max(200),
  level: z.enum(["BEGINNER", "INTERMEDIATE", "ADVANCED"]),
  description: z.string().optional(),
  daysPerWeek: z.coerce.number().int().min(1).max(7),
  equipmentIds: z.array(z.number()).optional(),
  activityIds: z.array(z.number()).optional(),
  trainingGoalIds: z.array(z.number()).optional(),
});

type ProgramForm = z.infer<typeof programSchema>;

// ── Duration input ─────────────────────────────────────────────────────────────

function DurationInput({
  value,
  onChange,
}: {
  value: number;
  onChange: (v: number) => void;
}) {
  const mins = Math.floor(value / 60);
  const secs = value % 60;
  return (
    <div className="flex items-center gap-1">
      <input
        type="number"
        min={0}
        value={mins || ""}
        onChange={(e) => onChange(Number(e.target.value) * 60 + secs)}
        placeholder="0"
        className="w-12 h-8 rounded-md border border-input bg-background px-2 text-sm text-center"
      />
      <span className="text-xs text-muted-foreground">min</span>
      <input
        type="number"
        min={0}
        max={59}
        value={secs || ""}
        onChange={(e) => onChange(mins * 60 + Number(e.target.value))}
        placeholder="0"
        className="w-12 h-8 rounded-md border border-input bg-background px-2 text-sm text-center"
      />
      <span className="text-xs text-muted-foreground">sec</span>
    </div>
  );
}

// ── Set row ───────────────────────────────────────────────────────────────────

function SetRow({
  slotIndex,
  setIndex,
  control,
  register,
  onRemove,
}: {
  slotIndex: number;
  setIndex: number;
  control: ReturnType<typeof useForm<RoutineSubForm>>["control"];
  register: ReturnType<typeof useForm<RoutineSubForm>>["register"];
  onRemove: () => void;
}) {
  const type = useWatch({ control, name: `slots.${slotIndex}.sets.${setIndex}.type` }) as SetType;
  const selectClass = "h-9 rounded-md border border-input bg-background px-2 text-xs focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring";
  const inputClass = "h-9 w-16 rounded-md border border-input bg-background px-2 text-xs text-center";

  return (
    <div className="flex items-center gap-1.5 flex-wrap">
      <span className="text-xs text-muted-foreground w-4 text-right">{setIndex + 1}.</span>

      <select {...register(`slots.${slotIndex}.sets.${setIndex}.type`)} className={selectClass} style={{ width: 90 }}>
        {SET_TYPES.map((t) => (
          <option key={t.value} value={t.value}>{t.label}</option>
        ))}
      </select>

      {(type === "REPS" || type === "FAILURE") && (
        <>
          <input type="number" min={0} {...register(`slots.${slotIndex}.sets.${setIndex}.reps`)} placeholder="Reps" className={inputClass} />
          <span className="text-xs text-muted-foreground">reps</span>
        </>
      )}
      {(type === "TIMED" || type === "AMRAP") && (
        <>
          <input type="number" min={0} {...register(`slots.${slotIndex}.sets.${setIndex}.durationSecs`)} placeholder="Sec" className={inputClass} />
          <span className="text-xs text-muted-foreground">sec</span>
        </>
      )}
      {type !== "AMRAP" && (
        <>
          <input type="number" min={0} step={0.5} {...register(`slots.${slotIndex}.sets.${setIndex}.weightKg`)} placeholder="kg" className={inputClass} />
          <select {...register(`slots.${slotIndex}.sets.${setIndex}.weightMode`)} className={selectClass} style={{ width: 80 }}>
            {WEIGHT_MODES.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
          </select>
          <span className="text-xs text-muted-foreground">weight</span>
        </>
      )}
      <button type="button" onClick={onRemove} className="text-muted-foreground hover:text-destructive transition-colors ml-auto">
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}

// ── Slot card ─────────────────────────────────────────────────────────────────

function SlotCard({
  id,
  slotIndex,
  collapsed,
  onToggleCollapse,
  control,
  register,
  setValue,
  onRemove,
  onOpenPicker,
  exercises,
}: {
  id: string;
  slotIndex: number;
  collapsed: boolean;
  onToggleCollapse: () => void;
  control: ReturnType<typeof useForm<RoutineSubForm>>["control"];
  register: ReturnType<typeof useForm<RoutineSubForm>>["register"];
  setValue: ReturnType<typeof useForm<RoutineSubForm>>["setValue"];
  onRemove: () => void;
  onOpenPicker: () => void;
  exercises: ExerciseResponse[];
}) {
  const exerciseId = useWatch({ control, name: `slots.${slotIndex}.exerciseId` }) ?? 0;
  const exercise = useMemo(() => exercises.find((e) => e.id === exerciseId) ?? null, [exerciseId, exercises]);
  const isDeletedExercise = Number(exerciseId) > 0 && !exercise;
  const restSecs = useWatch({ control, name: `slots.${slotIndex}.restBetweenSetsSecs` }) ?? 0;

  const { fields: setFields, append: appendSet, remove: removeSet } = useFieldArray({
    control,
    name: `slots.${slotIndex}.sets`,
  });

  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id });
  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
    zIndex: isDragging ? 50 : undefined,
  };

  return (
    <div ref={setNodeRef} style={style} className="rounded-lg border border-primary/40 bg-background p-3 space-y-2.5 hover:border-primary transition-colors">
      <div className="flex items-start gap-2">
        <div className="flex flex-col items-center gap-0.5 pt-1.5 shrink-0">
          <GripVertical
            className="h-4 w-4 text-muted-foreground/50 cursor-grab active:cursor-grabbing touch-none"
            {...attributes}
            {...listeners}
          />
          <button
            type="button"
            onClick={onToggleCollapse}
            className="text-muted-foreground hover:text-foreground transition-colors"
            title={collapsed ? "Expand sets" : "Collapse sets"}
          >
            <ChevronDown className={cn("h-3.5 w-3.5 transition-transform", collapsed && "-rotate-90")} />
          </button>
        </div>

        <div className="flex items-center gap-2 flex-1 min-w-0">
          <button
            type="button"
            onClick={onOpenPicker}
            className="flex-1 h-8 flex items-center gap-2 px-2 rounded-md border border-input bg-muted/40 hover:bg-accent/30 transition-colors text-sm min-w-0"
          >
            {exercise ? (
              <>
                <div className="h-5 w-5 rounded shrink-0 overflow-hidden bg-muted">
                  {exercise.thumbnail?.url ? (
                    <img src={exercise.thumbnail.url} className="h-full w-full object-cover" alt="" />
                  ) : (
                    <Dumbbell className="h-3 w-3 text-muted-foreground m-1" />
                  )}
                </div>
                <span className="truncate">{exercise.title}</span>
              </>
            ) : isDeletedExercise ? (
              <>
                <X className="h-3.5 w-3.5 text-destructive shrink-0" />
                <span className="text-destructive italic text-xs">Deleted exercise — replace or remove</span>
              </>
            ) : (
              <span className="text-muted-foreground">Select exercise…</span>
            )}
            <ChevronDown className="h-3.5 w-3.5 text-muted-foreground shrink-0 ml-auto" />
          </button>
          <button type="button" onClick={onRemove} className="text-muted-foreground hover:text-destructive transition-colors shrink-0">
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>

      {collapsed && setFields.length > 0 && (
        <p className="pl-6 text-xs text-muted-foreground">{setFields.length} set{setFields.length !== 1 ? "s" : ""}</p>
      )}

      {!collapsed && (
        <>
          <div className="space-y-1.5 pl-6">
            {setFields.map((f, si) => (
              <SetRow
                key={f.id}
                slotIndex={slotIndex}
                setIndex={si}
                control={control}
                register={register}
                onRemove={() => removeSet(si)}
              />
            ))}
            <button
              type="button"
              onClick={() => appendSet({ type: "REPS", reps: 10, weightMode: "TOTAL" })}
              className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors mt-1"
            >
              <Plus className="h-3 w-3" /> Add set
            </button>
          </div>
          <div className="pl-6 flex items-center gap-2">
            <span className="text-xs text-muted-foreground">Rest between sets:</span>
            <DurationInput value={restSecs} onChange={(v) => setValue(`slots.${slotIndex}.restBetweenSetsSecs`, v)} />
          </div>
        </>
      )}
    </div>
  );
}

// ── Exercise picker ───────────────────────────────────────────────────────────

function ExercisePicker({
  onSelect,
  onBack,
}: {
  onSelect: (ex: ExerciseResponse) => void;
  onBack: () => void;
}) {
  const [source, setSource] = useState<"mine" | "published">("mine");
  const [title, setTitle] = useState("");
  const debouncedTitle = useDebouncedValue(title.trim(), 350);
  const [muscleGroupId, setMuscleGroupId] = useState<number | "">("");
  const [muscleSubGroupId, setMuscleSubGroupId] = useState<number | "">("");
  const [page, setPage] = useState(0);

  const handleGroupChange = (v: number | "") => { setMuscleGroupId(v); setMuscleSubGroupId(""); setPage(0); };

  useEffect(() => { setPage(0); }, [debouncedTitle]);

  const { data: muscleGroups = [] } = useQuery({ queryKey: ["catalog", "muscles"], queryFn: catalogApi.listMuscleGroups, staleTime: 60_000 });
  const selectedGroup = muscleGroups.find((g) => g.id === muscleGroupId);

  const filter = {
    title: debouncedTitle || undefined,
    muscleGroupIds: muscleGroupId ? [Number(muscleGroupId)] : undefined,
    muscleSubGroupIds: muscleSubGroupId ? [Number(muscleSubGroupId)] : undefined,
  };

  const { data: mineData, isLoading: mineLoading } = useQuery({
    queryKey: ["exercises-picker-prog", filter, page],
    queryFn: () => exerciseApi.list(filter, page, 12),
    staleTime: 30_000,
    enabled: source === "mine",
  });

  const { data: pubData, isLoading: pubLoading } = useQuery({
    queryKey: ["publications-picker-prog", filter, page],
    queryFn: () => publicationApi.listFeed(filter, page, 12),
    staleTime: 30_000,
    enabled: source === "published",
  });

  const isLoading = source === "mine" ? mineLoading : pubLoading;
  const exercises: ExerciseResponse[] =
    source === "mine"
      ? (mineData?.content ?? [])
      : (pubData?.content ?? []).map((p) => p.exercise);
  const pageData = source === "mine" ? mineData : pubData;

  const selectClass = "h-7 rounded-md border border-input bg-background px-2 text-xs focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring flex-1 min-w-0";

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <button type="button" onClick={onBack} className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors shrink-0">
          <ChevronDown className="h-4 w-4 rotate-90" /> Back
        </button>
        <Input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Search exercises…"
          className="h-8 text-sm flex-1"
          autoFocus
        />
      </div>

      <div className="flex gap-1 p-0.5 rounded-lg bg-muted/50 border border-border">
        <button
          type="button"
          onClick={() => { setSource("mine"); setPage(0); }}
          className={cn(
            "flex-1 text-xs font-medium py-1.5 rounded-md transition-colors",
            source === "mine" ? "bg-background text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"
          )}
        >
          My exercises
        </button>
        <button
          type="button"
          onClick={() => { setSource("published"); setPage(0); }}
          className={cn(
            "flex-1 text-xs font-medium py-1.5 rounded-md transition-colors",
            source === "published" ? "bg-background text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"
          )}
        >
          Published
        </button>
      </div>

      <div className="flex items-center gap-2 flex-wrap">
        <select value={muscleGroupId} onChange={(e) => handleGroupChange(e.target.value ? Number(e.target.value) : "")} className={selectClass} style={{ maxWidth: 140 }}>
          <option value="">All muscles</option>
          {muscleGroups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
        </select>
        {selectedGroup && selectedGroup.subGroups.length > 0 && (
          <select value={muscleSubGroupId} onChange={(e) => { setMuscleSubGroupId(e.target.value ? Number(e.target.value) : ""); setPage(0); }} className={selectClass} style={{ maxWidth: 140 }}>
            <option value="">All sub-groups</option>
            {selectedGroup.subGroups.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        )}
        {(title || muscleGroupId) && (
          <button type="button" onClick={() => { setTitle(""); setMuscleGroupId(""); setMuscleSubGroupId(""); setPage(0); }} className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-0.5 shrink-0">
            <X className="h-3 w-3" /> Clear
          </button>
        )}
      </div>

      {isLoading ? (
        <div className="flex justify-center py-6"><Spinner size="sm" label="" /></div>
      ) : exercises.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-6">No exercises found.</p>
      ) : (
        <div className="space-y-1.5 overflow-y-auto max-h-[240px]">
          {exercises.map((ex) => (
            <button
              key={ex.id}
              type="button"
              onClick={() => onSelect(ex)}
              className="w-full flex items-center gap-3 px-3 py-2 rounded-lg border border-border hover:border-primary/50 hover:bg-accent/30 text-left transition-colors"
            >
              <div className="h-8 w-8 rounded-md bg-muted flex items-center justify-center shrink-0 overflow-hidden">
                {ex.thumbnail?.url ? (
                  <img src={ex.thumbnail.url} alt={ex.title} className="h-full w-full object-cover" />
                ) : (
                  <Dumbbell className="h-4 w-4 text-muted-foreground/50" />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <span className="text-sm truncate block">{ex.title}</span>
                {ex.publicationId != null && (
                  <span className="text-[10px] text-sky-400">Published</span>
                )}
              </div>
              <span className={cn("text-[10px] font-bold px-1.5 py-0.5 rounded-full shrink-0", LEVEL_BADGE[ex.level] ?? "bg-muted text-muted-foreground")}>
                {ex.level[0] + ex.level.slice(1).toLowerCase()}
              </span>
            </button>
          ))}
        </div>
      )}

      {pageData && pageData.totalPages > 1 && (
        <div className="flex justify-center items-center gap-2">
          <button type="button" disabled={pageData.first} onClick={() => setPage((p) => p - 1)} className="text-xs text-muted-foreground hover:text-foreground disabled:opacity-40 px-2 py-1 rounded border border-border">← Prev</button>
          <span className="text-xs text-muted-foreground">{page + 1} / {pageData.totalPages}</span>
          <button type="button" disabled={pageData.last} onClick={() => setPage((p) => p + 1)} className="text-xs text-muted-foreground hover:text-foreground disabled:opacity-40 px-2 py-1 rounded border border-border">Next →</button>
        </div>
      )}
    </div>
  );
}

// ── Details catalog picker ─────────────────────────────────────────────────────

function DetailsPicker({
  equipment,
  activities,
  trainingGoals,
  equipmentIds,
  activityIds,
  trainingGoalIds,
  onToggle,
  onSync,
  syncCount,
  syncLabel,
}: {
  equipment: { id: number; name: string; icon?: { url: string } | null }[];
  activities: { id: number; name: string; icon?: { url: string } | null }[];
  trainingGoals: { id: number; name: string; icon?: { url: string } | null }[];
  equipmentIds: number[];
  activityIds: number[];
  trainingGoalIds: number[];
  onToggle: (field: "equipmentIds" | "activityIds" | "trainingGoalIds", id: number) => void;
  onSync?: () => void;
  syncCount?: number;
  syncLabel?: string;
}) {
  const [open, setOpen] = useState<"equipment" | "activities" | "trainingGoals" | null>(null);

  const config = [
    { key: "equipment" as const, field: "equipmentIds" as const, items: equipment, selected: equipmentIds, label: "Equipment" },
    { key: "activities" as const, field: "activityIds" as const, items: activities, selected: activityIds, label: "Activities" },
    { key: "trainingGoals" as const, field: "trainingGoalIds" as const, items: trainingGoals, selected: trainingGoalIds, label: "Training Goals" },
  ];

  const current = open ? config.find((c) => c.key === open)! : null;

  return (
    <div className="rounded-lg border border-primary/40 bg-card/50 p-4 min-h-[220px]">
      {current ? (
        <div className="space-y-3">
          <button type="button" onClick={() => setOpen(null)} className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
            <ChevronDown className="h-4 w-4 rotate-90" /> Back
          </button>
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">{current.label}</p>
          <div className="grid grid-cols-3 gap-2 overflow-y-auto max-h-[280px]">
            {current.items.map((item) => {
              const isSelected = current.selected.includes(item.id);
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => onToggle(current.field, item.id)}
                  className={cn(
                    "flex flex-col items-center gap-2 p-3 rounded-xl border transition-colors text-center",
                    isSelected ? "border-primary bg-primary/10 text-primary" : "border-border bg-card hover:border-primary/50 hover:bg-accent/40"
                  )}
                >
                  <div className={cn("h-10 w-10 rounded-lg flex items-center justify-center", isSelected ? "bg-primary/20" : "bg-muted")}>
                    {item.icon?.url ? (
                      <img src={item.icon.url} alt={item.name} className="h-7 w-7 object-contain" />
                    ) : (
                      <Layers className="h-5 w-5 text-muted-foreground" />
                    )}
                  </div>
                  <span className="text-xs font-medium leading-tight">{item.name}</span>
                </button>
              );
            })}
          </div>
        </div>
      ) : (
        <div className="space-y-2">
          {onSync && (
            <button
              type="button"
              onClick={onSync}
              disabled={!syncCount}
              className="w-full flex items-center justify-between px-4 py-2.5 rounded-lg border border-dashed border-primary/40 bg-primary/5 hover:bg-primary/10 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <span className="text-xs font-medium text-primary">{syncLabel ?? "Sync"}</span>
              {syncCount && syncCount > 0 ? (
                <span className="text-[10px] text-muted-foreground">{syncCount} item{syncCount !== 1 ? "s" : ""} found</span>
              ) : null}
            </button>
          )}
          {config.map(({ key, label, selected }) => (
            <button
              key={key}
              type="button"
              onClick={() => setOpen(key)}
              className="w-full flex items-center justify-between px-4 py-3 rounded-lg border border-border bg-background hover:border-primary/50 hover:bg-accent/30 transition-colors"
            >
              <span className="text-sm font-medium">{label}</span>
              <div className="flex items-center gap-2">
                {selected.length > 0 && (
                  <span className="text-xs text-primary font-medium">{selected.length}</span>
                )}
                <ChevronDown className="h-4 w-4 text-muted-foreground -rotate-90" />
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

// ── Thumbnail upload hook ─────────────────────────────────────────────────────

function useThumbnailUpload() {
  const fileRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [token, setToken] = useState<string | undefined>();
  const [removed, setRemoved] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reset = (existingUrl?: string | null) => {
    setPreview(existingUrl ?? null);
    setToken(undefined);
    setRemoved(false);
    setError(null);
  };

  const remove = () => {
    setPreview(null);
    setToken(undefined);
    setRemoved(true);
    if (fileRef.current) fileRef.current.value = "";
  };

  const handleFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setPreview(URL.createObjectURL(file));
    setRemoved(false);
    setError(null);
    try {
      const fd = new FormData();
      fd.append("file", file);
      const { data } = await api.post<ImageUploadResponse>("/media/images/upload", fd);
      setToken(data.uploadToken);
    } catch (err) {
      setError(getErrorMessage(err));
      setPreview(null);
    } finally {
      setUploading(false);
    }
  };

  return { token, removed, preview, uploading, error, reset, remove, fileRef, handleFile };
}

// ── ProgramRoutineSubDialog ────────────────────────────────────────────────────
// Full routine editor (mirrors RoutineDialog) but works on local state.
// Calls onSave(RoutineEntry) instead of making API calls.

function ProgramRoutineSubDialog({
  open,
  initial,
  onSave,
  onClose,
  equipment,
  activities,
  trainingGoals,
}: {
  open: boolean;
  initial: RoutineEntry | null;
  onSave: (entry: RoutineEntry) => void;
  onClose: () => void;
  equipment: { id: number; name: string; icon?: { url: string } | null }[];
  activities: { id: number; name: string; icon?: { url: string } | null }[];
  trainingGoals: { id: number; name: string; icon?: { url: string } | null }[];
}) {
  const [pickerSlotIndex, setPickerSlotIndex] = useState<number | null>(null);
  const [expandedSlot, setExpandedSlot] = useState<number | null>(0);
  const [resolvedExercises, setResolvedExercises] = useState<ExerciseResponse[]>([]);
  const thumb = useThumbnailUpload();

  const { register, handleSubmit, control, watch, setValue, reset, formState: { errors } } = useForm<RoutineSubForm>({
    resolver: zodResolver(routineSubSchema),
    defaultValues: { level: "INTERMEDIATE", slots: [], equipmentIds: [], activityIds: [], trainingGoalIds: [] },
  });

  const { fields: slotFields, append: appendSlot, remove: removeSlot, move: moveSlot } = useFieldArray({ control, name: "slots" });

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (over && active.id !== over.id) {
      const oi = slotFields.findIndex((f) => f.id === active.id);
      const ni = slotFields.findIndex((f) => f.id === over.id);
      if (oi !== -1 && ni !== -1) moveSlot(oi, ni);
    }
  };

  const equipmentIds = watch("equipmentIds") ?? [];
  const activityIds = watch("activityIds") ?? [];
  const trainingGoalIds = watch("trainingGoalIds") ?? [];
  const restBetweenExercisesSecs = watch("restBetweenExercisesSecs") ?? 0;
  const slotsWatched = useWatch({ control, name: "slots" });

  const toggle = (field: "equipmentIds" | "activityIds" | "trainingGoalIds", id: number) => {
    const cur = watch(field) ?? [];
    setValue(field, cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id]);
  };

  const handleSync = () => {
    const exercises = resolvedExercises.filter((e) => (slotsWatched ?? []).some((s) => s?.exerciseId === e.id));
    setValue("equipmentIds", [...new Set(exercises.flatMap((e) => e.equipment.map((x) => x.id)))]);
    setValue("activityIds",  [...new Set(exercises.flatMap((e) => e.activities.map((x) => x.id)))]);
    setValue("trainingGoalIds", [...new Set(exercises.flatMap((e) => e.trainingGoals.map((x) => x.id)))]);
  };

  const syncCount = useMemo(() => {
    const exercises = resolvedExercises.filter((e) => (slotsWatched ?? []).some((s) => s?.exerciseId === e.id));
    return new Set([
      ...exercises.flatMap((e) => e.equipment.map((x) => x.id)),
      ...exercises.flatMap((e) => e.activities.map((x) => x.id)),
      ...exercises.flatMap((e) => e.trainingGoals.map((x) => x.id)),
    ]).size;
  }, [resolvedExercises, slotsWatched]);

  useEffect(() => {
    if (open) {
      setPickerSlotIndex(null);
      setExpandedSlot(0);
      if (initial) {
        thumb.reset(initial.thumbnailPreviewUrl);
        setResolvedExercises(initial.resolvedExercises);
        reset({
          title: initial.title,
          level: initial.level,
          description: initial.description,
          restBetweenExercisesSecs: initial.restBetweenExercisesSecs,
          slots: initial.slots,
          equipmentIds: initial.equipmentIds,
          activityIds: initial.activityIds,
          trainingGoalIds: initial.trainingGoalIds,
        });
      } else {
        thumb.reset(null);
        setResolvedExercises([]);
        reset({ level: "INTERMEDIATE", slots: [], equipmentIds: [], activityIds: [], trainingGoalIds: [] });
      }
    }
  }, [open, initial]);

  const handleExerciseSelect = (ex: ExerciseResponse) => {
    if (pickerSlotIndex === null) return;
    setValue(`slots.${pickerSlotIndex}.exerciseId`, ex.id);
    setResolvedExercises((prev) => prev.some((e) => e.id === ex.id) ? prev : [...prev, ex]);
    setPickerSlotIndex(null);
  };

  const onSubmit = (data: RoutineSubForm) => {
    const entry: RoutineEntry = {
      localId: initial?.localId ?? crypto.randomUUID(),
      id: initial?.id,
      title: data.title,
      level: data.level,
      description: data.description ?? "",
      thumbnailPreviewUrl: thumb.preview,
      thumbnailToken: thumb.token,
      thumbnailId: thumb.token || thumb.removed ? undefined : initial?.thumbnailId,
      removeThumbnail: thumb.removed,
      restBetweenExercisesSecs: data.restBetweenExercisesSecs ?? 0,
      slots: (data.slots ?? []).map((s) => ({
        exerciseId: Number(s.exerciseId),
        restBetweenSetsSecs: s.restBetweenSetsSecs ?? 0,
        sets: s.sets.map((set) => ({
          type: set.type,
          reps: set.reps,
          weightKg: set.weightKg,
          weightMode: set.weightMode,
          durationSecs: set.durationSecs,
        })),
      })),
      resolvedExercises,
      equipmentIds: data.equipmentIds ?? [],
      activityIds: data.activityIds ?? [],
      trainingGoalIds: data.trainingGoalIds ?? [],
    };
    onSave(entry);
    onClose();
  };

  const selectClass = "flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:opacity-50";

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-2xl flex flex-col">
        <DialogHeader>
          <DialogTitle>{initial?.id ? "Edit Routine" : "Add Routine to Program"}</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-0 overflow-hidden flex-1">
          <Tabs defaultValue="info" className="flex-1 overflow-hidden flex flex-col">
            <TabsList className="mx-auto">
              <TabsTrigger value="info">Info</TabsTrigger>
              <TabsTrigger value="slots">
                Exercises
                {slotFields.length > 0 && (
                  <span className="ml-1 bg-primary/20 text-primary text-[10px] font-bold rounded-full px-1.5">{slotFields.length}</span>
                )}
              </TabsTrigger>
              <TabsTrigger value="details">Details</TabsTrigger>
            </TabsList>

            <div className="flex-1 overflow-y-auto px-1 pb-2">

              {/* ── Info ── */}
              <TabsContent value="info" className="space-y-4 mt-4">
                <div className="grid grid-cols-3 gap-3">
                  <div className="col-span-2 space-y-1.5">
                    <Label>Title</Label>
                    <Input {...register("title")} placeholder="e.g. Push Day A" />
                    {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
                  </div>
                  <div className="space-y-1.5">
                    <Label>Level</Label>
                    <select {...register("level")} className={selectClass}>
                      {LEVELS.map((l) => <option key={l.value} value={l.value}>{l.label}</option>)}
                    </select>
                  </div>
                </div>

                <div className="space-y-1.5">
                  <Label>Description <span className="text-muted-foreground">(optional)</span></Label>
                  <Textarea {...register("description")} rows={3} placeholder="Brief overview of this routine…" />
                </div>

                <div className="space-y-1.5">
                  <Label>Thumbnail <span className="text-muted-foreground">(optional)</span></Label>
                  <div className="flex items-center gap-3">
                    <div
                      className="h-16 w-16 rounded-lg border border-border bg-muted flex items-center justify-center cursor-pointer hover:bg-accent transition-colors shrink-0 overflow-hidden"
                      onClick={() => thumb.fileRef.current?.click()}
                    >
                      {thumb.uploading ? <Spinner size="sm" label="" /> : thumb.preview ? (
                        <img src={thumb.preview} alt="thumb" className="h-full w-full object-cover" />
                      ) : (
                        <ImagePlus className="h-5 w-5 text-muted-foreground" />
                      )}
                    </div>
                    <div className="flex flex-col gap-1">
                      <p className="text-xs text-muted-foreground">Click to {thumb.preview ? "change" : "upload"}{thumb.token && <span className="text-primary ml-1">(ready)</span>}</p>
                      {thumb.preview && !thumb.uploading && (
                        <button type="button" onClick={thumb.remove} className="text-xs text-destructive hover:underline text-left">Remove</button>
                      )}
                    </div>
                    <input ref={thumb.fileRef} type="file" accept="image/*" className="hidden" onChange={thumb.handleFile} />
                  </div>
                  {thumb.error && <p className="text-xs text-destructive">{thumb.error}</p>}
                </div>
              </TabsContent>

              {/* ── Exercises/Slots ── */}
              <TabsContent value="slots" className="mt-4">
                {pickerSlotIndex !== null ? (
                  <div className="rounded-lg border border-primary/40 bg-card/50 p-4 min-h-[300px]">
                    <ExercisePicker
                      onSelect={handleExerciseSelect}
                      onBack={() => setPickerSlotIndex(null)}
                    />
                  </div>
                ) : (
                  <div className="rounded-lg border border-primary/40 bg-card/50 p-4 min-h-[220px] space-y-3">
                    <div className="flex items-center justify-between">
                      <p className="text-xs text-muted-foreground">
                        {slotFields.length === 0 ? "Add exercises to this routine." : `${slotFields.length} exercise${slotFields.length !== 1 ? "s" : ""}`}
                      </p>
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        className="h-7 text-xs gap-1"
                        onClick={() => {
                          const ni = slotFields.length;
                          appendSlot({ exerciseId: 0, restBetweenSetsSecs: 60, sets: [{ type: "REPS", reps: 10, weightMode: "TOTAL" }] });
                          setExpandedSlot(ni);
                          setPickerSlotIndex(ni);
                        }}
                      >
                        <Plus className="h-3 w-3" /> Add exercise
                      </Button>
                    </div>

                    <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
                      <SortableContext items={slotFields.map((f) => f.id)} strategy={verticalListSortingStrategy}>
                        <div className="space-y-2 overflow-y-auto max-h-[320px] pr-1">
                          {slotFields.map((field, index) => (
                            <SlotCard
                              key={field.id}
                              id={field.id}
                              slotIndex={index}
                              collapsed={expandedSlot !== index}
                              onToggleCollapse={() => setExpandedSlot((prev) => prev === index ? null : index)}
                              control={control}
                              register={register}
                              setValue={setValue}
                              onRemove={() => removeSlot(index)}
                              onOpenPicker={() => setPickerSlotIndex(index)}
                              exercises={resolvedExercises}
                            />
                          ))}
                        </div>
                      </SortableContext>
                    </DndContext>

                    <div className="flex items-center gap-2 pt-1 border-t border-border/50">
                      <span className="text-xs text-muted-foreground shrink-0">Rest between exercises:</span>
                      <DurationInput value={restBetweenExercisesSecs} onChange={(v) => setValue("restBetweenExercisesSecs", v)} />
                    </div>
                  </div>
                )}
              </TabsContent>

              {/* ── Details ── */}
              <TabsContent value="details" className="mt-4">
                <DetailsPicker
                  equipment={equipment}
                  activities={activities}
                  trainingGoals={trainingGoals}
                  equipmentIds={equipmentIds}
                  activityIds={activityIds}
                  trainingGoalIds={trainingGoalIds}
                  onToggle={toggle}
                  onSync={handleSync}
                  syncCount={syncCount}
                  syncLabel="Sync from exercises"
                />
              </TabsContent>
            </div>
          </Tabs>

          <DialogFooter className="pt-2">
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={thumb.uploading}>
              {thumb.uploading ? <Spinner size="sm" label="" /> : initial?.id ? "Save routine" : "Add to program"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ── Copy routine picker ───────────────────────────────────────────────────────

function CopyRoutinePicker({
  onSelect,
  onBack,
}: {
  onSelect: (r: RoutineEntry) => void;
  onBack: () => void;
}) {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["routines-copy-picker", search, page],
    queryFn: () => routineApi.list({}, page, 12),
    staleTime: 30_000,
  });

  const routines = (data?.content ?? []).filter((r) =>
    search ? r.title.toLowerCase().includes(search.toLowerCase()) : true
  );

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={onBack}
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
        All data (exercises, sets, details, thumbnail) will be copied. You can change the thumbnail before saving.
      </p>

      {isLoading ? (
        <div className="flex justify-center py-6"><Spinner size="sm" label="" /></div>
      ) : routines.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-6">No routines found.</p>
      ) : (
        <div className="space-y-1.5 overflow-y-auto max-h-[280px]">
          {routines.map((r) => (
            <button
              key={r.id}
              type="button"
              onClick={() => onSelect(routineToEntry(r))}
              className="w-full flex items-center gap-3 px-3 py-2 rounded-lg border border-border hover:border-primary/60 hover:bg-accent/30 text-left transition-colors"
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
                  {r.slots.length} exercise{r.slots.length !== 1 ? "s" : ""} · {r.level[0] + r.level.slice(1).toLowerCase()}
                </p>
              </div>
              <span className="text-xs text-primary font-medium shrink-0">Copy</span>
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

// ── Helper: convert an existing RoutineResponse to a copyable RoutineEntry ────

function routineToEntry(r: RoutineResponse): RoutineEntry {
  return {
    localId: crypto.randomUUID(),
    id: undefined,
    title: r.title,
    level: r.level,
    description: r.description ?? "",
    thumbnailPreviewUrl: r.thumbnail?.url ?? null,
    thumbnailToken: undefined,
    thumbnailId: r.thumbnail?.id,
    removeThumbnail: false,
    restBetweenExercisesSecs: parseDuration(r.restBetweenExercises),
    slots: r.slots.map((s) => ({
      exerciseId: s.exerciseId,
      restBetweenSetsSecs: parseDuration(s.restBetweenSets),
      sets: s.sets.map((set) => ({
        type: set.type,
        reps: set.reps ?? undefined,
        weightKg: set.weightKg ?? undefined,
        weightMode: set.weightMode ?? "TOTAL",
        durationSecs: set.duration ? parseDuration(set.duration) : undefined,
      })),
    })),
    resolvedExercises: r.slots.map((s) => s.exercise).filter((e): e is ExerciseResponse => e != null),
    equipmentIds: r.equipment.map((e) => e.id),
    activityIds: r.activities.map((a) => a.id),
    trainingGoalIds: r.trainingGoals.map((g) => g.id),
  };
}

// ── Duplicate program picker ──────────────────────────────────────────────────

function DuplicateProgramPicker({
  onSelect,
  onBack,
}: {
  onSelect: (p: TrainingProgramResponse) => void;
  onBack: () => void;
}) {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["programs-duplicate-picker", page],
    queryFn: () => programApi.list({}, page, 12),
    staleTime: 30_000,
  });

  const programs = (data?.content ?? []).filter((p) =>
    search ? p.title.toLowerCase().includes(search.toLowerCase()) : true
  );

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={onBack}
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
        Program data, routines, and thumbnails will be copied. You can change thumbnails before saving.
      </p>

      {isLoading ? (
        <div className="flex justify-center py-6"><Spinner size="sm" label="" /></div>
      ) : programs.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-6">No programs found.</p>
      ) : (
        <div className="space-y-1.5 overflow-y-auto max-h-[280px]">
          {programs.map((p) => (
            <button
              key={p.id}
              type="button"
              onClick={() => onSelect(p)}
              className="w-full flex items-center gap-3 px-3 py-2 rounded-lg border border-border hover:border-primary/60 hover:bg-accent/30 text-left transition-colors"
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
                  {p.routines.length} routine{p.routines.length !== 1 ? "s" : ""} · {p.daysPerWeek}d/wk · {p.level[0] + p.level.slice(1).toLowerCase()}
                </p>
              </div>
              <span className="text-xs text-primary font-medium shrink-0">Duplicate</span>
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

function RoutineEntryRow({
  entry,
  onEdit,
  onRemove,
}: {
  entry: RoutineEntry;
  onEdit: () => void;
  onRemove: () => void;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: entry.localId });
  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
    zIndex: isDragging ? 50 : undefined,
  };

  return (
    <div ref={setNodeRef} style={style} className="rounded-lg border border-primary/40 bg-background hover:border-primary transition-colors">
      <div className="flex items-center gap-2 px-3 py-2.5">
        <GripVertical
          className="h-4 w-4 text-muted-foreground/50 cursor-grab active:cursor-grabbing touch-none shrink-0"
          {...attributes}
          {...listeners}
        />

        {/* Thumbnail */}
        <div className="h-8 w-8 rounded-md bg-muted overflow-hidden shrink-0 flex items-center justify-center">
          {entry.thumbnailPreviewUrl ? (
            <img src={entry.thumbnailPreviewUrl} className="h-full w-full object-cover" alt="" />
          ) : (
            <Layers className="h-3.5 w-3.5 text-muted-foreground" />
          )}
        </div>

        {/* Info */}
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium truncate">{entry.title || <span className="italic text-muted-foreground">Untitled routine</span>}</p>
          <p className="text-xs text-muted-foreground">
            {entry.slots.length} exercise{entry.slots.length !== 1 ? "s" : ""} · {entry.level[0] + entry.level.slice(1).toLowerCase()}
          </p>
        </div>

        {/* Actions */}
        <button type="button" onClick={onEdit} className="p-1.5 rounded-md text-muted-foreground hover:text-primary hover:bg-primary/10 transition-colors shrink-0" title="Edit routine">
          <Pencil className="h-3.5 w-3.5" />
        </button>
        <button type="button" onClick={onRemove} className="p-1.5 rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors shrink-0">
          <Trash2 className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  );
}

// ── Main dialog ───────────────────────────────────────────────────────────────

export function ProgramDialog({
  open,
  editing,
  seedFrom = null,
  onClose,
}: {
  open: boolean;
  editing: TrainingProgramResponse | null;
  /** When creating (not editing), preload form as a duplicate of this program. */
  seedFrom?: TrainingProgramResponse | null;
  onClose: () => void;
}) {
  const qc = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);
  const [routineEntries, setRoutineEntries] = useState<RoutineEntry[]>([]);
  const [subDialogOpen, setSubDialogOpen] = useState(false);
  const [editingEntry, setEditingEntry] = useState<RoutineEntry | null>(null);
  const [copyPickerOpen, setCopyPickerOpen] = useState(false);
  const [duplicatePickerOpen, setDuplicatePickerOpen] = useState(false);
  const [sourceThumbnailId, setSourceThumbnailId] = useState<number | undefined>();
  const thumb = useThumbnailUpload();

  const { data: equipment = [] } = useQuery({ queryKey: ["catalog", "equipment"], queryFn: catalogApi.listEquipment, staleTime: 60_000 });
  const { data: activities = [] } = useQuery({ queryKey: ["catalog", "activities"], queryFn: catalogApi.listActivities, staleTime: 60_000 });
  const { data: trainingGoals = [] } = useQuery({ queryKey: ["catalog", "training-goals"], queryFn: catalogApi.listTrainingGoals, staleTime: 60_000 });

  const { register, handleSubmit, watch, setValue, reset, formState: { errors } } = useForm<ProgramForm>({
    resolver: zodResolver(programSchema),
    defaultValues: { level: "INTERMEDIATE", daysPerWeek: 3, equipmentIds: [], activityIds: [], trainingGoalIds: [] },
  });

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  const handleDragEnd = (event: { active: { id: string | number }; over: { id: string | number } | null }) => {
    const { active, over } = event;
    if (over && active.id !== over.id) {
      setRoutineEntries((prev) => {
        const oi = prev.findIndex((e) => e.localId === active.id);
        const ni = prev.findIndex((e) => e.localId === over.id);
        if (oi !== -1 && ni !== -1) return arrayMove(prev, oi, ni);
        return prev;
      });
    }
  };

  const equipmentIds = watch("equipmentIds") ?? [];
  const activityIds = watch("activityIds") ?? [];
  const trainingGoalIds = watch("trainingGoalIds") ?? [];

  const toggle = (field: "equipmentIds" | "activityIds" | "trainingGoalIds", id: number) => {
    const cur = watch(field) ?? [];
    setValue(field, cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id]);
  };

  const handleSync = () => {
    setValue("equipmentIds", [...new Set(routineEntries.flatMap((r) => r.equipmentIds))]);
    setValue("activityIds",  [...new Set(routineEntries.flatMap((r) => r.activityIds))]);
    setValue("trainingGoalIds", [...new Set(routineEntries.flatMap((r) => r.trainingGoalIds))]);
  };

  const syncCount = useMemo(() => {
    return new Set([
      ...routineEntries.flatMap((r) => r.equipmentIds),
      ...routineEntries.flatMap((r) => r.activityIds),
      ...routineEntries.flatMap((r) => r.trainingGoalIds),
    ]).size;
  }, [routineEntries]);

  useEffect(() => {
    if (open) {
      setServerError(null);
      setSubDialogOpen(false);
      setCopyPickerOpen(false);
      setDuplicatePickerOpen(false);
      if (editing) {
        const sorted = [...editing.routines].sort((a, b) => (a.position ?? 0) - (b.position ?? 0));
        setRoutineEntries(
          sorted.map((r: RoutineResponse) => ({
            localId: crypto.randomUUID(),
            id: r.id,
            title: r.title,
            level: r.level,
            description: r.description ?? "",
            thumbnailPreviewUrl: r.thumbnail?.url ?? null,
            thumbnailToken: undefined,
            thumbnailId: r.thumbnail?.id,
            removeThumbnail: false,
            restBetweenExercisesSecs: parseDuration(r.restBetweenExercises),
            slots: r.slots.map((s) => ({
              exerciseId: s.exerciseId,
              restBetweenSetsSecs: parseDuration(s.restBetweenSets),
              sets: s.sets.map((set) => ({
                type: set.type,
                reps: set.reps ?? undefined,
                weightKg: set.weightKg ?? undefined,
                weightMode: set.weightMode ?? "TOTAL",
                durationSecs: set.duration ? parseDuration(set.duration) : undefined,
              })),
            })),
            resolvedExercises: r.slots.map((s) => s.exercise).filter((e): e is ExerciseResponse => e != null),
            equipmentIds: r.equipment.map((e) => e.id),
            activityIds: r.activities.map((a) => a.id),
            trainingGoalIds: r.trainingGoals.map((g) => g.id),
          }))
        );
        setSourceThumbnailId(undefined);
        thumb.reset(editing.thumbnail?.url ?? null);
        reset({
          title: editing.title,
          level: editing.level,
          description: editing.description ?? "",
          daysPerWeek: editing.daysPerWeek,
          equipmentIds: editing.equipment.map((e) => e.id),
          activityIds: editing.activities.map((a) => a.id),
          trainingGoalIds: editing.trainingGoals.map((g) => g.id),
        });
      } else if (seedFrom) {
        handleDuplicateProgram(seedFrom);
      } else {
        setSourceThumbnailId(undefined);
        thumb.reset(null);
        setRoutineEntries([]);
        reset({ level: "INTERMEDIATE", daysPerWeek: 3, equipmentIds: [], activityIds: [], trainingGoalIds: [] });
      }
    }
  }, [open, editing, seedFrom]);

  const handleDuplicateProgram = (source: TrainingProgramResponse) => {
    const sorted = [...source.routines].sort((a, b) => (a.position ?? 0) - (b.position ?? 0));
    setRoutineEntries(sorted.map(routineToEntry));
    setSourceThumbnailId(source.thumbnail?.id);
    thumb.reset(source.thumbnail?.url ?? null);
    reset({
      title: `${source.title} (copy)`,
      level: source.level,
      description: source.description ?? "",
      daysPerWeek: source.daysPerWeek,
      equipmentIds: source.equipment.map((e) => e.id),
      activityIds: source.activities.map((a) => a.id),
      trainingGoalIds: source.trainingGoals.map((g) => g.id),
    });
    setDuplicatePickerOpen(false);
  };

  const handleRoutineSave = (entry: RoutineEntry) => {
    setRoutineEntries((prev) => {
      const idx = prev.findIndex((e) => e.localId === entry.localId);
      if (idx !== -1) {
        const next = [...prev];
        next[idx] = entry;
        return next;
      }
      return [...prev, entry];
    });
  };

  const openAddRoutine = () => {
    setEditingEntry(null);
    setSubDialogOpen(true);
  };

  const openEditRoutine = (entry: RoutineEntry) => {
    setEditingEntry(entry);
    setSubDialogOpen(true);
  };

  const removeEntry = (localId: string) => {
    setRoutineEntries((prev) => prev.filter((e) => e.localId !== localId));
  };

  const mutation = useMutation({
    mutationFn: (data: ProgramForm) => {
      const routineDtos = routineEntries.map((r, i) => ({
        id: r.id,
        title: r.title,
        level: r.level,
        description: r.description || undefined,
        thumbnailUploadToken: r.thumbnailToken,
        thumbnailId:
          !r.id && !r.thumbnailToken && !r.removeThumbnail ? r.thumbnailId : undefined,
        removeThumbnail: r.removeThumbnail || undefined,
        position: i + 1,
        restBetweenExercises: formatDuration(r.restBetweenExercisesSecs),
        slots: r.slots.map((s, si) => ({
          exerciseId: s.exerciseId,
          position: si + 1,
          restBetweenSets: formatDuration(s.restBetweenSetsSecs),
          sets: s.sets.map((set) => ({
            type: set.type,
            reps: set.reps || undefined,
            weightKg: set.weightKg ?? undefined,
            weightMode: set.weightMode ?? "TOTAL",
            duration: formatDuration(set.durationSecs ?? 0),
          })),
        })),
        equipmentIds: r.equipmentIds,
        activityIds: r.activityIds,
        trainingGoalIds: r.trainingGoalIds,
      }));

      const payload = {
        title: data.title,
        level: data.level,
        description: data.description || undefined,
        thumbnailUploadToken: thumb.token,
        thumbnailId:
          !editing && !thumb.token && !thumb.removed ? sourceThumbnailId : undefined,
        removeThumbnail: thumb.removed,
        daysPerWeek: Number(data.daysPerWeek),
        routines: routineDtos,
        equipmentIds: data.equipmentIds,
        activityIds: data.activityIds,
        trainingGoalIds: data.trainingGoalIds,
      };
      return editing ? programApi.update(editing.id, payload) : programApi.create(payload);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["programs"] });
      onClose();
    },
    onError: (err) => setServerError(getErrorMessage(err)),
  });

  const selectClass = "flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:opacity-50";

  return (
    <>
      <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
        <DialogContent className="sm:max-w-2xl flex flex-col">
          <DialogHeader>
            <DialogTitle>{editing ? "Edit Program" : "New Program"}</DialogTitle>
          </DialogHeader>

          <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="flex flex-col gap-0 overflow-hidden flex-1">
            {serverError && <ApiError message={serverError} className="mb-2" />}

            {duplicatePickerOpen && !editing ? (
              <div className="rounded-lg border border-primary/40 bg-card/50 p-4 min-h-[300px] mx-1 mb-2">
                <DuplicateProgramPicker
                  onSelect={handleDuplicateProgram}
                  onBack={() => setDuplicatePickerOpen(false)}
                />
              </div>
            ) : (
            <Tabs defaultValue="info" className="flex-1 overflow-hidden flex flex-col">
              <TabsList className="mx-auto">
                <TabsTrigger value="info">Info</TabsTrigger>
                <TabsTrigger value="routines">
                  Routines {routineEntries.length > 0 && <span className="ml-1 text-xs text-primary">({routineEntries.length})</span>}
                </TabsTrigger>
                <TabsTrigger value="details">Details</TabsTrigger>
              </TabsList>

              <div className="flex-1 overflow-y-auto px-1 pb-2">

                {/* ── Info tab ── */}
                <TabsContent value="info" className="space-y-4 mt-4">
                  {!editing && (
                    <button
                      type="button"
                      onClick={() => setDuplicatePickerOpen(true)}
                      className="w-full flex items-center justify-between px-4 py-2.5 rounded-lg border border-dashed border-primary/40 bg-primary/5 hover:bg-primary/10 transition-colors"
                    >
                      <span className="flex items-center gap-2 text-xs font-medium text-primary">
                        <Copy className="h-3.5 w-3.5" />
                        Duplicate an existing program
                      </span>
                      <ChevronDown className="h-4 w-4 text-primary -rotate-90" />
                    </button>
                  )}

                  <div className="grid grid-cols-3 gap-3">
                    <div className="col-span-2 space-y-1.5">
                      <Label>Title</Label>
                      <Input {...register("title")} placeholder="e.g. 12-Week Strength" />
                      {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
                    </div>
                    <div className="space-y-1.5">
                      <Label>Level</Label>
                      <select {...register("level")} className={selectClass}>
                        {LEVELS.map((l) => <option key={l.value} value={l.value}>{l.label}</option>)}
                      </select>
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <Label>Days per week</Label>
                    <div className="flex items-center gap-2">
                      <Input type="number" min={1} max={7} {...register("daysPerWeek")} className="w-20" />
                      <span className="text-sm text-muted-foreground">days/week</span>
                    </div>
                    {errors.daysPerWeek && <p className="text-xs text-destructive">{errors.daysPerWeek.message}</p>}
                  </div>

                  <div className="space-y-1.5">
                    <Label>Description <span className="text-muted-foreground">(optional)</span></Label>
                    <Textarea {...register("description")} rows={3} placeholder="Brief overview of this program…" />
                  </div>

                  <div className="space-y-1.5">
                    <Label>Thumbnail <span className="text-muted-foreground">(optional)</span></Label>
                    <div className="flex items-center gap-3">
                      <div
                        className="h-16 w-16 rounded-lg border border-border bg-muted flex items-center justify-center cursor-pointer hover:bg-accent transition-colors shrink-0 overflow-hidden"
                        onClick={() => thumb.fileRef.current?.click()}
                      >
                        {thumb.uploading ? <Spinner size="sm" label="" /> : thumb.preview ? (
                          <img src={thumb.preview} alt="thumb" className="h-full w-full object-cover" />
                        ) : (
                          <ImagePlus className="h-5 w-5 text-muted-foreground" />
                        )}
                      </div>
                      <div className="flex flex-col gap-1">
                        <p className="text-xs text-muted-foreground">Click to {thumb.preview ? "change" : "upload"}{thumb.token && <span className="text-primary ml-1">(ready)</span>}</p>
                        {thumb.preview && !thumb.uploading && (
                          <button type="button" onClick={() => { setSourceThumbnailId(undefined); thumb.remove(); }} className="text-xs text-destructive hover:underline text-left">Remove</button>
                        )}
                      </div>
                      <input ref={thumb.fileRef} type="file" accept="image/*" className="hidden" onChange={(e) => { setSourceThumbnailId(undefined); void thumb.handleFile(e); }} />
                    </div>
                    {thumb.error && <p className="text-xs text-destructive">{thumb.error}</p>}
                  </div>
                </TabsContent>

                {/* ── Routines tab ── */}
                <TabsContent value="routines" className="mt-4">
                  <div className="rounded-lg border border-primary/40 bg-card/50 p-4 min-h-[220px] space-y-3">

                    {copyPickerOpen ? (
                      <CopyRoutinePicker
                        onSelect={(entry) => {
                          setCopyPickerOpen(false);
                          setEditingEntry(entry);
                          setSubDialogOpen(true);
                        }}
                        onBack={() => setCopyPickerOpen(false)}
                      />
                    ) : (
                      <>
                        <div className="flex items-center justify-between gap-2">
                          <p className="text-xs text-muted-foreground">
                            {routineEntries.length === 0
                              ? "Add routines to build your program."
                              : `${routineEntries.length} routine${routineEntries.length !== 1 ? "s" : ""}`}
                          </p>
                          <div className="flex items-center gap-2">
                            <Button
                              type="button"
                              size="sm"
                              variant="outline"
                              className="h-7 text-xs gap-1"
                              onClick={() => setCopyPickerOpen(true)}
                            >
                              <Layers className="h-3 w-3" /> Copy routine
                            </Button>
                            <Button
                              type="button"
                              size="sm"
                              variant="outline"
                              className="h-7 text-xs gap-1"
                              onClick={openAddRoutine}
                            >
                              <Plus className="h-3 w-3" /> New routine
                            </Button>
                          </div>
                        </div>

                        <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
                          <SortableContext items={routineEntries.map((e) => e.localId)} strategy={verticalListSortingStrategy}>
                            <div className="space-y-2 overflow-y-auto max-h-[320px] pr-1">
                              {routineEntries.map((entry) => (
                                <RoutineEntryRow
                                  key={entry.localId}
                                  entry={entry}
                                  onEdit={() => openEditRoutine(entry)}
                                  onRemove={() => removeEntry(entry.localId)}
                                />
                              ))}
                            </div>
                          </SortableContext>
                        </DndContext>
                      </>
                    )}
                  </div>
                </TabsContent>

                {/* ── Details tab ── */}
                <TabsContent value="details" className="mt-4">
                  <DetailsPicker
                    equipment={equipment}
                    activities={activities}
                    trainingGoals={trainingGoals}
                    equipmentIds={equipmentIds}
                    activityIds={activityIds}
                    trainingGoalIds={trainingGoalIds}
                    onToggle={toggle}
                    onSync={handleSync}
                    syncCount={syncCount}
                    syncLabel="Sync from routines"
                  />
                </TabsContent>
              </div>
            </Tabs>
            )}

            <DialogFooter className="pt-2">
              <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
              {!duplicatePickerOpen && (
                <Button type="submit" disabled={mutation.isPending || thumb.uploading}>
                  {mutation.isPending ? <Spinner size="sm" label="" /> : editing ? "Save changes" : "Create program"}
                </Button>
              )}
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Sub-dialog rendered as sibling to avoid nested Dialog issues */}
      <ProgramRoutineSubDialog
        open={subDialogOpen}
        initial={editingEntry}
        onSave={handleRoutineSave}
        onClose={() => setSubDialogOpen(false)}
        equipment={equipment}
        activities={activities}
        trainingGoals={trainingGoals}
      />
    </>
  );
}
