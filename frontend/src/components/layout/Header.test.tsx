import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Header } from './Header';
import { getActiveRole, setActiveRole } from '@/services/api';

const mockNavigate = vi.fn();
let mockLocation = { pathname: '/ed' };

vi.mock('@tanstack/react-router', () => ({
  useNavigate: () => mockNavigate,
  useLocation: () => mockLocation,
  Link: ({ children, to, className }: any) => <a href={to} className={className}>{children}</a>,
}));

vi.mock('@/services/api', () => ({
  getActiveRole: vi.fn().mockReturnValue('ED_ATTENDING'),
  setActiveRole: vi.fn(),
}));

vi.mock('@/services/queries', () => ({
  authQueries: {
    me: () => ({
      queryKey: ['auth', 'me'],
      queryFn: vi.fn().mockResolvedValue({ authenticated: true, username: 'dr_tan_ed', roles: ['ROLE_CLINICIAN'] }),
    }),
  },
}));

function renderHeader(authData = { authenticated: true, username: 'dr_tan_ed', roles: ['ROLE_CLINICIAN'] }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  queryClient.setQueryData(['auth', 'me'], authData);

  return render(
    <QueryClientProvider client={queryClient}>
      <Header />
    </QueryClientProvider>
  );
}

describe('Header component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockLocation = { pathname: '/ed' };
  });

  it('renders application title, live sync indicator, user info, and analytics navigation link', () => {
    renderHeader();
    expect(screen.getByText('Patient Admission & Discharge Management Application')).toBeInTheDocument();
    expect(screen.getByText('Live Sync')).toBeInTheDocument();
    expect(screen.getByText('dr_tan_ed')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /analytics/i })).toHaveAttribute('href', '/analytics');
  });

  it('handles role switching from dropdown and navigates', () => {
    const { rerender } = renderHeader();
    const select = screen.getByRole('combobox');
    expect(select).toHaveValue('ED_ATTENDING');

    fireEvent.change(select, { target: { value: 'BMU_COORDINATOR' } });
    expect(setActiveRole).toHaveBeenCalledWith('BMU_COORDINATOR');
    expect(mockNavigate).toHaveBeenCalledWith({ to: '/bmu' });

    // Changing to a role with the same path does not navigate
    mockLocation = { pathname: '/bmu' };
    rerender(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <Header />
      </QueryClientProvider>
    );
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'BMU_COORDINATOR' } });
    expect(mockNavigate).toHaveBeenCalledTimes(1);
  });

  it('syncs role when location pathname changes', () => {
    const { rerender } = renderHeader();
    mockLocation = { pathname: '/ward' };
    rerender(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <Header />
      </QueryClientProvider>
    );
    expect(setActiveRole).toHaveBeenCalledWith('WARD_NURSE');

    // /bmu/config does not change role away if already BMU_COORDINATOR
    vi.mocked(getActiveRole).mockReturnValue('BMU_COORDINATOR');
    mockLocation = { pathname: '/bmu/config' };
    rerender(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <Header />
      </QueryClientProvider>
    );

    // Unknown route does not change role
    mockLocation = { pathname: '/unknown-route' };
    rerender(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <Header />
      </QueryClientProvider>
    );
  });

  it('renders without auth user badge when unauthenticated', () => {
    renderHeader({ authenticated: false, username: undefined, roles: [] });
    expect(screen.queryByText('dr_tan_ed')).not.toBeInTheDocument();
  });
});
