import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { FamilyTalkingPoints, BmuOperationNote } from './ed';

describe('FamilyTalkingPoints delay reason formatting', () => {
  it('formats standard delay reason codes to human-readable labels', () => {
    const { rerender } = render(<FamilyTalkingPoints delayReasonTag="HOUSEKEEPING_DELAY" />);
    expect(screen.getByText('Delay: Housekeeping Delay')).toBeInTheDocument();

    rerender(<FamilyTalkingPoints delayReasonTag="BED_SHORTAGE" />);
    expect(screen.getByText('Delay: Bed Shortage')).toBeInTheDocument();

    rerender(<FamilyTalkingPoints delayReasonTag="SPECIALIZED_ISOLATION_CLEANING" />);
    expect(screen.getByText('Delay: Specialized Cleaning')).toBeInTheDocument();

    rerender(<FamilyTalkingPoints delayReasonTag="SURGE_TRAUMA_EVENT" />);
    expect(screen.getByText('Delay: Surge Trauma Event')).toBeInTheDocument();
  });

  it('formats unexpected snake_case codes gracefully', () => {
    render(<FamilyTalkingPoints delayReasonTag="CT_SCAN_BACKLOG" />);
    expect(screen.getByText('Delay: Ct Scan Backlog')).toBeInTheDocument();
  });
});

describe('FamilyTalkingPoints component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders summary trigger with formatted label and chevron indicator', () => {
    render(
      <FamilyTalkingPoints
        delayReasonTag="HOUSEKEEPING_DELAY"
        operationalDelayReason="Cleaning ongoing"
      />
    );

    expect(screen.getByText('Delay: Housekeeping Delay')).toBeInTheDocument();
    expect(screen.getByText('Script')).toBeInTheDocument();
  });

  it('renders scripted talking points inside blockquote and embedded BMU Operation Note', () => {
    render(
      <FamilyTalkingPoints
        delayReasonTag="BED_SHORTAGE"
        operationalDelayReason="Wards congested"
      />
    );

    expect(screen.getByText(/Hospital wards are currently experiencing high census/i)).toBeInTheDocument();
    expect(screen.getByText('BMU Operation Note')).toBeInTheDocument();
    expect(screen.getByText(/Wards congested/i)).toBeInTheDocument();
  });

  it('handles copy button click and displays copied feedback', async () => {
    const writeTextMock = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      value: {
        writeText: writeTextMock,
      },
      configurable: true,
      writable: true,
    });

    render(
      <FamilyTalkingPoints
        delayReasonTag="HOUSEKEEPING_DELAY"
      />
    );

    const copyBtn = screen.getByRole('button', { name: /^Copy/i });
    expect(copyBtn).toHaveTextContent('Copy');

    fireEvent.click(copyBtn);
    expect(writeTextMock).toHaveBeenCalled();
    expect(await screen.findByText('Copied')).toBeInTheDocument();
  });

  it('does not duplicate BMU note if it is identical to talking points', () => {
    const customReason = 'Custom operational note explaining bed delay';
    render(
      <FamilyTalkingPoints
        delayReasonTag="CUSTOM_TAG"
        operationalDelayReason={customReason}
      />
    );

    expect(screen.getByText(`"${customReason}"`)).toBeInTheDocument();
    expect(screen.queryByText('BMU Operation Note')).not.toBeInTheDocument();
  });
});

describe('BmuOperationNote component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders standalone BMU Operation Note with scroll container and copy action', async () => {
    const writeTextMock = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      value: {
        writeText: writeTextMock,
      },
      configurable: true,
      writable: true,
    });

    const longNote =
      'Ward 8A bed undergoing 30-min UV terminal disinfection following bio-hazard clearance; expected ready in 15 mins. EVS supervisor dispatched with air quality monitor for sign-off.';

    render(<BmuOperationNote note={longNote} />);

    expect(screen.getByText('BMU Operation Note')).toBeInTheDocument();
    const noteEl = screen.getByLabelText('BMU operational delay note content');
    expect(noteEl).toBeInTheDocument();
    expect(noteEl).toHaveTextContent(longNote);
    expect(noteEl).toHaveAttribute('tabindex', '0');

    const copyBtn = screen.getByTitle('Copy BMU note to clipboard');
    fireEvent.click(copyBtn);
    expect(writeTextMock).toHaveBeenCalledWith(longNote);
    expect(await screen.findByText('Copied')).toBeInTheDocument();
  });
});
