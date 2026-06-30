package com.example.world.service;

import com.example.world.constants.RedisKeys;
import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.util.ByteTypeConverter;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface RedisService {
    Consumer<RedisConnection> getEntityIds(List<String> ids);
    Consumer<RedisConnection> saveNewEntities(String type, Long nextId, int count);
    Consumer<RedisConnection> updateEntitiesPipe(List<RedisEntity> entities);
    Consumer<RedisConnection> updateEntitiesPipe(
            List<RedisEntity> entities,
            Long nextEntityId
    );

    Consumer<RedisConnection> getNearByIds(List<RedisEntity> entities, int range);
    Map<Long, List<RedisEntity>> geoSearchNearbyResultToIds(
            List<RedisEntity> entities,
            List<Object> geoResults,
            Map<Long, RedisEntity> entityMap
    );
    Consumer<RedisConnection> getCollisionIds(List<NextMove> nextMoveList, double range);
    Consumer<RedisConnection> saveSpawnEntities(List<RedisEntity> spawnList, Long nextEntityId);

    default Consumer<Cursor<byte[]>> batchConsumer(
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

    public default boolean skipGeoSearch(RedisEntity entity) {
        return entity.isSkipSearch();
    }

    public default Consumer<RedisConnection> removeEntity(List<RedisEntity> entities) {
        return connection -> {
            for (RedisEntity entity : entities) {
                if(entity.isDead()) {
                    String prevCellKey = entity.getCellKey();
                    String entityKey = "{%s}:entity:%d".formatted(prevCellKey, entity.getId());
                    byte[] entityByteKey = ByteTypeConverter.stringToByte(entityKey);
                    byte[] prevCellByteKey = ByteTypeConverter.stringToByte(prevCellKey);

                    connection.keyCommands().del(entityByteKey);
                    connection.geoCommands().geoRemove(
                            prevCellByteKey,
                            entityByteKey
                    );
                    connection.setCommands().sRem(
                            RedisKeys.WORLD_BYTE,
                            entityByteKey
                    );
                }
            }
        };
    }

    public default Consumer<RedisConnection> hashFlush(List<RedisEntity> entities) {
        return connection -> {
            for (RedisEntity entity : entities) {
                if(entity.isDead()) continue;
                String cellKey = entity.getCellKey();
                String entityKey = "{%s}:entity:%d".formatted(cellKey, entity.getId());
                byte[] entityByteKey = ByteTypeConverter.stringToByte(entityKey);
                Map<byte[], byte[]> map = EntityMapper.entityToByteMap(entity);
                connection.hashCommands().hMSet(entityByteKey, map);
            }
        };
    }
}
