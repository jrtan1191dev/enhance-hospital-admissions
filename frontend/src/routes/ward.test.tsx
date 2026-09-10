import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { WardRoute } from './ward';

const mockWards: any = [
  {
    id: 'w1',
    wardCode: '8A',
    levelNumber: 8,
    wardClass: 'B2',
    specialty: 'CARDIOLOGY',
    genderCohortLocked: 'MALE',
    infectionLocked: 'NONE',
    beds: [
      {
        id: 'bed-8A01',
        bedNumber: '8A-01',
        status: 'OCCUPIED_TAKEN',
        telemetryCapable: true,
        nearNursingStation: true,
        assignedPatient: {
          id: 'p101',
          name: 'Uncle Seng',
          queueToken: 'TOK-881',
          gender: 'MALE',
          age: 75,
          wardClassPreference: 'B2',
          suspectedDiagnosis: 'Post-PCI cardiac stabilization',
        },
      },
      {
        id: 'bed-8A02',
        bedNumber: '8A-02',
        status: 'EMPTY_PENDING_CLEANING',
        telemetryCapable: false,
        nearNursingStation: false,
      },
    ],
  },
];

const mockRunwayEntries: any = [
  {
    patientId: 'p101',
    patientName: 'Uncle Seng',
    bedNumber: '8A-01',
    wardCode: 'Ward 8A',
    levelNumber: 8,
    estimatedDateOfDischarge: '2026-09-12',
    confidence: 'HIGH',
    runwayStage: 'RUNWAY_D2',
    medicationStatus: 'PACKING_IN_PROGRESS',
    rationale: 'Post-PCI cardiac stabilization complete',
  },
];

const mockTurnoverTasks: any = [
  {
    bedId: 'bed-8A02',
    bedNumber: '8A-02',
    wardCode: 'Ward 8A',
    levelNumber: 8,
    vacatedAt: '2026-09-10T10:00:00',
    cleaningStartedAt: '2026-09-10T10:00:00',
    remainingMinutes: 18,
    slaStatus: 'ON_TRACK',
  },
];

const mockCheckinMutate = vi.fn();
const mockVacateMutate = vi.fn();
const mockCleanMutate = vi.fn();
const mockUpdateEddMutate = vi.fn();
const mockSignoffMutate = vi.fn();
const mockDeliverMedMutate = vi.fn();

vi.mock('../services/queries', () => ({
  bmuQueries: {
    inventory: () => ({
      queryKey: ['bmu', 'inventory'],
      queryFn: () => Promise.resolve(mockWards),
    }),
  },
  wardQueries: {
    runway: () => ({
      queryKey: ['ward', 'runway'],
      queryFn: () => Promise.resolve(mockRunwayEntries),
    }),
    turnoverTasks: () => ({
      queryKey: ['ward', 'turnover-tasks'],
      queryFn: () => Promise.resolve(mockTurnoverTasks),
    }),
  },
  useCheckinPatient: (onSuccess?: any) => ({
    mutate: (bedId: string) => {
      mockCheckinMutate(bedId);
      onSuccess?.();
    },
    isPending: false,
  }),
  useVacatePatient: (onSuccess?: any) => ({
    mutate: (bedId: string) => {
      mockVacateMutate(bedId);
      onSuccess?.();
    },
    isPending: false,
  }),
  useCleanBed: (onSuccess?: any, onError?: any) => ({
    mutate: (bedId: string) => {
      mockCleanMutate(bedId);
      onSuccess?.();
    },
    isPending: false,
  }),
  useUpdateEdd: (onSuccess?: any, onError?: any) => ({
    mutate: (variables: any) => {
      mockUpdateEddMutate(variables);
      onSuccess?.();
    },
    isPending: false,
  }),
  useDischargeSignoff: (onSuccess?: any, onError?: any) => ({
    mutate: (patientId: string) => {
      mockSignoffMutate(patientId);
      onSuccess?.();
    },
    isPending: false,
  }),
  useDeliverMedication: (onSuccess?: any, onError?: any) => ({
    mutate: (patientId: string) => {
      mockDeliverMedMutate(patientId);
      onSuccess?.();
    },
    isPending: false,
  }),
}));

function renderWardRoute() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  queryClient.setQueryData(['bmu', 'inventory'], mockWards);
  queryClient.setQueryData(['ward', 'runway'], mockRunwayEntries);
  queryClient.setQueryData(['ward', 'turnover-tasks'], mockTurnoverTasks);

  return render(
    <QueryClientProvider client={queryClient}>
      <WardRoute />
    </QueryClientProvider>
  );
}

describe('WardRoute component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders ward roster with D-2 Potential Discharge badge and turnover queue', () => {
    renderWardRoute();

    expect(screen.getByText('Inpatient Ward Reception & Rapid Bed Turnover')).toBeInTheDocument();
    expect(screen.getByText(/D-2 Potential Discharge/i)).toBeInTheDocument();
    expect(screen.getByText('Uncle Seng')).toBeInTheDocument();
    expect(screen.getAllByText(/Post-PCI cardiac stabilization/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Housekeeping (EVS) 30-Minute Turnover Queue (1 Beds Pending)')).toBeInTheDocument();
    expect(screen.getByText(/18m left \(On Track\)/i)).toBeInTheDocument();
  });

  it('handles Set/Update EDD modal opening and saving', () => {
    renderWardRoute();

    const updateEddBtn = screen.getByRole('button', { name: /Update EDD \/ Rationale/i });
    fireEvent.click(updateEddBtn);

    expect(screen.getByText('Establish Advance Discharge Runway (EDD)')).toBeInTheDocument();
    const saveBtn = screen.getByRole('button', { name: /Save EDD & Runway Stage/i });
    fireEvent.click(saveBtn);

    expect(mockUpdateEddMutate).toHaveBeenCalledWith(
      expect.objectContaining({
        patientId: 'p101',
      })
    );
  });

  it('handles bedside medication delivery confirmation and bed vacate actions', () => {
    renderWardRoute();

    const deliverBtn = screen.getByRole('button', { name: /Confirm Bedside Delivery/i });
    fireEvent.click(deliverBtn);
    expect(mockDeliverMedMutate).toHaveBeenCalledWith('p101');

    const vacateBtn = screen.getByRole('button', { name: /Patient Vacated Bed/i });
    fireEvent.click(vacateBtn);
    expect(mockVacateMutate).toHaveBeenCalledWith('bed-8A01');
  });

  it('handles housekeeping sign-off clean action', () => {
    renderWardRoute();

    const cleanBtn = screen.getByRole('button', { name: /Sign-Off Clean/i });
    fireEvent.click(cleanBtn);
    expect(mockCleanMutate).toHaveBeenCalledWith('bed-8A02');
  });
});
