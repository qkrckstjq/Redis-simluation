package com.example.world.stream;

import com.example.world.constants.RedisKeys;
import com.example.world.entity.EntityHistoryDto;
import com.example.world.entity.SimulationEvent;
import com.example.world.repository.RedisRepository;
import com.example.world.service.EventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
}