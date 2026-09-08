import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { bmuQueries, useCheckinPatient, useVacatePatient, useCleanBed } from '../services/queries';
import type { Bed } from '../types/admissions';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import {
  BedDouble,
  CheckCircle2,
  Sparkles,
  LogOut,
  UserCheck,
  Clock,
  Brush,
  Layers,
} from 'lucide-react';

export function WardRoute() {
  const [activeWardCode, setActiveWardCode] = useState<string>('8A');
  const [notification, setNotification] = useState<string | null>(null);

  // TanStack Query via queries.ts queryOptions
  const { data: wards = [], isLoading } = useQuery(bmuQueries.inventory());

  // Nurse Check-in Mutation Hook
  const checkinMutation = useCheckinPatient(() => {
    setNotification('Patient successfully checked in! Bed status transitioned to OCCUPIED_TAKEN (Grey).');
    setTimeout(() => setNotification(null), 4500);
  });

  // Nurse Vacate Mutation Hook
  const vacateMutation = useVacatePatient(() => {
    setNotification('Bed vacated! Status changed to MUSTARD YELLOW (Turnover Cleaning 30m SLA active).');
    setTimeout(() => setNotification(null), 4500);
  });

  // Housekeeping Clean Mutation Hook
  const cleanMutation = useCleanBed(() => {
    setNotification('Terminal sanitization complete! Bed status transitioned to WHITE (Cleaned & Available).');
    setTimeout(() => setNotification(null), 4500);
  });

  const activeWard = wards.find((w) => w.wardCode === activeWardCode) || wards[0];

  // Collect all beds currently needing turnover across the entire hospital
  const turnoverQueue: Bed[] = wards.flatMap((w) =>
    (w.beds || [])
      .filter((b) => b.status === 'EMPTY_PENDING_CLEANING')
      .map((b) => ({ ...b, ward: w }))
  );

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <BedDouble className="h-6 w-6 text-blue-600" />
            Inpatient Ward Reception & Rapid Bed Turnover
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Ward nursing check-in/vacate triggers and Environmental Services (EVS) 30-minute terminal sanitization sign-offs.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="outline" className="px-3 py-1 text-xs bg-slate-50 text-slate-700">
            Nursing & Housekeeping Loop
          </Badge>
        </div>
      </div>

      {notification && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-800 text-sm flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 text-emerald-600 flex-shrink-0" />
          {notification}
        </div>
      )}

      {/* Housekeeping / EVS Active Turnover Queue Banner */}
      <Card className="border-amber-300 bg-amber-50/40 shadow-xs">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Brush className="h-5 w-5 text-amber-600" />
              <div>
                <CardTitle className="text-base text-amber-950">
                  Housekeeping (EVS) 30-Minute Turnover Queue ({turnoverQueue.length} Beds Pending)
                </CardTitle>
                <CardDescription className="text-amber-800 text-xs">
                  Beds in MUSTARD YELLOW require terminal sanitization before BMU can re-allocate.
                </CardDescription>
              </div>
            </div>
            <Badge variant="outline" className="bg-amber-100 text-amber-900 border-amber-300 text-xs font-mono">
              30m SLA Active
            </Badge>
          </div>
        </CardHeader>
        <CardContent>
          {turnoverQueue.length === 0 ? (
            <div className="text-xs text-amber-800/80 py-2 italic">
              No beds currently pending cleaning. All vacated beds have completed terminal sanitization.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3 pt-1">
              {turnoverQueue.map((bed) => (
                <div
                  key={bed.id}
                  className="p-3 bg-white rounded-lg border border-amber-300 shadow-xs flex items-center justify-between text-xs"
                >
                  <div>
                    <div className="font-bold text-slate-900 text-sm">Bed {bed.bedNumber}</div>
                    <div className="text-slate-500 font-mono text-[11px]">Ward {bed.ward?.wardCode} • Level {bed.ward?.levelNumber}</div>
                    <div className="flex items-center gap-1 text-[10px] text-amber-700 font-medium mt-1">
                      <Clock className="h-3 w-3" /> SLA Countdown: ~18m left
                    </div>
                  </div>

                  <Button
                    size="sm"
                    className="bg-amber-600 hover:bg-amber-700 text-white text-xs h-8"
                    disabled={cleanMutation.isPending}
                    onClick={() => cleanMutation.mutate(bed.id)}
                  >
                    <Sparkles className="h-3.5 w-3.5 mr-1" />
                    Sign-Off Clean
                  </Button>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Ward Nursing View */}
      <Card>
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <CardTitle className="text-lg flex items-center gap-2">
                <Layers className="h-5 w-5 text-blue-600" />
                Ward Nursing Station Bed Control
              </CardTitle>
              <CardDescription>
                Select a ward to view allocated, occupied, and pending beds. Check in incoming patients or vacate discharged patients.
              </CardDescription>
            </div>

            {/* Ward Selector Buttons */}
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-slate-500 mr-1">Select Ward:</span>
              {wards.map((w) => (
                <Button
                  key={w.wardCode}
                  size="sm"
                  variant={activeWard?.wardCode === w.wardCode ? 'default' : 'outline'}
                  onClick={() => setActiveWardCode(w.wardCode)}
                  className="text-xs font-semibold"
                >
                  Ward {w.wardCode} ({w.specialty})
                </Button>
              ))}
            </div>
          </div>
        </CardHeader>

        <CardContent>
          {isLoading ? (
            <div className="p-8 text-center text-slate-500 text-sm">Loading ward roster...</div>
          ) : !activeWard ? (
            <div className="p-8 text-center text-slate-500 text-sm">No ward selected.</div>
          ) : (
            <div className="space-y-4">
              {/* Active Ward Meta Banner */}
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 flex flex-wrap items-center justify-between gap-2 text-xs">
                <div className="flex items-center gap-3">
                  <span className="font-bold text-slate-900 text-base">Ward {activeWard.wardCode}</span>
                  <Badge variant="outline">Level {activeWard.levelNumber}</Badge>
                  <Badge variant="secondary">{activeWard.wardClass}</Badge>
                  <Badge variant="outline" className="bg-blue-50 text-blue-700">{activeWard.specialty}</Badge>
                </div>
                <div className="text-slate-600">
                  Cohort Lock: <strong>{activeWard.genderCohortLocked || 'Unlocked (Flex)'}</strong>
                </div>
              </div>

              {/* Bed Cards Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 pt-2">
                {activeWard.beds?.map((bed) => {
                  const isAssigned = bed.status === 'EMPTY_ASSIGNED'; // Green
                  const isOccupied = bed.status === 'OCCUPIED_TAKEN';   // Grey
                  const isPendingClean = bed.status === 'EMPTY_PENDING_CLEANING'; // Mustard Yellow
                  const isClean = bed.status === 'EMPTY_CLEANED';      // White

                  let borderClass = 'border-slate-200 bg-white';
                  let statusBadge = <Badge variant="outline" className="text-[10px]">Clean & Ready (White)</Badge>;

                  if (isAssigned) {
                    borderClass = 'border-emerald-300 bg-emerald-50/50 ring-1 ring-emerald-300';
                    statusBadge = <Badge variant="success" className="text-[10px]">Patient in Transit (Green)</Badge>;
                  } else if (isOccupied) {
                    borderClass = 'border-slate-400 bg-slate-100';
                    statusBadge = <Badge variant="secondary" className="text-[10px] bg-slate-200 text-slate-800">Occupied (Grey)</Badge>;
                  } else if (isPendingClean) {
                    borderClass = 'border-amber-300 bg-amber-50/60';
                    statusBadge = <Badge variant="warning" className="text-[10px]">Pending Clean (Mustard Yellow)</Badge>;
                  }

                  return (
                    <div
                      key={bed.id}
                      className={`p-4 rounded-xl border flex flex-col justify-between transition-all ${borderClass}`}
                    >
                      <div className="space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-base text-slate-900">Bed {bed.bedNumber}</span>
                          {statusBadge}
                        </div>

                        {/* Bed attributes */}
                        <div className="flex flex-wrap gap-1 text-[10px]">
                          {bed.telemetryCapable && (
                            <span className="px-1.5 py-0.5 rounded bg-blue-100 text-blue-800 font-medium">
                              Telemetry
                            </span>
                          )}
                          {bed.nearNursingStation && (
                            <span className="px-1.5 py-0.5 rounded bg-purple-100 text-purple-800 font-medium">
                              Near Station
                            </span>
                          )}
                        </div>

                        {/* Patient info if assigned or occupied */}
                        {bed.assignedPatient ? (
                          <div className="p-2.5 bg-white/80 rounded-lg border border-slate-200 text-xs space-y-1 mt-2">
                            <div className="font-bold text-slate-900 flex justify-between">
                              <span>{bed.assignedPatient.name}</span>
                              <span className="font-mono text-[10px] text-slate-500">
                                {bed.assignedPatient.queueToken}
                              </span>
                            </div>
                            <div className="text-[11px] text-slate-600">
                              {bed.assignedPatient.gender}, {bed.assignedPatient.age}y • Class {bed.assignedPatient.wardClassPreference}
                            </div>
                            <div className="text-[11px] text-slate-700 truncate">
                              <strong>Dx:</strong> {bed.assignedPatient.suspectedDiagnosis || 'Under medical management'}
                            </div>
                          </div>
                        ) : (
                          <div className="py-4 text-center text-xs text-slate-400 italic">
                            No patient currently assigned to this bed.
                          </div>
                        )}
                      </div>

                      {/* Nurse & Housekeeping Action Buttons */}
                      <div className="pt-4 mt-2 border-t border-slate-200/80 flex items-center justify-end gap-2">
                        {isAssigned && (
                          <Button
                            size="sm"
                            className="w-full bg-emerald-600 hover:bg-emerald-700 text-white text-xs"
                            disabled={checkinMutation.isPending}
                            onClick={() => checkinMutation.mutate(bed.id)}
                          >
                            <UserCheck className="h-3.5 w-3.5 mr-1" />
                            Check-In Patient Arrival
                          </Button>
                        )}

                        {isOccupied && (
                          <Button
                            size="sm"
                            variant="destructive"
                            className="w-full text-xs"
                            disabled={vacateMutation.isPending}
                            onClick={() => vacateMutation.mutate(bed.id)}
                          >
                            <LogOut className="h-3.5 w-3.5 mr-1" />
                            Vacate Bed on Discharge
                          </Button>
                        )}

                        {isPendingClean && (
                          <Button
                            size="sm"
                            className="w-full bg-amber-600 hover:bg-amber-700 text-white text-xs"
                            disabled={cleanMutation.isPending}
                            onClick={() => cleanMutation.mutate(bed.id)}
                          >
                            <Sparkles className="h-3.5 w-3.5 mr-1" />
                            Sign-Off Terminal Cleaning
                          </Button>
                        )}

                        {isClean && (
                          <span className="text-xs text-slate-400 py-1">Available for BMU allocation</span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
