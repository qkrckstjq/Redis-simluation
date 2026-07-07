package com.example.world.service.batch;

import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.repository.RedisRepository;
import com.example.world.service.AsyncService;
import com.example.world.service.BehaviorService;
import com.example.world.service.EntityMapper;
import com.example.world.service.RedisService;
import com.example.world.service.ai.AiDecisionService;
import com.example.world.service.inmemory.EntityManager;
import com.example.world.websocket.WebSocketMapper;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.cumulative.CumulativeTimer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Getter
public class InMemoryProcess implements Process {
    private final RedisRepository redisRepository;
    private final RedisService redisService;
    private final EntityMapper entityMapper;
    private final AiDecisionService aiDecisionService;
    private final BehaviorService behaviorService;
    private final AsyncService asyncService;
    private final EntityManager entityManager;
    private final WebSocketMapper webSocketMapper;
    private CompletableFuture<Void> saveSpawnEntities;
    private CompletableFuture<Void> saveUpdateEntities;
    private CompletableFuture<Void> flushStreamEntities;
    private CompletableFuture<Void> flushWebsocketEntities;


    @Override
    @Timed("simulation.entity.read")
    public void setEntities(List<String> ids) {
        entityManager.initEntityList();
        if(webSocketMapper.getTick() % 10 == 0) asyncService.redisHashFlush(entityManager.getEntityList());
    }

    @Override
    @Timed(value = "simulation.entity.mapping")
    public void setEntitiesMap() {
//        entityManager.getEntityMap();
    }

    @Override
    @Timed(value = "simulation.entity.skip_geo_search")
    public void skipGeoSearchEntities() {
        entityManager.setNoneTargetEntities(
                entityManager.getEntityList().stream()
                .filter(entity -> !redisService.skipGeoSearch(entity))
                .toList());
    }

    @Override
    @Timed(value = "simulation.geo.search")
    public void getGeoSearch() {
        entityManager.setGeoResults(
                redisRepository.responsePipeLine(
                        redisService.getNearByIds(
                                entityManager.getNoneTargetEntities(), 10
                        )
                )
        );
    }

    @Override
    @Timed(value = "simulation.geo.mapping")
    public void mappingNearByEntities() {
        entityManager.setNearEntities(
                redisService.geoSearchNearbyResultToIds(
                        entityManager.getNoneTargetEntities(),
                        entityManager.getGeoResults(),
                        entityManager.getEntityMap()
                )
        );
    }

    @Override
    @Timed(value = "simulation.ai.decision")
    public void aiDecision() {
        aiDecisionService.decideState(
                entityManager.getEntityList(),
                entityManager.getNearEntities(),
                entityManager.getEntityMap()
        );
    }

    @Override
    @Timed(value = "simulation.move.next")
    public void setNextMove() {
        entityManager.setNextMoves(
                behaviorService.decideMoves(
                        entityManager.getEntityList(),
                        entityManager.getEntityMap(),
                        entityManager.getNearEntities(),
                        entityManager.getSpawnEntities()
                )
        );
    }

    @Override
    public void saveSpawnEntities() {
        List<RedisEntity> spawnEntities = entityManager.getSpawnEntities();
        Long nextEntityId = redisRepository.allocateIds(spawnEntities.size());
        saveSpawnEntities = asyncService.spawnEntities(spawnEntities, nextEntityId);
        entityManager.addAllEntities(spawnEntities, nextEntityId);
    }

    @Override
    public void saveUpdateEntities() {
        saveUpdateEntities = asyncService.redisUpdateEntitiesInMemory(entityManager.getEntityList());
    }

    @Override
    public void flushStreamEntities() {
        flushStreamEntities = asyncService.publish(entityManager.getEntityList());
    }

    @Override
    public void flushWebSocketEntities() {
        flushWebsocketEntities = asyncService.mappingAndSend(
                entityManager.getEntityList(),
                entityManager.getGeoResults(),
                entityManager.getNoneTargetEntities()
        );
    }

    @Override
    @Timed(value = "simulation.collision.move")
    public void moveWithCollision() {
        entityManager.initSpawnEntities();
        behaviorService.moveWithCollision(
                entityManager.getNextMoves(),
                null
        );
    }

    @Override
    @Timed(value = "simulation.process.end")
    public void endProcess() {
        CompletableFuture.allOf(
//                websocketFuture,
//                streamFuture,
                saveUpdateEntities,
                saveSpawnEntities
        ).join();
    }
}
