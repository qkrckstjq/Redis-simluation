package com.example.world.service;

import com.example.world.entity.EntitySnapshotDto;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.Tick;
import com.example.world.entity.log.PerformanceLog;
import com.example.world.entity.log.PerformanceMetric;
import com.example.world.repository.RedisRepository;
import com.example.world.stream.EventMapper;
import com.example.world.stream.HistoryService;
import com.example.world.stream.StreamService;
import com.example.world.websocket.WebSocketMapper;
import com.example.world.websocket.WebSocketService;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class AsyncService {
    private final RedisService entityClusterService;
    private final RedisService inMemoryRedisService;
    private final StreamService streamService;
    private final EventMapper eventMapper;
    private final RedisRepository redisRepository;
    private final WebSocketMapper webSocketMapper;
    private final WebSocketService webSocketService;
    private final Executor streamExecutor;
    private final Executor historyExecutor;
    private final Executor websocketExecutor;
    private final Executor redisUpdateExecutor;
    private final Executor spawnExecutor;
    private final PerformanceLog performanceLog;
    private final PerformanceMetric metric;
    private final HistoryService historyService;
    private final TickManager tickManager;

    public AsyncService(
            @Qualifier("entityClusterService")
            RedisService entityClusterService,
            @Qualifier("inMemoryRedisService")
            RedisService inMemoryRedisService,
            StreamService streamService,
            EventMapper eventMapper,
            RedisRepository redisRepository,
            WebSocketMapper webSocketMapper,
            WebSocketService webSocketService,
            @Qualifier("streamExecutor")
            Executor streamExecutor,
            @Qualifier("historyExecutor")
            Executor historyExecutor,
            @Qualifier("webSocketExecutor")
            Executor websocketExecutor,
            @Qualifier("redisUpdateExecutor")
            Executor redisUpdateExecutor,
            @Qualifier("spawnExecutor")
            Executor spawnExecutor,
            PerformanceLog performanceLog,
            HistoryService historyService,
            TickManager tickManager
    ) {
        this.entityClusterService = entityClusterService;
        this.inMemoryRedisService = inMemoryRedisService;
        this.streamService = streamService;
        this.eventMapper = eventMapper;
        this.redisRepository = redisRepository;
        this.webSocketMapper = webSocketMapper;
        this.webSocketService = webSocketService;
        this.streamExecutor = streamExecutor;
        this.historyExecutor = historyExecutor;
        this.websocketExecutor = websocketExecutor;
        this.redisUpdateExecutor = redisUpdateExecutor;
        this.spawnExecutor = spawnExecutor;
        this.performanceLog = performanceLog;
        this.metric = performanceLog.getMetric();
        this.historyService = historyService;
        this.tickManager = tickManager;
    }

    @Timed(value = "simulation.entity.history")
    public CompletableFuture<Void> addHistory(List<RedisEntity> entityList) {
        return CompletableFuture.runAsync(() -> {
            long start = System.nanoTime();
            redisRepository.requestPipeLine(
                    historyService.addHistoryEntities(
                            eventMapper.entitiesToHistoryEvents(entityList)
                    )
            );

            metric.setSaveHistory(System.nanoTime() - start);
        }, historyExecutor);
    }

    @Timed(value = "simulation.stream.flush")
    public CompletableFuture<Void> streamPublish(List<RedisEntity> entityList) {
        return CompletableFuture.runAsync(() -> {
            long start = System.nanoTime();
            redisRepository.requestPipeLine(
                    streamService.publishStreamEvents(
                            eventMapper.entitiesToStreamEvents(entityList)
                    )
            );

            metric.setStreamPublish(System.nanoTime() - start);
        }, streamExecutor);
    }

    @Timed(value = "simulation.websocket.flush")
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
            metric.setWebsocketSend(System.nanoTime() - start);
        }, websocketExecutor);
    }

    @Timed(value = "simulation.redis.update")
    public CompletableFuture<Void> redisUpdateEntities(List<RedisEntity> entityList) {
        return CompletableFuture.runAsync(() -> {
            long start = System.nanoTime();

            redisRepository.requestPipeLine(
                    entityClusterService.updateEntitiesPipe(entityList)
            );

            metric.setRedisUpdate(System.nanoTime() - start);
        }, redisUpdateExecutor);
    }

    @Timed(value = "simulation.entity.spawn")
    public CompletableFuture<Void> spawnEntities(List<RedisEntity> spawnList, Long nextEntityId) {
        return CompletableFuture.runAsync(() -> {
            long start = System.nanoTime();
            redisRepository.requestPipeLine(
                    entityClusterService.saveSpawnEntities(spawnList, nextEntityId)
            );

            metric.setAddSpawnEntities(System.nanoTime() - start);
        }, spawnExecutor);
    }

    @Timed(value = "simulation.inmemory.update")
    public CompletableFuture<Void> redisUpdateEntitiesInMemory(
            List<RedisEntity> entities
    ) {
        return CompletableFuture.runAsync(() -> {
            long start = System.nanoTime();

            redisRepository.requestPipeLine(inMemoryRedisService.updateEntitiesPipe(entities));

            metric.setRedisUpdate(System.nanoTime() - start);
        }, redisUpdateExecutor);
    }

    @Timed(value = "simulation.inmemory.hash.flush")
    public CompletableFuture<Void> redisHashFlush(
            List<RedisEntity> entities
    ) {
        return CompletableFuture.runAsync(() -> {
            long start = System.nanoTime();

            redisRepository.requestPipeLine(inMemoryRedisService.hashFlush(entities));

            System.out.printf("[Async] Redis HashFlush    : %d ms%n",
                    (System.nanoTime() - start) / 1_000_000);
        }, redisUpdateExecutor);
    }

    @Timed(value = "simulation.tick.update")
    public void updateTick() {
        CompletableFuture.runAsync(() -> {
            redisRepository.setTick(String.valueOf(tickManager.currentTick()));
        });
    }
}
