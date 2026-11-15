import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { App } from "./App";
import { FakeSocket } from "./test/fakeSocket";
import { scenarioResponseFixture } from "./test/scenarioFixture";

const health = { status: "UP", service: "warzation-backend", version: "0.0.1-SNAPSHOT" };

beforeEach(() => {
  FakeSocket.reset();
  vi.stubEnv("VITE_API_BASE_URL", "http://api.test");
  vi.stubEnv("VITE_WS_URL", "ws://api.test/ws");
  vi.stubGlobal("WebSocket", FakeSocket);
});

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
});

function mockFetch(implementation: (url: string) => Promise<Response>) {
  const fetchMock = vi.fn(implementation);
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

/** Backend no ar: responde o health e o cenário do mapa. */
async function onlineBackend(url: string): Promise<Response> {
  if (url.endsWith("/api/v1/scenarios/nordeste-teste")) {
    return Response.json(scenarioResponseFixture());
  }
  return Response.json(health);
}

it("consulta o endpoint de saúde configurado e mostra o Backend conectado", async () => {
  const fetchMock = mockFetch(onlineBackend);

  render(<App />);

  expect(screen.getByRole("heading", { name: "Warzation" })).toBeVisible();
  expect(screen.getByText("Verificando conexão...")).toBeVisible();
  expect(await screen.findByText("Backend conectado")).toBeVisible();
  expect(fetchMock).toHaveBeenCalledWith("http://api.test/api/v1/health", expect.anything());
});

it("mostra o Backend offline sem quebrar quando a API não responde", async () => {
  mockFetch(async () => {
    throw new TypeError("Failed to fetch");
  });

  render(<App />);

  expect(await screen.findByText("Backend offline")).toBeVisible();
  expect(screen.getByText(/Não foi possível alcançar o Backend/)).toBeVisible();
  expect(screen.queryByText("Sala de teste")).not.toBeInTheDocument();
  expect(screen.getByText("O mapa aparece quando o Backend estiver conectado.")).toBeVisible();
});

it("carrega o mapa pela API quando o Backend está conectado", async () => {
  const fetchMock = mockFetch(onlineBackend);

  render(<App />);

  const map = await screen.findByRole("group", { name: /Mapa/ });
  expect(map.querySelectorAll("path.province")).toHaveLength(24);
  expect(fetchMock).toHaveBeenCalledWith("http://api.test/api/v1/scenarios/nordeste-teste", expect.anything());

  fireEvent.click(map.querySelector('[data-province-id="ba-salvador"]')!);
  expect(screen.getByText("0 de 4 ocupados")).toBeInTheDocument();
});

it("mostra erro se o mapa não vier da API", async () => {
  mockFetch(async (url) =>
    url.endsWith("/api/v1/health")
      ? Response.json(health)
      : Response.json({ code: "NOT_FOUND", message: "Cenário não encontrado" }, { status: 404 }),
  );

  render(<App />);

  expect(await screen.findByRole("alert")).toHaveTextContent("Cenário não encontrado");
});

it("permite tentar de novo depois de uma falha", async () => {
  let healthCalls = 0;
  mockFetch(async (url) => {
    if (url.endsWith("/api/v1/health") && ++healthCalls === 1) {
      return Response.json({ code: "INTERNAL_ERROR", message: "Erro interno" }, { status: 500 });
    }
    return onlineBackend(url);
  });

  render(<App />);
  expect(await screen.findByText("Backend offline")).toBeVisible();

  fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
  expect(await screen.findByText("Backend conectado")).toBeVisible();
});

it("avisa quando as variáveis de ambiente não foram configuradas", () => {
  vi.stubEnv("VITE_API_BASE_URL", "");
  const fetchMock = mockFetch(onlineBackend);

  render(<App />);

  expect(screen.getByRole("alert")).toHaveTextContent("VITE_API_BASE_URL");
  expect(fetchMock).not.toHaveBeenCalled();
});

it("mostra o estado oficial da sala de teste e envia comandos pelo WebSocket", async () => {
  mockFetch(onlineBackend);
  render(<App />);
  await screen.findByText("Sala de teste");

  const socket = FakeSocket.latest();
  expect(socket.url).toBe("ws://api.test/ws");
  act(() => socket.serverOpen());
  expect(socket.sent[0]).toMatchObject({ type: "JOIN_ROOM", payload: { roomId: "dev" } });

  act(() =>
    socket.serverSend({
      type: "ROOM_STATE",
      id: "s1",
      roomId: "dev",
      payload: { roomId: "dev", counter: 4, version: 4, updatedAt: "2026-11-15T12:00:00Z" },
    }),
  );
  expect(screen.getByTestId("room-counter")).toHaveTextContent("4");

  fireEvent.click(screen.getByRole("button", { name: "+1" }));
  const command = socket.sent.at(-1);
  expect(command).toMatchObject({ type: "COMMAND", payload: { type: "INCREMENT_COUNTER", payload: { amount: 1 } } });

  // O contador só muda quando o servidor publica o novo estado.
  expect(screen.getByTestId("room-counter")).toHaveTextContent("4");
  act(() =>
    socket.serverSend({
      type: "ROOM_STATE",
      id: "s2",
      roomId: "dev",
      payload: { roomId: "dev", counter: 5, version: 5, updatedAt: "2026-11-15T12:00:01Z" },
    }),
  );
  expect(screen.getByTestId("room-counter")).toHaveTextContent("5");
});
