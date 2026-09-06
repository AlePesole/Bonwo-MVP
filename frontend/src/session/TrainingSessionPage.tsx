import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
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
import { displayDuration, formatDuration, parseDuration } from "@/routine/api";
import { ExercisePicker } from "@/routine/RoutineDialog";
import { sessionApi } from "@/session/api";
import { cn, formatTimeAgo } from "@/lib/utils";
import { getErrorMessage } from "@/lib/axios";
import type {
  ExerciseResponse,
  SetType,
  TrainingSessionResponse,
  TrainingSetDto,
  TrainingSetResponse,
  TrainingSlotDto,
  TrainingSlotResponse,
  WeightMode,
} from "@/types/api";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/components/ApiError";
import { PageSpinner, Spinner } from "@/components/Spinner";
import {
  AlertTriangle,
  ArrowLeft,
  Check,
  CheckCircle2,
  Circle,
  Clock,
  Dumbbell,
  GripVertical,
  Pencil,
  Play,
  Plus,
  Timer,
  Trash2,
  X,
  Eye,
} from "lucide-react";
import { ExerciseDetailDialog } from "@/exercise/ExerciseDetailDialog";

const SET_LABEL: Record<SetType, string> = {
  REPS: "Reps",
  TIMED: "Timed",
  AMRAP: "AMRAP",
  FAILURE: "To failure",
};

const fieldClass =
  "h-7 w-14 rounded-md border border-border/80 bg-background/80 px-1.5 text-xs text-center tabular-nums focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:opacity-60";
const selectClass =
  "h-7 rounded-md border border-border/80 bg-background/80 px-1.5 text-[10px] focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:opacity-60";

function setDescription(set: TrainingSetResponse): string {
  const parts: string[] = [];
  if (set.type === "REPS" || set.type === "FAILURE") {
    if (set.reps) parts.push(`${set.reps} reps`);
  }
  if (set.type === "TIMED" || set.type === "AMRAP") {
    if (set.duration) parts.push(displayDuration(set.duration));
  }
  if (set.type !== "AMRAP") {
    if (set.weightKg && set.weightMode === "PER_SIDE") {
      parts.push(`${set.weightKg}kg x2`);
    } else if (set.totalWeightKg) {
      parts.push(`${set.totalWeightKg}kg`);
    }
  }
  return parts.join(" · ") || SET_LABEL[set.type];
}

type SetPatch = Partial<
  Pick<TrainingSetResponse, "reps" | "weightKg" | "weightMode" | "duration" | "done">
>;

function applySetPatch(set: TrainingSetResponse, patch: SetPatch): TrainingSetResponse {
  const next: TrainingSetResponse = { ...set, ...patch };
  if ("weightKg" in patch || "weightMode" in patch) {
    if (next.weightKg == null) next.totalWeightKg = null;
    else if (next.weightMode === "PER_SIDE") next.totalWeightKg = next.weightKg * 2;
    else next.totalWeightKg = next.weightKg;
  }
  return next;
}

function defaultSet(from?: TrainingSetResponse): TrainingSetResponse {
  if (from) return { ...from, done: false };
  return {
    type: "REPS",
    reps: 10,
    weightKg: null,
    weightMode: "TOTAL",
    totalWeightKg: null,
    duration: null,
    done: false,
  };
}

function withPositions(slots: TrainingSlotResponse[]): TrainingSlotResponse[] {
  return slots.map((slot, i) => ({ ...slot, position: i + 1 }));
}

function toSetDto(set: TrainingSetResponse): TrainingSetDto {
  return {
    type: set.type,
    reps: set.reps,
    weightKg: set.weightKg,
    weightMode: set.weightMode ?? undefined,
    duration: set.duration,
    done: set.done,
  };
}

function toSlotDtos(slots: TrainingSlotResponse[]): TrainingSlotDto[] {
  return slots.map((slot) => ({
    exerciseId: slot.exerciseId,
    position: slot.position,
    restBetweenSets: slot.restBetweenSets,
    sets: slot.sets.map(toSetDto),
  }));
}

function formatClock(totalSeconds: number): string {
  const s = Math.max(0, Math.floor(totalSeconds));
  const m = Math.floor(s / 60);
  const sec = s % 60;
  return `${m}:${String(sec).padStart(2, "0")}`;
}

function RestCountdown({ seconds, onDone }: { seconds: number; onDone: () => void }) {
  const [left, setLeft] = useState(seconds);
  const onDoneRef = useRef(onDone);
  onDoneRef.current = onDone;

  useEffect(() => {
    setLeft(seconds);
  }, [seconds]);

  useEffect(() => {
    if (left <= 0) {
      onDoneRef.current();
      return;
    }
    const id = window.setTimeout(() => setLeft((v) => v - 1), 1000);
    return () => window.clearTimeout(id);
  }, [left]);

  if (left <= 0) return null;

  return (
    <div className="sticky top-16 z-30 -mx-1 px-1 py-1">
      <div className="rounded-xl border border-amber-500/50 bg-amber-950/95 backdrop-blur-sm shadow-lg px-4 py-3 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-amber-200">
          <Timer className="h-4 w-4" />
          <span className="text-sm font-medium">Rest</span>
        </div>
        <span className="text-2xl font-bold tabular-nums text-amber-100">{formatClock(left)}</span>
        <Button type="button" size="sm" variant="ghost" className="text-amber-200" onClick={onDone}>
          Skip
        </Button>
      </div>
    </div>
  );
}

function SetCountdownTimer({
  durationIso,
  resetKey,
}: {
  durationIso: string | null;
  resetKey: number;
}) {
  const total = parseDuration(durationIso);
  const [left, setLeft] = useState(total);
  const [running, setRunning] = useState(false);

  useEffect(() => {
    setRunning(false);
    setLeft(parseDuration(durationIso));
  }, [resetKey, durationIso]);

  useEffect(() => {
    if (!running) return;
    if (left <= 0) {
      setRunning(false);
      return;
    }
    const id = window.setTimeout(() => setLeft((v) => v - 1), 1000);
    return () => window.clearTimeout(id);
  }, [running, left]);

  if (total <= 0) return null;

  if (!running) {
    return (
      <button
        type="button"
        onClick={() => {
          setLeft(total);
          setRunning(true);
        }}
        className="shrink-0 inline-flex items-center gap-1 h-7 px-2 rounded-md border border-sky-500/40 bg-sky-500/10 text-sky-300 text-[11px] font-medium hover:bg-sky-500/20 transition-colors"
        aria-label="Start set timer"
      >
        <Play className="h-3 w-3" />
        Start
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={() => setRunning(false)}
      className="shrink-0 inline-flex items-center gap-1.5 h-7 px-2 rounded-md border border-sky-500/50 bg-sky-500/15 text-sky-200 text-[11px] font-semibold tabular-nums hover:bg-sky-500/25 transition-colors"
      aria-label="Pause set timer"
      title="Pause"
    >
      <Timer className="h-3 w-3" />
      {formatClock(left)}
    </button>
  );
}

function EditableSetFields({
  set,
  disabled,
  onPatch,
}: {
  set: TrainingSetResponse;
  disabled: boolean;
  onPatch: (patch: SetPatch) => void;
}) {
  const showReps = set.type === "REPS" || set.type === "FAILURE";
  const showDuration = set.type === "TIMED" || set.type === "AMRAP";
  const showWeight = set.type !== "AMRAP";
  const durationSecs = parseDuration(set.duration);

  return (
    <div className="flex items-center gap-1.5 flex-1 flex-wrap min-w-0">
      {showReps && (
        <>
          <input
            type="number"
            min={0}
            disabled={disabled}
            aria-label="Reps"
            value={set.reps || ""}
            onChange={(e) => {
              const v = e.target.value === "" ? 0 : Math.max(0, Math.floor(Number(e.target.value)));
              onPatch({ reps: Number.isFinite(v) ? v : 0 });
            }}
            className={fieldClass}
          />
          <span className="text-[10px] text-muted-foreground">reps</span>
        </>
      )}
      {showDuration && (
        <>
          <input
            type="number"
            min={0}
            disabled={disabled}
            aria-label="Duration seconds"
            value={durationSecs || ""}
            onChange={(e) => {
              const v = e.target.value === "" ? 0 : Math.max(0, Math.floor(Number(e.target.value)));
              onPatch({ duration: formatDuration(Number.isFinite(v) ? v : 0) });
            }}
            className={fieldClass}
          />
          <span className="text-[10px] text-muted-foreground">sec</span>
        </>
      )}
      {showWeight && (
        <>
          <input
            type="number"
            min={0}
            step={0.5}
            disabled={disabled}
            aria-label="Weight kg"
            value={set.weightKg ?? ""}
            onChange={(e) => {
              if (e.target.value === "") {
                onPatch({ weightKg: null });
                return;
              }
              const v = Number(e.target.value);
              onPatch({ weightKg: Number.isFinite(v) && v >= 0 ? v : null });
            }}
            className={fieldClass}
          />
          <select
            disabled={disabled}
            aria-label="Weight mode"
            value={set.weightMode ?? "TOTAL"}
            onChange={(e) => onPatch({ weightMode: e.target.value as WeightMode })}
            className={selectClass}
          >
            <option value="TOTAL">total</option>
            <option value="PER_SIDE">/side</option>
          </select>
          <span className="text-[10px] text-muted-foreground">kg</span>
        </>
      )}
    </div>
  );
}

function SessionSlotCard({
  slot,
  sortableId,
  readOnly,
  setTimerResetKey,
  onToggleSet,
  onPatchSet,
  onAddSet,
  onRemoveSet,
  onRemoveSlot,
  onViewExercise,
}: {
  slot: TrainingSlotResponse;
  sortableId: string;
  readOnly: boolean;
  setTimerResetKey: number;
  onToggleSet: (setIndex: number) => void;
  onPatchSet: (setIndex: number, patch: SetPatch) => void;
  onAddSet: () => void;
  onRemoveSet: (setIndex: number) => void;
  onRemoveSlot: () => void;
  onViewExercise: (exercise: ExerciseResponse) => void;
}) {
  const [expanded, setExpanded] = useState(!slot.done);
  const doneSets = slot.sets.filter((s) => s.done).length;
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: sortableId,
    disabled: readOnly,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.55 : 1,
    zIndex: isDragging ? 50 : undefined,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={cn(
        "rounded-lg border bg-card/60 overflow-hidden transition-colors",
        slot.done ? "border-emerald-500/40" : "border-primary/40 hover:border-primary"
      )}
    >
      <div className="flex items-center gap-2 p-3">
        {!readOnly && (
          <button
            type="button"
            className="shrink-0 p-1 rounded-md text-muted-foreground/60 hover:text-muted-foreground cursor-grab active:cursor-grabbing touch-none"
            aria-label="Drag to reorder"
            title="Drag to reorder"
            {...attributes}
            {...listeners}
          >
            <GripVertical className="h-4 w-4" />
          </button>
        )}
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
                <span className="text-xs text-muted-foreground">
                  {doneSets}/{slot.sets.length} sets
                </span>
              </div>
            </>
          ) : (
            <>
              <AlertTriangle className="h-4 w-4 text-destructive shrink-0" />
              <span className="flex-1 text-sm text-destructive italic">Deleted exercise</span>
            </>
          )}
        </button>
        {slot.done ? (
          <CheckCircle2 className="h-5 w-5 text-emerald-400 shrink-0" />
        ) : (
          <Circle className="h-5 w-5 text-muted-foreground/50 shrink-0" />
        )}
        {slot.exercise && (
          <button
            type="button"
            onClick={() => onViewExercise(slot.exercise!)}
            className="shrink-0 p-1.5 rounded-md text-muted-foreground hover:text-primary hover:bg-primary/10 transition-colors"
            aria-label="View exercise"
            title="View exercise"
          >
            <Eye className="h-4 w-4" />
          </button>
        )}
        {!readOnly && (
          <button
            type="button"
            onClick={onRemoveSlot}
            className="shrink-0 p-1.5 rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
            aria-label="Remove exercise"
            title="Remove exercise"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        )}
      </div>

      {expanded && (
        <div className="px-4 pb-3 space-y-1.5 border-t border-border pt-2">
          {slot.sets.map((set, i) => {
            const isTimed = set.type === "TIMED" || set.type === "AMRAP";
            return (
              <div
                key={i}
                className={cn(
                  "flex items-center gap-2 text-xs rounded-md px-2 py-1.5",
                  set.done ? "bg-emerald-500/10" : "bg-muted/40"
                )}
              >
                <button
                  type="button"
                  disabled={readOnly}
                  onClick={() => onToggleSet(i)}
                  className={cn(
                    "shrink-0 h-6 w-6 rounded-md border flex items-center justify-center transition-colors",
                    set.done
                      ? "border-emerald-500/50 bg-emerald-500/20 text-emerald-300"
                      : "border-border text-muted-foreground hover:border-primary hover:text-primary",
                    readOnly && "opacity-60 cursor-not-allowed"
                  )}
                  aria-label={set.done ? `Unmark set ${i + 1}` : `Mark set ${i + 1} done`}
                >
                  {set.done ? <Check className="h-3.5 w-3.5" /> : null}
                </button>
                <span className="text-muted-foreground w-4 text-right shrink-0">{i + 1}.</span>
                <span className="bg-muted px-1.5 py-0.5 rounded text-[10px] font-medium uppercase shrink-0">
                  {set.type}
                </span>
                {readOnly ? (
                  <span className="text-foreground/80 flex-1">{setDescription(set)}</span>
                ) : (
                  <EditableSetFields
                    set={set}
                    disabled={false}
                    onPatch={(patch) => onPatchSet(i, patch)}
                  />
                )}
                {isTimed && !set.done && !readOnly && (
                  <SetCountdownTimer durationIso={set.duration} resetKey={setTimerResetKey} />
                )}
                {!readOnly && (
                  <button
                    type="button"
                    onClick={() => onRemoveSet(i)}
                    disabled={slot.sets.length <= 1}
                    className="shrink-0 p-1 text-muted-foreground hover:text-destructive disabled:opacity-30 disabled:hover:text-muted-foreground transition-colors"
                    aria-label={`Remove set ${i + 1}`}
                    title={slot.sets.length <= 1 ? "Keep at least one set" : "Remove set"}
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                )}
              </div>
            );
          })}
          {!readOnly && (
            <button
              type="button"
              onClick={onAddSet}
              className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors mt-1 px-2"
            >
              <Plus className="h-3 w-3" /> Add set
            </button>
          )}
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

export function TrainingSessionPage() {
  const { id } = useParams<{ id: string }>();
  const sessionId = Number(id);
  const navigate = useNavigate();
  const qc = useQueryClient();

  const [finalNote, setFinalNote] = useState("");
  const [restSeconds, setRestSeconds] = useState<number | null>(null);
  const [restNonce, setRestNonce] = useState(0);
  const [setTimerResetKey, setSetTimerResetKey] = useState(0);
  const [editMode, setEditMode] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [pickingExercise, setPickingExercise] = useState(false);
  const [viewExercise, setViewExercise] = useState<ExerciseResponse | null>(null);
  const slotsDebounceRef = useRef<number | null>(null);
  const noteDebounceRef = useRef<number | null>(null);
  const noteSkipSyncRef = useRef(false);
  const latestSlotsRef = useRef<TrainingSlotResponse[] | null>(null);
  const updateSeqRef = useRef(0);

  const { data, isLoading, error } = useQuery({
    queryKey: ["training-session", sessionId],
    queryFn: ({ signal }) => sessionApi.getById(sessionId, signal),
    enabled: Number.isFinite(sessionId) && sessionId > 0,
  });

  useEffect(() => {
    if (data && !noteSkipSyncRef.current) setFinalNote(data.finalNote ?? "");
  }, [data]);

  useEffect(() => {
    return () => {
      if (slotsDebounceRef.current != null) window.clearTimeout(slotsDebounceRef.current);
      if (noteDebounceRef.current != null) window.clearTimeout(noteDebounceRef.current);
    };
  }, []);

  const slots = data?.slots ?? [];
  const readOnly = data?.status === "COMPLETED" && !editMode;
  const doneSlots = slots.filter((s) => s.done).length;
  const totalSets = slots.reduce((acc, s) => acc + s.sets.length, 0);
  const doneSets = slots.reduce((acc, s) => acc + s.sets.filter((x) => x.done).length, 0);

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ["training-session", sessionId] });
    qc.invalidateQueries({ queryKey: ["training-sessions"] });
  };

  const updateMutation = useMutation({
    mutationFn: (payload: { slots?: TrainingSlotDto[]; finalNote?: string }) =>
      sessionApi.update(sessionId, payload),
    onError: (err) => setActionError(getErrorMessage(err)),
  });

  /** PUT with full snapshot — only apply response if no newer save was dispatched meanwhile. */
  function saveUpdate(
    payload: { slots?: TrainingSlotDto[]; finalNote?: string },
    onSaved?: () => void
  ) {
    const mySeq = ++updateSeqRef.current;
    updateMutation.mutate(payload, {
      onSuccess: (updated) => {
        if (mySeq === updateSeqRef.current) {
          qc.setQueryData<TrainingSessionResponse>(["training-session", sessionId], (prev) => {
            if (!prev) return updated;
            // Slots-only response must not wipe a note the user is still typing
            if (noteSkipSyncRef.current && !("finalNote" in payload)) {
              return { ...updated, finalNote: prev.finalNote };
            }
            return updated;
          });
          onSaved?.();
        } else if ("finalNote" in payload) {
          // Stale full snapshot, but this note write landed — merge note only
          qc.setQueryData<TrainingSessionResponse>(["training-session", sessionId], (prev) =>
            prev ? { ...prev, finalNote: updated.finalNote } : updated
          );
          onSaved?.();
        }
        qc.invalidateQueries({ queryKey: ["training-sessions"] });
        setActionError(null);
      },
    });
  }

  const queueSlotsUpdate = (nextSlots: TrainingSlotResponse[], immediate = false) => {
    latestSlotsRef.current = nextSlots;
    qc.setQueryData<TrainingSessionResponse>(["training-session", sessionId], (prev) =>
      prev ? { ...prev, slots: nextSlots } : prev
    );

    if (slotsDebounceRef.current != null) window.clearTimeout(slotsDebounceRef.current);
    const run = () => {
      const slotsToSave = latestSlotsRef.current;
      if (!slotsToSave) return;
      saveUpdate({ slots: toSlotDtos(slotsToSave) });
    };
    if (immediate) run();
    else slotsDebounceRef.current = window.setTimeout(run, 450);
  };

  const completeMutation = useMutation({
    mutationFn: () => sessionApi.complete(sessionId, finalNote.trim() || undefined),
    onSuccess: (updated) => {
      qc.setQueryData(["training-session", sessionId], updated);
      invalidate();
      setRestSeconds(null);
      setActionError(null);
      noteSkipSyncRef.current = false;
    },
    onError: (err) => setActionError(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: () => sessionApi.delete(sessionId),
    onSuccess: () => {
      qc.removeQueries({ queryKey: ["training-session", sessionId] });
      qc.invalidateQueries({ queryKey: ["training-sessions"] });
      navigate("/sessions", { replace: true });
    },
    onError: (err) => setActionError(getErrorMessage(err)),
  });

  const canMutateSlots = (session: TrainingSessionResponse) =>
    session.status === "IN_PROGRESS" || editMode;

  const handleToggleSet = (slotIndex: number, setIndex: number) => {
    const current = qc.getQueryData<TrainingSessionResponse>(["training-session", sessionId]);
    if (!current || !canMutateSlots(current)) return;

    const nextSlots = current.slots.map((slot, si) => {
      if (si !== slotIndex) return slot;
      const nextSets = slot.sets.map((set, zi) =>
        zi === setIndex ? applySetPatch(set, { done: !set.done }) : set
      );
      return {
        ...slot,
        sets: nextSets,
        done: nextSets.length > 0 && nextSets.every((s) => s.done),
      };
    });

    const toggledOn = !current.slots[slotIndex].sets[setIndex].done;
    if (toggledOn && current.status === "IN_PROGRESS") {
      setSetTimerResetKey((k) => k + 1);
      const restIso = current.slots[slotIndex].restBetweenSets;
      const secs = parseDuration(restIso);
      if (secs > 0) {
        setRestNonce((n) => n + 1);
        setRestSeconds(secs);
      } else {
        setRestSeconds(null);
      }
    }

    queueSlotsUpdate(nextSlots, true);
  };

  const handlePatchSet = (slotIndex: number, setIndex: number, patch: SetPatch) => {
    const current = qc.getQueryData<TrainingSessionResponse>(["training-session", sessionId]);
    if (!current || !canMutateSlots(current)) return;

    const nextSlots = current.slots.map((slot, si) => {
      if (si !== slotIndex) return slot;
      const nextSets = slot.sets.map((set, zi) =>
        zi === setIndex ? applySetPatch(set, patch) : set
      );
      return {
        ...slot,
        sets: nextSets,
        done: nextSets.length > 0 && nextSets.every((s) => s.done),
      };
    });
    queueSlotsUpdate(nextSlots, "weightMode" in patch);
  };

  const handleAddSet = (slotIndex: number) => {
    const current = qc.getQueryData<TrainingSessionResponse>(["training-session", sessionId]);
    if (!current || !canMutateSlots(current)) return;

    const nextSlots = current.slots.map((slot, si) => {
      if (si !== slotIndex) return slot;
      const nextSets = [...slot.sets, defaultSet(slot.sets[slot.sets.length - 1])];
      return { ...slot, sets: nextSets, done: false };
    });
    queueSlotsUpdate(nextSlots, true);
  };

  const handleRemoveSet = (slotIndex: number, setIndex: number) => {
    const current = qc.getQueryData<TrainingSessionResponse>(["training-session", sessionId]);
    if (!current || !canMutateSlots(current)) return;
    if (current.slots[slotIndex].sets.length <= 1) return;

    const nextSlots = current.slots.map((slot, si) => {
      if (si !== slotIndex) return slot;
      const nextSets = slot.sets.filter((_, zi) => zi !== setIndex);
      return {
        ...slot,
        sets: nextSets,
        done: nextSets.length > 0 && nextSets.every((s) => s.done),
      };
    });
    queueSlotsUpdate(nextSlots, true);
  };

  const handleRemoveSlot = (slotIndex: number) => {
    const current = qc.getQueryData<TrainingSessionResponse>(["training-session", sessionId]);
    if (!current || !canMutateSlots(current)) return;

    queueSlotsUpdate(withPositions(current.slots.filter((_, si) => si !== slotIndex)), true);
  };

  const handleAddExercise = (ex: ExerciseResponse) => {
    const current = qc.getQueryData<TrainingSessionResponse>(["training-session", sessionId]);
    if (!current || !canMutateSlots(current)) return;

    const newSlot: TrainingSlotResponse = {
      exerciseId: ex.id,
      exercise: ex,
      position: current.slots.length + 1,
      sets: [defaultSet()],
      restBetweenSets: "PT60S",
      done: false,
    };
    queueSlotsUpdate(withPositions([...current.slots, newSlot]), true);
    setPickingExercise(false);
  };

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;

    const current = qc.getQueryData<TrainingSessionResponse>(["training-session", sessionId]);
    if (!current || !canMutateSlots(current)) return;

    const ids = current.slots.map((s) => String(s.exerciseId));
    const oldIndex = ids.indexOf(String(active.id));
    const newIndex = ids.indexOf(String(over.id));
    if (oldIndex === -1 || newIndex === -1) return;

    queueSlotsUpdate(withPositions(arrayMove(current.slots, oldIndex, newIndex)), true);
  };

  const handleNoteChange = (value: string) => {
    setFinalNote(value);
    noteSkipSyncRef.current = true;
    if (noteDebounceRef.current != null) window.clearTimeout(noteDebounceRef.current);
    noteDebounceRef.current = window.setTimeout(() => {
      saveUpdate({ finalNote: value.trim() }, () => {
        noteSkipSyncRef.current = false;
      });
    }, 500);
  };

  const progressPct = useMemo(() => {
    if (totalSets === 0) return 0;
    return Math.round((doneSets / totalSets) * 100);
  }, [doneSets, totalSets]);

  if (!Number.isFinite(sessionId) || sessionId <= 0) {
    return <ApiError message="Invalid training session." className="max-w-lg mx-auto mt-10" />;
  }

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} className="max-w-lg mx-auto mt-10" />;
  if (!data) return null;

  return (
    <div className="max-w-2xl mx-auto space-y-5 pb-10">
      <div className="flex items-center gap-2">
        <Button type="button" variant="ghost" size="sm" asChild className="gap-1.5 -ml-2">
          <Link to="/sessions">
            <ArrowLeft className="h-4 w-4" /> Sessions
          </Link>
        </Button>
      </div>

      <div className="space-y-2">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold">{data.routineTitle}</h1>
            <p className="text-sm text-muted-foreground mt-1">
              Started {formatTimeAgo(data.startedAt)}
              {data.status === "COMPLETED" && data.duration
                ? ` · ${displayDuration(data.duration)}`
                : null}
            </p>
          </div>
          <span
            className={cn(
              "inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border",
              data.status === "IN_PROGRESS"
                ? "bg-sky-500/15 text-sky-300 border-sky-500/30"
                : "bg-emerald-500/15 text-emerald-300 border-emerald-500/30"
            )}
          >
            {data.status === "IN_PROGRESS" ? "In progress" : "Completed"}
          </span>
        </div>

        <div className="space-y-1.5">
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>
              {doneSlots}/{slots.length} exercises · {doneSets}/{totalSets} sets
            </span>
            <span>{progressPct}%</span>
          </div>
          <div className="h-2 rounded-full bg-muted overflow-hidden">
            <div
              className="h-full bg-primary transition-all duration-300"
              style={{ width: `${progressPct}%` }}
            />
          </div>
        </div>
      </div>

      {actionError && <ApiError message={actionError} />}

      {restSeconds != null && data.status === "IN_PROGRESS" && (
        <RestCountdown
          key={restNonce}
          seconds={restSeconds}
          onDone={() => setRestSeconds(null)}
        />
      )}

      {pickingExercise && !readOnly ? (
        <div className="rounded-xl border border-primary/40 bg-card/60 p-4">
          <ExercisePicker
            onSelect={handleAddExercise}
            onBack={() => setPickingExercise(false)}
            excludeIds={slots.map((s) => s.exerciseId)}
          />
        </div>
      ) : (
        <div className="space-y-2">
          {!readOnly && (
            <div className="flex justify-end">
              <Button
                type="button"
                size="sm"
                variant="outline"
                className="h-8 gap-1.5"
                onClick={() => setPickingExercise(true)}
              >
                <Plus className="h-3.5 w-3.5" /> Add exercise
              </Button>
            </div>
          )}
          <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
            <SortableContext
              items={slots.map((s) => String(s.exerciseId))}
              strategy={verticalListSortingStrategy}
            >
              <div className="space-y-2">
                {slots.map((slot, slotIndex) => (
                  <SessionSlotCard
                    key={String(slot.exerciseId)}
                    sortableId={String(slot.exerciseId)}
                    slot={slot}
                    readOnly={readOnly}
                    setTimerResetKey={setTimerResetKey}
                    onToggleSet={(setIndex) => handleToggleSet(slotIndex, setIndex)}
                    onPatchSet={(setIndex, patch) => handlePatchSet(slotIndex, setIndex, patch)}
                    onAddSet={() => handleAddSet(slotIndex)}
                    onRemoveSet={(setIndex) => handleRemoveSet(slotIndex, setIndex)}
                    onRemoveSlot={() => handleRemoveSlot(slotIndex)}
                    onViewExercise={setViewExercise}
                  />
                ))}
              </div>
            </SortableContext>
          </DndContext>
          {slots.length === 0 && (
            <p className="text-sm text-muted-foreground text-center py-8">
              {readOnly ? "No exercises in this session." : "No exercises yet. Add one to continue."}
            </p>
          )}
        </div>
      )}

      <div className="rounded-xl border border-primary/40 bg-card/60 p-4 space-y-2">
        <label htmlFor="session-final-note" className="text-sm font-medium">
          Final note
        </label>
        <textarea
          id="session-final-note"
          value={finalNote}
          onChange={(e) => handleNoteChange(e.target.value)}
          rows={3}
          placeholder="How did it feel? Anything to remember…"
          className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm resize-y min-h-[72px]"
        />
      </div>

      {data.status === "IN_PROGRESS" ? (
        <div className="flex flex-col sm:flex-row gap-2">
          <Button
            type="button"
            className="flex-1 gap-2"
            disabled={completeMutation.isPending}
            onClick={() => completeMutation.mutate()}
          >
            {completeMutation.isPending ? (
              <Spinner size="sm" label="" />
            ) : (
              <CheckCircle2 className="h-4 w-4" />
            )}
            Finish workout
          </Button>
          <Button
            type="button"
            variant="outline"
            className="gap-2 text-destructive border-destructive/40 hover:bg-destructive/10"
            disabled={deleteMutation.isPending}
            onClick={() => {
              if (window.confirm("Discard this training session?")) deleteMutation.mutate();
            }}
          >
            <Trash2 className="h-4 w-4" />
            Discard
          </Button>
        </div>
      ) : (
        <div className="space-y-3">
          <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-200">
            Workout completed
            {data.duration ? ` in ${displayDuration(data.duration)}` : ""}.
          </div>
          {editMode ? (
            <Button
              type="button"
              variant="secondary"
              className="w-full gap-2"
              onClick={() => {
                setEditMode(false);
                setPickingExercise(false);
                setRestSeconds(null);
              }}
            >
              <Check className="h-4 w-4" />
              Done editing
            </Button>
          ) : (
            <Button
              type="button"
              variant="outline"
              className="w-full gap-2"
              onClick={() => setEditMode(true)}
            >
              <Pencil className="h-4 w-4" />
              Edit session
            </Button>
          )}
        </div>
      )}

      <ExerciseDetailDialog exercise={viewExercise} onClose={() => setViewExercise(null)} />
    </div>
  );
}
