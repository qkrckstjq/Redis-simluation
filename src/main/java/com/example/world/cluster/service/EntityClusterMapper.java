package com.example.world.cluster.service;

import com.example.world.cluster.CellManager;
import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
import com.example.world.service.EntityMapper;
import com.example.world.util.GeoUtil;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class EntityClusterMapper implements EntityMapper {
    public Map<Long, List<Long>> geoSearchCollisionsToIds(
            List<Object> collisionIds,
            List<NextMove> nextMoves
    ) {

        Map<Long, List<Long>> result = new HashMap<>();

        int responseIndex = 0;

        for (NextMove nextMove : nextMoves) {

            Long selfId = nextMove.getEntity().getId();

            Set<Long> ids = new HashSet<>();

            double range = 0.2;

            int minCellX = (int) ((nextMove.getNextX() - range) / CellManager.CELL_SIZE);
            int maxCellX = (int) ((nextMove.getNextX() + range) / CellManager.CELL_SIZE);

            int minCellY = (int) ((nextMove.getNextY() - range) / CellManager.CELL_SIZE);
            int maxCellY = (int) ((nextMove.getNextY() + range) / CellManager.CELL_SIZE);

            int searchCount =
                    (maxCellX - minCellX + 1) *
                            (maxCellY - minCellY + 1);

            for (int i = 0; i < searchCount; i++) {

                GeoResults<RedisGeoCommands.GeoLocation<byte[]>> geoResults =
                        (GeoResults<RedisGeoCommands.GeoLocation<byte[]>>) collisionIds.get(responseIndex++);

                if (geoResults == null) {
                    continue;
                }

                for (GeoResult<RedisGeoCommands.GeoLocation<byte[]>> geoResult : geoResults) {

                    String key = new String(
                            geoResult.getContent().getName(),
                            StandardCharsets.UTF_8
                    );
                    Long id = Long.parseLong(key.substring(17));

                    if (!id.equals(selfId)) {
                        ids.add(id);
                    }
                }
            }
            result.put(selfId, new ArrayList<>(ids));
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

            Long id = Long.parseLong(key.substring(17));

            if(!id.equals(baseEntityId)) {
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

            Long id = Long.parseLong(key.substring(17));

            if(!id.equals(baseEntityId)) {
                RedisEntity targetEntity = entityMap.get(id);

                if (targetEntity != null) {
                    nearbyEntities.add(targetEntity);
                }
            }
        }
        return nearbyEntities;
    }

    public Map<Long, List<RedisEntity>> geoSearchNearbyResultToIds(
            List<RedisEntity> entities,
            List<Object> geoResults,
            Map<Long, RedisEntity> entityMap
    ) {
        Map<Long, List<RedisEntity>> result = new HashMap<>();

        int responseIndex = 0;
        int range = 10;

        for (RedisEntity entity : entities) {

            List<RedisEntity> nearbyEntities = new ArrayList<>();
            Set<Long> nearbyIds = new HashSet<>();

            int minCellX = (entity.getX() - range) / CellManager.CELL_SIZE;
            int maxCellX = (entity.getX() + range) / CellManager.CELL_SIZE;

            int minCellY = (entity.getY() - range) / CellManager.CELL_SIZE;
            int maxCellY = (entity.getY() + range) / CellManager.CELL_SIZE;

            int searchCount =
                    (maxCellX - minCellX + 1) *
                            (maxCellY - minCellY + 1);

            for (int i = 0; i < searchCount; i++) {

                @SuppressWarnings("unchecked")
                GeoResults<RedisGeoCommands.GeoLocation<byte[]>> nearResults =
                        (GeoResults<RedisGeoCommands.GeoLocation<byte[]>>) geoResults.get(responseIndex++);

//                if (nearResults != null) {
//                    System.out.println("----- entity=" + entity.getId());
//
//                    for (GeoResult<RedisGeoCommands.GeoLocation<byte[]>> r : nearResults) {
//                        System.out.println(new String(r.getContent().getName()));
//                    }
//                }

                if (nearResults == null) {
                    continue;
                }

                List<RedisEntity> list =
                        convertNearbyEntities(entity.getId(), nearResults, entityMap);

                for (RedisEntity nearby : list) {
                    if (nearbyIds.add(nearby.getId())) {
                        nearbyEntities.add(nearby);
                    }
                }
            }
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
