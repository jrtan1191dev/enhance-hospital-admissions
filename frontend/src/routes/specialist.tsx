import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { specialistQueries, useClaimBroadcast, useSubmitConsult } from '../services/queries';
import type { AcuityTier, AssessmentBroadcast, SpecialtyCluster } from '../types/admissions';
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
import { Users, CheckCircle2, AlertTriangle, MessageSquare, Lock, Sparkles } from 'lucide-react';

const CLUSTERS: SpecialtyCluster[] = ['CARDIOLOGY', 'GENERAL_MEDICINE', 'SURGERY', 'ORTHOPAEDICS'];

export function SpecialistRoute() {
  const [selectedCluster, setSelectedCluster] = useState<SpecialtyCluster | undefined>(undefined);
  const [consultModalBroadcast, setConsultModalBroadcast] = useState<AssessmentBroadcast | null>(null);

  // Consult Form
  const [consultImpression, setConsultImpression] = useState<string>('');
  const [recommendedTier, setRecommendedTier] = useState<AcuityTier>('TIER_2_ACUTE_URGENT');
  const [diversionEndorsed, setDiversionEndorsed] = useState<boolean>(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  // TanStack Query: Fetch Broadcasts via queries.ts queryOptions
  const { data: broadcasts = [], isLoading } = useQuery(
    specialistQueries.broadcasts(selectedCluster)
  );

  // Centralized Claim Mutation Hook
  const claimMutation = useClaimBroadcast(() => {
    setStatusMessage('Case successfully claimed and locked to specialist.');
    setTimeout(() => setStatusMessage(null), 4000);
  });

  // Centralized Consult Mutation Hook
  const consultMutation = useSubmitConsult(() => {
    setStatusMessage('Consult impression submitted and appended to BMU dossier.');
    setConsultModalBroadcast(null);
    setTimeout(() => setStatusMessage(null), 4000);
  });

  const handleOpenConsult = (broadcast: AssessmentBroadcast) => {
    setConsultModalBroadcast(broadcast);
    setRecommendedTier(
      broadcast.admissionRequest.secondaryAcuityTier || broadcast.admissionRequest.primaryAcuityTier
    );
    setConsultImpression(
      broadcast.consultNotes ||
        'Agree with ED assessment. Patient requires continuous telemetry bed in cardiology ward.'
    );
    setDiversionEndorsed(broadcast.admissionRequest.diversionRecommended || false);
  };

  const handleSubmitConsult = (e: React.FormEvent) => {
    e.preventDefault();
    if (!consultModalBroadcast) return;
    consultMutation.mutate({
      id: consultModalBroadcast.id,
      data: {
        secondaryAcuityTier: recommendedTier,
        consultNotes: consultImpression,
        diversionRecommended: diversionEndorsed,
      },
    });
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

      {/* Specialty Cluster Filter Tabs */}
      <div className="flex flex-wrap items-center gap-1.5 bg-slate-100/90 p-1.5 rounded-xl border border-slate-200 shadow-2xs">
        <button
          type="button"
          onClick={() => setSelectedCluster(undefined)}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 cursor-pointer ${
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
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 cursor-pointer ${
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
            const isClaimed = broadcast.status === 'CLAIMED' || broadcast.status === 'COMPLETED';
            const isConsulted = broadcast.status === 'COMPLETED';

            const borderAccent = isConsulted
              ? 'border-l-4 border-l-emerald-500'
              : isClaimed
              ? 'border-l-4 border-l-purple-500'
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
                    <Badge
                      variant={
                        isConsulted
                          ? 'success'
                          : isClaimed
                          ? 'purple'
                          : 'secondary'
                      }
                      className="text-xs"
                    >
                      {broadcast.status}
                    </Badge>
                  </div>
                  <CardDescription className="text-xs flex items-center justify-between pt-1">
                    <span>Cluster: <strong>{broadcast.targetCluster}</strong></span>
                    <span>{patient.gender}, {patient.age}y • Class {patient.wardClassPreference}</span>
                  </CardDescription>
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

                  {/* Specialist Impression (if already consulted) */}
                  {isConsulted && (
                    <div className="p-3 bg-purple-50/80 rounded-xl border border-purple-200/70 text-xs text-purple-950 space-y-1">
                      <div className="font-semibold flex items-center gap-1">
                        <MessageSquare className="h-3.5 w-3.5 text-purple-600" />
                        Consult Impression ({broadcast.claimedBySpecialistId || 'Specialist'}):
                      </div>
                      <p className="italic text-purple-900 text-[11px] leading-relaxed">{broadcast.consultNotes}</p>
                      <div className="flex items-center gap-2 pt-1 font-medium">
                        <span>Recommended: <strong>{req.secondaryAcuityTier}</strong></span>
                        {req.diversionRecommended && (
                          <Badge variant="success" className="text-[10px]">Diversion Endorsed</Badge>
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

                    <Button
                      size="sm"
                      variant={isConsulted ? 'outline' : 'default'}
                      onClick={() => handleOpenConsult(broadcast)}
                      className="text-xs bg-purple-600 hover:bg-purple-700 text-white cursor-pointer"
                    >
                      <MessageSquare className="h-3.5 w-3.5 mr-1" />
                      {isConsulted ? 'Edit Consult' : 'Submit Consult Impression'}
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
                    checked={diversionEndorsed}
                    onChange={(e) => setDiversionEndorsed(e.target.checked)}
                    className="rounded border-slate-300 text-purple-600 focus:ring-purple-500"
                  />
                  <span>Endorse Alternative Diversion Pathway (Sister Hospital / MIC@Home)</span>
                </label>
                <p className="text-[11px] text-slate-500 ml-5 mt-0.5">
                  Allows BMU to evaluate direct diversion to Outram Community Hospital (OCH) or Hospital-at-Home.
                </p>
              </div>

              <DialogFooter className="pt-2">
                <Button type="button" variant="outline" onClick={() => setConsultModalBroadcast(null)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={consultMutation.isPending} className="bg-purple-600 hover:bg-purple-700 text-white">
                  {consultMutation.isPending ? 'Saving...' : 'Submit Consult (1-Click)'}
                </Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
