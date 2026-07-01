package com.example.world.entity.log;

import org.springframework.stereotype.Service;

@Service
public class PerformanceLogger {

    public void print(PerformanceLog log) {

        PerformanceMetric metric = log.getMetric();

        System.out.printf(
                """
                [1] Entity Read              : %d ms
                [2] Nearby Search            : %d ms
                [3] Mapping Nearby           : %d ms
                [4] AI Decision              : %d ms
                [5] Collision                : %d ms
                [6] Move                     : %d ms
                [7] Add Spawn Entities       : %d ms
                [8] Apply Move               : %d ms

                [Async] Redis Update         : %d ms
                [Async] Stream Publish       : %d ms
                [Async] WebSocket Send       : %d ms
                [Async] Total                : %d ms

                TOTAL                        : %d ms

                """,
                metric.getEntityRead(),
                metric.getGeoSearch(),
                metric.getMappingNearBy(),
                metric.getAiDecision(),
                metric.getCollision(),
                metric.getMove(),
                metric.getAddSpawnEntities(),
                metric.getApplyMove(),

                metric.getRedisUpdate(),
                metric.getStreamPublish(),
                metric.getWebsocketSend(),

                Math.max(
                        metric.getRedisUpdate(),
                        Math.max(
                                metric.getStreamPublish(),
                                metric.getWebsocketSend()
                        )
                ),

                metric.getTotal()
        );
    }
}
