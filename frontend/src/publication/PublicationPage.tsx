import { Link } from "react-router-dom";
import { CalendarDays, Dumbbell, Layers } from "lucide-react";

interface SectionCard {
  title: string;
  description: string;
  icon: React.ReactNode;
  to?: string;
  comingSoon?: boolean;
}

const SECTIONS: SectionCard[] = [
  {
    title: "Exercises",
    description: "Publish exercises with video, track likes, views and uses.",
    icon: <Dumbbell className="h-6 w-6" />,
    to: "/publications/exercises",
  },
  {
    title: "Routines",
    description: "Publish structured workout plans with exercises and sets.",
    icon: <Layers className="h-6 w-6" />,
    comingSoon: true,
  },
  {
    title: "Programs",
    description: "Publish multi-week training programs built from routines.",
    icon: <CalendarDays className="h-6 w-6" />,
    comingSoon: true,
  },
];

export function PublicationPage() {
  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8 text-center">
        <h1 className="text-3xl font-bold">Publications</h1>
        <p className="text-muted-foreground mt-1">Share your work with the community.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        {SECTIONS.map((s) =>
          s.comingSoon || !s.to ? (
            <div
              key={s.title}
              className="relative rounded-xl border border-border/60 bg-card/50 p-5 opacity-60 cursor-not-allowed"
            >
              <span className="absolute top-3 right-3 text-[10px] font-semibold uppercase tracking-wide px-2 py-0.5 rounded-full border border-amber-500/40 bg-amber-500/15 text-amber-400">
                Coming soon
              </span>
              <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center text-muted-foreground mb-3">
                {s.icon}
              </div>
              <h2 className="font-semibold text-foreground mb-1">{s.title}</h2>
              <p className="text-xs text-muted-foreground leading-snug">{s.description}</p>
            </div>
          ) : (
            <Link
              key={s.title}
              to={s.to}
              className="rounded-xl border border-primary/40 bg-card p-5 hover:border-primary hover:bg-card/80 transition-colors group"
            >
              <div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center text-primary mb-3 group-hover:bg-primary/20 transition-colors">
                {s.icon}
              </div>
              <h2 className="font-semibold text-foreground mb-1">{s.title}</h2>
              <p className="text-xs text-muted-foreground leading-snug">{s.description}</p>
            </Link>
          )
        )}
      </div>
    </div>
  );
}
