/// <reference types="vitest" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 3000,
    allowedHosts: ['akademia.diegobarrioh.dev'],
    // Dev proxy: mirrors the nginx rules so the OAuth2 flow works without Docker.
    // The /api prefix covers both REST calls and OAuth2 endpoints.
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/__tests__/setup.ts'
  }
})
