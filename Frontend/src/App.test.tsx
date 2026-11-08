import { render, screen } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { App } from "./App";

beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({ ok: true }),
  );
});

it("identifica o projeto e mostra o estado conectado", async () => {
  render(<App />);

  expect(screen.getByRole("heading", { name: "Warzation" })).toBeVisible();
  expect(await screen.findByText("Backend conectado")).toBeVisible();
});
