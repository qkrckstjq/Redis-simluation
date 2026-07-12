package com.example.world.entity.log;

public final class PerformanceLogger {
    private static final long SCALE = 1_000_000;

    private PerformanceLogger() {}

    public static void print(PerformanceLog log) {

        PerformanceMetric metric = log.getMetric();

        System.out.printf(
                """
                [1] Entity Read              : %d ms
                [2] skip Geo Entities        : %d ms
                [3] Nearby Search            : %d ms
                [4] Mapping Nearby           : %d ms
                [5] AI Decision              : %d ms
                [6] Move with Collision      : %d ms
                [7] Add Spawn Entities       : %d ms
                [8] Apply Move               : %d ms

                [Async] Redis Update         : %d ms
                [Async] Save History         : %d ms
                [Async] Stream Publish       : %d ms
                [Async] WebSocket Send       : %d ms
                [Async] Total                : %d ms

                TOTAL                        : %d ms

                """,
                metric.getEntityRead() / SCALE,
                metric.getMappingSkipGeo() / SCALE,
                metric.getGeoSearch() / SCALE,
                metric.getMappingNearBy() / SCALE,
                metric.getAiDecision() / SCALE,
                metric.getMoveWithCollision() / SCALE,
                metric.getAddSpawnEntities() / SCALE,
                metric.getApplyMove() / SCALE,
                metric.getRedisUpdate() / SCALE,
                metric.getSaveHistory() / SCALE,
                metric.getStreamPublish() / SCALE,
                metric.getWebsocketSend() / SCALE,
                metric.getAsyncTotal() / SCALE,
                metric.getTotal() / SCALE
        );
    }
}
