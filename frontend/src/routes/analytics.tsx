import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { analyticsQueries } from '../services/queries';
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import {
  RotateCcw,
  Clock,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Activity,
  Bed,
  Users,
  Sparkles,
  ArrowRightLeft,
  Calendar,
  Layers,
} from 'lucide-react';

type DatePreset = 'ALL_TIME' | 'TODAY' | 'PAST_7_DAYS' | 'CUSTOM';

interface MetricCardProps {
  title: string;
  value: string | number;
  unit?: string;
  subtitle?: string;
  benchmark?: string;
  status: 'GREEN' | 'AMBER' | 'RED' | 'NEUTRAL';
  icon?: React.ReactNode;
}

function MetricCard({ title, value, unit, subtitle, benchmark, status, icon }: MetricCardProps) {
  const badgeVariant =
    status === 'GREEN' ? 'success' : status === 'AMBER' ? 'warning' : status === 'RED' ? 'destructive' : 'secondary';

  const badgeText =
    status === 'GREEN' ? 'Target Met' : status === 'AMBER' ? 'Warning' : status === 'RED' ? 'Action Required' : 'Info';

  const statusIcon =
    status === 'GREEN' ? (
      <CheckCircle2 className="w-3 h-3 text-emerald-700 shrink-0" aria-hidden="true" />
    ) : status === 'AMBER' ? (
      <AlertTriangle className="w-3 h-3 text-amber-700 shrink-0" aria-hidden="true" />
    ) : status === 'RED' ? (
      <XCircle className="w-3 h-3 text-red-600 shrink-0" aria-hidden="true" />
    ) : null;

  return (
    <Card className="shadow-xs hover:shadow-sm transition-shadow">
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-2">
            {icon && <div className="text-slate-500">{icon}</div>}
            <CardTitle className="text-sm font-semibold text-slate-800">{title}</CardTitle>
          </div>
          <Badge variant={badgeVariant} className="text-[10px] px-1.5 py-0.5 gap-1">
            {statusIcon}
            <span>{badgeText}</span>
          </Badge>
        </div>
        {subtitle && <CardDescription className="text-xs text-slate-500">{subtitle}</CardDescription>}
      </CardHeader>
      <CardContent>
        <div className="flex items-baseline gap-1.5">
          <span className="text-2xl font-bold tracking-tight text-slate-900">{value}</span>
          {unit && <span className="text-xs font-medium text-slate-500">{unit}</span>}
        </div>
        {benchmark && (
          <p className="mt-2 text-[11px] font-medium text-slate-600 flex items-center gap-1">
            <span className="text-slate-400">Benchmark:</span> {benchmark}
          </p>
        )}
      </CardContent>
    </Card>
  );
}

function MetricCardSkeleton() {
  return (
    <Card className="shadow-xs animate-pulse" aria-hidden="true">
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <div className="h-4 w-28 bg-slate-200 rounded" />
          <div className="h-4 w-16 bg-slate-200 rounded-full" />
        </div>
        <div className="h-3 w-36 bg-slate-100 rounded mt-1" />
      </CardHeader>
      <CardContent>
        <div className="h-8 w-20 bg-slate-200 rounded" />
        <div className="h-3 w-28 bg-slate-100 rounded mt-2" />
      </CardContent>
    </Card>
  );
}

export function AnalyticsRoute() {
  const [preset, setPreset] = useState<DatePreset>('ALL_TIME');
  const [customStart, setCustomStart] = useState<string>('');
  const [customEnd, setCustomEnd] = useState<string>('');

  let startDateParam: string | undefined = undefined;
  let endDateParam: string | undefined = undefined;

  const now = new Date();
  if (preset === 'TODAY') {
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
    startDateParam = todayStart.toISOString();
  } else if (preset === 'PAST_7_DAYS') {
    const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
    startDateParam = sevenDaysAgo.toISOString();
  } else if (preset === 'CUSTOM') {
    if (customStart) startDateParam = new Date(customStart).toISOString();
    if (customEnd) endDateParam = new Date(customEnd).toISOString();
  }

  const {
    data: kpi,
    isLoading,
    isFetching,
    isError,
    error,
    dataUpdatedAt,
    refetch,
  } = useQuery(analyticsQueries.kpiSummary(startDateParam, endDateParam));

  const handlePresetSelect = (selected: DatePreset) => {
    setPreset(selected);
  };

  const lastUpdatedText = dataUpdatedAt ? new Date(dataUpdatedAt).toLocaleTimeString() : 'Never';

  // Benchmark evaluations for Epic 1
  const edTurnaroundStatus =
    (kpi?.avgEdTurnaroundMinutes ?? 0) === 0
      ? 'NEUTRAL'
      : (kpi?.avgEdTurnaroundMinutes ?? 0) <= 15.0
      ? 'GREEN'
      : (kpi?.avgEdTurnaroundMinutes ?? 0) <= 25.0
      ? 'AMBER'
      : 'RED';

  const edP95Status =
    (kpi?.edTurnaroundP95Minutes ?? 0) === 0
      ? 'NEUTRAL'
      : (kpi?.edTurnaroundP95Minutes ?? 0) <= 30.0
      ? 'GREEN'
      : (kpi?.edTurnaroundP95Minutes ?? 0) <= 45.0
      ? 'AMBER'
      : 'RED';

  const claimLatencyStatus =
    (kpi?.specialistClaimLatencyAvgMinutes ?? 0) === 0
      ? 'NEUTRAL'
      : (kpi?.specialistClaimLatencyAvgMinutes ?? 0) <= 10.0
      ? 'GREEN'
      : (kpi?.specialistClaimLatencyAvgMinutes ?? 0) <= 20.0
      ? 'AMBER'
      : 'RED';

  const concordanceStatus =
    (kpi?.primarySpecialistConcordanceRatePct ?? 0) === 0
      ? 'NEUTRAL'
      : (kpi?.primarySpecialistConcordanceRatePct ?? 0) >= 85.0
      ? 'GREEN'
      : (kpi?.primarySpecialistConcordanceRatePct ?? 0) >= 70.0
      ? 'AMBER'
      : 'RED';

  // Epic 2 Evaluations
  const bmuAcceptanceStatus =
    (kpi?.bmuSuggestionAcceptanceRatePct ?? 0) === 0
      ? 'NEUTRAL'
      : (kpi?.bmuSuggestionAcceptanceRatePct ?? 0) >= 75.0
      ? 'GREEN'
      : (kpi?.bmuSuggestionAcceptanceRatePct ?? 0) >= 60.0
      ? 'AMBER'
      : 'RED';

  const transferSlaStatus =
    (kpi?.sisterHospitalSlaCompliancePct ?? 0) === 0
      ? 'NEUTRAL'
      : (kpi?.sisterHospitalSlaCompliancePct ?? 0) >= 80.0
      ? 'GREEN'
      : 'AMBER';

  const holdingWardStatus =
    (kpi?.batchHoldingWardAdoptionRatePct ?? 0) === 0
      ? 'NEUTRAL'
      : (kpi?.batchHoldingWardAdoptionRatePct ?? 0) >= 20.0
      ? 'GREEN'
      : 'AMBER';

  // Epic 3 Evaluations
  const trackerAccessStatus =
    (kpi?.patientTrackerAccessRatePct ?? 0) === 0
      ? 'NEUTRAL'
      : (kpi?.patientTrackerAccessRatePct ?? 0) >= 60.0
      ? 'GREEN'
      : 'AMBER';

  const periodicPushStatus =
    (kpi?.twoHourPeriodicUpdateDeliveryPct ?? 0) === 0
      ? 'NEUTRAL'
      : (kpi?.twoHourPeriodicUpdateDeliveryPct ?? 0) >= 90.0
      ? 'GREEN'
      : 'AMBER';

  // Epic 4 Evaluations
  const noonDischargeStatus =
    (kpi?.dischargeBeforeNoonRatePct ?? 0) === 0
      ? 'NEUTRAL'
      : (kpi?.dischargeBeforeNoonRatePct ?? 0) >= 40.0
      ? 'GREEN'
      : 'AMBER';

  const cleaningSlaStatus =
    (kpi?.housekeeping30mSlaCompliancePct ?? 0) === 0
      ? 'NEUTRAL'
      : (kpi?.housekeeping30mSlaCompliancePct ?? 0) >= 85.0
      ? 'GREEN'
      : 'AMBER';

  return (
    <div className="space-y-6">
      {/* Header & Control Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-slate-900 tracking-tight">Hospital Operational KPI & Executive Analytics</h1>
            <Badge variant="outline" className="text-[10px] font-mono text-blue-700 bg-blue-50 border-blue-200">
              5s Polling Active
            </Badge>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Real-time performance monitoring across clinical intake, BMU orchestration, patient experience, and bed turnover.
          </p>
        </div>

        {/* Sync Status & Refresh */}
        <div className="flex items-center gap-3">
          <div className="text-right hidden sm:block">
            <p className="text-[11px] text-slate-400">Last updated</p>
            <p className="text-xs font-mono font-medium text-slate-700">{lastUpdatedText}</p>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            disabled={isFetching}
            className="cursor-pointer gap-1.5"
            aria-label="Refresh metrics"
          >
            <RotateCcw className={`w-3.5 h-3.5 ${isFetching ? 'animate-spin motion-reduce:animate-none' : ''}`} />
            <span>Refresh Metrics</span>
          </Button>
        </div>
      </div>

      {/* Error Alert Banner */}
      {isError && (
        <div
          role="alert"
          className="p-4 rounded-xl bg-red-50 border border-red-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-red-800 text-xs"
        >
          <div className="flex items-center gap-2">
            <XCircle className="w-4 h-4 text-red-600 shrink-0" aria-hidden="true" />
            <span>
              Failed to load operational metrics: {error instanceof Error ? error.message : 'Unable to connect to hospital analytics API'}.
            </span>
          </div>
          <Button
            size="xs"
            variant="outline"
            onClick={() => refetch()}
            className="border-red-300 hover:bg-red-100 text-red-800 self-start sm:self-auto cursor-pointer"
          >
            Retry Connection
          </Button>
        </div>
      )}

      {/* Temporal Filter Controls */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-semibold text-slate-600 flex items-center gap-1.5 mr-1">
            <Calendar className="w-3.5 h-3.5 text-slate-400" /> Reporting Period:
          </span>
          <Button
            size="xs"
            variant={preset === 'ALL_TIME' ? 'default' : 'outline'}
            onClick={() => handlePresetSelect('ALL_TIME')}
          >
            All Time (Demo Mode)
          </Button>
          <Button
            size="xs"
            variant={preset === 'TODAY' ? 'default' : 'outline'}
            onClick={() => handlePresetSelect('TODAY')}
          >
            Today (Last 24h)
          </Button>
          <Button
            size="xs"
            variant={preset === 'PAST_7_DAYS' ? 'default' : 'outline'}
            onClick={() => handlePresetSelect('PAST_7_DAYS')}
          >
            Past 7 Days
          </Button>
          <Button
            size="xs"
            variant={preset === 'CUSTOM' ? 'default' : 'outline'}
            onClick={() => handlePresetSelect('CUSTOM')}
          >
            Custom Range
          </Button>
        </div>

        {preset === 'CUSTOM' && (
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <label htmlFor="custom-start-date" className="text-slate-500 font-medium">From:</label>
            <Input
              id="custom-start-date"
              type="date"
              value={customStart}
              onChange={(e) => setCustomStart(e.target.value)}
              className="h-7 text-xs w-36"
              aria-label="Start date"
            />
            <label htmlFor="custom-end-date" className="text-slate-500 font-medium">To:</label>
            <Input
              id="custom-end-date"
              type="date"
              value={customEnd}
              onChange={(e) => setCustomEnd(e.target.value)}
              className="h-7 text-xs w-36"
              aria-label="End date"
            />
          </div>
        )}
      </div>

      {/* 1. ED Clinical Intake & Specialist Collaboration (Epic 1) */}
      <section
        aria-labelledby="section-epic-1-title"
        className="space-y-3 [content-visibility:auto] [contain-intrinsic-size:auto_300px_auto_350px]"
      >
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-md bg-blue-100 text-blue-700 flex items-center justify-center font-bold text-xs" aria-hidden="true">
            1
          </div>
          <h2 id="section-epic-1-title" className="text-sm font-bold text-slate-900 uppercase tracking-wider">
            ED Clinical Intake & Specialist Collaboration
          </h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4" role="region" aria-busy={isLoading && !kpi}>
          {isLoading && !kpi ? (
            Array.from({ length: 5 }).map((_, i) => <MetricCardSkeleton key={i} />)
          ) : (
            <>
              <MetricCard
                title="Avg ED Turnaround"
                value={kpi?.avgEdTurnaroundMinutes ?? 0}
                unit="mins"
                subtitle="Triage to admission order"
                benchmark="Target: < 15 mins"
                status={edTurnaroundStatus}
                icon={<Clock className="w-4 h-4" />}
              />
              <MetricCard
                title="P95 ED Turnaround"
                value={kpi?.edTurnaroundP95Minutes ?? 0}
                unit="mins"
                subtitle="95th percentile intake latency"
                benchmark="Target: < 30 mins"
                status={edP95Status}
                icon={<Activity className="w-4 h-4" />}
              />
              <MetricCard
                title="Specialist Claim Latency"
                value={kpi?.specialistClaimLatencyAvgMinutes ?? 0}
                unit="mins"
                subtitle="Broadcast to consult claim"
                benchmark="Target: < 10 mins"
                status={claimLatencyStatus}
                icon={<Users className="w-4 h-4" />}
              />
              <MetricCard
                title="Triage Concordance"
                value={kpi?.primarySpecialistConcordanceRatePct ?? 0}
                unit="%"
                subtitle="Primary vs specialist acuity"
                benchmark="Target: ≥ 85%"
                status={concordanceStatus}
                icon={<CheckCircle2 className="w-4 h-4" />}
              />
              <MetricCard
                title="Digital Bed Requests"
                value={kpi?.digitalBedRequestCount ?? 0}
                unit="requests"
                subtitle="100% digital transmission"
                benchmark="Target: 100% phone-free"
                status={(kpi?.digitalBedRequestCount ?? 0) > 0 ? 'GREEN' : 'NEUTRAL'}
                icon={<Sparkles className="w-4 h-4" />}
              />
            </>
          )}
        </div>
      </section>

      {/* 2. BMU Capacity Orchestration & Diversions (Epic 2) */}
      <section
        aria-labelledby="section-epic-2-title"
        className="space-y-3 [content-visibility:auto] [contain-intrinsic-size:auto_300px_auto_350px]"
      >
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-md bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-xs" aria-hidden="true">
            2
          </div>
          <h2 id="section-epic-2-title" className="text-sm font-bold text-slate-900 uppercase tracking-wider">
            BMU Capacity Orchestration & Diversions
          </h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4" role="region" aria-busy={isLoading && !kpi}>
          {isLoading && !kpi ? (
            Array.from({ length: 5 }).map((_, i) => <MetricCardSkeleton key={i} />)
          ) : (
            <>
              <MetricCard
                title="Suggestion Acceptance"
                value={kpi?.bmuSuggestionAcceptanceRatePct ?? 0}
                unit="%"
                subtitle="1-click algorithmic approvals"
                benchmark="Target: ≥ 75%"
                status={bmuAcceptanceStatus}
                icon={<CheckCircle2 className="w-4 h-4" />}
              />
              <MetricCard
                title="Manual Overrides"
                value={kpi?.bmuManualOverrideCount ?? 0}
                unit="events"
                subtitle="Coordinator plan adjustments"
                benchmark="Target: Minimize"
                status={(kpi?.bmuManualOverrideCount ?? 0) > 0 ? 'AMBER' : 'GREEN'}
                icon={<AlertTriangle className="w-4 h-4" />}
              />
              <MetricCard
                title="Total Diversions"
                value={kpi?.totalDiversionCount ?? 0}
                unit={`pax (${kpi?.diversionRatePct ?? 0}%)`}
                subtitle="Community Hosp & MIC@Home"
                benchmark="Diversion target active"
                status={(kpi?.totalDiversionCount ?? 0) > 0 ? 'GREEN' : 'NEUTRAL'}
                icon={<ArrowRightLeft className="w-4 h-4" />}
              />
              <MetricCard
                title="Transfer SLA (30m)"
                value={kpi?.sisterHospitalSlaCompliancePct ?? 0}
                unit="%"
                subtitle="Sister hospital bilateral SLA"
                benchmark="SLA: ≤ 30 mins (≥ 80%)"
                status={transferSlaStatus}
                icon={<Clock className="w-4 h-4" />}
              />
              <MetricCard
                title="Holding Ward Adoption"
                value={kpi?.batchHoldingWardAdoptionRatePct ?? 0}
                unit="%"
                subtitle="Batch holding buffer utilization"
                benchmark="Surge mitigation active"
                status={holdingWardStatus}
                icon={<Layers className="w-4 h-4" />}
              />
            </>
          )}
        </div>

        {/* Override and Diversion Breakdown Cards if present */}
        {kpi?.overrideReasonsBreakdown && Object.keys(kpi.overrideReasonsBreakdown).length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-2">
            <Card className="shadow-xs">
              <CardHeader className="pb-2">
                <CardTitle className="text-xs font-semibold text-slate-800">
                  Override Reasons Breakdown
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-1.5 text-xs">
                {Object.entries(kpi.overrideReasonsBreakdown).map(([reason, count]) => (
                  <div key={reason} className="flex justify-between items-center py-1 border-b border-slate-100 last:border-0">
                    <span className="font-mono text-slate-700">{reason}</span>
                    <Badge variant="secondary" className="font-mono">{count}</Badge>
                  </div>
                ))}
              </CardContent>
            </Card>

            <Card className="shadow-xs">
              <CardHeader className="pb-2">
                <CardTitle className="text-xs font-semibold text-slate-800">
                  Alternative Care Diversion Channels
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-1.5 text-xs">
                {kpi.diversionChannelsBreakdown && Object.entries(kpi.diversionChannelsBreakdown).map(([channel, count]) => (
                  <div key={channel} className="flex justify-between items-center py-1 border-b border-slate-100 last:border-0">
                    <span className="font-mono text-slate-700">{channel}</span>
                    <Badge variant="secondary" className="font-mono">{count}</Badge>
                  </div>
                ))}
              </CardContent>
            </Card>
          </div>
        )}
      </section>

      {/* 3. Patient & Family Milestone Tracking (Epic 3) */}
      <section
        aria-labelledby="section-epic-3-title"
        className="space-y-3 [content-visibility:auto] [contain-intrinsic-size:auto_300px_auto_350px]"
      >
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-md bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-xs" aria-hidden="true">
            3
          </div>
          <h2 id="section-epic-3-title" className="text-sm font-bold text-slate-900 uppercase tracking-wider">
            Patient & Family Milestone Tracking
          </h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4" role="region" aria-busy={isLoading && !kpi}>
          {isLoading && !kpi ? (
            Array.from({ length: 4 }).map((_, i) => <MetricCardSkeleton key={i} />)
          ) : (
            <>
              <MetricCard
                title="Public Tracker Access"
                value={kpi?.patientTrackerAccessRatePct ?? 0}
                unit="%"
                subtitle="Admitted patients accessing token"
                benchmark="Target: ≥ 60%"
                status={trackerAccessStatus}
                icon={<Users className="w-4 h-4" />}
              />
              <MetricCard
                title="Periodic Push Updates"
                value={kpi?.twoHourPeriodicUpdateDeliveryPct ?? 0}
                unit="%"
                subtitle="Patients waiting ≥ 2h receiving updates"
                benchmark="Target: ≥ 90%"
                status={periodicPushStatus}
                icon={<Activity className="w-4 h-4" />}
              />
              <MetricCard
                title="Prolonged Wait Delay Tagging"
                value={kpi?.prolongedWaitCommunicationRatePct ?? 0}
                unit="%"
                subtitle="Delayed patients with reason tags"
                benchmark="Target: 100%"
                status={(kpi?.prolongedWaitCommunicationRatePct ?? 0) >= 80 ? 'GREEN' : 'NEUTRAL'}
                icon={<Clock className="w-4 h-4" />}
              />
              <MetricCard
                title="Caregiver Counseling Rate"
                value={kpi?.caregiverCounselingConnectRatePct ?? 0}
                unit="%"
                subtitle="1-click MSW & finance calls"
                benchmark="Proactive support demand"
                status="NEUTRAL"
                icon={<Users className="w-4 h-4" />}
              />
            </>
          )}
        </div>
      </section>

      {/* 4. Inpatient Discharge Runway & Rapid Turnover (Epic 4) */}
      <section
        aria-labelledby="section-epic-4-title"
        className="space-y-3 [content-visibility:auto] [contain-intrinsic-size:auto_300px_auto_350px]"
      >
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-md bg-amber-100 text-amber-700 flex items-center justify-center font-bold text-xs" aria-hidden="true">
            4
          </div>
          <h2 id="section-epic-4-title" className="text-sm font-bold text-slate-900 uppercase tracking-wider">
            Inpatient Discharge Runway & Rapid Turnover
          </h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4" role="region" aria-busy={isLoading && !kpi}>
          {isLoading && !kpi ? (
            Array.from({ length: 5 }).map((_, i) => <MetricCardSkeleton key={i} />)
          ) : (
            <>
              <MetricCard
                title="Discharge Before Noon"
                value={kpi?.dischargeBeforeNoonRatePct ?? 0}
                unit="%"
                subtitle="Beds vacated before 12:00 PM"
                benchmark="Target: ≥ 40%"
                status={noonDischargeStatus}
                icon={<Bed className="w-4 h-4" />}
              />
              <MetricCard
                title="Advance Runway (48h EDD)"
                value={kpi?.advanceRunwayEstablishmentRatePct ?? 0}
                unit="%"
                subtitle="EDD recorded ≥ 48h prior"
                benchmark="Target: ≥ 70%"
                status={(kpi?.advanceRunwayEstablishmentRatePct ?? 0) >= 70 ? 'GREEN' : 'NEUTRAL'}
                icon={<Layers className="w-4 h-4" />}
              />
              <MetricCard
                title="Bedside Meds Delivery"
                value={kpi?.bedsideMedicationDeliveryAdoptionPct ?? 0}
                unit="%"
                subtitle="Direct bedside dispensing"
                benchmark="Target: ≥ 50%"
                status={(kpi?.bedsideMedicationDeliveryAdoptionPct ?? 0) >= 50 ? 'GREEN' : 'NEUTRAL'}
                icon={<Sparkles className="w-4 h-4" />}
              />
              <MetricCard
                title="Turnover Cleaning Avg"
                value={kpi?.housekeepingTurnoverAvgMinutes ?? 0}
                unit="mins"
                subtitle="Bed vacate to clean sign-off"
                benchmark="Target: ≤ 30 mins"
                status={(kpi?.housekeepingTurnoverAvgMinutes ?? 0) > 0 && (kpi?.housekeepingTurnoverAvgMinutes ?? 0) <= 30 ? 'GREEN' : 'NEUTRAL'}
                icon={<Clock className="w-4 h-4" />}
              />
              <MetricCard
                title="Housekeeping 30m SLA"
                value={kpi?.housekeeping30mSlaCompliancePct ?? 0}
                unit="%"
                subtitle="Turnovers completed within 30m"
                benchmark="Target: ≥ 85%"
                status={cleaningSlaStatus}
                icon={<CheckCircle2 className="w-4 h-4" />}
              />
            </>
          )}
        </div>
      </section>
    </div>
  );
}
