package com.warzation.application;

import com.warzation.domain.Room;
import java.time.OffsetDateTime;

/** Estado oficial de uma sala, como o servidor o publica. */
public record RoomState(String roomId, long counter, long version, OffsetDateTime updatedAt) {

    public static RoomState from(Room room) {
        return new RoomState(room.getId(), room.getCounterValue(), room.getVersion(), room.getUpdatedAt());
    }
}
