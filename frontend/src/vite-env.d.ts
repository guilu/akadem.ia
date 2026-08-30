/// <reference types="vite/client" />

/**
 * Commit corto del build, inyectado por `vite.config.ts`.
 *
 * Cadena vacía cuando el build no pudo averiguarlo — dentro del contenedor
 * `.git` está en el `.dockerignore` y la imagen de node no trae git, así que
 * ahí llega por `VITE_BUILD_SHA` o no llega.
 */
declare const __BUILD_SHA__: string;
