import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { profileApi } from "./api";
import { ProfileCard } from "./ProfileCard";
import { PageSpinner } from "@/components/Spinner";
import { ApiError } from "@/components/ApiError";
import { getErrorMessage } from "@/lib/axios";

export function PublicProfilePage() {
  const { username } = useParams<{ username: string }>();

  const { data, isLoading, error } = useQuery({
    queryKey: ["profile", username],
    queryFn: () => profileApi.getPublic(username!),
    enabled: !!username,
  });

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} className="max-w-lg mx-auto" />;
  if (!data) return null;

  return (
    <div className="max-w-2xl mx-auto">
      <ProfileCard profile={data} editable={false} />
    </div>
  );
}
