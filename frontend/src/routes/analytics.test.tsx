import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AnalyticsRoute } from './analytics';
import { api } from '../services/api';
import type { HospitalKpiSummaryDto } from '../types/admissions';

vi.mock('../services/api', () => ({
  api: {
    getKpiSummary: vi.fn(),
  },
}));

const mockSummary: HospitalKpiSummaryDto = {
  periodStart: 'ALL_TIME',
  periodEnd: 'ALL_TIME',
  avgEdTurnaroundMinutes: 12.4,
  edTurnaroundP95Minutes: 24.8,
  specialistClaimLatencyAvgMinutes: 7.5,
  primarySpecialistConcordanceRatePct: 88.0,
  digitalBedRequestCount: 35,
  bmuSuggestionAcceptanceRatePct: 82.5,
  bmuManualOverrideCount: 4,
  totalDiversionCount: 6,
  diversionRatePct: 17.1,
  sisterHospitalSlaCompliancePct: 92.0,
  batchHoldingWardAdoptionRatePct: 25.0,
  overrideReasonsBreakdown: {
    NON_TOP_RANK_SELECTION: 3,
    GOVERNMENT_SUBSIDY_CLASS_UPGRADE: 1,
  },
  diversionChannelsBreakdown: {
    COMMUNITY_HOSPITAL: 4,
    MIC_AT_HOME: 2,
  },
  patientTrackerAccessRatePct: 65.0,
  twoHourPeriodicUpdateDeliveryPct: 95.0,
  prolongedWaitCommunicationRatePct: 85.0,
  caregiverCounselingConnectRatePct: 40.0,
  dischargeBeforeNoonRatePct: 45.0,
  advanceRunwayEstablishmentRatePct: 72.0,
  bedsideMedicationDeliveryAdoptionPct: 58.0,
  housekeepingTurnoverAvgMinutes: 22.0,
  housekeeping30mSlaCompliancePct: 91.0,
};

function renderAnalyticsRoute() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AnalyticsRoute />
    </QueryClientProvider>
  );
}

describe('AnalyticsRoute component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(api.getKpiSummary).mockResolvedValue(mockSummary);
  });

  it('renders dashboard title, active polling badge, and refresh button', async () => {
    renderAnalyticsRoute();

    expect(await screen.findByText('Hospital Operational KPI & Executive Analytics')).toBeInTheDocument();
    expect(screen.getByText('5s Polling Active')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /refresh metrics/i })).toBeInTheDocument();
  });

  it('renders reporting period preset buttons', async () => {
    renderAnalyticsRoute();

    expect(await screen.findByRole('button', { name: /all time \(demo mode\)/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /today \(last 24h\)/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /past 7 days/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /custom range/i })).toBeInTheDocument();
  });

  it('renders Epic 1 clinical intake stat cards with formatted values and benchmarks', async () => {
    renderAnalyticsRoute();

    expect(await screen.findByText('ED Clinical Intake & Specialist Collaboration')).toBeInTheDocument();
    expect(await screen.findByText('12.4')).toBeInTheDocument();
    expect(screen.getByText('Avg ED Turnaround')).toBeInTheDocument();
    expect(screen.getByText('P95 ED Turnaround')).toBeInTheDocument();
    expect(screen.getByText('24.8')).toBeInTheDocument();
    expect(screen.getByText('Specialist Claim Latency')).toBeInTheDocument();
    expect(screen.getByText('7.5')).toBeInTheDocument();
    expect(screen.getByText('Triage Concordance')).toBeInTheDocument();
    expect(screen.getByText('88')).toBeInTheDocument();
    expect(screen.getByText('Digital Bed Requests')).toBeInTheDocument();
    expect(screen.getByText('35')).toBeInTheDocument();
  });

  it('clicking custom range reveals date pickers', async () => {
    renderAnalyticsRoute();

    const customBtn = await screen.findByRole('button', { name: /custom range/i });
    fireEvent.click(customBtn);

    expect(screen.getByText('From:')).toBeInTheDocument();
    expect(screen.getByText('To:')).toBeInTheDocument();
  });

  it('clicking refresh metrics triggers refetch', async () => {
    renderAnalyticsRoute();

    const refreshBtn = await screen.findByRole('button', { name: /refresh metrics/i });
    fireEvent.click(refreshBtn);

    expect(api.getKpiSummary).toHaveBeenCalled();
  });

  it('renders Epic 2 BMU capacity section with breakdown distributions', async () => {
    renderAnalyticsRoute();

    expect(await screen.findByText('BMU Capacity Orchestration & Diversions')).toBeInTheDocument();
    expect(await screen.findByText('82.5')).toBeInTheDocument();
    expect(screen.getByText('Suggestion Acceptance')).toBeInTheDocument();
    expect(screen.getByText('Manual Overrides')).toBeInTheDocument();
    expect(screen.getAllByText('4').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Total Diversions')).toBeInTheDocument();
    expect(screen.getByText('Transfer SLA (30m)')).toBeInTheDocument();
    expect(screen.getByText('92')).toBeInTheDocument();

    // Breakdown cards
    expect(screen.getByText('Override Reasons Breakdown')).toBeInTheDocument();
    expect(screen.getByText('NON_TOP_RANK_SELECTION')).toBeInTheDocument();
    expect(screen.getByText('GOVERNMENT_SUBSIDY_CLASS_UPGRADE')).toBeInTheDocument();
    expect(screen.getByText('Alternative Care Diversion Channels')).toBeInTheDocument();
    expect(screen.getByText('COMMUNITY_HOSPITAL')).toBeInTheDocument();
    expect(screen.getByText('MIC_AT_HOME')).toBeInTheDocument();
  });

  it('renders Epic 3 patient milestone and counseling section', async () => {
    renderAnalyticsRoute();

    expect(await screen.findByText('Patient & Family Milestone Tracking')).toBeInTheDocument();
    expect(await screen.findByText('65')).toBeInTheDocument();
    expect(screen.getByText('Public Tracker Access')).toBeInTheDocument();
    expect(screen.getByText('Periodic Push Updates')).toBeInTheDocument();
    expect(screen.getByText('95')).toBeInTheDocument();
    expect(screen.getByText('Prolonged Wait Delay Tagging')).toBeInTheDocument();
    expect(screen.getByText('85')).toBeInTheDocument();
    expect(screen.getByText('Caregiver Counseling Rate')).toBeInTheDocument();
    expect(screen.getByText('40')).toBeInTheDocument();
  });

  it('renders Epic 4 discharge runway and turnover section', async () => {
    renderAnalyticsRoute();

    expect(await screen.findByText('Inpatient Discharge Runway & Rapid Turnover')).toBeInTheDocument();
    expect(screen.getByText('Discharge Before Noon')).toBeInTheDocument();
    expect(await screen.findByText('45')).toBeInTheDocument();
    expect(screen.getByText('Advance Runway (48h EDD)')).toBeInTheDocument();
    expect(screen.getByText('72')).toBeInTheDocument();
    expect(screen.getByText('Bedside Meds Delivery')).toBeInTheDocument();
    expect(screen.getByText('58')).toBeInTheDocument();
    expect(screen.getByText('Turnover Cleaning Avg')).toBeInTheDocument();
    expect(screen.getByText('22')).toBeInTheDocument();
    expect(screen.getByText('Housekeeping 30m SLA')).toBeInTheDocument();
    expect(screen.getByText('91')).toBeInTheDocument();
  });
});
