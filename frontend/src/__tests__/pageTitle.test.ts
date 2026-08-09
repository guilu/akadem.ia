import { describe, it, expect } from 'vitest';
import { SITE_NAME, titleForPath } from '../lib/pageTitle';
import { ROUTES } from '../constants/routes';

describe('titleForPath', () => {
  it('keeps the marketing title on the home page', () => {
    expect(titleForPath(ROUTES.home)).toBe('Akadem.ia — Prepara tus oposiciones con IA');
  });

  it('suffixes the site name on inner pages', () => {
    expect(titleForPath(ROUTES.flashcards)).toBe(`Flashcards · ${SITE_NAME}`);
    expect(titleForPath(ROUTES.settings)).toBe(`Ajustes · ${SITE_NAME}`);
  });

  it('resolves dynamic routes through the same sanitizer analytics uses', () => {
    expect(titleForPath('/descarga/super-secret-token')).toBe(`Descarga de tu temario · ${SITE_NAME}`);
    expect(titleForPath('/exams/attempts/7f3a-91bc')).toBe(`Intento de examen · ${SITE_NAME}`);
    expect(titleForPath('/syllabuses/42/subjects')).toBe(`Temas del temario · ${SITE_NAME}`);
  });

  it('ignores query strings and hash fragments', () => {
    expect(titleForPath('/flashcards/study?unit=3')).toBe(`Estudiar flashcards · ${SITE_NAME}`);
    expect(titleForPath('/settings#tab=perfil')).toBe(`Ajustes · ${SITE_NAME}`);
  });

  it('never leaks a token or OAuth2 code into the title', () => {
    expect(titleForPath('/descarga/eyJhbGciOiJIUzI1NiJ9.secret')).not.toContain('secret');
    expect(titleForPath('/oauth2/callback?code=4/0AX4XfWh-secret')).not.toContain('secret');
  });

  it('falls back to the site name on unknown paths', () => {
    expect(titleForPath('/ruta/que/no/existe')).toBe(SITE_NAME);
  });

  it('has a title for every static route declared in ROUTES', () => {
    // ROUTES is `as const`, so its values are a union of literals and builder
    // functions — widen before narrowing to the static string routes.
    const staticPaths = (Object.values(ROUTES) as unknown[]).filter(
      (r): r is string => typeof r === 'string',
    );
    const missing = staticPaths.filter((p) => titleForPath(p) === SITE_NAME);
    expect(missing).toEqual([]);
  });
});
