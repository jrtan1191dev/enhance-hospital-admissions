import {
  createRootRoute,
  createRoute,
  createRouter,
  Outlet,
  Navigate,
} from '@tanstack/react-router';
import { Header } from './components/layout/Header';
import { EdRoute } from './routes/ed';
import { SpecialistRoute } from './routes/specialist';
import { BmuRoute } from './routes/bmu';
import { BmuConfigRoute } from './routes/bmu-config';
import { PatientRoute } from './routes/patient';
import { WardRoute } from './routes/ward';

// Root Layout Route
const rootRoute = createRootRoute({
  component: () => (
    <div className="min-h-screen bg-slate-50 flex flex-col font-sans">
      <Header />
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 bg-white py-4 text-center text-xs text-slate-500">
        SingHealth Bed Capacity Orchestration System • Prototype Evaluation Mode
      </footer>
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

const routeTree = rootRoute.addChildren([
  indexRoute,
  edRoute,
  specialistRoute,
  bmuRoute,
  bmuConfigRoute,
  patientRoute,
  wardRoute,
]);

export const router = createRouter({ routeTree });

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}
