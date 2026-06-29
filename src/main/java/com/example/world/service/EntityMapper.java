package com.example.world.service;

import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
import com.example.world.util.ByteTypeConverter;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface EntityMapper {
    public Map<Long, List<Long>> geoSearchCollisionsToIds(List<Object> collisionIds, List<NextMove> nextMoves);

    public List<Long> convertNearbyIds(Long baseEntityId, GeoResults<RedisGeoCommands.GeoLocation<byte[]>> geoResults);

    public List<RedisEntity> convertNearbyEntities(
            Long baseEntityId,
            GeoResults<RedisGeoCommands.GeoLocation<byte[]>> geoResults,
            Map<Long, RedisEntity> entityMap
    );

    public Map<Long, List<RedisEntity>> geoSearchNearbyResultToIds(
            List<RedisEntity> entities,
            List<Object> geoResults,
            Map<Long, RedisEntity> entityMap
    );

    public List<RedisEntity> idsToRedisEntities(Map<Long, List<RedisEntity>> entityMap, Long id);

//    public List<RedisEntity> objectsToRedisEntities(List<Object> objectList);

    public Map<Long, RedisEntity> entitiesToHashMap(List<RedisEntity> entityList);

    public static List<RedisEntity> objectsToRedisEntities(List<Object> objectList) {
        List<RedisEntity> entities = new ArrayList<>();

        for (Object result : objectList) {

            @SuppressWarnings("unchecked")
            Map<String, String> map =
                    (Map<String, String>) result;

            if (map == null || map.isEmpty()) {
                continue;
            }

            String targetId = map.get("targetId");

            entities.add(
                    new RedisEntity(
                            Long.parseLong(map.get("id")),
                            Integer.parseInt(map.get("age")),
                            TypeEnum.valueOf(map.get("type")),
                            StateEnum.valueOf(map.get("state")),
                            Integer.parseInt(map.get("stamina")),
                            Integer.parseInt(map.get("hp")),
                            Integer.parseInt(map.get("x")),
                            Integer.parseInt(map.get("y")),
                            Boolean.parseBoolean(map.get("breedReady")),
                            Integer.parseInt(map.get("breedReadyTick")),
                            map.get("cell"),
                            targetId.equals("null") ? null : Long.parseLong(targetId),
                            Boolean.parseBoolean(map.get("skipSearch"))
                    )
            );
        }
        return entities;
    }

    public static Map<byte[], byte[]> entityToByteMap(RedisEntity entity) {
        Map<byte[], byte[]> map = new HashMap<>();
        map.put(
                ByteTypeConverter.stringToByte("id"),
                ByteTypeConverter.numToByte(Math.toIntExact(entity.getId()))
        );

        map.put(
                ByteTypeConverter.stringToByte("age"),
                ByteTypeConverter.numToByte(entity.getAge())
        );

        map.put(
                ByteTypeConverter.stringToByte("hp"),
                ByteTypeConverter.numToByte(entity.getHp())
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
                ByteTypeConverter.stringToByte("type"),
                ByteTypeConverter.stringToByte(entity.getType().name())
        );

        map.put(
                ByteTypeConverter.stringToByte("state"),
                ByteTypeConverter.stringToByte(entity.getState().getState())
        );

        map.put(
                ByteTypeConverter.stringToByte("stamina"),
                ByteTypeConverter.numToByte(entity.getStamina())
        );

        map.put(
                ByteTypeConverter.stringToByte("breedReady"),
                ByteTypeConverter.stringToByte(String.valueOf(entity.isBreedReady()))
        );

        map.put(
                ByteTypeConverter.stringToByte("breedReadyTick"),
                ByteTypeConverter.numToByte(entity.getBreedReadyTick())
        );

        map.put(
                ByteTypeConverter.stringToByte("cell"),
                ByteTypeConverter.stringToByte(entity.getCellKey())
        );

        map.put(
                ByteTypeConverter.stringToByte("targetId"),
                ByteTypeConverter.numToByte(entity.getTargetId())
        );

        map.put(
                ByteTypeConverter.stringToByte("skipSearch"),
                ByteTypeConverter.numToByte(String.valueOf(entity.isSkipSearch()))
        );
        return map;
    }

    public static Map<byte[], byte[]> newEntityToByteMap(
            int id,
            int hp,
            String type,
            String state,
            int stamina,
            int x,
            int y,
            String cellKey
    ) {
        Map<byte[], byte[]> map = new HashMap<>();
        map.put(
                ByteTypeConverter.stringToByte("id"),
                ByteTypeConverter.numToByte(id)
        );

        map.put(
                ByteTypeConverter.stringToByte("age"),
                ByteTypeConverter.numToByte(0)
        );

        map.put(
                ByteTypeConverter.stringToByte("hp"),
                ByteTypeConverter.numToByte(hp)
        );

        map.put(
                ByteTypeConverter.stringToByte("type"),
                ByteTypeConverter.stringToByte(type)
        );

        map.put(
                ByteTypeConverter.stringToByte("state"),
                ByteTypeConverter.stringToByte(state)
        );

        map.put(
                ByteTypeConverter.stringToByte("stamina"),
                ByteTypeConverter.numToByte(stamina)
        );

        map.put(
                ByteTypeConverter.stringToByte("x"),
                ByteTypeConverter.numToByte(x)
        );

        map.put(
                ByteTypeConverter.stringToByte("y"),
                ByteTypeConverter.numToByte(y)
        );

        map.put(
                ByteTypeConverter.stringToByte("breedReady"),
                ByteTypeConverter.stringToByte(String.valueOf(false))
        );

        map.put(
                ByteTypeConverter.stringToByte("breedReadyTick"),
                ByteTypeConverter.numToByte(0)
        );

        map.put(
                ByteTypeConverter.stringToByte("cell"),
                ByteTypeConverter.stringToByte(cellKey)
        );

        map.put(
                ByteTypeConverter.stringToByte("targetId"),
                ByteTypeConverter.stringToByte("null")
        );

        map.put(
                ByteTypeConverter.stringToByte("skipSearch"),
                ByteTypeConverter.stringToByte(String.valueOf(false))
        );
        return map;
    }
}
