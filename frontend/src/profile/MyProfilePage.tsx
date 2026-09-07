import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { profileApi } from "./api";
import { ProfileCard } from "./ProfileCard";
import { EditProfileDialog } from "./EditProfileDialog";
import { useAuth } from "@/auth/AuthContext";
import { PageSpinner } from "@/components/Spinner";
import { ApiError } from "@/components/ApiError";
import { Button } from "@/components/ui/button";
import { getErrorMessage } from "@/lib/axios";
import { Trash2 } from "lucide-react";

export function MyProfilePage() {
  const [editOpen, setEditOpen] = useState(false);
  const { logout } = useAuth();
  const navigate = useNavigate();

  const { data, isLoading, error } = useQuery({
    queryKey: ["profile", "me"],
    queryFn: ({ signal }) => profileApi.getMe(signal),
  });

  const deleteMutation = useMutation({
    mutationFn: profileApi.deleteMe,
    onSuccess: async () => {
      await logout();
      navigate("/login", { replace: true });
    },
  });

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} className="max-w-lg mx-auto" />;
  if (!data) return null;

  const handleDelete = () => {
    if (
      window.confirm(
        "Delete your account permanently? This will erase your profile and everything you created — " +
          "exercises, routines, programs, training sessions, publications and uploaded media. This cannot be undone."
      )
    ) {
      deleteMutation.mutate();
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-4">
      <ProfileCard profile={data} editable onEditClick={() => setEditOpen(true)} />

      {deleteMutation.isError && (
        <ApiError message={getErrorMessage(deleteMutation.error)} />
      )}

      <div className="rounded-lg border border-destructive/40 bg-destructive/5 p-4 flex items-center justify-between gap-4">
        <div>
          <p className="text-sm font-medium">Delete account</p>
          <p className="text-xs text-muted-foreground">
            Permanently deletes your account and everything you created. This cannot be undone.
          </p>
        </div>
        <Button
          variant="destructive"
          size="sm"
          className="gap-1.5 shrink-0"
          onClick={handleDelete}
          disabled={deleteMutation.isPending}
        >
          <Trash2 className="h-4 w-4" />
          {deleteMutation.isPending ? "Deleting…" : "Delete account"}
        </Button>
      </div>

      <EditProfileDialog open={editOpen} onClose={() => setEditOpen(false)} profile={data} />
    </div>
  );
}
