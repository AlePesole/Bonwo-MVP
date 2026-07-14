import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { profileApi } from "./api";
import { ProfileCard } from "./ProfileCard";
import { EditProfileDialog } from "./EditProfileDialog";
import { PageSpinner } from "@/components/Spinner";
import { ApiError } from "@/components/ApiError";
import { getErrorMessage } from "@/lib/axios";

export function MyProfilePage() {
  const [editOpen, setEditOpen] = useState(false);

  const { data, isLoading, error } = useQuery({
    queryKey: ["profile", "me"],
    queryFn: profileApi.getMe,
  });

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} className="max-w-lg mx-auto" />;
  if (!data) return null;

  return (
    <div className="max-w-2xl mx-auto">
      <ProfileCard profile={data} editable onEditClick={() => setEditOpen(true)} />
      <EditProfileDialog open={editOpen} onClose={() => setEditOpen(false)} profile={data} />
    </div>
  );
}
