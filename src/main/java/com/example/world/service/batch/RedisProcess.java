package com.example.world.service.batch;

import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.repository.RedisRepository;
import com.example.world.service.AsyncService;
import com.example.world.service.BehaviorService;
import com.example.world.service.EntityMapper;
import com.example.world.service.RedisService;
import com.example.world.service.ai.AiDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class RedisProcess implements Process{
    private final RedisRepository redisRepository;
    private final RedisService redisService;
    private final EntityMapper entityMapper;
    private final AiDecisionService aiDecisionService;
    private final BehaviorService behaviorService;
    private final AsyncService asyncService;
    private List<RedisEntity> entities = new ArrayList<>();
    private Map<Long, RedisEntity> entityMap = new HashMap<>();
    private List<RedisEntity> spawnEntities = new ArrayList<>();
    private List<RedisEntity> noneTargetEntities = new ArrayList<>();
    private List<Object> geoResults = new ArrayList<>();
    private Map<Long, List<RedisEntity>> nearEntities = new HashMap<>();
    private List<NextMove> nextMoves = new ArrayList<>();
    private CompletableFuture<Void> saveSpawnEntities;
    private CompletableFuture<Void> saveUpdateEntities;
    private CompletableFuture<Void> flushStreamEntities;
    private CompletableFuture<Void> flushWebsocketEntities;

    @Override
    public void setEntities(List<String> ids) {
        List<Object> hGetAllEntities = redisRepository.responsePipeLine(redisService.getEntityIds(ids));
        entities = EntityMapper.objectsToRedisEntities(hGetAllEntities);
    }

    @Override
    public void setEntitiesMap() {
        entityMap = entityMapper.entitiesToHashMap(entities);
    }

    @Override
    public void skipGeoSearchEntities() {
        noneTargetEntities = entities.stream()
                .filter(entity -> !redisService.skipGeoSearch(entity))
                .toList();
    }

    @Override
    public void getGeoSearch() {
        geoResults = redisRepository.responsePipeLine(redisService.getNearByIds(entities, 10));
    }

    @Override
    public void mappingNearByEntities() {
        nearEntities = redisService.geoSearchNearbyResultToIds(noneTargetEntities, geoResults, entityMap);
    }

    @Override
    public void aiDecision() {
        aiDecisionService.decideState(entities, nearEntities, entityMap);
    }

    @Override
    public void setNextMove() {
        nextMoves = behaviorService.decideMoves(entities, entityMap, nearEntities, spawnEntities);
    }

    @Override
    public void saveSpawnEntities() {
        Long nextEntityId = redisRepository.allocateIds(spawnEntities.size());
        saveSpawnEntities = asyncService.spawnEntities(spawnEntities, nextEntityId);
    }

    @Override
    public void saveUpdateEntities() {
        saveUpdateEntities = asyncService.redisUpdateEntities(entities);
    }

    @Override
    public void flushStreamEntities() {
        flushStreamEntities = asyncService.addHistory(entities);
    }

    @Override
    public void flushWebSocketEntities() {
        flushWebsocketEntities = asyncService.mappingAndSend(entities, geoResults, noneTargetEntities);
    }

    @Override
    public void moveWithCollision() {
        spawnEntities = new ArrayList<>();
        behaviorService.moveWithCollision(nextMoves, null);
    }

    @Override
    public void endProcess() {
        CompletableFuture.allOf(
//                websocketFuture,
//                streamFuture,
                saveUpdateEntities,
                saveSpawnEntities
        ).join();
    }
}

