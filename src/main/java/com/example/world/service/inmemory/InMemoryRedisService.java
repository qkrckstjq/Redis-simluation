package com.example.world.service.inmemory;

import com.example.world.cluster.CellManager;
import com.example.world.constants.RedisKeys;
import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
import com.example.world.service.EntityMapper;
import com.example.world.service.RedisService;
import com.example.world.util.ByteTypeConverter;
import com.example.world.util.GeoUtil;
import com.example.world.util.RandUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Qualifier("inMemoryRedisService")
@RequiredArgsConstructor
public class InMemoryRedisService implements RedisService {
    private final CellManager cellManager;
    private final EntityManager entityManager;

    @Override
    public Consumer<RedisConnection> getEntityIds(List<String> ids) {
        return null;
    }

    @Override
    public Consumer<RedisConnection> saveNewEntities(String type, Long nextId, int count) {
        int start = nextId.intValue();
        return connection -> {
            for (int i = start; i < count + start; i++) {
                TypeEnum curType = TypeEnum.valueOf(type);
                int randX = RandUtil.getIntRand(GeoUtil.MIN_COORDINATE, GeoUtil.MAX_COORDINATE);
                int randY = RandUtil.getIntRand(GeoUtil.MIN_COORDINATE, GeoUtil.MAX_COORDINATE);
                String cellKey = cellManager.getGeoKey(randX, randY);
                byte[] geoEntityKey = ByteTypeConverter.stringToByte("{%s}:entity:%d".formatted(cellKey, i));
                byte[] cellByteKey = ByteTypeConverter.stringToByte(cellKey);
                Point point = new Point(GeoUtil.scaleIn(randX), GeoUtil.scaleIn(randY));

                RedisEntity entity = new RedisEntity(
                        (long) i,
                        0,
                        curType,
                        StateEnum.MOVE,
                        100,
                        100,
                        randX,
                        randY,
                        false,
                        0,
                        cellKey,
                        null,
                        false
                );
                entityManager.addEntity(entity);

                Map<byte[], byte[]> entityMap = EntityMapper.newEntityToByteMap(
                        i,
                        100,
                        type,
                        StateEnum.MOVE.getState(),
                        100,
                        randX,
                        randY,
                        cellKey
                );

                connection.stringCommands().incrBy(RedisKeys.ENTITY_BYTE, 1);

                connection.setCommands().sAdd(
                        RedisKeys.WORLD_BYTE,
                        geoEntityKey
                );

                connection.hashCommands().hMSet(
                        geoEntityKey,
                        entityMap
                );


                connection.geoCommands().geoAdd(cellByteKey, point, geoEntityKey);
            }
        };
    }

    @Override
    public Consumer<RedisConnection> updateEntitiesPipe(List<RedisEntity> entities) {

        return null;
    }

    @Override
    public Consumer<RedisConnection> getNearByIds(List<RedisEntity> entities, int range) {
        return null;
    }

    @Override
    public Map<Long, List<RedisEntity>> geoSearchNearbyResultToIds(List<RedisEntity> entities, List<Object> geoResults, Map<Long, RedisEntity> entityMap) {
        return Map.of();
    }

    @Override
    public Consumer<RedisConnection> getCollisionIds(List<NextMove> nextMoveList, double range) {
        return null;
    }

    @Override
    public Consumer<RedisConnection> saveSpawnEntities(List<RedisEntity> spawnList, Long nextEntityId) {
        return null;
    }
}
