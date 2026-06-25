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
    private final String createEntitySha;
    private final String spawnEntitySha;
    private final CellManager cellManager;
    private final GeoMapper geoMapper;
    private final GeoService geoService;
    private final EntityMapperImpl entityMapper;

    public Consumer<RedisConnection> saveNewEntities(
            String type,
            Long nextId,
            int count
    ) {

        int start = nextId.intValue();

        return connection -> {

            for (int i = start; i < start + count; i++) {

                int randX = RandUtil.getIntRand(GeoUtil.MIN_COORDINATE, GeoUtil.MAX_COORDINATE);

                int randY = RandUtil.getIntRand(GeoUtil.MIN_COORDINATE, GeoUtil.MAX_COORDINATE);

                String cellKey = cellManager.getGeoKey(randX, randY);

                String entityKey = "{%s}:entity:%d".formatted(cellKey, i);

                connection.scriptingCommands().evalSha(
                        createEntitySha,
                        ReturnType.INTEGER,
                        4,
                        ByteTypeConverter.stringToByte(entityKey),
                        ByteTypeConverter.stringToByte(cellKey),
                        RedisKeys.WORLD_BYTE,
                        RedisKeys.ENTITY_BYTE,
                        ByteTypeConverter.numToByte(i),
                        ByteTypeConverter.numToByte(100),
                        ByteTypeConverter.stringToByte(type),
                        ByteTypeConverter.stringToByte(StateEnum.MOVE.getState()),
                        ByteTypeConverter.numToByte(100),
                        ByteTypeConverter.numToByte(randX),
                        ByteTypeConverter.numToByte(randY),
                        ByteTypeConverter.stringToByte(cellKey),
                        ByteTypeConverter.stringToByte(String.valueOf(GeoUtil.scaleIn(randX))),
                        ByteTypeConverter.stringToByte(String.valueOf(GeoUtil.scaleIn(randY)))
                );
            }
        };
    }

    public Consumer<RedisConnection> updateEntitiesPipe(
            List<RedisEntity> entities
    ) {

        return connection -> {

            for (RedisEntity entity : entities) {

                connection.scriptingCommands().evalSha(
                        updateEntitySha,
                        ReturnType.INTEGER,
                        3,

                        ByteTypeConverter.stringToByte(
                                "entity:" + entity.getId()
                        ),

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
            Long nextEntityId
    ) {

        return connection -> {

            long id = nextEntityId;

            for (RedisEntity entity : spawnList) {

                entity.setId(id);

                String cellKey = cellManager.getGeoKey(entity.getX(), entity.getY());
                entity.setCellKey(cellKey);
                String entityKey = "{%s}:entity:%d".formatted(cellKey, id);

                connection.scriptingCommands().evalSha(
                        spawnEntitySha,
                        ReturnType.INTEGER,
                        3,
                        ByteTypeConverter.stringToByte(entityKey),
                        ByteTypeConverter.stringToByte(cellKey),
                        RedisKeys.WORLD_BYTE,
                        ByteTypeConverter.numToByte(entity.getId()),
                        ByteTypeConverter.numToByte(entity.getAge()),
                        ByteTypeConverter.stringToByte(entity.getType().name()),
                        ByteTypeConverter.stringToByte(entity.getState().name()),
                        ByteTypeConverter.numToByte(entity.getStamina()),
                        ByteTypeConverter.numToByte(entity.getHp()),
                        ByteTypeConverter.numToByte(entity.getX()),
                        ByteTypeConverter.numToByte(entity.getY()),
                        ByteTypeConverter.stringToByte(String.valueOf(entity.isBreedReady())),
                        ByteTypeConverter.numToByte(entity.getBreedReadyTick()),
                        ByteTypeConverter.stringToByte(cellKey),
                        ByteTypeConverter.stringToByte(
                                entity.getTargetId() == null ? "" : String.valueOf(entity.getTargetId())
                        ),
                        ByteTypeConverter.stringToByte(String.valueOf(GeoUtil.scaleIn(entity.getX()))),
                        ByteTypeConverter.stringToByte(String.valueOf(GeoUtil.scaleIn(entity.getY())))
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
