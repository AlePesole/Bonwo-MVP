import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient, keepPreviousData } from "@tanstack/react-query";
import { sessionApi } from "@/session/api";
import { displayDuration } from "@/routine/api";
import { cn, formatTimeAgo } from "@/lib/utils";
import { getErrorMessage } from "@/lib/axios";
import type { TrainingSessionResponse } from "@/types/api";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/components/ApiError";
import { PageSpinner } from "@/components/Spinner";
import { CheckCircle2, Clock, Play, Timer, Trash2 } from "lucide-react";

function SessionCard({
  session,
  deleting,
  onDelete,
}: {
  session: TrainingSessionResponse;
  deleting: boolean;
  onDelete: (id: number) => void;
}) {
  const doneSets = session.slots.reduce((acc, s) => acc + s.sets.filter((x) => x.done).length, 0);
  const totalSets = session.slots.reduce((acc, s) => acc + s.sets.length, 0);
  const inProgress = session.status === "IN_PROGRESS";

  return (
    <div
      className={cn(
        "rounded-xl border bg-card p-4 hover:border-primary transition-colors",
        inProgress ? "border-sky-500/50 bg-sky-500/5" : "border-primary/40"
      )}
    >
      <div className="flex items-start gap-3">
        <Link to={`/training-sessions/${session.id}`} className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <h3 className="font-semibold truncate">{session.routineTitle}</h3>
              <p className="text-xs text-muted-foreground mt-1">
                {formatTimeAgo(session.startedAt)}
                {session.status === "COMPLETED" && session.duration
                  ? ` · ${displayDuration(session.duration)}`
                  : null}
                {` · ${doneSets}/${totalSets} sets`}
              </p>
            </div>
            <span
              className={cn(
                "shrink-0 inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold border",
                inProgress
                  ? "bg-sky-500/15 text-sky-300 border-sky-500/30"
                  : "bg-emerald-500/15 text-emerald-300 border-emerald-500/30"
              )}
            >
              {inProgress ? (
                <>
                  <Play className="h-3 w-3" /> In progress
                </>
              ) : (
                <>
                  <CheckCircle2 className="h-3 w-3" /> Done
                </>
              )}
            </span>
          </div>
        </Link>
        <button
          type="button"
          disabled={deleting}
          onClick={() => {
            const label = inProgress ? "Discard this training session?" : "Delete this training session?";
            if (window.confirm(label)) onDelete(session.id);
          }}
          className="shrink-0 p-1.5 rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors disabled:opacity-50"
          aria-label="Delete session"
          title="Delete"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

export function SessionsPage() {
  const [page, setPage] = useState(0);
  const qc = useQueryClient();
  const [actionError, setActionError] = useState<string | null>(null);

  const { data: recent } = useQuery({
    queryKey: ["training-sessions", "active-probe"],
    queryFn: ({ signal }) => sessionApi.listMine(0, 20, signal),
  });

  const { data, isLoading, error } = useQuery({
    queryKey: ["training-sessions", page],
    queryFn: ({ signal }) => sessionApi.listMine(page, 12, signal),
    placeholderData: keepPreviousData,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => sessionApi.delete(id),
    onSuccess: () => {
      setActionError(null);
      qc.invalidateQueries({ queryKey: ["training-sessions"] });
    },
    onError: (err) => setActionError(getErrorMessage(err)),
  });

  const active = useMemo(
    () => recent?.content.find((s) => s.status === "IN_PROGRESS") ?? null,
    [recent]
  );

  const sessions = data?.content ?? [];
  const history = useMemo(
    () => (active ? sessions.filter((s) => s.id !== active.id) : sessions),
    [sessions, active]
  );

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="text-center space-y-1">
        <h1 className="text-3xl font-bold">Sessions</h1>
        <p className="text-muted-foreground text-sm">
          Continue an active workout or review past sessions.
        </p>
      </div>

      {isLoading && <PageSpinner />}
      {error && <ApiError message={getErrorMessage(error)} />}
      {actionError && <ApiError message={actionError} />}

      {!isLoading && !error && (
        <>
          {active && (
            <div className="rounded-xl border border-sky-500/40 bg-sky-500/10 p-4 space-y-3">
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2 text-sky-200">
                  <Timer className="h-4 w-4" />
                  <span className="text-sm font-semibold">Session in progress</span>
                </div>
                <button
                  type="button"
                  disabled={deleteMutation.isPending}
                  onClick={() => {
                    if (window.confirm("Discard this training session?")) {
                      deleteMutation.mutate(active.id);
                    }
                  }}
                  className="shrink-0 p-1.5 rounded-md text-sky-200/70 hover:text-destructive hover:bg-destructive/10 transition-colors disabled:opacity-50"
                  aria-label="Discard session"
                  title="Discard"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
              <p className="text-sm font-medium">{active.routineTitle}</p>
              <p className="text-xs text-muted-foreground">Started {formatTimeAgo(active.startedAt)}</p>
              <Button asChild className="w-full gap-2">
                <Link to={`/training-sessions/${active.id}`}>
                  <Play className="h-4 w-4" /> Continue workout
                </Link>
              </Button>
            </div>
          )}

          <div className="space-y-3">
            {history.map((s) => (
              <SessionCard
                key={s.id}
                session={s}
                deleting={deleteMutation.isPending && deleteMutation.variables === s.id}
                onDelete={(id) => deleteMutation.mutate(id)}
              />
            ))}
            {!sessions.length && (
              <div className="text-center py-16 text-muted-foreground">
                <Clock className="h-10 w-10 mx-auto mb-3 opacity-30" />
                <p className="text-sm">No training sessions yet.</p>
                <p className="text-xs mt-1">Open a routine and tap Start routine to begin.</p>
              </div>
            )}
          </div>

          {data && data.totalPages > 1 && (
            <div className="flex justify-center items-center gap-2">
              <button
                type="button"
                disabled={data.first}
                onClick={() => setPage((p) => p - 1)}
                className="text-xs text-muted-foreground hover:text-foreground disabled:opacity-40 px-2 py-1 rounded border border-border"
              >
                ← Prev
              </button>
              <span className="text-xs text-muted-foreground">
                {page + 1} / {data.totalPages}
              </span>
              <button
                type="button"
                disabled={data.last}
                onClick={() => setPage((p) => p + 1)}
                className="text-xs text-muted-foreground hover:text-foreground disabled:opacity-40 px-2 py-1 rounded border border-border"
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
