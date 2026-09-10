import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { useQuery } from '@tanstack/react-query';
import {
  useReactTable,
  getCoreRowModel,
  flexRender,
  type ColumnDef,
} from '@tanstack/react-table';
import { bmuQueries, specialistQueries, wardQueries, useAllocateBed, useDeallocateBed, useAssignAdmittingCluster, useReferSisterHospital, useRequestReconciliation, useApproveBatchHoldingWard, useApproveCohortSwap, useRecallDiversion, useExtendDiversionSla, useLogTelephoneFollowUp, useAttachDelayTag } from '../services/queries';
import type { AdmissionRequest, Bed, SpecialtyCluster, DelayReasonCode } from '../types/admissions';
import { DELAY_REASON_TALKING_POINTS } from '../types/admissions';
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
  PhoneCall,
  Sliders,
} from 'lucide-react';

export function BmuRoute() {
  const [selectedRequest, setSelectedRequest] = useState<AdmissionRequest | null>(null);
  const [comparisonRequest, setComparisonRequest] = useState<AdmissionRequest | null>(null);
  const [diversionModalOpen, setDiversionModalOpen] = useState(false);
  const [selectedFacility, setSelectedFacility] = useState('Outram Community Hospital (OCH)');
  const [notification, setNotification] = useState<string | null>(null);
  const [overrideModalOpen, setOverrideModalOpen] = useState(false);
  const [pendingOverrideBed, setPendingOverrideBed] = useState<{ bedId: string; bedNumber: string; rank: number; score: number } | null>(null);
  const [overrideReasonCode, setOverrideReasonCode] = useState<string>('GOVERNMENT_SUBSIDY_CLASS_UPGRADE');
  const [followUpModalOpen, setFollowUpModalOpen] = useState(false);
  const [followUpNotes, setFollowUpNotes] = useState('');
  const [delayTagModalOpen, setDelayTagModalOpen] = useState(false);
  const [selectedDelayCode, setSelectedDelayCode] = useState<DelayReasonCode>('HOUSEKEEPING_DELAY');
  const [delayNote, setDelayNote] = useState('');
  const [targetDelayRequest, setTargetDelayRequest] = useState<AdmissionRequest | null>(null);

  // Queries via queries.ts queryOptions
  const { data: queue = [], isLoading: queueLoading } = useQuery(bmuQueries.queue());
  const { data: inventory = [], isLoading: inventoryLoading } = useQuery(bmuQueries.inventory());
  const { data: broadcasts = [] } = useQuery(specialistQueries.broadcasts());
  const { data: recommendations = [], isLoading: recsLoading } = useQuery(
    bmuQueries.recommendations(selectedRequest?.id)
  );
  const { data: batchSuggestions = [] } = useQuery(bmuQueries.batchSuggestions());
  const { data: cohortSwapSuggestions = [] } = useQuery(bmuQueries.cohortSwapSuggestions());
  const { data: capacityForecast } = useQuery(wardQueries.capacityForecast());

  // Batch Holding Ward Mutation Hook
  const approveBatchMutation = useApproveBatchHoldingWard(() => {
    setNotification('Batch holding ward approved! Beds allocated and ward cohort locked.');
    setTimeout(() => setNotification(null), 5000);
  }, (err) => {
    setNotification('Failed to approve batch holding ward: ' + err.message);
    setTimeout(() => setNotification(null), 5000);
  });

  // Dynamic Cohort Swap Mutation Hook
  const approveCohortSwapMutation = useApproveCohortSwap(() => {
    setNotification('Cohort swap approved! Reservation transferred and flex ward liberated.');
    setTimeout(() => setNotification(null), 5000);
  }, (err) => {
    setNotification('Cohort swap rejected (Conflict / ED Departure): ' + err.message);
    setTimeout(() => setNotification(null), 5000);
  });

  // Bed Allocation Mutation Hook
  const allocateMutation = useAllocateBed(() => {
    setNotification('Bed successfully allocated/reallocated! Bed state transitioned to GREEN (In-Transit).');
    setSelectedRequest(null);
    setTimeout(() => setNotification(null), 5000);
  });

  // Bed Deallocation Mutation Hook
  const deallocateMutation = useDeallocateBed(() => {
    setNotification('Bed successfully deallocated! Bed returned to WHITE (EMPTY_CLEANED) and request reset to BED_REQUESTED.');
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

  // Diversion Escalation Mutations
  const recallDiversionMutation = useRecallDiversion(() => {
    setNotification('Diversion recalled! Patient returned to acute admission queue.');
    setSelectedRequest(null);
    setTimeout(() => setNotification(null), 5000);
  });

  const extendSlaMutation = useExtendDiversionSla(() => {
    setNotification('Diversion SLA extended by +15 minutes.');
    setTimeout(() => setNotification(null), 5000);
  });

  const logFollowUpMutation = useLogTelephoneFollowUp(() => {
    setNotification('Telephone follow-up logged in operational delay history.');
    setFollowUpModalOpen(false);
    setFollowUpNotes('');
    setTimeout(() => setNotification(null), 5000);
  });

  const attachDelayTagMutation = useAttachDelayTag(() => {
    setNotification('Operational delay tag successfully attached.');
    setDelayTagModalOpen(false);
    setDelayNote('');
    setTargetDelayRequest(null);
    setTimeout(() => setNotification(null), 5000);
  }, (err) => {
    setNotification('Failed to attach delay tag: ' + err.message);
    setTimeout(() => setNotification(null), 5000);
  });

  // Helper for Acuity Dwell SLA calculations
  const getDwellSlaInfo = (request: AdmissionRequest) => {
    const reqTime = request.requestedAt ? new Date(request.requestedAt).getTime() : new Date(request.createdAt).getTime();
    const dwellMinutes = Math.max(0, Math.floor((Date.now() - reqTime) / 60000));
    
    const effectiveTier = request.effectiveAcuityTier || request.primaryAcuityTier;
    let slaThresholdMinutes = 120; // default Tier 3
    if (effectiveTier === 'TIER_1_CRITICAL') {
      slaThresholdMinutes = 0; // immediate
    } else if (effectiveTier === 'TIER_2_ACUTE_URGENT') {
      slaThresholdMinutes = 60;
    } else if (effectiveTier === 'TIER_3_ACUTE_STABLE') {
      slaThresholdMinutes = 120;
    } else if (effectiveTier === 'TIER_4_SUBACUTE_DIVERSION') {
      slaThresholdMinutes = 180;
    }

    const isBreached = effectiveTier === 'TIER_1_CRITICAL' 
      ? (request.status === 'BED_REQUESTED' && dwellMinutes > 0)
      : dwellMinutes > slaThresholdMinutes;

    return {
      dwellMinutes,
      slaThresholdMinutes,
      isBreached,
      effectiveTier,
    };
  };

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
            {row.original.diversionRecommended && (
              <Badge className="text-[10px] bg-teal-100 text-teal-800 border-teal-300 flex items-center gap-1 font-semibold">
                <CheckCircle2 className="h-3 w-3 text-teal-600" />
                {row.original.diversionPathway === 'HOSPITAL_AT_HOME_MIC' ? 'MIC@Home Endorsed' : 'Subacute Endorsed'}
              </Badge>
            )}
            {row.original.sisterHospitalReferralId && (
              <Badge className="text-[10px] bg-purple-100 text-purple-800 border-purple-300 flex items-center gap-1 font-semibold">
                Referral: {row.original.sisterHospitalReferralId}
              </Badge>
            )}
            {row.original.virtualBedNumber && (
              <Badge className="text-[10px] bg-blue-100 text-blue-800 border-blue-300 flex items-center gap-1 font-semibold">
                Virtual: {row.original.virtualBedNumber}
              </Badge>
            )}
          </div>
          {row.original.clinicalConditionUpdated && (
            <Badge variant="warning" className="text-[10px] bg-amber-100 text-amber-900 border-amber-300 flex items-center gap-1 font-semibold animate-pulse">
              <Sparkles className="h-3 w-3 text-amber-600" /> CLINICAL_CONDITION_UPDATED
            </Badge>
          )}
        </div>
      ),
    },
    {
      id: 'dwellSla',
      header: 'Dwell & SLA',
      cell: ({ row }) => {
        const info = getDwellSlaInfo(row.original);
        return (
          <div className="space-y-1">
            <div className={`text-xs font-mono flex items-center gap-1 ${
              info.isBreached
                ? 'text-rose-700 font-bold'
                : info.dwellMinutes > info.slaThresholdMinutes * 0.75
                ? 'text-amber-700 font-semibold'
                : 'text-slate-600'
            }`}>
              <Clock className={`h-3 w-3 ${info.isBreached ? 'text-rose-600 animate-pulse' : 'text-slate-400'}`} />
              <span>{info.dwellMinutes}m dwell</span>
            </div>
            {info.isBreached ? (
              <Badge variant="destructive" className="text-[9px] uppercase tracking-wider font-mono font-bold animate-pulse">
                SLA Breach ({info.slaThresholdMinutes === 0 ? 'Immediate' : `>${info.slaThresholdMinutes}m`})
              </Badge>
            ) : (
              <span className="text-[10px] text-slate-400 block font-mono">
                SLA: {info.slaThresholdMinutes === 0 ? 'Immediate' : `${info.slaThresholdMinutes}m`}
              </span>
            )}
            {row.original.delayReasonTag && (
              <Badge variant="outline" className="text-[9px] bg-amber-50 text-amber-900 border-amber-300 font-mono block truncate max-w-[130px]" title={row.original.operationalDelayReason || row.original.delayReasonTag}>
                {row.original.delayReasonTag}
              </Badge>
            )}
          </div>
        );
      },
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
          <Button
            size="sm"
            variant="outline"
            title="Tag Operational Delay"
            onClick={(e) => {
              e.stopPropagation();
              setTargetDelayRequest(row.original);
              setSelectedDelayCode('HOUSEKEEPING_DELAY');
              setDelayNote(row.original.operationalDelayReason || '');
              setDelayTagModalOpen(true);
            }}
            className="text-[11px] h-7 px-2 border-amber-200 text-amber-800 hover:bg-amber-50 cursor-pointer"
          >
            Tag Delay
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
          <Link
            to="/bmu/config"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-700 bg-slate-100 hover:bg-slate-200 border border-slate-300 rounded-lg transition-colors cursor-pointer"
          >
            <Sliders className="h-3.5 w-3.5 text-slate-600" />
            Solver Config
          </Link>
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

      {/* Advance Discharge Runway Capacity Forecast (24h to 72h) */}
      <Card className="border-slate-200 bg-white shadow-xs">
        <CardHeader className="pb-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <CardTitle className="text-base flex items-center gap-2">
                <Clock className="h-5 w-5 text-blue-600" />
                Inpatient Discharge Runway & Capacity Forecast (24h to 72h)
              </CardTitle>
              <CardDescription className="text-xs">
                Aggregate capacity projections established during morning clinical ward rounds to anticipate bed vacancies.
              </CardDescription>
            </div>
            <div className="flex items-center gap-2">
              <Badge variant="outline" className="bg-blue-50 text-blue-800 border-blue-200 text-xs">
                Next 24h: <strong>{capacityForecast?.totalNext24Hours ?? 0} Beds</strong>
              </Badge>
              <Badge variant="outline" className="bg-indigo-50 text-indigo-800 border-indigo-200 text-xs">
                Next 48h: <strong>{capacityForecast?.totalNext48Hours ?? 0} Beds</strong>
              </Badge>
              <Badge variant="outline" className="bg-purple-50 text-purple-800 border-purple-200 text-xs">
                Next 72h: <strong>{capacityForecast?.totalNext72Hours ?? 0} Beds</strong>
              </Badge>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {(!capacityForecast?.byWard || capacityForecast.byWard.length === 0) ? (
            <div className="text-xs text-slate-500 py-2 italic text-center">
              No upcoming advance discharges scheduled within the next 72 hours.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
              {/* By Ward Table */}
              <div className="border border-slate-200 rounded-lg overflow-hidden">
                <div className="bg-slate-50 px-3 py-2 font-semibold text-slate-800 border-b border-slate-200">
                  Discharge Projections by Ward
                </div>
                <div className="overflow-x-auto [scrollbar-width:thin]">
                  <table className="w-full text-left min-w-[340px]">
                    <thead className="bg-slate-50/50 text-[11px] text-slate-500 border-b border-slate-100">
                      <tr>
                        <th className="px-3 py-1.5 font-medium">Ward</th>
                        <th className="px-3 py-1.5 font-medium">Specialty</th>
                        <th className="px-2 py-1.5 font-medium text-center">24h</th>
                        <th className="px-2 py-1.5 font-medium text-center">48h</th>
                        <th className="px-2 py-1.5 font-medium text-center">72h</th>
                        <th className="px-3 py-1.5 font-medium text-right">Total</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-mono">
                      {capacityForecast.byWard.map((w) => (
                        <tr key={w.wardCode} className="hover:bg-slate-50/70">
                          <td className="px-3 py-2 font-sans font-semibold text-slate-900">{w.wardCode}</td>
                          <td className="px-3 py-2 font-sans text-slate-600 text-[11px]">{w.cluster || 'General'}</td>
                          <td className="px-2 py-2 text-center text-blue-700 font-bold">{w.next24Hours}</td>
                          <td className="px-2 py-2 text-center text-indigo-700">{w.next48Hours}</td>
                          <td className="px-2 py-2 text-center text-purple-700">{w.next72Hours}</td>
                          <td className="px-3 py-2 text-right font-bold text-slate-900">{w.total}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* By Specialty Cluster Table */}
              <div className="border border-slate-200 rounded-lg overflow-hidden">
                <div className="bg-slate-50 px-3 py-2 font-semibold text-slate-800 border-b border-slate-200">
                  Discharge Projections by Specialty Cluster
                </div>
                <div className="overflow-x-auto [scrollbar-width:thin]">
                  <table className="w-full text-left min-w-[340px]">
                    <thead className="bg-slate-50/50 text-[11px] text-slate-500 border-b border-slate-100">
                      <tr>
                        <th className="px-3 py-1.5 font-medium">Specialty Cluster</th>
                        <th className="px-2 py-1.5 font-medium text-center">24h</th>
                        <th className="px-2 py-1.5 font-medium text-center">48h</th>
                        <th className="px-2 py-1.5 font-medium text-center">72h</th>
                        <th className="px-3 py-1.5 font-medium text-right">Total</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-mono">
                      {capacityForecast.byCluster.map((c) => (
                        <tr key={c.cluster} className="hover:bg-slate-50/70">
                          <td className="px-3 py-2 font-sans font-semibold text-slate-900">{c.cluster.replace('_', ' ')}</td>
                          <td className="px-2 py-2 text-center text-blue-700 font-bold">{c.next24Hours}</td>
                          <td className="px-2 py-2 text-center text-indigo-700">{c.next48Hours}</td>
                          <td className="px-2 py-2 text-center text-purple-700">{c.next72Hours}</td>
                          <td className="px-3 py-2 text-right font-bold text-slate-900">{c.total}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Dynamic Holding Ward Batching Suggestion Banner */}
      {batchSuggestions.length > 0 && (
        <div className="space-y-3">
          {batchSuggestions.map((suggestion) => (
            <Card key={suggestion.suggestionId} className="border-indigo-300 bg-indigo-50/50 shadow-sm">
              <CardContent className="p-4">
                <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                  <div className="space-y-1.5 flex-1">
                    <div className="flex items-center gap-2">
                      <Badge className="bg-indigo-600 hover:bg-indigo-700 text-white text-[11px] font-semibold">
                        Surge Cluster Detected ({suggestion.admissionRequestIds.length} Patients)
                      </Badge>
                      <span className="text-xs font-semibold text-indigo-950">
                        Target Flex Ward: {suggestion.targetWardName}
                      </span>
                    </div>
                    <div className="text-xs text-slate-700 flex flex-wrap items-center gap-3">
                      <span><strong>Ward Class:</strong> {suggestion.commonWardClass}</span>
                      <span>•</span>
                      <span><strong>Cohort Gender:</strong> {suggestion.commonGender}</span>
                      <span>•</span>
                      <span><strong>Infection:</strong> {suggestion.commonInfectionStatus}</span>
                      <span>•</span>
                      <span><strong>Unlocked Capacity:</strong> {suggestion.unlockedCapacityCount} beds</span>
                    </div>
                    <div className="text-xs text-slate-600">
                      <strong>Patients (FIFO Dwell Sliced):</strong> {suggestion.patientNames.join(', ')}
                      {suggestion.unlockedCapacityCount > suggestion.admissionRequestIds.length && (
                        <span className="ml-2 text-indigo-700 font-medium">
                          ({suggestion.unlockedCapacityCount - suggestion.admissionRequestIds.length} residual bed(s) remain clean under cohort lock)
                        </span>
                      )}
                    </div>
                  </div>
                  <Button
                    size="sm"
                    disabled={approveBatchMutation.isPending}
                    onClick={() =>
                      approveBatchMutation.mutate({
                        suggestionId: suggestion.suggestionId,
                        targetWardId: suggestion.targetWardId,
                        admissionRequestIds: suggestion.admissionRequestIds,
                      })
                    }
                    className="bg-indigo-600 hover:bg-indigo-700 text-white text-xs cursor-pointer shadow-xs whitespace-nowrap"
                  >
                    {approveBatchMutation.isPending ? 'Allocating Batch...' : `Approve Batch Holding Ward (${suggestion.admissionRequestIds.length} Beds)`}
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Dynamic Cohort-Swap Re-Optimization Proposals */}
      {cohortSwapSuggestions.length > 0 && (
        <div className="space-y-3">
          {cohortSwapSuggestions.map((suggestion) => (
            <Card key={suggestion.suggestionId} className="border-teal-400 bg-teal-50/50 shadow-sm">
              <CardContent className="p-4">
                <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                  <div className="space-y-2 flex-1">
                    <div className="flex items-center gap-2">
                      <Badge className="bg-teal-700 text-white text-[11px] font-semibold">
                        Dynamic Cohort-Swap Proposal
                      </Badge>
                      <Badge variant="outline" className="bg-white text-teal-800 border-teal-300 text-[11px] font-mono font-semibold">
                        +{suggestion.unlockedCapacityCount} Beds Liberated in {suggestion.currentWardName}
                      </Badge>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs bg-white p-3 rounded-lg border border-teal-200">
                      <div>
                        <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">Current Isolated Bed (Blocks Ward)</span>
                        <div className="font-semibold text-slate-900 mt-0.5">{suggestion.patientName}</div>
                        <div className="text-slate-600 text-[11px]">Bed: <strong>{suggestion.currentBedNumber}</strong> ({suggestion.currentWardName})</div>
                        <div className="text-[11px] text-amber-700 font-medium mt-0.5">Blocks {suggestion.unlockedCapacityCount}-bed flex ward from accepting surge batches</div>
                      </div>

                      <div className="border-t sm:border-t-0 sm:border-l sm:pl-3 border-teal-100">
                        <span className="text-[11px] font-semibold text-teal-700 uppercase tracking-wider block">Proposed Transfer Target (Partially Occupied)</span>
                        <div className="font-semibold text-teal-950 mt-0.5">{suggestion.patientName}</div>
                        <div className="text-teal-800 text-[11px]">Target Bed: <strong>{suggestion.targetBedNumber}</strong> ({suggestion.targetWardName})</div>
                        <div className="text-[11px] text-emerald-700 font-medium mt-0.5">Consolidates partially occupied cohort without conflicts</div>
                      </div>
                    </div>
                  </div>

                  <Button
                    size="sm"
                    disabled={approveCohortSwapMutation.isPending}
                    onClick={() =>
                      approveCohortSwapMutation.mutate({
                        admissionRequestId: suggestion.admissionRequestId,
                        targetBedId: suggestion.targetBedId,
                      })
                    }
                    className="bg-teal-700 hover:bg-teal-800 text-white text-xs cursor-pointer shadow-xs whitespace-nowrap self-stretch sm:self-center"
                  >
                    {approveCohortSwapMutation.isPending ? 'Executing Swap...' : 'Approve Cohort Swap'}
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
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
                <Table className="min-w-[760px]">
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

                  {/* Operational Delay Tag Banner */}
                  {selectedRequest.delayReasonTag && (
                    <div className="p-3 bg-amber-50 border border-amber-300 rounded-lg text-xs space-y-1.5">
                      <div className="flex items-center justify-between font-bold text-amber-950">
                        <span className="flex items-center gap-1.5">
                          <AlertTriangle className="h-4 w-4 text-amber-600" />
                          Operational Delay: {selectedRequest.delayReasonTag}
                        </span>
                        <Badge variant="outline" className="text-[10px] bg-white border-amber-400 text-amber-900 font-mono">
                          Active Delay Tag
                        </Badge>
                      </div>
                      {selectedRequest.operationalDelayReason && (
                        <p className="text-[11px] text-slate-700 bg-white p-2 rounded border border-amber-200">
                          {selectedRequest.operationalDelayReason}
                        </p>
                      )}
                    </div>
                  )}

                  {/* Active Referral SLA Lifecycle & Countdown */}
                  {selectedRequest.sisterHospitalReferralId ? (() => {
                    const dispatchedTime = selectedRequest.referralDispatchedAt ? new Date(selectedRequest.referralDispatchedAt).getTime() : new Date(selectedRequest.createdAt).getTime();
                    const slaMins = selectedRequest.referralSlaMinutes || 30;
                    const elapsedMins = Math.floor((Date.now() - dispatchedTime) / 60000);
                    const remainingMins = slaMins - elapsedMins;
                    const isExpired = remainingMins <= 0;

                    return (
                      <div className={`p-3 rounded-lg border text-xs space-y-2.5 ${
                        isExpired
                          ? 'bg-rose-50 border-rose-300 text-rose-950'
                          : 'bg-purple-50 border-purple-200 text-purple-950'
                      }`}>
                        <div className="flex items-center justify-between font-semibold">
                          <div className="flex items-center gap-1.5">
                            <Clock className={`h-4 w-4 ${isExpired ? 'text-rose-600 animate-pulse' : 'text-purple-600'}`} />
                            <span>{isExpired ? 'Bilateral SLA Expired' : 'Active Diversion Referral'}</span>
                          </div>
                          <Badge className={`text-[10px] font-mono ${
                            isExpired ? 'bg-rose-600 text-white' : 'bg-purple-600 text-white'
                          }`}>
                            {selectedRequest.sisterHospitalReferralId}
                          </Badge>
                        </div>

                        {selectedRequest.virtualBedNumber && (
                          <div className="p-2 bg-white rounded border border-purple-200 font-mono text-purple-900 flex items-center justify-between">
                            <span>MIC@Home Virtual Bed:</span>
                            <Badge className="bg-purple-100 text-purple-800 text-[11px] font-bold">
                              {selectedRequest.virtualBedNumber} (DIVERTED_HAH)
                            </Badge>
                          </div>
                        )}

                        <div className="flex items-center justify-between text-[11px]">
                          <span>Destination: <strong>{selectedRequest.referralFacility || 'Sister Hospital'}</strong></span>
                          <span className={isExpired ? 'text-rose-700 font-bold' : 'text-purple-700 font-medium'}>
                            {isExpired
                              ? `Overdue by ${Math.abs(remainingMins)} mins`
                              : `${remainingMins} mins remaining of ${slaMins}m SLA`}
                          </span>
                        </div>

                        {selectedRequest.operationalDelayReason && (
                          <p className="text-[11px] bg-white p-1.5 rounded border border-purple-100 text-slate-700">
                            <strong>Note:</strong> {selectedRequest.operationalDelayReason}
                          </p>
                        )}

                        {/* Actionable Escalation Triggers upon SLA Expiration */}
                        {isExpired && (
                          <div className="space-y-1.5 pt-1 border-t border-rose-200">
                            <div className="text-[11px] font-bold text-rose-800 flex items-center gap-1">
                              <AlertTriangle className="h-3.5 w-3.5 text-rose-600" />
                              Partner Response Overdue — Take Escalation Action:
                            </div>
                            <div className="grid grid-cols-1 sm:grid-cols-3 gap-1.5">
                              <Button
                                size="sm"
                                disabled={recallDiversionMutation.isPending}
                                onClick={() => recallDiversionMutation.mutate(selectedRequest.id)}
                                className="bg-rose-600 hover:bg-rose-700 text-white text-[10px] h-7 px-2 cursor-pointer"
                              >
                                {recallDiversionMutation.isPending ? 'Recalling...' : 'Recall to Acute'}
                              </Button>
                              <Button
                                size="sm"
                                disabled={extendSlaMutation.isPending}
                                onClick={() => extendSlaMutation.mutate(selectedRequest.id)}
                                className="bg-amber-600 hover:bg-amber-700 text-white text-[10px] h-7 px-2 cursor-pointer"
                              >
                                {extendSlaMutation.isPending ? 'Extending...' : 'Extend +15m'}
                              </Button>
                              <Button
                                size="sm"
                                onClick={() => setFollowUpModalOpen(true)}
                                className="bg-slate-700 hover:bg-slate-800 text-white text-[10px] h-7 px-2 cursor-pointer"
                              >
                                Log Follow-Up
                              </Button>
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })() : selectedRequest.diversionRecommended ? (
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
                        className="w-full bg-teal-600 hover:bg-teal-700 text-white text-xs cursor-pointer shadow-xs"
                        onClick={() => {
                          const facility = selectedRequest.diversionPathway === 'HOSPITAL_AT_HOME_MIC'
                              ? 'Mobile Inpatient Care at Home (MIC@Home)'
                              : 'Outram Community Hospital (OCH)';
                          setSelectedFacility(facility);
                          setDiversionModalOpen(true);
                        }}
                      >
                        <ExternalLink className="h-3.5 w-3.5 mr-1" />
                        Open Digital Referral Packet & Dispatch ({selectedRequest.diversionPathway === 'HOSPITAL_AT_HOME_MIC' ? 'MIC@Home' : 'OCH'})
                      </Button>
                    </div>
                  ) : null}

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

                  {selectedRequest.status === 'BED_ALLOCATED' && selectedRequest.assignedBed && (
                    <div className="p-3 bg-amber-50 rounded-lg border border-amber-200 flex items-center justify-between text-xs">
                      <div>
                        <div className="font-semibold text-amber-900">Tentatively Allocated</div>
                        <div className="text-amber-700 text-[11px]">Bed {selectedRequest.assignedBed.bedNumber} (EMPTY_ASSIGNED)</div>
                      </div>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={deallocateMutation.isPending}
                        className="text-xs text-rose-700 border-rose-300 hover:bg-rose-50"
                        onClick={() => deallocateMutation.mutate(selectedRequest.id)}
                      >
                        Deallocate Bed
                      </Button>
                    </div>
                  )}

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

                              {rec.isSafetyViolated ? (
                                <div className="mt-2.5 p-2 rounded bg-rose-50 border border-rose-200 text-rose-800 text-[11px] flex flex-col gap-1">
                                  <div className="flex items-center gap-1 font-semibold text-rose-700">
                                    <AlertTriangle className="h-3.5 w-3.5" />
                                    Absolute Safety Invariant Violated
                                  </div>
                                  <p className="text-[10px] text-rose-600">
                                    {rec.safetyViolationReason || 'Biological gender or airborne isolation safety violation. Zero override allowed.'}
                                  </p>
                                  <Button
                                    size="sm"
                                    disabled
                                    className="w-full mt-1 bg-slate-200 text-slate-400 cursor-not-allowed text-xs h-7"
                                  >
                                    Allocation Forbidden
                                  </Button>
                                </div>
                              ) : (
                                <Button
                                  size="sm"
                                  disabled={allocateMutation.isPending || isCurrentBed}
                                  className={`w-full mt-2.5 text-white font-medium text-xs h-8 ${
                                    isCurrentBed
                                      ? 'bg-slate-400 cursor-not-allowed'
                                      : rec.isOperationalOverride || idx > 0
                                      ? 'bg-amber-600 hover:bg-amber-700'
                                      : selectedRequest.status === 'BED_ALLOCATED'
                                      ? 'bg-blue-600 hover:bg-blue-700'
                                      : 'bg-emerald-600 hover:bg-emerald-700'
                                  }`}
                                  onClick={() => {
                                    if (rec.isOperationalOverride || idx > 0) {
                                      setPendingOverrideBed({
                                        bedId: rec.bedId,
                                        bedNumber: rec.bedNumber,
                                        rank: idx + 1,
                                        score: rec.score,
                                      });
                                      setOverrideModalOpen(true);
                                    } else {
                                      allocateMutation.mutate({
                                        admissionRequestId: selectedRequest.id,
                                        bedId: rec.bedId,
                                        rank: 1,
                                        score: rec.score,
                                        overrideReason:
                                          selectedRequest.status === 'BED_ALLOCATED'
                                            ? 'DYNAMIC_BED_REALLOCATION'
                                            : undefined,
                                      });
                                    }
                                  }}
                                >
                                  <CheckCircle2 className="h-3.5 w-3.5 mr-1" />
                                  {isCurrentBed
                                    ? `Currently Reserved (Bed ${rec.bedNumber})`
                                    : rec.isOperationalOverride || idx > 0
                                    ? `Override & Allocate Bed ${rec.bedNumber}`
                                    : selectedRequest.status === 'BED_ALLOCATED'
                                    ? `Reallocate to Bed ${rec.bedNumber}`
                                    : `Allocate Bed ${rec.bedNumber}`}
                                </Button>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>

                  {/* Operational Authority: Delay Tagging & Diversion */}
                  <div className="pt-2 border-t border-slate-200 flex gap-2">
                    <Button
                      variant="outline"
                      className="flex-1 text-xs text-amber-800 border-amber-300 hover:bg-amber-50 cursor-pointer"
                      onClick={() => {
                        setTargetDelayRequest(selectedRequest);
                        setSelectedDelayCode('HOUSEKEEPING_DELAY');
                        setDelayNote(selectedRequest.operationalDelayReason || '');
                        setDelayTagModalOpen(true);
                      }}
                    >
                      <AlertTriangle className="h-3.5 w-3.5 mr-1 text-amber-600" />
                      Tag Delay
                    </Button>
                    <Button
                      variant="outline"
                      className="flex-1 text-xs text-purple-700 border-purple-200 hover:bg-purple-50 cursor-pointer"
                      onClick={() => setDiversionModalOpen(true)}
                    >
                      <ExternalLink className="h-3.5 w-3.5 mr-1" />
                      Enact Diversion
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
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <ExternalLink className="h-5 w-5 text-purple-600" />
              Sister Hospital & Diversion Routing
            </DialogTitle>
            <DialogDescription>
              {selectedRequest && (
                <span>
                  Digital Referral Packet for <strong>{selectedRequest.patient.name}</strong> ({selectedRequest.patient.nric})
                </span>
              )}
            </DialogDescription>
          </DialogHeader>

          {selectedRequest && (() => {
            const reqBroadcasts = broadcasts.filter(b => b.admissionRequest?.id === selectedRequest.id);
            const impressions = reqBroadcasts.map(b => b.consultNotes).filter(Boolean);

            return (
              <div className="space-y-3.5 py-2 text-xs">
                {/* Pre-populated Patient Demographics & Vitals */}
                <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg space-y-2">
                  <div className="font-semibold text-slate-800 text-[11px] uppercase tracking-wider">
                    Patient Demographics & Clinical Summary
                  </div>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-slate-600">
                    <div>
                      <span className="text-[10px] text-slate-400 block">NRIC / Token</span>
                      <span className="font-medium text-slate-800">{selectedRequest.patient.nric} ({selectedRequest.patient.queueToken})</span>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-400 block">Age / Gender</span>
                      <span className="font-medium text-slate-800">{selectedRequest.patient.age}y / {selectedRequest.patient.gender}</span>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-400 block">Blood Pressure</span>
                      <span className="font-medium text-slate-800">{selectedRequest.patient.vitalsBp || '120/80'} mmHg</span>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-400 block">Heart Rate / SpO2</span>
                      <span className="font-medium text-slate-800">{selectedRequest.patient.vitalsHr || 75} bpm / {selectedRequest.patient.vitalsSpo2 || 98}%</span>
                    </div>
                  </div>
                  {selectedRequest.patient.suspectedDiagnosis && (
                    <div className="pt-1 border-t border-slate-200">
                      <span className="text-[10px] text-slate-400 block">Suspected Diagnosis</span>
                      <span className="font-medium text-slate-800">{selectedRequest.patient.suspectedDiagnosis}</span>
                    </div>
                  )}
                  {impressions.length > 0 && (
                    <div className="pt-1 border-t border-slate-200">
                      <span className="text-[10px] text-slate-400 block">Consult Impressions & Recommendations</span>
                      <ul className="list-disc pl-4 space-y-0.5 text-slate-700">
                        {impressions.map((imp, idx) => (
                          <li key={idx}>{imp}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>

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
                    onClick={() => {
                      diversionMutation.mutate({
                        admissionRequestId: selectedRequest.id,
                        facility: selectedFacility,
                      });
                      setDiversionModalOpen(false);
                    }}
                  >
                    {diversionMutation.isPending ? 'Dispatching...' : 'Dispatch Referral'}
                  </Button>
                </DialogFooter>
              </div>
            );
          })()}
        </DialogContent>
      </Dialog>

      {/* Log Telephone Follow-Up Dialog */}
      <Dialog open={followUpModalOpen} onOpenChange={setFollowUpModalOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-slate-900">
              <PhoneCall className="h-5 w-5 text-slate-700" />
              Log Telephone Follow-Up
            </DialogTitle>
            <DialogDescription>
              Record communication details with partner facility for request #{selectedRequest?.id.slice(0, 8)}.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3 py-2 text-xs">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Coordinator Communication Notes
              </label>
              <textarea
                rows={4}
                value={followUpNotes}
                onChange={(e) => setFollowUpNotes(e.target.value)}
                placeholder="Spoke with receiving admissions desk coordinator; awaiting bed status update..."
                className="w-full border border-slate-300 rounded-md p-2 text-slate-800"
              />
            </div>
            <DialogFooter className="pt-2">
              <Button variant="outline" onClick={() => setFollowUpModalOpen(false)}>
                Cancel
              </Button>
              <Button
                disabled={logFollowUpMutation.isPending || !followUpNotes.trim()}
                className="bg-slate-800 hover:bg-slate-900 text-white"
                onClick={() => {
                  if (selectedRequest && followUpNotes.trim()) {
                    logFollowUpMutation.mutate({
                      requestId: selectedRequest.id,
                      notes: followUpNotes.trim(),
                    });
                  }
                }}
              >
                {logFollowUpMutation.isPending ? 'Saving...' : 'Save Follow-Up Note'}
              </Button>
            </DialogFooter>
          </div>
        </DialogContent>
      </Dialog>

      {/* Attach Operational Delay Tag Dialog */}
      <Dialog open={delayTagModalOpen} onOpenChange={setDelayTagModalOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-slate-900">
              <AlertTriangle className="h-5 w-5 text-amber-600" />
              Tag Operational Delay
            </DialogTitle>
            <DialogDescription>
              {targetDelayRequest && (
                <span>
                  Attach structured delay reason for <strong>{targetDelayRequest.patient.name}</strong> ({targetDelayRequest.patient.queueToken}).
                </span>
              )}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3 py-2 text-xs">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Delay Reason Code
              </label>
              <select
                value={selectedDelayCode}
                onChange={(e) => {
                  const code = e.target.value as DelayReasonCode;
                  setSelectedDelayCode(code);
                  if (!delayNote) {
                    setDelayNote(DELAY_REASON_TALKING_POINTS[code]);
                  }
                }}
                className="w-full bg-white border border-slate-300 rounded-md px-3 py-2 font-medium"
              >
                <option value="HOUSEKEEPING_DELAY">HOUSEKEEPING_DELAY (Terminal Cleaning / EVS Turnover)</option>
                <option value="BED_SHORTAGE">BED_SHORTAGE (High Census / Awaiting Discharges)</option>
                <option value="SPECIALIZED_ISOLATION_CLEANING">SPECIALIZED_ISOLATION_CLEANING (Negative Pressure / Bio-clean)</option>
                <option value="SURGE_TRAUMA_EVENT">SURGE_TRAUMA_EVENT (Acute Trauma Surge / Bed Mobilization)</option>
              </select>
            </div>

            <div className="p-2.5 bg-amber-50 border border-amber-200 rounded-lg text-amber-900 space-y-1">
              <span className="font-semibold block text-[11px]">Contextual Family Talking Points (Sent to ED):</span>
              <p className="text-[11px] text-amber-950 italic">
                "{DELAY_REASON_TALKING_POINTS[selectedDelayCode]}"
              </p>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Operational Delay Note (Optional Explanatory Details)
              </label>
              <textarea
                rows={3}
                value={delayNote}
                onChange={(e) => setDelayNote(e.target.value)}
                placeholder="e.g. Ward 8A bed undergoing 30-min UV terminal disinfection; expected ready in 15 mins."
                className="w-full border border-slate-300 rounded-md p-2 text-slate-800"
              />
            </div>

            <DialogFooter className="pt-2">
              <Button variant="outline" onClick={() => setDelayTagModalOpen(false)}>
                Cancel
              </Button>
              <Button
                disabled={attachDelayTagMutation.isPending}
                className="bg-amber-600 hover:bg-amber-700 text-white cursor-pointer"
                onClick={() => {
                  if (targetDelayRequest) {
                    attachDelayTagMutation.mutate({
                      requestId: targetDelayRequest.id,
                      data: {
                        delayReasonCode: selectedDelayCode,
                        note: delayNote || DELAY_REASON_TALKING_POINTS[selectedDelayCode],
                      },
                    });
                  }
                }}
              >
                {attachDelayTagMutation.isPending ? 'Tagging...' : 'Attach Delay Tag'}
              </Button>
            </DialogFooter>
          </div>
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

      {/* Structured Override Reason Dialog Modal */}
      <Dialog open={overrideModalOpen} onOpenChange={setOverrideModalOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-amber-700">
              <AlertTriangle className="h-5 w-5 text-amber-600" />
              Operational Constraint Override
            </DialogTitle>
            <DialogDescription>
              Allocating Bed {pendingOverrideBed?.bedNumber} requires a mandatory institutional justification code. Overrides generate an immutable OVERRIDE_ALLOCATION audit record.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3 py-2">
            <label className="text-xs font-semibold text-slate-700">
              Structured Reason Code:
            </label>
            <select
              value={overrideReasonCode}
              onChange={(e) => setOverrideReasonCode(e.target.value)}
              className="w-full text-xs rounded border border-slate-300 p-2 bg-white focus:outline-none focus:ring-2 focus:ring-amber-500"
            >
              <option value="GOVERNMENT_SUBSIDY_CLASS_UPGRADE">
                GOVERNMENT_SUBSIDY_CLASS_UPGRADE (Subsidized Ward Class Upgrade)
              </option>
              <option value="EMERGENCY_PORTABLE_TELEMETRY_DEPLOYED">
                EMERGENCY_PORTABLE_TELEMETRY_DEPLOYED (Deploy Portable Wireless Telemetry)
              </option>
              <option value="ATTENDING_CLINICAL_REQUEST">
                ATTENDING_CLINICAL_REQUEST (Attending Physician Request)
              </option>
              <option value="WARD_STAFFING_LIMITATION">
                WARD_STAFFING_LIMITATION (Nursing / Staffing Limitation)
              </option>
              <option value="FAMILY_PROXIMITY_REQUEST">
                FAMILY_PROXIMITY_REQUEST (Family / Social Consideration)
              </option>
            </select>
          </div>

          <DialogFooter className="flex justify-end gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setOverrideModalOpen(false);
                setPendingOverrideBed(null);
              }}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              className="bg-amber-600 hover:bg-amber-700 text-white"
              disabled={allocateMutation.isPending}
              onClick={() => {
                if (selectedRequest && pendingOverrideBed) {
                  allocateMutation.mutate({
                    admissionRequestId: selectedRequest.id,
                    bedId: pendingOverrideBed.bedId,
                    rank: pendingOverrideBed.rank,
                    score: pendingOverrideBed.score,
                    overrideReason: overrideReasonCode,
                  });
                  setOverrideModalOpen(false);
                  setPendingOverrideBed(null);
                }
              }}
            >
              Confirm Override & Allocate
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
