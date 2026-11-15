package com.warzation.realtime;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warzation.domain.Room;
import com.warzation.domain.RoomRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoomControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    private String roomId;

    @BeforeEach
    void createRoom() {
        roomId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        roomRepository.save(new Room(roomId));
    }

    @Test
    void seededDevRoomExists() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId", equalTo("dev")));
    }

    @Test
    void validCommandIsAppliedByTheServer() throws Exception {
        String commandId = UUID.randomUUID().toString();

        sendCommand(command(commandId, "INCREMENT_COUNTER", 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", equalTo("APPLIED")))
                .andExpect(jsonPath("$.commandId", equalTo(commandId)))
                .andExpect(jsonPath("$.resultingCounter", equalTo(3)))
                .andExpect(jsonPath("$.room.counter", equalTo(3)))
                .andExpect(jsonPath("$.room.version", equalTo(1)));

        expectCounter(3, 1);
    }

    @Test
    void resendingTheSameCommandIdDoesNotDuplicateTheEffect() throws Exception {
        Map<String, Object> command = command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 5);

        sendCommand(command).andExpect(status().isCreated());
        sendCommand(command)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("DUPLICATE")))
                .andExpect(jsonPath("$.resultingCounter", equalTo(5)))
                .andExpect(jsonPath("$.room.counter", equalTo(5)));
        sendCommand(command).andExpect(status().isOk());

        expectCounter(5, 1);
    }

    @Test
    void reusingACommandIdForADifferentIntentIsAConflict() throws Exception {
        String commandId = UUID.randomUUID().toString();
        sendCommand(command(commandId, "INCREMENT_COUNTER", 1)).andExpect(status().isCreated());

        sendCommand(command(commandId, "INCREMENT_COUNTER", 2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("COMMAND_CONFLICT")));

        expectCounter(1, 1);
    }

    @Test
    void invalidCommandsAreRejectedWithoutChangingState() throws Exception {
        sendCommand(command("nao-e-uuid", "INCREMENT_COUNTER", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("INVALID_COMMAND")))
                .andExpect(jsonPath("$.fields.commandId").exists());

        sendCommand(command(UUID.randomUUID().toString(), "DELETE_EVERYTHING", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.type").exists());

        sendCommand(command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['payload.amount']").exists());

        sendCommand(command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 1000))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['payload.amount']").exists());

        sendCommand(Map.of("commandId", UUID.randomUUID().toString(), "type", "INCREMENT_COUNTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['payload.amount']").exists());

        mockMvc.perform(post("/api/v1/rooms/{roomId}/commands", roomId)
                        .header("Authorization", PLAYER_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2, 3]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("INVALID_COMMAND")));

        expectCounter(0, 0);
    }

    @Test
    void clientCannotSetTheOfficialStateDirectly() throws Exception {
        mockMvc.perform(put("/api/v1/rooms/{roomId}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"counter\": 999}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code", equalTo("METHOD_NOT_ALLOWED")));

        Map<String, Object> command = command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 1);
        command.put("counter", 999);
        command.put("version", 999);
        sendCommand(command)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.room.counter", equalTo(1)));

        expectCounter(1, 1);
    }

    @Test
    void commandsWithoutValidPlayerAreRejected() throws Exception {
        Map<String, Object> command = command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 1);

        sendCommand(command, null)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code", equalTo("UNAUTHENTICATED")));
        sendCommand(command, "Bearer token-que-nao-existe-123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", equalTo("Token de jogador inválido")));
        sendCommand(command, "token-de-teste-jogador-1")
                .andExpect(status().isUnauthorized());

        expectCounter(0, 0);
    }

    @Test
    void playerComesFromTheTokenNotFromTheBody() throws Exception {
        Map<String, Object> command = command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 1);
        command.put("playerId", "jogador-2");

        sendCommand(command, PLAYER_1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerId", equalTo("jogador-1")));
    }

    @Test
    void anotherPlayerCannotReuseSomeoneElsesCommandId() throws Exception {
        Map<String, Object> command = command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 1);
        sendCommand(command, PLAYER_1).andExpect(status().isCreated());

        sendCommand(command, PLAYER_2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("COMMAND_CONFLICT")));

        expectCounter(1, 1);
    }

    @Test
    void commandResultCanBeQueried() throws Exception {
        String commandId = UUID.randomUUID().toString();
        sendCommand(command(commandId, "INCREMENT_COUNTER", 4)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/rooms/{roomId}/commands/{commandId}", roomId, commandId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commandId", equalTo(commandId)))
                .andExpect(jsonPath("$.status", equalTo("APPLIED")))
                .andExpect(jsonPath("$.resultingCounter", equalTo(4)));

        mockMvc.perform(get("/api/v1/rooms/{roomId}/commands/{commandId}", roomId, UUID.randomUUID()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/rooms/dev/commands/{commandId}", commandId))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownRoomReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/sala-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));

        mockMvc.perform(post("/api/v1/rooms/sala-que-nao-existe/commands")
                        .header("Authorization", PLAYER_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 1))))
                .andExpect(status().isNotFound());
    }

    static final String PLAYER_1 = "Bearer token-de-teste-jogador-1";
    static final String PLAYER_2 = "Bearer token-de-teste-jogador-2";

    private ResultActions sendCommand(Map<String, Object> command) throws Exception {
        return sendCommand(command, PLAYER_1);
    }

    private ResultActions sendCommand(Map<String, Object> command, String authorization) throws Exception {
        var request = post("/api/v1/rooms/{roomId}/commands", roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return mockMvc.perform(request);
    }

    private void expectCounter(long counter, long version) throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomId}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counter", equalTo((int) counter)))
                .andExpect(jsonPath("$.version", equalTo((int) version)));
    }

    static Map<String, Object> command(String commandId, String type, long amount) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("commandId", commandId);
        command.put("type", type);
        command.put("payload", Map.of("amount", amount));
        return command;
    }
}
