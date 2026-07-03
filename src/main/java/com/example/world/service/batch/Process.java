package com.example.world.service.batch;

import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Process {
    public void setEntities(List<String> ids);
    public void setEntitiesMap();
    public void skipGeoSearchEntities();
    public void getGeoSearch();
    public void mappingNearByEntities();
    public void aiDecision();
    public void setNextMove();
    public void saveSpawnEntities();
    public void saveUpdateEntities();
    public void flushStreamEntities();
    public void flushWebSocketEntities();
    public void moveWithCollision();
    public void endProcess();
}
