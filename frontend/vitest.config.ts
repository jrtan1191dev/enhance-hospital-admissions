import { defineConfig, mergeConfig } from 'vitest/config';
import viteConfig from './vite.config.ts';

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      globals: true,
      environment: 'happy-dom',
      setupFiles: ['./src/test/setup.ts'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'json', 'html'],
        include: [
          'src/lib/**/*.{ts,tsx}',
          'src/services/**/*.{ts,tsx}',
          'src/components/**/*.{ts,tsx}',
          'src/routes/patient.tsx',
        ],
        exclude: [
          'src/main.tsx',
          'src/types/**',
          'src/**/*.d.ts',
          'src/test/**',
        ],
        thresholds: {
          lines: 90,
          functions: 90,
          branches: 90,
          statements: 90,
        },
      },
    },
  })
);
