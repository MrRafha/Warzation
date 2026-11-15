import type { Scenario } from "./types";

type Props = {
  scenario: Scenario;
  selectedId: string | null;
  onSelect: (provinceId: string) => void;
};

/** Alternativa ao mapa para teclado e leitores de tela: províncias agrupadas por estado. */
export function ProvinceList({ scenario, selectedId, onSelect }: Props) {
  return (
    <details className="province-list">
      <summary>Lista de províncias</summary>
      {scenario.data.states.map((state) => (
        <section key={state.id} aria-label={state.name}>
          <h4>{state.name}</h4>
          <ul>
            {scenario.data.provinces
              .filter((province) => province.state === state.id)
              .map((province) => (
                <li key={province.id}>
                  <button
                    type="button"
                    className="link-button"
                    aria-current={province.id === selectedId ? "true" : undefined}
                    onClick={() => onSelect(province.id)}
                  >
                    {province.name}
                  </button>
                </li>
              ))}
          </ul>
        </section>
      ))}
    </details>
  );
}
