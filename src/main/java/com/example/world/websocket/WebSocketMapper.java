package com.example.world.websocket;

import com.example.world.cluster.CellManager;
import com.example.world.cluster.service.EntityClusterMapper;
import com.example.world.entity.EntitySnapshotDto;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.Tick;
import com.example.world.service.EntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WebSocketMapper {
    private long tick = 1L;
    private static final String MOVE = "MOVE";
    private final EntityClusterMapper geoMapper;

    public EntitySnapshotDto redisEntityToSnapShotDto(RedisEntity entity) {
        return new EntitySnapshotDto(
                entity.getId(),
                entity.getType(),
                entity.getState(),
                entity.getStamina(),
                entity.getHp(),
                entity.getX(),
                entity.getY());
    }

    public List<EntitySnapshotDto> redisEntitiesToSnapShotDto(List<RedisEntity> entities) {
        return entities.stream()
                .map(this::redisEntityToSnapShotDto)
                .toList();
    }

//    public Tick redisEntitiesToTick(List<RedisEntity> entities) {
//        return new Tick(
//                MOVE,
//                tick++,
//                redisEntitiesToSnapShotDto(entities));
//    }

    public Tick redisEntitiesToTick(List<EntitySnapshotDto> entities) {
        return new Tick(
                MOVE,
                tick++,
                entities);
    }


    public List<EntitySnapshotDto> geoSearchResultsToEntitiesSnapShotDtos(
            List<RedisEntity> entities,
            List<Object> geoResults
    ) {

        List<EntitySnapshotDto> result = new ArrayList<>();

        for (int i = 0; i < entities.size(); i++) {
            RedisEntity entity = entities.get(i);
            if(entity.isDead()) continue;

            @SuppressWarnings("unchecked")
            GeoResults<RedisGeoCommands.GeoLocation<byte[]>> nearResults =
                    (GeoResults<RedisGeoCommands.GeoLocation<byte[]>>) geoResults.get(i);

            List<Long> nearbyIds = geoMapper.convertNearbyIds(entity.getId(), nearResults);

            result.add(new EntitySnapshotDto(
                    entity.getId(),
                    entity.getAge(),
                    entity.getType(),
                    entity.getState(),
                    entity.getStamina(),
                    entity.getHp(),
                    entity.getX(),
                    entity.getY(),
                    nearbyIds,
                    entity.getTargetId()
            ));
        }
        return result;
    }

    public List<EntitySnapshotDto> geoSearchResultsToClusterEntitiesSnapShotDtos(
            List<RedisEntity> entities,
            List<Object> geoResults,
            List<RedisEntity> noneTargetEntities
    ) {
        List<EntitySnapshotDto> result = new ArrayList<>();
        int responseIndex = 0;
        int range = 10;

        int idx = 0;
        for (RedisEntity entity : entities) {

            int minCellX = (entity.getX() - range) / CellManager.CELL_SIZE;
            int maxCellX = (entity.getX() + range) / CellManager.CELL_SIZE;
            int minCellY = (entity.getY() - range) / CellManager.CELL_SIZE;
            int maxCellY = (entity.getY() + range) / CellManager.CELL_SIZE;

            Set<Long> nearbyIds = null;
            if (!entity.isDead()) {
                nearbyIds = new LinkedHashSet<>();
            }

            if(idx < noneTargetEntities.size() && entity.getId().equals(noneTargetEntities.get(idx).getId())) {
                idx++;
                for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                    for (int cellY = minCellY; cellY <= maxCellY; cellY++) {

                        if (responseIndex >= geoResults.size()) {
                            continue;
                        }

                        @SuppressWarnings("unchecked")
                        GeoResults<RedisGeoCommands.GeoLocation<byte[]>> nearResults =
                                (GeoResults<RedisGeoCommands.GeoLocation<byte[]>>) geoResults.get(responseIndex++);

                        if (nearbyIds != null && nearResults != null) {
                            nearbyIds.addAll(
                                    geoMapper.convertNearbyIds(entity.getId(), nearResults)
                            );
                        }
                    }
                }
            }


            if (!entity.isDead()) {
                result.add(new EntitySnapshotDto(
                        entity.getId(),
                        entity.getAge(),
                        entity.getType(),
                        entity.getState(),
                        entity.getStamina(),
                        entity.getHp(),
                        entity.getX(),
                        entity.getY(),
                        new ArrayList<>(nearbyIds),
                        entity.getTargetId()
                ));
            }
        }

        return result;
    }

    public Long getTick() {
        return this.tick;
    }
}
