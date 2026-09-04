import { useMemo, useRef, useState, type ReactNode } from "react";
import { cn } from "@/lib/utils";
import type {
  ActivationLevel,
  MuscleGroupResponse,
  MuscleSubGroupResponse,
} from "@/types/api";
import { Dumbbell } from "lucide-react";

/** Resolve primary muscle groups for card icons; falls back to embedded subGroup when catalog isn't ready. */
export function resolvePrimaryMuscleGroups(
  muscles: { role: ActivationLevel; subGroup?: MuscleSubGroupResponse | null }[],
  muscleGroupMap: Map<number, MuscleGroupResponse>,
  limit = Infinity
): MuscleGroupResponse[] {
  const seen = new Set<number>();
  const groups: MuscleGroupResponse[] = [];
  for (const m of muscles) {
    if (m.role !== "PRIMARY") continue;
    const groupId = m.subGroup?.groupId;
    if (groupId == null || seen.has(groupId)) continue;
    seen.add(groupId);
    const fromCatalog = muscleGroupMap.get(groupId);
    if (fromCatalog) {
      groups.push(fromCatalog);
    } else if (m.subGroup) {
      groups.push({
        id: groupId,
        name: m.subGroup.name,
        icon: m.subGroup.icon,
        subGroups: [],
      });
    }
    if (groups.length >= limit) break;
  }
  return groups;
}

// ── Shared drag-to-scroll row ─────────────────────────────────────────────────

function DragScrollRow({ children }: { children: ReactNode }) {
  const scrollerRef = useRef<HTMLDivElement>(null);
  const drag = useRef({
    active: false,
    startX: 0,
    scrollLeft: 0,
    moved: false,
  });
  const [grabbing, setGrabbing] = useState(false);

  const onPointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (e.button !== 0 && e.pointerType === "mouse") return;
    const el = scrollerRef.current;
    if (!el) return;
    drag.current = {
      active: true,
      startX: e.clientX,
      scrollLeft: el.scrollLeft,
      moved: false,
    };
  };

  const onPointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!drag.current.active) return;
    const el = scrollerRef.current;
    if (!el) return;
    const dx = e.clientX - drag.current.startX;
    if (!drag.current.moved && Math.abs(dx) < 6) return;

    if (!drag.current.moved) {
      drag.current.moved = true;
      setGrabbing(true);
      el.setPointerCapture(e.pointerId);
    }
    el.scrollLeft = drag.current.scrollLeft - dx;
  };

  const endDrag = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!drag.current.active) return;
    const wasDrag = drag.current.moved;
    drag.current.active = false;
    setGrabbing(false);
    if (wasDrag) {
      try {
        scrollerRef.current?.releasePointerCapture(e.pointerId);
      } catch {
        /* already released */
      }
    }
    requestAnimationFrame(() => {
      drag.current.moved = false;
    });
  };

  return (
    <div
      ref={scrollerRef}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={endDrag}
      onPointerCancel={endDrag}
      className={cn(
        "overflow-x-auto scrollbar-none -mx-1 px-1 select-none",
        grabbing ? "cursor-grabbing" : "cursor-grab"
      )}
    >
      <div
        className="flex items-start gap-2 w-max pb-1"
        onClickCapture={(e) => {
          if (drag.current.moved) {
            e.preventDefault();
            e.stopPropagation();
          }
        }}
      >
        {children}
      </div>
    </div>
  );
}

function MuscleChip({
  active,
  name,
  iconUrl,
  onClick,
}: {
  active: boolean;
  name: string;
  iconUrl?: string | null;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "flex flex-col items-center w-16 shrink-0 rounded-lg border overflow-hidden transition-colors",
        active
          ? "border-primary bg-primary/10 text-primary"
          : "border-border bg-card/50 text-muted-foreground hover:border-primary/40 hover:bg-accent/30"
      )}
    >
      <div
        className={cn(
          "w-full aspect-square flex items-center justify-center pointer-events-none",
          active ? "bg-primary/20" : "bg-muted"
        )}
      >
        {iconUrl ? (
          <img src={iconUrl} alt={name} className="h-full w-full object-contain" draggable={false} />
        ) : (
          <Dumbbell className="h-4 w-4" />
        )}
      </div>
      <span className="text-[10px] font-medium leading-tight text-center line-clamp-2 w-full px-1 py-1.5 pointer-events-none">
        {name}
      </span>
    </button>
  );
}

// ── Public component ──────────────────────────────────────────────────────────

export function MuscleGroupFilterRow({
  muscleGroups,
  selectedGroupIds,
  selectedSubGroupIds,
  onGroupChange,
  onSubGroupChange,
  isPending = false,
  isError = false,
}: {
  muscleGroups: MuscleGroupResponse[];
  selectedGroupIds: number[];
  selectedSubGroupIds: number[];
  onGroupChange: (ids: number[]) => void;
  onSubGroupChange: (ids: number[]) => void;
  isPending?: boolean;
  isError?: boolean;
}) {
  const subGroups = useMemo(() => {
    if (selectedGroupIds.length === 0) return [] as MuscleSubGroupResponse[];
    const selected = new Set(selectedGroupIds);
    return muscleGroups
      .filter((g) => selected.has(g.id))
      .flatMap((g) => g.subGroups);
  }, [muscleGroups, selectedGroupIds]);

  if (muscleGroups.length === 0) {
    if (isPending) {
      return <p className="text-xs text-muted-foreground py-1">Loading muscles…</p>;
    }
    if (isError) {
      return <p className="text-xs text-destructive py-1">Couldn't load muscle filters.</p>;
    }
    return null;
  }

  const toggleGroup = (id: number) => {
    const next = selectedGroupIds.includes(id)
      ? selectedGroupIds.filter((x) => x !== id)
      : [...selectedGroupIds, id];
    onGroupChange(next);
  };

  const toggleSubGroup = (id: number) => {
    onSubGroupChange(
      selectedSubGroupIds.includes(id)
        ? selectedSubGroupIds.filter((x) => x !== id)
        : [...selectedSubGroupIds, id]
    );
  };

  return (
    <div className="space-y-2">
      <DragScrollRow>
        {muscleGroups.map((g) => (
          <MuscleChip
            key={g.id}
            active={selectedGroupIds.includes(g.id)}
            name={g.name}
            iconUrl={g.icon?.url}
            onClick={() => toggleGroup(g.id)}
          />
        ))}
      </DragScrollRow>

      {subGroups.length > 0 && (
        <DragScrollRow>
          {subGroups.map((s) => (
            <MuscleChip
              key={s.id}
              active={selectedSubGroupIds.includes(s.id)}
              name={s.name}
              iconUrl={s.icon?.url}
              onClick={() => toggleSubGroup(s.id)}
            />
          ))}
        </DragScrollRow>
      )}
    </div>
  );
}
