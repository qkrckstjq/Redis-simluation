package com.example.world.service.batch;

import com.example.world.entity.EntitySnapshotDto;
import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.Tick;
import com.example.world.repository.RedisRepository;
import com.example.world.service.*;
import com.example.world.service.ai.AiDecisionService;
import com.example.world.service.inmemory.EntityManager;
import com.example.world.stream.StreamService;
import com.example.world.websocket.WebSocketMapper;
import com.example.world.websocket.WebSocketService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
    private final Random random = new Random();
    public static final int BATCH_SIZE = 100000;
    private final double SCALE = 1000;
    private final AsyncService asyncService;
    private final EntityManager entityManager;

    public BatchProcessor(
            RedisRepository redisRepository,
            WebSocketService webSocketService,
            AiDecisionService aiDecisionService,
            StreamService streamService,
            EventMapper eventMapper,
            WebSocketMapper webSocketMapper,
            EntityMapper entityMapper,
            BehaviorService behaviorService,
            RedisService entityClusterService,
            AsyncService asyncService,
            EntityManager entityManager
    ) {
        this.redisRepository = redisRepository;
        this.webSocketService = webSocketService;
        this.aiDecisionService = aiDecisionService;
        this.streamService = streamService;
        this.eventMapper = eventMapper;
        this.webSocketMapper = webSocketMapper;
        this.entityMapper = entityMapper;
        this.behaviorService = behaviorService;
        this.redisService = entityClusterService;
        this.asyncService = asyncService;
        this.entityManager = entityManager;
    }

    public void processSync(List<String> ids) {
        long totalStart = System.nanoTime();
        long checkpoint = totalStart;

        List<Object> hGetAllEntities = redisRepository.responsePipeLine(redisService.getEntityIds(ids));
        List<RedisEntity> entityList = EntityMapper.objectsToRedisEntities(hGetAllEntities);
        Map<Long, RedisEntity> entityMap = entityMapper.entitiesToHashMap(entityList);
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
        System.out.printf("[1] Entity Read         : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        List<RedisEntity> noneTargetEntities = entityList.stream()
                .filter(entity -> !redisService.skipGeoSearch(entity))
                .toList();

        checkpoint = System.nanoTime();
        List<Object> geoResults = redisRepository.responsePipeLine(redisService.getNearByIds(noneTargetEntities, 10));
        System.out.printf("[2] Nearby Search       : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        long asyncStart = System.nanoTime();
        CompletableFuture<Void> websocketFuture = asyncService.mappingAndSend(entityList, geoResults, noneTargetEntities);
        CompletableFuture<Void> streamFuture = asyncService.publish(entityList);

        checkpoint = System.nanoTime();
        Map<Long, List<RedisEntity>> nearEntities = redisService.geoSearchNearbyResultToIds(noneTargetEntities, geoResults, entityMap);
        System.out.printf("[3] Mapping nearby      : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);

        checkpoint = System.nanoTime();
        aiDecisionService.decideState(entityList, nearEntities, entityMap);
        System.out.printf("[4] AI Decision         : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        List<RedisEntity> spawnList = new ArrayList<>();
        List<NextMove> nextMoves = behaviorService.decideMoves(entityList, entityMap, nearEntities, spawnList);
        System.out.printf("[5] Move Decision with collision : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        CompletableFuture<Void> spawnFuture = asyncService.spawnEntities(spawnList);


        checkpoint = System.nanoTime();
        behaviorService.moveWithCollision(nextMoves, null);
        System.out.printf("[7] Apply Move          : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);

        CompletableFuture<Void> redisUpdateFuture = asyncService.redisUpdateEntities(entityList);

        CompletableFuture.allOf(
//                websocketFuture,
//                streamFuture,
                redisUpdateFuture,
                spawnFuture
        ).join();

        System.out.printf("[Async] Total             : %d ms%n",
                (System.nanoTime() - asyncStart) / 1_000_000);

        System.out.printf("TOTAL                 : %d ms%n%n",
                (System.nanoTime() - totalStart) / 1_000_000);
    }
}
