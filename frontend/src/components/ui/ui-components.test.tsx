import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { Badge } from './badge';
import { Button } from './button';
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardAction,
  CardContent,
  CardFooter,
} from './card';
import { Input } from './input';
import {
  Table,
  TableHeader,
  TableBody,
  TableFooter,
  TableRow,
  TableHead,
  TableCell,
  TableCaption,
} from './table';
import { ToastContainer } from './toast';
import { toast, toastStore } from '@/lib/toast';
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogFooter,
  DialogTitle,
  DialogDescription,
  DialogClose,
} from './dialog';

describe('UI Primitives', () => {
  describe('Button & buttonVariants', () => {
    it('renders with default props and handles click', () => {
      const handleClick = vi.fn();
      render(<Button onClick={handleClick}>Click Me</Button>);
      const btn = screen.getByRole('button', { name: 'Click Me' });
      expect(btn).toBeInTheDocument();
      fireEvent.click(btn);
      expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it('supports variants and sizes', () => {
      const { rerender } = render(<Button variant="destructive" size="sm">Destructive</Button>);
      expect(screen.getByRole('button')).toHaveClass('bg-destructive/10');

      rerender(<Button variant="outline" size="lg">Outline</Button>);
      expect(screen.getByRole('button')).toHaveClass('border-border');

      rerender(<Button variant="secondary" size="icon">Sec</Button>);
      rerender(<Button variant="ghost" size="xs">Ghost</Button>);
      rerender(<Button variant="link" size="icon-sm">Link</Button>);
      rerender(<Button size="icon-xs">Icon XS</Button>);
      rerender(<Button size="icon-lg">Icon LG</Button>);
    });
  });

  describe('Badge & badgeVariants', () => {
    it('renders with various variants', () => {
      const { rerender } = render(<Badge>Default Badge</Badge>);
      expect(screen.getByText('Default Badge')).toBeInTheDocument();

      rerender(<Badge variant="secondary">Secondary</Badge>);
      expect(screen.getByText('Secondary')).toHaveClass('bg-secondary');

      rerender(<Badge variant="destructive">Destructive</Badge>);
      rerender(<Badge variant="outline">Outline</Badge>);
      rerender(<Badge variant="ghost">Ghost</Badge>);
      rerender(<Badge variant="link">Link</Badge>);
      rerender(<Badge variant="success">Success</Badge>);
      expect(screen.getByText('Success')).toHaveClass('bg-emerald-100');

      rerender(<Badge variant="warning">Warning</Badge>);
      expect(screen.getByText('Warning')).toHaveClass('bg-amber-100');

      rerender(<Badge variant="purple">Purple</Badge>);
      expect(screen.getByText('Purple')).toHaveClass('bg-purple-100');
    });
  });

  describe('Card components', () => {
    it('renders card and all subcomponents', () => {
      render(
        <Card size="sm" className="custom-card">
          <CardHeader className="custom-header">
            <CardTitle>Card Title</CardTitle>
            <CardDescription>Card Description</CardDescription>
            <CardAction>Action</CardAction>
          </CardHeader>
          <CardContent>Card Content Body</CardContent>
          <CardFooter>Card Footer</CardFooter>
        </Card>
      );

      expect(screen.getByText('Card Title')).toBeInTheDocument();
      expect(screen.getByText('Card Description')).toBeInTheDocument();
      expect(screen.getByText('Action')).toBeInTheDocument();
      expect(screen.getByText('Card Content Body')).toBeInTheDocument();
      expect(screen.getByText('Card Footer')).toBeInTheDocument();
    });
  });

  describe('Input component', () => {
    it('renders input and handles value changes', () => {
      const handleChange = vi.fn();
      render(<Input placeholder="Enter patient ID" onChange={handleChange} />);
      const input = screen.getByPlaceholderText('Enter patient ID');
      fireEvent.change(input, { target: { value: 'P-1234' } });
      expect(handleChange).toHaveBeenCalled();
    });
  });

  describe('Table components', () => {
    it('renders complete table structure', () => {
      render(
        <Table>
          <TableCaption>Admissions Roster</TableCaption>
          <TableHeader>
            <TableRow>
              <TableHead>Patient</TableHead>
              <TableHead>Ward</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell>Tan Ah Seng</TableCell>
              <TableCell>Ward 4A</TableCell>
            </TableRow>
          </TableBody>
          <TableFooter>
            <TableRow>
              <TableCell colSpan={2}>Total: 1</TableCell>
            </TableRow>
          </TableFooter>
        </Table>
      );

      expect(screen.getByText('Admissions Roster')).toBeInTheDocument();
      expect(screen.getByText('Patient')).toBeInTheDocument();
      expect(screen.getByText('Tan Ah Seng')).toBeInTheDocument();
      expect(screen.getByText('Ward 4A')).toBeInTheDocument();
      expect(screen.getByText('Total: 1')).toBeInTheDocument();
    });
  });

  describe('ToastContainer', () => {
    it('renders nothing when there are no toasts', () => {
      const { container } = render(<ToastContainer />);
      expect(container.firstChild).toBeNull();
    });

    it('renders error, success, and info toasts and supports closing', () => {
      render(<ToastContainer />);

      act(() => {
        toast.error('Alert Error', 'Something broke');
      });
      expect(screen.getByText('Alert Error')).toBeInTheDocument();
      expect(screen.getByText('Something broke')).toBeInTheDocument();

      act(() => {
        toast.success('Patient Admitted');
        toast.info('Info Notice');
      });
      expect(screen.getByText('Patient Admitted')).toBeInTheDocument();
      expect(screen.getByText('Info Notice')).toBeInTheDocument();

      // Dismiss via close button
      const closeButtons = screen.getAllByRole('button', { name: 'Close notification' });
      act(() => {
        fireEvent.click(closeButtons[0]);
      });

      // Clean up remaining
      act(() => {
        for (const t of toastStore.getSnapshot()) {
          toastStore.dismiss(t.id);
        }
      });
    });
  });

  describe('Dialog components', () => {
    it('renders open dialog and children', () => {
      render(
        <Dialog open={true}>
          <DialogTrigger render={<button>Open</button>} />
          <DialogContent showCloseButton={true}>
            <DialogHeader>
              <DialogTitle>Confirm Transfer</DialogTitle>
              <DialogDescription>Are you sure you want to proceed?</DialogDescription>
            </DialogHeader>
            <p>Dialog Body</p>
            <DialogClose render={<button>Dismiss</button>} />
            <DialogFooter showCloseButton={true}>
              <button>Confirm</button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      );

      expect(screen.getByText('Confirm Transfer')).toBeInTheDocument();
      expect(screen.getByText('Are you sure you want to proceed?')).toBeInTheDocument();
      expect(screen.getByText('Dialog Body')).toBeInTheDocument();
      expect(screen.getByText('Confirm')).toBeInTheDocument();
      expect(screen.getByText('Dismiss')).toBeInTheDocument();
    });
  });
});
