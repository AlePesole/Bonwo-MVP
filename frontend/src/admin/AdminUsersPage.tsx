import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "./api";
import { EditUserDialog } from "./EditUserDialog";
import { getErrorMessage } from "@/lib/axios";
import type { UserResponse, UserRole } from "@/types/api";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { PageSpinner } from "@/components/Spinner";
import { ApiError } from "@/components/ApiError";
import { ChevronLeft, ChevronRight, MoreHorizontal } from "lucide-react";

const PAGE_SIZE = 20;

function statusBadge(status: string) {
  if (status === "ACTIVE") return <Badge variant="secondary">Active</Badge>;
  if (status === "BANNED") return <Badge variant="destructive">Banned</Badge>;
  return <Badge variant="outline">{status}</Badge>;
}

function roleBadge(role: UserRole) {
  return role === "ADMIN" ? (
    <Badge className="bg-amber-100 text-amber-800 border-amber-200">Admin</Badge>
  ) : (
    <Badge variant="outline">User</Badge>
  );
}

export function AdminUsersPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [editUser, setEditUser] = useState<UserResponse | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ["admin", "users", page],
    queryFn: () => adminApi.listUsers(page, PAGE_SIZE),
  });

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ["admin", "users"] });
    setActionError(null);
  };

  const banMutation = useMutation({
    mutationFn: adminApi.banUser,
    onSuccess: invalidate,
    onError: (err) => setActionError(getErrorMessage(err)),
  });

  const unbanMutation = useMutation({
    mutationFn: adminApi.unbanUser,
    onSuccess: invalidate,
    onError: (err) => setActionError(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: adminApi.deleteUser,
    onSuccess: invalidate,
    onError: (err) => setActionError(getErrorMessage(err)),
  });

  const roleMutation = useMutation({
    mutationFn: ({ id, role }: { id: number; role: UserRole }) => adminApi.changeRole(id, role),
    onSuccess: invalidate,
    onError: (err) => setActionError(getErrorMessage(err)),
  });

  if (isLoading) return <PageSpinner />;
  if (error) return <ApiError message={getErrorMessage(error)} className="max-w-lg mx-auto" />;
  if (!data) return null;

  const totalPages = data.totalPages;

  return (
    <div className="max-w-6xl mx-auto space-y-4">
      <div className="text-center">
        <h1 className="text-3xl font-bold">Users</h1>
        <p className="text-muted-foreground mt-1">
          {data.totalElements} total users
        </p>
      </div>

      {actionError && <ApiError message={actionError} />}

      <div className="rounded-lg border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Username</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Joined</TableHead>
              <TableHead className="w-12" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.map((u) => (
              <TableRow key={u.id}>
                <TableCell className="font-medium">@{u.username}</TableCell>
                <TableCell className="text-muted-foreground text-sm">{u.email}</TableCell>
                <TableCell>{roleBadge(u.role)}</TableCell>
                <TableCell>{statusBadge(u.status)}</TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {new Date(u.createdAt).toLocaleDateString()}
                </TableCell>
                <TableCell>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon" className="h-8 w-8">
                        <MoreHorizontal className="h-4 w-4" />
                        <span className="sr-only">Actions</span>
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuLabel>Actions</DropdownMenuLabel>
                      <DropdownMenuSeparator />

                      <DropdownMenuItem onClick={() => setEditUser(u)}>
                        Edit username / bio
                      </DropdownMenuItem>

                      {u.status === "ACTIVE" ? (
                        <DropdownMenuItem
                          className="text-amber-600 focus:text-amber-600"
                          onClick={() => banMutation.mutate(u.id)}
                        >
                          Ban user
                        </DropdownMenuItem>
                      ) : u.status === "BANNED" ? (
                        <DropdownMenuItem onClick={() => unbanMutation.mutate(u.id)}>
                          Unban user
                        </DropdownMenuItem>
                      ) : null}

                      <DropdownMenuItem
                        onClick={() =>
                          roleMutation.mutate({
                            id: u.id,
                            role: u.role === "ADMIN" ? "USER" : "ADMIN",
                          })
                        }
                      >
                        {u.role === "ADMIN" ? "Demote to User" : "Promote to Admin"}
                      </DropdownMenuItem>

                      <DropdownMenuSeparator />

                      <DropdownMenuItem
                        className="text-destructive focus:text-destructive"
                        onClick={() => {
                          if (confirm(`Delete @${u.username}? This cannot be undone.`)) {
                            deleteMutation.mutate(u.id);
                          }
                        }}
                      >
                        Delete (soft)
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={data.first}
              onClick={() => setPage((p) => p - 1)}
            >
              <ChevronLeft className="h-4 w-4" />
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={data.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}

      <EditUserDialog user={editUser} onClose={() => setEditUser(null)} />
    </div>
  );
}
