import { abundanceLabel, resourceLabel } from "./labels";
import type { Scenario } from "./types";

const SPACES_PER_PROVINCE = 3;
const SPACES_PER_CAPITAL = 4;

type Props = {
  scenario: Scenario;
  provinceId: string | null;
  onSelect: (provinceId: string) => void;
};

/**
 * Dados de uma província. Dono, construções e estoque são estado de partida;
 * sem partida ativa, aparecem vazios em vez de inventados.
 */
export function ProvincePanel({ scenario, provinceId, onSelect }: Props) {
  const province = provinceId ? scenario.provinces.get(provinceId) : undefined;

  if (!province) {
    return (
      <aside className="province-panel" aria-label="Província selecionada">
        <p className="muted">Selecione uma província no mapa ou na lista.</p>
      </aside>
    );
  }

  const isCapital = scenario.capitals.has(province.id);
  const spaces = isCapital ? SPACES_PER_CAPITAL : SPACES_PER_PROVINCE;
  const neighbours = scenario.neighbours.get(province.id) ?? [];

  return (
    <aside className="province-panel" aria-label="Província selecionada">
      <p className="eyebrow">{scenario.stateNames.get(province.state) ?? province.state}</p>
      <h2>{province.name}</h2>

      <dl className="province-facts">
        <dt>Cidade principal</dt>
        <dd>
          {province.mainCity.name}
          {isCapital && " (capital no cenário)"}
        </dd>

        <dt>Dono</dt>
        <dd>Neutra (sem partida ativa)</dd>

        <dt>Terreno</dt>
        <dd>{scenario.terrainNames.get(province.terrain) ?? province.terrain}</dd>

        <dt>Costa</dt>
        <dd>{province.coastal ? "Sim" : "Não"}</dd>

        <dt>Depósitos</dt>
        <dd>
          {province.deposits.length === 0
            ? "Nenhum"
            : province.deposits
                .map((deposit) => `${resourceLabel[deposit.resource]} (${abundanceLabel[deposit.abundance]})`)
                .join(", ")}
        </dd>

        <dt>Espaços de construção</dt>
        <dd>0 de {spaces} ocupados</dd>

        <dt>Construções</dt>
        <dd>Nenhuma</dd>

        <dt>Estoque</dt>
        <dd>— (definido na partida)</dd>
      </dl>

      <h3>Conexões ({neighbours.length})</h3>
      <ul className="neighbour-list">
        {neighbours.map((id) => {
          const neighbour = scenario.provinces.get(id);
          return (
            <li key={id}>
              <button type="button" className="link-button" onClick={() => onSelect(id)}>
                {neighbour?.name ?? id}
              </button>
              {neighbour && neighbour.state !== province.state && (
                <span className="muted"> · {neighbour.state}</span>
              )}
            </li>
          );
        })}
      </ul>
    </aside>
  );
}
