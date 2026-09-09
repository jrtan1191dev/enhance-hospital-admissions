import { Link, useNavigate, useLocation } from '@tanstack/react-router';
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
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const { data: auth } = useQuery(authQueries.me());

  const [role, setRole] = useState<RolePersona>(getActiveRole());
  const [prevPath, setPrevPath] = useState(location.pathname);

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
  };

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur shadow-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Left: Logo & Brand */}
          <Link to="/" className="flex items-center gap-3 hover:opacity-90 transition-opacity">
            <div className="h-10 w-10 rounded-lg bg-blue-600 flex items-center justify-center text-white shadow-sm font-bold text-lg">
              🏥
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-slate-900 text-lg tracking-tight">Patient Admission & Discharge Management Application</span>
                <Badge variant="secondary" className="text-[10px] font-mono px-1.5 py-0 bg-blue-50 text-blue-700 border-blue-200">
                  v1.0-PROTOTYPE
                </Badge>
              </div>
              <p className="text-xs text-slate-500 hidden sm:block">Emergency & Bed Capacity Orchestration System</p>
            </div>
          </Link>

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
  );
}
