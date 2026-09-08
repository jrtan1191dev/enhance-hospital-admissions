import { Link } from '@tanstack/react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getActiveRole, setActiveRole } from '../../services/api';
import { authQueries } from '../../services/queries';
import type { RolePersona } from '../../types/admissions';
import { Badge } from '../ui/badge';
import { Activity, UserCheck, ShieldCheck } from 'lucide-react';
import { useState } from 'react';

const ROLES: { id: RolePersona; label: string; icon: string; path: string; user: string }[] = [
  { id: 'ED_ATTENDING', label: 'ED Attending', icon: '🩺', path: '/ed', user: 'dr_tan_ed' },
  { id: 'SPECIALIST', label: 'Inpatient Specialist', icon: '👨‍⚕️', path: '/specialist', user: 'dr_lim_cardio' },
  { id: 'BMU_COORDINATOR', label: 'BMU Coordinator', icon: '🏢', path: '/bmu', user: 'bmu_coord_wong' },
  { id: 'WARD_NURSE', label: 'Ward Nurse', icon: '👩‍⚕️', path: '/ward', user: 'nurse_sarah' },
  { id: 'HOUSEKEEPING', label: 'EVS Housekeeping', icon: '🧹', path: '/ward', user: 'evs_staff_kumar' },
  { id: 'PATIENT', label: 'Patient Tracker', icon: '📱', path: '/patient', user: 'patient_p101' },
];

export function Header() {
  const [role, setRole] = useState<RolePersona>(getActiveRole());
  const queryClient = useQueryClient();
  const { data: auth } = useQuery(authQueries.me());

  const handleRoleChange = (newRole: RolePersona) => {
    setActiveRole(newRole);
    setRole(newRole);
    queryClient.invalidateQueries();
  };

  return (
    <header className="sticky top-0 z-50 border-b border-slate-200 bg-white/95 backdrop-blur shadow-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Brand */}
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
              <p className="text-xs text-slate-500">Emergency & Bed Capacity Orchestration System</p>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center space-x-1">
            <Link
              to="/ed"
              className="px-3 py-1.5 rounded-md text-sm font-medium transition-colors"
              activeProps={{ className: 'bg-blue-50 text-blue-700 font-semibold' }}
              inactiveProps={{ className: 'text-slate-600 hover:text-slate-900 hover:bg-slate-100' }}
            >
              🩺 ED Intake
            </Link>
            <Link
              to="/specialist"
              className="px-3 py-1.5 rounded-md text-sm font-medium transition-colors"
              activeProps={{ className: 'bg-blue-50 text-blue-700 font-semibold' }}
              inactiveProps={{ className: 'text-slate-600 hover:text-slate-900 hover:bg-slate-100' }}
            >
              👨‍⚕️ Specialist Pool
            </Link>
            <Link
              to="/bmu"
              className="px-3 py-1.5 rounded-md text-sm font-medium transition-colors"
              activeProps={{ className: 'bg-blue-50 text-blue-700 font-semibold' }}
              inactiveProps={{ className: 'text-slate-600 hover:text-slate-900 hover:bg-slate-100' }}
            >
              🏢 BMU Capacity
            </Link>
            <Link
              to="/bmu/config"
              className="px-3 py-1.5 rounded-md text-sm font-medium transition-colors"
              activeProps={{ className: 'bg-blue-50 text-blue-700 font-semibold' }}
              inactiveProps={{ className: 'text-slate-600 hover:text-slate-900 hover:bg-slate-100' }}
            >
              ⚙️ Solver Config
            </Link>
            <Link
              to="/ward"
              className="px-3 py-1.5 rounded-md text-sm font-medium transition-colors"
              activeProps={{ className: 'bg-blue-50 text-blue-700 font-semibold' }}
              inactiveProps={{ className: 'text-slate-600 hover:text-slate-900 hover:bg-slate-100' }}
            >
              🛏️ Ward & Turnover
            </Link>
            <Link
              to="/patient"
              className="px-3 py-1.5 rounded-md text-sm font-medium transition-colors"
              activeProps={{ className: 'bg-emerald-50 text-emerald-700 font-semibold' }}
              inactiveProps={{ className: 'text-emerald-700 hover:bg-emerald-50' }}
            >
              📱 Patient Tracker
            </Link>
          </nav>

          {/* Role Persona Switcher */}
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
  );
}
