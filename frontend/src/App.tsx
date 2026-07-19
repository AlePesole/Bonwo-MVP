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

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Public auth routes (no navbar) */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Main layout with navbar */}
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/profile" replace />} />
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

        <Route path="*" element={<Navigate to="/profile" replace />} />
      </Routes>
    </AuthProvider>
  );
}
