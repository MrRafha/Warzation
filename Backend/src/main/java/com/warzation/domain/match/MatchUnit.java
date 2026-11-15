package com.warzation.domain.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Pilha de unidades de um tipo numa província. Só o necessário para existir
 * no mapa; recrutamento, movimento e combate vêm em sprints futuras.
 */
@Entity
@Table(name = "match_units")
public class MatchUnit {

    public static final String BASIC_INFANTRY = "INFANTARIA_BASICA";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "match_id", nullable = false, length = 36)
    private String matchId;

    @Column(name = "province_id", nullable = false, length = 50)
    private String provinceId;

    /** Nulo para guarnição neutra. */
    @Column(name = "owner_nation_id", length = 50)
    private String ownerNationId;

    @Column(name = "unit_type", nullable = false, length = 30)
    private String unitType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected MatchUnit() {
    }

    public MatchUnit(String matchId, String provinceId, String ownerNationId, String unitType, int quantity) {
        this.id = UUID.randomUUID();
        this.matchId = matchId;
        this.provinceId = provinceId;
        this.ownerNationId = ownerNationId;
        this.unitType = unitType;
        this.quantity = quantity;
    }

    public String getProvinceId() {
        return provinceId;
    }

    public String getOwnerNationId() {
        return ownerNationId;
    }

    public String getUnitType() {
        return unitType;
    }

    public int getQuantity() {
        return quantity;
    }
}
