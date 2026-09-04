import React, { useRef, useState } from "react";
import { Navigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  adminActivityApi,
  adminEquipmentApi,
  adminTrainingGoalApi,
  adminMuscleGroupApi,
  adminMuscleSubGroupApi,
  type ActivityRequest,
  type EquipmentRequest,
  type TrainingGoalRequest,
  type MuscleGroupRequest,
  type CreateMuscleSubGroupRequest,
  type UpdateMuscleSubGroupRequest,
} from "./catalogApi";
import { api, getErrorMessage } from "@/lib/axios";
import type { ActivityResponse, EquipmentResponse, ImageUploadResponse, MuscleGroupResponse, MuscleSubGroupResponse, TrainingGoalResponse } from "@/types/api";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ApiError } from "@/components/ApiError";
import { PageSpinner, Spinner } from "@/components/Spinner";
import { ChevronDown, ChevronRight, Dumbbell, Flame, ImagePlus, Layers, Pencil, Plus, Target, Trash2, X } from "lucide-react";

// ── Shared icon upload hook ───────────────────────────────────────────────────

function useIconUpload() {
  const fileRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [token, setToken] = useState<string | undefined>();
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reset = (existingUrl?: string | null) => {
    setPreview(existingUrl ?? null);
    setToken(undefined);
    setError(null);
  };

  const handleFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setError(null);
    setUploading(true);
    setPreview(URL.createObjectURL(file));
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

  const Field = ({ label = "Icon" }: { label?: string }) => {
    const inputId = `icon-upload-${label.toLowerCase().replace(/\s+/g, "-").replace(/[^a-z0-9-]/g, "")}`;
    return (
    <div className="space-y-1.5">
      <Label htmlFor={inputId}>{label}</Label>
      <div className="flex items-center gap-3">
        <div
          className="h-12 w-12 rounded-lg border border-border bg-muted flex items-center justify-center cursor-pointer hover:bg-accent transition-colors shrink-0"
          onClick={() => fileRef.current?.click()}
        >
          {uploading ? (
            <Spinner size="sm" label="" />
          ) : preview ? (
            <img src={preview} alt="icon" className="h-10 w-10 object-contain rounded-md" />
          ) : (
            <ImagePlus className="h-5 w-5 text-muted-foreground" />
          )}
        </div>
        <div className="text-xs text-muted-foreground">
          Click to {preview ? "change" : "upload"} icon
          {token && <span className="text-primary ml-1">(ready)</span>}
        </div>
        <input id={inputId} ref={fileRef} type="file" accept="image/*" className="hidden" onChange={handleFile} />
      </div>
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
  };

  return { token, preview, uploading, reset, Field };
}

// ── Catalog item row ──────────────────────────────────────────────────────────

function CatalogRow({
  icon,
  name,
  detail,
  onEdit,
  onDelete,
  FallbackIcon = Dumbbell,
}: {
  icon: { url: string } | null;
  name: string;
  detail?: string | null;
  onEdit: () => void;
  onDelete: () => void;
  FallbackIcon?: React.ElementType;
}) {
  return (
    <div className="flex items-center gap-3 rounded-lg border border-primary/40 bg-card px-4 py-3">
      <div className="h-9 w-9 rounded-md bg-muted flex items-center justify-center shrink-0">
        {icon?.url ? (
          <img src={icon.url} alt={name} className="h-7 w-7 object-contain" />
        ) : (
          <FallbackIcon className="h-4 w-4 text-muted-foreground" />
        )}
      </div>
      <div className="flex-1 min-w-0">
        <p className="font-medium text-sm leading-tight">{name}</p>
        {detail && <p className="text-xs text-muted-foreground truncate mt-0.5">{detail}</p>}
      </div>
      <div className="flex items-center gap-1 shrink-0">
        <Button variant="ghost" size="icon" className="h-8 w-8" onClick={onEdit}>
          <Pencil className="h-3.5 w-3.5" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-destructive hover:text-destructive"
          onClick={onDelete}
        >
          <Trash2 className="h-3.5 w-3.5" />
        </Button>
      </div>
    </div>
  );
}

// ── Activities tab ────────────────────────────────────────────────────────────

const activitySchema = z.object({
  name: z.string().min(1, "Required").max(100),
  detail: z.string().min(1, "Required"),
});
type ActivityForm = z.infer<typeof activitySchema>;

function ActivitiesTab() {
  const qc = useQueryClient();
  const [editing, setEditing] = useState<ActivityResponse | null>(null);
  const [open, setOpen] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const icon = useIconUpload();

  const { data, isLoading, error } = useQuery({
    queryKey: ["catalog", "activities"],
    queryFn: adminActivityApi.list,
  });

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<ActivityForm>({
    resolver: zodResolver(activitySchema),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ["catalog", "activities"] });

  const saveMutation = useMutation({
    mutationFn: (values: ActivityRequest) =>
      editing ? adminActivityApi.update(editing.id, values) : adminActivityApi.create(values),
    onSuccess: () => { invalidate(); closeDialog(); },
    onError: (err) => setServerError(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: adminActivityApi.delete,
    onSuccess: invalidate,
    onError: (err) => setServerError(getErrorMessage(err)),
  });

  const openCreate = () => {
    setEditing(null);
    reset({ name: "", detail: "" });
    icon.reset(null);
    setServerError(null);
    setOpen(true);
  };

  const openEdit = (a: ActivityResponse) => {
    setEditing(a);
    reset({ name: a.name, detail: a.detail });
    icon.reset(a.icon?.url);
    setServerError(null);
    setOpen(true);
  };

  const closeDialog = () => { setOpen(false); setEditing(null); };

  const onSubmit = (values: ActivityForm) => {
    saveMutation.mutate({ ...values, iconUploadToken: icon.token });
  };

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} />;

  return (
    <div className="space-y-3">
      {serverError && <ApiError message={serverError} />}
      <div className="flex justify-end">
        <Button size="sm" onClick={openCreate}>
          <Plus className="h-4 w-4" /> New activity
        </Button>
      </div>

      {!data?.length && (
        <p className="text-center text-muted-foreground text-sm py-8">No activities yet.</p>
      )}
      {data?.map((a) => (
        <CatalogRow
          key={a.id}
          icon={a.icon}
          name={a.name}
          detail={a.detail}
          FallbackIcon={Flame}
          onEdit={() => openEdit(a)}
          onDelete={() => confirm(`Delete "${a.name}"?`) && deleteMutation.mutate(a.id)}
        />
      ))}

      <Dialog open={open} onOpenChange={(v) => !v && closeDialog()}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{editing ? "Edit Activity" : "New Activity"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {serverError && <ApiError message={serverError} />}
            <div className="space-y-1.5">
              <Label htmlFor="act-name">Name</Label>
              <Input id="act-name" placeholder="e.g. Running" {...register("name")} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="act-detail">Detail</Label>
              <Textarea id="act-detail" placeholder="Short description" rows={2} {...register("detail")} />
              {errors.detail && <p className="text-sm text-destructive">{errors.detail.message}</p>}
            </div>
            <icon.Field label="Icon (optional)" />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={closeDialog} disabled={isSubmitting}>Cancel</Button>
              <Button type="submit" disabled={isSubmitting || icon.uploading}>
                {isSubmitting ? "Saving…" : "Save"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ── Equipment tab ─────────────────────────────────────────────────────────────

const equipmentSchema = z.object({ name: z.string().min(1, "Required").max(100) });
type EquipmentForm = z.infer<typeof equipmentSchema>;

function EquipmentTab() {
  const qc = useQueryClient();
  const [editing, setEditing] = useState<EquipmentResponse | null>(null);
  const [open, setOpen] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const icon = useIconUpload();

  const { data, isLoading, error } = useQuery({
    queryKey: ["catalog", "equipment"],
    queryFn: adminEquipmentApi.list,
  });

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<EquipmentForm>({
    resolver: zodResolver(equipmentSchema),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ["catalog", "equipment"] });

  const saveMutation = useMutation({
    mutationFn: (values: EquipmentRequest) =>
      editing ? adminEquipmentApi.update(editing.id, values) : adminEquipmentApi.create(values),
    onSuccess: () => { invalidate(); closeDialog(); },
    onError: (err) => setServerError(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: adminEquipmentApi.delete,
    onSuccess: invalidate,
    onError: (err) => setServerError(getErrorMessage(err)),
  });

  const openCreate = () => {
    setEditing(null);
    reset({ name: "" });
    icon.reset(null);
    setServerError(null);
    setOpen(true);
  };

  const openEdit = (e: EquipmentResponse) => {
    setEditing(e);
    reset({ name: e.name });
    icon.reset(e.icon?.url);
    setServerError(null);
    setOpen(true);
  };

  const closeDialog = () => { setOpen(false); setEditing(null); };

  const onSubmit = (values: EquipmentForm) => {
    saveMutation.mutate({ ...values, iconUploadToken: icon.token });
  };

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} />;

  return (
    <div className="space-y-3">
      {serverError && <ApiError message={serverError} />}
      <div className="flex justify-end">
        <Button size="sm" onClick={openCreate}>
          <Plus className="h-4 w-4" /> New equipment
        </Button>
      </div>

      {!data?.length && (
        <p className="text-center text-muted-foreground text-sm py-8">No equipment yet.</p>
      )}
      {data?.map((e) => (
        <CatalogRow
          key={e.id}
          icon={e.icon}
          name={e.name}
          FallbackIcon={Dumbbell}
          onEdit={() => openEdit(e)}
          onDelete={() => confirm(`Delete "${e.name}"?`) && deleteMutation.mutate(e.id)}
        />
      ))}

      <Dialog open={open} onOpenChange={(v) => !v && closeDialog()}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{editing ? "Edit Equipment" : "New Equipment"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {serverError && <ApiError message={serverError} />}
            <div className="space-y-1.5">
              <Label htmlFor="eq-name">Name</Label>
              <Input id="eq-name" placeholder="e.g. Barbell" {...register("name")} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>
            <icon.Field label="Icon (optional)" />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={closeDialog} disabled={isSubmitting}>Cancel</Button>
              <Button type="submit" disabled={isSubmitting || icon.uploading}>
                {isSubmitting ? "Saving…" : "Save"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ── Training Goals tab ────────────────────────────────────────────────────────

const goalSchema = z.object({
  name: z.string().min(1, "Required").max(100),
  detail: z.string().optional(),
});
type GoalForm = z.infer<typeof goalSchema>;

function TrainingGoalsTab() {
  const qc = useQueryClient();
  const [editing, setEditing] = useState<TrainingGoalResponse | null>(null);
  const [open, setOpen] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const icon = useIconUpload();

  const { data, isLoading, error } = useQuery({
    queryKey: ["catalog", "training-goals"],
    queryFn: adminTrainingGoalApi.list,
  });

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<GoalForm>({
    resolver: zodResolver(goalSchema),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ["catalog", "training-goals"] });

  const saveMutation = useMutation({
    mutationFn: (values: TrainingGoalRequest) =>
      editing ? adminTrainingGoalApi.update(editing.id, values) : adminTrainingGoalApi.create(values),
    onSuccess: () => { invalidate(); closeDialog(); },
    onError: (err) => setServerError(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: adminTrainingGoalApi.delete,
    onSuccess: invalidate,
    onError: (err) => setServerError(getErrorMessage(err)),
  });

  const openCreate = () => {
    setEditing(null);
    reset({ name: "", detail: "" });
    icon.reset(null);
    setServerError(null);
    setOpen(true);
  };

  const openEdit = (g: TrainingGoalResponse) => {
    setEditing(g);
    reset({ name: g.name, detail: g.detail ?? "" });
    icon.reset(g.icon?.url);
    setServerError(null);
    setOpen(true);
  };

  const closeDialog = () => { setOpen(false); setEditing(null); };

  const onSubmit = (values: GoalForm) => {
    saveMutation.mutate({ ...values, iconUploadToken: icon.token });
  };

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} />;

  return (
    <div className="space-y-3">
      {serverError && <ApiError message={serverError} />}
      <div className="flex justify-end">
        <Button size="sm" onClick={openCreate}>
          <Plus className="h-4 w-4" /> New goal
        </Button>
      </div>

      {!data?.length && (
        <p className="text-center text-muted-foreground text-sm py-8">No training goals yet.</p>
      )}
      {data?.map((g) => (
        <CatalogRow
          key={g.id}
          icon={g.icon}
          name={g.name}
          detail={g.detail}
          FallbackIcon={Target}
          onEdit={() => openEdit(g)}
          onDelete={() => confirm(`Delete "${g.name}"?`) && deleteMutation.mutate(g.id)}
        />
      ))}

      <Dialog open={open} onOpenChange={(v) => !v && closeDialog()}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{editing ? "Edit Training Goal" : "New Training Goal"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {serverError && <ApiError message={serverError} />}
            <div className="space-y-1.5">
              <Label htmlFor="goal-name">Name</Label>
              <Input id="goal-name" placeholder="e.g. Lose weight" {...register("name")} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="goal-detail">Detail (optional)</Label>
              <Textarea id="goal-detail" placeholder="Short description" rows={2} {...register("detail")} />
            </div>
            <icon.Field label="Icon (optional)" />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={closeDialog} disabled={isSubmitting}>Cancel</Button>
              <Button type="submit" disabled={isSubmitting || icon.uploading}>
                {isSubmitting ? "Saving…" : "Save"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ── Muscles tab ───────────────────────────────────────────────────────────────

const MUSCLE_KEYS = [["admin", "muscles"], ["catalog", "muscles"]] as const;
const invalidateMuscles = (qc: ReturnType<typeof useQueryClient>) =>
  MUSCLE_KEYS.forEach((key) => qc.invalidateQueries({ queryKey: key }));

// Zod schemas
const groupSchema = z.object({ name: z.string().min(3, "Min. 3 chars").max(100) });
type GroupForm = z.infer<typeof groupSchema>;

const subGroupSchema = z
  .object({
    name: z.string().min(3, "Min. 3 chars").max(100),
    detail: z.string().optional(),
    svgPathFront: z.string().optional(),
    svgPathBack: z.string().optional(),
  })
  .refine((d) => (d.svgPathFront?.trim() || d.svgPathBack?.trim()), {
    message: "At least one SVG path (front or back) is required",
    path: ["svgPathFront"],
  });
type SubGroupForm = z.infer<typeof subGroupSchema>;

// Small path indicator badge
function PathBadge({ label, filled }: { label: string; filled: boolean }) {
  return (
    <span
      className={`inline-flex items-center justify-center text-[10px] font-bold rounded px-1.5 py-0.5 leading-none ${
        filled
          ? "bg-primary/20 text-primary border border-primary/30"
          : "bg-muted text-muted-foreground border border-border"
      }`}
    >
      {label}
    </span>
  );
}

// Dialog for creating / editing a MuscleSubGroup
function SubGroupDialog({
  open,
  groupId,
  editing,
  onClose,
  onSuccess,
}: {
  open: boolean;
  groupId: number;
  editing: MuscleSubGroupResponse | null;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const icon = useIconUpload();
  const qc = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);
  const [cleared, setCleared] = useState<Set<"svgPathFront" | "svgPathBack">>(new Set());

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<SubGroupForm>({ resolver: zodResolver(subGroupSchema) });

  React.useEffect(() => {
    if (open) {
      icon.reset(editing?.icon?.url ?? null);
      setCleared(new Set());
      reset({
        name: editing?.name ?? "",
        detail: editing?.detail ?? "",
        svgPathFront: editing?.svgPathFront ?? "",
        svgPathBack: editing?.svgPathBack ?? "",
      });
      setServerError(null);
    }
  }, [open, editing]);

  const clearPath = (field: "svgPathFront" | "svgPathBack") => {
    setValue(field, "");
    setCleared((prev) => new Set([...prev, field]));
  };

  // For edit: "" = delete, null = don't touch, string = set value.
  // For create: cleared state is irrelevant (all fields start empty).
  const buildPath = (field: "svgPathFront" | "svgPathBack", value?: string): string | null => {
    const trimmed = value?.trim();
    if (trimmed) return trimmed;
    if (cleared.has(field)) return "";
    return null;
  };

  const mutation = useMutation({
    mutationFn: (data: SubGroupForm) => {
      if (editing) {
        return adminMuscleSubGroupApi.update(editing.id, {
          name: data.name,
          detail: data.detail?.trim() || null,
          svgPathFront: buildPath("svgPathFront", data.svgPathFront),
          svgPathBack: buildPath("svgPathBack", data.svgPathBack),
          iconUploadToken: icon.token,
        });
      }
      return adminMuscleSubGroupApi.create({
        groupId,
        name: data.name,
        detail: data.detail?.trim() || undefined,
        svgPathFront: data.svgPathFront?.trim() || undefined,
        svgPathBack: data.svgPathBack?.trim() || undefined,
        iconUploadToken: icon.token,
      });
    },
    onSuccess: () => {
      invalidateMuscles(qc);
      onSuccess();
    },
    onError: (err) => setServerError(getErrorMessage(err)),
  });

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{editing ? "Edit Muscle SubGroup" : "New Muscle SubGroup"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-4">
          {serverError && <ApiError message={serverError} />}

          <div className="space-y-1.5">
            <Label htmlFor="sg-name">Name</Label>
            <Input id="sg-name" {...register("name")} placeholder="e.g. Pectoralis Major" />
            {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="sg-detail">Description <span className="text-muted-foreground">(optional)</span></Label>
            <Textarea id="sg-detail" {...register("detail")} rows={2} placeholder="Short description…" />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <Label htmlFor="sg-svg-front">SVG Path — Front</Label>
                {editing && (
                  <button
                    type="button"
                    onClick={() => clearPath("svgPathFront")}
                    className="flex items-center gap-1 text-xs text-muted-foreground hover:text-destructive transition-colors"
                  >
                    <X className="h-3 w-3" /> Clear
                  </button>
                )}
              </div>
              <Textarea
                id="sg-svg-front"
                {...register("svgPathFront")}
                rows={3}
                placeholder="M 0 0 L … (front view)"
                className="font-mono text-xs"
              />
            </div>
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <Label htmlFor="sg-svg-back">SVG Path — Back</Label>
                {editing && (
                  <button
                    type="button"
                    onClick={() => clearPath("svgPathBack")}
                    className="flex items-center gap-1 text-xs text-muted-foreground hover:text-destructive transition-colors"
                  >
                    <X className="h-3 w-3" /> Clear
                  </button>
                )}
              </div>
              <Textarea
                id="sg-svg-back"
                {...register("svgPathBack")}
                rows={3}
                placeholder="M 0 0 L … (back view)"
                className="font-mono text-xs"
              />
            </div>
          </div>
          {errors.svgPathFront?.message && (
            <p className="text-xs text-destructive">{errors.svgPathFront.message}</p>
          )}

          <icon.Field label="Icon (optional)" />

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={mutation.isPending || icon.uploading}>
              {mutation.isPending ? <Spinner size="sm" label="" /> : editing ? "Save" : "Create"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// Dialog for creating / editing a MuscleGroup
function GroupDialog({
  open,
  editing,
  onClose,
}: {
  open: boolean;
  editing: MuscleGroupResponse | null;
  onClose: () => void;
}) {
  const icon = useIconUpload();
  const qc = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<GroupForm>({ resolver: zodResolver(groupSchema) });

  React.useEffect(() => {
    if (open) {
      icon.reset(editing?.icon?.url ?? null);
      reset({ name: editing?.name ?? "" });
      setServerError(null);
    }
  }, [open, editing]);

  const mutation = useMutation({
    mutationFn: (data: GroupForm) => {
      const payload: MuscleGroupRequest = { name: data.name, iconUploadToken: icon.token };
      if (editing) return adminMuscleGroupApi.update(editing.id, payload);
      return adminMuscleGroupApi.create(payload);
    },
    onSuccess: () => {
      invalidateMuscles(qc);
      onClose();
    },
    onError: (err) => setServerError(getErrorMessage(err)),
  });

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{editing ? "Edit Muscle Group" : "New Muscle Group"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-4">
          {serverError && <ApiError message={serverError} />}
          <div className="space-y-1.5">
            <Label htmlFor="mg-name">Name</Label>
            <Input id="mg-name" {...register("name")} placeholder="e.g. Chest" />
            {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
          </div>
          <icon.Field />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={mutation.isPending || icon.uploading}>
              {mutation.isPending ? <Spinner size="sm" label="" /> : editing ? "Save" : "Create"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// Main Muscles tab
function MusclesTab() {
  const qc = useQueryClient();
  const { data, isLoading, error } = useQuery({
    queryKey: ["admin", "muscles"],
    queryFn: adminMuscleGroupApi.list,
  });

  const [expanded, setExpanded] = useState<Set<number>>(new Set());
  const [groupDialog, setGroupDialog] = useState<{ open: boolean; editing: MuscleGroupResponse | null }>({ open: false, editing: null });
  const [subDialog, setSubDialog] = useState<{ open: boolean; groupId: number; editing: MuscleSubGroupResponse | null }>({ open: false, groupId: 0, editing: null });

  const deleteGroup = useMutation({
    mutationFn: adminMuscleGroupApi.delete,
    onSuccess: () => invalidateMuscles(qc),
  });

  const deleteSubGroup = useMutation({
    mutationFn: adminMuscleSubGroupApi.delete,
    onSuccess: () => invalidateMuscles(qc),
  });

  const toggleExpand = (id: number) =>
    setExpanded((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} />;

  return (
    <div className="space-y-3">
      <div className="flex justify-end">
        <Button size="sm" onClick={() => setGroupDialog({ open: true, editing: null })}>
          <Plus className="h-4 w-4" /> New group
        </Button>
      </div>

      {!data?.length && (
        <p className="text-center text-muted-foreground text-sm py-8">No muscle groups yet.</p>
      )}

      {data?.map((group) => {
        const isOpen = expanded.has(group.id);
        return (
          <div key={group.id} className="rounded-lg border border-primary/40 bg-card overflow-hidden hover:border-primary transition-colors">
            {/* Group header row */}
            <div className="flex items-center gap-3 px-4 py-3">
              <button
                type="button"
                onClick={() => toggleExpand(group.id)}
                className="text-muted-foreground hover:text-foreground transition-colors shrink-0"
              >
                {isOpen ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
              </button>

              {group.icon ? (
                <img src={group.icon.url} alt={group.name} className="h-7 w-7 object-contain rounded shrink-0" />
              ) : (
                <Layers className="h-5 w-5 text-muted-foreground shrink-0" />
              )}

              <span className="font-semibold flex-1 truncate">{group.name}</span>
              <span className="text-xs text-muted-foreground mr-2">
                {group.subGroups.length} subgroup{group.subGroups.length !== 1 ? "s" : ""}
              </span>

              <Button
                size="icon"
                variant="ghost"
                className="h-7 w-7 shrink-0"
                onClick={() => setGroupDialog({ open: true, editing: group })}
              >
                <Pencil className="h-3.5 w-3.5" />
              </Button>
              <Button
                size="icon"
                variant="ghost"
                className="h-7 w-7 text-destructive hover:text-destructive shrink-0"
                onClick={() => window.confirm(`Delete group "${group.name}" and all its subgroups?`) && deleteGroup.mutate(group.id)}
              >
                <Trash2 className="h-3.5 w-3.5" />
              </Button>
            </div>

            {/* Subgroups list (collapsible) */}
            {isOpen && (
              <div className="border-t border-border">
                {group.subGroups.length === 0 && (
                  <p className="text-xs text-muted-foreground px-10 py-2">No subgroups yet.</p>
                )}
                {group.subGroups.map((sub) => (
                  <div key={sub.id} className="flex items-center gap-3 px-10 py-2.5 border-b border-border/50 last:border-b-0">
                    {sub.icon ? (
                      <img src={sub.icon.url} alt={sub.name} className="h-5 w-5 object-contain rounded shrink-0" />
                    ) : (
                      <Layers className="h-4 w-4 text-muted-foreground shrink-0" />
                    )}
                    <span className="flex-1 text-sm truncate">{sub.name}</span>
                    <div className="flex items-center gap-1 shrink-0">
                      <PathBadge label="F" filled={!!sub.svgPathFront} />
                      <PathBadge label="B" filled={!!sub.svgPathBack} />
                    </div>
                    <Button
                      size="icon"
                      variant="ghost"
                      className="h-6 w-6 shrink-0"
                      onClick={() => setSubDialog({ open: true, groupId: group.id, editing: sub })}
                    >
                      <Pencil className="h-3 w-3" />
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      className="h-6 w-6 text-destructive hover:text-destructive shrink-0"
                      onClick={() => window.confirm(`Delete "${sub.name}"?`) && deleteSubGroup.mutate(sub.id)}
                    >
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  </div>
                ))}

                <div className="px-10 py-2.5">
                  <Button
                    size="sm"
                    variant="outline"
                    className="h-7 text-xs gap-1"
                    onClick={() => setSubDialog({ open: true, groupId: group.id, editing: null })}
                  >
                    <Plus className="h-3 w-3" /> Add subgroup
                  </Button>
                </div>
              </div>
            )}
          </div>
        );
      })}

      <GroupDialog
        open={groupDialog.open}
        editing={groupDialog.editing}
        onClose={() => setGroupDialog({ open: false, editing: null })}
      />

      <SubGroupDialog
        open={subDialog.open}
        groupId={subDialog.groupId}
        editing={subDialog.editing}
        onClose={() => setSubDialog({ open: false, groupId: 0, editing: null })}
        onSuccess={() => setSubDialog({ open: false, groupId: 0, editing: null })}
      />
    </div>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────

// ── Main content (no page chrome — used by Catalog hub View/Edit) ─────────────

export function AdminCatalogContent() {
  return (
    <Tabs defaultValue="activities">
      <div className="flex justify-center mb-6">
        <TabsList>
          <TabsTrigger value="activities" className="gap-1.5">
            <Flame className="h-4 w-4" />
            Activities
          </TabsTrigger>
          <TabsTrigger value="equipment" className="gap-1.5">
            <Dumbbell className="h-4 w-4" />
            Equipment
          </TabsTrigger>
          <TabsTrigger value="goals" className="gap-1.5">
            <Target className="h-4 w-4" />
            Training Goals
          </TabsTrigger>
          <TabsTrigger value="muscles" className="gap-1.5">
            <Layers className="h-4 w-4" />
            Muscles
          </TabsTrigger>
        </TabsList>
      </div>

      <TabsContent value="activities"><ActivitiesTab /></TabsContent>
      <TabsContent value="equipment"><EquipmentTab /></TabsContent>
      <TabsContent value="goals"><TrainingGoalsTab /></TabsContent>
      <TabsContent value="muscles"><MusclesTab /></TabsContent>
    </Tabs>
  );
}

/** @deprecated Prefer /catalog?mode=edit — kept for deep links. */
export function AdminCatalogPage() {
  return <Navigate to="/catalog?mode=edit" replace />;
}
