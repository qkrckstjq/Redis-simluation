package com.example.world.service;

import com.example.world.entity.*;
import com.example.world.repository.RedisRepository;
import com.example.world.service.ai.AiDecisionService;
import com.example.world.websocket.WebSocketMapper;
import com.example.world.websocket.WebSocketService;
import com.example.world.stream.StreamService;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class EntityService {
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
    private final int BATCH_SIZE = 100000;
    private final double SCALE = 1000;

    public EntityService(
        RedisRepository redisRepository,
        WebSocketService webSocketService,
        AiDecisionService aiDecisionService,
        StreamService streamService,
        EventMapper eventMapper,
        WebSocketMapper webSocketMapper,
        EntityMapper entityMapper,
        BehaviorService behaviorService,
        RedisService entityClusterService
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
    }

    public void createEntity(String type, String name, int hp, int x, int y) {
//        RedisEntity entity = new RedisEntity(type, name, hp, x, y);

    }

    public void createEntityAmount(String type, int count) {
        Long curId = redisRepository.getEntityId();
        redisRepository.requestPipeLine(redisService.saveNewEntities(type, curId + 1, count));
    }


    public void processTickList() {
        redisRepository.scanWithCursor(
                () -> redisRepository.scanWorldEntities(BATCH_SIZE),
                redisService.batchConsumer(BATCH_SIZE, this::processBatch));
    }

//    private void processBatch(List<String> ids) {
//        //get all entities by pipeLine(Redis)
//        List<Object> hGetAllEntities = redisRepository.responsePipeLine(redisService.getEntityIds(ids));
//        List<RedisEntity> entityList = entityMapper.objectsToRedisEntities(hGetAllEntities);
//        Map<Long, RedisEntity> entityMap = entityMapper.entitiesToHashMap(entityList);
//        //------------------------------//
//
//        //get all nearby entities
//        List<Object> geoResults = redisRepository.responsePipeLine(redisService.getNearByIds(entityList, 10));
//        Map<Long, List<RedisEntity>> nearEntities = entityMapper.geoSearchNearbyResultToIds(entityList, geoResults, entityMap);
//        //------------------------------//
//
//        //move with AI
//        aiDecisionService.decideState(entityList, nearEntities);
//        //
//
//        //check nextMoves can move\
//        List<NextMove> nextMoves = behaviorService.decideMoves(entityList, entityMap);
//        //
//
//        //get all collisionIds each entities
//        List<Object> geoPosResults = redisRepository.responsePipeLine(redisService.getCollisionIds(nextMoves, 0.2));
//        Map<Long, List<Long>> collisionResults = entityMapper.geoSearchCollisionsToIds(geoPosResults, nextMoves);
//
//        //        //get all nearby entities
////        List<Object> geoResults = redisRepository.responsePipeLine(geoService.getNearByIds(entities, 10));
//        List<EntitySnapshotDto> snapshotDtoList = webSocketMapper.geoSearchResultsToClusterEntitiesSnapShotDtos(entityList, geoResults);
////        //------------------------------//
//
//
//        behaviorService.moveWithCollision(nextMoves, collisionResults); //entity의 x, y좌표 업데이트
//        //------------------------------//
//
//        //put updateEntities by pipeLine(Redis)
//        redisRepository.requestPipeLine(redisService.updateEntitiesPipe(entityList));
//        //------------------------------//
//
//        //publish entities event to redis stream
//        redisRepository.requestPipeLine(streamService.publish(eventMapper.entitiesToEvents(entityList)));
//        //
//
//        Tick tick = webSocketMapper.redisEntitiesToTick(snapshotDtoList);
//        webSocketService.sendSnapShots(tick);
//    }

    private void processBatch(List<String> ids) {

        long totalStart = System.nanoTime();
        long checkpoint = totalStart;

        List<Object> hGetAllEntities = redisRepository.responsePipeLine(redisService.getEntityIds(ids));
        List<RedisEntity> entityList = entityMapper.objectsToRedisEntities(hGetAllEntities);
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
        System.out.printf("[2] Mapping nearby      : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        aiDecisionService.decideState(entityList, nearEntities, entityMap);
        System.out.printf("[3] AI Decision         : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        List<RedisEntity> spawnList = new ArrayList<>();
        List<NextMove> nextMoves = behaviorService.decideMoves(entityList, entityMap, nearEntities, spawnList);
        System.out.printf("[4] Move Decision       : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        Long nextEntityId = redisRepository.allocateIds(spawnList.size());
        redisRepository.requestPipeLine(redisService.saveSpawnEntities(spawnList, nextEntityId));
        System.out.printf("[3] save Spawn Entities : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


//        checkpoint = System.nanoTime();
//        List<Object> geoPosResults = redisRepository.responsePipeLine(redisService.getCollisionIds(nextMoves, 0.2));
//        Map<Long, List<Long>> collisionResults = entityMapper.geoSearchCollisionsToIds(geoPosResults, nextMoves);
//        System.out.printf("[5] Collision Search    : %d ms%n",
//                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        List<EntitySnapshotDto> snapshotDtoList = webSocketMapper.geoSearchResultsToClusterEntitiesSnapShotDtos(
                entityList,
                geoResults,
                noneTargetEntities
        );
        System.out.printf("[6] Snapshot Build      : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        behaviorService.moveWithCollision(nextMoves, null);
        System.out.printf("[7] Apply Move          : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        redisRepository.requestPipeLine(redisService.updateEntitiesPipe(entityList));
        System.out.printf("[8] Redis Update        : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        redisRepository.requestPipeLine(streamService.publish(eventMapper.entitiesToEvents(entityList)));
        System.out.printf("[9] Stream Publish      : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);


        checkpoint = System.nanoTime();
        Tick tick = webSocketMapper.redisEntitiesToTick(snapshotDtoList);
        webSocketService.sendSnapShots(tick);
        System.out.printf("[10] WebSocket Send     : %d ms%n",
                (System.nanoTime() - checkpoint) / 1_000_000);

        System.out.printf("TOTAL                 : %d ms%n%n",
                (System.nanoTime() - totalStart) / 1_000_000);
    }

    public List<RedisGeo> getNearEntities(Long base, double range) {
        String entityKey = "entity:" + base;
        GeoResults<GeoLocation<String>> list = redisRepository.getNearEntities(entityKey, range);
        return list.getContent().stream()
                .map(entity -> {
                    GeoLocation<String> content = entity.getContent();
                    String member = content.getName();
                    Point point = content.getPoint();

                    double x = point.getX();
                    double y = point.getY();
                    return new RedisGeo(x, y, member);
                }).toList();
    }

    public Double getDistBetEntities(Long entity1, Long entity2) {
        String key1 = "entity:" + entity1;
        String key2 = "entity:" + entity2;
        return redisRepository.getDistBetEntities(key1, key2);
    }

//    public static boolean isDead(RedisEntity entity) {
//        return entity.getHp() < 0 || entity.getAge() >= 1000;
//    }
//
//    public static boolean isBreedReady(RedisEntity entity) {
//        if(entity.getType().equals(TypeEnum.SHEEP)) {
//            if(entity.getAge() < 400) {
//                entity.setBreedReady(false);
//                return false;
//            }
//
//            if(entity.getHp() >= 80 && entity.getStamina() >= 50) {
//                entity.setBreedReady(true);
//                return true;
//            }
//            entity.setBreedReady(false);
//            return false;
//        }
//
//        if(entity.getBreedReadyTick() > 0) {
//            entity.setBreedReady(true);
//            return true;
//        }
//        entity.setBreedReady(false);
//        return false;
//    }
//
//    public static void successHunt(RedisEntity wolf) {
//        wolf.setBreedReady(true);
//        wolf.setBreedReadyTick(100);
//        wolf.decreaseAge(1000);
//        wolf.setTargetId(null);
//    }
//
//    public static void healHp(RedisEntity entity) {
//        entity.increaseHp();
//    }
//
//    public static void afterBreed(RedisEntity entity) {
//        entity.setHp(entity.getHp() - 20);
//        entity.setStamina(entity.getStamina() - 40);
//        entity.setBreedReady(false);
//        entity.setBreedReadyTick(0);
//    }
}
