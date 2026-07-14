import { useEffect, useRef, useState, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { profileApi } from "./api";
import { api, getErrorMessage } from "@/lib/axios";
import { catalogApi } from "@/catalog/api";
import type { ImageUploadResponse, UserProfileResponse } from "@/types/api";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { ApiError } from "@/components/ApiError";
import { Spinner } from "@/components/Spinner";
import { Camera, Minus, Plus } from "lucide-react";

const schema = z.object({
  bio: z.string().max(500, "Bio cannot exceed 500 characters").optional(),
  ageYears: z.string().optional(),
  heightCm: z.string().optional(),
  weightKg: z.string().optional(),
  activityIds: z.array(z.number()).optional(),
});

type FormData = z.infer<typeof schema>;

// ── Number stepper ────────────────────────────────────────────────────────────

interface NumberStepperProps {
  label: string;
  value: string;
  onChange: (v: string) => void;
  min: number;
  max: number;
  step?: number;
}

function NumberStepper({ label, value, onChange, min, max, step = 1 }: NumberStepperProps) {
  const num = value === "" ? null : Number(value);

  const decrement = useCallback(() => {
    const current = num ?? min;
    const next = Math.max(min, current - step);
    onChange(String(next));
  }, [num, min, step, onChange]);

  const increment = useCallback(() => {
    const current = num ?? min;
    const next = Math.min(max, current + step);
    onChange(String(next));
  }, [num, min, max, step, onChange]);

  return (
    <div className="space-y-1.5">
      <Label className="text-xs text-muted-foreground">{label}</Label>
      <div className="flex items-center gap-0 rounded-md border border-input bg-input overflow-hidden">
        <button
          type="button"
          onClick={decrement}
          disabled={num !== null && num <= min}
          className="flex h-11 w-11 items-center justify-center text-muted-foreground hover:text-primary hover:bg-primary/10 transition-colors disabled:opacity-30 disabled:cursor-not-allowed shrink-0"
        >
          <Minus className="h-4 w-4" />
        </button>
        <input
          type="number"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          min={min}
          max={max}
          step={step}
          className="flex-1 bg-transparent text-center text-sm font-medium focus:outline-none [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
          placeholder="—"
        />
        <button
          type="button"
          onClick={increment}
          disabled={num !== null && num >= max}
          className="flex h-11 w-11 items-center justify-center text-muted-foreground hover:text-primary hover:bg-primary/10 transition-colors disabled:opacity-30 disabled:cursor-not-allowed shrink-0"
        >
          <Plus className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

interface EditProfileDialogProps {
  open: boolean;
  onClose: () => void;
  profile: UserProfileResponse;
}

export function EditProfileDialog({ open, onClose, profile }: EditProfileDialogProps) {
  const qc = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [avatarUploadToken, setAvatarUploadToken] = useState<string | undefined>();
  const [avatarPreview, setAvatarPreview] = useState<string | undefined>(
    profile.avatar?.url ?? undefined
  );
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      bio: profile.bio ?? "",
      ageYears: profile.ageYears != null ? String(profile.ageYears) : "",
      heightCm: profile.heightCm != null ? String(profile.heightCm) : "",
      weightKg: profile.weightKg != null ? String(profile.weightKg) : "",
      activityIds: profile.activities.map((a) => a.id),
    },
  });

  const selectedActivityIds = watch("activityIds") ?? [];

  const { data: activities } = useQuery({
    queryKey: ["catalog", "activities"],
    queryFn: catalogApi.listActivities,
    staleTime: Infinity,
  });

  const mutation = useMutation({
    mutationFn: profileApi.patchMe,
    onSuccess: (updated) => {
      qc.setQueryData(["profile", "me"], updated);
      onClose();
    },
    onError: (err) => setServerError(getErrorMessage(err)),
  });

  useEffect(() => {
    if (open) {
      reset({
        bio: profile.bio ?? "",
        ageYears: profile.ageYears != null ? String(profile.ageYears) : "",
        heightCm: profile.heightCm != null ? String(profile.heightCm) : "",
        weightKg: profile.weightKg != null ? String(profile.weightKg) : "",
        activityIds: profile.activities.map((a) => a.id),
      });
      setAvatarUploadToken(undefined);
      setAvatarPreview(profile.avatar?.url ?? undefined);
      setServerError(null);
      setUploadError(null);
    }
  }, [open, profile, reset]);

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploadError(null);
    setIsUploadingAvatar(true);

    const preview = URL.createObjectURL(file);
    setAvatarPreview(preview);

    try {
      const formData = new FormData();
      formData.append("file", file);
      const { data } = await api.post<ImageUploadResponse>("/media/images/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setAvatarUploadToken(data.uploadToken);
    } catch (err) {
      setUploadError(getErrorMessage(err));
      setAvatarPreview(profile.avatar?.url ?? undefined);
    } finally {
      setIsUploadingAvatar(false);
    }
  };

  const toggleActivity = (id: number) => {
    const current = selectedActivityIds;
    setValue(
      "activityIds",
      current.includes(id) ? current.filter((x) => x !== id) : [...current, id]
    );
  };

  const toNum = (s: string | undefined) =>
    s && s.trim() !== "" ? Number(s) : null;

  const onSubmit = async (values: FormData) => {
    setServerError(null);
    mutation.mutate({
      avatarUploadToken,
      bio: values.bio ?? undefined,
      ageYears: toNum(values.ageYears),
      heightCm: toNum(values.heightCm),
      weightKg: toNum(values.weightKg),
      activityIds: values.activityIds,
    });
  };

  const initials = profile.username.slice(0, 2).toUpperCase();

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Edit Profile</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          {(serverError || uploadError) && (
            <ApiError message={serverError ?? uploadError ?? ""} />
          )}

          {/* Avatar upload */}
          <div className="flex items-center gap-4">
            <div className="relative group">
              <Avatar className="h-16 w-16 border-2 border-primary/20">
                {avatarPreview && <AvatarImage src={avatarPreview} alt="Avatar" />}
                <AvatarFallback className="bg-primary/10 text-primary font-bold text-xl">
                  {initials}
                </AvatarFallback>
              </Avatar>
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={isUploadingAvatar}
                className="absolute inset-0 flex items-center justify-center rounded-full bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity"
              >
                {isUploadingAvatar ? (
                  <Spinner size="sm" label="" />
                ) : (
                  <Camera className="h-5 w-5 text-white" />
                )}
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={handleAvatarChange}
              />
            </div>
            <div className="text-sm text-muted-foreground">
              <p className="font-medium text-foreground">Profile photo</p>
              <p>Click the photo to change it</p>
            </div>
          </div>

          {/* Bio */}
          <div className="space-y-1.5">
            <Label htmlFor="bio">Bio</Label>
            <Textarea
              id="bio"
              placeholder="Tell us about yourself…"
              rows={3}
              {...register("bio")}
            />
            {errors.bio && <p className="text-sm text-destructive">{errors.bio.message}</p>}
          </div>

          {/* Stats */}
          <div className="grid grid-cols-3 gap-3">
            <NumberStepper
              label="Age (years)"
              value={watch("ageYears") ?? ""}
              onChange={(v) => setValue("ageYears", v)}
              min={13}
              max={120}
            />
            <NumberStepper
              label="Height (cm)"
              value={watch("heightCm") ?? ""}
              onChange={(v) => setValue("heightCm", v)}
              min={50}
              max={300}
            />
            <NumberStepper
              label="Weight (kg)"
              value={watch("weightKg") ?? ""}
              onChange={(v) => setValue("weightKg", v)}
              min={20}
              max={500}
              step={0.5}
            />
          </div>

          {/* Activities */}
          {activities && activities.length > 0 && (
            <div className="space-y-2">
              <Label>Activities</Label>
              <div className="flex flex-wrap gap-2 max-h-40 overflow-y-auto rounded-md border p-3">
                {activities.map((a) => {
                  const selected = selectedActivityIds.includes(a.id);
                  return (
                    <button
                      key={a.id}
                      type="button"
                      onClick={() => toggleActivity(a.id)}
                      className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                        selected
                          ? "border-primary bg-primary text-primary-foreground"
                          : "border-border bg-background hover:bg-muted"
                      }`}
                    >
                      {a.icon?.url && (
                        <img src={a.icon.url} alt="" className="h-3 w-3 rounded-full" />
                      )}
                      {a.name}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting || isUploadingAvatar}>
              {isSubmitting ? "Saving…" : "Save changes"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
