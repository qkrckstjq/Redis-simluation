package com.example.world.stream;

import com.example.world.entity.*;
import com.example.world.service.TickManager;
import com.example.world.util.ByteTypeConverter;
import com.example.world.websocket.WebSocketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventMapper {
    private final WebSocketMapper webSocketMapper;
    private final ObjectMapper objectMapper;
    private final TickManager tickManager;

    public List<HistoryEvent> entitiesToHistoryEvents(List<RedisEntity> entityList) {
        return entityList.stream()
                .map(this::entityToHistoryEvent)
                .toList();
    }

    public List<StreamEvent> entitiesToStreamEvents(List<RedisEntity> entityList) {
        return entityList.stream()
                .map(this::entityToStreamEvent)
                .toList();
    }

    public String stringToValue(Object object) {
        return objectMapper.writeValueAsString(object);
    }

    public EntityHistoryDto stringToEntityHistoryDto(String value) throws JsonParseException {
        JsonNode jsonNode = objectMapper.readTree(value);

        int tick = jsonNode.get("tick").asInt();
        StateEnum state = StateEnum.valueOf(jsonNode.get("state").asText());
        long entityId = jsonNode.get("entityId").asLong();
        int age = jsonNode.get("age").asInt();

        long targetId = 0;
        JsonNode targetNode = jsonNode.get("targetId");
        if (targetNode != null && !targetNode.isNull()) {
            targetId = targetNode.asLong();
        }
        System.out.println(tick);
        return new EntityHistoryDto(
                tick,
                state,
                targetId,
                entityId,
                age
        );
    }

    public HistoryEvent stringToSimulationEventDto(String value) {
        return objectMapper.readValue(value, HistoryEvent.class);
    }

    private Map<String, String> entityToMap(RedisEntity entity, StateEnum state) {
        Map<String, String> result = new HashMap<>();
        result.computeIfPresent("targetId",
                (k, targetId) -> targetId.equals("null") ? null : String.valueOf(entity.getTargetId()));
        result.put("x", String.valueOf(entity.getX()));
        result.put("y", String.valueOf(entity.getY()));
        return result;
    }

    public Map<byte[], byte[]> historyEventToMap(HistoryEvent event) {
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

    public Map<byte[], byte[]> streamEventToMap(StreamEvent event) {
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

    public HistoryEvent entityToHistoryEvent(RedisEntity entity) {
        return new HistoryEvent(
                entity.getState(),
                entity.getId(),
                tickManager.currentTick(),
                entity.getTargetId() == null ? 0 : entity.getTargetId(),
                entity.getAge()
        );
    }

    public StreamEvent entityToStreamEvent(RedisEntity entity) {
        return new StreamEvent(
                entity.getState(),
                entity.getId(),
                tickManager.currentTick(),
                entity.getTargetId() == null ? 0 : entity.getTargetId(),
                entity.getAge()
        );
    }
}
