import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  useReactTable,
  getCoreRowModel,
  flexRender,
  type ColumnDef,
} from '@tanstack/react-table';
import { bmuQueries, useAllocateBed, useReferSisterHospital } from '../services/queries';
import type { AdmissionRequest, Bed } from '../types/admissions';
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
} from 'lucide-react';

export function BmuRoute() {
  const [selectedRequest, setSelectedRequest] = useState<AdmissionRequest | null>(null);
  const [diversionModalOpen, setDiversionModalOpen] = useState(false);
  const [selectedFacility, setSelectedFacility] = useState('Outram Community Hospital (OCH)');
  const [notification, setNotification] = useState<string | null>(null);

  // Queries via queries.ts queryOptions
  const { data: queue = [], isLoading: queueLoading } = useQuery(bmuQueries.queue());
  const { data: inventory = [], isLoading: inventoryLoading } = useQuery(bmuQueries.inventory());
  const { data: recommendations = [], isLoading: recsLoading } = useQuery(
    bmuQueries.recommendations(selectedRequest?.id)
  );

  // Bed Allocation Mutation Hook
  const allocateMutation = useAllocateBed(() => {
    setNotification('Bed successfully allocated! Bed state transitioned to GREEN (In-Transit).');
    setSelectedRequest(null);
    setTimeout(() => setNotification(null), 5000);
  });

  // Diversion Referral Mutation Hook
  const diversionMutation = useReferSisterHospital(() => {
    setNotification(`Referral initiated to ${selectedFacility} (30-min SLA timer active).`);
    setDiversionModalOpen(false);
    setSelectedRequest(null);
    setTimeout(() => setNotification(null), 5000);
  });

  // Table Columns for Admission Queue (TanStack Table)
  const columns: ColumnDef<AdmissionRequest>[] = [
    {
      accessorKey: 'primaryAcuityTier',
      header: 'Priority Tier',
      cell: ({ row }) => {
        const tier = row.original.primaryAcuityTier;
        const variantMap: Record<string, 'destructive' | 'warning' | 'secondary' | 'success' | 'outline'> = {
          TIER_1_CRITICAL: 'destructive',
          TIER_2_ACUTE_URGENT: 'warning',
          TIER_3_ACUTE_STABLE: 'secondary',
          TIER_4_SUBACUTE_DIVERSION: 'success',
        };
        return (
          <Badge variant={variantMap[tier] || 'outline'} className="text-[10px]">
            {tier}
          </Badge>
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
      header: 'Safety Alignment',
      cell: ({ row }) => (
        <div>
          {row.original.discordant ? (
            <Badge variant="destructive" className="text-[10px] flex items-center gap-1">
              <AlertTriangle className="h-3 w-3" /> Acuity Divergence
            </Badge>
          ) : (
            <Badge variant="secondary" className="text-[10px] text-slate-600 bg-slate-100">
              Aligned
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
          {row.original.patient.telemetryRequired && (
            <span className="block text-[10px] text-amber-700 font-medium">Telemetry Req</span>
          )}
        </div>
      ),
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
        <Button
          size="sm"
          variant={selectedRequest?.id === row.original.id ? 'default' : 'outline'}
          onClick={() => setSelectedRequest(row.original)}
          className="text-xs"
        >
          {selectedRequest?.id === row.original.id ? 'Managing' : 'Select'}
        </Button>
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
              ) : selectedRequest.status !== 'BED_REQUESTED' ? (
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
                  {/* Discordance Alert Banner */}
                  {selectedRequest.discordant && (
                    <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-900 space-y-1">
                      <div className="font-semibold flex items-center gap-1.5">
                        <AlertTriangle className="h-4 w-4 text-red-600" />
                        Clinical Discordance Flagged
                      </div>
                      <p className="text-[11px] text-red-800">
                        Primary ED Attending: <strong>{selectedRequest.primaryAcuityTier}</strong> vs Specialist: <strong>{selectedRequest.secondaryAcuityTier}</strong>.
                        Defaulting to higher acuity reservation under Safety-First policy.
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
                      Class: <strong>{selectedRequest.patient.wardClassPreference}</strong> • Telemetry: <strong>{selectedRequest.patient.telemetryRequired ? 'Yes' : 'No'}</strong> • Fall Risk: <strong>{selectedRequest.patient.fallRiskScore}</strong>
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
                        {recommendations.slice(0, 3).map((rec, idx) => (
                          <div
                            key={rec.bedId}
                            className={`p-3 rounded-lg border text-xs transition-all ${
                              idx === 0
                                ? 'bg-emerald-50/60 border-emerald-300 ring-1 ring-emerald-400'
                                : 'bg-white border-slate-200'
                            }`}
                          >
                            <div className="flex items-center justify-between font-semibold">
                              <div className="flex items-center gap-1.5">
                                <span className="text-slate-900 text-sm">Bed {rec.bedNumber}</span>
                                <Badge variant="outline" className="text-[10px] font-mono">
                                  Ward {rec.wardCode}
                                </Badge>
                                {idx === 0 && (
                                  <Badge variant="success" className="text-[9px] px-1 py-0">
                                    #1 Match
                                  </Badge>
                                )}
                              </div>
                              <span className="text-emerald-700 font-bold text-sm">Score: +{rec.score}</span>
                            </div>

                            <div className="flex flex-wrap gap-1 mt-1.5">
                              {rec.breakdown.map((item, bIdx) => (
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
                              className="w-full mt-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-xs h-8"
                              disabled={allocateMutation.isPending}
                              onClick={() =>
                                allocateMutation.mutate({
                                  admissionRequestId: selectedRequest.id,
                                  bedId: rec.bedId,
                                })
                              }
                            >
                              <CheckCircle2 className="h-3.5 w-3.5 mr-1" />
                              1-Click Allocate Bed {rec.bedNumber}
                            </Button>
                          </div>
                        ))}
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
    </div>
  );
}
