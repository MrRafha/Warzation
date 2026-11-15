package com.warzation.application;

import java.util.UUID;

/** Intenção enviada pelo cliente, já validada quanto ao formato. */
public record RoomCommand(UUID commandId, RoomCommandType type, long amount) {

    public static final long MIN_AMOUNT = 1;
    public static final long MAX_AMOUNT = 100;
}
