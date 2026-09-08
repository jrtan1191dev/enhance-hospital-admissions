import { Link } from '@tanstack/react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getActiveRole, setActiveRole } from '../../services/api';
import { authQueries } from '../../services/queries';
import type { RolePersona } from '../../types/admissions';
import { Badge } from '../ui/badge';
import { Activity, UserCheck, ShieldCheck, Menu, X } from 'lucide-react';
import { useState, useEffect } from 'react';

const ROLES: { id: RolePersona; label: string; icon: string; path: string; user: string }[] = [
  { id: 'ED_ATTENDING', label: 'ED Attending', icon: '🩺', path: '/ed', user: 'dr_tan_ed' },
  { id: 'SPECIALIST', label: 'Inpatient Specialist', icon: '👨‍⚕️', path: '/specialist', user: 'dr_lim_cardio' },
  { id: 'BMU_COORDINATOR', label: 'BMU Coordinator', icon: '🏢', path: '/bmu', user: 'bmu_coord_wong' },
  { id: 'WARD_NURSE', label: 'Ward Nurse', icon: '👩‍⚕️', path: '/ward', user: 'nurse_sarah' },
  { id: 'HOUSEKEEPING', label: 'EVS Housekeeping', icon: '🧹', path: '/ward', user: 'evs_staff_kumar' },
  { id: 'PATIENT', label: 'Patient Tracker', icon: '📱', path: '/patient', user: 'patient_p101' },
];

const NAV_ITEMS = [
  {
    path: '/ed',
    label: 'ED Clinical Intake',
    icon: '🩺',
    badge: 'Urgent',
    description: 'Triage, synthesized vitals & 1-click bed request',
  },
  {
    path: '/specialist',
    label: 'Specialist Consult Pool',
    icon: '👨‍⚕️',
    badge: 'Async',
    description: 'Broadcast review, case claiming & directives',
  },
  {
    path: '/bmu',
    label: 'BMU Capacity Hub',
    icon: '🏢',
    badge: 'Solver',
    description: 'Prioritized queue, heuristic scoring & allocation',
  },
  {
    path: '/bmu/config',
    label: 'Solver Weight Config',
    icon: '⚙️',
    badge: 'Tuning',
    description: 'Constraint weights & dynamic batch thresholds',
  },
  {
    path: '/ward',
    label: 'Ward & Bed Turnover',
    icon: '🛏️',
    badge: 'EVS',
    description: 'Nursing check-in, vacate & 30m sanitization SLA',
  },
  {
    path: '/patient',
    label: 'Patient Milestone Tracker',
    icon: '📱',
    badge: 'Public',
    description: 'Public mobile journey tracker & care insights',
  },
];

export function Header() {
  const [role, setRole] = useState<RolePersona>(getActiveRole());
  const [sidebarOpen, setSidebarOpen] = useState<boolean>(false);
  const queryClient = useQueryClient();
  const { data: auth } = useQuery(authQueries.me());

  // Close sidebar on Escape key
  useEffect(() => {
    if (!sidebarOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setSidebarOpen(false);
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [sidebarOpen]);

  const handleRoleChange = (newRole: RolePersona) => {
    setActiveRole(newRole);
    setRole(newRole);
    queryClient.invalidateQueries();
  };

  return (
    <>
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur shadow-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            {/* Left: Hamburger Button + Logo & Brand */}
            <div className="flex items-center gap-3 sm:gap-4">
              <button
                type="button"
                onClick={() => setSidebarOpen(true)}
                className="p-2 -ml-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors cursor-pointer"
                aria-label="Open Navigation Menu"
                aria-expanded={sidebarOpen}
              >
                <Menu className="h-6 w-6" />
              </button>

              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-lg bg-blue-600 flex items-center justify-center text-white shadow-sm font-bold text-lg">
                  🏥
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-slate-900 text-lg tracking-tight">PatientFlow & BedCapacity</span>
                    <Badge variant="secondary" className="text-[10px] font-mono px-1.5 py-0 bg-blue-50 text-blue-700 border-blue-200">
                      v1.0-PROTOTYPE
                    </Badge>
                  </div>
                  <p className="text-xs text-slate-500 hidden sm:block">Emergency & Bed Capacity Orchestration System</p>
                </div>
              </div>
            </div>

            {/* Right: Role Persona Switcher & Indicators */}
            <div className="flex items-center gap-2">
              <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-lg border border-slate-200">
                <span className="text-xs font-semibold px-2 text-slate-500 hidden sm:inline flex items-center gap-1">
                  <UserCheck className="h-3 w-3" /> Persona:
                </span>
                <select
                  value={role}
                  onChange={(e) => handleRoleChange(e.target.value as RolePersona)}
                  className="text-xs font-medium bg-white border border-slate-200 rounded px-2 py-1 text-slate-800 focus:outline-none focus:ring-1 focus:ring-blue-500 cursor-pointer shadow-xs"
                >
                  {ROLES.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.icon} {r.label} ({r.user})
                    </option>
                  ))}
                </select>
              </div>

              {auth?.authenticated && (
                <div className="hidden xl:flex items-center text-xs text-blue-700 bg-blue-50 border border-blue-200 px-2 py-1 rounded-md font-mono" title={`Spring Security Roles: ${auth.roles?.join(', ')}`}>
                  <ShieldCheck className="h-3 w-3 mr-1 text-blue-600" />
                  <span>{auth.username}</span>
                </div>
              )}

              <div className="hidden lg:flex items-center text-xs text-emerald-600 bg-emerald-50 border border-emerald-200 px-2 py-1 rounded-md">
                <Activity className="h-3 w-3 mr-1 animate-pulse" />
                <span>Live Sync</span>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Slide-out Navigation Drawer / Sidebar */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-50 flex">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs transition-opacity animate-in fade-in-0 duration-200"
            onClick={() => setSidebarOpen(false)}
            aria-hidden="true"
          />

          {/* Sidebar Sheet */}
          <div
            className="relative w-80 max-w-[85vw] bg-white shadow-2xl z-50 flex flex-col h-full border-r border-slate-200 animate-in slide-in-from-left duration-200"
            role="dialog"
            aria-modal="true"
            aria-label="Main Navigation Menu"
          >
            {/* Sidebar Header */}
            <div className="p-4 border-b border-slate-200 flex items-center justify-between bg-slate-50/70">
              <div className="flex items-center gap-2.5">
                <div className="h-9 w-9 rounded-lg bg-blue-600 flex items-center justify-center text-white shadow-xs font-bold text-base">
                  🏥
                </div>
                <div>
                  <div className="font-bold text-slate-900 text-sm tracking-tight leading-tight">PatientFlow & BedCapacity</div>
                  <div className="text-[11px] text-slate-500">SingHealth Hospital Network</div>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setSidebarOpen(false)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer"
                aria-label="Close navigation menu"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Navigation Links */}
            <nav className="flex-1 overflow-y-auto p-3 space-y-1.5">
              <div className="px-3 py-1.5 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
                Hospital Modules
              </div>

              {NAV_ITEMS.map((item) => (
                <Link
                  key={item.path}
                  to={item.path}
                  onClick={() => setSidebarOpen(false)}
                  className="flex items-start gap-3 px-3 py-2.5 rounded-xl text-sm transition-all"
                  activeProps={{ className: 'bg-blue-50 text-blue-900 font-semibold border border-blue-200/80 shadow-xs' }}
                  inactiveProps={{ className: 'text-slate-700 hover:bg-slate-100 hover:text-slate-900' }}
                >
                  <span className="text-xl mt-0.5">{item.icon}</span>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <span className="font-medium text-sm leading-snug">{item.label}</span>
                      {item.badge && (
                        <Badge variant="outline" className="text-[9px] px-1.5 py-0 font-mono text-blue-600 bg-blue-50 border-blue-200">
                          {item.badge}
                        </Badge>
                      )}
                    </div>
                    <p className="text-xs text-slate-500 font-normal leading-tight mt-0.5 truncate">{item.description}</p>
                  </div>
                </Link>
              ))}
            </nav>

            {/* Sidebar Footer */}
            <div className="p-4 border-t border-slate-200 bg-slate-50 space-y-2">
              <div className="text-xs text-slate-600 flex items-center justify-between">
                <span>Current Role:</span>
                <span className="font-semibold text-slate-800">{ROLES.find((r) => r.id === role)?.label}</span>
              </div>
              <div className="text-[10px] text-slate-400 leading-tight">
                SingHealth Bed Capacity System • Prototype Build v1.0
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
