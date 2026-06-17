package com.example.world.service;

import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;

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

    public List<RedisEntity> objectsToRedisEntities(List<Object> objectList);

    public Map<Long, RedisEntity> entitiesToHashMap(List<RedisEntity> entityList);
}
