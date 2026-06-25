package com.example.world.service;

import com.example.world.entity.EntitySnapshotDto;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.Tick;
import com.example.world.repository.RedisRepository;
import com.example.world.stream.StreamService;
import com.example.world.websocket.WebSocketMapper;
import com.example.world.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AsyncService {

    private final StreamService streamService;
    private final EventMapper eventMapper;
    private final RedisRepository redisRepository;
    private final WebSocketMapper webSocketMapper;
    private final WebSocketService webSocketService;

    @Async("streamExecutor")
    public void publish(List<RedisEntity> entityList) {

        redisRepository.requestPipeLine(
                streamService.publish(
                        eventMapper.entitiesToEvents(entityList)
                )
        );
    }

    @Async("webSocketExecutor")
    public void sendSnapshots(List<EntitySnapshotDto> snapshotDtoList) {

        Tick tick = webSocketMapper.redisEntitiesToTick(snapshotDtoList);

        webSocketService.sendSnapShots(tick);
    }
}
