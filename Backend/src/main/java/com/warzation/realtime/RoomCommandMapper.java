package com.warzation.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.warzation.application.InvalidCommandException;
import com.warzation.application.RoomCommand;
import com.warzation.application.RoomCommandType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Converte o JSON recebido (HTTP ou WebSocket) em um {@link RoomCommand}.
 * Campos desconhecidos são ignorados: o cliente só expressa a intenção, nunca o
 * estado resultante.
 */
@Component
public class RoomCommandMapper {

    public RoomCommand toCommand(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new InvalidCommandException("O comando deve ser um objeto JSON", Map.of());
        }

        Map<String, String> errors = new LinkedHashMap<>();
        UUID commandId = parseCommandId(body.get("commandId"), errors);
        RoomCommandType type = parseType(body.get("type"), errors);
        long amount = 0;

        if (type == RoomCommandType.INCREMENT_COUNTER) {
            JsonNode payload = body.get("payload");
            JsonNode amountNode = payload == null ? null : payload.get("amount");
            if (amountNode == null || !amountNode.isIntegralNumber() || !amountNode.canConvertToLong()) {
                errors.put("payload.amount", "obrigatório e inteiro");
            } else {
                amount = amountNode.asLong();
                if (amount < RoomCommand.MIN_AMOUNT || amount > RoomCommand.MAX_AMOUNT) {
                    errors.put("payload.amount",
                            "deve estar entre " + RoomCommand.MIN_AMOUNT + " e " + RoomCommand.MAX_AMOUNT);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidCommandException("Comando inválido", errors);
        }
        return new RoomCommand(commandId, type, amount);
    }

    public static UUID parseUuid(String value) {
        if (value == null || value.length() != 36) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static UUID parseCommandId(JsonNode node, Map<String, String> errors) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            errors.put("commandId", "obrigatório (UUID)");
            return null;
        }
        UUID commandId = parseUuid(node.asText());
        if (commandId == null) {
            errors.put("commandId", "deve ser um UUID válido");
        }
        return commandId;
    }

    private static RoomCommandType parseType(JsonNode node, Map<String, String> errors) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            errors.put("type", "obrigatório");
            return null;
        }
        return RoomCommandType.parse(node.asText()).orElseGet(() -> {
            errors.put("type", "desconhecido; use um de " + Arrays.toString(RoomCommandType.values()));
            return null;
        });
    }
}
