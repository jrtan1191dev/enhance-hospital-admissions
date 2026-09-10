import { Link, useNavigate, useLocation } from '@tanstack/react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getActiveRole, setActiveRole } from '../../services/api';
import { authQueries } from '../../services/queries';
import type { RolePersona } from '../../types/admissions';
import { Badge } from '../ui/badge';
import { Activity, UserCheck, ShieldCheck, BarChart3, Menu, X } from 'lucide-react';
import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { cn } from '../../lib/utils';

const ROLES: { id: RolePersona; label: string; icon: string; path: string; user: string }[] = [
  { id: 'ED_ATTENDING', label: 'ED Attending', icon: '🩺', path: '/ed', user: 'dr_tan_ed' },
  { id: 'SPECIALIST', label: 'Inpatient Specialist', icon: '👨‍⚕️', path: '/specialist', user: 'dr_lim_cardio' },
  { id: 'BMU_COORDINATOR', label: 'BMU Coordinator', icon: '🏢', path: '/bmu', user: 'bmu_coord_wong' },
  { id: 'WARD_NURSE', label: 'Ward Nurse', icon: '👩‍⚕️', path: '/ward', user: 'nurse_sarah' },
  { id: 'HOUSEKEEPING', label: 'EVS Housekeeping', icon: '🧹', path: '/ward', user: 'evs_staff_kumar' },
  { id: 'PATIENT', label: 'Patient Tracker', icon: '📱', path: '/patient', user: 'patient_p101' },
];

export function Header() {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const { data: auth } = useQuery(authQueries.me());

  const [role, setRole] = useState<RolePersona>(getActiveRole());
  const [prevPath, setPrevPath] = useState(location.pathname);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // Close mobile drawer on Escape key and manage body scroll lock
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setIsMobileMenuOpen(false);
    };
    if (isMobileMenuOpen) {
      document.addEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isMobileMenuOpen]);

  // Sync active role with URL route changes during render without cascading effects
  if (prevPath !== location.pathname) {
    setPrevPath(location.pathname);
    const currentRoleObj = ROLES.find((r) => r.id === role);
    if (
      currentRoleObj?.path !== location.pathname &&
      !(location.pathname === '/bmu/config' && role === 'BMU_COORDINATOR')
    ) {
      const matchingRole = ROLES.find((r) => r.path === location.pathname);
      if (matchingRole) {
        setActiveRole(matchingRole.id);
        setRole(matchingRole.id);
        queryClient.invalidateQueries();
      }
    }
  }

  const handleRoleChange = (newRole: RolePersona) => {
    setActiveRole(newRole);
    setRole(newRole);
    queryClient.invalidateQueries();

    const target = ROLES.find((r) => r.id === newRole);
    if (target && target.path !== location.pathname) {
      navigate({ to: target.path });
    }
    setIsMobileMenuOpen(false);
  };

  const isAnalyticsActive = location.pathname === '/analytics';

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200/80 bg-white/95 backdrop-blur-md shadow-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 gap-3 sm:gap-4 lg:gap-6">
          {/* Left: Logo & Brand with robust min-width & truncation handling */}
          <div className="flex items-center min-w-0 flex-1 mr-2 sm:mr-4">
            <Link
              to="/"
              className="group flex items-center gap-2.5 sm:gap-3 min-w-0 hover:opacity-95 transition-opacity"
            >
              <div className="h-9 w-9 sm:h-10 sm:w-10 rounded-xl bg-blue-600 flex items-center justify-center text-white shadow-xs font-bold text-base sm:text-lg shrink-0 ring-1 ring-blue-700/10 group-hover:bg-blue-700 transition-colors">
                🏥
              </div>
              <div className="min-w-0 flex-1">
                <div className="min-w-0 line-clamp-2 sm:line-clamp-1">
                  <span
                    className="font-bold text-slate-900 text-xs sm:text-base lg:text-lg tracking-tight leading-tight"
                    title="Patient Admission & Discharge Management Application"
                  >
                    Patient Admission & Discharge Management Application
                  </span>{' '}
                  <Badge
                    variant="secondary"
                    className="inline-flex align-middle text-[9px] sm:text-[10px] font-mono px-1 sm:px-1.5 py-0 bg-blue-50 text-blue-700 border-blue-200 shrink-0"
                  >
                    v1.0-PROTOTYPE
                  </Badge>
                </div>
                <p className="text-xs text-slate-500 truncate hidden md:block mt-0.5">
                  Emergency & Bed Capacity Orchestration System
                </p>
              </div>
            </Link>
          </div>

          {/* Desktop Nav: Analytics Navigation Button */}
          <nav aria-label="Main navigation" className="hidden md:flex items-center shrink-0">
            <Link
              to="/analytics"
              aria-current={isAnalyticsActive ? 'page' : undefined}
              className={cn(
                'group inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all duration-150 select-none cursor-pointer shadow-2xs',
                'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-1',
                isAnalyticsActive
                  ? 'bg-blue-600 text-white border-blue-600 shadow-xs hover:bg-blue-700 active:bg-blue-800'
                  : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50 hover:text-slate-900 hover:border-slate-300 active:bg-slate-100'
              )}
            >
              <BarChart3
                className={cn(
                  'w-3.5 h-3.5 shrink-0 transition-transform duration-150 group-hover:scale-110',
                  isAnalyticsActive ? 'text-white' : 'text-blue-600'
                )}
              />
              <span>Analytics</span>
            </Link>
          </nav>

          {/* Desktop Right: Role Persona Switcher & Indicators */}
          <div className="hidden md:flex items-center gap-2 sm:gap-2.5 shrink-0">
            <div className="flex items-center gap-1.5 bg-slate-100/90 p-1 rounded-lg border border-slate-200/90 shadow-2xs">
              <span className="text-xs font-medium px-2 text-slate-500 hidden sm:inline-flex items-center gap-1.5">
                <UserCheck className="h-3.5 w-3.5 text-slate-400" />
                Persona:
              </span>
              <select
                value={role}
                onChange={(e) => handleRoleChange(e.target.value as RolePersona)}
                className="text-xs font-medium bg-white border border-slate-200 rounded-md px-2.5 py-1 text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 cursor-pointer shadow-2xs hover:border-slate-300 transition-colors"
                aria-label="Select role persona"
              >
                {ROLES.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.icon} {r.label} ({r.user})
                  </option>
                ))}
              </select>
            </div>

            {auth?.authenticated && (
              <div
                className="hidden xl:flex items-center gap-1.5 text-xs text-blue-700 bg-blue-50/80 border border-blue-200/90 px-2.5 py-1 rounded-lg font-mono shadow-2xs"
                title={`Spring Security Roles: ${auth.roles?.join(', ')}`}
              >
                <ShieldCheck className="h-3.5 w-3.5 text-blue-600 shrink-0" />
                <span>{auth.username}</span>
              </div>
            )}

            <div className="hidden lg:flex items-center gap-1.5 text-xs text-emerald-700 bg-emerald-50/80 border border-emerald-200/90 px-2.5 py-1 rounded-lg font-medium shadow-2xs">
              <Activity className="h-3.5 w-3.5 text-emerald-600 animate-pulse shrink-0" />
              <span>Live Sync</span>
            </div>
          </div>

          {/* Mobile Only: Hamburger Menu Button */}
          <div className="flex md:hidden items-center shrink-0">
            <button
              type="button"
              onClick={() => setIsMobileMenuOpen(true)}
              aria-label="Open navigation menu"
              aria-expanded={isMobileMenuOpen}
              className="inline-flex items-center justify-center p-2 rounded-lg text-slate-700 hover:text-slate-900 hover:bg-slate-100 border border-slate-200 shadow-2xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 cursor-pointer transition-colors"
            >
              <Menu className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Side Nav Drawer portaled to document.body to break out of header containing block */}
      {isMobileMenuOpen && typeof document !== 'undefined' && createPortal(
        <div className="md:hidden fixed inset-0 z-[100] flex justify-end" role="dialog" aria-modal="true" aria-label="Mobile Navigation">
          {/* Backdrop overlay */}
          <div
            className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity duration-200"
            onClick={() => setIsMobileMenuOpen(false)}
            aria-hidden="true"
          />

          {/* Drawer panel */}
          <div className="relative z-10 w-full max-w-xs bg-white h-full shadow-2xl flex flex-col justify-between p-5 border-l border-slate-200 animate-in slide-in-from-right duration-200 overflow-y-auto">
            <div className="space-y-6">
              {/* Drawer header */}
              <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                <div className="flex items-center gap-2.5">
                  <div className="h-8 w-8 rounded-lg bg-blue-600 flex items-center justify-center text-white shadow-xs font-bold text-sm">
                    🏥
                  </div>
                  <div>
                    <span className="font-bold text-slate-900 text-sm tracking-tight block">Patient Admission & Discharge Management Application</span>
                    <Badge variant="secondary" className="text-[9px] font-mono px-1 py-0 bg-blue-50 text-blue-700">
                      v1.0-PROTOTYPE
                    </Badge>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 cursor-pointer"
                  aria-label="Close menu"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Navigation Section */}
              <div className="space-y-2">
                <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block px-1">
                  Navigation
                </span>
                <Link
                  to="/analytics"
                  onClick={() => setIsMobileMenuOpen(false)}
                  className={cn(
                    'flex items-center justify-between p-2.5 rounded-xl text-sm font-medium border transition-all cursor-pointer shadow-2xs',
                    isAnalyticsActive
                      ? 'bg-blue-600 text-white border-blue-600 font-semibold shadow-xs'
                      : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                  )}
                >
                  <div className="flex items-center gap-2.5">
                    <BarChart3 className={cn('w-4 h-4', isAnalyticsActive ? 'text-white' : 'text-blue-600')} />
                    <span>Analytics Dashboard</span>
                  </div>
                  {isAnalyticsActive && <span className="text-xs">Active</span>}
                </Link>
              </div>

              {/* Persona Switcher Section */}
              <div className="space-y-2">
                <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block px-1">
                  Active Persona & Role
                </span>
                <div className="space-y-1.5">
                  {ROLES.map((r) => {
                    const isCurrent = r.id === role;
                    return (
                      <button
                        key={r.id}
                        type="button"
                        onClick={() => handleRoleChange(r.id)}
                        className={cn(
                          'w-full flex items-center justify-between p-2.5 rounded-xl text-left text-xs transition-colors cursor-pointer border',
                          isCurrent
                            ? 'bg-blue-50/80 border-blue-300 text-blue-900 font-semibold shadow-2xs'
                            : 'bg-slate-50/70 border-slate-200/80 text-slate-700 hover:bg-slate-100'
                        )}
                      >
                        <div className="flex items-center gap-2.5">
                          <span className="text-lg">{r.icon}</span>
                          <div>
                            <div className="font-semibold text-slate-900">{r.label}</div>
                            <div className="text-[10px] text-slate-500 font-mono">({r.user})</div>
                          </div>
                        </div>
                        {isCurrent && (
                          <Badge variant="outline" className="text-[9px] bg-blue-100 text-blue-800 border-blue-300">
                            Active
                          </Badge>
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Drawer Footer: Live Status & Auth info */}
            <div className="border-t border-slate-100 pt-4 space-y-2 mt-4">
              <div className="flex items-center justify-between text-xs text-emerald-700 bg-emerald-50 border border-emerald-200 px-3 py-2 rounded-lg font-medium">
                <div className="flex items-center gap-1.5">
                  <Activity className="h-3.5 w-3.5 text-emerald-600 animate-pulse" />
                  <span>Live Sync Active</span>
                </div>
                <span className="h-2 w-2 rounded-full bg-emerald-500" />
              </div>

              {auth?.authenticated && (
                <div className="flex items-center gap-1.5 text-xs text-blue-700 bg-blue-50 border border-blue-200 px-3 py-2 rounded-lg font-mono">
                  <ShieldCheck className="h-3.5 w-3.5 text-blue-600 shrink-0" />
                  <span>User: {auth.username}</span>
                </div>
              )}
            </div>
          </div>
        </div>,
        document.body
      )}
    </header>
  );
}
