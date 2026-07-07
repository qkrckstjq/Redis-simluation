package com.example.world.cluster.service;

import com.example.world.cluster.CellManager;
import com.example.world.constants.RedisKeys;
import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
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
@RequiredArgsConstructor
@Qualifier("entityClusterService")
public class EntityClusterService implements RedisService {
    private final CellManager cellManager;
    private final GeoClusterService geoClusterService;
    private final EntityClusterMapper entityClusterMapper;

    public Consumer<RedisConnection> saveNewEntities(String type, Long nextId, int count) {
        int start = nextId.intValue();
        return connection -> {
            for (int i = start; i < count + start; i++) {
//                String entityKey = "entity:" + i;
//                byte[] entityByteKey = ByteTypeConverter.stringToByte(entityKey);
                int randX = RandUtil.getIntRand(GeoUtil.MIN_COORDINATE, GeoUtil.MAX_COORDINATE);
                int randY = RandUtil.getIntRand(GeoUtil.MIN_COORDINATE, GeoUtil.MAX_COORDINATE);
                String cellKey = cellManager.getGeoKey(randX, randY);
                byte[] geoEntityKey = ByteTypeConverter.stringToByte("{%s}:entity:%d".formatted(cellKey, i));
                byte[] cellByteKey = ByteTypeConverter.stringToByte(cellKey);
                Point point = new Point(GeoUtil.scaleIn(randX), GeoUtil.scaleIn(randY));

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

    public Consumer<RedisConnection> updateEntitiesPipe(List<RedisEntity> entities) {
        return connection -> {

            for (RedisEntity entity : entities) {
                String prevCellKey = entity.getCellKey();
                String nextCellKey = cellManager.getGeoKey(entity.getX(), entity.getY());
                String entityKey = "{%s}:entity:%d".formatted(prevCellKey, entity.getId());
                byte[] entityByteKey = ByteTypeConverter.stringToByte(entityKey);
                byte[] prevCellByteKey = ByteTypeConverter.stringToByte(prevCellKey);
                byte[] nextCellByteKey = ByteTypeConverter.stringToByte(nextCellKey);

                if(entity.isDead()) {
                    connection.keyCommands().del(entityByteKey);
                    connection.geoCommands().geoRemove(
                            prevCellByteKey,
                            entityByteKey
                    );
                    connection.setCommands().sRem(
                            RedisKeys.WORLD_BYTE,
                            entityByteKey
                    );
                    continue;
                }

                if(!entity.checkCellKey(nextCellKey)) {
                    connection.keyCommands().del(entityByteKey);
                    connection.geoCommands().geoRemove(
                            prevCellByteKey,
                            entityByteKey
                    );
                    connection.setCommands().sRem(
                            RedisKeys.WORLD_BYTE,
                            entityByteKey
                    );
                    entity.setCellKey(nextCellKey);
                }

                byte[] nextEntityByteKey = ByteTypeConverter.stringToByte("{%s}:entity:%d".formatted(nextCellKey, entity.getId()));
                Point point = new Point(GeoUtil.scaleIn(entity.getX()), GeoUtil.scaleIn(entity.getY()));

                Map<byte[], byte[]> map = EntityMapper.entityToByteMap(entity);

                if(!entity.isSkipGeoUpdate()) {
                    connection.geoCommands().geoAdd(nextCellByteKey, point, nextEntityByteKey);
                }

                connection.hashCommands().hMSet(nextEntityByteKey, map);
                connection.setCommands().sAdd(
                        RedisKeys.WORLD_BYTE,
                        nextEntityByteKey
                );
            }
        };
    }

    @Override
    public Consumer<RedisConnection> updateEntitiesPipe(List<RedisEntity> entities, Long nextEntityId) {
        return null;
    }

    public Consumer<RedisConnection> saveSpawnEntities(
            List<RedisEntity> spawnList,
            Long nextEntityId) {

        return connection -> {
            long id = nextEntityId;
            for (RedisEntity entity : spawnList) {

                entity.setId(id);

                String cellKey = cellManager.getGeoKey(
                        entity.getX(),
                        entity.getY()
                );

                String entityKey = "{%s}:entity:%d".formatted(cellKey, id);

                entity.setCellKey(cellKey);

                Map<byte[], byte[]> map =
                        EntityMapper.entityToByteMap(entity);

                byte[] entityKeyBytes =
                        ByteTypeConverter.stringToByte(entityKey);

                connection.hashCommands().hMSet(
                        entityKeyBytes,
                        map
                );

                connection.setCommands().sAdd(
                        RedisKeys.WORLD_BYTE,
                        ByteTypeConverter.stringToByte(entityKey)
                );

                connection.geoCommands().geoAdd(
                        ByteTypeConverter.stringToByte(cellKey),
                        new Point(
                                GeoUtil.scaleIn(entity.getX()),
                                GeoUtil.scaleIn(entity.getY())
                        ),
                        entityKeyBytes
                );
                id++;
            }
        };
    }

    public Consumer<RedisConnection> getEntityIds(List<String> ids) {
        return connection -> {
            for (String id : ids) {
                connection.hashCommands().hGetAll(ByteTypeConverter.stringToByte(id));
            }
        };
    }

    public Consumer<RedisConnection> getNearByIds(List<RedisEntity> entities, int range) {
        return geoClusterService.getNearByIds(entities, range);
    }

    public Map<Long, List<RedisEntity>> geoSearchNearbyResultToIds(
            List<RedisEntity> entities,
            List<Object> geoResults,
            Map<Long, RedisEntity> entityMap
    ) {
        return entityClusterMapper.geoSearchNearbyResultToIds(entities, geoResults, entityMap);
    }

    public Consumer<RedisConnection> getCollisionIds(List<NextMove> nextMoveList, double range) {
        return geoClusterService.getCollisionIds(nextMoveList, range);
    }
}
