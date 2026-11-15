package com.warzation.application.map;

import java.util.List;

/**
 * Formato de {@code data/scenarios/<id>/scenario.json}, versão 1. Documentado em
 * {@code Backend/data/README.md}. Campos desconhecidos são rejeitados na leitura.
 */
public record ScenarioFile(
        int formatVersion,
        String id,
        String name,
        boolean provisional,
        String geometryFile,
        List<State> states,
        List<Terrain> terrains,
        List<Province> provinces,
        List<List<String>> connections,
        List<Nation> nations) {

    public record State(String id, String name) {
    }

    public record Terrain(String id, String name, String description) {
    }

    public record Province(
            String id,
            String name,
            String state,
            MainCity mainCity,
            String terrain,
            Boolean coastal,
            List<Deposit> deposits) {
    }

    public record MainCity(String name, Double lon, Double lat) {
    }

    public record Deposit(Resource resource, Abundance abundance) {
    }

    public record Nation(String id, String name, String capital, List<String> startingProvinces) {
    }

    public enum Resource {
        PEDRA,
        METAL,
        PETROLEO
    }

    public enum Abundance {
        BAIXA,
        MEDIA,
        ALTA
    }
}
