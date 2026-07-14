import React, { useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  adminActivityApi,
  adminEquipmentApi,
  adminTrainingGoalApi,
  type ActivityRequest,
  type EquipmentRequest,
  type TrainingGoalRequest,
} from "./catalogApi";
import { api, getErrorMessage } from "@/lib/axios";
import type { ActivityResponse, EquipmentResponse, ImageUploadResponse, TrainingGoalResponse } from "@/types/api";
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
import { Dumbbell, Flame, ImagePlus, Pencil, Plus, Target, Trash2, Wrench } from "lucide-react";

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
      const { data } = await api.post<ImageUploadResponse>("/media/images/upload", fd, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setToken(data.uploadToken);
    } catch (err) {
      setError(getErrorMessage(err));
      setPreview(null);
    } finally {
      setUploading(false);
    }
  };

  const Field = ({ label = "Icon" }: { label?: string }) => (
    <div className="space-y-1.5">
      <Label>{label}</Label>
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
        <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={handleFile} />
      </div>
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );

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
    <div className="flex items-center gap-3 rounded-lg border border-border bg-card px-4 py-3">
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

// ── Main page ─────────────────────────────────────────────────────────────────

export function AdminCatalogPage() {
  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-6 text-center">
        <h1 className="text-3xl font-bold">Catalog</h1>
        <p className="text-muted-foreground mt-1">Manage activities, equipment and training goals</p>
      </div>

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
        </TabsList>
        </div>

        <TabsContent value="activities"><ActivitiesTab /></TabsContent>
        <TabsContent value="equipment"><EquipmentTab /></TabsContent>
        <TabsContent value="goals"><TrainingGoalsTab /></TabsContent>
      </Tabs>
    </div>
  );
}
