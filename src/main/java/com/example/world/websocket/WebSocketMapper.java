package com.example.world.websocket;

import com.example.world.entity.EntitySnapshotDto;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.Tick;
import com.example.world.service.EntityMapper;
import com.example.world.service.EntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WebSocketMapper {
    private long tick = 1L;
    private static final String MOVE = "MOVE";
    private final EntityMapper geoMapper;

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
            if(EntityService.isDead(entity)) continue;

            @SuppressWarnings("unchecked")
            GeoResults<RedisGeoCommands.GeoLocation<byte[]>> nearResults =
                    (GeoResults<RedisGeoCommands.GeoLocation<byte[]>>) geoResults.get(i);

            List<Long> nearbyIds = geoMapper.convertNearbyIds(entity.getId(), nearResults);

            result.add(new EntitySnapshotDto(
                    entity.getId(),
                    entity.getType(),
                    entity.getState(),
                    entity.getStamina(),
                    entity.getHp(),
                    entity.getX(),
                    entity.getY(),
                    nearbyIds
            ));
        }
        return result;
    }

    public Long getTick() {
        return this.tick;
    }
}
