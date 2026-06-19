package com.example.world.service;

import com.example.world.cluster.CellManager;
import com.example.world.constants.RedisKeys;
import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
import com.example.world.util.ByteTypeConverter;
import com.example.world.util.GeoUtil;
import com.example.world.util.RandUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Qualifier("luaScriptService")
public class LuaScriptService implements RedisService {
    private final String updateEntitySha;
    private final CellManager cellManager;
    private final GeoMapper geoMapper;
    private final GeoService geoService;
    private final EntityMapperImpl entityMapper;

    public Consumer<RedisConnection> saveNewEntities(String type, Long nextId, int count) {
        int start = nextId.intValue();
        return connection -> {
            for (int i = start; i < count + start; i++) {

                String key = "entity:" + i;
                byte[] entityKey = ByteTypeConverter.stringToByte(key);
                byte[] curId = ByteTypeConverter.numToByte(i);
                int randX = RandUtil.getIntRand(GeoUtil.MIN_COORDINATE, GeoUtil.MAX_COORDINATE);
                int randY = RandUtil.getIntRand(GeoUtil.MIN_COORDINATE, GeoUtil.MAX_COORDINATE);
                Point point = new Point(GeoUtil.scaleIn(randX), GeoUtil.scaleIn(randY));
                Map<byte[], byte[]> entityMap = new HashMap<>();

                entityMap.put(
                        ByteTypeConverter.stringToByte("id"),
                        curId
                );

                entityMap.put(
                        ByteTypeConverter.stringToByte("hp"),
                        ByteTypeConverter.numToByte(100)
                );

                entityMap.put(
                        ByteTypeConverter.stringToByte("type"),
                        ByteTypeConverter.stringToByte(TypeEnum.valueOf(type).toString())
                );

                entityMap.put(
                        ByteTypeConverter.stringToByte("state"),
                        ByteTypeConverter.stringToByte(StateEnum.MOVE.getState())
                );

                entityMap.put(
                        ByteTypeConverter.stringToByte("stamina"),
                        ByteTypeConverter.numToByte(100)
                );

                entityMap.put(
                        ByteTypeConverter.stringToByte("x"),
                        ByteTypeConverter.numToByte(randX)
                );

                entityMap.put(
                        ByteTypeConverter.stringToByte("y"),
                        ByteTypeConverter.numToByte(randY)
                );

                connection.stringCommands().incrBy(RedisKeys.ENTITY_BYTE, 1);

                connection.setCommands().sAdd(
                        RedisKeys.WORLD_BYTE,
                        curId
                );

                connection.hashCommands().hMSet(
                        entityKey,
                        entityMap
                );

                connection.geoCommands().geoAdd(RedisKeys.GEO_BYTE, point, entityKey);
            }
        };
    }

    public Consumer<RedisConnection> updateEntitiesPipe(List<RedisEntity> entities) {

        return connection -> {

            for (RedisEntity entity : entities) {

                connection.scriptingCommands().evalSha(
                        updateEntitySha,
                        ReturnType.INTEGER,
                        3,

                        ByteTypeConverter.stringToByte("entity:" + entity.getId()),
                        RedisKeys.GEO_BYTE,
                        RedisKeys.WORLD_BYTE,

                        ByteTypeConverter.numToByte(entity.getHp()),
                        ByteTypeConverter.stringToByte(String.valueOf(entity.getX())),
                        ByteTypeConverter.stringToByte(String.valueOf(entity.getY())),
                        ByteTypeConverter.stringToByte(entity.getState().getState()),
                        ByteTypeConverter.numToByte(entity.getStamina()),
                        ByteTypeConverter.stringToByte(String.valueOf(GeoUtil.scaleIn(entity.getX()))),
                        ByteTypeConverter.stringToByte(String.valueOf(GeoUtil.scaleIn(entity.getY()))),
                        ByteTypeConverter.stringToByte(String.valueOf(entity.getId()))
                );
            }

        };
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
                        geoMapper.entityToByteMap(entity);

                byte[] entityKeyBytes =
                        ByteTypeConverter.stringToByte(entityKey);

                connection.hashCommands().hMSet(
                        entityKeyBytes,
                        map
                );

                connection.setCommands().sAdd(
                        RedisKeys.WORLD_BYTE,
                        ByteTypeConverter.numToByte((int) id)
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
                connection.hashCommands().hGetAll(ByteTypeConverter.stringToByte(("entity:" + id)));
            }
        };
    }

    public Consumer<RedisConnection> getNearByIds(List<RedisEntity> entities, int range) {
        return geoService.getNearByIds(entities, range);
    }

    public Map<Long, List<RedisEntity>> geoSearchNearbyResultToIds(
            List<RedisEntity> entities,
            List<Object> geoResults,
            Map<Long, RedisEntity> entityMap
    ) {
        return entityMapper.geoSearchNearbyResultToIds(entities, geoResults, entityMap);
    }

    public Consumer<RedisConnection> getCollisionIds(List<NextMove> nextMoveList, double range) {
        return geoService.getCollisionIds(nextMoveList, range);
    }
}
