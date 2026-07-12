package com.example.world.stream;

import com.example.world.constants.RedisKeys;
import com.example.world.entity.EntityHistoryDto;
import com.example.world.entity.HistoryEvent;
import com.example.world.repository.RedisRepository;
import com.example.world.util.ByteTypeConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class HistoryService {
    private final RedisRepository redisRepository;
    private final EventMapper eventMapper;

    public void save(HistoryEvent event) {
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

    public Consumer<RedisConnection> addHistoryEntities(List<HistoryEvent> simulationEvents) {
        return connection -> {
            for(HistoryEvent event : simulationEvents) {
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