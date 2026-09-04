import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { catalogApi } from "./api";
import { MuscleMap } from "./MuscleMap";
import { AdminCatalogContent } from "@/admin/AdminCatalogPage";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent } from "@/components/ui/card";
import { PageSpinner } from "@/components/Spinner";
import { ApiError } from "@/components/ApiError";
import { getErrorMessage } from "@/lib/axios";
import { cn } from "@/lib/utils";
import { Dumbbell, Eye, Flame, Layers, Pencil, Target } from "lucide-react";

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
    queryFn: ({ signal }) => catalogApi.listActivities(signal),
    staleTime: Infinity,
  });

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} />;
  if (!data?.length)
    return <p className="text-muted-foreground text-sm text-center py-8">No activities found.</p>;

  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {data.map((a) => (
        <CatalogItemCard key={a.id} icon={a.icon} name={a.name} detail={a.detail} />
      ))}
    </div>
  );
}

function EquipmentTab() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["catalog", "equipment"],
    queryFn: ({ signal }) => catalogApi.listEquipment(signal),
    staleTime: Infinity,
  });

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} />;
  if (!data?.length)
    return <p className="text-muted-foreground text-sm text-center py-8">No equipment found.</p>;

  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {data.map((e) => (
        <CatalogItemCard key={e.id} icon={e.icon} name={e.name} />
      ))}
    </div>
  );
}

function TrainingGoalsTab() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["catalog", "training-goals"],
    queryFn: ({ signal }) => catalogApi.listTrainingGoals(signal),
    staleTime: Infinity,
  });

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} />;
  if (!data?.length)
    return <p className="text-muted-foreground text-sm text-center py-8">No training goals found.</p>;

  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {data.map((g) => (
        <CatalogItemCard key={g.id} icon={g.icon} name={g.name} detail={g.detail} />
      ))}
    </div>
  );
}

function BrowseCatalogContent() {
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
  );
}

type CatalogMode = "view" | "edit";

export function CatalogPage() {
  const [params, setParams] = useSearchParams();
  const mode: CatalogMode = params.get("mode") === "edit" ? "edit" : "view";

  const setMode = (next: CatalogMode) => {
    if (next === "edit") setParams({ mode: "edit" }, { replace: true });
    else setParams({}, { replace: true });
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6 text-center space-y-4">
        <div>
          <h1 className="text-3xl font-bold">Catalog</h1>
          <p className="text-muted-foreground mt-1">
            {mode === "edit"
              ? "Manage activities, equipment, training goals and muscles"
              : "Browse activities, equipment, training goals and muscles"}
          </p>
        </div>

        <div className="inline-flex items-center rounded-lg border border-border bg-muted/40 p-1">
          <button
            type="button"
            onClick={() => setMode("view")}
            className={cn(
              "inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
              mode === "view"
                ? "bg-background text-foreground shadow-sm"
                : "text-muted-foreground hover:text-foreground"
            )}
          >
            <Eye className="h-3.5 w-3.5" />
            View
          </button>
          <button
            type="button"
            onClick={() => setMode("edit")}
            className={cn(
              "inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
              mode === "edit"
                ? "bg-background text-foreground shadow-sm"
                : "text-muted-foreground hover:text-foreground"
            )}
          >
            <Pencil className="h-3.5 w-3.5" />
            Edit
          </button>
        </div>
      </div>

      {mode === "edit" ? <AdminCatalogContent /> : <BrowseCatalogContent />}
    </div>
  );
}
