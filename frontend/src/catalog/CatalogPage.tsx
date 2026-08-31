import { useQuery } from "@tanstack/react-query";
import { catalogApi } from "./api";
import { MuscleMap } from "./MuscleMap";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent } from "@/components/ui/card";
import { PageSpinner } from "@/components/Spinner";
import { ApiError } from "@/components/ApiError";
import { getErrorMessage } from "@/lib/axios";
import type { ActivityResponse, EquipmentResponse, TrainingGoalResponse } from "@/types/api";
import { Dumbbell, Flame, Layers, Target } from "lucide-react";

function CatalogItemCard({
  icon,
  name,
  detail,
}: {
  icon: { url: string } | null;
  name: string;
  detail?: string | null;
}) {
  return (
    <Card className="hover:shadow-md transition-shadow">
      <CardContent className="flex items-start gap-3 pt-4 pb-4">
        <div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
          {icon?.url ? (
            <img src={icon.url} alt={name} className="h-6 w-6 object-contain" />
          ) : (
            <Dumbbell className="h-5 w-5 text-primary" />
          )}
        </div>
        <div className="min-w-0">
          <p className="font-semibold text-sm leading-tight">{name}</p>
          {detail && <p className="text-xs text-muted-foreground mt-0.5 leading-snug">{detail}</p>}
        </div>
      </CardContent>
    </Card>
  );
}

function ActivityTab() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["catalog", "activities"],
    queryFn: catalogApi.listActivities,
    staleTime: Infinity,
  });

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} />;
  if (!data?.length)
    return <p className="text-muted-foreground text-sm text-center py-8">No activities found.</p>;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
      {data.map((a: ActivityResponse) => (
        <CatalogItemCard key={a.id} icon={a.icon} name={a.name} detail={a.detail} />
      ))}
    </div>
  );
}

function EquipmentTab() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["catalog", "equipment"],
    queryFn: catalogApi.listEquipment,
    staleTime: Infinity,
  });

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} />;
  if (!data?.length)
    return <p className="text-muted-foreground text-sm text-center py-8">No equipment found.</p>;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
      {data.map((e: EquipmentResponse) => (
        <CatalogItemCard key={e.id} icon={e.icon} name={e.name} />
      ))}
    </div>
  );
}

function TrainingGoalsTab() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["catalog", "training-goals"],
    queryFn: catalogApi.listTrainingGoals,
    staleTime: Infinity,
  });

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} />;
  if (!data?.length)
    return (
      <p className="text-muted-foreground text-sm text-center py-8">No training goals found.</p>
    );

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
      {data.map((g: TrainingGoalResponse) => (
        <CatalogItemCard key={g.id} icon={g.icon} name={g.name} detail={g.detail} />
      ))}
    </div>
  );
}

export function CatalogPage() {
  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6 text-center">
        <h1 className="text-3xl font-bold">Browse Catalog</h1>
        <p className="text-muted-foreground mt-1">Browse activities, equipment, training goals and muscles</p>
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
            <TabsTrigger value="muscles" className="gap-1.5">
              <Layers className="h-4 w-4" />
              Muscles
            </TabsTrigger>
          </TabsList>
        </div>

        <TabsContent value="activities">
          <ActivityTab />
        </TabsContent>
        <TabsContent value="equipment">
          <EquipmentTab />
        </TabsContent>
        <TabsContent value="goals">
          <TrainingGoalsTab />
        </TabsContent>
        <TabsContent value="muscles">
          <MuscleMap />
        </TabsContent>
      </Tabs>
    </div>
  );
}
