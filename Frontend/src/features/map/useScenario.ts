import { useEffect, useState } from "react";
import type { Scenario } from "./types";

export type ScenarioState =
  | { status: "loading" }
  | { status: "ready"; scenario: Scenario }
  | { status: "error"; message: string };

export function useScenario(load: () => Promise<Scenario>) {
  const [state, setState] = useState<ScenarioState>({ status: "loading" });

  useEffect(() => {
    let active = true;
    load()
      .then((scenario) => active && setState({ status: "ready", scenario }))
      .catch((error: unknown) => {
        if (active) {
          setState({ status: "error", message: error instanceof Error ? error.message : "Erro ao carregar o mapa" });
        }
      });
    return () => {
      active = false;
    };
  }, [load]);

  return state;
}
