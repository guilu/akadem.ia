/**
 * Los dos dominios bajo los que viven hoy las aplicaciones mientras no tienen
 * uno propio. Cuando akadem.ia se mude, el aviso desaparece solo: no hay
 * variable de entorno que recordar ni despliegue especial que hacer.
 */
const PREPRO_DOMAINS = ["diegobarrioh.dev", "backendtothefuture.com"];

/**
 * Se compara por etiqueta de dominio y no con `endsWith`.
 *
 * `endsWith` daría por bueno `notdiegobarrioh.dev`, que es de otro. La
 * comprobación exige o bien el dominio exacto, o bien que lo que va delante
 * termine en un punto — que es lo que separa un subdominio nuestro de un
 * dominio ajeno que casualmente acaba igual.
 */
export function isPreproHost(hostname: string): boolean {
  return PREPRO_DOMAINS.some(
    (domain) => hostname === domain || hostname.endsWith(`.${domain}`)
  );
}
