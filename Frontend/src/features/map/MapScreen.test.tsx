import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { MapScreen } from "./MapScreen";
import { loadBundledScenario } from "./scenario";

async function renderMap() {
  const view = render(<MapScreen loadScenario={loadBundledScenario} />);
  await screen.findByRole("group", { name: /Mapa/ });
  return view;
}

function panel() {
  return within(screen.getByRole("complementary", { name: "Província selecionada" }));
}

function mapProvince(name: RegExp) {
  return within(screen.getByRole("group", { name: /Mapa/ })).getByRole("button", { name });
}

describe("MapScreen", () => {
  it("desenha todas as províncias do cenário", async () => {
    await renderMap();

    const provinces = within(screen.getByRole("group", { name: /Mapa/ })).getAllByRole("button");
    expect(provinces).toHaveLength(24);
    expect(panel().getByText(/Selecione uma província/)).toBeInTheDocument();
  });

  it("mostra os dados da província clicada", async () => {
    await renderMap();

    fireEvent.click(mapProvince(/^Petrolina, Pernambuco$/));

    expect(panel().getByRole("heading", { name: "Petrolina" })).toBeInTheDocument();
    expect(panel().getByText("Pernambuco")).toBeInTheDocument();
    expect(panel().getByText("Caatinga")).toBeInTheDocument();
    expect(panel().getByText("Pedra (alta)")).toBeInTheDocument();
    expect(panel().getByText("Neutra (sem partida ativa)")).toBeInTheDocument();
    expect(panel().getByText("0 de 3 ocupados")).toBeInTheDocument();
    expect(mapProvince(/^Petrolina/)).toHaveAttribute("aria-pressed", "true");
  });

  it("dá 4 espaços às capitais do cenário", async () => {
    await renderMap();

    fireEvent.click(mapProvince(/^Teresina/));

    expect(panel().getByText("0 de 4 ocupados")).toBeInTheDocument();
    expect(panel().getByText(/capital no cenário/)).toBeInTheDocument();
  });

  it("destaca os vizinhos no mapa e permite navegar por eles", async () => {
    const { container } = await renderMap();

    fireEvent.click(mapProvince(/^Teresina/));
    const neighbours = panel().getAllByRole("listitem");
    expect(container.querySelectorAll(".province.neighbour")).toHaveLength(neighbours.length);
    expect(container.querySelector('[data-province-id="pi-floriano"]')).toHaveClass("neighbour");

    fireEvent.click(panel().getByRole("button", { name: "Floriano" }));
    expect(panel().getByRole("heading", { name: "Floriano" })).toBeInTheDocument();
  });

  it("permite selecionar pelo teclado no mapa", async () => {
    await renderMap();

    const recife = mapProvince(/^Recife/);
    expect(recife).toHaveAttribute("tabindex", "0");
    fireEvent.keyDown(recife, { key: "Enter" });

    expect(panel().getByRole("heading", { name: "Recife" })).toBeInTheDocument();
  });

  it("oferece a lista de províncias como alternativa ao mapa", async () => {
    await renderMap();

    const list = within(screen.getByText("Lista de províncias").closest("details")!);
    fireEvent.click(within(list.getByRole("region", { name: "Sergipe" })).getByRole("button", { name: "Itabaiana" }));

    expect(panel().getByRole("heading", { name: "Itabaiana" })).toBeInTheDocument();
  });

  it("avisa quando o mapa não carrega", async () => {
    render(<MapScreen loadScenario={() => Promise.reject(new Error("arquivo corrompido"))} />);

    expect(await screen.findByRole("alert")).toHaveTextContent("arquivo corrompido");
  });
});
