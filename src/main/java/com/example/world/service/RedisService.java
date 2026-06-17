package com.example.world.service;

import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface RedisService {
    public Consumer<RedisConnection> getEntityIds(List<String> ids);
    public Consumer<RedisConnection> saveNewEntities(String type, Long nextId, int count);
    public Consumer<RedisConnection> updateEntitiesPipe(List<RedisEntity> entities);
    public Consumer<RedisConnection> getNearByIds(List<RedisEntity> entities, int range);
    public Consumer<RedisConnection> getCollisionIds(List<NextMove> nextMoveList, double range);
    public Consumer<RedisConnection> saveSpawnEntities(List<RedisEntity> spawnList, Long nextEntityId);

    public default Consumer<Cursor<byte[]>> batchConsumer(
            int batchSize,
            Consumer<List<String>> consumer
    ) {
        return cursor -> {

            List<String> batchIds = new ArrayList<>(batchSize);

            while (cursor.hasNext()) {

                batchIds.add(
                        new String(
                                cursor.next(),
                                StandardCharsets.UTF_8
                        )
                );

                if (batchIds.size() >= batchSize) {
                    consumer.accept(batchIds);
                    batchIds = new ArrayList<>(batchSize);
                }
            }

            if (!batchIds.isEmpty()) {
                consumer.accept(batchIds);
            }
        };
    }
}
