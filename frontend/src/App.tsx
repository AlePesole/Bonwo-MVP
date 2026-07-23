import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "@/auth/AuthContext";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { LoginPage } from "@/auth/LoginPage";
import { RegisterPage } from "@/auth/RegisterPage";
import { Layout } from "@/components/Layout";
import { MyProfilePage } from "@/profile/MyProfilePage";
import { PublicProfilePage } from "@/profile/PublicProfilePage";
import { AdminUsersPage } from "@/admin/AdminUsersPage";
import { AdminCatalogPage } from "@/admin/AdminCatalogPage";
import { CatalogPage } from "@/catalog/CatalogPage";
import { LibraryPage } from "@/library/LibraryPage";
import { WorkoutsPage } from "@/library/WorkoutsPage";
import { RoutinesPage } from "@/library/RoutinesPage";

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Public auth routes (no navbar) */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Main layout with navbar */}
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/library" replace />} />
          <Route path="/users/:username" element={<PublicProfilePage />} />
          <Route
            path="/catalog"
            element={
              <ProtectedRoute requireAdmin>
                <CatalogPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <MyProfilePage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/library"
            element={
              <ProtectedRoute>
                <LibraryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/library/workouts"
            element={
              <ProtectedRoute>
                <WorkoutsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/library/routines"
            element={
              <ProtectedRoute>
                <RoutinesPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/users"
            element={
              <ProtectedRoute requireAdmin>
                <AdminUsersPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/catalog"
            element={
              <ProtectedRoute requireAdmin>
                <AdminCatalogPage />
              </ProtectedRoute>
            }
          />
        </Route>

        <Route path="*" element={<Navigate to="/library" replace />} />
      </Routes>
    </AuthProvider>
  );
}
