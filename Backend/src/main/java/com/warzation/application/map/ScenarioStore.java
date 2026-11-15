package com.warzation.application.map;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste a definição dos cenários para que partidas possam referenciar
 * províncias e nações por chave estrangeira. A geometria não vai para o banco:
 * é apresentação e é servida a partir dos arquivos.
 */
@Repository
public class ScenarioStore {

    public enum SyncResult {
        INSERTED,
        REPLACED,
        UNCHANGED
    }

    private final JdbcTemplate jdbc;

    public ScenarioStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Grava o cenário; se o mesmo conteúdo já foi gravado, não faz nada. */
    @Transactional
    public SyncResult sync(LoadedScenario scenario) {
        ScenarioFile file = scenario.file();
        List<String> hashes = jdbc.queryForList(
                "SELECT content_hash FROM scenarios WHERE id = ?", String.class, file.id());
        if (!hashes.isEmpty() && hashes.get(0).equals(scenario.contentHash())) {
            return SyncResult.UNCHANGED;
        }
        boolean existed = !hashes.isEmpty();
        if (existed) {
            Integer usedBy = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM matches WHERE scenario_id = ?", Integer.class, file.id());
            if (usedBy != null && usedBy > 0) {
                throw new ScenarioValidationException("data/scenarios/" + file.id(), List.of(
                        "o conteúdo do cenário mudou, mas ele já é usado por " + usedBy + " partida(s); "
                                + "publique as mudanças com um novo id de cenário"));
            }
            jdbc.update("DELETE FROM scenarios WHERE id = ?", file.id());
        }

        jdbc.update("""
                INSERT INTO scenarios (id, name, format_version, provisional, content_hash, loaded_at)
                VALUES (?, ?, ?, ?, ?, ?)""",
                file.id(), file.name(), file.formatVersion(), file.provisional(), scenario.contentHash(),
                Timestamp.from(Instant.now()));

        for (ScenarioFile.Province p : file.provinces()) {
            jdbc.update("""
                    INSERT INTO scenario_provinces
                        (scenario_id, id, name, state_id, main_city, city_lon, city_lat, terrain, coastal, building_slots)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    file.id(), p.id(), p.name(), p.state(), p.mainCity().name(), p.mainCity().lon(),
                    p.mainCity().lat(), p.terrain(), p.coastal(), scenario.buildingSlots().get(p.id()));
            for (ScenarioFile.Deposit deposit : p.deposits() == null ? List.<ScenarioFile.Deposit>of() : p.deposits()) {
                jdbc.update("""
                        INSERT INTO scenario_province_deposits (scenario_id, province_id, resource, abundance)
                        VALUES (?, ?, ?, ?)""",
                        file.id(), p.id(), deposit.resource().name(), deposit.abundance().name());
            }
        }

        for (List<String> edge : file.connections()) {
            String a = edge.get(0).compareTo(edge.get(1)) < 0 ? edge.get(0) : edge.get(1);
            String b = a.equals(edge.get(0)) ? edge.get(1) : edge.get(0);
            jdbc.update("INSERT INTO scenario_connections (scenario_id, province_a, province_b) VALUES (?, ?, ?)",
                    file.id(), a, b);
        }

        for (ScenarioFile.Nation nation : file.nations()) {
            jdbc.update("""
                    INSERT INTO scenario_nations (scenario_id, id, name, capital_province_id)
                    VALUES (?, ?, ?, ?)""",
                    file.id(), nation.id(), nation.name(), nation.capital());
            for (String province : nation.startingProvinces()) {
                jdbc.update("""
                        INSERT INTO scenario_nation_provinces (scenario_id, nation_id, province_id)
                        VALUES (?, ?, ?)""",
                        file.id(), nation.id(), province);
            }
        }
        return existed ? SyncResult.REPLACED : SyncResult.INSERTED;
    }
}
