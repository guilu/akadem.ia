/// <reference types="vitest" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * Commit corto del build, para la banda de preproducción.
 *
 * Se lee del entorno y no se calcula aquí: preguntarle a git obligaría a
 * importar `node:child_process`. La llamada vive en el script de `build`, que
 * es donde un shell es la herramienta natural.
 *
 * Vacío cuando nadie lo ha puesto, y entonces la banda avisa sin línea de
 * versión. Inventar un relleno pondría en pantalla un identificador que no
 * corresponde a ningún commit.
 */
const BUILD_SHA = process.env.VITE_BUILD_SHA ?? '';

export default defineConfig({
  define: {
    __BUILD_SHA__: JSON.stringify(BUILD_SHA),
  },
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
