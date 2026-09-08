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
    setRecommendedTier(broadcast.admissionRequest.primaryAcuityTier);
    setConsultImpression(
      broadcast.consultImpression ||
        'Agree with ED assessment. Patient requires continuous telemetry bed in cardiology ward.'
    );
    setDiversionEndorsed(broadcast.specialistDiversionEndorsed || false);
  };

  const handleSubmitConsult = (e: React.FormEvent) => {
    e.preventDefault();
    if (!consultModalBroadcast) return;
    consultMutation.mutate({
      id: consultModalBroadcast.id,
      data: {
        specialistId: 'dr_lim_cardio',
        consultImpression: consultImpression,
        recommendedAcuityTier: recommendedTier,
        specialistDiversionEndorsed: diversionEndorsed,
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

      {/* Specialty Cluster Filter Buttons */}
      <div className="flex flex-wrap items-center gap-2 bg-white p-3 rounded-lg border border-slate-200">
        <span className="text-xs font-semibold text-slate-500 mr-2">Filter Cluster:</span>
        <Button
          size="sm"
          variant={selectedCluster === undefined ? 'default' : 'outline'}
          onClick={() => setSelectedCluster(undefined)}
        >
          All Clusters
        </Button>
        {CLUSTERS.map((cluster) => (
          <Button
            key={cluster}
            size="sm"
            variant={selectedCluster === cluster ? 'default' : 'outline'}
            onClick={() => setSelectedCluster(cluster)}
          >
            {cluster.replace('_', ' ')}
          </Button>
        ))}
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
            const isClaimed = broadcast.status === 'CLAIMED' || broadcast.status === 'CONSULTED';
            const isConsulted = broadcast.status === 'CONSULTED';

            return (
              <Card key={broadcast.id} className="border-slate-200 shadow-xs hover:border-slate-300 transition-colors">
                <CardHeader className="pb-3 border-b border-slate-100 bg-slate-50/50">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-slate-900 text-base">{patient.name}</span>
                      <Badge variant="outline" className="text-[10px] font-mono">
                        {patient.queueToken}
                      </Badge>
                    </div>
                    <Badge
                      variant={
                        isConsulted
                          ? 'success'
                          : isClaimed
                          ? 'warning'
                          : 'secondary'
                      }
                      className="text-xs"
                    >
                      {broadcast.status}
                    </Badge>
                  </div>
                  <CardDescription className="text-xs flex items-center justify-between pt-1">
                    <span>Cluster: <strong>{broadcast.cluster}</strong></span>
                    <span>{patient.gender}, {patient.age}y • Class {patient.wardClassPreference}</span>
                  </CardDescription>
                </CardHeader>

                <CardContent className="pt-4 space-y-3">
                  {/* Clinical Baseline */}
                  <div className="p-3 bg-slate-50 rounded-lg text-xs space-y-1">
                    <div className="text-slate-700">
                      <strong>Suspected Diagnosis:</strong> {patient.suspectedDiagnosis || 'Cardiac/Medical evaluation'}
                    </div>
                    <div className="grid grid-cols-3 gap-2 text-slate-600 font-mono text-[11px] pt-1">
                      <div>BP: {patient.vitalsBp || '--'}</div>
                      <div>SpO2: {patient.vitalsSpo2 || '--'}%</div>
                      <div className="text-red-600 font-semibold">Trop: {patient.labTroponin || 'Normal'}</div>
                    </div>
                  </div>

                  {/* ED Attending Lead Details */}
                  <div className="text-xs text-slate-600 space-y-1">
                    <div>
                      <strong>ED Attending Priority:</strong>{' '}
                      <Badge variant="outline" className="text-[10px] font-semibold text-blue-700 bg-blue-50">
                        {req.primaryAcuityTier}
                      </Badge>
                    </div>
                    {patient.telemetryRequired && (
                      <Badge variant="outline" className="text-[10px] mr-1 bg-amber-50 text-amber-800 border-amber-200">
                        Telemetry Required
                      </Badge>
                    )}
                    {patient.infectionStatus !== 'NONE' && (
                      <Badge variant="destructive" className="text-[10px]">
                        {patient.infectionStatus}
                      </Badge>
                    )}
                  </div>

                  {/* Specialist Impression (if already consulted) */}
                  {isConsulted && (
                    <div className="p-3 bg-purple-50 rounded-lg border border-purple-100 text-xs text-purple-950 space-y-1">
                      <div className="font-semibold flex items-center gap-1">
                        <MessageSquare className="h-3.5 w-3.5 text-purple-600" />
                        Consult Impression ({broadcast.claimedByDoctor}):
                      </div>
                      <p className="italic text-purple-900">{broadcast.consultImpression}</p>
                      <div className="flex items-center gap-2 pt-1 font-medium">
                        <span>Recommended: <strong>{broadcast.recommendedAcuityTier}</strong></span>
                        {broadcast.specialistDiversionEndorsed && (
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
                        className="text-xs"
                      >
                        <Lock className="h-3.5 w-3.5 mr-1" />
                        Claim Broadcast Case
                      </Button>
                    )}

                    <Button
                      size="sm"
                      variant={isConsulted ? 'outline' : 'default'}
                      onClick={() => handleOpenConsult(broadcast)}
                      className="text-xs bg-purple-600 hover:bg-purple-700 text-white"
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
                  Patient: <strong>{consultModalBroadcast.admissionRequest.patient.name}</strong> ({consultModalBroadcast.admissionRequest.patient.nric}) • Cluster: {consultModalBroadcast.cluster}
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
