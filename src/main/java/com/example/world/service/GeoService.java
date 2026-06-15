package com.example.world.service;

import com.example.world.constants.RedisKeys;
import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.repository.RedisRepository;
import com.example.world.util.ByteTypeConverter;
import com.example.world.util.GeoUtil;
import org.springframework.data.geo.Distance;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class GeoService {
    private final int DEFAULT_RANGE = 10;

    public Consumer<RedisConnection> getNearByIds(List<RedisEntity> entities, int range) {
        return connection -> {
            for (RedisEntity entity : entities) {
                Long entityId = entity.getId();
                byte[] memberKey = ByteTypeConverter.stringToByte("entity:" + entityId);
                connection.geoCommands().geoSearch(
                        RedisKeys.GEO_BYTE,
                        GeoReference.fromMember(memberKey),
                        GeoShape.byRadius(new Distance(range)),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().sortAscending()
                );
            }
        };
    }

    public Consumer<RedisConnection> getCollisionIds(List<NextMove> nextMoveList, double range) {
        return connection -> {
            for (NextMove nextMove : nextMoveList) {
                connection.geoCommands().geoSearch(
                        RedisKeys.GEO_BYTE,
                        GeoReference.fromCoordinate(
                                GeoUtil.scaleIn(nextMove.getNextX()),
                                GeoUtil.scaleIn(nextMove.getNextY())
                        ),
                        GeoShape.byRadius(new Distance(range)),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                );
            }
        };
    }
}
