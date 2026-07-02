package com.example.world.entity.log;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
public class PerformanceMetric {
    private long entityRead;
    private long mappingSkipGeo;
    private long geoSearch;
    private long mappingNearBy;
    private long aiDecision;
    private long moveWithCollision;
    private long addSpawnEntities;
    private long applyMove;
    private long redisUpdate;
    private long streamPublish;
    private long websocketSend;
    private long asyncTotal;
    private long total;
}
