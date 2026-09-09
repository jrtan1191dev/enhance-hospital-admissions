import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  useReactTable,
  getCoreRowModel,
  flexRender,
  type ColumnDef,
} from '@tanstack/react-table';
import { bmuQueries, specialistQueries, useAllocateBed, useAssignAdmittingCluster, useReferSisterHospital, useRequestReconciliation } from '../services/queries';
import type { AdmissionRequest, Bed, SpecialtyCluster } from '../types/admissions';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import {
  Building2,
  CheckCircle2,
  AlertTriangle,
  Layers,
  Sparkles,
  ExternalLink,
  Clock,
  FileText,
  GitCompare,
} from 'lucide-react';

export function BmuRoute() {
  const [selectedRequest, setSelectedRequest] = useState<AdmissionRequest | null>(null);
  const [comparisonRequest, setComparisonRequest] = useState<AdmissionRequest | null>(null);
  const [diversionModalOpen, setDiversionModalOpen] = useState(false);
  const [selectedFacility, setSelectedFacility] = useState('Outram Community Hospital (OCH)');
  const [notification, setNotification] = useState<string | null>(null);

  // Queries via queries.ts queryOptions
  const { data: queue = [], isLoading: queueLoading } = useQuery(bmuQueries.queue());
  const { data: inventory = [], isLoading: inventoryLoading } = useQuery(bmuQueries.inventory());
  const { data: broadcasts = [] } = useQuery(specialistQueries.broadcasts());
  const { data: recommendations = [], isLoading: recsLoading } = useQuery(
    bmuQueries.recommendations(selectedRequest?.id)
  );

  // Bed Allocation Mutation Hook
  const allocateMutation = useAllocateBed(() => {
    setNotification('Bed successfully allocated/reallocated! Bed state transitioned to GREEN (In-Transit).');
    setSelectedRequest(null);
    setTimeout(() => setNotification(null), 5000);
  });

  // Authoritative Admitting Specialty Cluster Mutation Hook
  const assignClusterMutation = useAssignAdmittingCluster(() => {
    setNotification('Authoritative admitting specialty cluster updated! Algorithmic recommendations refreshed.');
    setTimeout(() => setNotification(null), 5000);
  });

  // Diversion Referral Mutation Hook
  const diversionMutation = useReferSisterHospital(() => {
    setNotification(`Referral initiated to ${selectedFacility} (30-min SLA timer active).`);
    setDiversionModalOpen(false);
    setSelectedRequest(null);
    setTimeout(() => setNotification(null), 5000);
  });

  // Clinical Reconciliation Mutation Hook
  const reconcileMutation = useRequestReconciliation(() => {
    setNotification('Clinical reconciliation request dispatched to ED attending and consulting specialists.');
    setTimeout(() => setNotification(null), 5000);
  });

  // Table Columns for Admission Queue (TanStack Table)
  const columns: ColumnDef<AdmissionRequest>[] = [
    {
      id: 'priorityTier',
      header: 'Priority Tier',
      cell: ({ row }) => {
        const effTier = row.original.effectiveAcuityTier || row.original.primaryAcuityTier;
        const isElevated = row.original.effectiveAcuityTier && row.original.effectiveAcuityTier !== row.original.primaryAcuityTier;
        const variantMap: Record<string, 'destructive' | 'warning' | 'secondary' | 'success' | 'outline'> = {
          TIER_1_CRITICAL: 'destructive',
          TIER_2_ACUTE_URGENT: 'warning',
          TIER_3_ACUTE_STABLE: 'secondary',
          TIER_4_SUBACUTE_DIVERSION: 'success',
        };
        return (
          <div className="flex flex-col gap-0.5 items-start">
            <Badge variant={variantMap[effTier] || 'outline'} className="text-[10px] font-semibold">
              {effTier}
            </Badge>
            {isElevated && (
              <span className="text-[9px] text-red-600 font-mono font-medium">
                ↑ From {row.original.primaryAcuityTier.replace('TIER_', 'T')}
              </span>
            )}
          </div>
        );
      },
    },
    {
      id: 'patientDetails',
      header: 'Patient Details',
      cell: ({ row }) => (
        <div>
          <div className="font-semibold text-slate-900">{row.original.patient.name}</div>
          <div className="text-xs text-slate-500 font-mono">
            {row.original.patient.queueToken} • {row.original.patient.gender}, {row.original.patient.age}y
          </div>
        </div>
      ),
    },
    {
      accessorKey: 'discordant',
      header: 'Safety Alignment & Alerts',
      cell: ({ row }) => (
        <div className="space-y-1">
          <div className="flex flex-wrap items-center gap-1">
            {row.original.discordant ? (
              <Badge
                variant="destructive"
                className="text-[10px] flex items-center gap-1 cursor-pointer font-semibold animate-pulse"
                onClick={(e) => {
                  e.stopPropagation();
                  setComparisonRequest(row.original);
                }}
                title="Click to view comparative notes"
              >
                <AlertTriangle className="h-3 w-3" /> Discordance Flagged
              </Badge>
            ) : (
              <Badge variant="secondary" className="text-[10px] text-slate-600 bg-slate-100">
                Aligned
              </Badge>
            )}
            {row.original.reconciliationRequested && (
              <Badge variant="outline" className="text-[10px] bg-indigo-50 text-indigo-700 border-indigo-200 flex items-center gap-1 font-medium">
                <Clock className="h-3 w-3 text-indigo-500" /> Reconcile Pending
              </Badge>
            )}
          </div>
          {row.original.clinicalConditionUpdated && (
            <Badge variant="warning" className="text-[10px] bg-amber-100 text-amber-900 border-amber-300 flex items-center gap-1 font-semibold animate-pulse">
              <AlertTriangle className="h-3 w-3 text-amber-600" /> Condition Updated
            </Badge>
          )}
        </div>
      ),
    },
    {
      id: 'classNeeds',
      header: 'Class & Needs',
      cell: ({ row }) => (
        <div className="text-xs space-y-0.5">
          <Badge variant="outline" className="text-[10px]">{row.original.patient.wardClassPreference}</Badge>
          {(row.original.effectiveTelemetry || row.original.primaryTelemetry || row.original.patient.telemetryRequired) && (
            <span className="block text-[10px] text-amber-700 font-medium">Telemetry Req</span>
          )}
        </div>
      ),
    },
    {
      id: 'specialtyCluster',
      header: 'Specialty Cluster',
      cell: ({ row }) => {
        const admitting = row.original.admittingSpecialtyCluster;
        const suspected = row.original.suspectedDiagnosisService;
        return (
          <div className="text-xs space-y-0.5">
            {admitting ? (
              <Badge variant="outline" className="text-[10px] bg-blue-50 text-blue-800 border-blue-300 font-semibold">
                {admitting} (Admitting)
              </Badge>
            ) : suspected ? (
              <Badge variant="outline" className="text-[10px] text-slate-700">
                {suspected} (ED)
              </Badge>
            ) : (
              <span className="text-slate-400 text-[10px]">-</span>
            )}
          </div>
        );
      },
    },
    {
      accessorKey: 'status',
      header: 'Queue Status',
      cell: ({ row }) => (
        <Badge
          variant={
            row.original.status === 'BED_ALLOCATED'
              ? 'success'
              : row.original.status.startsWith('DIVERTED')
              ? 'purple'
              : 'secondary'
          }
          className="text-[10px]"
        >
          {row.original.status}
        </Badge>
      ),
    },
    {
      id: 'action',
      header: 'Action',
      cell: ({ row }) => (
        <div className="flex items-center gap-1.5">
          <Button
            size="sm"
            variant={selectedRequest?.id === row.original.id ? 'default' : 'outline'}
            onClick={() => setSelectedRequest(row.original)}
            className="text-xs"
          >
            {selectedRequest?.id === row.original.id ? 'Managing' : 'Select'}
          </Button>
          <Button
            size="sm"
            variant="ghost"
            title="View Comparative Notes"
            onClick={() => setComparisonRequest(row.original)}
            className="text-xs text-slate-600 hover:text-slate-900 px-2 cursor-pointer"
          >
            <GitCompare className="h-3.5 w-3.5" />
          </Button>
          {row.original.discordant && !row.original.reconciliationRequested && (
            <Button
              size="sm"
              variant="outline"
              title="Request Clinical Reconciliation"
              disabled={reconcileMutation.isPending}
              onClick={() => reconcileMutation.mutate(row.original.id)}
              className="text-[11px] h-7 px-2 border-indigo-200 text-indigo-700 hover:bg-indigo-50 cursor-pointer"
            >
              Reconcile
            </Button>
          )}
        </div>
      ),
    },
  ];

  const table = useReactTable({
    data: queue,
    columns,
    getCoreRowModel: getCoreRowModel(),
  });

  // Bed Status Styling Helper
  const getBedStatusBadge = (status: Bed['status']) => {
    switch (status) {
      case 'EMPTY_PENDING_CLEANING':
        return {
          bg: 'bg-amber-100 border-amber-300 text-amber-900',
          dot: 'bg-amber-500',
          label: 'MUSTARD YELLOW (Turnover 30m)',
        };
      case 'EMPTY_CLEANED':
        return {
          bg: 'bg-white border-slate-300 text-slate-800 shadow-xs hover:border-blue-400',
          dot: 'bg-slate-300',
          label: 'WHITE (Clean & Ready)',
        };
      case 'EMPTY_ASSIGNED':
        return {
          bg: 'bg-emerald-100 border-emerald-300 text-emerald-900',
          dot: 'bg-emerald-500',
          label: 'GREEN (Assigned / In Transit)',
        };
      case 'OCCUPIED_TAKEN':
        return {
          bg: 'bg-slate-200 border-slate-400 text-slate-700',
          dot: 'bg-slate-600',
          label: 'GREY (Occupied)',
        };
      default:
        return { bg: 'bg-slate-50', dot: 'bg-slate-400', label: status };
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Building2 className="h-6 w-6 text-blue-600" />
            Bed Management Unit (BMU) Capacity & Allocation Hub
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Deterministic heuristic constraint solver, dynamic holding batch recommendations, and 4-state bed lifecycle orchestration.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="outline" className="px-3 py-1 text-xs bg-slate-50 text-slate-700">
            BMU Coordinator: Wong (bmu_coord_wong)
          </Badge>
        </div>
      </div>

      {notification && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-800 text-sm flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 text-emerald-600 flex-shrink-0" />
          {notification}
        </div>
      )}

      {/* Hospital Capacity Executive KPI Cards */}
      {(() => {
        const allBeds = inventory.flatMap((w) => w.beds || []);
        const totalCount = allBeds.length;
        const occupied = allBeds.filter((b) => b.status === 'OCCUPIED_TAKEN').length;
        const assigned = allBeds.filter((b) => b.status === 'EMPTY_ASSIGNED').length;
        const cleaning = allBeds.filter((b) => b.status === 'EMPTY_PENDING_CLEANING').length;
        const clean = allBeds.filter((b) => b.status === 'EMPTY_CLEANED').length;
        const occRate = totalCount > 0 ? Math.round(((occupied + assigned) / totalCount) * 100) : 0;

        return (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
            <div className="p-4 rounded-xl border border-slate-200 bg-white shadow-2xs">
              <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Hospital Occupancy</div>
              <div className="flex items-baseline gap-2 mt-1">
                <span className="text-2xl font-bold text-slate-900">{occRate}%</span>
                <span className="text-xs text-slate-500 font-mono">({occupied + assigned}/{totalCount})</span>
              </div>
              <div className="w-full bg-slate-100 rounded-full h-1.5 mt-2 overflow-hidden">
                <div className="bg-blue-600 h-1.5 rounded-full" style={{ width: `${occRate}%` }} />
              </div>
            </div>

            <div className="p-4 rounded-xl border border-emerald-200 bg-emerald-50/40 shadow-2xs">
              <div className="text-xs font-semibold text-emerald-800 flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
                Green (In-Transit)
              </div>
              <div className="text-2xl font-bold text-emerald-950 mt-1">{assigned}</div>
              <div className="text-[11px] text-emerald-700 mt-1">Assigned, moving to bed</div>
            </div>

            <div className="p-4 rounded-xl border border-slate-300 bg-white shadow-2xs">
              <div className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-slate-400" />
                White (Available)
              </div>
              <div className="text-2xl font-bold text-slate-900 mt-1">{clean}</div>
              <div className="text-[11px] text-slate-500 mt-1">Ready for allocation</div>
            </div>

            <div className="p-4 rounded-xl border border-amber-300 bg-amber-50/50 shadow-2xs">
              <div className="text-xs font-semibold text-amber-900 flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-amber-500" />
                Mustard Yellow
              </div>
              <div className="text-2xl font-bold text-amber-950 mt-1">{cleaning}</div>
              <div className="text-[11px] text-amber-800 mt-1">Turnover cleaning (30m)</div>
            </div>

            <div className="p-4 rounded-xl border border-slate-300 bg-slate-100/70 shadow-2xs col-span-2 sm:col-span-1">
              <div className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-slate-600" />
                Grey (Occupied)
              </div>
              <div className="text-2xl font-bold text-slate-900 mt-1">{occupied}</div>
              <div className="text-[11px] text-slate-600 mt-1">Inpatient receiving care</div>
            </div>
          </div>
        );
      })()}

      {/* Main Allocation Workspace */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Prioritized Admission Queue (TanStack Table) */}
        <div className="lg:col-span-7 space-y-4">
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-lg">Prioritized Admission Requests ({queue.length})</CardTitle>
                  <CardDescription>Acuity-first priority queue with discordance alerts and dwell-time tracking.</CardDescription>
                </div>
                <Badge variant="secondary" className="text-xs">Live Polling</Badge>
              </div>
            </CardHeader>
            <CardContent className="p-0">
              {queueLoading ? (
                <div className="p-8 text-center text-slate-500 text-sm">Loading queue...</div>
              ) : queue.length === 0 ? (
                <div className="p-8 text-center text-slate-500 text-sm">No pending admission requests.</div>
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
                        className={selectedRequest?.id === row.original.id ? 'bg-blue-50/70' : 'hover:bg-slate-50'}
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

        {/* Selected Request & Algorithmic Bed Recommendation Card */}
        <div className="lg:col-span-5 space-y-4">
          <Card className="border-blue-200 shadow-sm">
            <CardHeader className="bg-slate-50 border-b border-slate-200">
              <div className="flex items-center justify-between">
                <CardTitle className="text-base flex items-center gap-2 text-slate-900">
                  <Sparkles className="h-4 w-4 text-blue-600" />
                  Solver Recommendation
                </CardTitle>
                {selectedRequest && (
                  <Badge variant="outline" className="font-mono text-xs bg-white">
                    {selectedRequest.patient.queueToken}
                  </Badge>
                )}
              </div>
              <CardDescription>
                {selectedRequest
                  ? `Scoring available beds for ${selectedRequest.patient.name}`
                  : 'Select an admission request on the left to evaluate bed placement'}
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-5">
              {!selectedRequest ? (
                <div className="py-12 text-center text-slate-400 text-sm">
                  <Layers className="h-10 w-10 mx-auto text-slate-300 mb-2" />
                  Click "Select" on any waiting admission request to review algorithmic bed matches or trigger diversion.
                </div>
              ) : selectedRequest.status !== 'BED_REQUESTED' && selectedRequest.status !== 'BED_ALLOCATED' ? (
                <div className="p-4 bg-slate-50 rounded-lg text-xs space-y-2">
                  <div className="font-semibold text-slate-800">
                    Request already resolved: <Badge variant="outline">{selectedRequest.status}</Badge>
                  </div>
                  {selectedRequest.assignedBed && (
                    <div className="text-slate-600">
                      Assigned Bed: <strong>{selectedRequest.assignedBed.bedNumber}</strong> (Ward {selectedRequest.assignedBed.ward?.wardCode})
                    </div>
                  )}
                </div>
              ) : (
                <div className="space-y-4">
                  {/* Tentative Reservation Status Banner for BED_ALLOCATED */}
                  {selectedRequest.status === 'BED_ALLOCATED' && (
                    <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-xs space-y-1">
                      <div className="flex items-center justify-between font-semibold text-emerald-900">
                        <div className="flex items-center gap-1.5">
                          <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                          Tentative Bed Reservation
                        </div>
                        <Badge variant="success" className="text-[10px]">
                          {selectedRequest.status}
                        </Badge>
                      </div>
                      <p className="text-emerald-800">
                        Currently reserved: <strong>Bed {selectedRequest.assignedBed?.bedNumber}</strong> (Ward {selectedRequest.assignedBed?.ward?.wardCode || selectedRequest.assignedBed?.ward?.specialty || ''}).
                      </p>
                      <p className="text-[11px] text-emerald-700">
                        Dynamic reallocation enabled. Selecting another recommended bed below safely reverts the current bed without phantom capacity locks.
                      </p>
                    </div>
                  )}

                  {/* Authoritative Admitting Specialty Cluster Selector */}
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-slate-800">Authoritative Admitting Specialty:</span>
                      <Badge variant="outline" className="text-[10px] bg-white font-mono">
                        {selectedRequest.admittingSpecialtyCluster || selectedRequest.suspectedDiagnosisService || 'UNASSIGNED'}
                      </Badge>
                    </div>
                    <div className="flex items-center gap-2">
                      <select
                        value={selectedRequest.admittingSpecialtyCluster || selectedRequest.suspectedDiagnosisService || ''}
                        onChange={(e) => {
                          const val = e.target.value as SpecialtyCluster;
                          if (val) {
                            assignClusterMutation.mutate({
                              requestId: selectedRequest.id,
                              cluster: val,
                            });
                            setSelectedRequest({
                              ...selectedRequest,
                              admittingSpecialtyCluster: val,
                            });
                          }
                        }}
                        disabled={assignClusterMutation.isPending}
                        className="flex-1 text-xs bg-white border border-slate-300 rounded px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-blue-500 font-medium text-slate-800"
                      >
                        <option value="CARDIOLOGY">Cardiology</option>
                        <option value="GENERAL_MEDICINE">General Medicine</option>
                        <option value="SURGERY">Surgery</option>
                        <option value="ORTHOPAEDICS">Orthopaedics</option>
                      </select>
                      {assignClusterMutation.isPending && (
                        <span className="text-[10px] text-slate-500">Updating...</span>
                      )}
                    </div>
                  </div>

                  {/* Specialist Endorsed Diversion Action */}
                  {selectedRequest.diversionRecommended && (
                    <div className="p-3 bg-teal-50 border border-teal-200 rounded-lg space-y-2">
                      <div className="flex items-center justify-between text-xs">
                        <span className="font-semibold text-teal-900 flex items-center gap-1.5">
                          <CheckCircle2 className="h-4 w-4 text-teal-600" />
                          Specialist Endorsed Alternative Care Pathway
                        </span>
                        <Badge className="bg-teal-100 text-teal-800 text-[10px] font-mono">
                          {selectedRequest.diversionPathway === 'HOSPITAL_AT_HOME_MIC' ? 'MIC@Home' : 'OCH Subacute'}
                        </Badge>
                      </div>
                      <p className="text-[11px] text-teal-700">
                        {selectedRequest.diversionPathway === 'HOSPITAL_AT_HOME_MIC'
                          ? 'Patient meets clinical stability criteria for home-based hospitalization under daily tele-monitoring & mobile nursing visits.'
                          : 'Patient endorsed for step-down rehabilitation at Outram Community Hospital to preserve acute tertiary beds.'}
                      </p>
                      <Button
                        size="sm"
                        disabled={diversionMutation.isPending}
                        className="w-full bg-teal-600 hover:bg-teal-700 text-white text-xs cursor-pointer shadow-xs"
                        onClick={() => {
                          const facility = selectedRequest.diversionPathway === 'HOSPITAL_AT_HOME_MIC'
                              ? 'Mobile Inpatient Care at Home (MIC@Home)'
                              : 'Outram Community Hospital (OCH)';
                          setSelectedFacility(facility);
                          diversionMutation.mutate({
                            admissionRequestId: selectedRequest.id,
                            facility,
                          });
                        }}
                      >
                        <ExternalLink className="h-3.5 w-3.5 mr-1" />
                        Enact Diversion ({selectedRequest.diversionPathway === 'HOSPITAL_AT_HOME_MIC' ? 'MIC@Home' : 'OCH'})
                      </Button>
                    </div>
                  )}

                  {/* Discordance Alert Banner */}
                  {selectedRequest.discordant && (
                    <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-900 space-y-1.5">
                      <div className="font-semibold flex items-center justify-between">
                        <div className="flex items-center gap-1.5 text-red-700">
                          <AlertTriangle className="h-4 w-4 text-red-600" />
                          Clinical Discordance Flagged
                        </div>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setComparisonRequest(selectedRequest)}
                          className="text-[11px] h-6 px-2 py-0 border-red-300 text-red-800 bg-white hover:bg-red-50 cursor-pointer"
                        >
                          <GitCompare className="h-3 w-3 mr-1" /> View Alignment
                        </Button>
                      </div>
                      <p className="text-[11px] text-red-800">
                        Primary ED Attending: <strong>{selectedRequest.primaryAcuityTier}</strong> vs Specialist Consults.
                        Defaulting to <strong>{selectedRequest.effectiveAcuityTier || selectedRequest.primaryAcuityTier}</strong> under Safety-First policy.
                      </p>
                    </div>
                  )}

                  {/* Patient Requirements Summary */}
                  <div className="p-3 bg-blue-50/60 rounded-lg border border-blue-100 text-xs space-y-1">
                    <div className="flex justify-between font-semibold text-blue-950">
                      <span>{selectedRequest.patient.name}</span>
                      <span>{selectedRequest.patient.gender}, {selectedRequest.patient.age}y</span>
                    </div>
                    <div className="text-slate-600">
                      Class: <strong>{selectedRequest.patient.wardClassPreference}</strong> • Telemetry: <strong>{selectedRequest.effectiveTelemetry || selectedRequest.patient.telemetryRequired ? 'Yes' : 'No'}</strong> • Fall Risk: <strong>{selectedRequest.patient.fallRiskScore}</strong>
                    </div>
                  </div>

                  {/* Top Recommended Beds */}
                  <div>
                    <h4 className="text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                      Top Algorithmic Matches (Pure Java Heuristics):
                    </h4>
                    {recsLoading ? (
                      <div className="text-xs text-slate-500 py-4 text-center">Calculating constraint scores...</div>
                    ) : recommendations.length === 0 ? (
                      <div className="text-xs text-slate-500 py-4 text-center">No compatible beds currently available.</div>
                    ) : (
                      <div className="space-y-2.5">
                        {recommendations.slice(0, 3).map((rec, idx) => {
                          const isCurrentBed = selectedRequest.assignedBed?.id === rec.bedId;
                          return (
                            <div
                              key={rec.bedId}
                              className={`p-3 rounded-lg border text-xs transition-all ${
                                isCurrentBed
                                  ? 'bg-blue-50/60 border-blue-300 ring-1 ring-blue-400'
                                  : idx === 0
                                  ? 'bg-emerald-50/60 border-emerald-300 ring-1 ring-emerald-400'
                                  : 'bg-white border-slate-200'
                              }`}
                            >
                              <div className="flex items-center justify-between font-semibold">
                                <div className="flex items-center gap-1.5">
                                  <span className="text-slate-900 text-sm">Bed {rec.bedNumber}</span>
                                  <Badge variant="outline" className="text-[10px] font-mono">
                                    {rec.wardName} (L{rec.level})
                                  </Badge>
                                  {isCurrentBed ? (
                                    <Badge variant="outline" className="text-[9px] px-1 py-0 bg-blue-100 text-blue-800 border-blue-300">
                                      Current Tentative Bed
                                    </Badge>
                                  ) : (rec.isRecommended || idx === 0) ? (
                                    <Badge variant="success" className="text-[9px] px-1 py-0">
                                      #1 Match
                                    </Badge>
                                  ) : null}
                                </div>
                                <span className="text-emerald-700 font-bold text-sm">Score: +{rec.score}</span>
                              </div>

                              <div className="flex flex-wrap gap-1 mt-1.5">
                                {(rec.scoreBreakdown || []).map((item, bIdx) => (
                                  <span
                                    key={bIdx}
                                    className="text-[10px] px-1.5 py-0.5 rounded bg-slate-100 text-slate-700 font-mono"
                                  >
                                    {item}
                                  </span>
                                ))}
                              </div>

                              <Button
                                size="sm"
                                disabled={allocateMutation.isPending || isCurrentBed}
                                className={`w-full mt-2.5 text-white font-medium text-xs h-8 ${
                                  isCurrentBed
                                    ? 'bg-slate-400 cursor-not-allowed'
                                    : selectedRequest.status === 'BED_ALLOCATED'
                                    ? 'bg-amber-600 hover:bg-amber-700'
                                    : 'bg-emerald-600 hover:bg-emerald-700'
                                }`}
                                onClick={() =>
                                  allocateMutation.mutate({
                                    admissionRequestId: selectedRequest.id,
                                    bedId: rec.bedId,
                                    rank: idx + 1,
                                    score: rec.score,
                                    overrideReason:
                                      selectedRequest.status === 'BED_ALLOCATED'
                                        ? 'DYNAMIC_BED_REALLOCATION'
                                        : idx > 0
                                        ? 'NON_TOP_RANK_SELECTION'
                                        : undefined,
                                  })
                                }
                              >
                                <CheckCircle2 className="h-3.5 w-3.5 mr-1" />
                                {isCurrentBed
                                  ? `Currently Reserved (Bed ${rec.bedNumber})`
                                  : selectedRequest.status === 'BED_ALLOCATED'
                                  ? `Reallocate to Bed ${rec.bedNumber}`
                                  : `Allocate Bed ${rec.bedNumber}`}
                              </Button>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>

                  {/* Operational Diversion Authority */}
                  <div className="pt-2 border-t border-slate-200">
                    <Button
                      variant="outline"
                      className="w-full text-xs text-purple-700 border-purple-200 hover:bg-purple-50"
                      onClick={() => setDiversionModalOpen(true)}
                    >
                      <ExternalLink className="h-3.5 w-3.5 mr-1" />
                      Enact Diversion to Sister Hospital / MIC@Home
                    </Button>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Interactive Bed Inventory Matrix */}
      <Card>
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <CardTitle className="text-lg flex items-center gap-2">
                <Layers className="h-5 w-5 text-blue-600" />
                Live Bed Inventory Matrix (Level → Ward → Bed)
              </CardTitle>
              <CardDescription>
                Real-time 4-state lifecycle machine representation across inpatient wards.
              </CardDescription>
            </div>

            {/* Legend */}
            <div className="flex flex-wrap items-center gap-3 text-xs">
              <div className="flex items-center gap-1.5">
                <span className="h-2.5 w-2.5 rounded-full bg-amber-500"></span>
                <span className="text-slate-600">Mustard Yellow (Cleaning)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="h-2.5 w-2.5 rounded-full bg-slate-300 border border-slate-400"></span>
                <span className="text-slate-600">White (Clean)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="h-2.5 w-2.5 rounded-full bg-emerald-500"></span>
                <span className="text-slate-600">Green (Assigned)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="h-2.5 w-2.5 rounded-full bg-slate-600"></span>
                <span className="text-slate-600">Grey (Occupied)</span>
              </div>
            </div>
          </div>
        </CardHeader>

        <CardContent>
          {inventoryLoading ? (
            <div className="p-8 text-center text-slate-500 text-sm">Loading bed inventory...</div>
          ) : inventory.length === 0 ? (
            <div className="p-8 text-center text-slate-500 text-sm">No wards found in hospital inventory.</div>
          ) : (
            <div className="space-y-6">
              {inventory.map((ward) => (
                <div key={ward.id} className="p-4 rounded-xl border border-slate-200 bg-slate-50/50 space-y-3">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-slate-900 text-base">Ward {ward.wardCode}</span>
                      <Badge variant="outline" className="text-[10px] font-mono">
                        Level {ward.levelNumber}
                      </Badge>
                      <Badge variant="secondary" className="text-[10px]">
                        {ward.wardClass}
                      </Badge>
                      <Badge variant="outline" className="text-[10px] bg-blue-50 text-blue-700">
                        {ward.specialty}
                      </Badge>
                    </div>

                    <div className="flex items-center gap-2 text-xs text-slate-500">
                      <span>Cohort Lock: <strong>{ward.genderCohortLocked || 'Unlocked (Flex)'}</strong></span>
                      {ward.infectionLocked && ward.infectionLocked !== 'NONE' && (
                        <Badge variant="destructive" className="text-[10px]">
                          {ward.infectionLocked}
                        </Badge>
                      )}
                    </div>
                  </div>

                  {/* Beds Grid */}
                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
                    {ward.beds?.map((bed) => {
                      const styling = getBedStatusBadge(bed.status);
                      return (
                        <div
                          key={bed.id}
                          className={`p-3 rounded-lg border flex flex-col justify-between transition-all ${styling.bg}`}
                        >
                          <div className="flex items-center justify-between">
                            <span className="font-bold text-sm text-slate-900">{bed.bedNumber}</span>
                            <span className={`h-2.5 w-2.5 rounded-full ${styling.dot}`} title={styling.label} />
                          </div>

                          <div className="mt-2 text-[10px] space-y-0.5">
                            <div className="font-medium truncate">{styling.label.split(' ')[0]}</div>
                            {bed.telemetryCapable && (
                              <div className="text-amber-700 font-semibold">Telemetry</div>
                            )}
                            {bed.assignedPatient && (
                              <div className="font-semibold text-slate-900 truncate">
                                Pt: {bed.assignedPatient.name}
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Diversion Modal Dialog (shadcn Dialog) */}
      <Dialog open={diversionModalOpen} onOpenChange={setDiversionModalOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <ExternalLink className="h-5 w-5 text-purple-600" />
              Sister Hospital & Diversion Routing
            </DialogTitle>
            <DialogDescription>
              {selectedRequest && (
                <span>
                  Refer patient <strong>{selectedRequest.patient.name}</strong> to an alternative care pathway under BMU operational authority.
                </span>
              )}
            </DialogDescription>
          </DialogHeader>

          {selectedRequest && (
            <div className="space-y-4 py-2 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Target Diversion Facility / Program
                </label>
                <select
                  value={selectedFacility}
                  onChange={(e) => setSelectedFacility(e.target.value)}
                  className="w-full bg-white border border-slate-300 rounded-md px-3 py-2 font-medium"
                >
                  <option value="Outram Community Hospital (OCH)">Outram Community Hospital (OCH)</option>
                  <option value="Alexandra Hospital (AH)">Alexandra Hospital (AH)</option>
                  <option value="St. Andrew's Community Hospital (SACH)">St. Andrew's Community Hospital (SACH)</option>
                  <option value="MIC@Home (Hospital-at-Home Virtual Ward)">MIC@Home (Hospital-at-Home Virtual Ward)</option>
                </select>
              </div>

              <div className="p-3 bg-purple-50 rounded-lg text-purple-900 space-y-1">
                <div className="font-semibold flex items-center gap-1">
                  <Clock className="h-3.5 w-3.5" /> 30-Minute Bilateral SLA
                </div>
                <p className="text-[11px]">
                  Referral packet will be dispatched electronically to the destination admissions desk. A 30-minute bilateral SLA timer is initiated.
                </p>
              </div>

              <DialogFooter className="pt-2">
                <Button variant="outline" onClick={() => setDiversionModalOpen(false)}>
                  Cancel
                </Button>
                <Button
                  disabled={diversionMutation.isPending}
                  className="bg-purple-600 hover:bg-purple-700 text-white"
                  onClick={() =>
                    diversionMutation.mutate({
                      admissionRequestId: selectedRequest.id,
                      facility: selectedFacility,
                    })
                  }
                >
                  {diversionMutation.isPending ? 'Dispatching...' : 'Dispatch Referral'}
                </Button>
              </DialogFooter>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Side-by-Side Clinical Consensus & Comparative Notes Dialog */}
      <Dialog open={!!comparisonRequest} onOpenChange={(open) => !open && setComparisonRequest(null)}>
        <DialogContent className="sm:max-w-3xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-slate-900">
              <GitCompare className="h-5 w-5 text-blue-600" />
              Clinical Consensus & Comparative Notes
            </DialogTitle>
            <DialogDescription>
              {comparisonRequest && (
                <span>
                  Patient: <strong>{comparisonRequest.patient.name}</strong> ({comparisonRequest.patient.nric}) • Token: {comparisonRequest.patient.queueToken} • Class: {comparisonRequest.requestedWardClass || 'B2'}
                </span>
              )}
            </DialogDescription>
          </DialogHeader>

          {comparisonRequest && (() => {
            const reqBroadcasts = broadcasts.filter(b => b.admissionRequest?.id === comparisonRequest.id);
            return (
              <div className="space-y-4 py-2">
                {/* Safety-First Policy Summary Banner */}
                <div className={`p-3 rounded-xl border text-xs space-y-1 ${
                  comparisonRequest.discordant
                    ? 'bg-amber-50 border-amber-200 text-amber-950'
                    : 'bg-emerald-50 border-emerald-200 text-emerald-950'
                }`}>
                  <div className="flex items-center justify-between font-semibold">
                    <span className="flex items-center gap-1.5">
                      {comparisonRequest.discordant ? (
                        <AlertTriangle className="h-4 w-4 text-amber-600" />
                      ) : (
                        <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                      )}
                      {comparisonRequest.discordant ? 'Safety-First Acuity Discordance Override Active' : 'Consensus Alignment Achieved'}
                    </span>
                    <Badge variant={comparisonRequest.discordant ? 'destructive' : 'success'} className="text-[10px]">
                      Effective: {comparisonRequest.effectiveAcuityTier || comparisonRequest.primaryAcuityTier}
                    </Badge>
                  </div>
                  <p className="text-[11px] text-slate-700">
                    Under hospital safety policy, the highest acuity tier and continuous telemetry constraints are automatically elevated for bed placement reservation.
                  </p>
                </div>

                {/* Clinical Discordance Reconciliation Prompt */}
                {comparisonRequest.discordant && (
                  <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 p-3 bg-indigo-50/80 border border-indigo-200 rounded-xl text-xs">
                    <div>
                      <div className="font-bold text-indigo-950 flex items-center gap-1.5">
                        <Clock className="h-4 w-4 text-indigo-600" />
                        Clinical Reconciliation Alert
                      </div>
                      <p className="text-[11px] text-slate-600 mt-0.5">
                        Alert ED attending and specialists to reconcile acuity/telemetry discordance. Tentative bed assignment continues non-blockingly.
                      </p>
                    </div>
                    <Button
                      size="sm"
                      variant="outline"
                      className="shrink-0 text-xs bg-white text-indigo-700 border-indigo-300 hover:bg-indigo-100 cursor-pointer"
                      disabled={reconcileMutation.isPending || comparisonRequest.reconciliationRequested}
                      onClick={() => {
                        reconcileMutation.mutate(comparisonRequest.id);
                        setComparisonRequest({ ...comparisonRequest, reconciliationRequested: true });
                      }}
                    >
                      {comparisonRequest.reconciliationRequested ? 'Reconciliation Pending' : 'Trigger Reconciliation'}
                    </Button>
                  </div>
                )}

                {/* Side-by-Side Comparison Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* Left Column: ED Attending Assessment */}
                  <div className="p-4 rounded-xl border border-blue-200 bg-blue-50/30 space-y-3">
                    <div className="flex items-center justify-between border-b border-blue-100 pb-2">
                      <div className="font-bold text-sm text-blue-900 flex items-center gap-1.5">
                        <FileText className="h-4 w-4 text-blue-600" />
                        ED Attending Assessment
                      </div>
                      <Badge variant="outline" className="text-[10px] bg-white text-blue-800 border-blue-200">
                        Primary Lead
                      </Badge>
                    </div>

                    <div className="space-y-2 text-xs">
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500">Primary Acuity Tier:</span>
                        <Badge variant="outline" className="font-semibold text-blue-700 bg-white">
                          {comparisonRequest.primaryAcuityTier}
                        </Badge>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500">Telemetry Required:</span>
                        <Badge variant={comparisonRequest.primaryTelemetry ? 'warning' : 'outline'} className="text-[10px]">
                          {comparisonRequest.primaryTelemetry ? 'Telemetry Required' : 'Standard'}
                        </Badge>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500">Suspected Service:</span>
                        <span className="font-semibold text-slate-800">{comparisonRequest.suspectedDiagnosisService || 'GENERAL_MEDICINE'}</span>
                      </div>
                      <div className="pt-2 border-t border-blue-100/70">
                        <span className="text-slate-500 block mb-1 font-medium">Diagnostic Notes & Synthesis:</span>
                        <div className="p-2.5 bg-white rounded-lg border border-blue-100 text-[11px] text-slate-700 italic">
                          {comparisonRequest.patient.suspectedDiagnosis || 'EHR Synthesis: Acute presentation evaluated at triage.'}
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Right Column: Consulting Specialists */}
                  <div className="p-4 rounded-xl border border-purple-200 bg-purple-50/30 space-y-3">
                    <div className="flex items-center justify-between border-b border-purple-100 pb-2">
                      <div className="font-bold text-sm text-purple-900 flex items-center gap-1.5">
                        <GitCompare className="h-4 w-4 text-purple-600" />
                        Specialist Consultations ({reqBroadcasts.length})
                      </div>
                      <Badge variant="outline" className="text-[10px] bg-white text-purple-800 border-purple-200">
                        Second Opinions
                      </Badge>
                    </div>

                    {reqBroadcasts.length === 0 ? (
                      <div className="p-4 text-center text-xs text-slate-400 italic">
                        Direct admission without specialist consult broadcast.
                      </div>
                    ) : (
                      <div className="space-y-3">
                        {reqBroadcasts.map((b) => (
                          <div key={b.id} className="p-3 bg-white rounded-lg border border-purple-100 text-xs space-y-2">
                            <div className="flex items-center justify-between">
                              <span className="font-bold text-slate-900">{b.targetCluster}</span>
                              <Badge variant={b.status === 'COMPLETED' ? 'success' : 'secondary'} className="text-[10px]">
                                {b.status}
                              </Badge>
                            </div>
                            <div className="grid grid-cols-2 gap-1 text-[11px] text-slate-600">
                              <div>Specialist: <strong>{b.claimedBySpecialistId || '--'}</strong></div>
                              <div>Secondary Tier: <strong className="text-purple-700">{b.secondaryAcuityTier || '--'}</strong></div>
                              <div>Telemetry: <strong>{b.secondaryTelemetry ? 'Endorsed' : 'Standard'}</strong></div>
                              {b.diversionPathway && b.diversionPathway !== 'NONE' && (
                                <div className="text-emerald-700 font-semibold">Diversion: {b.diversionPathway}</div>
                              )}
                            </div>
                            {b.consultNotes && (
                              <div className="pt-1 border-t border-slate-100">
                                <span className="text-[10px] font-medium text-slate-500 block mb-0.5">Impression:</span>
                                <p className="text-[11px] text-slate-800 italic bg-slate-50 p-2 rounded">
                                  "{b.consultNotes}"
                                </p>
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                <DialogFooter className="pt-2">
                  <Button variant="outline" onClick={() => setComparisonRequest(null)}>
                    Close Comparison
                  </Button>
                </DialogFooter>
              </div>
            );
          })()}
        </DialogContent>
      </Dialog>
    </div>
  );
}
