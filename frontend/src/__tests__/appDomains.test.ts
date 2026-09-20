import { describe, expect, it } from "vitest";
import { isAppHost } from "../appDomains";

describe("isAppHost", () => {
  it("reconoce los dominios exactos", () => {
    expect(isAppHost("diegobarrioh.dev")).toBe(true);
    expect(isAppHost("backendtothefuture.com")).toBe(true);
  });

  it("reconoce los subdominios de cada aplicación", () => {
    expect(isAppHost("akademia.backendtothefuture.com")).toBe(true);
    expect(isAppHost("forma.backendtothefuture.com")).toBe(true);
    expect(isAppHost("tokenmeter.backendtothefuture.com")).toBe(true);
    expect(isAppHost("akademia.diegobarrioh.dev")).toBe(true);
  });

  it("no reconoce localhost ni la IP de la red local", () => {
    expect(isAppHost("localhost")).toBe(false);
    expect(isAppHost("127.0.0.1")).toBe(false);
    expect(isAppHost("192.168.1.175")).toBe(false);
  });

  /*
   * El motivo de que este módulo exista. `endsWith` daba por bueno un dominio
   * ajeno que termina igual que el nuestro, y la comprobación duplicada en
   * `api.ts` lo arrastraba.
   */
  it("no se deja engañar por un dominio que solo termina igual", () => {
    expect(isAppHost("notdiegobarrioh.dev")).toBe(false);
    expect(isAppHost("evil-backendtothefuture.com")).toBe(false);
  });

  it("no reconoce un dominio de producción ajeno a la lista", () => {
    expect(isAppHost("akademia.com")).toBe(false);
  });
});
