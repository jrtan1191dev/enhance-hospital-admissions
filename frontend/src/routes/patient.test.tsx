import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PatientRoute } from './patient';

const mockPatients = [
  { id: 'p101', name: 'Tan Ah Seng', queueToken: 'TOKEN-P101' },
  { id: 'p102', name: 'Siti Binte Ahmad', queueToken: 'TOKEN-P102' },
];

const mockTrackerData: any = {
  patientName: 'Tan Ah Seng',
  admissionStatus: 'BED_ALLOCATED',
  estimatedWaitMinutes: 45,
  queuePosition: 2,
  assignedBedNumber: '4A-12',
  assignedWardName: 'Ward 4A Cardiology',
  assignedLevel: 4,
  coPayEstimate: 'Estimated MediShield Life co-payment: $350',
  careGuidance: 'Please remain hydrated and inform nursing staff if discomfort increases.',
  delayReason: 'Housekeeping turnaround delay',
  delayContactHotline: '+65 6321 4310',
  diversionRecommended: true,
  diversionFacilityName: 'Outram Community Hospital',
};

let mockIsLoading = false;
let mockError: any = null;
const mockSimulateMutate = vi.fn();
const mockRecordActionMutate = vi.fn();

vi.mock('../services/queries', () => ({
  patientQueries: {
    availablePatients: () => ({
      queryKey: ['patient', 'available'],
      queryFn: () => Promise.resolve(mockPatients),
    }),
    track: (token: string) => ({
      queryKey: ['patient', 'track', token],
      queryFn: () => (mockError ? Promise.reject(mockError) : Promise.resolve(mockTrackerData)),
    }),
  },
  useSimulatePeriodicUpdate: (onSuccess?: any) => ({
    mutate: () => {
      mockSimulateMutate();
      onSuccess?.({ dispatchedCount: 1, message: 'Broadcast dispatched' });
    },
    isPending: false,
  }),
  useRecordPatientAction: () => ({
    mutate: mockRecordActionMutate,
    isPending: false,
  }),
}));

function renderPatientRoute() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  queryClient.setQueryData(['patient', 'available'], mockPatients);
  if (!mockIsLoading && !mockError && mockTrackerData) {
    queryClient.setQueryData(['patient', 'track', 'TOKEN-P101'], mockTrackerData);
    queryClient.setQueryData(['patient', 'track', 'TOKEN-P102'], {
      ...mockTrackerData,
      patientName: 'Siti Binte Ahmad',
      admissionStatus: 'BED_REQUESTED',
    });
  }

  return render(
    <QueryClientProvider client={queryClient}>
      <PatientRoute />
    </QueryClientProvider>
  );
}

describe('PatientRoute component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockIsLoading = false;
    mockError = null;
  });

  it('renders patient milestone tracker view with full data', () => {
    renderPatientRoute();

    expect(screen.getByText('Patient & Family Public Milestone Tracker')).toBeInTheDocument();
    expect(screen.getByText('Tan Ah Seng')).toBeInTheDocument();
    expect(screen.getByText('Bed 4A-12')).toBeInTheDocument();
    expect(screen.getByText(/Ward 4A Cardiology/)).toBeInTheDocument();
    expect(screen.getByText('45')).toBeInTheDocument();
    expect(screen.getByText(/pax/)).toBeInTheDocument();
    expect(screen.getByText(/Housekeeping turnaround delay/)).toBeInTheDocument();
    expect(screen.getByText(/Estimated MediShield Life co-payment/)).toBeInTheDocument();
    expect(screen.getByText(/Please remain hydrated/)).toBeInTheDocument();
  });

  it('handles simulator token switching', () => {
    renderPatientRoute();
    const select = screen.getByRole('combobox');
    expect(select).toHaveValue('TOKEN-P101');

    fireEvent.change(select, { target: { value: 'TOKEN-P102' } });
    expect(select).toHaveValue('TOKEN-P102');
  });

  it('handles simulate update and hotline buttons', () => {
    renderPatientRoute();
    const simBtn = screen.getByRole('button', { name: /Simulate Periodic Update/i });
    fireEvent.click(simBtn);
    expect(mockSimulateMutate).toHaveBeenCalled();

    const mswBtn = screen.getByRole('button', { name: /Call MSW/i });
    fireEvent.click(mswBtn);
    expect(mockRecordActionMutate).toHaveBeenCalledWith({
      token: 'TOKEN-P101',
      actionType: 'MSW_CALL',
    });

    const financeBtn = screen.getByRole('button', { name: /Call Finance/i });
    fireEvent.click(financeBtn);
    expect(mockRecordActionMutate).toHaveBeenCalledWith({
      token: 'TOKEN-P101',
      actionType: 'FINANCE_CALL',
    });
  });

  it('renders different admission statuses properly', () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(['patient', 'available'], mockPatients);
    queryClient.setQueryData(['patient', 'track', 'TOKEN-P101'], {
      ...mockTrackerData,
      admissionStatus: 'ADMITTED_INPATIENT',
      assignedBedNumber: undefined,
      coPayEstimate: undefined,
      careGuidance: undefined,
      delayReason: undefined,
    });

    render(
      <QueryClientProvider client={queryClient}>
        <PatientRoute />
      </QueryClientProvider>
    );

    expect(screen.getAllByText('Admitted to Inpatient Ward Bed').length).toBeGreaterThanOrEqual(1);
  });

  it('handles ASSESSMENT_PENDING, DISCHARGED, and UNKNOWN statuses', () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(['patient', 'available'], mockPatients);
    queryClient.setQueryData(['patient', 'track', 'TOKEN-P101'], {
      ...mockTrackerData,
      admissionStatus: 'ASSESSMENT_PENDING',
      delayReason: undefined,
    });

    const { rerender } = render(
      <QueryClientProvider client={queryClient}>
        <PatientRoute />
      </QueryClientProvider>
    );
    expect(screen.getAllByText('ED Clinical Assessment in Progress').length).toBeGreaterThanOrEqual(1);

    queryClient.setQueryData(['patient', 'track', 'TOKEN-P101'], {
      ...mockTrackerData,
      admissionStatus: 'DISCHARGED',
    });
    rerender(
      <QueryClientProvider client={queryClient}>
        <PatientRoute />
      </QueryClientProvider>
    );
    expect(screen.getByText('Patient Discharged')).toBeInTheDocument();

    queryClient.setQueryData(['patient', 'track', 'TOKEN-P101'], {
      ...mockTrackerData,
      admissionStatus: 'UNKNOWN_STATUS',
    });
    rerender(
      <QueryClientProvider client={queryClient}>
        <PatientRoute />
      </QueryClientProvider>
    );
    expect(screen.getByText('Evaluating Patient Journey')).toBeInTheDocument();
  });
});
