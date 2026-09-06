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
import ProgramsPage from "@/library/ProgramsPage";
import { LibraryCollectionPage } from "@/library/LibraryCollectionPage";
import { SessionsPage } from "@/session/SessionsPage";
import { PublicationPage } from "@/publication/PublicationPage";
import { PublicationExercisesPage } from "@/publication/PublicationExercisesPage";
import { ExplorePage } from "@/explore/ExplorePage";
import { TrainingSessionPage } from "@/session/TrainingSessionPage";

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

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
            path="/library/programs"
            element={
              <ProtectedRoute>
                <ProgramsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/library/saves"
            element={
              <ProtectedRoute>
                <LibraryCollectionPage source="saves" />
              </ProtectedRoute>
            }
          />
          <Route
            path="/library/likes"
            element={
              <ProtectedRoute>
                <LibraryCollectionPage source="likes" />
              </ProtectedRoute>
            }
          />
          <Route
            path="/sessions"
            element={
              <ProtectedRoute>
                <SessionsPage />
              </ProtectedRoute>
            }
          />
          <Route path="/library/sessions" element={<Navigate to="/sessions" replace />} />
          <Route
            path="/training-sessions/:id"
            element={
              <ProtectedRoute>
                <TrainingSessionPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/explore"
            element={
              <ProtectedRoute>
                <ExplorePage />
              </ProtectedRoute>
            }
          />
          <Route path="/explore/:scope" element={<Navigate to="/explore" replace />} />
          <Route path="/explore/:scope/exercises" element={<Navigate to="/explore" replace />} />

          <Route
            path="/publications"
            element={
              <ProtectedRoute>
                <PublicationPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/publications/exercises"
            element={
              <ProtectedRoute>
                <PublicationExercisesPage />
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
