import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { LayoutDashboard, LogOut, User, BookOpen } from "lucide-react";

function NavItem({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `text-sm font-medium transition-colors hover:text-primary ${
          isActive ? "text-primary" : "text-muted-foreground"
        }`
      }
    >
      {children}
    </NavLink>
  );
}

export function Layout() {
  const { isAuthenticated, isAdmin, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
  };

  const initials = user?.username?.slice(0, 2).toUpperCase() ?? "?";

  return (
    <div className="min-h-screen bg-background">
      {/* Navbar */}
      <header className="sticky top-0 z-40 border-b border-border/60 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container mx-auto flex h-16 items-center justify-between px-4">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2 font-bold text-xl">
            <img src="/bonwo-logo.png" alt="Bonwo" className="h-10 w-10" />
            <span className="text-foreground">Bonwo</span>
          </Link>

          {/* Nav links — only shown when authenticated */}
          <nav className="hidden md:flex items-center gap-6">
            {isAuthenticated && <NavItem to="/profile">My Profile</NavItem>}
            {isAuthenticated && <NavItem to="/library">Library</NavItem>}
            {isAdmin && (
              <>
                <NavItem to="/catalog">Browse Catalog</NavItem>
                <NavItem to="/admin/users">Users</NavItem>
                <NavItem to="/admin/catalog">Catalog</NavItem>
              </>
            )}
          </nav>

          {/* Right side */}
          <div className="flex items-center gap-3">
            {isAuthenticated && user ? (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <button className="flex items-center gap-2 rounded-full outline-none ring-offset-background focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2">
                    <Avatar className="h-8 w-8 border border-border">
                      <AvatarFallback className="bg-primary/20 text-primary text-xs font-semibold">
                        {initials}
                      </AvatarFallback>
                    </Avatar>
                    <span className="hidden sm:block text-sm font-medium">{user.username}</span>
                  </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-48">
                  <DropdownMenuLabel className="text-xs text-muted-foreground font-normal">
                    {user.email}
                  </DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem asChild>
                    <Link to="/profile" className="flex items-center gap-2 cursor-pointer">
                      <User className="h-4 w-4" />
                      My Profile
                    </Link>
                  </DropdownMenuItem>
                  {isAdmin && (
                    <>
                      <DropdownMenuItem asChild>
                        <Link to="/admin/users" className="flex items-center gap-2 cursor-pointer">
                          <LayoutDashboard className="h-4 w-4" />
                          Manage Users
                        </Link>
                      </DropdownMenuItem>
                      <DropdownMenuItem asChild>
                        <Link to="/admin/catalog" className="flex items-center gap-2 cursor-pointer">
                          <BookOpen className="h-4 w-4" />
                          Manage Catalog
                        </Link>
                      </DropdownMenuItem>
                    </>
                  )}
                  <DropdownMenuSeparator />
                  <DropdownMenuItem
                    className="flex items-center gap-2 cursor-pointer text-destructive focus:text-destructive"
                    onClick={handleLogout}
                  >
                    <LogOut className="h-4 w-4" />
                    Sign out
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            ) : (
              <div className="flex items-center gap-2">
                <Button variant="ghost" size="sm" asChild>
                  <Link to="/login">Sign in</Link>
                </Button>
                <Button size="sm" asChild>
                  <Link to="/register">Sign up</Link>
                </Button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Mobile nav — only for authenticated users */}
      {isAuthenticated && (
        <div className="md:hidden border-b border-border/60 px-4 py-2 flex gap-4 bg-background/95">
          <NavItem to="/profile">My Profile</NavItem>
          <NavItem to="/library">Library</NavItem>
          {isAdmin && (
            <>
              <NavItem to="/catalog">Browse Catalog</NavItem>
              <NavItem to="/admin/users">Users</NavItem>
              <NavItem to="/admin/catalog">Catalog</NavItem>
            </>
          )}
        </div>
      )}

      {/* Page content */}
      <main className="container mx-auto px-4 py-8">
        <Outlet />
      </main>
    </div>
  );
}
