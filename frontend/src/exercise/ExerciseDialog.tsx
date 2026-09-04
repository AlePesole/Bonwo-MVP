import React, { useEffect, useMemo, useRef, useState } from "react";
import { useFieldArray, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { exerciseApi } from "./api";
import { catalogApi } from "@/catalog/api";
import { api, getErrorMessage } from "@/lib/axios";
import { cn } from "@/lib/utils";
import type { ExerciseResponse, ImageUploadResponse, Level, MuscleGroupResponse, VideoResponse } from "@/types/api";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select } from "@/components/ui/select";
import { ApiError } from "@/components/ApiError";
import { Spinner } from "@/components/Spinner";
import { ChevronDown, Film, ImagePlus, Layers, Plus, X } from "lucide-react";

// ── Activation helpers ────────────────────────────────────────────────────────

type ActivationRole = "PRIMARY" | "SECONDARY" | "STABILIZER";

const getRole = (a: number): ActivationRole =>
  a >= 0.7 ? "PRIMARY" : a >= 0.3 ? "SECONDARY" : "STABILIZER";

const ROLE_LABEL: Record<ActivationRole, string> = {
  PRIMARY: "Primary",
  SECONDARY: "Secondary",
  STABILIZER: "Stabilizer",
};

const ROLE_COLOR: Record<ActivationRole, string> = {
  PRIMARY: "text-red-400",
  SECONDARY: "text-orange-400",
  STABILIZER: "text-yellow-300",
};

const ROLE_TRACK_COLOR: Record<ActivationRole, string> = {
  PRIMARY: "#f87171",
  SECONDARY: "#fb923c",
  STABILIZER: "#fde047",
};

// ── Level options ─────────────────────────────────────────────────────────────

const LEVELS: { value: Level; label: string }[] = [
  { value: "BEGINNER", label: "Beginner" },
  { value: "INTERMEDIATE", label: "Intermediate" },
  { value: "ADVANCED", label: "Advanced" },
];

// ── Schema ────────────────────────────────────────────────────────────────────

const schema = z.object({
  title: z.string().min(1, "Title is required").max(200),
  level: z.enum(["BEGINNER", "INTERMEDIATE", "ADVANCED"]),
  description: z.string().optional(),
  instructions: z.string().optional(),
  muscles: z
    .array(
      z.object({
        subGroupId: z.coerce.number().min(1, "Select a muscle"),
        activation: z.number().min(0.1).max(1.0),
      })
    )
    .optional(),
  equipmentIds: z.array(z.number()).optional(),
  activityIds: z.array(z.number()).optional(),
  trainingGoalIds: z.array(z.number()).optional(),
});

type ExerciseForm = z.infer<typeof schema>;

// ── Muscle catalog picker ─────────────────────────────────────────────────────

function MusclePicker({
  muscleGroups,
  selectedIds,
  onToggle,
  onBack,
}: {
  muscleGroups: MuscleGroupResponse[];
  selectedIds: number[];
  onToggle: (subGroupId: number) => void;
  onBack: () => void;
}) {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={onBack}
          className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          <ChevronDown className="h-4 w-4 rotate-90" /> Back
        </button>
        {selectedIds.length > 0 && (
          <span className="text-xs text-muted-foreground">
            {selectedIds.length} selected
          </span>
        )}
      </div>

      <div className="overflow-y-auto max-h-[320px] space-y-4 pr-1">
        {muscleGroups.map((group) => (
          <div key={group.id}>
            <div className="flex items-center gap-2 mb-2">
              <MuscleIcon icon={group.icon?.url ?? null} size="sm" />
              <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                {group.name}
              </span>
            </div>
            <div className="grid grid-cols-3 gap-2">
              {group.subGroups.map((sub) => {
                const isSelected = selectedIds.includes(sub.id);
                return (
                  <button
                    key={sub.id}
                    type="button"
                    onClick={() => onToggle(sub.id)}
                    className={cn(
                      "flex flex-col items-center gap-2 p-3 rounded-xl border transition-colors text-center",
                      isSelected
                        ? "border-primary bg-primary/10 text-primary"
                        : "border-border bg-card hover:border-primary/50 hover:bg-accent/40 text-foreground"
                    )}
                  >
                    <div className={cn(
                      "h-10 w-10 rounded-lg flex items-center justify-center",
                      isSelected ? "bg-primary/20" : "bg-muted"
                    )}>
                      <MuscleIcon icon={sub.icon?.url ?? group.icon?.url ?? null} size="lg" />
                    </div>
                    <span className="text-xs font-medium leading-tight">{sub.name}</span>
                  </button>
                );
              })}
              {group.subGroups.length === 0 && (
                <p className="col-span-3 text-xs text-muted-foreground italic px-1">No subgroups.</p>
              )}
            </div>
          </div>
        ))}
        {muscleGroups.length === 0 && (
          <p className="text-sm text-muted-foreground text-center py-8">No muscles in catalog yet.</p>
        )}
      </div>
    </div>
  );
}

function MuscleIcon({
  icon,
  size,
}: {
  icon: string | null;
  size: "xs" | "sm" | "lg";
}) {
  const dim = size === "xs" ? "h-3.5 w-3.5" : size === "sm" ? "h-5 w-5" : "h-7 w-7";
  if (icon)
    return <img src={icon} alt="" className={cn(dim, "object-contain shrink-0 rounded-sm")} />;
  return <Layers className={cn(dim, "text-muted-foreground shrink-0")} />;
}

// ── Activation slider ─────────────────────────────────────────────────────────

function ActivationSlider({
  activation,
  onChange,
}: {
  activation: number;
  onChange: (v: number) => void;
}) {
  const role = getRole(activation);
  const pct = ((activation - 0.1) / 0.9) * 100;
  const color = ROLE_TRACK_COLOR[role];

  return (
    <div className="flex flex-col gap-0.5 w-36 shrink-0">
      <input
        type="range"
        min={0.1}
        max={1.0}
        step={0.1}
        value={activation}
        onChange={(e) => onChange(Number(e.target.value))}
        aria-label={`Muscle activation — ${ROLE_LABEL[role]}`}
        className="w-full h-2 rounded-full appearance-none cursor-pointer"
        style={{
          background: `linear-gradient(to right, ${color} 0%, ${color} ${pct}%, #3f3f46 ${pct}%, #3f3f46 100%)`,
          accentColor: color,
        }}
      />
      <div className="flex justify-between items-center px-0.5">
        <span className={cn("text-[10px] font-semibold leading-none", ROLE_COLOR[role])}>
          {ROLE_LABEL[role]}
        </span>
        <span className="text-[10px] text-muted-foreground leading-none">
          {activation.toFixed(2)}
        </span>
      </div>
    </div>
  );
}

// ── Muscle entry row ──────────────────────────────────────────────────────────

function MuscleEntryRow({
  index,
  control,
  setValue,
  muscleGroups,
  onRemove,
}: {
  index: number;
  control: ReturnType<typeof useForm<ExerciseForm>>["control"];
  setValue: ReturnType<typeof useForm<ExerciseForm>>["setValue"];
  muscleGroups: MuscleGroupResponse[];
  onRemove: () => void;
}) {
  const subGroupId = useWatch({ control, name: `muscles.${index}.subGroupId` }) ?? 0;
  const activation = useWatch({ control, name: `muscles.${index}.activation` }) ?? 0.5;

  const selected = useMemo(() => {
    for (const g of muscleGroups) {
      const sub = g.subGroups.find((s) => s.id === subGroupId);
      if (sub) return { group: g, sub };
    }
    return null;
  }, [subGroupId, muscleGroups]);

  return (
    <div className="flex items-center gap-2">
      <div className="flex-1 h-9 flex items-center gap-2 px-3 rounded-md border border-input bg-background text-sm min-w-0">
        <MuscleIcon icon={selected?.sub.icon?.url ?? selected?.group.icon?.url ?? null} size="sm" />
        <span className="truncate flex-1 text-left text-sm">
          {selected ? selected.sub.name : <span className="text-muted-foreground">Unknown</span>}
        </span>
      </div>

      <ActivationSlider
        activation={activation}
        onChange={(v) => setValue(`muscles.${index}.activation`, v)}
      />

      <Button
        type="button"
        size="icon"
        variant="ghost"
        className="h-8 w-8 shrink-0 text-muted-foreground hover:text-destructive"
        onClick={onRemove}
      >
        <X className="h-4 w-4" />
      </Button>
    </div>
  );
}

// ── Catalog multi-picker (Details tab) ───────────────────────────────────────

function CatalogPickerSection({
  label,
  items,
  selected,
  onToggle,
}: {
  label: string;
  items: { id: number; name: string; icon?: { url: string } | null }[];
  selected: number[];
  onToggle: (id: number) => void;
}) {
  return (
    <div className="space-y-2">
      <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
        {label}
        {selected.length > 0 && (
          <span className="ml-2 bg-primary/20 text-primary text-[10px] font-bold rounded-full px-1.5 py-0.5">
            {selected.length}
          </span>
        )}
      </p>
      {items.length === 0 ? (
        <p className="text-xs text-muted-foreground italic">No items in catalog.</p>
      ) : (
        <div className="grid grid-cols-3 gap-2">
          {items.map((item) => {
            const isSelected = selected.includes(item.id);
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => onToggle(item.id)}
                className={cn(
                  "flex flex-col items-center gap-2 p-3 rounded-xl border transition-colors text-center",
                  isSelected
                    ? "border-primary bg-primary/10 text-primary"
                    : "border-border bg-card hover:border-primary/50 hover:bg-accent/40 text-foreground"
                )}
              >
                <div
                  className={cn(
                    "h-10 w-10 rounded-lg flex items-center justify-center",
                    isSelected ? "bg-primary/20" : "bg-muted"
                  )}
                >
                  {item.icon?.url ? (
                    <img
                      src={item.icon.url}
                      alt={item.name}
                      className="h-7 w-7 object-contain"
                    />
                  ) : (
                    <Layers className="h-5 w-5 text-muted-foreground" />
                  )}
                </div>
                <span className="text-xs font-medium leading-tight">{item.name}</span>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

// ── Muscles tab content (manages picker state) ────────────────────────────────

function MusclesTabContent({
  fields,
  control,
  setValue,
  muscleGroups,
  onAppend,
  onRemove,
}: {
  fields: ReturnType<typeof useFieldArray<ExerciseForm, "muscles">>["fields"];
  control: ReturnType<typeof useForm<ExerciseForm>>["control"];
  setValue: ReturnType<typeof useForm<ExerciseForm>>["setValue"];
  muscleGroups: MuscleGroupResponse[];
  onAppend: (subGroupId: number) => void;
  onRemove: (index: number) => void;
}) {
  const [pickerOpen, setPickerOpen] = useState(false);

  const musclesWatched = useWatch({ control, name: "muscles" });
  const selectedIds = (musclesWatched ?? []).map((m) => m.subGroupId).filter(Boolean) as number[];

  const handleToggle = (subGroupId: number) => {
    const idx = selectedIds.indexOf(subGroupId);
    if (idx === -1) {
      onAppend(subGroupId);
    } else {
      onRemove(idx);
    }
  };

  if (pickerOpen) {
    return (
      <div className="rounded-lg border border-primary/40 bg-card/50 p-4 min-h-[300px]">
        <MusclePicker
          muscleGroups={muscleGroups}
          selectedIds={selectedIds}
          onToggle={handleToggle}
          onBack={() => setPickerOpen(false)}
        />
      </div>
    );
  }

  return (
    <div className="rounded-lg border border-primary/40 bg-card/50 p-4 min-h-[220px] space-y-3">
      <div className="flex items-center justify-between">
        <p className="text-xs text-muted-foreground">
          {fields.length === 0
            ? "Select muscles involved in this exercise."
            : `${fields.length} muscle${fields.length !== 1 ? "s" : ""} added`}
        </p>
        <Button
          type="button"
          size="sm"
          variant="outline"
          className="h-7 text-xs gap-1"
          onClick={() => setPickerOpen(true)}
        >
          <Plus className="h-3 w-3" /> Select muscles
        </Button>
      </div>

      <div className="space-y-2 overflow-y-auto max-h-[300px] pr-1">
        {fields.map((field, index) => (
          <MuscleEntryRow
            key={field.id}
            index={index}
            control={control}
            setValue={setValue}
            muscleGroups={muscleGroups}
            onRemove={() => onRemove(index)}
          />
        ))}
      </div>
    </div>
  );
}

// ── Details tab content ───────────────────────────────────────────────────────

type DetailCategory = "equipment" | "activities" | "trainingGoals";

const DETAIL_LABELS: Record<DetailCategory, string> = {
  equipment: "Equipment",
  activities: "Activities",
  trainingGoals: "Training Goals",
};

function DetailsTabContent({
  equipment,
  activities,
  trainingGoals,
  equipmentIds,
  activityIds,
  trainingGoalIds,
  onToggle,
}: {
  equipment: { id: number; name: string; icon?: { url: string } | null }[];
  activities: { id: number; name: string; icon?: { url: string } | null }[];
  trainingGoals: { id: number; name: string; icon?: { url: string } | null }[];
  equipmentIds: number[];
  activityIds: number[];
  trainingGoalIds: number[];
  onToggle: (field: "equipmentIds" | "activityIds" | "trainingGoalIds", id: number) => void;
}) {
  const [open, setOpen] = useState<DetailCategory | null>(null);

  const config: {
    key: DetailCategory;
    field: "equipmentIds" | "activityIds" | "trainingGoalIds";
    items: { id: number; name: string; icon?: { url: string } | null }[];
    selected: number[];
  }[] = [
    { key: "equipment", field: "equipmentIds", items: equipment, selected: equipmentIds },
    { key: "activities", field: "activityIds", items: activities, selected: activityIds },
    { key: "trainingGoals", field: "trainingGoalIds", items: trainingGoals, selected: trainingGoalIds },
  ];

  const current = open ? config.find((c) => c.key === open)! : null;

  return (
    <div className="rounded-lg border border-primary/40 bg-card/50 p-4 min-h-[220px]">
      {current ? (
        <div className="space-y-3">
          <button
            type="button"
            onClick={() => setOpen(null)}
            className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            <ChevronDown className="h-4 w-4 rotate-90" /> Back
          </button>
          <CatalogPickerSection
            label={DETAIL_LABELS[current.key]}
            items={current.items}
            selected={current.selected}
            onToggle={(id) => onToggle(current.field, id)}
          />
        </div>
      ) : (
        <div className="space-y-2">
          {config.map(({ key, selected, items }) => (
            <button
              key={key}
              type="button"
              onClick={() => setOpen(key)}
              className="w-full flex items-center justify-between px-4 py-3 rounded-lg border border-border bg-background hover:border-primary/50 hover:bg-accent/30 transition-colors"
            >
              <span className="text-sm font-medium">{DETAIL_LABELS[key]}</span>
              <div className="flex items-center gap-2">
                {selected.length > 0 && (
                  <span className="bg-primary/20 text-primary text-[10px] font-bold rounded-full px-2 py-0.5">
                    {selected.length}
                  </span>
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

// ── Video upload ──────────────────────────────────────────────────────────────

interface VideoUploadResponse {
  uploadToken: string;
  thumbnailUrl: string | null;
  durationSeconds: number | null;
  expiresAt: string;
}

function useVideoUpload() {
  const fileRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [token, setToken] = useState<string | undefined>();
  const [duration, setDuration] = useState<number | null>(null);
  const [removed, setRemoved] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reset = (existing?: VideoResponse | null) => {
    setPreview(existing?.url ?? null);
    setToken(undefined);
    setDuration(null);
    setRemoved(false);
    setError(null);
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
      const { data } = await api.post<VideoUploadResponse>("/media/videos/upload", fd);
      setToken(data.uploadToken);
      setDuration(data.durationSeconds);
    } catch (err) {
      setError(getErrorMessage(err));
      setPreview(null);
    } finally {
      setUploading(false);
    }
  };

  const clear = () => {
    setPreview(null);
    setToken(undefined);
    setDuration(null);
    setRemoved(true);
    if (fileRef.current) fileRef.current.value = "";
  };

  return { token, removed, preview, duration, uploading, error, reset, clear, fileRef, handleFile };
}

// ── Main dialog ───────────────────────────────────────────────────────────────

export function ExerciseDialog({
  open,
  editing,
  onClose,
}: {
  open: boolean;
  editing: ExerciseResponse | null;
  onClose: () => void;
}) {
  const qc = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);
  const thumb = useThumbnailUpload();
  const video = useVideoUpload();

  const { data: muscleGroups = [] } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: ({ signal }) => catalogApi.listMuscleGroups(signal),
    enabled: open,
  });
  const { data: equipment = [] } = useQuery({
    queryKey: ["catalog", "equipment"],
    queryFn: ({ signal }) => catalogApi.listEquipment(signal),
    enabled: open,
  });
  const { data: activities = [] } = useQuery({
    queryKey: ["catalog", "activities"],
    queryFn: ({ signal }) => catalogApi.listActivities(signal),
    enabled: open,
  });
  const { data: trainingGoals = [] } = useQuery({
    queryKey: ["catalog", "training-goals"],
    queryFn: ({ signal }) => catalogApi.listTrainingGoals(signal),
    enabled: open,
  });

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<ExerciseForm>({
    resolver: zodResolver(schema),
    defaultValues: {
      level: "INTERMEDIATE",
      muscles: [],
      equipmentIds: [],
      activityIds: [],
      trainingGoalIds: [],
    },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "muscles" });

  const equipmentIds = watch("equipmentIds") ?? [];
  const activityIds = watch("activityIds") ?? [];
  const trainingGoalIds = watch("trainingGoalIds") ?? [];

  const toggle = (field: "equipmentIds" | "activityIds" | "trainingGoalIds", id: number) => {
    const cur = watch(field) ?? [];
    setValue(field, cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id]);
  };

  useEffect(() => {
    if (open) {
      thumb.reset(editing?.thumbnail?.url ?? null);
      video.reset(editing?.mainVideo ?? null);
      setServerError(null);
      reset({
        title: editing?.title ?? "",
        level: editing?.level ?? "INTERMEDIATE",
        description: editing?.description ?? "",
        instructions: editing?.instructions ?? "",
        muscles: editing?.muscles.map((m) => ({ subGroupId: m.subGroupId, activation: m.activation })) ?? [],
        equipmentIds: editing?.equipment.map((e) => e.id) ?? [],
        activityIds: editing?.activities.map((a) => a.id) ?? [],
        trainingGoalIds: editing?.trainingGoals.map((g) => g.id) ?? [],
      });
    }
  }, [open, editing]);

  const mutation = useMutation({
    mutationFn: (data: ExerciseForm) => {
      const payload = {
        title: data.title,
        level: data.level,
        description: data.description || undefined,
        instructions: data.instructions || undefined,
        thumbnailUploadToken: thumb.token,
        removeThumbnail: thumb.removed,
        mainVideoUploadToken: video.token,
        removeMainVideo: video.removed,
        muscles: data.muscles?.map((m) => ({
          subGroupId: Number(m.subGroupId),
          activation: m.activation,
        })),
        equipmentIds: data.equipmentIds,
        activityIds: data.activityIds,
        trainingGoalIds: data.trainingGoalIds,
      };
      return editing ? exerciseApi.update(editing.id, payload) : exerciseApi.create(payload);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["exercises"] });
      onClose();
    },
    onError: (err) => setServerError(getErrorMessage(err)),
  });

  const selectClass =
    "flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:opacity-50";

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-2xl flex flex-col">
        <DialogHeader>
          <DialogTitle>{editing ? "Edit Exercise" : "New Exercise"}</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="flex flex-col gap-4">
          {serverError && <ApiError message={serverError} />}

          <Tabs defaultValue="info">
            <TabsList className="w-full">
              <TabsTrigger value="info" className="flex-1">Info</TabsTrigger>
              <TabsTrigger value="muscles" className="flex-1">
                Muscles {fields.length > 0 && <span className="ml-1.5 bg-primary/20 text-primary text-[10px] font-bold rounded-full px-1.5">{fields.length}</span>}
              </TabsTrigger>
              <TabsTrigger value="details" className="flex-1">Details</TabsTrigger>
            </TabsList>

            {/* ── Info tab ── */}
            <TabsContent value="info" className="space-y-4 mt-4">
              <div className="grid grid-cols-3 gap-3">
                <div className="col-span-2 space-y-1.5">
                  <Label htmlFor="ex-title">Title</Label>
                  <Input id="ex-title" {...register("title")} placeholder="e.g. Barbell Bench Press" />
                  {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="ex-level">Level</Label>
                  <Select id="ex-level" {...register("level")} className={selectClass}>
                    {LEVELS.map((l) => (
                      <option key={l.value} value={l.value}>{l.label}</option>
                    ))}
                  </Select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label htmlFor="ex-description">Description <span className="text-muted-foreground">(optional)</span></Label>
                  <Textarea id="ex-description" {...register("description")} rows={4} placeholder="Brief overview…" />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="ex-instructions">Instructions <span className="text-muted-foreground">(optional)</span></Label>
                  <Textarea id="ex-instructions" {...register("instructions")} rows={4} placeholder="Step by step…" />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                {/* Thumbnail */}
                <div className="space-y-1.5">
                  <Label htmlFor="ex-thumbnail">Thumbnail <span className="text-muted-foreground">(optional)</span></Label>
                  <div className="flex items-center gap-3">
                    <div
                      className="h-16 w-16 rounded-lg border border-border bg-muted flex items-center justify-center cursor-pointer hover:bg-accent transition-colors shrink-0 overflow-hidden"
                      onClick={() => thumb.fileRef.current?.click()}
                    >
                      {thumb.uploading ? (
                        <Spinner size="sm" label="" />
                      ) : thumb.preview ? (
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
                        <button
                          type="button"
                          onClick={thumb.remove}
                          className="text-xs text-destructive hover:underline text-left"
                        >
                          Remove
                        </button>
                      )}
                    </div>
                    <input id="ex-thumbnail" ref={thumb.fileRef} type="file" accept="image/*" className="hidden" onChange={thumb.handleFile} />
                  </div>
                  {thumb.error && <p className="text-xs text-destructive">{thumb.error}</p>}
                </div>

                {/* Video */}
                <div className="space-y-1.5">
                  <Label htmlFor="ex-video">Video <span className="text-muted-foreground">(optional)</span></Label>
                  <div className="flex items-center gap-3">
                    <div
                      className="h-16 w-24 rounded-lg border border-border bg-muted flex items-center justify-center cursor-pointer hover:bg-accent transition-colors shrink-0 overflow-hidden relative"
                      onClick={() => video.fileRef.current?.click()}
                    >
                      {video.uploading ? (
                        <Spinner size="sm" label="" />
                      ) : video.preview ? (
                        <>
                          <video src={video.preview} className="h-full w-full object-cover" muted />
                          <div className="absolute inset-0 flex items-center justify-center bg-black/30">
                            <Film className="h-4 w-4 text-white" />
                          </div>
                        </>
                      ) : (
                        <Film className="h-5 w-5 text-muted-foreground" />
                      )}
                    </div>
                    <div className="flex flex-col gap-1">
                      <p className="text-xs text-muted-foreground">
                        Click to {video.preview ? "change" : "upload"}
                        {video.token && <span className="text-primary ml-1">(ready)</span>}
                      </p>
                      {video.duration != null && (
                        <p className="text-xs text-muted-foreground">
                          {Math.floor(video.duration / 60)}:{String(video.duration % 60).padStart(2, "0")} min
                        </p>
                      )}
                      {video.preview && !video.uploading && (
                        <button
                          type="button"
                          onClick={(e) => { e.stopPropagation(); video.clear(); }}
                          className="text-xs text-destructive hover:underline text-left"
                        >
                          Remove
                        </button>
                      )}
                    </div>
                    <input id="ex-video" ref={video.fileRef} type="file" accept="video/*" className="hidden" onChange={video.handleFile} />
                  </div>
                  {video.error && <p className="text-xs text-destructive">{video.error}</p>}
                </div>
              </div>
            </TabsContent>

            {/* ── Muscles tab ── */}
            <TabsContent value="muscles" className="mt-4">
              <MusclesTabContent
                fields={fields}
                control={control}
                setValue={setValue}
                muscleGroups={muscleGroups}
                onAppend={(subGroupId) => append({ subGroupId, activation: 0.5 })}
                onRemove={(i) => remove(i)}
              />
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
              />
            </TabsContent>
          </Tabs>

          <DialogFooter className="pt-2">
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={mutation.isPending || thumb.uploading || video.uploading}>
              {mutation.isPending ? <Spinner size="sm" label="" /> : editing ? "Save changes" : "Create exercise"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
