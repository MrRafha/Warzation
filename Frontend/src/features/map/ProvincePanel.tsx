import { abundanceLabel, resourceLabel } from "./labels";
import type { ProvinceDetails, Scenario } from "./types";

type Props = {
  scenario: Scenario;
  provinceId: string | null;
  onSelect: (provinceId: string) => void;
  /** Dados da partida; sem eles, dono, unidades e estoque aparecem vazios. */
  details?: ProvinceDetails;
};

/**
 * Dados de uma província. Dono, construções e estoque são estado de partida;
 * sem partida ativa, aparecem vazios em vez de inventados.
 */
export function ProvincePanel({ scenario, provinceId, onSelect, details }: Props) {
  const province = provinceId ? scenario.provinces.get(provinceId) : undefined;

  if (!province) {
    return (
      <aside className="province-panel" aria-label="Província selecionada">
        <p className="muted">Selecione uma província no mapa ou na lista.</p>
      </aside>
    );
  }

  const isCapital = scenario.capitals.has(province.id);
  const spaces = scenario.buildingSlots.get(province.id);
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
        <dd>{details ? (details.owner?.label ?? "Neutra") : "Neutra (sem partida ativa)"}</dd>

        {details && (
          <>
            <dt>Unidades</dt>
            <dd>
              {details.units.length === 0
                ? "Nenhuma"
                : details.units
                    .map((unit) => `${unit.quantity}× ${unit.label}${unit.neutral ? " (guarnição neutra)" : ""}`)
                    .join(", ")}
            </dd>
          </>
        )}

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
        <dd>{spaces === undefined ? "—" : `0 de ${spaces} ocupados`}</dd>

        <dt>Construções</dt>
        <dd>Nenhuma</dd>

        <dt>Estoque</dt>
        <dd>
          {!details
            ? "— (definido na partida)"
            : details.stock.length === 0
              ? "Vazio"
              : details.stock.map((item) => `${item.label} ${item.amount}`).join(", ")}
        </dd>
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
