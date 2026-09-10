import {
  createRootRoute,
  createRoute,
  createRouter,
  Outlet,
  Navigate,
} from '@tanstack/react-router';
import { AlertTriangle, RotateCcw } from 'lucide-react';
import { Header } from './components/layout/Header';
import { ToastContainer } from './components/ui/toast';
import { EdRoute } from './routes/ed';
import { SpecialistRoute } from './routes/specialist';
import { BmuRoute } from './routes/bmu';
import { BmuConfigRoute } from './routes/bmu-config';
import { PatientRoute } from './routes/patient';
import { WardRoute } from './routes/ward';
import { AnalyticsRoute } from './routes/analytics';

// Root Layout Route
const rootRoute = createRootRoute({
  component: () => (
    <div className="min-h-screen bg-slate-50 flex flex-col font-sans">
      <Header />
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 bg-white py-4 text-center text-xs text-slate-500">
        Emergency & Bed Capacity Orchestration System • Prototype Evaluation Mode
      </footer>
      <ToastContainer />
    </div>
  ),
});

// Route Definitions
const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: () => <Navigate to="/ed" />,
});

const edRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/ed',
  component: EdRoute,
});

const specialistRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/specialist',
  component: SpecialistRoute,
});

const bmuRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/bmu',
  component: BmuRoute,
});

const bmuConfigRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/bmu/config',
  component: BmuConfigRoute,
});

const patientRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/patient',
  component: PatientRoute,
});

const wardRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/ward',
  component: WardRoute,
});

const analyticsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/analytics',
  component: AnalyticsRoute,
});

const routeTree = rootRoute.addChildren([
  indexRoute,
  edRoute,
  specialistRoute,
  bmuRoute,
  bmuConfigRoute,
  patientRoute,
  wardRoute,
  analyticsRoute,
]);

export const router = createRouter({
  routeTree,
  defaultErrorComponent: ({ error, reset }) => (
    <div className="max-w-md mx-auto my-12 p-6 bg-white border border-rose-200 rounded-2xl shadow-xs text-center">
      <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-rose-100 text-rose-600 mb-3">
        <AlertTriangle className="w-6 h-6" />
      </div>
      <h2 className="text-base font-semibold text-slate-900">View encountered an error</h2>
      <p className="text-xs text-slate-600 mt-1.5 mb-4 leading-relaxed">
        {(error instanceof Error ? error.message : String(error)) || 'An unexpected error occurred loading this view.'}
      </p>
      <button
        type="button"
        onClick={() => reset()}
        className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-slate-900 text-white rounded-lg text-xs font-medium hover:bg-slate-800 transition-colors cursor-pointer"
      >
        <RotateCcw className="w-3.5 h-3.5" />
        Try Again
      </button>
    </div>
  ),
});

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}
