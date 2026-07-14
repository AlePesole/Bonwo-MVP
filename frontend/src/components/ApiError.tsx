import { AlertCircle } from "lucide-react";
import { cn } from "@/lib/utils";

interface ApiErrorProps {
  message: string;
  className?: string;
}

export function ApiError({ message, className }: ApiErrorProps) {
  return (
    <div
      className={cn(
        "flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive",
        className
      )}
      role="alert"
    >
      <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
      <span>{message}</span>
    </div>
  );
}
