import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * Merges and resolves Tailwind CSS class names with clsx conditional logic.
 *
 * @param inputs - Class values, objects, or arrays to conditionally combine.
 * @returns Deduplicated and merged Tailwind class string.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
