import { isAppHost } from "../appDomains";

/**
 * Hoy los dominios que sirven la aplicación son prestados, así que estar en uno
 * de ellos ES estar en preproducción, y la comprobación es la misma que la de
 * `isAppHost`.
 *
 * Sigue siendo una función aparte a propósito: el día que akadem.ia tenga
 * dominio propio, ese dominio entra en `APP_DOMAINS` —la API se resuelve
 * igual— pero NO debe encender el aviso. Ese día los dos conceptos se separan,
 * y tener ya dos nombres distintos es lo que permite cambiar uno sin tocar el
 * otro.
 */
export function isPreproHost(hostname: string): boolean {
  return isAppHost(hostname);
}
