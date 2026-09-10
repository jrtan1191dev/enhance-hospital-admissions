import { describe, it, expect } from 'vitest';
import { cn } from './utils';

describe('cn utility', () => {
  it('merges class names correctly', () => {
    expect(cn('class-a', 'class-b')).toBe('class-a class-b');
  });

  it('handles conditional classes and falsy values', () => {
    const isHidden = false;
    expect(cn('class-a', isHidden && 'class-b', null, undefined, 'class-c')).toBe('class-a class-c');
  });

  it('resolves conflicting tailwind classes with twMerge', () => {
    expect(cn('px-2 py-1', 'px-4')).toBe('py-1 px-4');
    expect(cn('text-red-500', 'text-blue-500')).toBe('text-blue-500');
  });

  it('handles arrays and nested objects', () => {
    expect(cn(['foo', 'bar'], { baz: true, qux: false })).toBe('foo bar baz');
  });
});
