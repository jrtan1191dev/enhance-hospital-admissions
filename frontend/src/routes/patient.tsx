import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  patientQueries,
  useSimulatePeriodicUpdate,
  useRecordPatientAction,
} from '../services/queries';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { toast } from '../lib/toast';
import {
  Smartphone,
  CheckCircle2,
  Clock,
  Users,
  BedDouble,
  ShieldCheck,
  AlertCircle,
  Info,
  Phone,
  PhoneCall,
  BellRing,
  Calendar,
  Pill,
} from 'lucide-react';

const MILESTONES = [
  {
    step: 1,
    title: 'Admission Decision Confirmed & Bed Queued',
    desc: 'Bed request dispatched to Bed Management Unit (BMU)',
  },
  {
    step: 2,
    title: 'Bed Assigned & Preparing Room',
    desc: 'Matching ward identified; housekeeping sanitization underway',
  },
  {
    step: 3,
    title: 'Admitted to Inpatient Ward Bed',
    desc: 'Patient received and checked in by ward nursing staff',
  },
];

export function PatientRoute() {
  const [selectedToken, setSelectedToken] = useState<string>('TOKEN-P101');

  // Fetch available patient tokens for quick-picker
  const { data: availablePatients = [] } = useQuery(patientQueries.availablePatients());

  // Fetch Milestone status with 3-second live polling
  const { data: tracker, isLoading, error } = useQuery(
    patientQueries.track(selectedToken)
  );

  // Periodic update simulation mutation
  const simulateMutation = useSimulatePeriodicUpdate((data) => {
    toast.success(
      'Periodic Update Broadcast Simulated',
      data.message || `Refreshed ${data.dispatchedCount} waiting patient(s)`
    );
  });

  // Action recording mutation for hotlines (KPI 18)
  const recordActionMutation = useRecordPatientAction();

  // Milestone derivation
  const getMilestoneStep = (status?: string): number => {
    switch (status) {
      case 'ASSESSMENT_PENDING':
        return 0; // Quiescent pre-milestone state
      case 'BED_REQUESTED':
        return 1;
      case 'BED_ALLOCATED':
        return 2;
      case 'ADMITTED_INPATIENT':
      case 'DISCHARGED':
        return 3;
      default:
        return 0;
    }
  };

  const getMilestoneLabel = (status?: string): string => {
    switch (status) {
      case 'ASSESSMENT_PENDING':
        return 'ED Clinical Assessment in Progress';
      case 'BED_REQUESTED':
        return 'Admission Confirmed — Awaiting Bed Allocation';
      case 'BED_ALLOCATED':
        return 'Bed Allocated — Preparing Inpatient Room';
      case 'ADMITTED_INPATIENT':
        return 'Admitted to Inpatient Ward Bed';
      case 'DISCHARGED':
        return 'Patient Discharged';
      default:
        return 'Evaluating Patient Journey';
    }
  };

  const currentMilestone = getMilestoneStep(tracker?.admissionStatus);
  const milestoneLabel = getMilestoneLabel(tracker?.admissionStatus);

  const handleSimulatePeriodicUpdate = () => {
    simulateMutation.mutate();
  };

  const handleCallHotline = (
    actionType: 'MSW_CALL' | 'FINANCE_CALL',
    phoneNumber: string,
    serviceName: string
  ) => {
    recordActionMutation.mutate({ token: selectedToken, actionType });
    toast.info(`Calling ${serviceName}`, `Connecting to ${phoneNumber}...`);
    window.location.href = `tel:${phoneNumber.replace(/\s+/g, '')}`;
  };

  return (
    <div className="space-y-6">
      {/* Top Banner & Simulator Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Smartphone className="h-6 w-6 text-emerald-600" />
            Patient & Family Public Milestone Tracker
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Token-activated mobile tracking interface displaying real-time admission milestones, estimated wait duration, and care insights.
          </p>
        </div>

        {/* Quick-Picker & Simulation Actions */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 bg-slate-50 p-2 rounded-lg border border-slate-200">
            <span className="text-xs font-semibold text-slate-600">Simulate Patient:</span>
            <select
              value={selectedToken}
              onChange={(e) => setSelectedToken(e.target.value)}
              className="text-xs font-medium bg-white border border-slate-300 rounded px-2.5 py-1.5 focus:ring-1 focus:ring-emerald-500 shadow-xs cursor-pointer"
            >
              {availablePatients.map((p) => (
                <option key={p.id} value={p.queueToken}>
                  {p.name} ({p.queueToken})
                </option>
              ))}
            </select>
          </div>

          <Button
            size="sm"
            variant="outline"
            onClick={handleSimulatePeriodicUpdate}
            disabled={simulateMutation.isPending}
            className="text-xs font-semibold gap-1.5 bg-blue-50 border-blue-200 text-blue-700 hover:bg-blue-100 hover:text-blue-800"
          >
            <BellRing className="h-3.5 w-3.5 text-blue-600" />
            {simulateMutation.isPending ? 'Broadcasting...' : 'Simulate Periodic Update'}
          </Button>
        </div>
      </div>

      {/* Mobile Simulator Mockup Frame */}
      <div className="flex justify-center py-4">
        <div className="w-full max-w-full sm:max-w-[390px] rounded-2xl sm:rounded-[3rem] border-2 sm:border-[10px] border-slate-900 bg-slate-900 p-1.5 sm:p-2.5 shadow-2xl ring-1 ring-slate-800">
          {/* Top Notch / Dynamic Island */}
          <div className="relative mx-auto mb-2 h-5 w-28 rounded-full bg-slate-950 flex items-center justify-between px-2.5 shadow-inner">
            <div className="h-2 w-2 rounded-full bg-blue-950/80 ring-1 ring-blue-500/20"></div>
            <div className="h-1.5 w-10 rounded-full bg-slate-800"></div>
          </div>

          {/* Screen Container */}
          <div className="rounded-xl sm:rounded-[2.4rem] bg-slate-50 overflow-hidden min-h-[660px] flex flex-col justify-between p-3 sm:p-4 text-slate-900 shadow-inner">
            {/* iOS Style Top Status Bar */}
            <div className="flex items-center justify-between text-[11px] font-semibold text-slate-700 px-1 pt-0.5 pb-2">
              <span>9:41</span>
              <div className="flex items-center gap-1.5 text-slate-800">
                <span className="text-[10px] tracking-tighter font-bold">5G</span>
                <span className="h-2.5 w-5 rounded-xs border border-slate-700 p-0.5 flex items-center">
                  <span className="h-full w-3/4 bg-slate-800 rounded-2xs"></span>
                </span>
              </div>
            </div>

            {/* In-App Header & Content */}
            <div className="space-y-3">
              <div className="flex items-center justify-between border-b border-slate-200/80 pb-2">
                <div className="flex items-center gap-2">
                  <div className="h-6 w-6 rounded-md bg-emerald-600 text-white flex items-center justify-center text-xs font-bold shadow-2xs">
                    🏥
                  </div>
                  <div>
                    <span className="font-bold text-xs text-slate-900 tracking-tight block">HealthHub SingHealth</span>
                  </div>
                </div>
                <Badge variant="outline" className="text-[10px] font-mono bg-white text-emerald-800 border-emerald-300">
                  {selectedToken}
                </Badge>
              </div>

              {isLoading ? (
                <div className="py-20 text-center text-xs text-slate-500">Loading tracking status...</div>
              ) : error ? (
                <div className="py-20 text-center text-xs text-red-600">Failed to track token.</div>
              ) : tracker ? (
                <>
                  {/* Greeting & Summary Banner */}
                  <div className="bg-gradient-to-br from-emerald-600 to-teal-700 text-white p-4 rounded-2xl shadow-sm space-y-1">
                    <p className="text-[11px] text-emerald-100 font-medium">Admission Journey Tracker</p>
                    <h3 className="font-bold text-base leading-tight">{tracker.patientName}</h3>
                    <p className="text-xs text-emerald-100/90">{milestoneLabel}</p>
                  </div>

                  {/* Quiescent State Notice for ASSESSMENT_PENDING */}
                  {currentMilestone === 0 ? (
                    <div className="bg-amber-50 border border-amber-200 rounded-2xl p-3.5 text-xs text-amber-900 space-y-1.5 shadow-2xs">
                      <div className="flex items-center gap-1.5 font-bold text-amber-950">
                        <Info className="h-4 w-4 text-amber-600 flex-shrink-0" />
                        ED Clinical Assessment in Progress
                      </div>
                      <p className="text-[11px] text-amber-800/90 leading-relaxed">
                        Your emergency care team is currently reviewing clinical evaluations, lab results, and diagnostic scans before an inpatient admission decision is confirmed.
                      </p>
                    </div>
                  ) : (
                    <>
                      {/* Non-FIFO Clinical Urgency Banner (Ticket 02) */}
                      <div className="bg-blue-50/80 border border-blue-200/70 rounded-xl p-2.5 text-[11px] text-blue-900 flex items-start gap-2 shadow-2xs">
                        <AlertCircle className="h-4 w-4 text-blue-600 flex-shrink-0 mt-0.5" />
                        <div className="leading-tight">
                          <span className="font-semibold text-blue-950">Clinical Urgency Priority: </span>
                          Hospital admissions are prioritized by acute clinical urgency and infection prevention rather than first-come-first-served sequence.
                        </div>
                      </div>

                      {/* Operational Metrics Cards (Ticket 01 & 02) */}
                      <div className="grid grid-cols-2 gap-2 text-center">
                        <div className="bg-white p-3 rounded-xl border border-slate-200/90 shadow-2xs">
                          <div className="flex items-center justify-center gap-1 text-[10px] text-slate-500 font-medium">
                            <Clock className="h-3 w-3 text-blue-600" /> Est. Wait Time
                          </div>
                          <div className="text-base font-bold text-slate-900 mt-1">
                            {tracker.estimatedWaitMinutes} <span className="text-[10px] font-normal text-slate-500">mins</span>
                          </div>
                        </div>

                        <div className="bg-white p-3 rounded-xl border border-slate-200/90 shadow-2xs">
                          <div className="flex items-center justify-center gap-1 text-[10px] text-slate-500 font-medium">
                            <Users className="h-3 w-3 text-purple-600" /> Patients Ahead
                          </div>
                          <div className="text-base font-bold text-slate-900 mt-1">
                            {tracker.patientsAhead ?? (tracker.queuePosition > 0 ? tracker.queuePosition - 1 : 0)}{' '}
                            <span className="text-[10px] font-normal text-slate-500">pax</span>
                          </div>
                          <p className="text-[9px] text-slate-400 mt-0.5">
                            Class {tracker.requestedWardClass ?? 'B2'} Queue
                          </p>
                        </div>
                      </div>
                    </>
                  )}

                  {/* Empathetic Operational Delay Card (Ticket 03) */}
                  {tracker.delayReason && (
                    <div className="bg-orange-50 border border-orange-200 rounded-xl p-3 text-xs text-orange-950 space-y-2 shadow-2xs">
                      <div className="flex items-center gap-1.5 font-bold text-orange-900 text-[11px]">
                        <Clock className="h-3.5 w-3.5 text-orange-600" />
                        Operational Care Update
                      </div>
                      <p className="text-[11px] text-orange-800 leading-normal">
                        {tracker.delayReason}
                      </p>
                      {tracker.delayContactHotline && (
                        <div className="flex items-center justify-between pt-1 border-t border-orange-200/60 text-[10px]">
                          <span className="text-orange-700">Hotline: {tracker.delayContactHotline}</span>
                          <a
                            href={`tel:${tracker.delayContactHotline.replace(/\s+/g, '')}`}
                            className="font-semibold text-orange-900 underline hover:text-orange-700 flex items-center gap-1"
                          >
                            <Phone className="h-3 w-3" /> Call Liaison
                          </a>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Assigned Bed Badge if known (Level -> Ward -> Bed Spatial Hierarchy) */}
                  {tracker.assignedBedNumber && (
                    <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 p-3 rounded-xl text-xs flex items-center justify-between text-blue-950 shadow-2xs">
                      <div className="flex items-center gap-2.5">
                        <div className="p-2 rounded-lg bg-blue-600 text-white shadow-2xs">
                          <BedDouble className="h-4 w-4" />
                        </div>
                        <div>
                          <div className="font-bold text-sm">Bed {tracker.assignedBedNumber}</div>
                          <div className="text-[11px] text-blue-700">
                            {tracker.assignedLevel ? `Level ${tracker.assignedLevel} • ` : ''}
                            {tracker.assignedWardName}
                          </div>
                        </div>
                      </div>
                      <Badge variant="outline" className="text-[10px] bg-white text-blue-700 font-medium border-blue-200">
                        Assigned
                      </Badge>
                    </div>
                  )}

                  {/* Bedside Medication Hand-off Banner (Ticket 02) */}
                  {tracker.medicationDeliveryStatus === 'DELIVERED_BEDSIDE' && (
                    <div className="bg-emerald-50 border border-emerald-300 rounded-2xl p-3.5 text-xs text-emerald-950 space-y-1.5 shadow-xs">
                      <div className="flex items-center gap-2 font-bold text-emerald-900 text-sm">
                        <CheckCircle2 className="h-5 w-5 text-emerald-600 flex-shrink-0" />
                        Medications Received at Bedside — Ready to Vacate
                      </div>
                      <p className="text-[11px] text-emerald-800 leading-relaxed">
                        Pre-packed discharge medications have been delivered and explained at your bedside. You may proceed with departure.
                      </p>
                    </div>
                  )}

                  {tracker.medicationDeliveryStatus === 'PACKING_IN_PROGRESS' && (
                    <div className="bg-blue-50 border border-blue-200 rounded-2xl p-3 text-xs text-blue-950 flex items-center gap-2.5 shadow-2xs">
                      <Pill className="h-4 w-4 text-blue-600 animate-pulse flex-shrink-0" />
                      <div className="text-[11px] leading-tight">
                        <strong className="block text-blue-900 mb-0.5">Discharge Medications in Preparation</strong>
                        Inpatient pharmacy is pre-packing medications for delivery directly to your bedside.
                      </div>
                    </div>
                  )}

                  {/* Planned Estimated Date of Discharge (EDD) Card (Ticket 01) */}
                  {tracker.estimatedDateOfDischarge && (
                    <div className="bg-indigo-50/80 border border-indigo-200 rounded-2xl p-3.5 text-xs text-indigo-950 space-y-1.5 shadow-2xs">
                      <div className="flex items-center justify-between">
                        <span className="font-bold flex items-center gap-1.5 text-indigo-950">
                          <Calendar className="h-4 w-4 text-indigo-600" />
                          Planned Discharge Date (EDD)
                        </span>
                        {tracker.eddConfidence && (
                          <Badge variant="outline" className="text-[9px] bg-white text-indigo-800 border-indigo-300">
                            {tracker.eddConfidence} Confidence
                          </Badge>
                        )}
                      </div>
                      <div className="text-base font-bold text-indigo-900">
                        {tracker.estimatedDateOfDischarge}
                      </div>
                      <p className="text-[10px] text-indigo-700 leading-snug">
                        Your clinical care team has scheduled your expected discharge date.
                      </p>
                    </div>
                  )}

                  {/* 3-Stage Visual Milestone Stepper (Ticket 01) */}
                  <div className="bg-white p-3.5 rounded-2xl border border-slate-200/90 shadow-2xs space-y-3">
                    <h4 className="text-[11px] font-bold text-slate-800 uppercase tracking-wider">
                      Journey Milestones
                    </h4>
                    <div className="space-y-4 relative before:absolute before:left-[11px] before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200">
                      {MILESTONES.map((m) => {
                        const isPast = currentMilestone > m.step;
                        const isCurrent = currentMilestone === m.step;
                        return (
                          <div key={m.step} className="flex items-start gap-3 relative z-10">
                            <div
                              className={`h-6 w-6 rounded-full flex items-center justify-center text-[10px] font-bold flex-shrink-0 shadow-2xs ${
                                isPast
                                  ? 'bg-emerald-600 text-white ring-2 ring-white'
                                  : isCurrent
                                  ? 'bg-blue-600 text-white ring-3 ring-blue-200 animate-pulse'
                                  : 'bg-slate-100 text-slate-400 border border-slate-300 ring-2 ring-white'
                              }`}
                            >
                              {isPast ? <CheckCircle2 className="h-3.5 w-3.5" /> : m.step}
                            </div>
                            <div className="text-xs pt-0.5">
                              <div
                                className={`font-semibold ${
                                  isCurrent ? 'text-blue-700 font-bold' : isPast ? 'text-slate-900' : 'text-slate-400'
                                }`}
                              >
                                {m.title}
                              </div>
                              <p className="text-[10px] text-slate-500 leading-tight mt-0.5">{m.desc}</p>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  {/* Financial & Care Explainer (Ticket 05 & 06) */}
                  <div className="bg-slate-100/90 p-3.5 rounded-2xl text-xs space-y-2 border border-slate-200/70 shadow-2xs">
                    <div className="flex items-center justify-between text-[11px] font-bold text-slate-800">
                      <span className="flex items-center gap-1.5">
                        <ShieldCheck className="h-4 w-4 text-blue-600" />
                        Financial & Care Explainer
                      </span>
                      <span className="text-[9px] font-medium text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded border border-emerald-200">
                        FYI Insights
                      </span>
                    </div>

                    <p className="text-[10px] text-slate-500 italic leading-snug">
                      FYI Insights: Figures are informational peace-of-mind estimates and require no upfront deposits or digital signatures.
                    </p>

                    {tracker.coPayEstimate && (
                      <div className="bg-white p-2.5 rounded-lg border border-slate-200/80 text-[11px] text-slate-700 leading-normal font-medium">
                        {tracker.coPayEstimate}
                      </div>
                    )}

                    {tracker.careGuidance && (
                      <div className="bg-white p-2.5 rounded-lg border border-slate-200/80 text-[11px] text-slate-600 leading-normal">
                        {tracker.careGuidance}
                      </div>
                    )}

                    {tracker.diversionRecommended && tracker.diversionPathway && (
                      <div className="bg-amber-50 p-2.5 rounded-lg border border-amber-200/80 text-[11px] text-amber-900 leading-normal">
                        <div className="font-semibold flex items-center gap-1 mb-1">
                          <Info className="h-3.5 w-3.5 text-amber-600" />
                          <span>Alternative Step-Down Care Recommended</span>
                        </div>
                        <p>{tracker.diversionPathway}</p>
                      </div>
                    )}

                    {/* 1-Click Support Hotlines with Interaction Audit Logging (Ticket 06) */}
                    <div className="pt-1 grid grid-cols-2 gap-2">
                      <button
                        onClick={() => handleCallHotline('MSW_CALL', '+65 6321 4311', 'Medical Social Work')}
                        className="flex items-center justify-center gap-1.5 bg-white hover:bg-slate-50 border border-slate-300 text-slate-800 font-semibold py-1.5 px-2 rounded-lg text-[10px] shadow-2xs transition-colors cursor-pointer"
                      >
                        <PhoneCall className="h-3 w-3 text-emerald-600" />
                        <span>Call MSW</span>
                      </button>
                      <button
                        onClick={() => handleCallHotline('FINANCE_CALL', '+65 6321 4312', 'Financial Counseling')}
                        className="flex items-center justify-center gap-1.5 bg-white hover:bg-slate-50 border border-slate-300 text-slate-800 font-semibold py-1.5 px-2 rounded-lg text-[10px] shadow-2xs transition-colors cursor-pointer"
                      >
                        <PhoneCall className="h-3 w-3 text-blue-600" />
                        <span>Call Finance</span>
                      </button>
                    </div>
                  </div>
                </>
              ) : null}
            </div>

            {/* Bottom Nav Simulation */}
            <div className="pt-3 border-t border-slate-200 text-center text-[10px] text-slate-400">
              Singapore Ministry of Health • HealthHub Platform
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
