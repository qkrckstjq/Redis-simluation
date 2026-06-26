package com.example.world.service;

import com.example.world.entity.EntitySnapshotDto;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.Tick;
import com.example.world.repository.RedisRepository;
import com.example.world.stream.StreamService;
import com.example.world.websocket.WebSocketMapper;
import com.example.world.websocket.WebSocketService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class AsyncService {
    private final RedisService redisService;
    private final StreamService streamService;
    private final EventMapper eventMapper;
    private final RedisRepository redisRepository;
    private final WebSocketMapper webSocketMapper;
    private final WebSocketService webSocketService;
    private final Executor streamExecutor;
    private final Executor websocketExecutor;
    private final Executor redisUpdateExecutor;
    private final Executor spawnExecutor;

    public AsyncService(
            RedisService entityClusterService,
            StreamService streamService,
            EventMapper eventMapper,
            RedisRepository redisRepository,
            WebSocketMapper webSocketMapper,
            WebSocketService webSocketService,
            @Qualifier("streamExecutor")
            Executor streamExecutor,
            @Qualifier("webSocketExecutor")
            Executor websocketExecutor,
            @Qualifier("redisUpdateExecutor")
            Executor redisUpdateExecutor,
            @Qualifier("spawnExecutor")
            Executor spawnExecutor
    ) {
        this.redisService = entityClusterService;
        this.streamService = streamService;
        this.eventMapper = eventMapper;
        this.redisRepository = redisRepository;
        this.webSocketMapper = webSocketMapper;
        this.webSocketService = webSocketService;
        this.streamExecutor = streamExecutor;
        this.websocketExecutor = websocketExecutor;
        this.redisUpdateExecutor = redisUpdateExecutor;
        this.spawnExecutor = spawnExecutor;
    }

    public CompletableFuture<Void> publish(List<RedisEntity> entityList) {
        return CompletableFuture.runAsync(() -> {
            long start = System.nanoTime();

            redisRepository.requestPipeLine(
                    streamService.publish(
                            eventMapper.entitiesToEvents(entityList)
                    )
            );

            System.out.printf("[Async] Stream Publish     : %d ms%n",
                    (System.nanoTime() - start) / 1_000_000);
        }, streamExecutor);
    }

    public CompletableFuture<Void> mappingAndSend(
            List<RedisEntity> entities,
            List<Object> geoResults,
            List<RedisEntity> noneTargetEntities
    ) {
        return CompletableFuture.runAsync(() -> {
            long start = System.nanoTime();

            List<EntitySnapshotDto> snapshotDtoList =
                    webSocketMapper.geoSearchResultsToClusterEntitiesSnapShotDtos(
                            entities,
                            geoResults,
                            noneTargetEntities
                    );

            Tick tick = webSocketMapper.redisEntitiesToTick(snapshotDtoList);

            webSocketService.sendSnapShots(tick);

            System.out.printf("[Async] WebSocket Send     : %d ms%n",
                    (System.nanoTime() - start) / 1_000_000);
        }, websocketExecutor);
    }

    public CompletableFuture<Void> redisUpdateEntities(List<RedisEntity> entityList) {
        return CompletableFuture.runAsync(() -> {
            long start = System.nanoTime();

            redisRepository.requestPipeLine(
                    redisService.updateEntitiesPipe(entityList)
            );

            System.out.printf("[Async] Redis Update       : %d ms%n",
                    (System.nanoTime() - start) / 1_000_000);
        }, redisUpdateExecutor);
    }

    public CompletableFuture<Void> spawnEntities(List<RedisEntity> spawnList) {
        return CompletableFuture.runAsync(() -> {
            long start = System.nanoTime();

            Long nextEntityId = redisRepository.allocateIds(spawnList.size());
            redisRepository.requestPipeLine(
                    redisService.saveSpawnEntities(spawnList, nextEntityId)
            );

            System.out.printf("[Async] Spawn Entities     : %d ms%n",
                    (System.nanoTime() - start) / 1_000_000);
        }, spawnExecutor);
    }
}
