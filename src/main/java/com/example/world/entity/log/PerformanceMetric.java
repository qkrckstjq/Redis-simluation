package com.example.world.entity.log;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceMetric {
    private long entityRead;
    private long geoSearch;
    private long mappingNearBy;
    private long aiDecision;
    private long collision;
    private long move;
    private long addSpawnEntities;
    private long applyMove;
    private long redisUpdate;
    private long streamPublish;
    private long websocketSend;
    private long total;
}
