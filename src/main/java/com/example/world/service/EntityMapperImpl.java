package com.example.world.service;

import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EntityMapperImpl implements EntityMapper{

    public Map<Long, List<Long>> geoSearchCollisionsToIds(List<Object> collisionIds, List<NextMove> nextMoves) {

        Map<Long, List<Long>> result = new HashMap<>();

        for (int i = 0; i < collisionIds.size(); i++) {

            GeoResults<RedisGeoCommands.GeoLocation<byte[]>> geoResults =
                    (GeoResults<RedisGeoCommands.GeoLocation<byte[]>>) collisionIds.get(i);

            Long selfId = nextMoves.get(i).getEntity().getId();

            List<Long> ids = new ArrayList<>();

            if (geoResults != null) {
                for (GeoResult<RedisGeoCommands.GeoLocation<byte[]>> geoResult : geoResults) {

                    String key = new String(
                            geoResult.getContent().getName(),
                            StandardCharsets.UTF_8
                    );

                    Long id = Long.parseLong(key.replace("entity:", ""));

                    if (!id.equals(selfId)) {
                        ids.add(id);
                    }
                }
            }

            result.put(selfId, ids);
        }

        return result;
    }

    public List<Long> convertNearbyIds(
            Long baseEntityId,
            GeoResults<RedisGeoCommands.GeoLocation<byte[]>> geoResults
    ) {
        List<Long> nearbyIds = new ArrayList<>();
        if (geoResults == null) {
            return nearbyIds;
        }
        for (GeoResult<RedisGeoCommands.GeoLocation<byte[]>> geoResult : geoResults) {
            String key = new String(
                    geoResult.getContent().getName(),
                    StandardCharsets.UTF_8
            );

            long id = Long.parseLong(key.substring("entity:".length()));

            if(id != baseEntityId) {
                nearbyIds.add(id);
            }
        }
        return nearbyIds;
    }

    public List<RedisEntity> convertNearbyEntities(
            Long baseEntityId,
            GeoResults<RedisGeoCommands.GeoLocation<byte[]>> geoResults,
            Map<Long, RedisEntity> entityMap
    ) {
        List<RedisEntity> nearbyEntities = new ArrayList<>();
        if (geoResults == null) {
            return nearbyEntities;
        }
        for (GeoResult<RedisGeoCommands.GeoLocation<byte[]>> geoResult : geoResults) {
            String key = new String(
                    geoResult.getContent().getName(),
                    StandardCharsets.UTF_8
            );

            long id = Long.parseLong(key.substring("entity:".length()));

            if(id != baseEntityId) {
                nearbyEntities.add(entityMap.get(id));
            }
        }
        return nearbyEntities;
    }

    public Map<Long, List<RedisEntity>> geoSearchNearbyResultToIds(
            List<RedisEntity> entities,
            List<Object> geoResults,
            Map<Long, RedisEntity> entityMap) {
        Map<Long, List<RedisEntity>> result = new HashMap<>();
        for (int i = 0; i < entities.size(); i++) {

            RedisEntity entity = entities.get(i);

            @SuppressWarnings("unchecked")
            GeoResults<RedisGeoCommands.GeoLocation<byte[]>> nearResults =
                    (GeoResults<RedisGeoCommands.GeoLocation<byte[]>>) geoResults.get(i);

            List<RedisEntity> nearbyEntities = convertNearbyEntities(entity.getId(), nearResults, entityMap);

            result.put(entity.getId(), nearbyEntities);
        }
        return result;
    }

    public List<RedisEntity> idsToRedisEntities(Map<Long, List<RedisEntity>> entityMap, Long id) {
        return new ArrayList<>(entityMap.get(id));
    }

//    public List<RedisEntity> objectsToRedisEntities(List<Object> objectList) {
//        List<RedisEntity> entities = new ArrayList<>();
//
//        for (Object result : objectList) {
//
//            @SuppressWarnings("unchecked")
//            Map<String, String> map =
//                    (Map<String, String>) result;
//
//            if (map == null || map.isEmpty()) {
//                continue;
//            }
//
//            entities.add(
//                    new RedisEntity(
//                            Long.parseLong(map.get("id")),
//                            TypeEnum.valueOf(map.get("type")),
//                            StateEnum.valueOf(map.get("state")),
//                            Integer.parseInt(map.get("stamina")),
//                            Integer.parseInt(map.get("hp")),
//                            Integer.parseInt(map.get("x")),
//                            Integer.parseInt(map.get("y")),
//                            map.get("cell")
//                    )
//            );
//        }
//        return entities;
//    }

    public Map<Long, RedisEntity> entitiesToHashMap(List<RedisEntity> entityList) {
        Map<Long, RedisEntity> result = new HashMap<>();

        entityList.forEach(entity -> {
            Long id = entity.getId();
            result.put(id, entity);
        });

        return result;
    }
}
