package com.warzation.domain.match;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Estoque de um recurso guardado numa província (o estoque é local). */
@Entity
@Table(name = "match_province_stocks")
public class MatchProvinceStock {

    @EmbeddedId
    private MatchProvinceStockId id;

    @Column(name = "amount", nullable = false)
    private long amount;

    protected MatchProvinceStock() {
    }

    public MatchProvinceStock(String matchId, String provinceId, String resource, long amount) {
        this.id = new MatchProvinceStockId(matchId, provinceId, resource);
        this.amount = amount;
    }

    public String getProvinceId() {
        return id.getProvinceId();
    }

    public String getResource() {
        return id.getResource();
    }

    public long getAmount() {
        return amount;
    }
}
