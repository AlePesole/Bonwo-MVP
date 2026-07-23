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
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
  arrayMove,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { routineApi, formatDuration, parseDuration } from "./api";
import { exerciseApi } from "@/exercise/api";
import { catalogApi } from "@/catalog/api";
import { api, getErrorMessage } from "@/lib/axios";
import { cn } from "@/lib/utils";
import type {
  ExerciseResponse,
  ImageUploadResponse,
  Level,
  RoutineResponse,
  SetType,
  WeightMode,
} from "@/types/api";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select } from "@/components/ui/select";
import { ApiError } from "@/components/ApiError";
import { Spinner } from "@/components/Spinner";
import { ChevronDown, Dumbbell, GripVertical, ImagePlus, Layers, Plus, Trash2, X } from "lucide-react";

// ── Constants ─────────────────────────────────────────────────────────────────

const LEVELS: { value: Level; label: string }[] = [
  { value: "BEGINNER", label: "Beginner" },
  { value: "INTERMEDIATE", label: "Intermediate" },
  { value: "ADVANCED", label: "Advanced" },
];

const SET_TYPES: { value: SetType; label: string; hint: string }[] = [
  { value: "REPS", label: "Reps", hint: "Fixed number of reps" },
  { value: "TIMED", label: "Timed", hint: "Hold for X seconds" },
  { value: "AMRAP", label: "AMRAP", hint: "As many reps as possible" },
  { value: "FAILURE", label: "To failure", hint: "Go until failure" },
];

const WEIGHT_MODES: { value: WeightMode; label: string }[] = [
  { value: "TOTAL", label: "Total" },
  { value: "PER_SIDE", label: "Per side" },
];

// ── Zod schema ────────────────────────────────────────────────────────────────

const setSchema = z.object({
  type: z.enum(["REPS", "TIMED", "AMRAP", "FAILURE"]),
  reps: z.coerce.number().int().min(0).optional(),
  weightKg: z.coerce.number().min(0).nullable().optional(),
  weightMode: z.enum(["TOTAL", "PER_SIDE"]).optional(),
  durationSecs: z.coerce.number().int().min(0).optional(),
});

const slotSchema = z.object({
  exerciseId: z.coerce.number().min(1, "Select an exercise"),
  restBetweenSetsSecs: z.coerce.number().int().min(0).optional(),
  sets: z.array(setSchema).min(1, "At least one set required"),
});

const routineSchema = z.object({
  title: z.string().min(1, "Title is required").max(200),
  level: z.enum(["BEGINNER", "INTERMEDIATE", "ADVANCED"]),
  description: z.string().optional(),
  restBetweenExercisesSecs: z.coerce.number().int().min(0).optional(),
  slots: z.array(slotSchema).optional(),
  equipmentIds: z.array(z.number()).optional(),
  activityIds: z.array(z.number()).optional(),
  trainingGoalIds: z.array(z.number()).optional(),
});

type RoutineForm = z.infer<typeof routineSchema>;

// ── Duration input ─────────────────────────────────────────────────────────────

function DurationInput({
  value,
  onChange,
  placeholder,
}: {
  value: number;
  onChange: (v: number) => void;
  placeholder?: string;
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
        placeholder={placeholder ?? "0"}
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
  control: ReturnType<typeof useForm<RoutineForm>>["control"];
  register: ReturnType<typeof useForm<RoutineForm>>["register"];
  onRemove: () => void;
}) {
  const type = useWatch({ control, name: `slots.${slotIndex}.sets.${setIndex}.type` }) as SetType;
  const weightMode = useWatch({ control, name: `slots.${slotIndex}.sets.${setIndex}.weightMode` }) as WeightMode;
  const weightKg = useWatch({ control, name: `slots.${slotIndex}.sets.${setIndex}.weightKg` }) as number | undefined;
  const selectClass = "h-9 rounded-md border border-input bg-background px-2 text-xs focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring";
  const inputClass = "h-9 w-16 rounded-md border border-input bg-background px-2 text-xs text-center";

  return (
    <div className="flex items-center gap-1.5 flex-wrap">
      <span className="text-xs text-muted-foreground w-4 text-right">{setIndex + 1}.</span>

      {/* Type */}
      <Select {...register(`slots.${slotIndex}.sets.${setIndex}.type`)} className={selectClass} style={{ width: 90 }}>
        {SET_TYPES.map((t) => (
          <option key={t.value} value={t.value}>{t.label}</option>
        ))}
      </Select>

      {/* Reps — show for REPS / FAILURE */}
      {(type === "REPS" || type === "FAILURE") && (
        <>
          <input
            type="number"
            min={0}
            {...register(`slots.${slotIndex}.sets.${setIndex}.reps`)}
            placeholder="Reps"
            className={inputClass}
          />
          <span className="text-xs text-muted-foreground">reps</span>
        </>
      )}

      {/* Duration — show for TIMED / AMRAP */}
      {(type === "TIMED" || type === "AMRAP") && (
        <>
          <input
            type="number"
            min={0}
            {...register(`slots.${slotIndex}.sets.${setIndex}.durationSecs`)}
            placeholder="Sec"
            className={inputClass}
          />
          <span className="text-xs text-muted-foreground">sec</span>
        </>
      )}

      {/* Weight — not for AMRAP */}
      {type !== "AMRAP" && (
        <>
          <input
            type="number"
            min={0}
            step={0.5}
            {...register(`slots.${slotIndex}.sets.${setIndex}.weightKg`)}
            placeholder="kg"
            className={inputClass}
          />
          <Select {...register(`slots.${slotIndex}.sets.${setIndex}.weightMode`)} className={selectClass} style={{ width: 80 }}>
            {WEIGHT_MODES.map((m) => (
              <option key={m.value} value={m.value}>{m.label}</option>
            ))}
          </Select>
          <span className="text-xs text-muted-foreground">weight</span>
        </>
      )}

      <button type="button" onClick={onRemove} className="text-muted-foreground hover:text-destructive transition-colors ml-auto">
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}

// ── Exercise picker ───────────────────────────────────────────────────────────

const LEVEL_OPTS = [
  { value: "", label: "All levels" },
  { value: "BEGINNER", label: "Beginner" },
  { value: "INTERMEDIATE", label: "Intermediate" },
  { value: "ADVANCED", label: "Advanced" },
];

const LEVEL_BADGE: Record<string, string> = {
  BEGINNER: "bg-emerald-500/20 text-emerald-400",
  INTERMEDIATE: "bg-amber-500/20 text-amber-400",
  ADVANCED: "bg-red-500/20 text-red-400",
};

function ExercisePicker({
  onSelect,
  onBack,
  excludeIds,
}: {
  onSelect: (ex: ExerciseResponse) => void;
  onBack: () => void;
  excludeIds: number[];
}) {
  const [title, setTitle] = useState("");
  const [muscleGroupId, setMuscleGroupId] = useState<number | "">("");
  const [muscleSubGroupId, setMuscleSubGroupId] = useState<number | "">("");
  const [page, setPage] = useState(0);

  // Reset page when any filter changes
  const handleTitleChange = (v: string) => { setTitle(v); setPage(0); };
  const handleGroupChange = (v: number | "") => { setMuscleGroupId(v); setMuscleSubGroupId(""); setPage(0); };
  const handleSubGroupChange = (v: number | "") => { setMuscleSubGroupId(v); setPage(0); };

  const { data: muscleGroups = [] } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: catalogApi.listMuscleGroups,
    staleTime: 60_000,
  });

  const selectedGroup = muscleGroups.find((g) => g.id === muscleGroupId);

  const filter = {
    title: title || undefined,
    muscleGroupId: muscleGroupId || undefined,
    muscleSubGroupId: muscleSubGroupId || undefined,
  };

  const { data, isLoading } = useQuery({
    queryKey: ["exercises-picker", filter, page],
    queryFn: () => exerciseApi.list(filter, page, 12),
    staleTime: 30_000,
  });

  const exercises = data?.content ?? [];

  const selectClass = "h-7 rounded-md border border-input bg-background px-2 text-xs focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring flex-1 min-w-0";

  return (
    <div className="space-y-3">
      {/* Back + title search */}
      <div className="flex items-center gap-2">
        <button type="button" onClick={onBack} className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors shrink-0">
          <ChevronDown className="h-4 w-4 rotate-90" /> Back
        </button>
        <Input
          value={title}
          onChange={(e) => handleTitleChange(e.target.value)}
          placeholder="Search exercises…"
          className="h-8 text-sm flex-1"
          autoFocus
        />
      </div>

      {/* Filter row */}
      <div className="flex items-center gap-2 flex-wrap">
        <select
          value={muscleGroupId}
          onChange={(e) => handleGroupChange(e.target.value ? Number(e.target.value) : "")}
          className={selectClass}
          style={{ maxWidth: 140 }}
        >
          <option value="">All muscles</option>
          {muscleGroups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
        </select>

        {selectedGroup && selectedGroup.subGroups.length > 0 && (
          <select
            value={muscleSubGroupId}
            onChange={(e) => handleSubGroupChange(e.target.value ? Number(e.target.value) : "")}
            className={selectClass}
            style={{ maxWidth: 140 }}
          >
            <option value="">All sub-groups</option>
            {selectedGroup.subGroups.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        )}

        {(title || muscleGroupId) && (
          <button
            type="button"
            onClick={() => { setTitle(""); setMuscleGroupId(""); setMuscleSubGroupId(""); setPage(0); }}
            className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-0.5 shrink-0"
          >
            <X className="h-3 w-3" /> Clear
          </button>
        )}
      </div>

      {/* Results */}
      {isLoading ? (
        <div className="flex justify-center py-6"><Spinner size="sm" label="" /></div>
      ) : exercises.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-6">No exercises found.</p>
      ) : (
        <div className="space-y-1.5 overflow-y-auto max-h-[240px]">
          {exercises.map((ex) => {
            const disabled = excludeIds.includes(ex.id);
            return (
              <button
                key={ex.id}
                type="button"
                disabled={disabled}
                onClick={() => onSelect(ex)}
                className={cn(
                  "w-full flex items-center gap-3 px-3 py-2 rounded-lg border text-left transition-colors",
                  disabled
                    ? "border-border opacity-40 cursor-not-allowed"
                    : "border-border hover:border-primary/50 hover:bg-accent/30"
                )}
              >
                <div className="h-8 w-8 rounded-md bg-muted flex items-center justify-center shrink-0 overflow-hidden">
                  {ex.thumbnail?.url ? (
                    <img src={ex.thumbnail.url} alt={ex.title} className="h-full w-full object-cover" />
                  ) : (
                    <Dumbbell className="h-4 w-4 text-muted-foreground/50" />
                  )}
                </div>
                <span className="text-sm flex-1 truncate">{ex.title}</span>
                <span className={cn("text-[10px] font-bold px-1.5 py-0.5 rounded-full shrink-0", LEVEL_BADGE[ex.level] ?? "bg-muted text-muted-foreground")}>
                  {ex.level[0] + ex.level.slice(1).toLowerCase()}
                </span>
                {disabled && <span className="text-xs text-muted-foreground">added</span>}
              </button>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex justify-center items-center gap-2">
          <button type="button" disabled={data.first} onClick={() => setPage((p) => p - 1)}
            className="text-xs text-muted-foreground hover:text-foreground disabled:opacity-40 px-2 py-1 rounded border border-border">
            ← Prev
          </button>
          <span className="text-xs text-muted-foreground">{page + 1} / {data.totalPages}</span>
          <button type="button" disabled={data.last} onClick={() => setPage((p) => p + 1)}
            className="text-xs text-muted-foreground hover:text-foreground disabled:opacity-40 px-2 py-1 rounded border border-border">
            Next →
          </button>
        </div>
      )}
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
  control: ReturnType<typeof useForm<RoutineForm>>["control"];
  register: ReturnType<typeof useForm<RoutineForm>>["register"];
  setValue: ReturnType<typeof useForm<RoutineForm>>["setValue"];
  onRemove: () => void;
  onOpenPicker: () => void;
  exercises: ExerciseResponse[];
}) {
  const exerciseId = useWatch({ control, name: `slots.${slotIndex}.exerciseId` }) ?? 0;
  const exercise = useMemo(() => exercises.find((e) => e.id === exerciseId) ?? null, [exerciseId, exercises]);

  const { fields: setFields, append: appendSet, remove: removeSet } = useFieldArray({
    control,
    name: `slots.${slotIndex}.sets`,
  });

  const restSecs = useWatch({ control, name: `slots.${slotIndex}.restBetweenSetsSecs` }) ?? 0;

  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
    zIndex: isDragging ? 50 : undefined,
  };

  return (
    <div ref={setNodeRef} style={style} className="rounded-lg border border-primary/40 bg-background p-3 space-y-2.5 hover:border-primary transition-colors">
      {/* Header */}
      <div className="flex items-start gap-2">
        {/* Left column: grip + collapse arrow */}
        <div className="flex flex-col items-center gap-0.5 pt-1.5 shrink-0">
          <GripVertical
            className="h-4 w-4 text-muted-foreground/50 cursor-grab active:cursor-grabbing touch-none"
            {...attributes}
            {...listeners}
          />
          <button
            type="button"
            onClick={() => onToggleCollapse()}
            className="text-muted-foreground hover:text-foreground transition-colors"
            title={collapsed ? "Expand sets" : "Collapse sets"}
          >
            <ChevronDown className={cn("h-3.5 w-3.5 transition-transform", collapsed && "-rotate-90")} />
          </button>
        </div>

        {/* Right: exercise picker + trash */}
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

      {/* Sets count badge when collapsed */}
      {collapsed && setFields.length > 0 && (
        <p className="pl-6 text-xs text-muted-foreground">{setFields.length} set{setFields.length !== 1 ? "s" : ""}</p>
      )}

      {/* Sets */}
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

          {/* Rest between sets */}
          <div className="pl-6 flex items-center gap-2">
            <span className="text-xs text-muted-foreground">Rest between sets:</span>
            <DurationInput
              value={restSecs}
              onChange={(v) => setValue(`slots.${slotIndex}.restBetweenSetsSecs`, v)}
            />
          </div>
        </>
      )}
    </div>
  );
}

// ── Details multi-picker (reusing pattern from ExerciseDialog) ────────────────

function DetailsTabContent({
  equipment,
  activities,
  trainingGoals,
  equipmentIds,
  activityIds,
  trainingGoalIds,
  onToggle,
  onSync,
  syncCount,
}: {
  equipment: { id: number; name: string; icon?: { url: string } | null }[];
  activities: { id: number; name: string; icon?: { url: string } | null }[];
  trainingGoals: { id: number; name: string; icon?: { url: string } | null }[];
  equipmentIds: number[];
  activityIds: number[];
  trainingGoalIds: number[];
  onToggle: (field: "equipmentIds" | "activityIds" | "trainingGoalIds", id: number) => void;
  onSync: () => void;
  syncCount: number;
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
          {/* Sync button */}
          <button
            type="button"
            onClick={onSync}
            disabled={syncCount === 0}
            className="w-full flex items-center justify-between px-4 py-2.5 rounded-lg border border-dashed border-primary/40 bg-primary/5 hover:bg-primary/10 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <span className="text-xs font-medium text-primary">Sync from exercises</span>
            {syncCount > 0 && (
              <span className="text-[10px] text-muted-foreground">{syncCount} item{syncCount !== 1 ? "s" : ""} found</span>
            )}
          </button>

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
                  <span className="bg-primary/20 text-primary text-[10px] font-bold rounded-full px-2 py-0.5">{selected.length}</span>
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

// ── Thumbnail upload ──────────────────────────────────────────────────────────

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

// ── Main dialog ───────────────────────────────────────────────────────────────

export function RoutineDialog({
  open,
  editing,
  onClose,
}: {
  open: boolean;
  editing: RoutineResponse | null;
  onClose: () => void;
}) {
  const qc = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);
  const [pickerSlotIndex, setPickerSlotIndex] = useState<number | null>(null);
  const [expandedSlot, setExpandedSlot] = useState<number | null>(0);
  const thumb = useThumbnailUpload();

  const { data: equipment = [] } = useQuery({ queryKey: ["catalog", "equipment"], queryFn: catalogApi.listEquipment, staleTime: 60_000 });
  const { data: activities = [] } = useQuery({ queryKey: ["catalog", "activities"], queryFn: catalogApi.listActivities, staleTime: 60_000 });
  const { data: trainingGoals = [] } = useQuery({ queryKey: ["catalog", "training-goals"], queryFn: catalogApi.listTrainingGoals, staleTime: 60_000 });

  const { register, handleSubmit, control, watch, setValue, reset, formState: { errors } } = useForm<RoutineForm>({
    resolver: zodResolver(routineSchema),
    defaultValues: { level: "INTERMEDIATE", slots: [], equipmentIds: [], activityIds: [], trainingGoalIds: [] },
  });

  const { fields: slotFields, append: appendSlot, remove: removeSlot, move: moveSlot } = useFieldArray({ control, name: "slots" });

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (over && active.id !== over.id) {
      const oldIndex = slotFields.findIndex((f) => f.id === active.id);
      const newIndex = slotFields.findIndex((f) => f.id === over.id);
      if (oldIndex !== -1 && newIndex !== -1) moveSlot(oldIndex, newIndex);
    }
  };

  const equipmentIds = watch("equipmentIds") ?? [];
  const activityIds = watch("activityIds") ?? [];
  const trainingGoalIds = watch("trainingGoalIds") ?? [];
  const restBetweenExercisesSecs = watch("restBetweenExercisesSecs") ?? 0;

  // Track exercises added to slots for the picker
  const slotsWatched = useWatch({ control, name: "slots" });
  const addedExerciseIds = (slotsWatched ?? []).map((s) => s?.exerciseId).filter(Boolean) as number[];

  // Fetch exercises referenced in editing routine
  const [resolvedExercises, setResolvedExercises] = useState<ExerciseResponse[]>([]);

  const toggle = (field: "equipmentIds" | "activityIds" | "trainingGoalIds", id: number) => {
    const cur = watch(field) ?? [];
    setValue(field, cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id]);
  };

  const handleSync = () => {
    const exercises = resolvedExercises.filter((e) =>
      (slotsWatched ?? []).some((s) => s?.exerciseId === e.id)
    );
    setValue("equipmentIds",    [...new Set(exercises.flatMap((e) => e.equipment.map((x) => x.id)))]);
    setValue("activityIds",     [...new Set(exercises.flatMap((e) => e.activities.map((x) => x.id)))]);
    setValue("trainingGoalIds", [...new Set(exercises.flatMap((e) => e.trainingGoals.map((x) => x.id)))]);
  };

  const syncCount = useMemo(() => {
    const exercises = resolvedExercises.filter((e) =>
      (slotsWatched ?? []).some((s) => s?.exerciseId === e.id)
    );
    return new Set([
      ...exercises.flatMap((e) => e.equipment.map((x) => x.id)),
      ...exercises.flatMap((e) => e.activities.map((x) => x.id)),
      ...exercises.flatMap((e) => e.trainingGoals.map((x) => x.id)),
    ]).size;
  }, [resolvedExercises, slotsWatched]);

  useEffect(() => {
    if (open) {
      thumb.reset(editing?.thumbnail?.url ?? null);
      setServerError(null);
      setPickerSlotIndex(null);
      if (editing) {
        const resolved = editing.slots
          .map((s) => s.exercise)
          .filter((e): e is ExerciseResponse => e != null);
        setResolvedExercises(resolved);
        reset({
          title: editing.title,
          level: editing.level,
          description: editing.description ?? "",
          restBetweenExercisesSecs: parseDuration(editing.restBetweenExercises),
          slots: editing.slots.map((s, i) => ({
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
          equipmentIds: editing.equipment.map((e) => e.id),
          activityIds: editing.activities.map((a) => a.id),
          trainingGoalIds: editing.trainingGoals.map((g) => g.id),
        });
      } else {
        setResolvedExercises([]);
        reset({ level: "INTERMEDIATE", slots: [], equipmentIds: [], activityIds: [], trainingGoalIds: [] });
      }
    }
  }, [open, editing]);

  const mutation = useMutation({
    mutationFn: (data: RoutineForm) => {
      const payload = {
        title: data.title,
        level: data.level,
        description: data.description || undefined,
        thumbnailUploadToken: thumb.token,
        removeThumbnail: thumb.removed,
        restBetweenExercises: formatDuration(data.restBetweenExercisesSecs ?? 0),
        slots: (data.slots ?? []).map((s, i) => ({
          exerciseId: Number(s.exerciseId),
          position: i + 1,
          restBetweenSets: formatDuration(s.restBetweenSetsSecs ?? 0),
          sets: s.sets.map((set) => ({
            type: set.type,
            reps: set.reps || undefined,
            weightKg: set.weightKg ?? undefined,
            weightMode: set.weightMode || "TOTAL",
            duration: formatDuration(set.durationSecs ?? 0),
          })),
        })),
        equipmentIds: data.equipmentIds,
        activityIds: data.activityIds,
        trainingGoalIds: data.trainingGoalIds,
      };
      return editing ? routineApi.update(editing.id, payload) : routineApi.create(payload);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["routines"] });
      onClose();
    },
    onError: (err) => setServerError(getErrorMessage(err)),
  });

  const selectClass = "flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:opacity-50";

  const handleExerciseSelect = (ex: ExerciseResponse) => {
    if (pickerSlotIndex === null) return;
    setValue(`slots.${pickerSlotIndex}.exerciseId`, ex.id);
    setResolvedExercises((prev) => prev.some((e) => e.id === ex.id) ? prev : [...prev, ex]);
    setPickerSlotIndex(null);
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-2xl flex flex-col">
        <DialogHeader>
          <DialogTitle>{editing ? "Edit Routine" : "New Routine"}</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="flex flex-col gap-4">
          {serverError && <ApiError message={serverError} />}

          <Tabs defaultValue="info">
            <TabsList className="w-full">
              <TabsTrigger value="info" className="flex-1">Info</TabsTrigger>
              <TabsTrigger value="slots" className="flex-1">
                Exercises
                {slotFields.length > 0 && (
                  <span className="ml-1.5 bg-primary/20 text-primary text-[10px] font-bold rounded-full px-1.5">{slotFields.length}</span>
                )}
              </TabsTrigger>
              <TabsTrigger value="details" className="flex-1">Details</TabsTrigger>
            </TabsList>

            {/* ── Info tab ── */}
            <TabsContent value="info" className="space-y-4 mt-4">
              <div className="grid grid-cols-3 gap-3">
                <div className="col-span-2 space-y-1.5">
                  <Label>Title</Label>
                  <Input {...register("title")} placeholder="e.g. Push Day A" />
                  {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
                </div>
                <div className="space-y-1.5">
                  <Label>Level</Label>
                  <Select {...register("level")} className={selectClass}>
                    {LEVELS.map((l) => (
                      <option key={l.value} value={l.value}>{l.label}</option>
                    ))}
                  </Select>
                </div>
              </div>

              <div className="space-y-1.5">
                <Label>Description <span className="text-muted-foreground">(optional)</span></Label>
                <Textarea {...register("description")} rows={3} placeholder="Brief overview of this routine…" />
              </div>

              {/* Thumbnail */}
              <div className="space-y-1.5">
                <Label>Thumbnail <span className="text-muted-foreground">(optional)</span></Label>
                <div className="flex items-center gap-3">
                  <div
                    className="h-16 w-24 rounded-lg border border-border bg-muted flex items-center justify-center cursor-pointer hover:bg-accent transition-colors shrink-0 overflow-hidden"
                    onClick={() => thumb.fileRef.current?.click()}
                  >
                    {thumb.uploading ? <Spinner size="sm" label="" /> : thumb.preview ? (
                      <img src={thumb.preview} alt="thumb" className="h-full w-full object-cover" />
                    ) : (
                      <ImagePlus className="h-5 w-5 text-muted-foreground" />
                    )}
                  </div>
                  <div className="flex flex-col gap-1">
                    <p className="text-xs text-muted-foreground">
                      Click to {thumb.preview ? "change" : "upload"}
                      {thumb.token && <span className="text-primary ml-1">(ready)</span>}
                    </p>
                    {thumb.preview && !thumb.uploading && (
                      <button type="button" onClick={thumb.remove} className="text-xs text-destructive hover:underline text-left">Remove</button>
                    )}
                  </div>
                  <input ref={thumb.fileRef} type="file" accept="image/*" className="hidden" onChange={thumb.handleFile} />
                </div>
                {thumb.error && <p className="text-xs text-destructive">{thumb.error}</p>}
              </div>
            </TabsContent>

            {/* ── Slots/Exercises tab ── */}
            <TabsContent value="slots" className="mt-4">
              {pickerSlotIndex !== null ? (
                <div className="rounded-lg border border-primary/40 bg-card/50 p-4 min-h-[300px]">
                  <ExercisePicker
                    onSelect={handleExerciseSelect}
                    onBack={() => setPickerSlotIndex(null)}
                    excludeIds={[]}
                  />
                </div>
              ) : (
                <div className="rounded-lg border border-primary/40 bg-card/50 p-4 min-h-[220px] space-y-3">
                  <div className="flex items-center justify-between">
                    <p className="text-xs text-muted-foreground">
                      {slotFields.length === 0
                        ? "Add exercises to this routine."
                        : `${slotFields.length} exercise${slotFields.length !== 1 ? "s" : ""}`}
                    </p>
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      className="h-7 text-xs gap-1"
                      onClick={() => {
                        const newIndex = slotFields.length;
                        appendSlot({ exerciseId: 0, restBetweenSetsSecs: 60, sets: [{ type: "REPS", reps: 10, weightMode: "TOTAL" }] });
                        setExpandedSlot(newIndex);
                        setPickerSlotIndex(newIndex);
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

                  {/* Rest between exercises */}
                  <div className="flex items-center gap-2 pt-1 border-t border-border/50 mt-1">
                    <span className="text-xs text-muted-foreground shrink-0">Rest between exercises:</span>
                    <DurationInput
                      value={restBetweenExercisesSecs}
                      onChange={(v) => setValue("restBetweenExercisesSecs", v)}
                    />
                  </div>
                </div>
              )}
            </TabsContent>

            {/* ── Details tab ── */}
            <TabsContent value="details" className="mt-4">
              <DetailsTabContent
                equipment={equipment}
                activities={activities}
                trainingGoals={trainingGoals}
                equipmentIds={equipmentIds}
                activityIds={activityIds}
                trainingGoalIds={trainingGoalIds}
                onToggle={toggle}
                onSync={handleSync}
                syncCount={syncCount}
              />
            </TabsContent>
          </Tabs>

          <DialogFooter className="pt-2">
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={mutation.isPending || thumb.uploading}>
              {mutation.isPending ? <Spinner size="sm" label="" /> : editing ? "Save changes" : "Create routine"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
