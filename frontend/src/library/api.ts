import { api } from "@/lib/axios";
import type { LibraryFolderDetail, LibraryFolderSummary } from "@/types/api";

export const libraryApi = {
  listFolders: (signal?: AbortSignal) =>
    api.get<LibraryFolderSummary[]>("/library/folders", { signal }).then((r) => r.data),

  getFolderDetail: (id: number, signal?: AbortSignal) =>
    api.get<LibraryFolderDetail>(`/library/folders/${id}`, { signal }).then((r) => r.data),

  createFolder: (name: string) =>
    api.post<LibraryFolderSummary>("/library/folders", { name }).then((r) => r.data),

  renameFolder: (id: number, name: string) =>
    api
      .patch<LibraryFolderSummary>(`/library/folders/${id}`, { name })
      .then((r) => r.data),

  deleteFolder: (id: number) => api.delete(`/library/folders/${id}`),

  addItem: (folderId: number, referenceId: number, type: string) =>
    api.post(`/library/folders/${folderId}/items`, { referenceId, type }),

  removeItem: (folderId: number, referenceId: number, type: string) =>
    api.delete(`/library/folders/${folderId}/items/${referenceId}`, {
      params: { type },
    }),
};
