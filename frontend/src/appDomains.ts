/**
 * Los dominios bajo los que viven hoy las aplicaciones mientras no tienen uno
 * propio.
 *
 * Es la única lista: de aquí cuelgan tanto el aviso de preproducción
 * (`isPreproHost`) como la resolución de `apiBase` en `api.ts`. Antes cada
 * fichero llevaba su propia comprobación y solo uno de los dos estaba bien,
 * así que mudar de dominio arreglaba el aviso y rompía la API en silencio.
 */
const APP_DOMAINS = ["diegobarrioh.dev", "backendtothefuture.com"];

/**
 * Indica si el host es uno de los nuestros: el dominio exacto o un subdominio
 * suyo.
 *
 * Se compara por etiqueta de dominio y no con `endsWith`. `endsWith` daría por
 * bueno `notdiegobarrioh.dev`, que es de otro. La comprobación exige o bien el
 * dominio exacto, o bien que lo que va delante termine en un punto — que es lo
 * que separa un subdominio nuestro de un dominio ajeno que casualmente acaba
 * igual.
 */
export function isAppHost(hostname: string): boolean {
  return APP_DOMAINS.some(
    (domain) => hostname === domain || hostname.endsWith(`.${domain}`)
  );
}
