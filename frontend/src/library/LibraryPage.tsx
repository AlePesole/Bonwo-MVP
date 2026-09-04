import { Link } from "react-router-dom";
import { Bookmark, CalendarDays, Dumbbell, Heart, Layers } from "lucide-react";

interface SectionCard {
  title: string;
  description: string;
  icon: React.ReactNode;
  to: string;
}

const BUILT_SECTIONS: SectionCard[] = [
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

const COLLECTION_SECTIONS: SectionCard[] = [
  {
    title: "Saves",
    description: "Publications you bookmarked for later.",
    icon: <Bookmark className="h-6 w-6" />,
    to: "/library/saves",
  },
  {
    title: "Likes",
    description: "Publications you liked.",
    icon: <Heart className="h-6 w-6" />,
    to: "/library/likes",
  },
];

function SectionGrid({ sections }: { sections: SectionCard[] }) {
  return (
    <div className="grid gap-4 sm:grid-cols-3">
      {sections.map((s) => (
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
  );
}

export function LibraryPage() {
  return (
    <div className="max-w-3xl mx-auto space-y-10">
      <div className="text-center">
        <h1 className="text-3xl font-bold">My Library</h1>
        <p className="text-muted-foreground mt-1">Everything you've built and collected, in one place.</p>
      </div>

      <section className="space-y-4">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider text-center">
          Built by you
        </h2>
        <SectionGrid sections={BUILT_SECTIONS} />
      </section>

      <section className="space-y-4">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider text-center">
          Collections
        </h2>
        <SectionGrid sections={COLLECTION_SECTIONS} />
      </section>
    </div>
  );
}
