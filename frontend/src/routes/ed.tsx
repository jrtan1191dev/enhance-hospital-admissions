import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  useReactTable,
  getCoreRowModel,
  flexRender,
  type ColumnDef,
} from '@tanstack/react-table';
import { edQueries, specialistQueries, useSubmitEdAssessment } from '../services/queries';
import type { AcuityTier, InfectionStatus, Patient, SpecialtyCluster, WardClass } from '../types/admissions';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Input } from '../components/ui/input';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import { Stethoscope, CheckCircle2, AlertCircle, HeartPulse, ShieldAlert, Sparkles, Users, Clock } from 'lucide-react';

export function EdRoute() {
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'WAITING' | 'ASSESSED'>('WAITING');

  // Form State
  const [acuityTier, setAcuityTier] = useState<AcuityTier>('TIER_2_ACUTE_URGENT');
  const [specialty, setSpecialty] = useState<SpecialtyCluster>('CARDIOLOGY');
  const [wardClass, setWardClass] = useState<WardClass>('B2');
  const [telemetry, setTelemetry] = useState<boolean>(true);
  const [fallRisk, setFallRisk] = useState<boolean>(true);
  const [isolation, setIsolation] = useState<InfectionStatus>('NONE');
  const [clinicalNotes, setClinicalNotes] = useState<string>('Pre-populated diagnostic synthesis: Elevated Troponin with chest pain.');
  const [requiresSpecialistConsult, setRequiresSpecialistConsult] = useState<boolean>(false);
  const [targetClusters, setTargetClusters] = useState<SpecialtyCluster[]>(['CARDIOLOGY']);
  const [selectionTime, setSelectionTime] = useState<number>(Date.now());

  // Fetch Waiting Patients and Assessed Admissions via TanStack Query
  const { data: patients = [], isLoading, error } = useQuery(edQueries.patients());
  const { data: assessedAdmissions = [], isLoading: isLoadingAssessed } = useQuery(edQueries.admissions());
  const { data: allBroadcasts = [] } = useQuery(specialistQueries.broadcasts());

  const escalatedBroadcasts = allBroadcasts.filter((b) => b.status === 'AUTO_ESCALATED');

  // Submit Assessment Mutation via centralized TanStack Query service hook
  const submitMutation = useSubmitEdAssessment(() => {
    setSuccessMessage(`Assessment submitted successfully for ${selectedPatient?.name}! Bed request queued.`);
    setSelectedPatient(null);
    setTimeout(() => setSuccessMessage(null), 5000);
  });

  // Auto-select patient and initialize smart form
  const handleSelectPatient = (patient: Patient) => {
    setSelectedPatient(patient);
    setSuccessMessage(null);
    setSelectionTime(Date.now());
    setRequiresSpecialistConsult(false);
    if (patient.labTroponin && patient.labTroponin !== 'Normal') {
      setAcuityTier('TIER_2_ACUTE_URGENT');
      setSpecialty('CARDIOLOGY');
      setTargetClusters(['CARDIOLOGY']);
      setTelemetry(true);
    } else {
      setAcuityTier('TIER_3_ACUTE_STABLE');
      setSpecialty('GENERAL_MEDICINE');
      setTargetClusters(['GENERAL_MEDICINE']);
      setTelemetry(false);
    }
    setWardClass(patient.wardClassPreference || 'B2');
    setIsolation(patient.infectionStatus || 'NONE');
    setFallRisk((patient.fallRiskScore || 0) > 50);
    setClinicalNotes(`EHR Synthesis: ${patient.suspectedDiagnosis || 'Acute presentation'}. Vitals BP ${patient.vitalsBp || '120/80'}, SpO2 ${patient.vitalsSpo2 || 98}%.`);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPatient) return;

    const isTroponin = !!(selectedPatient.labTroponin && selectedPatient.labTroponin !== 'Normal');
    const recTier: AcuityTier = isTroponin ? 'TIER_2_ACUTE_URGENT' : 'TIER_3_ACUTE_STABLE';
    const recSpecialty: SpecialtyCluster = isTroponin ? 'CARDIOLOGY' : 'GENERAL_MEDICINE';
    const recTelemetry = isTroponin;
    const recWardClass = selectedPatient.wardClassPreference || 'B2';

    const overrides: import('../types/admissions').ClinicalBaselineOverride[] = [];
    if (acuityTier !== recTier) {
      overrides.push({ field: 'acuityTier', originalValue: recTier, submittedValue: acuityTier });
    }
    if (specialty !== recSpecialty) {
      overrides.push({ field: 'specialty', originalValue: recSpecialty, submittedValue: specialty });
    }
    if (wardClass !== recWardClass) {
      overrides.push({ field: 'wardClass', originalValue: recWardClass, submittedValue: wardClass });
    }
    if (telemetry !== recTelemetry) {
      overrides.push({ field: 'telemetry', originalValue: String(recTelemetry), submittedValue: String(telemetry) });
    }

    const elapsedMins = Math.max(0.1, Math.round(((Date.now() - selectionTime) / 60000) * 10) / 10);

    submitMutation.mutate({
      patientId: selectedPatient.id,
      suspectedDiagnosisService: specialty,
      primaryAcuityTier: acuityTier,
      requestedWardClass: wardClass,
      primaryTelemetry: telemetry,
      requiresSpecialistConsult,
      targetClusters: requiresSpecialistConsult ? (targetClusters.length > 0 ? targetClusters : [specialty]) : undefined,
      overrides: overrides.length > 0 ? overrides : undefined,
      clinicalNotes,
      recommendedAccepted: overrides.length === 0,
      elapsedMins,
    });
  };

  // TanStack Table columns definition
  const columns: ColumnDef<Patient>[] = [
    {
      accessorKey: 'name',
      header: 'Patient Name & NRIC',
      cell: ({ row }) => (
        <div>
          <div className="font-semibold text-slate-900">{row.original.name}</div>
          <div className="text-xs text-slate-500 font-mono">{row.original.nric} • {row.original.gender}, {row.original.age}y</div>
        </div>
      ),
    },
    {
      accessorKey: 'suspectedDiagnosis',
      header: 'Suspected Diagnosis',
      cell: ({ row }) => (
        <span className="text-xs font-medium text-slate-700 bg-slate-100 px-2 py-1 rounded">
          {row.original.suspectedDiagnosis || 'Pending triage review'}
        </span>
      ),
    },
    {
      id: 'vitals',
      header: 'Key Vitals & Labs',
      cell: ({ row }) => (
        <div className="text-xs space-y-0.5">
          <div>BP: <span className="font-semibold text-slate-700">{row.original.vitalsBp || '--'}</span> | SpO2: <span className="font-semibold text-slate-700">{row.original.vitalsSpo2 || '--'}%</span></div>
          <div className="text-red-600 font-medium">Troponin: {row.original.labTroponin || 'Normal'}</div>
        </div>
      ),
    },
    {
      accessorKey: 'wardClassPreference',
      header: 'Class & Precautions',
      cell: ({ row }) => (
        <div className="flex flex-wrap gap-1">
          <Badge variant="outline" className="text-[10px]">{row.original.wardClassPreference}</Badge>
          {row.original.infectionStatus !== 'NONE' && (
            <Badge variant="destructive" className="text-[10px]">{row.original.infectionStatus}</Badge>
          )}
        </div>
      ),
    },
    {
      id: 'action',
      header: 'Action',
      cell: ({ row }) => (
        <Button
          size="sm"
          variant={selectedPatient?.id === row.original.id ? 'default' : 'outline'}
          onClick={() => handleSelectPatient(row.original)}
        >
          {selectedPatient?.id === row.original.id ? 'Reviewing' : 'Assess'}
        </Button>
      ),
    },
  ];

  const table = useReactTable({
    data: patients,
    columns,
    getCoreRowModel: getCoreRowModel(),
  });

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Stethoscope className="h-6 w-6 text-blue-600" />
            Emergency Department (ED) Clinical Intake
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Automated diagnostic parameter synthesis with 1-click clinical directives submission and parallel specialist broadcast.
          </p>
        </div>
        <Badge variant="outline" className="self-start sm:self-center px-3 py-1 text-xs bg-blue-50 text-blue-700 border-blue-200">
          Attending Lead: Dr. Tan (dr_tan_ed)
        </Badge>
      </div>

      {successMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-800 text-sm flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 text-emerald-600 flex-shrink-0" />
          {successMessage}
        </div>
      )}

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-red-800 text-sm flex items-center gap-2">
          <AlertCircle className="h-4 w-4 text-red-600 flex-shrink-0" />
          Failed to load ED patient queue: {(error as Error).message}
        </div>
      )}

      {/* High Priority SLA Auto-Escalation Alert Banner */}
      {escalatedBroadcasts.length > 0 && (
        <div className="p-3.5 bg-red-50 border-l-4 border-l-red-600 border border-red-200 rounded-xl flex items-center justify-between text-red-950 text-xs shadow-xs">
          <div className="flex items-center gap-2.5">
            <AlertCircle className="h-5 w-5 text-red-600 shrink-0 animate-bounce" />
            <div>
              <span className="font-bold text-red-900">
                CRITICAL SLA ALERT: {escalatedBroadcasts.length} Consult {escalatedBroadcasts.length === 1 ? 'Broadcast' : 'Broadcasts'} Auto-Escalated!
              </span>
              <p className="text-[11px] text-red-700 mt-0.5">
                Consult request breached acuity SLA without claim ({escalatedBroadcasts.map(b => b.targetCluster).join(', ')}). Assigned to on-call cluster lead.
              </p>
            </div>
          </div>
          <Badge variant="destructive" className="text-[10px] uppercase font-mono tracking-wider animate-pulse">
            SLA Breached
          </Badge>
        </div>
      )}

      {/* Board Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-200">
        <button
          type="button"
          onClick={() => setActiveTab('WAITING')}
          className={`px-4 py-2.5 text-sm font-semibold border-b-2 flex items-center gap-2 cursor-pointer transition-all ${
            activeTab === 'WAITING'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <HeartPulse className="h-4 w-4" />
          Awaiting Assessment
          <Badge variant={activeTab === 'WAITING' ? 'default' : 'secondary'} className="text-xs">
            {patients.length}
          </Badge>
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('ASSESSED')}
          className={`px-4 py-2.5 text-sm font-semibold border-b-2 flex items-center gap-2 cursor-pointer transition-all ${
            activeTab === 'ASSESSED'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <CheckCircle2 className="h-4 w-4" />
          Assessed Admissions
          <Badge variant={activeTab === 'ASSESSED' ? 'default' : 'secondary'} className="text-xs">
            {assessedAdmissions.length}
          </Badge>
        </button>
      </div>

      {activeTab === 'ASSESSED' ? (
        <Card>
          <CardHeader className="pb-3 border-b border-slate-100">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <CardTitle className="text-lg flex items-center gap-2">
                  Assessed Admissions Tracker
                  <Badge variant="secondary" className="font-mono text-xs">{assessedAdmissions.length}</Badge>
                </CardTitle>
                <CardDescription>Track all submitted ED admissions, real-time consult status, and bed allocation progress.</CardDescription>
              </div>
              <Badge variant="secondary" className="self-start sm:self-center text-xs flex items-center gap-1.5">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-ping" />
                Live Polling
              </Badge>
            </div>
          </CardHeader>
          <CardContent className="p-0">
            {isLoadingAssessed ? (
              <div className="p-8 text-center text-slate-500 text-sm">Loading assessed admissions...</div>
            ) : assessedAdmissions.length === 0 ? (
              <div className="p-8 text-center text-slate-500 text-sm">No admissions have been assessed yet.</div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="text-xs font-semibold">Patient & Token</TableHead>
                    <TableHead className="text-xs font-semibold">Acuity (Primary / Effective)</TableHead>
                    <TableHead className="text-xs font-semibold">Discipline & Class</TableHead>
                    <TableHead className="text-xs font-semibold">Care Directives</TableHead>
                    <TableHead className="text-xs font-semibold">Admission Status</TableHead>
                    <TableHead className="text-xs font-semibold">Assigned Bed</TableHead>
                    <TableHead className="text-xs font-semibold">Submitted At</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {assessedAdmissions.map((admission) => (
                    <TableRow key={admission.id} className="hover:bg-slate-50">
                      <TableCell className="py-3 text-xs">
                        <div className="font-semibold text-slate-900">{admission.patient?.name || 'Unknown Patient'}</div>
                        <div className="text-[11px] text-slate-500 font-mono">
                          {admission.patient?.queueToken || admission.id.slice(0, 8)} • {admission.patient?.nric || 'N/A'}
                        </div>
                      </TableCell>
                      <TableCell className="py-3 text-xs">
                        <div className="flex items-center gap-1">
                          <Badge variant="outline" className="text-[10px]">
                            {admission.primaryAcuityTier.replace('TIER_', 'T').replace('_', ' ')}
                          </Badge>
                          {admission.effectiveAcuityTier && (
                            <Badge variant="secondary" className="text-[10px] bg-blue-50 text-blue-700 font-semibold">
                              Eff: {admission.effectiveAcuityTier.replace('TIER_', 'T').replace('_', ' ')}
                            </Badge>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="py-3 text-xs">
                        <div className="font-medium text-slate-800">
                          {admission.suspectedDiagnosisService || admission.admittingSpecialtyCluster || 'GENERAL_MEDICINE'}
                        </div>
                        <div className="text-[11px] text-slate-500">
                          Class {admission.requestedWardClass || 'B2'}
                        </div>
                      </TableCell>
                      <TableCell className="py-3 text-xs">
                        <div className="flex items-center gap-1">
                          {admission.effectiveTelemetry || admission.primaryTelemetry ? (
                            <Badge variant="outline" className="text-[10px] bg-purple-50 text-purple-700 border-purple-200 flex items-center gap-1">
                              <HeartPulse className="h-3 w-3" /> Telemetry
                            </Badge>
                          ) : (
                            <span className="text-[11px] text-slate-400">Standard</span>
                          )}
                          {admission.discordant && (
                            <Badge variant="destructive" className="text-[10px]">
                              Discordant
                            </Badge>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="py-3 text-xs">
                        <div className="flex flex-col gap-1 items-start">
                          <Badge
                            variant={
                              admission.status === 'BED_ALLOCATED'
                                ? 'default'
                                : admission.status === 'BED_REQUESTED'
                                ? 'secondary'
                                : 'outline'
                            }
                            className="text-[10px] font-semibold"
                          >
                            {admission.status}
                          </Badge>
                          {allBroadcasts.some(b => b.admissionRequest?.id === admission.id && b.status === 'AUTO_ESCALATED') && (
                            <Badge variant="destructive" className="text-[9px] py-0 px-1 font-mono uppercase animate-pulse">
                              Auto-Escalated
                            </Badge>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="py-3 text-xs font-mono font-medium text-slate-700">
                        {admission.assignedBed ? (
                          <span className="text-emerald-700 font-semibold">{admission.assignedBed.bedNumber}</span>
                        ) : (
                          <span className="text-slate-400 italic">Pending Allocation</span>
                        )}
                      </TableCell>
                      <TableCell className="py-3 text-xs text-slate-500 flex items-center gap-1">
                        <Clock className="h-3 w-3 text-slate-400" />
                        {admission.requestedAt ? new Date(admission.requestedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Recent'}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      ) : (
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Waiting ED Patient Queue Table (TanStack Table) */}
        <div className="lg:col-span-7 space-y-4">
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <CardTitle className="text-lg flex items-center gap-2">
                    Waiting ED Patients
                    <Badge variant="secondary" className="font-mono text-xs">{patients.length}</Badge>
                  </CardTitle>
                  <CardDescription>Select a patient to review synthesized findings and dispatch an admission bed request.</CardDescription>
                </div>
                <Badge variant="secondary" className="self-start sm:self-center text-xs flex items-center gap-1.5">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-ping" />
                  Live Polling
                </Badge>
              </div>

              {/* Triage Quick Stats Bar */}
              <div className="flex flex-wrap items-center gap-2 pt-2">
                <div className="text-[11px] font-medium px-2.5 py-1 rounded-md bg-slate-100 text-slate-700 flex items-center gap-1">
                  <span>Waiting:</span>
                  <span className="font-bold text-slate-900">{patients.length}</span>
                </div>
                <div className="text-[11px] font-medium px-2.5 py-1 rounded-md bg-red-50 text-red-800 border border-red-200/60 flex items-center gap-1">
                  <span>Elevated Trop:</span>
                  <span className="font-bold">{patients.filter(p => p.labTroponin && p.labTroponin !== 'Normal').length}</span>
                </div>
                <div className="text-[11px] font-medium px-2.5 py-1 rounded-md bg-amber-50 text-amber-800 border border-amber-200/60 flex items-center gap-1">
                  <span>High Fall Risk:</span>
                  <span className="font-bold">{patients.filter(p => (p.fallRiskScore || 0) > 50).length}</span>
                </div>
              </div>
            </CardHeader>
            <CardContent className="p-0">
              {isLoading ? (
                <div className="p-8 text-center text-slate-500 text-sm">Loading waiting queue...</div>
              ) : patients.length === 0 ? (
                <div className="p-8 text-center text-slate-500 text-sm">No patients currently waiting in ED triage.</div>
              ) : (
                <Table>
                  <TableHeader>
                    {table.getHeaderGroups().map((headerGroup) => (
                      <TableRow key={headerGroup.id}>
                        {headerGroup.headers.map((header) => (
                          <TableHead key={header.id} className="text-xs font-semibold">
                            {header.isPlaceholder
                              ? null
                              : flexRender(header.column.columnDef.header, header.getContext())}
                          </TableHead>
                        ))}
                      </TableRow>
                    ))}
                  </TableHeader>
                  <TableBody>
                    {table.getRowModel().rows.map((row) => (
                      <TableRow
                        key={row.id}
                        className={selectedPatient?.id === row.original.id ? 'bg-blue-50/80 ring-1 ring-inset ring-blue-300' : 'hover:bg-slate-50 cursor-pointer'}
                        onClick={() => handleSelectPatient(row.original)}
                      >
                        {row.getVisibleCells().map((cell) => (
                          <TableCell key={cell.id} className="py-3 text-xs">
                            {flexRender(cell.column.columnDef.cell, cell.getContext())}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Smart Pre-Populated Assessment Form */}
        <div className="lg:col-span-5">
          <Card className="border-blue-200 shadow-sm">
            <CardHeader className="bg-slate-50 border-b border-slate-200">
              <div className="flex items-center justify-between">
                <CardTitle className="text-base flex items-center gap-2 text-slate-900">
                  <Sparkles className="h-4 w-4 text-blue-600" />
                  Smart Assessment Dossier
                </CardTitle>
                {selectedPatient && (
                  <Badge variant="outline" className="font-mono text-xs bg-white text-blue-800 border-blue-200">
                    {selectedPatient.queueToken}
                  </Badge>
                )}
              </div>
              <CardDescription>
                {selectedPatient
                  ? `Reviewing clinical parameters for ${selectedPatient.name}`
                  : 'Select a patient from the queue to open smart assessment'}
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-5">
              {!selectedPatient ? (
                <div className="py-12 text-center text-slate-400 text-sm">
                  <HeartPulse className="h-10 w-10 mx-auto text-slate-300 mb-2" />
                  Select an ED patient on the left to review synthesized labs, vitals, and approve admission directives.
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="space-y-4">
                  {/* Pre-populated Patient Banner */}
                  <div className="p-3 bg-gradient-to-r from-blue-50/80 to-slate-50 rounded-xl border border-blue-100 text-xs space-y-2">
                    <div className="font-semibold text-blue-950 flex items-center justify-between">
                      <span className="text-sm">{selectedPatient.name}</span>
                      <span className="font-mono text-slate-500">{selectedPatient.nric}</span>
                    </div>
                    <div className="text-slate-600 text-[11px]">
                      <strong>Suspected:</strong> {selectedPatient.suspectedDiagnosis || 'Chest pain under evaluation'}
                    </div>
                    <div className="grid grid-cols-3 gap-2 pt-1 font-mono text-[11px] text-slate-700 bg-white/70 p-2 rounded-lg border border-blue-100">
                      <div>BP: <strong>{selectedPatient.vitalsBp || '120/80'}</strong></div>
                      <div>HR: <strong>{selectedPatient.vitalsHr || 78} bpm</strong></div>
                      <div>SpO2: <strong>{selectedPatient.vitalsSpo2 || 98}%</strong></div>
                    </div>
                  </div>

                  {/* Urgency Tier */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                      Urgency Acuity Tier
                    </label>
                    <select
                      value={acuityTier}
                      onChange={(e) => setAcuityTier(e.target.value as AcuityTier)}
                      className="w-full text-xs bg-white border border-slate-300 rounded-lg px-3 py-2 font-medium shadow-2xs focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none transition-all cursor-pointer"
                    >
                      <option value="TIER_1_CRITICAL">Tier 1: Critical / Resuscitation (ICU/HD)</option>
                      <option value="TIER_2_ACUTE_URGENT">Tier 2: Acute Urgent (Telemetry / High Monitoring)</option>
                      <option value="TIER_3_ACUTE_STABLE">Tier 3: Acute Stable (General Medical/Surgical)</option>
                      <option value="TIER_4_SUBACUTE_DIVERSION">Tier 4: Subacute / Potential Diversion</option>
                      <option value="TIER_5_OBSERVATION">Tier 5: Extended Observation / CDU</option>
                    </select>
                  </div>

                  {/* Specialty Cluster & Ward Class */}
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                        Admitting Specialty
                      </label>
                      <select
                        value={specialty}
                        onChange={(e) => setSpecialty(e.target.value as SpecialtyCluster)}
                        className="w-full text-xs bg-white border border-slate-300 rounded-lg px-3 py-2 font-medium shadow-2xs focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none transition-all cursor-pointer"
                      >
                        <option value="CARDIOLOGY">Cardiology</option>
                        <option value="GENERAL_MEDICINE">General Medicine</option>
                        <option value="SURGERY">General Surgery</option>
                        <option value="ORTHOPAEDICS">Orthopaedics</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                        Ward Class Preference
                      </label>
                      <select
                        value={wardClass}
                        onChange={(e) => setWardClass(e.target.value as WardClass)}
                        className="w-full text-xs bg-white border border-slate-300 rounded-lg px-3 py-2 font-medium shadow-2xs focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none transition-all cursor-pointer"
                      >
                        <option value="B2">Class B2 (5-6 bed cubicle)</option>
                        <option value="C">Class C (Partitioned cubicle)</option>
                        <option value="B1">Class B1 (4 bed cubicle)</option>
                        <option value="A">Class A (Single room)</option>
                      </select>
                    </div>
                  </div>

                  {/* Clinical Constraints Directive Tiles (:has() styled) */}
                  <div className="space-y-2 pt-1 border-t border-slate-200">
                    <label className="block text-xs font-semibold text-slate-700">
                      Care & Monitoring Directives
                    </label>
                    <div className="grid grid-cols-1 gap-2">
                      <label className="flex items-center gap-3 p-2.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 cursor-pointer transition-all has-[:checked]:border-blue-500 has-[:checked]:bg-blue-50/40 has-[:checked]:ring-1 has-[:checked]:ring-blue-500/30">
                        <input
                          type="checkbox"
                          checked={telemetry}
                          onChange={(e) => setTelemetry(e.target.checked)}
                          className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                        />
                        <div className="flex-1">
                          <div className="text-xs font-semibold text-slate-800 flex items-center gap-1.5">
                            <HeartPulse className="h-3.5 w-3.5 text-blue-600" />
                            Continuous Telemetry Monitoring
                          </div>
                          <div className="text-[11px] text-slate-500">Requires cardiac rhythm monitoring equipped bed</div>
                        </div>
                      </label>

                      <label className="flex items-center gap-3 p-2.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 cursor-pointer transition-all has-[:checked]:border-amber-500 has-[:checked]:bg-amber-50/40 has-[:checked]:ring-1 has-[:checked]:ring-amber-500/30">
                        <input
                          type="checkbox"
                          checked={fallRisk}
                          onChange={(e) => setFallRisk(e.target.checked)}
                          className="h-4 w-4 rounded border-slate-300 text-amber-600 focus:ring-amber-500"
                        />
                        <div className="flex-1">
                          <div className="text-xs font-semibold text-slate-800 flex items-center gap-1.5">
                            <AlertCircle className="h-3.5 w-3.5 text-amber-600" />
                            High Fall Risk Precautions
                          </div>
                          <div className="text-[11px] text-slate-500">Heuristic prioritizes proximity to nursing station</div>
                        </div>
                      </label>
                    </div>
                  </div>

                  {/* Isolation Status */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1.5">
                      <ShieldAlert className="h-3.5 w-3.5 text-amber-600" />
                      Infection / Isolation Precautions
                    </label>
                    <select
                      value={isolation}
                      onChange={(e) => setIsolation(e.target.value as InfectionStatus)}
                      className="w-full text-xs bg-white border border-slate-300 rounded-lg px-3 py-2 font-medium shadow-2xs focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none transition-all cursor-pointer"
                    >
                      <option value="NONE">None (Standard Precautions)</option>
                      <option value="CONTACT_MRSA">Contact Isolation (MRSA/VRE)</option>
                      <option value="AIRBORNE_COVID">Airborne Isolation (COVID/TB - Negative Pressure)</option>
                      <option value="DROPLET">Droplet Isolation (Influenza)</option>
                    </select>
                  </div>

                  {/* Clinical Directives Notes */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                      Attending Directives & EHR Note
                    </label>
                    <Input
                      value={clinicalNotes}
                      onChange={(e) => setClinicalNotes(e.target.value)}
                      className="text-xs rounded-lg"
                    />
                  </div>
                  {/* Admission Pathway: Direct vs Consult-Gated */}
                  <div className="pt-2 border-t border-slate-200">
                    <label className="flex items-center gap-3 p-2.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 cursor-pointer transition-all has-[:checked]:border-purple-500 has-[:checked]:bg-purple-50/40 has-[:checked]:ring-1 has-[:checked]:ring-purple-500/30">
                      <input
                        type="checkbox"
                        checked={requiresSpecialistConsult}
                        onChange={(e) => setRequiresSpecialistConsult(e.target.checked)}
                        className="h-4 w-4 rounded border-slate-300 text-purple-600 focus:ring-purple-500"
                      />
                      <div className="flex-1">
                        <div className="text-xs font-semibold text-slate-800 flex items-center gap-1.5">
                          <Users className="h-3.5 w-3.5 text-purple-600" />
                          Request Specialist Consult Pool (Consult-Gated)
                        </div>
                        <div className="text-[11px] text-slate-500">
                          Unchecked = Direct Admission (dispatches immediately to BMU queue in BED_REQUESTED)
                        </div>
                      </div>
                    </label>

                    {requiresSpecialistConsult && (
                      <div className="p-3 bg-purple-50/60 rounded-xl border border-purple-200 space-y-2 mt-2">
                        <label className="block text-xs font-semibold text-purple-950">
                          Target Specialty Clusters (Multi-Cluster Broadcast Pool)
                        </label>
                        <p className="text-[11px] text-purple-700 leading-snug">
                          Concurrent consult requests will be published to the selected clusters. Admission is held in <span className="font-semibold">ASSESSMENT_PENDING</span>.
                        </p>
                        <div className="grid grid-cols-2 gap-2 pt-1">
                          {(['CARDIOLOGY', 'GENERAL_MEDICINE', 'SURGERY', 'ORTHOPAEDICS'] as SpecialtyCluster[]).map((c) => {
                            const isSelected = targetClusters.includes(c);
                            return (
                              <label
                                key={c}
                                className={`flex items-center gap-2 p-2 rounded-lg border text-xs cursor-pointer transition-all ${
                                  isSelected
                                    ? 'bg-purple-100 border-purple-400 font-semibold text-purple-900 shadow-2xs'
                                    : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
                                }`}
                              >
                                <input
                                  type="checkbox"
                                  checked={isSelected}
                                  onChange={(e) => {
                                    if (e.target.checked) {
                                      setTargetClusters((prev) => [...prev, c]);
                                    } else {
                                      setTargetClusters((prev) => (prev.length > 1 ? prev.filter((item) => item !== c) : prev));
                                    }
                                  }}
                                  className="h-3.5 w-3.5 rounded border-slate-300 text-purple-600 focus:ring-purple-500"
                                />
                                <span>{c.replace('_', ' ')}</span>
                              </label>
                            );
                          })}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Submit Action */}
                  <Button
                    type="submit"
                    className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 mt-2 shadow-xs cursor-pointer"
                    disabled={submitMutation.isPending}
                  >
                    {submitMutation.isPending
                      ? 'Submitting Assessment...'
                      : requiresSpecialistConsult
                      ? 'Broadcast for Specialist Consult Pool'
                      : 'Confirm Direct Admission (1-Click to BMU)'}
                  </Button>
                </form>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
      )}
    </div>
  );
}
