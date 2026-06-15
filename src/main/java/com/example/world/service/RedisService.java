package com.example.world.service;

import com.example.world.constants.RedisKeys;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
import com.example.world.repository.RedisRepository;
import com.example.world.util.ByteTypeConverter;
import com.example.world.util.GeoUtil;
import com.example.world.util.RandUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisRepository redisRepository;
    private final int SCAN_SIZE = 1000;
//    private final byte[] worldKey = ByteTypeConverter.stringToByte(RedisRepository.WORLD_KEY);
//    private final byte[] entityId = ByteTypeConverter.stringToByte(RedisRepository.ENTITY_ID_KEY);
//    private final byte[] geoKey = ByteTypeConverter.stringToByte(RedisRepository.GEO_KEY);




    public List<RedisEntity> getEntities(List<Object> objectList) {
        List<RedisEntity> entities = new ArrayList<>();

        for (Object result : objectList) {

            @SuppressWarnings("unchecked")
            Map<String, String> map =
                    (Map<String, String>) result;

            if (map == null || map.isEmpty()) {
                continue;
            }

            entities.add(
                    new RedisEntity(
                            Long.parseLong(map.get("id")),
                            TypeEnum.valueOf(map.get("type")),
                            StateEnum.valueOf(map.get("state")),
                            Integer.parseInt(map.get("stamina")),
                            Integer.parseInt(map.get("hp")),
                            Integer.parseInt(map.get("x")),
                            Integer.parseInt(map.get("y"))
                    )
            );
        }
        return entities;
    }

    public Map<Long, RedisEntity> getHashMapEntities(List<RedisEntity> entityList) {
        Map<Long, RedisEntity> result = new HashMap<>();

        entityList.forEach(entity -> {
            Long id = entity.getId();
            result.put(id, entity);
        });

        return result;
    }

    public Consumer<RedisConnection> saveNewEntities(String type, Long nextId, int count) {
        int start = nextId.intValue();
        return connection -> {
            for (int i = start; i < count + start; i++) {

                String key = "entity:" + i;
                byte[] entityKey = ByteTypeConverter.stringToByte(key);
                byte[] curId = ByteTypeConverter.IntegerToByte(i);
                int randX = RandUtil.getIntRand(GeoUtil.MIN_COORDINATE, GeoUtil.MAX_COORDINATE);
                int randY = RandUtil.getIntRand(GeoUtil.MIN_COORDINATE, GeoUtil.MAX_COORDINATE);
                Point point = new Point(GeoUtil.scaleIn(randX), GeoUtil.scaleIn(randY));
                Map<byte[], byte[]> entityMap = new HashMap<>();

                entityMap.put(
                        ByteTypeConverter.stringToByte("id"),
                        curId
                );

                entityMap.put(
                        ByteTypeConverter.stringToByte("hp"),
                        ByteTypeConverter.IntegerToByte(100)
                );

                entityMap.put(
                        ByteTypeConverter.stringToByte("type"),
                        ByteTypeConverter.stringToByte(TypeEnum.valueOf(type).toString())
                );

                entityMap.put(
                        ByteTypeConverter.stringToByte("state"),
                        ByteTypeConverter.stringToByte(StateEnum.MOVE.getState())
                );

                entityMap.put(
                        ByteTypeConverter.stringToByte("stamina"),
                        ByteTypeConverter.IntegerToByte(100)
                );

                entityMap.put(
                        ByteTypeConverter.stringToByte("x"),
                        ByteTypeConverter.IntegerToByte(randX)
                );

                entityMap.put(
                        ByteTypeConverter.stringToByte("y"),
                        ByteTypeConverter.IntegerToByte(randY)
                );

                connection.stringCommands().incrBy(RedisKeys.ENTITY_BYTE, 1);

                connection.setCommands().sAdd(
                        RedisKeys.WORLD_BYTE,
                        curId
                );

                connection.hashCommands().hMSet(
                        entityKey,
                        entityMap
                );

                connection.geoCommands().geoAdd(RedisKeys.GEO_BYTE, point, entityKey);
            }
        };
    }

    public Consumer<RedisConnection> updateEntitiesPipe(List<RedisEntity> entities) {
        return connection -> {

            for (RedisEntity entity : entities) {
                String key = "entity:" + entity.getId();
                byte[] entityKey = ByteTypeConverter.stringToByte(key);

                if(EntityService.isDead(entity)) {
                    connection.keyCommands().del(entityKey);

                    connection.geoCommands().geoRemove(
                            RedisKeys.GEO_BYTE,
                            entityKey
                    );

                    connection.setCommands().sRem(
                            RedisKeys.WORLD_BYTE,
                            ByteTypeConverter.stringToByte(String.valueOf(entity.getId()))
                    );

                    continue;
                }



                Point point = new Point(GeoUtil.scaleIn(entity.getX()), GeoUtil.scaleIn(entity.getY()));

                Map<byte[], byte[]> map = new HashMap<>();

                map.put(
                        ByteTypeConverter.stringToByte("hp"),
                        ByteTypeConverter.IntegerToByte(entity.getHp())
                );

                map.put(
                        ByteTypeConverter.stringToByte("x"),
                        ByteTypeConverter.stringToByte(String.valueOf(entity.getX()))
                );

                map.put(
                        ByteTypeConverter.stringToByte("y"),
                        ByteTypeConverter.stringToByte(String.valueOf(entity.getY()))
                );

                map.put(
                        ByteTypeConverter.stringToByte("state"),
                        ByteTypeConverter.stringToByte(entity.getState().getState())
                );

                map.put(
                        ByteTypeConverter.stringToByte("stamina"),
                        ByteTypeConverter.IntegerToByte(entity.getStamina())
                );

                connection.geoCommands().geoAdd(RedisKeys.GEO_BYTE, point, entityKey);
                connection.hashCommands().hMSet(entityKey, map);
            }
        };
    }

    public Consumer<RedisConnection> getEntityIds(List<String> ids) {
        return connection -> {
            for (String id : ids) {

                connection.hashCommands().hGetAll(ByteTypeConverter.stringToByte(("entity:" + id)));
            }
        };
    }

    public Consumer<RedisConnection> getGeoEntityIds(List<String> ids) {
        return connection -> {
            for (String id : ids) {
                byte[] key = ByteTypeConverter.stringToByte(("entity:" + id));
                connection.geoCommands().geoPos(RedisKeys.GEO_BYTE, key);
            }
        };
    }

    public Consumer<Cursor<byte[]>> batchConsumer(
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
