import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ApiError, type ApiClient } from "../../lib/apiClient";
import { MatchLobby } from "./MatchLobby";
import type { MatchView } from "./types";

function match(overrides: Partial<MatchView> = {}): MatchView {
  return {
    id: "m1",
    name: "Partida A",
    scenarioId: "nordeste-teste",
    status: "OPEN",
    maxPlayers: 2,
    createdBy: "jogador-1",
    createdAt: "2026-11-15T12:00:00Z",
    participants: [],
    nations: [
      { id: "parnaiba", name: "Nação do Parnaíba", capital: "pi-teresina", playerId: null },
      { id: "sao-francisco", name: "Nação do São Francisco", capital: "ba-salvador", playerId: null },
    ],
    ...overrides,
  };
}

function fakeApi(lists: MatchView[][], post = vi.fn().mockResolvedValue({})) {
  const get = vi.fn();
  lists.forEach((list) => get.mockResolvedValueOnce(list));
  get.mockResolvedValue(lists.at(-1) ?? []);
  return { api: { get, post } as unknown as ApiClient & { get: typeof get; post: typeof post }, get, post };
}

describe("MatchLobby", () => {
  it("lista partidas abertas com as nações livres e ocupadas", async () => {
    const { api } = fakeApi([
      [
        match({
          participants: [{ playerId: "jogador-2", playerName: "Jogador 2", nationId: "sao-francisco", joinedAt: "" }],
        }),
      ],
    ]);

    render(<MatchLobby api={api} playerId="jogador-1" />);

    const card = within((await screen.findByText("Partida A")).closest("li")!);
    expect(card.getByText(/1\/2 jogadores/)).toBeInTheDocument();
    expect(card.getByRole("button", { name: "Entrar com Nação do Parnaíba" })).toBeInTheDocument();
    expect(card.getByText(/Jogador 2/)).toBeInTheDocument();
    expect(card.queryByRole("button", { name: /São Francisco/ })).not.toBeInTheDocument();
  });

  it("cria partida com um commandId novo e atualiza a lista", async () => {
    const { api, get, post } = fakeApi([[], [match({ name: "Minha partida" })]]);

    render(<MatchLobby api={api} playerId="jogador-1" />);
    await screen.findByText("Nenhuma partida aberta.");
    fireEvent.change(screen.getByLabelText("Nome da nova partida"), { target: { value: "Minha partida" } });
    fireEvent.click(screen.getByRole("button", { name: "Criar partida" }));

    expect(await screen.findByText("Minha partida")).toBeInTheDocument();
    expect(post).toHaveBeenCalledWith("/api/v1/matches", {
      commandId: expect.stringMatching(/^[0-9a-f-]{36}$/),
      scenarioId: "nordeste-teste",
      name: "Minha partida",
    });
    expect(get).toHaveBeenCalledWith("/api/v1/matches?status=OPEN");
  });

  it("entra numa partida escolhendo a nação", async () => {
    const joined = match({
      participants: [{ playerId: "jogador-1", playerName: "Jogador 1", nationId: "parnaiba", joinedAt: "" }],
    });
    const { api, post } = fakeApi([[match()], [joined]]);

    render(<MatchLobby api={api} playerId="jogador-1" />);
    fireEvent.click(await screen.findByRole("button", { name: "Entrar com Nação do Parnaíba" }));

    expect(await screen.findByText("Você está nesta partida.")).toBeInTheDocument();
    expect(post).toHaveBeenCalledWith("/api/v1/matches/m1/participants", {
      commandId: expect.any(String),
      nationId: "parnaiba",
    });
    expect(screen.queryByRole("button", { name: /Entrar com/ })).not.toBeInTheDocument();
  });

  it("mostra a recusa do servidor", async () => {
    const post = vi.fn().mockRejectedValue(new ApiError("http", "A nação parnaiba já foi escolhida", 409));
    const { api } = fakeApi([[match()]], post);

    render(<MatchLobby api={api} playerId="jogador-1" />);
    fireEvent.click(await screen.findByRole("button", { name: "Entrar com Nação do Parnaíba" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("já foi escolhida");
  });

  it("sem jogador, só lista", async () => {
    const { api } = fakeApi([[match()]]);

    render(<MatchLobby api={api} playerId={null} />);

    await screen.findByText("Partida A");
    expect(screen.queryByRole("button", { name: /Entrar com/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Criar partida" })).not.toBeInTheDocument();
    expect(screen.getByText(/Entre como jogador de teste/)).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText("Carregando partidas...")).not.toBeInTheDocument());
  });
});
