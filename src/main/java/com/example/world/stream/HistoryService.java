package com.example.world.stream;

import com.example.world.constants.RedisKeys;
import com.example.world.entity.EntityHistoryDto;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.SimulationEvent;
import com.example.world.repository.RedisRepository;
import com.example.world.service.EventMapper;
import com.example.world.util.ByteTypeConverter;
import io.lettuce.core.StrAlgoArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class HistoryService {
    private final RedisRepository redisRepository;
    private final EventMapper eventMapper;

    public void save(SimulationEvent event) {
        String key = RedisKeys.HISTORY_ENTITY_ + event.getEntityId();
        String value = eventMapper.stringToValue(event);

        redisRepository.lPush(key, value);
        redisRepository.listTrim(key);
    }

    public List<EntityHistoryDto> getEntityHistory(Long entityId) {
        String key = RedisKeys.HISTORY_ENTITY_ + entityId;
        List<String> entityHistoryList = redisRepository.lRange(key, 0, -1);
        return entityHistoryList.stream()
                .map(eventMapper::stringToEntityHistoryDto)
                .toList();
    }

    public Consumer<RedisConnection> addHistoryEntities(List<SimulationEvent> simulationEvents) {
        return connection -> {
            for(SimulationEvent event : simulationEvents) {
                String key = RedisKeys.HISTORY_ENTITY_ + event.getEntityId();
                String value = eventMapper.stringToValue(event);

                byte[] byteKey = ByteTypeConverter.stringToByte(key);
                byte[] byteValue = ByteTypeConverter.stringToByte(value);

                connection.listCommands().lPush(byteKey, byteValue);
                connection.listCommands().lTrim(byteKey, 0, 99);
            }
        };
    }
}