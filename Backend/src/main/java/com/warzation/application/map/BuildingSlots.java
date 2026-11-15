package com.warzation.application.map;

/** Regra do jogo, não dado do mapa: espaços de construção por província. */
public final class BuildingSlots {

    public static final int COMMON = 3;
    public static final int CAPITAL = 4;

    private BuildingSlots() {
    }

    public static int forProvince(boolean capital) {
        return capital ? CAPITAL : COMMON;
    }
}
