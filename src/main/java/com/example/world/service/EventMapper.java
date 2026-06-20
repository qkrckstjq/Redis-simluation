package com.example.world.service;

import com.example.world.entity.*;
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
        List<SimulationEvent> result = new ArrayList<>();
        Long curTick = webSocketMapper.getTick();
        entityList.forEach(entity -> {
            StateEnum entityState = entity.getState();
            if(entityState.equals(MOVE) || entityState.equals(IDLE) || entityState.equals(REST)) return;
            long entityId = entity.getId();
            Map<String, String> payLoad = entityToMap(entity, entityState);
            result.add(new SimulationEvent(entityState, curTick, entityId, payLoad));
        });
        return result;
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
}
