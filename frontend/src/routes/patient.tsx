import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { patientQueries } from '../services/queries';
import { Badge } from '../components/ui/badge';
import {
  Smartphone,
  CheckCircle2,
  Clock,
  Users,
  AlertCircle,
  PhoneCall,
  BedDouble,
  ShieldCheck,
} from 'lucide-react';

const MILESTONES = [
  { step: 1, title: 'Admission Decision Confirmed', desc: 'Bed request dispatched to Bed Management Unit (BMU)' },
  { step: 2, title: 'Bed Assigned & Sanitization', desc: 'Matching ward identified; housekeeping sanitization in progress' },
  { step: 3, title: 'Porter Transfer in Progress', desc: 'Porter dispatched for transfer to inpatient bed' },
  { step: 4, title: 'Admitted to Ward Bed', desc: 'Patient received and safely checked in by ward nursing staff' },
];

export function PatientRoute() {
  const [selectedToken, setSelectedToken] = useState<string>('TOKEN-P101');

  // Fetch available patient tokens via patientQueries.availablePatients()
  const { data: availablePatients = [] } = useQuery(patientQueries.availablePatients());

  // Fetch Milestone status with live polling via patientQueries.track()
  const { data: tracker, isLoading, error } = useQuery(
    patientQueries.track(selectedToken)
  );

  const currentMilestone = tracker?.milestoneNumber || 1;

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

        {/* Quick-Picker Dropdown */}
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
      </div>

      {/* Mobile Simulator Mockup Frame */}
      <div className="flex justify-center py-4">
        <div className="w-full max-w-[380px] rounded-[3rem] border-[10px] border-slate-900 bg-slate-900 p-2.5 shadow-2xl ring-1 ring-slate-800">
          {/* Top Notch / Dynamic Island */}
          <div className="relative mx-auto mb-2 h-5 w-28 rounded-full bg-slate-950 flex items-center justify-between px-2.5 shadow-inner">
            <div className="h-2 w-2 rounded-full bg-blue-950/80 ring-1 ring-blue-500/20"></div>
            <div className="h-1.5 w-10 rounded-full bg-slate-800"></div>
          </div>

          {/* Screen Container */}
          <div className="rounded-[2.4rem] bg-slate-50 overflow-hidden min-h-[620px] flex flex-col justify-between p-4 text-slate-900 shadow-inner">
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

            {/* Header in App */}
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
                  {/* Greeting & Summary */}
                  <div className="bg-gradient-to-br from-emerald-600 to-teal-700 text-white p-4 rounded-2xl shadow-sm space-y-1">
                    <p className="text-[11px] text-emerald-100 font-medium">Admission Status Update</p>
                    <h3 className="font-bold text-base leading-tight">{tracker.patientName}</h3>
                    <p className="text-xs text-emerald-100/90">{tracker.milestoneLabel}</p>
                  </div>

                  {/* Operational Metrics Cards */}
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
                        <Users className="h-3 w-3 text-purple-600" /> Ahead in Queue
                      </div>
                      <div className="text-base font-bold text-slate-900 mt-1">
                        {tracker.paxAheadInQueue} <span className="text-[10px] font-normal text-slate-500">pax</span>
                      </div>
                    </div>
                  </div>

                  {/* Assigned Bed Badge if known */}
                  {tracker.assignedBedNumber && (
                    <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 p-3 rounded-xl text-xs flex items-center justify-between text-blue-950 shadow-2xs">
                      <div className="flex items-center gap-2.5">
                        <div className="p-2 rounded-lg bg-blue-600 text-white shadow-2xs">
                          <BedDouble className="h-4 w-4" />
                        </div>
                        <div>
                          <div className="font-bold text-sm">Bed {tracker.assignedBedNumber}</div>
                          <div className="text-[11px] text-blue-700">Ward {tracker.assignedWardCode}</div>
                        </div>
                      </div>
                      <Badge variant="outline" className="text-[10px] bg-white text-blue-700 font-medium border-blue-200">
                        Assigned
                      </Badge>
                    </div>
                  )}

                  {/* Delay Reason Notice if any */}
                  {tracker.delayReason && (
                    <div className="bg-amber-50 border border-amber-200 p-2.5 rounded-xl text-[11px] text-amber-900 flex items-start gap-2">
                      <AlertCircle className="h-4 w-4 text-amber-600 flex-shrink-0 mt-0.5" />
                      <span>{tracker.delayReason}</span>
                    </div>
                  )}

                  {/* Milestones Stepper with connecting line */}
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

                  {/* Financial & Care Explainer Module */}
                  {tracker.financialExplainer && (
                    <div className="bg-slate-100/90 p-3 rounded-xl text-xs space-y-1.5 border border-slate-200/60">
                      <div className="flex items-center justify-between text-[11px] font-bold text-slate-800">
                        <span className="flex items-center gap-1.5">
                          <ShieldCheck className="h-3.5 w-3.5 text-blue-600" />
                          Care & Subsidy Insights
                        </span>
                        <span className="text-[9px] text-slate-500 font-normal bg-white px-1.5 py-0.5 rounded border border-slate-200">FYI</span>
                      </div>
                      <p className="text-[10px] text-slate-600 leading-normal">
                        {tracker.financialExplainer.coPayEstimate}
                      </p>
                      <div className="pt-1 flex items-center justify-between text-[10px] text-blue-700 font-medium">
                        <span className="flex items-center gap-1">
                          <PhoneCall className="h-3 w-3" /> MSW Contact:
                        </span>
                        <span className="font-mono">{tracker.financialExplainer.mswContact}</span>
                      </div>
                    </div>
                  )}
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
