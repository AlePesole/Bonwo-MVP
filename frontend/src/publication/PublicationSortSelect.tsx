import type { PublicationSort } from "@/types/api";
import { ArrowUpDown } from "lucide-react";

const SORT_OPTIONS: { value: PublicationSort; label: string }[] = [
  { value: "RECENT", label: "Recent" },
  { value: "MOST_LIKED", label: "Most liked" },
  { value: "MOST_VIEWED", label: "Most viewed" },
  { value: "MOST_USED", label: "Most used" },
];

export function PublicationSortSelect({
  value,
  onChange,
}: {
  value?: PublicationSort;
  onChange: (sort: PublicationSort) => void;
}) {
  return (
    <div className="relative">
      <ArrowUpDown className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
      <select
        value={value ?? "RECENT"}
        onChange={(e) => onChange(e.target.value as PublicationSort)}
        className="h-8 rounded-md border border-input bg-background pl-8 pr-2 text-xs focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
        aria-label="Sort publications"
      >
        {SORT_OPTIONS.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    </div>
  );
}
