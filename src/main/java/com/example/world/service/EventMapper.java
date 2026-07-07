package com.example.world.service;

import com.example.world.entity.*;
import com.example.world.util.ByteTypeConverter;
import com.example.world.websocket.WebSocketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.world.entity.StateEnum.*;

@Service
@RequiredArgsConstructor
public class EventMapper {
    private final WebSocketMapper webSocketMapper;
    private final ObjectMapper objectMapper;

    public List<SimulationEvent> entitiesToEvents(List<RedisEntity> entityList) {
        Long curTick = webSocketMapper.getTick();
        return entityList.stream()
                .map(entity -> new SimulationEvent(
                        entity.getState(),
                        curTick,
                        entity.getId(),
                        entity.getTargetId() == null ? 0 : entity.getTargetId(),
                        entity.getAge()
                ))
                .toList();
    }

    public String stringToValue(Object object) {
        return objectMapper.writeValueAsString(object);
    }

    public EntityHistoryDto stringToEntityHistoryDto(String value) {
        JsonNode jsonNode = objectMapper.readTree(value);
        JsonNode payLoad = jsonNode.get("payload");

        StateEnum state = StateEnum.valueOf(jsonNode.get("state").asString());

        Long targetId = null;
        if (payLoad != null && payLoad.hasNonNull("targetId")) {
            targetId = payLoad.get("targetId").asLong();
        }

        int x = payLoad.get("x").asInt();
        int y = payLoad.get("y").asInt();
        long tick = jsonNode.get("tick").asLong();

        return new EntityHistoryDto(state, targetId, x, y, tick);
    }

    public SimulationEvent stringToSimulationEventDto(String value) {
        return objectMapper.readValue(value, SimulationEvent.class);
    }

    private Map<String, String> entityToMap(RedisEntity entity, StateEnum state) {
        Map<String, String> result = new HashMap<>();
        result.computeIfPresent("targetId",
                (k, targetId) -> targetId.equals("null") ? null : String.valueOf(entity.getTargetId()));
        result.put("x", String.valueOf(entity.getX()));
        result.put("y", String.valueOf(entity.getY()));
        return result;
    }

    public Map<byte[], byte[]> eventToMap(SimulationEvent event) {
        Map<byte[], byte[]> result = new HashMap<>();

        result.put(
                ByteTypeConverter.stringToByte("tick"),
                ByteTypeConverter.numToByte(event.getTick())
        );

        result.put(
                ByteTypeConverter.stringToByte("state"),
                ByteTypeConverter.stringToByte(event.getState().name())
        );

        result.put(
                ByteTypeConverter.stringToByte("entityId"),
                ByteTypeConverter.numToByte(event.getEntityId())
        );

        result.put(
                ByteTypeConverter.stringToByte("targetId"),
                ByteTypeConverter.numToByte(event.getTargetId())
        );

        result.put(
                ByteTypeConverter.stringToByte("age"),
                ByteTypeConverter.numToByte(event.getAge())
        );

        return result;
    }
}
