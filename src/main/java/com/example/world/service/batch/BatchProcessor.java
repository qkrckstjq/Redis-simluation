package com.example.world.service.batch;

import com.example.world.entity.EntitySnapshotDto;
import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.Tick;
import com.example.world.entity.log.PerformanceLog;
import com.example.world.entity.log.PerformanceLogger;
import com.example.world.entity.log.PerformanceMetric;
import com.example.world.repository.RedisRepository;
import com.example.world.service.*;
import com.example.world.service.ai.AiDecisionService;
import com.example.world.service.inmemory.EntityManager;
import com.example.world.service.inmemory.InMemoryRedisService;
import com.example.world.stream.StreamService;
import com.example.world.websocket.WebSocketMapper;
import com.example.world.websocket.WebSocketService;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class BatchProcessor {
    private final RedisRepository redisRepository;
    private final WebSocketService webSocketService;
    private final AiDecisionService aiDecisionService;
    private final StreamService streamService;
    private final EventMapper eventMapper;
    private final WebSocketMapper webSocketMapper;
    private final EntityMapper entityMapper;
    private final BehaviorService behaviorService;
    private final RedisService redisService;
    private final InMemoryRedisService inMemoryRedisService;
    public static final int BATCH_SIZE = 100000;
    private final AsyncService asyncService;
    private final EntityManager entityManager;
    private final PerformanceLog performanceLog;
    private final PerformanceMetric metric;
    private final MeterRegistry meterRegistry;
    private final Process process;

    public BatchProcessor(
            RedisRepository redisRepository,
            WebSocketService webSocketService,
            AiDecisionService aiDecisionService,
            StreamService streamService,
            EventMapper eventMapper,
            WebSocketMapper webSocketMapper,
            EntityMapper entityMapper,
            BehaviorService behaviorService,
            RedisService redisService,
            InMemoryRedisService inMemoryRedisService,
            AsyncService asyncService,
            EntityManager entityManager,
            PerformanceLog performanceLog,
            MeterRegistry meterRegistry,
            Process inMemoryProcess
    ) {
        this.redisRepository = redisRepository;
        this.webSocketService = webSocketService;
        this.aiDecisionService = aiDecisionService;
        this.streamService = streamService;
        this.eventMapper = eventMapper;
        this.webSocketMapper = webSocketMapper;
        this.entityMapper = entityMapper;
        this.behaviorService = behaviorService;
        this.redisService = redisService;
        this.inMemoryRedisService = inMemoryRedisService;
        this.asyncService = asyncService;
        this.entityManager = entityManager;
        this.performanceLog = performanceLog;
        this.metric = performanceLog.getMetric();
        this.meterRegistry = meterRegistry;
        this.process = inMemoryProcess;
    }

    public void processSync(List<String> ids) {
        long totalStart = System.nanoTime();
        long checkpoint = totalStart;

        List<Object> hGetAllEntities = redisRepository.responsePipeLine(redisService.getEntityIds(ids));
        entityManager.setEntityList(EntityMapper.objectsToRedisEntities(hGetAllEntities));
        List<RedisEntity> entityList = entityManager.getEntityList();
        entityManager.entityListToMap();
        Map<Long, RedisEntity> entityMap = entityManager.getEntityMap();
        System.out.printf("[1] Entity Read         : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        List<RedisEntity> noneTargetEntities = entityList.stream()
                .filter(entity -> !redisService.skipGeoSearch(entity))
                .toList();

        checkpoint = System.nanoTime();
        List<Object> geoResults = redisRepository.responsePipeLine(redisService.getNearByIds(noneTargetEntities, 10));
        System.out.printf("[2] Nearby Search       : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        Map<Long, List<RedisEntity>> nearEntities = redisService.geoSearchNearbyResultToIds(noneTargetEntities, geoResults, entityMap);
        System.out.printf("[3] Mapping nearby      : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        List<EntitySnapshotDto> snapshotDtoList = webSocketMapper.geoSearchResultsToClusterEntitiesSnapShotDtos(
                entityList,
                geoResults,
                noneTargetEntities
        );
        System.out.printf("[4] Snapshot Build      : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        Tick tick = webSocketMapper.redisEntitiesToTick(snapshotDtoList);
        webSocketService.sendSnapShots(tick);
        System.out.printf("[5] WebSocket Send     : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        aiDecisionService.decideState(entityList, nearEntities, entityMap);
        System.out.printf("[6] AI Decision         : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        List<RedisEntity> spawnList = new ArrayList<>();
        List<NextMove> nextMoves = behaviorService.decideMoves(entityList, entityMap, nearEntities, spawnList);
        System.out.printf("[7] Move Decision with collision : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        Long nextEntityId = redisRepository.allocateIds(spawnList.size());
        redisRepository.requestPipeLine(redisService.saveSpawnEntities(spawnList, nextEntityId));
        System.out.printf("[8] save Spawn Entities : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


//        checkpoint = System.nanoTime();
//        List<Object> geoPosResults = redisRepository.responsePipeLine(redisService.getCollisionIds(nextMoves, 0.2));
//        Map<Long, List<Long>> collisionResults = entityMapper.geoSearchCollisionsToIds(geoPosResults, nextMoves);
//        System.out.printf("[5] Collision Search    : %d ms%n",
//                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        behaviorService.moveWithCollision(nextMoves, null);
        System.out.printf("[9] Apply Move          : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        redisRepository.requestPipeLine(redisService.updateEntitiesPipe(entityList));
        System.out.printf("[10] Redis Update        : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        redisRepository.requestPipeLine(streamService.publish(eventMapper.entitiesToEvents(entityList)));
        System.out.printf("[11] Stream Publish      : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);

        System.out.printf("TOTAL                 : %d ms%n%n",
                (System.nanoTime() - totalStart) / 1_000_000);
    }

    public void processAsync(List<String> ids) {
        long totalStart = System.nanoTime();
        long checkpoint = totalStart;

        List<Object> hGetAllEntities = redisRepository.responsePipeLine(redisService.getEntityIds(ids));
        List<RedisEntity> entityList = EntityMapper.objectsToRedisEntities(hGetAllEntities);
        Map<Long, RedisEntity> entityMap = entityMapper.entitiesToHashMap(entityList);
        metric.setEntityRead(System.nanoTime() - checkpoint);


        checkpoint = System.nanoTime();
        List<RedisEntity> noneTargetEntities = entityList.stream()
                .filter(entity -> !redisService.skipGeoSearch(entity))
                .toList();
        metric.setMappingSkipGeo(System.nanoTime() - checkpoint);


        checkpoint = System.nanoTime();
        List<Object> geoResults = redisRepository.responsePipeLine(redisService.getNearByIds(noneTargetEntities, 10));
        metric.setGeoSearch(System.nanoTime() - checkpoint);


        long asyncStart = System.nanoTime();
        CompletableFuture<Void> websocketFuture = asyncService.mappingAndSend(entityList, geoResults, noneTargetEntities);
        CompletableFuture<Void> streamFuture = asyncService.addHistory(entityList);

        checkpoint = System.nanoTime();
        Map<Long, List<RedisEntity>> nearEntities = redisService.geoSearchNearbyResultToIds(noneTargetEntities, geoResults, entityMap);
        metric.setMappingNearBy(System.nanoTime() - checkpoint);


        checkpoint = System.nanoTime();
        aiDecisionService.decideState(entityList, nearEntities, entityMap);
        metric.setAiDecision(System.nanoTime() - checkpoint);


        checkpoint = System.nanoTime();
        List<RedisEntity> spawnList = new ArrayList<>();
        List<NextMove> nextMoves = behaviorService.decideMoves(entityList, entityMap, nearEntities, spawnList);
        metric.setApplyMove(System.nanoTime() - checkpoint);


        Long nextEntityId = redisRepository.allocateIds(spawnList.size());
        CompletableFuture<Void> spawnFuture = asyncService.spawnEntities(spawnList, nextEntityId);


        checkpoint = System.nanoTime();
        behaviorService.moveWithCollision(nextMoves, null);
        metric.setMoveWithCollision(System.nanoTime() - checkpoint);


        CompletableFuture<Void> redisUpdateFuture = asyncService.redisUpdateEntities(entityList);

        CompletableFuture.allOf(
//                websocketFuture,
//                streamFuture,
                redisUpdateFuture,
                spawnFuture
        ).join();

        metric.setAsyncTotal(System.nanoTime() - asyncStart);
        metric.setTotal(System.nanoTime() - totalStart);
        PerformanceLogger.print(performanceLog);
    }

    @Timed(value = "simulation.process")
    public void processInMemoryAsync() {
        long totalStart = System.nanoTime();
        long checkpoint = totalStart;


        process.setEntities(null);
        process.setEntitiesMap();
        metric.setEntityRead((System.nanoTime() - checkpoint));



        checkpoint = System.nanoTime();
        process.skipGeoSearchEntities();
        metric.setMappingSkipGeo(System.nanoTime() - checkpoint);


        checkpoint = System.nanoTime();
        process.getGeoSearch();
        metric.setGeoSearch(System.nanoTime() - checkpoint);


        long asyncStart = System.nanoTime();
        process.flushWebSocketEntities();


        checkpoint = System.nanoTime();
        process.mappingNearByEntities();
        metric.setMappingNearBy(System.nanoTime() - checkpoint);


        checkpoint = System.nanoTime();
        process.aiDecision();
        metric.setAiDecision(System.nanoTime() - checkpoint);


        checkpoint = System.nanoTime();
        process.setNextMove();
        process.flushStreamEntities();
        metric.setMoveWithCollision(System.nanoTime() - checkpoint);


        checkpoint = System.nanoTime();
        process.saveSpawnEntities();


        checkpoint = System.nanoTime();
        process.moveWithCollision();
        metric.setApplyMove(System.nanoTime() - checkpoint);


        process.saveUpdateEntities();

        process.endProcess();
        metric.setAsyncTotal(System.nanoTime() - asyncStart);
        metric.setTotal(System.nanoTime() - totalStart);
        PerformanceLogger.print(performanceLog);
    }
}
