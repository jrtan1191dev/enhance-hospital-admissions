import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { specialistQueries, useClaimBroadcast, useSubmitConsult, useAmendConsult, useChainConsult } from '../services/queries';
import type { AcuityTier, AssessmentBroadcast, SpecialtyCluster, DiversionPathway } from '../types/admissions';
import { Card, CardContent, CardDescription, CardHeader } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { Users, CheckCircle2, AlertTriangle, MessageSquare, Lock, Sparkles, GitBranch, Clock } from 'lucide-react';

const CLUSTERS: SpecialtyCluster[] = ['CARDIOLOGY', 'GENERAL_MEDICINE', 'SURGERY', 'ORTHOPAEDICS'];

// Acuity-Driven SLA Thresholds: Tier 1-2 = 15m; Tier 3-5 = 30m
function getSlaMinutes(tier?: AcuityTier): number {
  if (tier === 'TIER_1_CRITICAL' || tier === 'TIER_2_ACUTE_URGENT') return 15;
  return 30;
}

function getSlaRemainingSeconds(broadcast: AssessmentBroadcast, nowMs: number): number {
  const reqTime = broadcast.createdAt || broadcast.admissionRequest?.requestedAt || broadcast.admissionRequest?.createdAt;
  if (!reqTime) return 0;
  const createdMs = new Date(reqTime).getTime();
  const slaMins = getSlaMinutes(broadcast.admissionRequest?.primaryAcuityTier);
  const deadlineMs = createdMs + slaMins * 60 * 1000;
  return Math.floor((deadlineMs - nowMs) / 1000);
}

function formatCountdown(totalSecs: number): string {
  if (totalSecs <= 0) return '00:00';
  const mins = Math.floor(totalSecs / 60);
  const secs = totalSecs % 60;
  return `${mins}m ${secs < 10 ? '0' : ''}${secs}s`;
}

export function SpecialistRoute() {
  const [selectedCluster, setSelectedCluster] = useState<SpecialtyCluster | undefined>(undefined);
  const [consultModalBroadcast, setConsultModalBroadcast] = useState<AssessmentBroadcast | null>(null);
  const [chainModalBroadcast, setChainModalBroadcast] = useState<AssessmentBroadcast | null>(null);
  const [chainCluster, setChainCluster] = useState<SpecialtyCluster>('GENERAL_MEDICINE');
  const [chainRationale, setChainRationale] = useState<string>('');

  // Consult Form
  const [consultImpression, setConsultImpression] = useState<string>('');
  const [recommendedTier, setRecommendedTier] = useState<AcuityTier>('TIER_2_ACUTE_URGENT');
  const [secondaryTelemetry, setSecondaryTelemetry] = useState<boolean>(true);
  const [diversionPathway, setDiversionPathway] = useState<DiversionPathway>('NONE');
  const [diversionEndorsed, setDiversionEndorsed] = useState<boolean>(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [now, setNow] = useState<number>(Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  // TanStack Query: Fetch Broadcasts via queries.ts queryOptions
  const { data: broadcasts = [], isLoading } = useQuery(
    specialistQueries.broadcasts(selectedCluster)
  );

  const escalatedBroadcasts = broadcasts.filter((b) => b.status === 'AUTO_ESCALATED');

  // Centralized Claim Mutation Hook with Conflict Handling
  const claimMutation = useClaimBroadcast(
    () => {
      setErrorMessage(null);
      setStatusMessage('Case successfully claimed and locked to specialist.');
      setTimeout(() => setStatusMessage(null), 4000);
    },
    (err: Error) => {
      setStatusMessage(null);
      setErrorMessage(`Claim Conflict: ${err.message || 'This broadcast was claimed concurrently by another specialist.'}`);
      setTimeout(() => setErrorMessage(null), 6000);
    }
  );

  // Centralized Consult Mutation Hook
  const consultMutation = useSubmitConsult(() => {
    setStatusMessage('Consult impression submitted and appended to BMU dossier.');
    setConsultModalBroadcast(null);
    setTimeout(() => setStatusMessage(null), 4000);
  });

  // Centralized Consult Amendment Mutation Hook
  const amendMutation = useAmendConsult(
    () => {
      setStatusMessage('Consult impression amended and updated in-place on BMU queue.');
      setConsultModalBroadcast(null);
      setTimeout(() => setStatusMessage(null), 4000);
    },
    (err: Error) => {
      setErrorMessage(`Consult amendment failed: ${err.message}`);
      setTimeout(() => setErrorMessage(null), 6000);
    }
  );

  // Centralized Chain Consult Mutation Hook
  const chainMutation = useChainConsult(
    () => {
      setStatusMessage('Secondary specialist consult chained and broadcast successfully.');
      setChainModalBroadcast(null);
      setChainRationale('');
      setTimeout(() => setStatusMessage(null), 4000);
    },
    (err: Error) => {
      setErrorMessage(`Chain consult failed: ${err.message}`);
      setTimeout(() => setErrorMessage(null), 6000);
    }
  );

  const handleOpenConsult = (broadcast: AssessmentBroadcast) => {
    setConsultModalBroadcast(broadcast);
    setRecommendedTier(
      broadcast.secondaryAcuityTier || broadcast.admissionRequest.secondaryAcuityTier || broadcast.admissionRequest.primaryAcuityTier
    );
    setSecondaryTelemetry(
      broadcast.secondaryTelemetry ?? broadcast.admissionRequest.patient.telemetryRequired ?? true
    );
    setDiversionPathway(
      broadcast.diversionPathway || broadcast.admissionRequest.diversionPathway || 'NONE'
    );
    setConsultImpression(
      broadcast.consultNotes ||
        'Agree with ED assessment. Patient requires continuous telemetry bed in cardiology ward.'
    );
    setDiversionEndorsed(
      (broadcast.diversionPathway && broadcast.diversionPathway !== 'NONE') ||
      broadcast.admissionRequest.diversionRecommended ||
      false
    );
  };

  const handleOpenChain = (broadcast: AssessmentBroadcast) => {
    setChainModalBroadcast(broadcast);
    const availableClusters = CLUSTERS.filter((c) => c !== broadcast.targetCluster);
    setChainCluster(availableClusters[0] || 'GENERAL_MEDICINE');
    setChainRationale('');
  };

  const handleChainSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chainModalBroadcast) return;
    chainMutation.mutate({
      id: chainModalBroadcast.id,
      data: {
        targetCluster: chainCluster,
        rationale: chainRationale,
      },
    });
  };

  const handleSubmitConsult = (e: React.FormEvent) => {
    e.preventDefault();
    if (!consultModalBroadcast) return;
    const payload = {
      secondaryAcuityTier: recommendedTier,
      secondaryTelemetry,
      consultNotes: consultImpression,
      diversionRecommended: diversionEndorsed || diversionPathway !== 'NONE',
      diversionPathway,
    };

    if (consultModalBroadcast.status === 'COMPLETED') {
      amendMutation.mutate({
        id: consultModalBroadcast.id,
        data: payload,
      });
    } else {
      consultMutation.mutate({
        id: consultModalBroadcast.id,
        data: payload,
      });
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Users className="h-6 w-6 text-purple-600" />
            Inpatient Specialist Consult Broadcast Pool
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Asynchronous multi-doctor consult broadcasts for service clusters. Claim cases and submit parallel specialist directives.
          </p>
        </div>
        <Badge variant="outline" className="self-start sm:self-center px-3 py-1 text-xs bg-purple-50 text-purple-700 border-purple-200">
          On-Call Specialist: Dr. Lim (dr_lim_cardio)
        </Badge>
      </div>

      {statusMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-800 text-sm flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 text-emerald-600 flex-shrink-0" />
          {statusMessage}
        </div>
      )}

      {errorMessage && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-lg text-rose-800 text-sm flex items-center gap-2">
          <AlertTriangle className="h-4 w-4 text-rose-600 flex-shrink-0" />
          {errorMessage}
        </div>
      )}

      {/* Critical SLA Auto-Escalation Banner */}
      {escalatedBroadcasts.length > 0 && (
        <div className="p-4 bg-red-50 border-l-4 border-l-red-600 border border-red-200 rounded-xl text-red-950 flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-3">
            <AlertTriangle className="h-5 w-5 text-red-600 shrink-0 animate-bounce" />
            <div>
              <div className="font-bold text-sm">
                CRITICAL SLA ESCALATION ({escalatedBroadcasts.length} Active {escalatedBroadcasts.length === 1 ? 'Case' : 'Cases'})
              </div>
              <div className="text-xs text-red-800 mt-0.5">
                One or more consult requests exceeded acuity-based SLA thresholds and have been auto-escalated to designated cluster leads for immediate review.
              </div>
            </div>
          </div>
          <Badge variant="destructive" className="font-mono text-xs uppercase animate-pulse">
            Immediate Review Required
          </Badge>
        </div>
      )}

      {/* Specialty Cluster Filter Tabs */}
      <div className="flex items-center gap-1.5 bg-slate-100/90 p-1.5 rounded-xl border border-slate-200 shadow-2xs overflow-x-auto [scrollbar-width:none] flex-nowrap sm:flex-wrap">
        <button
          type="button"
          onClick={() => setSelectedCluster(undefined)}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 cursor-pointer shrink-0 ${
            selectedCluster === undefined
              ? 'bg-white text-slate-900 shadow-2xs font-semibold'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-200/60'
          }`}
        >
          <span>🌐 All Clusters</span>
          <span className="text-[10px] bg-slate-200/80 px-1.5 py-0.2 rounded-full font-mono font-semibold">
            {broadcasts.length}
          </span>
        </button>
        {CLUSTERS.map((cluster) => {
          const count = broadcasts.filter((b) => b.targetCluster === cluster).length;
          const icons: Record<string, string> = {
            CARDIOLOGY: '❤️ Cardiology',
            GENERAL_MEDICINE: '🏥 Gen Medicine',
            SURGERY: '🔪 Surgery',
            ORTHOPAEDICS: '🦴 Orthopaedics',
          };
          return (
            <button
              key={cluster}
              type="button"
              onClick={() => setSelectedCluster(cluster)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 cursor-pointer shrink-0 ${
                selectedCluster === cluster
                  ? 'bg-white text-slate-900 shadow-2xs font-semibold'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-200/60'
              }`}
            >
              <span>{icons[cluster] || cluster}</span>
              {count > 0 && (
                <span className="text-[10px] bg-purple-100 text-purple-800 px-1.5 py-0.2 rounded-full font-mono font-semibold">
                  {count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Broadcast Cards Grid */}
      {isLoading ? (
        <div className="p-12 text-center text-slate-500 text-sm">Loading broadcasts feed...</div>
      ) : broadcasts.length === 0 ? (
        <Card className="p-12 text-center text-slate-500">
          <Sparkles className="h-10 w-10 mx-auto text-slate-300 mb-2" />
          <p className="font-medium text-slate-700">No active consult broadcasts in this feed.</p>
          <p className="text-xs text-slate-400 mt-1">
            When ED physicians lead assessments in ED Intake, cases will be broadcasted here immediately.
          </p>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {broadcasts.map((broadcast) => {
            const req = broadcast.admissionRequest;
            const patient = req.patient;
            const isClaimed = broadcast.status === 'CLAIMED' || broadcast.status === 'AUTO_ESCALATED' || broadcast.status === 'COMPLETED';
            const isConsulted = broadcast.status === 'COMPLETED';
            const isEscalated = broadcast.status === 'AUTO_ESCALATED';
            const isOpen = broadcast.status === 'OPEN';

            const remainingSecs = getSlaRemainingSeconds(broadcast, now);
            const isOverdue = isOpen && remainingSecs <= 0;

            const borderAccent = isConsulted
              ? 'border-l-4 border-l-emerald-500'
              : isEscalated
              ? 'border-l-4 border-l-red-500 bg-red-50/20'
              : isClaimed
              ? 'border-l-4 border-l-purple-500'
              : isOverdue
              ? 'border-l-4 border-l-red-500'
              : 'border-l-4 border-l-blue-500';

            return (
              <Card key={broadcast.id} className={`border-slate-200 shadow-xs hover:shadow-sm hover:border-slate-300 transition-all ${borderAccent}`}>
                <CardHeader className="pb-3 border-b border-slate-100 bg-slate-50/50">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-slate-900 text-base">{patient.name}</span>
                      <Badge variant="outline" className="text-[10px] font-mono bg-white">
                        {patient.queueToken}
                      </Badge>
                    </div>
                    <div className="flex items-center gap-1.5 flex-wrap">
                      {broadcast.parentBroadcastId && (
                        <Badge variant="outline" className="text-[10px] bg-purple-50 text-purple-700 border-purple-200 flex items-center gap-1 font-semibold">
                          <GitBranch className="h-3 w-3" /> Chained
                        </Badge>
                      )}
                      {isOpen && (
                        <Badge
                          variant={isOverdue ? 'destructive' : 'outline'}
                          className={`text-[10px] font-mono flex items-center gap-1 ${
                            isOverdue ? 'animate-pulse' : 'bg-amber-50 text-amber-800 border-amber-300'
                          }`}
                        >
                          <Clock className="h-3 w-3" />
                          {isOverdue ? 'SLA OVERDUE' : `SLA: ${formatCountdown(remainingSecs)}`}
                        </Badge>
                      )}
                      {req.reconciliationRequested && (
                        <Badge variant="destructive" className="text-[10px] bg-red-100 text-red-800 border-red-300 flex items-center gap-1 font-semibold animate-pulse">
                          <AlertTriangle className="h-3 w-3 text-red-600" />
                          Reconciliation Requested
                        </Badge>
                      )}
                      <Badge
                        variant={
                          broadcast.status === 'COMPLETED'
                            ? 'success'
                            : broadcast.status === 'AUTO_ESCALATED'
                            ? 'destructive'
                            : broadcast.status === 'CLAIMED'
                            ? 'purple'
                            : 'secondary'
                        }
                        className={`text-xs font-semibold ${isEscalated ? 'animate-pulse' : ''}`}
                      >
                        {broadcast.status}
                      </Badge>
                    </div>
                  </div>
                  <CardDescription className="text-xs flex items-center justify-between pt-1">
                    <span>Cluster: <strong>{broadcast.targetCluster}</strong></span>
                    <span>{patient.gender}, {patient.age}y • Class {patient.wardClassPreference}</span>
                  </CardDescription>
                  {broadcasts.some((b) => b.parentBroadcastId === broadcast.id) && (
                    <div className="flex items-center flex-wrap gap-1 text-[11px] text-purple-700 pt-1">
                      <GitBranch className="h-3 w-3" />
                      <span>Chained to:</span>
                      {broadcasts.filter((b) => b.parentBroadcastId === broadcast.id).map((b) => (
                        <Badge key={b.id} variant="outline" className="text-[9px] py-0 px-1.5 bg-purple-50 text-purple-800 border-purple-200">
                          {b.targetCluster} ({b.status})
                        </Badge>
                      ))}
                    </div>
                  )}
                </CardHeader>

                <CardContent className="pt-4 space-y-3">
                  {/* Clinical Baseline */}
                  <div className="p-3 bg-slate-50 rounded-xl text-xs space-y-1.5 border border-slate-100">
                    <div className="text-slate-700">
                      <strong>Suspected:</strong> {patient.suspectedDiagnosis || 'Cardiac/Medical evaluation'}
                    </div>
                    <div className="grid grid-cols-3 gap-2 text-slate-600 font-mono text-[11px] pt-1">
                      <div className="bg-white p-1.5 rounded border border-slate-200/70 text-center">BP: {patient.vitalsBp || '--'}</div>
                      <div className="bg-white p-1.5 rounded border border-slate-200/70 text-center">SpO2: {patient.vitalsSpo2 || '--'}%</div>
                      <div className="bg-white p-1.5 rounded border border-slate-200/70 text-center text-red-600 font-semibold">Trop: {patient.labTroponin || 'Normal'}</div>
                    </div>
                  </div>

                  {/* ED Attending Lead Details */}
                  <div className="text-xs text-slate-600 space-y-1">
                    <div className="flex items-center gap-1.5">
                      <span className="font-medium text-slate-500">ED Attending Priority:</span>
                      <Badge variant="outline" className="text-[10px] font-semibold text-blue-700 bg-blue-50 border-blue-200">
                        {req.primaryAcuityTier}
                      </Badge>
                    </div>
                    <div className="flex flex-wrap gap-1 pt-1">
                      {patient.telemetryRequired && (
                        <Badge variant="outline" className="text-[10px] bg-amber-50 text-amber-800 border-amber-200">
                          Telemetry Required
                        </Badge>
                      )}
                      {patient.infectionStatus !== 'NONE' && (
                        <Badge variant="destructive" className="text-[10px]">
                          {patient.infectionStatus}
                        </Badge>
                      )}
                    </div>
                  </div>

                  {/* Auto-Escalated Notice */}
                  {isEscalated && (
                    <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-950 space-y-1">
                      <div className="font-semibold flex items-center gap-1 text-red-700">
                        <AlertTriangle className="h-4 w-4 text-red-600 shrink-0" />
                        Auto-Escalated Consult (SLA Breached):
                      </div>
                      <p className="text-[11px] text-red-800">
                        Unclaimed beyond {getSlaMinutes(req.primaryAcuityTier)}m SLA. Automatically assigned to default on-call specialist lead:
                      </p>
                      <div className="pt-0.5">
                        <span className="font-mono font-bold text-xs bg-white px-2 py-0.5 rounded border border-red-200 text-red-700">
                          {broadcast.claimedBySpecialistId}
                        </span>
                      </div>
                    </div>
                  )}

                  {/* Specialist Impression (if already consulted) */}
                  {isConsulted && (
                    <div className="p-3 bg-purple-50/80 rounded-xl border border-purple-200/70 text-xs text-purple-950 space-y-1">
                      <div className="font-semibold flex items-center gap-1">
                        <MessageSquare className="h-3.5 w-3.5 text-purple-600" />
                        Consult Impression ({broadcast.claimedBySpecialistId || 'Specialist'}):
                      </div>
                      <p className="italic text-purple-900 text-[11px] leading-relaxed">{broadcast.consultNotes}</p>
                      <div className="flex items-center gap-2 pt-1 font-medium flex-wrap">
                        <span>Recommended: <strong>{req.secondaryAcuityTier}</strong></span>
                        {broadcast.secondaryTelemetry && (
                          <Badge variant="outline" className="text-[10px] bg-amber-50 text-amber-900 border-amber-200">
                            Telemetry Endorsed
                          </Badge>
                        )}
                        {(broadcast.diversionPathway && broadcast.diversionPathway !== 'NONE') && (
                          <Badge variant="success" className="text-[10px]">
                            {broadcast.diversionPathway === 'COMMUNITY_HOSPITAL' ? 'Community Hospital' : 'MIC@Home'}
                          </Badge>
                        )}
                      </div>
                    </div>
                  )}

                  {/* Actions */}
                  <div className="pt-2 flex items-center justify-end gap-2 border-t border-slate-100">
                    {!isClaimed && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => claimMutation.mutate(broadcast.id)}
                        disabled={claimMutation.isPending}
                        className="text-xs cursor-pointer"
                      >
                        <Lock className="h-3.5 w-3.5 mr-1" />
                        Claim Case
                      </Button>
                    )}

                    {(isClaimed || isConsulted) && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOpenChain(broadcast)}
                        disabled={chainMutation.isPending}
                        className="text-xs border-purple-200 text-purple-700 hover:bg-purple-50 cursor-pointer"
                      >
                        <GitBranch className="h-3.5 w-3.5 mr-1" />
                        Chain Consult
                      </Button>
                    )}

                    <Button
                      size="sm"
                      variant={isConsulted ? 'outline' : 'default'}
                      onClick={() => handleOpenConsult(broadcast)}
                      className={`text-xs cursor-pointer ${
                        isConsulted
                          ? 'border-purple-300 text-purple-700 hover:bg-purple-50'
                          : 'bg-purple-600 hover:bg-purple-700 text-white'
                      }`}
                    >
                      <MessageSquare className="h-3.5 w-3.5 mr-1" />
                      {isConsulted ? 'Amend Consult' : 'Submit Consult Impression'}
                    </Button>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Consult Note Modal Dialog (shadcn Dialog) */}
      <Dialog open={!!consultModalBroadcast} onOpenChange={(open) => !open && setConsultModalBroadcast(null)}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-slate-900">
              <MessageSquare className="h-5 w-5 text-purple-600" />
              Specialist Consult Impression
            </DialogTitle>
            <DialogDescription>
              {consultModalBroadcast && (
                <span>
                  Patient: <strong>{consultModalBroadcast.admissionRequest.patient.name}</strong> ({consultModalBroadcast.admissionRequest.patient.nric}) • Cluster: {consultModalBroadcast.targetCluster}
                </span>
              )}
            </DialogDescription>
          </DialogHeader>

          {consultModalBroadcast && (
            <form onSubmit={handleSubmitConsult} className="space-y-4 py-2">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Recommended Acuity Tier
                </label>
                <select
                  value={recommendedTier}
                  onChange={(e) => setRecommendedTier(e.target.value as AcuityTier)}
                  className="w-full text-xs bg-white border border-slate-300 rounded-md px-3 py-2 font-medium"
                >
                  <option value="TIER_1_CRITICAL">Tier 1: Critical / Resuscitation (ICU/HD)</option>
                  <option value="TIER_2_ACUTE_URGENT">Tier 2: Acute Urgent (Telemetry Inpatient)</option>
                  <option value="TIER_3_ACUTE_STABLE">Tier 3: Acute Stable (General Ward)</option>
                  <option value="TIER_4_SUBACUTE_DIVERSION">Tier 4: Subacute / Potential Diversion</option>
                  <option value="TIER_5_OBSERVATION">Tier 5: Extended Observation / CDU</option>
                </select>
                {recommendedTier !== consultModalBroadcast.admissionRequest.primaryAcuityTier && (
                  <p className="text-[11px] text-amber-700 mt-1 flex items-center gap-1 font-medium">
                    <AlertTriangle className="h-3 w-3" /> Note: This diverges from ED Attending's Tier ({consultModalBroadcast.admissionRequest.primaryAcuityTier}). BMU will be notified under Safety-First policy.
                  </p>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Consult Impression & Care Directives
                </label>
                <textarea
                  value={consultImpression}
                  onChange={(e) => setConsultImpression(e.target.value)}
                  rows={3}
                  className="w-full text-xs bg-white border border-slate-300 rounded-md p-2.5 focus:ring-1 focus:ring-purple-500"
                  placeholder="Enter specialist evaluation, recommended diagnostics, and inpatient care notes..."
                  required
                />
              </div>

              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                <label className="flex items-center gap-2 text-xs font-medium text-slate-800 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={secondaryTelemetry}
                    onChange={(e) => setSecondaryTelemetry(e.target.checked)}
                    className="rounded border-slate-300 text-purple-600 focus:ring-purple-500"
                  />
                  <span className="font-semibold">Require Continuous Telemetry Monitoring</span>
                </label>
                <p className="text-[11px] text-slate-500 ml-5 mt-0.5">
                  Cardiac rhythm monitoring directive specified by reviewing specialist.
                </p>
              </div>

              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 space-y-2">
                <label className="block text-xs font-semibold text-slate-800">
                  Diversion Pathway Endorsement
                </label>
                <select
                  value={diversionPathway}
                  onChange={(e) => {
                    const val = e.target.value as DiversionPathway;
                    setDiversionPathway(val);
                    setDiversionEndorsed(val !== 'NONE');
                    if (val !== 'NONE') {
                      setRecommendedTier('TIER_4_SUBACUTE_DIVERSION');
                    }
                  }}
                  className="w-full text-xs bg-white border border-slate-300 rounded-md px-3 py-2 font-medium"
                >
                  <option value="NONE">None (Acute Inpatient Admission Required)</option>
                  <option value="COMMUNITY_HOSPITAL">Community Hospital (Step-Down Rehabilitation - OCH/SKCH)</option>
                  <option value="HOSPITAL_AT_HOME_MIC">Hospital-at-Home (Mobile Inpatient Care - MIC@Home)</option>
                </select>
                <p className="text-[11px] text-slate-500">
                  Selecting Community Hospital or MIC@Home automatically sets recommended tier to Tier 4 Diversion for BMU transfer processing.
                </p>
              </div>

              <DialogFooter className="pt-2">
                <Button type="button" variant="outline" onClick={() => setConsultModalBroadcast(null)}>
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={consultMutation.isPending || amendMutation.isPending}
                  className="bg-purple-600 hover:bg-purple-700 text-white cursor-pointer"
                >
                  {consultMutation.isPending || amendMutation.isPending
                    ? 'Saving...'
                    : consultModalBroadcast.status === 'COMPLETED'
                    ? 'Save In-Place Amendment'
                    : 'Submit Consult'}
                </Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>

      {/* Chain Consult Modal Dialog */}
      <Dialog open={!!chainModalBroadcast} onOpenChange={(open) => !open && setChainModalBroadcast(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-slate-900">
              <GitBranch className="h-5 w-5 text-purple-600" />
              Chain Secondary Specialist Consult
            </DialogTitle>
            <DialogDescription>
              {chainModalBroadcast && (
                <span>
                  Spawn concurrent specialist review for <strong>{chainModalBroadcast.admissionRequest.patient.name}</strong> ({chainModalBroadcast.admissionRequest.patient.nric}).
                </span>
              )}
            </DialogDescription>
          </DialogHeader>

          {chainModalBroadcast && (
            <form onSubmit={handleChainSubmit} className="space-y-4 py-2">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Target Specialty Cluster
                </label>
                <select
                  value={chainCluster}
                  onChange={(e) => setChainCluster(e.target.value as SpecialtyCluster)}
                  className="w-full text-xs bg-white border border-slate-300 rounded-md px-3 py-2 font-medium"
                >
                  {CLUSTERS.filter((c) => c !== chainModalBroadcast.targetCluster).map((c) => (
                    <option key={c} value={c}>
                      {c.replace('_', ' ')}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Clinical Rationale for Secondary Consult
                </label>
                <textarea
                  value={chainRationale}
                  onChange={(e) => setChainRationale(e.target.value)}
                  rows={3}
                  className="w-full text-xs bg-white border border-slate-300 rounded-md p-2.5 focus:ring-1 focus:ring-purple-500"
                  placeholder="e.g. Concomitant fracture requiring orthopaedic fixation prior to telemetry transfer..."
                  required
                />
              </div>

              <DialogFooter className="pt-2">
                <Button type="button" variant="outline" onClick={() => setChainModalBroadcast(null)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={chainMutation.isPending} className="bg-purple-600 hover:bg-purple-700 text-white">
                  {chainMutation.isPending ? 'Chaining...' : 'Spawn Chained Consult'}
                </Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
