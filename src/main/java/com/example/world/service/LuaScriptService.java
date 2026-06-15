package com.example.world.service;

import com.example.world.constants.RedisKeys;
import com.example.world.entity.RedisEntity;
import com.example.world.repository.RedisRepository;
import com.example.world.util.ByteTypeConverter;
import com.example.world.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class LuaScriptService {
    private final String updateEntitySha;

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

                        ByteTypeConverter.IntegerToByte(entity.getHp()),
                        ByteTypeConverter.stringToByte(String.valueOf(entity.getX())),
                        ByteTypeConverter.stringToByte(String.valueOf(entity.getY())),
                        ByteTypeConverter.stringToByte(entity.getState().getState()),
                        ByteTypeConverter.IntegerToByte(entity.getStamina()),
                        ByteTypeConverter.stringToByte(String.valueOf(GeoUtil.scaleIn(entity.getX()))),
                        ByteTypeConverter.stringToByte(String.valueOf(GeoUtil.scaleIn(entity.getY()))),
                        ByteTypeConverter.stringToByte(String.valueOf(entity.getId()))
                );
            }

        };
    }

    public Consumer<RedisConnection> updateEntitiesPipeWithStream(List<RedisEntity> entities) {

        return connection -> {

            for (RedisEntity entity : entities) {

                connection.scriptingCommands().evalSha(
                        updateEntitySha,
                        ReturnType.INTEGER,
                        3,

                        ByteTypeConverter.stringToByte("entity:" + entity.getId()),
                        RedisKeys.GEO_BYTE,
                        RedisKeys.WORLD_BYTE,

                        ByteTypeConverter.IntegerToByte(entity.getHp()),
                        ByteTypeConverter.stringToByte(String.valueOf(entity.getX())),
                        ByteTypeConverter.stringToByte(String.valueOf(entity.getY())),
                        ByteTypeConverter.stringToByte(entity.getState().getState()),
                        ByteTypeConverter.IntegerToByte(entity.getStamina()),
                        ByteTypeConverter.stringToByte(String.valueOf(GeoUtil.scaleIn(entity.getX()))),
                        ByteTypeConverter.stringToByte(String.valueOf(GeoUtil.scaleIn(entity.getY()))),
                        ByteTypeConverter.stringToByte(String.valueOf(entity.getId()))
                );
            }

        };
    }
}
