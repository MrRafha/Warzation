package com.warzation.application;

import java.util.Arrays;
import java.util.Optional;

/** Tipos de comando aceitos pela sala de teste. */
public enum RoomCommandType {
    INCREMENT_COUNTER;

    public static Optional<RoomCommandType> parse(String value) {
        return Arrays.stream(values()).filter(type -> type.name().equals(value)).findFirst();
    }
}
