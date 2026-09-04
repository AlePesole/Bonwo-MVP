import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { catalogApi } from "./api";
import { BODY_DIAGRAMS } from "./bodyDiagram";
import { PageSpinner } from "@/components/Spinner";
import { ApiError } from "@/components/ApiError";
import { getErrorMessage } from "@/lib/axios";
import type { MuscleSubGroupResponse } from "@/types/api";
import { cn } from "@/lib/utils";

function BodyPanel({
  view,
  subGroups,
  hoveredId,
  onHover,
}: {
  view: "front" | "back";
  subGroups: MuscleSubGroupResponse[];
  hoveredId: number | null;
  onHover: (id: number | null) => void;
}) {
  const { src, viewBox } = BODY_DIAGRAMS[view];
  const visible = subGroups.filter((s) =>
    view === "front" ? s.svgPathFront != null : s.svgPathBack != null
  );

  return (
    <div className="flex flex-col items-center gap-1.5 flex-1 min-w-0">
      <span className="text-xs font-medium text-muted-foreground uppercase tracking-widest">
        {view === "front" ? "Front" : "Back"}
      </span>
      <div className="relative w-full rounded-xl bg-white/[0.06] border border-primary/40 p-3">
        <div className="relative">
          <img
            src={src}
            alt={`Body ${view}`}
            className="w-full select-none pointer-events-none"
          />
          <svg
            viewBox={viewBox}
            className="absolute inset-0 h-full w-full"
            preserveAspectRatio="none"
          >
          {visible.map((s) => (
            <path
              key={s.id}
              d={(view === "front" ? s.svgPathFront : s.svgPathBack) ?? undefined}
              className={cn(
                "cursor-pointer transition-colors",
                hoveredId === s.id
                  ? "fill-primary/70"
                  : "fill-primary/0 hover:fill-primary/40"
              )}
              stroke="transparent"
              onMouseEnter={() => onHover(s.id)}
              onMouseLeave={() => onHover(null)}
            />
          ))}
        </svg>
        </div>
      </div>
    </div>
  );
}

export function MuscleMap() {
  const [hoveredId, setHoveredId] = useState<number | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ["catalog", "muscles"],
    queryFn: ({ signal }) => catalogApi.listMuscleGroups(signal),
  });

  const subGroups = useMemo(() => data?.flatMap((g) => g.subGroups) ?? [], [data]);
  const hovered = useMemo(
    () => subGroups.find((s) => s.id === hoveredId) ?? null,
    [subGroups, hoveredId]
  );
  const hoveredGroup = useMemo(
    () => (hovered ? (data?.find((g) => g.id === hovered.groupId) ?? null) : null),
    [hovered, data]
  );

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} />;

  return (
    <div className="max-w-lg mx-auto">
      <div className="flex gap-4 items-start">
        <BodyPanel view="front" subGroups={subGroups} hoveredId={hoveredId} onHover={setHoveredId} />
        <BodyPanel view="back"  subGroups={subGroups} hoveredId={hoveredId} onHover={setHoveredId} />
      </div>

      <p className="text-center text-sm text-muted-foreground mt-3 h-5">
        {hovered
          ? <>
              <span className="text-muted-foreground">{hoveredGroup?.name}</span>
              <span className="mx-1.5 text-muted-foreground/50">·</span>
              <span className="font-medium text-foreground">{hovered.name}</span>
            </>
          : "Hover a muscle to see its name"}
      </p>
    </div>
  );
}