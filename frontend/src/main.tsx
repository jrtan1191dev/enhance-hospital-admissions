import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from '@tanstack/react-router';
import { router } from './router';
import { toast } from './lib/toast';
import './index.css';

const queryClient = new QueryClient({
  mutationCache: new MutationCache({
    onError: (error) => {
      toast.error('Action Failed', error.message || 'An unexpected error occurred processing your request.');
    },
  }),
  queryCache: new QueryCache({
    onError: (error, query) => {
      // Log polling/query background errors to preserve debug visibility without modal noise
      console.error(`[Query Error] ${String(query.queryKey)}:`, error);
    },
  }),
  defaultOptions: {
    queries: {
      staleTime: 1000,
      retry: (failureCount, error: any) => {
        // Do not retry 4xx client errors (400, 401, 403, 404, 409)
        const status = error?.response?.status;
        if (status && status >= 400 && status < 500) {
          return false;
        }
        return failureCount < 1;
      },
    },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
);
