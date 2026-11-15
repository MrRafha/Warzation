import { fireEvent, render, screen, within } from "@testing-library/react";
import { act } from "react";
import { describe, expect, it, vi } from "vitest";
import { RealtimeClient } from "../../lib/realtimeClient";
import { FakeSocket, fakeSocketFactory } from "../../test/fakeSocket";
import type { ApiClient } from "../../lib/apiClient";
import { matchState, matchView } from "../../test/matchFixtures";
import { loadFixtureScenario } from "../../test/scenarioFixture";
import { MatchScreen } from "./MatchScreen";
import type { MatchStateView, MatchView } from "./types";

const EVENTS = [
  { seq: 1, type: "MATCH_CREATED", playerId: "jogador-1", playerName: "Jogador 1", commandId: null, occurredAt: "2026-11-15T12:00:00Z", data: {} },
  { seq: 2, type: "PLAYER_JOINED", playerId: "jogador-2", playerName: "Jogador 2", commandId: null, occurredAt: "2026-11-15T12:01:00Z", data: { nationId: "sao-francisco" } },
  { seq: 3, type: "MATCH_STARTED", playerId: "jogador-1", playerName: "Jogador 1", commandId: null, occurredAt: "2026-11-15T12:02:00Z", data: {} },
];

function fakeApi(view: MatchView, state: MatchStateView) {
  const get = vi.fn(async (path: string) =>
    path.endsWith("/state") ? state : path.endsWith("/events") ? EVENTS : view,
  );
  const post = vi.fn().mockResolvedValue({});
  return { api: { get, post } as unknown as ApiClient, get, post };
}

async function renderScreen(view = matchView(), state = matchState(), playerId = "jogador-1") {
  const fake = fakeApi(view, state);
  const onBack = vi.fn();
  render(
    <MatchScreen api={fake.api} matchId="m1" playerId={playerId} loadScenario={loadFixtureScenario} onBack={onBack} />,
  );
  await screen.findByRole("group", { name: /Mapa/ });
  await screen.findByText(/em andamento|aguardando início/);
  return { ...fake, onBack };
}

function panel() {
  return within(screen.getByRole("complementary", { name: "Província selecionada" }));
}

describe("MatchScreen", () => {
  it("mostra dono, unidades e estoque da província selecionada", async () => {
    await renderScreen();

    fireEvent.click(document.querySelector('[data-province-id="pi-teresina"]')!);
    expect(panel().getByText("Nação do Parnaíba (Jogador 1)")).toBeInTheDocument();
    expect(panel().getByText("2× Infantaria básica")).toBeInTheDocument();
    expect(panel().getByText("Comida 60, Pedra 20")).toBeInTheDocument();

    fireEvent.click(document.querySelector('[data-province-id="pe-recife"]')!);
    expect(panel().getByText("Neutra")).toBeInTheDocument();
    expect(panel().getByText("1× Infantaria básica (guarnição neutra)")).toBeInTheDocument();
    expect(panel().getByText("Vazio")).toBeInTheDocument();
  });

  it("colore o mapa pelos donos e mostra a legenda das nações", async () => {
    await renderScreen();

    const teresina = document.querySelector('[data-province-id="pi-teresina"]')!;
    const salvador = document.querySelector('[data-province-id="ba-salvador"]')!;
    expect(teresina.getAttribute("fill")).not.toBe(salvador.getAttribute("fill"));
    const legend = screen.getByRole("list", { name: "Legenda de donos" });
    expect(within(legend).getByText("Nação do Parnaíba")).toBeInTheDocument();
    expect(within(legend).getByText("Neutra")).toBeInTheDocument();
    expect(screen.getByText(/você joga com Nação do Parnaíba/)).toBeInTheDocument();
  });

  it("quem criou inicia a partida aberta", async () => {
    const { post, get } = await renderScreen(
      matchView({ status: "OPEN", startedAt: null }),
      matchState({ status: "OPEN", startedAt: null, provinces: [] }),
    );

    fireEvent.click(screen.getByRole("button", { name: "Iniciar partida" }));

    await vi.waitFor(() =>
      expect(post).toHaveBeenCalledWith("/api/v1/matches/m1/start", { commandId: expect.any(String) }),
    );
    await vi.waitFor(() => expect(get.mock.calls.filter(([p]) => p.endsWith("/state")).length).toBeGreaterThan(1));
  });

  it("os outros participantes aguardam o início", async () => {
    await renderScreen(
      matchView({ status: "OPEN", startedAt: null }),
      matchState({ status: "OPEN", startedAt: null, provinces: [] }),
      "jogador-2",
    );

    expect(screen.queryByRole("button", { name: "Iniciar partida" })).not.toBeInTheDocument();
    expect(screen.getByText("Aguardando quem criou iniciar a partida.")).toBeInTheDocument();
  });

  it("atualiza ao vivo com o estado enviado pelo servidor", async () => {
    FakeSocket.reset();
    const fake = fakeApi(
      matchView({ status: "OPEN", startedAt: null }),
      matchState({ status: "OPEN", startedAt: null, provinces: [] }),
    );
    const realtime = new RealtimeClient("ws://test/ws", fakeSocketFactory, "token-1");
    render(
      <MatchScreen
        api={fake.api}
        matchId="m1"
        playerId="jogador-2"
        loadScenario={loadFixtureScenario}
        onBack={() => {}}
        realtime={realtime}
      />,
    );
    await screen.findByText(/aguardando início/);

    const socket = FakeSocket.latest();
    act(() => socket.serverOpen());
    expect(socket.sent.map((m) => m.type)).toEqual(["AUTHENTICATE", "JOIN_MATCH"]);
    expect(await screen.findByText("ao vivo")).toBeInTheDocument();

    // outro jogador iniciou a partida: o servidor difunde o novo estado
    act(() =>
      socket.serverSend({ type: "MATCH_STATE", id: "s1", roomId: "m1", payload: { match: matchView(), state: matchState() } }),
    );

    expect(await screen.findByText(/em andamento/)).toBeInTheDocument();
    fireEvent.click(document.querySelector('[data-province-id="ba-salvador"]')!);
    expect(panel().getByText("Nação do São Francisco (Jogador 2)")).toBeInTheDocument();

    // mensagens de outra partida são ignoradas
    act(() =>
      socket.serverSend({
        type: "MATCH_STATE",
        id: "s2",
        roomId: "outra",
        payload: { match: matchView({ name: "Outra" }), state: matchState() },
      }),
    );
    expect(screen.queryByText("Outra")).not.toBeInTheDocument();
  });

  it("mostra o histórico da partida em ordem", async () => {
    await renderScreen();

    const history = within(await screen.findByRole("region", { name: "Histórico" }));
    const items = await history.findAllByRole("listitem");
    expect(items.map((item) => item.textContent?.replace(/^\S+\s/, ""))).toEqual([
      "Jogador 1 criou a partida",
      "Jogador 2 entrou com Nação do São Francisco",
      "Jogador 1 iniciou a partida",
    ]);
  });

  it("volta para a lista de partidas", async () => {
    const { onBack } = await renderScreen();

    fireEvent.click(screen.getByRole("button", { name: "← Voltar às partidas" }));
    expect(onBack).toHaveBeenCalled();
  });
});
