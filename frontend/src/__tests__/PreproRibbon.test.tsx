import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import PreproRibbon from "../components/PreproRibbon";
import { isPreproHost } from "../components/preproHost";

/**
 * `window.location` no es escribible en jsdom, así que cada caso instala un
 * sustituto y el teardown devuelve el real.
 */
const realLocation = window.location;
function atHost(hostname: string) {
  Object.defineProperty(window, "location", {
    value: { ...realLocation, hostname },
    writable: true,
    configurable: true,
  });
}
afterEach(() => {
  Object.defineProperty(window, "location", {
    value: realLocation,
    writable: true,
    configurable: true,
  });
});

describe("isPreproHost", () => {
  it("reconoce los dos dominios y sus subdominios", () => {
    expect(isPreproHost("akademia.diegobarrioh.dev")).toBe(true);
    expect(isPreproHost("diegobarrioh.dev")).toBe(true);
    expect(isPreproHost("tokenmeter.backendtothefuture.com")).toBe(true);
  });

  it("no reconoce localhost ni un dominio de producción", () => {
    expect(isPreproHost("localhost")).toBe(false);
    expect(isPreproHost("akademia.com")).toBe(false);
  });

  it("no se deja engañar por un dominio que solo termina igual", () => {
    expect(isPreproHost("notdiegobarrioh.dev")).toBe(false);
    expect(isPreproHost("evil-backendtothefuture.com")).toBe(false);
  });
});

describe("PreproRibbon", () => {
  it("no pinta nada fuera de preproducción", () => {
    atHost("akademia.com");
    const { container } = render(<PreproRibbon sha="2d84807" />);
    expect(container).toBeEmptyDOMElement();
  });

  it("anuncia PREPRO y el commit desplegado", () => {
    atHost("akademia.diegobarrioh.dev");
    render(<PreproRibbon sha="2d84807" />);
    expect(screen.getByText("PREPRO")).toBeInTheDocument();
    expect(screen.getByText("2d84807")).toBeInTheDocument();
  });

  it("sin sha mantiene el aviso y omite la segunda línea", () => {
    atHost("akademia.diegobarrioh.dev");
    render(<PreproRibbon sha="" />);
    expect(screen.getByText("PREPRO")).toBeInTheDocument();
    expect(screen.queryByTestId("prepro-sha")).not.toBeInTheDocument();
  });
});
