package com.example.world.service;

import com.example.world.entity.*;
import com.example.world.repository.RedisRepository;
import com.example.world.websocket.WebSocketMapper;
import com.example.world.websocket.WebSocketService;
import com.example.world.stream.StreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EntityService {
    private final RedisRepository redisRepository;
    private final RedisService redisService;
    private final LuaScriptService luaScriptService;
    private final WebSocketService webSocketService;
    private final AiDecisionService aiDecisionService;
    private final StreamService streamService;
    private final EventMapper eventMapper;
    private final WebSocketMapper webSocketMapper;
    private final EntityMapper entityMapper;
    private final GeoService geoService;
    private final BehaviorService behaviorService;
    private final Random random = new Random();
    private final int BATCH_SIZE = 2000;
    private final double SCALE = 1000;

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

    private void processBatch(List<String> ids) {
        //get all entities by pipeLine(Redis)
        List<Object> hGetAllEntities = redisRepository.responsePipeLine(redisService.getEntityIds(ids));
        List<RedisEntity> entityList = entityMapper.objectsToRedisEntities(hGetAllEntities);
        Map<Long, RedisEntity> entityMap = entityMapper.entitiesToHashMap(entityList);
        //------------------------------//

        //get all nearby entities
        List<Object> geoResults = redisRepository.responsePipeLine(geoService.getNearByIds(entityList, 10));
        Map<Long, List<RedisEntity>> nearEntities = entityMapper.geoSearchNearbyResultToIds(entityList, geoResults, entityMap);
        //------------------------------//

        //move with AI
        aiDecisionService.decideState(entityList, nearEntities);
        //

        //check nextMoves can move\
        List<NextMove> nextMoves = behaviorService.decideMoves(entityList, entityMap);
        //

        //get all collisionIds each entities
        List<Object> geoPosResults = redisRepository.responsePipeLine(geoService.getCollisionIds(nextMoves, 0.2));
        Map<Long, List<Long>> collisionResults = entityMapper.geoSearchCollisionsToIds(geoPosResults, nextMoves);
        behaviorService.moveWithCollision(nextMoves, collisionResults);
        //------------------------------//


        //put updateEntities by pipeLine(Redis)
        redisRepository.requestPipeLine(luaScriptService.updateEntitiesPipe(entityList));
        //------------------------------//

        //publish entities event to redis stream
        redisRepository.requestPipeLine(streamService.publish(eventMapper.entitiesToEvents(entityList)));
        //


//        //get all nearby entities
//        List<Object> geoResults = redisRepository.responsePipeLine(geoService.getNearByIds(entities, 10));
        List<EntitySnapshotDto> snapshotDtoList = webSocketMapper.geoSearchResultsToEntitiesSnapShotDtos(entityList, geoResults);
//        //------------------------------//



        Tick tick = webSocketMapper.redisEntitiesToTick(snapshotDtoList);
        webSocketService.sendSnapShots(tick);
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

    public static boolean isDead(RedisEntity entity) {
        return entity.getHp() < 0;
    }
}
