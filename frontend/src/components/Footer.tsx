export default function Footer() {
  const year = new Date().getFullYear();

  return (
    <footer className="mt-16 border-t border-secondary/20 bg-bg/60">
      <div className="mx-auto flex max-w-7xl flex-col gap-4 px-5 py-8 text-sm text-text/65 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-col gap-1">
          <span>
            &copy; {year} <span className="font-medium text-text">Akadem.ia</span> · by diegobarrioh
          </span>
          <span className="text-xs text-text/45">
            Prepara tus oposiciones con exámenes, flashcards y quizzes generados por IA.
          </span>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs uppercase tracking-widest text-text/45">Apoyar</span>

          <a
            aria-label="Sponsor Akadem.ia on GitHub"
            className="inline-flex items-center gap-1.5 rounded-xl border border-pink-500/30 bg-pink-500/10 px-3 py-1.5 text-xs font-semibold text-pink-400 transition-colors hover:bg-pink-500/20"
            href="https://github.com/sponsors/guilu"
            rel="noopener noreferrer"
            target="_blank"
          >
            <svg
              aria-hidden="true"
              fill="currentColor"
              height="14"
              viewBox="0 0 16 16"
              width="14"
            >
              <path d="M8 1.314C12.438-3.248 23.534 4.735 8 15-7.534 4.736 3.562-3.248 8 1.314z" />
            </svg>
            Sponsor
          </a>

          <a
            aria-label="Buy me a coffee"
            className="inline-flex items-center gap-1.5 rounded-xl border border-amber-500/30 bg-amber-500/10 px-3 py-1.5 text-xs font-semibold text-amber-400 transition-colors hover:bg-amber-500/20"
            href="https://buymeacoffee.com/diegobarrioh"
            rel="noopener noreferrer"
            target="_blank"
          >
            <svg
              aria-hidden="true"
              fill="none"
              height="14"
              stroke="currentColor"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              viewBox="0 0 24 24"
              width="14"
            >
              <path d="M18 8h1a4 4 0 0 1 0 8h-1" />
              <path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z" />
              <line x1="6" x2="6" y1="1" y2="4" />
              <line x1="10" x2="10" y1="1" y2="4" />
              <line x1="14" x2="14" y1="1" y2="4" />
            </svg>
            Buy me a coffee
          </a>

          <a
            aria-label="Akadem.ia on GitHub"
            className="inline-flex items-center gap-1.5 rounded-xl border border-text/15 bg-text/5 px-3 py-1.5 text-xs font-semibold text-text/70 transition-colors hover:bg-text/10"
            href="https://github.com/guilu/akadem.ia"
            rel="noopener noreferrer"
            target="_blank"
          >
            <svg
              aria-hidden="true"
              fill="currentColor"
              height="14"
              viewBox="0 0 16 16"
              width="14"
            >
              <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0 0 16 8c0-4.42-3.58-8-8-8z" />
            </svg>
            GitHub
          </a>
        </div>
      </div>
    </footer>
  );
}
