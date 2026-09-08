import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  useReactTable,
  getCoreRowModel,
  flexRender,
  type ColumnDef,
} from '@tanstack/react-table';
import { edQueries, useSubmitEdAssessment } from '../services/queries';
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
import { Stethoscope, CheckCircle2, AlertCircle, HeartPulse, ShieldAlert, Sparkles } from 'lucide-react';

export function EdRoute() {
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form State
  const [acuityTier, setAcuityTier] = useState<AcuityTier>('TIER_2_ACUTE_URGENT');
  const [specialty, setSpecialty] = useState<SpecialtyCluster>('CARDIOLOGY');
  const [wardClass, setWardClass] = useState<WardClass>('CLASS_B2');
  const [telemetry, setTelemetry] = useState<boolean>(true);
  const [fallRisk, setFallRisk] = useState<boolean>(true);
  const [isolation, setIsolation] = useState<InfectionStatus>('NONE');
  const [clinicalNotes, setClinicalNotes] = useState<string>('Pre-populated diagnostic synthesis: Elevated Troponin with chest pain.');

  // Fetch Waiting Patients via TanStack Query queryOptions
  const { data: patients = [], isLoading, error } = useQuery(edQueries.patients());

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
    if (patient.labTroponin && patient.labTroponin !== 'Normal') {
      setAcuityTier('TIER_2_ACUTE_URGENT');
      setSpecialty('CARDIOLOGY');
      setTelemetry(true);
    } else {
      setAcuityTier('TIER_3_ACUTE_STABLE');
      setSpecialty('GENERAL_MEDICINE');
      setTelemetry(false);
    }
    setWardClass(patient.wardClassPreference || 'CLASS_B2');
    setIsolation(patient.infectionStatus || 'NONE');
    setFallRisk((patient.fallRiskScore || 0) > 50);
    setClinicalNotes(`EHR Synthesis: ${patient.suspectedDiagnosis || 'Acute presentation'}. Vitals BP ${patient.vitalsBp || '120/80'}, SpO2 ${patient.vitalsSpo2 || 98}%.`);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPatient) return;

    submitMutation.mutate({
      patientId: selectedPatient.id,
      acuityTier,
      specialtyCluster: specialty,
      wardClass,
      telemetryRequired: telemetry,
      fallRiskPrecautions: fallRisk,
      isolationRequired: isolation,
      clinicalNotes,
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

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Waiting ED Patient Queue Table (TanStack Table) */}
        <div className="lg:col-span-7 space-y-4">
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-lg">Waiting ED Patients ({patients.length})</CardTitle>
                  <CardDescription>Select a patient to review synthesized findings and dispatch an admission bed request.</CardDescription>
                </div>
                <Badge variant="secondary" className="text-xs">Live Polling</Badge>
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
                        className={selectedPatient?.id === row.original.id ? 'bg-blue-50/70' : 'hover:bg-slate-50'}
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
                  <Badge variant="outline" className="font-mono text-xs bg-white">
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
                  <div className="p-3 bg-blue-50/50 rounded-lg border border-blue-100 text-xs space-y-1">
                    <div className="font-semibold text-blue-950 flex items-center justify-between">
                      <span>{selectedPatient.name} ({selectedPatient.nric})</span>
                      <span>{selectedPatient.gender}, {selectedPatient.age} years old</span>
                    </div>
                    <div className="text-slate-600">
                      <strong>Suspected:</strong> {selectedPatient.suspectedDiagnosis || 'Chest pain under evaluation'}
                    </div>
                    <div className="grid grid-cols-3 gap-2 pt-1 font-mono text-[11px] text-slate-700">
                      <div>BP: <strong>{selectedPatient.vitalsBp || '120/80'}</strong></div>
                      <div>HR: <strong>{selectedPatient.vitalsHr || 78} bpm</strong></div>
                      <div>SpO2: <strong>{selectedPatient.vitalsSpo2 || 98}%</strong></div>
                    </div>
                  </div>

                  {/* Urgency Tier */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Urgency Acuity Tier
                    </label>
                    <select
                      value={acuityTier}
                      onChange={(e) => setAcuityTier(e.target.value as AcuityTier)}
                      className="w-full text-xs bg-white border border-slate-300 rounded-md px-3 py-2 focus:ring-1 focus:ring-blue-500 font-medium"
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
                      <label className="block text-xs font-semibold text-slate-700 mb-1">
                        Admitting Specialty
                      </label>
                      <select
                        value={specialty}
                        onChange={(e) => setSpecialty(e.target.value as SpecialtyCluster)}
                        className="w-full text-xs bg-white border border-slate-300 rounded-md px-3 py-2 focus:ring-1 focus:ring-blue-500 font-medium"
                      >
                        <option value="CARDIOLOGY">Cardiology</option>
                        <option value="GENERAL_MEDICINE">General Medicine</option>
                        <option value="SURGERY">General Surgery</option>
                        <option value="ORTHOPAEDICS">Orthopaedics</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1">
                        Ward Class Preference
                      </label>
                      <select
                        value={wardClass}
                        onChange={(e) => setWardClass(e.target.value as WardClass)}
                        className="w-full text-xs bg-white border border-slate-300 rounded-md px-3 py-2 focus:ring-1 focus:ring-blue-500 font-medium"
                      >
                        <option value="CLASS_B2">Class B2 (5-6 bed cubicle)</option>
                        <option value="CLASS_C">Class C (Partitioned cubicle)</option>
                        <option value="CLASS_B1">Class B1 (4 bed cubicle)</option>
                        <option value="CLASS_A">Class A (Single room)</option>
                      </select>
                    </div>
                  </div>

                  {/* Clinical Constraints Toggles */}
                  <div className="space-y-2 pt-1 border-t border-slate-200">
                    <label className="block text-xs font-semibold text-slate-700">
                      Care & Isolation Directives
                    </label>
                    <div className="flex flex-col gap-2">
                      <label className="flex items-center gap-2 text-xs text-slate-700 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={telemetry}
                          onChange={(e) => setTelemetry(e.target.checked)}
                          className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                        />
                        <span>Continuous Telemetry Monitoring Required</span>
                      </label>

                      <label className="flex items-center gap-2 text-xs text-slate-700 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={fallRisk}
                          onChange={(e) => setFallRisk(e.target.checked)}
                          className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                        />
                        <span>High Fall Risk Precautions (Near nursing station)</span>
                      </label>
                    </div>
                  </div>

                  {/* Isolation Status */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1 flex items-center gap-1">
                      <ShieldAlert className="h-3.5 w-3.5 text-amber-600" />
                      Infection / Isolation Precautions
                    </label>
                    <select
                      value={isolation}
                      onChange={(e) => setIsolation(e.target.value as InfectionStatus)}
                      className="w-full text-xs bg-white border border-slate-300 rounded-md px-3 py-2 focus:ring-1 focus:ring-blue-500 font-medium"
                    >
                      <option value="NONE">None (Standard Precautions)</option>
                      <option value="CONTACT_MRSA">Contact Isolation (MRSA/VRE)</option>
                      <option value="AIRBORNE_COVID">Airborne Isolation (COVID/TB - Negative Pressure)</option>
                      <option value="DROPLET">Droplet Isolation (Influenza)</option>
                    </select>
                  </div>

                  {/* Clinical Directives Notes */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Attending Directives & EHR Note
                    </label>
                    <Input
                      value={clinicalNotes}
                      onChange={(e) => setClinicalNotes(e.target.value)}
                      className="text-xs"
                    />
                  </div>

                  {/* Submit Action */}
                  <Button
                    type="submit"
                    className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 mt-2"
                    disabled={submitMutation.isPending}
                  >
                    {submitMutation.isPending ? 'Submitting Assessment...' : 'Confirm & Lead Assessment (1-Click)'}
                  </Button>
                </form>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
