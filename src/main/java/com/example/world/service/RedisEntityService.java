package com.example.world.service;

import com.example.world.entity.RedisGeo;
import com.example.world.repository.RedisRepository;
import com.example.world.service.batch.BatchProcessor;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.world.service.batch.BatchProcessor.BATCH_SIZE;

@Service
public class EntityServiceImpl implements EntityService {
    private final RedisRepository redisRepository;
    private final RedisService redisService;
    private final BatchProcessor batchProcessor;

    public EntityServiceImpl(
        RedisRepository redisRepository,
        RedisService entityClusterService,
        BatchProcessor batchProcessor
    ) {
        this.redisRepository = redisRepository;
        this.redisService = entityClusterService;
        this.batchProcessor = batchProcessor;
    }

    public void createEntity(String type, String name, int hp, int x, int y) {
//        RedisEntity entity = new RedisEntity(type, name, hp, x, y);

    }

    public void createEntityAmount(String type, int count) {
        Long curId = redisRepository.getEntityId();
        redisRepository.requestPipeLine(redisService.saveNewEntities(type, curId + 1, count));
    }


    public void processTickListSync() {
        redisRepository.scanWithCursor(
                () -> redisRepository.scanWorldEntities(BATCH_SIZE),
                redisService.batchConsumer(BATCH_SIZE, batchProcessor::processSync));
    }

    public void processTickListAsync() {
        redisRepository.scanWithCursor(
                () -> redisRepository.scanWorldEntities(BATCH_SIZE),
                redisService.batchConsumer(BATCH_SIZE, batchProcessor::processAsync));
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
}
