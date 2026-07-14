import { User, Activity, Scale, Ruler, Calendar, Info, Flame } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import type { ActivityResponse, UserProfileResponse } from "@/types/api";

interface ProfileCardProps {
  profile: UserProfileResponse;
  editable: boolean;
  onEditClick?: () => void;
}

function StatChip({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-center gap-2 rounded-lg border bg-muted/40 px-3 py-2 text-sm">
      <span className="text-muted-foreground">{icon}</span>
      <div>
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="font-medium">{value}</p>
      </div>
    </div>
  );
}

function ActivityChip({ activity }: { activity: ActivityResponse }) {
  return (
    <div className="inline-flex items-center gap-2 rounded-xl border border-border bg-muted/30 px-2.5 py-1.5 hover:bg-muted/60 transition-colors">
      {/* Icon */}
      <div className="h-7 w-7 rounded-md bg-primary/10 flex items-center justify-center shrink-0">
        {activity.icon?.url ? (
          <img
            src={activity.icon.url}
            alt={activity.name}
            className="h-5 w-5 object-contain"
          />
        ) : (
          <Flame className="h-4 w-4 text-primary" />
        )}
      </div>

      {/* Name */}
      <span className="text-sm font-medium leading-tight whitespace-nowrap">{activity.name}</span>

      {/* Info tooltip */}
      {activity.detail && (
        <div className="group relative shrink-0">
          <Info className="h-3.5 w-3.5 text-muted-foreground/50 hover:text-primary cursor-pointer transition-colors" />
          <div className="pointer-events-none absolute bottom-full right-0 mb-2 hidden group-hover:block z-20">
            <div className="w-52 rounded-lg border border-border bg-popover px-3 py-2 text-xs text-popover-foreground shadow-xl">
              {activity.detail}
              <div className="absolute -bottom-1.5 right-1.5 h-3 w-3 rotate-45 border-b border-r border-border bg-popover" />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export function ProfileCard({ profile, editable, onEditClick }: ProfileCardProps) {
  const initials = profile.username.slice(0, 2).toUpperCase();

  return (
    <Card>
      <CardHeader className="pb-4">
        <div className="flex items-start gap-4">
          <Avatar className="h-20 w-20 border-2 border-primary/20">
            {profile.avatar?.url && (
              <AvatarImage src={profile.avatar.url} alt={profile.username} />
            )}
            <AvatarFallback className="text-2xl bg-primary/10 text-primary font-bold">
              {initials}
            </AvatarFallback>
          </Avatar>

          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between gap-2">
              <h1 className="text-2xl font-bold truncate">@{profile.username}</h1>
              {editable && onEditClick && (
                <button
                  onClick={onEditClick}
                  className="text-sm text-primary hover:underline font-medium shrink-0"
                >
                  Edit profile
                </button>
              )}
            </div>
            {profile.bio && (
              <p className="mt-1 text-muted-foreground text-sm leading-relaxed">{profile.bio}</p>
            )}
          </div>
        </div>
      </CardHeader>

      <CardContent className="space-y-5">
        {/* Stats */}
        {(profile.ageYears || profile.heightCm || profile.weightKg) && (
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            {profile.ageYears && (
              <StatChip
                icon={<Calendar className="h-4 w-4" />}
                label="Age"
                value={`${profile.ageYears} yrs`}
              />
            )}
            {profile.heightCm && (
              <StatChip
                icon={<Ruler className="h-4 w-4" />}
                label="Height"
                value={`${profile.heightCm} cm`}
              />
            )}
            {profile.weightKg && (
              <StatChip
                icon={<Scale className="h-4 w-4" />}
                label="Weight"
                value={`${profile.weightKg} kg`}
              />
            )}
          </div>
        )}

        {/* Activities */}
        {profile.activities.length > 0 && (
          <div>
            <div className="flex items-center gap-1.5 text-sm font-medium text-muted-foreground mb-3">
              <Activity className="h-4 w-4" />
              Activities
            </div>
            <div className="flex flex-wrap gap-2">
              {profile.activities.map((a) => (
                <ActivityChip key={a.id} activity={a} />
              ))}
            </div>
          </div>
        )}

        {!profile.bio &&
          !profile.ageYears &&
          !profile.heightCm &&
          !profile.weightKg &&
          profile.activities.length === 0 && (
            <div className="text-center py-6 text-muted-foreground">
              <User className="h-10 w-10 mx-auto mb-2 opacity-40" />
              <p className="text-sm">This profile is still empty.</p>
            </div>
          )}
      </CardContent>
    </Card>
  );
}
