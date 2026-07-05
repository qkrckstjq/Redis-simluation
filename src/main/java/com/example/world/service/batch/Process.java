package com.example.world.service.batch;

import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import io.micrometer.core.annotation.Timed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Process {
    @Timed(value = "simulation.entity.read")
    public void setEntities(List<String> ids);

    @Timed(value = "simulation.entity.mapping")
    public void setEntitiesMap();

    @Timed(value = "simulation.entity.skip_geo_search")
    public void skipGeoSearchEntities();

    @Timed(value = "simulation.geo.search")
    public void getGeoSearch();

    @Timed(value = "simulation.geo.mapping")
    public void mappingNearByEntities();

    @Timed(value = "simulation.ai.decision")
    public void aiDecision();

    @Timed(value = "simulation.move.next")
    public void setNextMove();

    public void saveSpawnEntities();
    public void saveUpdateEntities();
    public void flushStreamEntities();
    public void flushWebSocketEntities();

    @Timed(value = "simulation.collision.move")
    public void moveWithCollision();
    public void endProcess();
}
