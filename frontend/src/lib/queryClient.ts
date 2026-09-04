import { QueryClient } from "@tanstack/react-query";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      gcTime: 1000 * 60 * 30, // keep unused cache 30 min (faster back-nav)
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
});

// Catalog rarely changes — avoid remount refetch storms under rapid navigation
queryClient.setQueryDefaults(["catalog"], {
  staleTime: Infinity,
  gcTime: 1000 * 60 * 60,
});
