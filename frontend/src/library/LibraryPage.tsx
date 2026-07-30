import { Link } from "react-router-dom";
import { CalendarDays, Dumbbell, Layers } from "lucide-react";

interface SectionCard {
  title: string;
  description: string;
  icon: React.ReactNode;
  to: string;
}

const SECTIONS: SectionCard[] = [
  {
    title: "My Exercises",
    description: "Your exercises organized and filterable.",
    icon: <Dumbbell className="h-6 w-6" />,
    to: "/library/workouts",
  },
  {
    title: "My Routines",
    description: "Structured workout plans with exercises and sets.",
    icon: <Layers className="h-6 w-6" />,
    to: "/library/routines",
  },
  {
    title: "My Programs",
    description: "Multi-week training programs built from routines.",
    icon: <CalendarDays className="h-6 w-6" />,
    to: "/library/programs",
  },
];

export function LibraryPage() {
  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8 text-center">
        <h1 className="text-3xl font-bold">My Library</h1>
        <p className="text-muted-foreground mt-1">Everything you've built, in one place.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        {SECTIONS.map((s) => (
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
        ))}
      </div>
    </div>
  );
}
