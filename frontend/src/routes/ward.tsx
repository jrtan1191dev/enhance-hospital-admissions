import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  bmuQueries,
  wardQueries,
  useCheckinPatient,
  useVacatePatient,
  useCleanBed,
  useUpdateEdd,
  useDischargeSignoff,
  useDeliverMedication,
} from '../services/queries';
import { useActiveRole } from '../services/api';
import type { Bed, EddConfidence, DischargeRunwayDto, RolePersona } from '../types/admissions';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Input } from '../components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import {
  BedDouble,
  CheckCircle2,
  Sparkles,
  LogOut,
  UserCheck,
  Clock,
  Brush,
  Layers,
  Calendar,
  Pill,
  FileCheck,
  AlertTriangle,
  Edit,
} from 'lucide-react';

interface WardRouteProps {
  role?: RolePersona;
}

export function WardRoute({ role: roleProp }: WardRouteProps = {}) {
  const activeRole = useActiveRole();
  const role = roleProp ?? activeRole;
  const isHousekeeping = role === 'HOUSEKEEPING';

  const [activeWardCode, setActiveWardCode] = useState<string>('8A');
  const [notification, setNotification] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // EDD Modal State
  const [eddModalOpen, setEddModalOpen] = useState(false);
  const [selectedPatientForEdd, setSelectedPatientForEdd] = useState<{
    patientId: string;
    patientName: string;
    bedNumber: string;
    edd: string;
    confidence: EddConfidence;
    rationale: string;
  } | null>(null);

  // Queries via queries.ts queryOptions
  const { data: wards = [], isLoading } = useQuery(bmuQueries.inventory());
  const { data: runwayEntries = [] } = useQuery(wardQueries.runway());
  const { data: turnoverTasks = [] } = useQuery(wardQueries.turnoverTasks());

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
  const cleanMutation = useCleanBed(
    () => {
      setNotification('Terminal sanitization complete! Bed status transitioned to WHITE (Cleaned & Available).');
      setTimeout(() => setNotification(null), 4500);
    },
    (err) => {
      setErrorMessage(err.message || 'Failed to sign off clean bed.');
      setTimeout(() => setErrorMessage(null), 5000);
    }
  );

  // EDD Update Mutation Hook
  const updateEddMutation = useUpdateEdd(
    () => {
      setNotification('Estimated Date of Discharge (EDD) successfully updated!');
      setEddModalOpen(false);
      setTimeout(() => setNotification(null), 4500);
    },
    (err) => {
      setErrorMessage(err.message || 'Failed to update EDD.');
      setTimeout(() => setErrorMessage(null), 5000);
    }
  );

  // Morning Sign-Off Mutation Hook
  const signoffMutation = useDischargeSignoff(
    () => {
      setNotification('Morning discharge sign-off confirmed! Pharmacy bedside packing initiated.');
      setTimeout(() => setNotification(null), 4500);
    },
    (err) => {
      setErrorMessage(err.message || 'Failed to execute morning sign-off.');
      setTimeout(() => setErrorMessage(null), 5000);
    }
  );

  // Bedside Medication Delivery Mutation Hook
  const deliverMedMutation = useDeliverMedication(
    () => {
      setNotification('Bedside medication delivery confirmed! Patient is ready to vacate.');
      setTimeout(() => setNotification(null), 4500);
    },
    (err) => {
      setErrorMessage(err.message || 'Failed to confirm bedside delivery.');
      setTimeout(() => setErrorMessage(null), 5000);
    }
  );

  const activeWard = wards.find((w) => w.wardCode === activeWardCode) || wards[0];

  // Map runway entries by bedNumber for fast lookup
  const runwayByBed = new Map<string, DischargeRunwayDto>();
  runwayEntries.forEach((r) => {
    if (r.bedNumber) {
      runwayByBed.set(r.bedNumber, r);
    }
  });

  const handleOpenEddModal = (bed: Bed, runway?: DischargeRunwayDto) => {
    const patientId = bed.assignedPatient?.id || runway?.patientId;
    if (!patientId) return;

    setSelectedPatientForEdd({
      patientId,
      patientName: bed.assignedPatient?.name || runway?.patientName || 'Inpatient',
      bedNumber: bed.bedNumber,
      edd: runway?.estimatedDateOfDischarge || new Date(Date.now() + 86400000 * 2).toISOString().split('T')[0],
      confidence: runway?.confidence || 'HIGH',
      rationale: runway?.rationale || '',
    });
    setEddModalOpen(true);
  };

  const handleSaveEdd = () => {
    if (!selectedPatientForEdd) return;
    updateEddMutation.mutate({
      patientId: selectedPatientForEdd.patientId,
      data: {
        edd: selectedPatientForEdd.edd,
        eddConfidence: selectedPatientForEdd.confidence,
        rationale: selectedPatientForEdd.rationale,
      },
    });
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            {isHousekeeping ? (
              <>
                <Brush className="h-6 w-6 text-amber-600 shrink-0" />
                Housekeeping (EVS) Rapid Bed Turnover
              </>
            ) : (
              <>
                <BedDouble className="h-6 w-6 text-blue-600 shrink-0" />
                Inpatient Ward Reception & Bed Control
              </>
            )}
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            {isHousekeeping
              ? '30-minute terminal sanitization queue, vacated bed disinfection tracking, and clean bed sign-offs for BMU allocation.'
              : 'Ward nursing discharge runways, morning authorizations, bedside meds handoffs, and bed status management.'}
          </p>
        </div>
        <div className="flex items-center gap-2">
          {isHousekeeping ? (
            <Badge variant="outline" className="px-3 py-1 text-xs bg-amber-50 text-amber-900 border-amber-300 font-medium">
              🧹 EVS Housekeeping View
            </Badge>
          ) : (
            <Badge variant="outline" className="px-3 py-1 text-xs bg-blue-50 text-blue-800 border-blue-200 font-medium">
              👩‍⚕️ Ward Nursing View
            </Badge>
          )}
        </div>
      </div>

      {notification && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-800 text-sm flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 text-emerald-600 flex-shrink-0" />
          {notification}
        </div>
      )}

      {errorMessage && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-lg text-rose-800 text-sm flex items-center gap-2">
          <AlertTriangle className="h-4 w-4 text-rose-600 flex-shrink-0" />
          {errorMessage}
        </div>
      )}

      {/* Housekeeping / EVS Active 30-Minute Turnover Queue Banner - Viewable ONLY by EVS Housekeeping */}
      {isHousekeeping && (
        <Card className="border-amber-300 bg-amber-50/40 shadow-xs">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Brush className="h-5 w-5 text-amber-600" />
              <div>
                <CardTitle className="text-base text-amber-950">
                  Housekeeping (EVS) 30-Minute Turnover Queue ({turnoverTasks.length} Beds Pending)
                </CardTitle>
                <CardDescription className="text-amber-800 text-xs">
                  Beds in MUSTARD YELLOW require terminal sanitization within 30m before BMU can re-allocate.
                </CardDescription>
              </div>
            </div>
            <Badge variant="outline" className="bg-amber-100 text-amber-900 border-amber-300 text-xs font-mono">
              30m SLA Active
            </Badge>
          </div>
        </CardHeader>
        <CardContent>
          {turnoverTasks.length === 0 ? (
            <div className="text-xs text-amber-800/80 py-2 italic">
              No beds currently pending cleaning. All vacated beds have completed terminal sanitization.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3 pt-1">
              {turnoverTasks.map((task) => {
                const isBreached = task.slaStatus === 'BREACHED';
                const isApproaching = task.slaStatus === 'APPROACHING_SLA';

                let countdownBadge = (
                  <span className="flex items-center gap-1 text-[11px] text-emerald-700 font-semibold mt-1">
                    <Clock className="h-3 w-3" /> {task.remainingMinutes}m left (On Track)
                  </span>
                );
                let taskCardBorder = 'border-amber-300 bg-white';

                if (isBreached) {
                  taskCardBorder = 'border-rose-400 bg-rose-50/70 ring-1 ring-rose-300';
                  countdownBadge = (
                    <span className="flex items-center gap-1 text-[11px] text-rose-700 font-bold mt-1">
                      <AlertTriangle className="h-3.5 w-3.5 text-rose-600 animate-pulse" />
                      {Math.abs(task.remainingMinutes)}m OVERDUE (Breached SLA)
                    </span>
                  );
                } else if (isApproaching) {
                  taskCardBorder = 'border-amber-400 bg-amber-50/70 ring-1 ring-amber-300';
                  countdownBadge = (
                    <span className="flex items-center gap-1 text-[11px] text-amber-800 font-bold mt-1">
                      <Clock className="h-3 w-3 text-amber-600 animate-bounce" />
                      {task.remainingMinutes}m left (Urgent SLA Threshold)
                    </span>
                  );
                }

                return (
                  <div
                    key={task.bedId}
                    className={`p-3 rounded-lg border shadow-xs flex items-center justify-between text-xs ${taskCardBorder}`}
                  >
                    <div>
                      <div className="font-bold text-slate-900 text-sm">Bed {task.bedNumber}</div>
                      <div className="text-slate-500 font-mono text-[11px]">
                        Ward {task.wardCode} {task.levelNumber ? `• Level ${task.levelNumber}` : ''}
                      </div>
                      {countdownBadge}
                    </div>

                    <Button
                      size="sm"
                      className="bg-amber-600 hover:bg-amber-700 text-white text-xs h-8"
                      disabled={cleanMutation.isPending}
                      onClick={() => cleanMutation.mutate(task.bedId)}
                    >
                      <Sparkles className="h-3.5 w-3.5 mr-1" />
                      Sign-Off Clean
                    </Button>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>
      )}

      {/* Ward Nursing View - Viewable ONLY by Ward Nurse */}
      {!isHousekeeping && (
        <>
          <Card>
            <CardHeader>
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
            <div>
              <CardTitle className="text-lg flex items-center gap-2">
                <Layers className="h-5 w-5 text-blue-600" />
                Ward Nursing Station Bed Control
              </CardTitle>
              <CardDescription>
                Select a ward to view beds. Track discharge runways, execute morning sign-offs, verify bedside meds, and vacate departing patients.
              </CardDescription>
            </div>

            {/* Ward Selector Pills */}
            <div className="flex items-center gap-1.5 bg-slate-100 p-1.5 rounded-xl border border-slate-200 overflow-x-auto [scrollbar-width:none] flex-nowrap sm:flex-wrap">
              {wards.map((w) => {
                const total = w.beds?.length || 0;
                const occ =
                  w.beds?.filter((b) => b.status === 'OCCUPIED_TAKEN' || b.status === 'EMPTY_ASSIGNED').length || 0;
                const isSelected = activeWard?.wardCode === w.wardCode;
                return (
                  <button
                    key={w.wardCode}
                    type="button"
                    onClick={() => setActiveWardCode(w.wardCode)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-2 cursor-pointer shrink-0 ${
                      isSelected
                        ? 'bg-white text-blue-900 shadow-2xs font-semibold'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-200/60'
                    }`}
                  >
                    <span>
                      Ward {w.wardCode} ({w.specialty.replace('_', ' ')})
                    </span>
                    <span
                      className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-semibold ${
                        isSelected ? 'bg-blue-100 text-blue-800' : 'bg-slate-200/70 text-slate-600'
                      }`}
                    >
                      {occ}/{total}
                    </span>
                  </button>
                );
              })}
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
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 flex flex-wrap items-center justify-between gap-2 text-xs">
                <div className="flex items-center gap-3">
                  <span className="font-bold text-slate-900 text-base">Ward {activeWard.wardCode}</span>
                  <Badge variant="outline" className="font-mono">
                    Level {activeWard.levelNumber}
                  </Badge>
                  <Badge variant="secondary">{activeWard.wardClass}</Badge>
                  <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
                    {activeWard.specialty}
                  </Badge>
                </div>
                <div className="flex items-center gap-3 text-slate-600">
                  <span>
                    Cohort Lock: <strong>{activeWard.genderCohortLocked || 'Unlocked (Flex)'}</strong>
                  </span>
                  {activeWard.infectionLocked && activeWard.infectionLocked !== 'NONE' && (
                    <Badge variant="destructive" className="text-[10px]">
                      {activeWard.infectionLocked}
                    </Badge>
                  )}
                </div>
              </div>

              {/* Bed Cards Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 pt-2">
                {activeWard.beds?.map((bed) => {
                  const isAssigned = bed.status === 'EMPTY_ASSIGNED'; // Green
                  const isOccupied = bed.status === 'OCCUPIED_TAKEN'; // Grey
                  const isPendingClean = bed.status === 'EMPTY_PENDING_CLEANING'; // Mustard Yellow
                  const isClean = bed.status === 'EMPTY_CLEANED'; // White

                  const runway = runwayByBed.get(bed.bedNumber);

                  let borderClass = 'border-slate-200 bg-white shadow-2xs';
                  let statusBadge = (
                    <Badge variant="outline" className="text-[10px]">
                      Clean & Ready (White)
                    </Badge>
                  );

                  if (isAssigned) {
                    borderClass =
                      'border-emerald-300 bg-gradient-to-b from-emerald-50/70 to-white ring-1 ring-emerald-300 shadow-xs';
                    statusBadge = (
                      <Badge variant="success" className="text-[10px] flex items-center gap-1">
                        <span className="h-1.5 w-1.5 rounded-full bg-emerald-600 animate-pulse" />
                        In Transit (Green)
                      </Badge>
                    );
                  } else if (isOccupied) {
                    borderClass = 'border-slate-300 bg-slate-50/80 shadow-2xs';
                    statusBadge = (
                      <Badge variant="secondary" className="text-[10px] bg-slate-200 text-slate-800">
                        Occupied (Grey)
                      </Badge>
                    );
                  } else if (isPendingClean) {
                    borderClass = 'border-amber-300 bg-gradient-to-b from-amber-50/70 to-white shadow-xs';
                    statusBadge = <Badge variant="warning" className="text-[10px]">Pending Clean (Yellow)</Badge>;
                  }

                  // Runway stage badge
                  let runwayStageBadge = null;
                  if (runway?.runwayStage) {
                    switch (runway.runwayStage) {
                      case 'RUNWAY_D3':
                        runwayStageBadge = (
                          <Badge className="bg-indigo-100 text-indigo-800 border-indigo-300 text-[10px]">
                            D-3 Potential Discharge
                          </Badge>
                        );
                        break;
                      case 'RUNWAY_D2':
                        runwayStageBadge = (
                          <Badge className="bg-purple-100 text-purple-800 border-purple-300 text-[10px] font-semibold">
                            D-2 Potential Discharge
                          </Badge>
                        );
                        break;
                      case 'RUNWAY_D1':
                        runwayStageBadge = (
                          <Badge className="bg-amber-100 text-amber-900 border-amber-300 text-[10px] font-semibold">
                            D-1 Imminent Departure
                          </Badge>
                        );
                        break;
                      case 'READY_FOR_MORNING_SIGNOFF':
                        runwayStageBadge = (
                          <Badge className="bg-rose-100 text-rose-800 border-rose-300 text-[10px] font-bold animate-pulse">
                            Ready for Morning Sign-Off
                          </Badge>
                        );
                        break;
                      case 'MEDICATIONS_PENDING':
                        runwayStageBadge = (
                          <Badge className="bg-blue-100 text-blue-800 border-blue-300 text-[10px] font-semibold">
                            Meds Packing in Progress
                          </Badge>
                        );
                        break;
                      case 'READY_TO_VACATE':
                        runwayStageBadge = (
                          <Badge className="bg-emerald-100 text-emerald-800 border-emerald-300 text-[10px] font-bold">
                            Meds Delivered — Ready to Vacate
                          </Badge>
                        );
                        break;
                    }
                  }

                  return (
                    <div
                      key={bed.id}
                      className={`p-4 rounded-xl border flex flex-col justify-between transition-all hover:shadow-xs ${borderClass}`}
                    >
                      <div className="space-y-2.5">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-base text-slate-900">Bed {bed.bedNumber}</span>
                          <div className="flex items-center gap-1.5">
                            {runwayStageBadge}
                            {statusBadge}
                          </div>
                        </div>

                        {/* Bed attributes */}
                        <div className="flex flex-wrap gap-1 text-[10px]">
                          {bed.telemetryCapable && (
                            <span className="px-2 py-0.5 rounded-md bg-blue-100 text-blue-800 font-medium flex items-center gap-1">
                              ⚡ Telemetry Monitored
                            </span>
                          )}
                          {bed.nearNursingStation && (
                            <span className="px-2 py-0.5 rounded-md bg-purple-100 text-purple-800 font-medium">
                              🏥 Near Nurse Station
                            </span>
                          )}
                        </div>

                        {/* Patient info if assigned or occupied */}
                        {bed.assignedPatient ? (
                          <div className="p-3 bg-white rounded-xl border border-slate-200/90 text-xs space-y-2 shadow-2xs">
                            <div className="font-bold text-slate-900 flex justify-between items-center">
                              <span className="text-sm">{bed.assignedPatient.name}</span>
                              <span className="font-mono text-[10px] text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded">
                                {bed.assignedPatient.queueToken}
                              </span>
                            </div>
                            <div className="text-[11px] text-slate-500 font-mono">
                              {bed.assignedPatient.gender}, {bed.assignedPatient.age}y • Class{' '}
                              {bed.assignedPatient.wardClassPreference}
                            </div>
                            <div className="text-[11px] text-slate-700 truncate pt-0.5">
                              <strong>Dx:</strong> {bed.assignedPatient.suspectedDiagnosis || 'Under medical management'}
                            </div>

                            {/* Discharge Runway Section */}
                            {isOccupied && (
                              <div className="mt-2 pt-2 border-t border-slate-100 space-y-1.5 text-[11px]">
                                <div className="flex items-center justify-between text-slate-600">
                                  <span className="flex items-center gap-1 font-medium">
                                    <Calendar className="h-3 w-3 text-blue-600" />
                                    EDD: <strong>{runway?.estimatedDateOfDischarge || 'Not Set'}</strong>
                                  </span>
                                  {runway?.confidence && (
                                    <Badge
                                      variant="outline"
                                      className={`text-[9px] px-1.5 py-0 ${
                                        runway.confidence === 'HIGH'
                                          ? 'border-emerald-400 text-emerald-700 bg-emerald-50'
                                          : runway.confidence === 'MEDIUM'
                                          ? 'border-amber-400 text-amber-700 bg-amber-50'
                                          : 'border-slate-300 text-slate-600'
                                      }`}
                                    >
                                      {runway.confidence} Confidence
                                    </Badge>
                                  )}
                                </div>

                                {runway?.rationale && (
                                  <div className="text-[10px] text-slate-500 italic bg-slate-50 p-1.5 rounded border border-slate-100">
                                    "{runway.rationale}"
                                  </div>
                                )}

                                {/* Medication Delivery Badge */}
                                {runway?.medicationStatus && (
                                  <div className="flex items-center justify-between pt-1">
                                    <span className="flex items-center gap-1 text-[10px] text-slate-600">
                                      <Pill className="h-3 w-3 text-purple-600" /> Bedside Meds:
                                    </span>
                                    <Badge
                                      variant="outline"
                                      className={`text-[9px] font-mono ${
                                        runway.medicationStatus === 'DELIVERED_BEDSIDE'
                                          ? 'bg-emerald-50 text-emerald-800 border-emerald-300'
                                          : runway.medicationStatus === 'PACKING_IN_PROGRESS'
                                          ? 'bg-blue-50 text-blue-800 border-blue-300 animate-pulse'
                                          : 'bg-slate-50 text-slate-600'
                                      }`}
                                    >
                                      {runway.medicationStatus}
                                    </Badge>
                                  </div>
                                )}
                              </div>
                            )}
                          </div>
                        ) : (
                          <div className="py-6 text-center text-xs text-slate-400 italic bg-slate-50/50 rounded-lg border border-dashed border-slate-200">
                            Available for BMU placement
                          </div>
                        )}
                      </div>

                      {/* Action Buttons */}
                      <div className="pt-3 mt-2 border-t border-slate-200/80 space-y-2">
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
                          <div className="space-y-1.5">
                            {/* Set / Update EDD button */}
                            <Button
                              size="sm"
                              variant="outline"
                              className="w-full text-xs h-7 text-slate-700 border-slate-300 hover:bg-slate-100"
                              onClick={() => handleOpenEddModal(bed, runway)}
                            >
                              <Edit className="h-3 w-3 mr-1 text-slate-500" />
                              {runway?.estimatedDateOfDischarge ? 'Update EDD / Rationale' : 'Set Advance EDD (D-2 / D-3)'}
                            </Button>

                            {/* Morning Final Sign-off Button */}
                            {(!runway?.dischargeSignoffAt &&
                              (runway?.runwayStage === 'READY_FOR_MORNING_SIGNOFF' ||
                                runway?.medicationStatus === 'NOT_DISPATCHED')) && (
                              <Button
                                size="sm"
                                className="w-full bg-blue-600 hover:bg-blue-700 text-white text-xs h-7"
                                disabled={signoffMutation.isPending}
                                onClick={() => {
                                  const pid = bed.assignedPatient?.id || runway?.patientId;
                                  if (pid) signoffMutation.mutate(pid);
                                }}
                              >
                                <FileCheck className="h-3.5 w-3.5 mr-1" />
                                Final Discharge Sign-Off
                              </Button>
                            )}

                            {/* Bedside Medication Confirm Delivery Button */}
                            {runway?.medicationStatus === 'PACKING_IN_PROGRESS' && (
                              <Button
                                size="sm"
                                className="w-full bg-purple-600 hover:bg-purple-700 text-white text-xs h-7"
                                disabled={deliverMedMutation.isPending}
                                onClick={() => {
                                  const pid = bed.assignedPatient?.id || runway?.patientId;
                                  if (pid) deliverMedMutation.mutate(pid);
                                }}
                              >
                                <Pill className="h-3.5 w-3.5 mr-1" />
                                Confirm Bedside Delivery
                              </Button>
                            )}

                            {/* Vacate Bed button */}
                            <Button
                              size="sm"
                              variant="destructive"
                              className="w-full text-xs h-7"
                              disabled={vacateMutation.isPending}
                              onClick={() => vacateMutation.mutate(bed.id)}
                            >
                              <LogOut className="h-3.5 w-3.5 mr-1" />
                              Patient Vacated Bed
                            </Button>
                          </div>
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
                          <span className="text-xs text-slate-400 py-1 block text-center">
                            Available for BMU allocation
                          </span>
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

      {/* Set / Update EDD Modal Dialog */}
      <Dialog open={eddModalOpen} onOpenChange={setEddModalOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-base">
              <Calendar className="h-5 w-5 text-blue-600" />
              Establish Advance Discharge Runway (EDD)
            </DialogTitle>
            <DialogDescription className="text-xs">
              Record Estimated Date of Discharge and clinical confidence for{' '}
              <strong>{selectedPatientForEdd?.patientName}</strong> (Bed {selectedPatientForEdd?.bedNumber}) during
              morning rounds.
            </DialogDescription>
          </DialogHeader>

          {selectedPatientForEdd && (
            <div className="space-y-4 py-2 text-xs">
              <div className="space-y-1">
                <label className="font-semibold text-slate-700">Estimated Date of Discharge (EDD)</label>
                <Input
                  type="date"
                  value={selectedPatientForEdd.edd}
                  onChange={(e) =>
                    setSelectedPatientForEdd({ ...selectedPatientForEdd, edd: e.target.value })
                  }
                  className="h-8 text-xs"
                />
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-slate-700">Clinical Confidence Rating</label>
                <select
                  value={selectedPatientForEdd.confidence}
                  onChange={(e) =>
                    setSelectedPatientForEdd({
                      ...selectedPatientForEdd,
                      confidence: e.target.value as EddConfidence,
                    })
                  }
                  className="w-full h-8 px-2 border rounded-md text-xs bg-white border-slate-300"
                >
                  <option value="HIGH">HIGH (Definite discharge expected on target date)</option>
                  <option value="MEDIUM">MEDIUM (Conditional on pending labs / oral switch)</option>
                  <option value="LOW">LOW (Uncertain trajectory / guarded prognosis)</option>
                </select>
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-slate-700">Clinical Rationale & Milestones</label>
                <textarea
                  rows={3}
                  value={selectedPatientForEdd.rationale}
                  onChange={(e) =>
                    setSelectedPatientForEdd({ ...selectedPatientForEdd, rationale: e.target.value })
                  }
                  placeholder="e.g. Afebrile x48h, oral antibiotics tolerated, physiotherapy cleared"
                  className="w-full p-2 border rounded-md text-xs border-slate-300"
                />
              </div>
            </div>
          )}

          <DialogFooter className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => setEddModalOpen(false)}>
              Cancel
            </Button>
            <Button
              size="sm"
              className="bg-blue-600 hover:bg-blue-700 text-white"
              disabled={updateEddMutation.isPending}
              onClick={handleSaveEdd}
            >
              Save EDD & Runway Stage
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      </>
      )}
    </div>
  );
}
