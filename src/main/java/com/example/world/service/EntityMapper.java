package com.example.world.service;

import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;

import java.util.ArrayList;
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

    public default List<RedisEntity> objectsToRedisEntities(List<Object> objectList) {
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
                            targetId.equals("null") ? null : Long.parseLong(targetId)
                    )
            );
        }
        return entities;
    }
}
