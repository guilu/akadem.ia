import { isPreproHost } from "./preproHost";

interface PreproRibbonProps {
  /** Commit desplegado. Vacío cuando el build no pudo averiguarlo. */
  readonly sha?: string;
  /** Sólo para pruebas; en producción lo decide el host real. */
  readonly hostname?: string;
}

/**
 * Banda diagonal que avisa de que esto no es producción.
 *
 * Va en la barra superior, a la derecha del logo, y toma los colores
 * invertidos del tema: `bg-text` sobre `text-bg`. Los dos tokens cambian de
 * valor con la clase `dark`, así que la inversión se mantiene en claro y en
 * oscuro sin declarar un color nuevo ni escribir una sola variante `dark:`.
 *
 * El commit y no una versión: `package.json` y el tag de git van sincronizados
 * en este repo, pero el sha dice qué hay desplegado ahora mismo, que es la
 * pregunta que se hace uno mirando una preproducción.
 *
 * El paralelogramo se hace sesgando el contenedor y contra-sesgando el
 * contenido, no con `clip-path`: así el fondo sigue siendo una caja normal que
 * hereda el color del tema.
 */
export default function PreproRibbon({ sha = __BUILD_SHA__, hostname }: PreproRibbonProps) {
  const host = hostname ?? window.location.hostname;
  if (!isPreproHost(host)) return null;

  return (
    <div
      role="status"
      aria-label={sha ? `Entorno de preproducción, versión ${sha}` : "Entorno de preproducción"}
      className="ml-4 flex h-16 select-none items-center justify-center bg-text px-5 text-bg [transform:skewX(-18deg)]"
    >
      <span
        aria-hidden="true"
        className="flex flex-col items-center gap-0.5 [transform:skewX(18deg)]"
      >
        <span className="text-sm font-bold leading-none tracking-widest">PREPRO</span>
        {sha ? (
          <span className="text-xs leading-none tracking-wide opacity-85" data-testid="prepro-sha">
            {sha}
          </span>
        ) : null}
      </span>
    </div>
  );
}
