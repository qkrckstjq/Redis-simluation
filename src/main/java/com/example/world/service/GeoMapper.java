package com.example.world.service;

import com.example.world.entity.RedisEntity;
import com.example.world.util.ByteTypeConverter;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class GeoMapper {
    public Map<byte[], byte[]> entityToByteMap(RedisEntity entity) {
        Map<byte[], byte[]> map = new HashMap<>();
        map.put(
                ByteTypeConverter.stringToByte("id"),
                ByteTypeConverter.numToByte(Math.toIntExact(entity.getId()))
        );

        map.put(
                ByteTypeConverter.stringToByte("age"),
                ByteTypeConverter.numToByte(entity.getAge())
        );

        map.put(
                ByteTypeConverter.stringToByte("hp"),
                ByteTypeConverter.numToByte(entity.getHp())
        );

        map.put(
                ByteTypeConverter.stringToByte("x"),
                ByteTypeConverter.stringToByte(String.valueOf(entity.getX()))
        );

        map.put(
                ByteTypeConverter.stringToByte("y"),
                ByteTypeConverter.stringToByte(String.valueOf(entity.getY()))
        );

        map.put(
                ByteTypeConverter.stringToByte("type"),
                ByteTypeConverter.stringToByte(entity.getType().name())
        );

        map.put(
                ByteTypeConverter.stringToByte("state"),
                ByteTypeConverter.stringToByte(entity.getState().getState())
        );

        map.put(
                ByteTypeConverter.stringToByte("stamina"),
                ByteTypeConverter.numToByte(entity.getStamina())
        );

        map.put(
                ByteTypeConverter.stringToByte("breedReady"),
                ByteTypeConverter.stringToByte(String.valueOf(entity.isBreedReady()))
        );

        map.put(
                ByteTypeConverter.stringToByte("breedReadyTick"),
                ByteTypeConverter.numToByte(entity.getBreedReadyTick())
        );

        map.put(
                ByteTypeConverter.stringToByte("cell"),
                ByteTypeConverter.stringToByte(entity.getCellKey())
        );

        map.put(
                ByteTypeConverter.stringToByte("targetId"),
                ByteTypeConverter.numToByte(entity.getTargetId())
        );
        return map;
    }

    public Map<byte[], byte[]> newEntityToByteMap(
            int id,
            int hp,
            String type,
            String state,
            int stamina,
            int x,
            int y,
            String cellKey
    ) {
        Map<byte[], byte[]> map = new HashMap<>();
        map.put(
                ByteTypeConverter.stringToByte("id"),
                ByteTypeConverter.numToByte(id)
        );

        map.put(
                ByteTypeConverter.stringToByte("age"),
                ByteTypeConverter.numToByte(0)
        );

        map.put(
                ByteTypeConverter.stringToByte("hp"),
                ByteTypeConverter.numToByte(hp)
        );

        map.put(
                ByteTypeConverter.stringToByte("type"),
                ByteTypeConverter.stringToByte(type)
        );

        map.put(
                ByteTypeConverter.stringToByte("state"),
                ByteTypeConverter.stringToByte(state)
        );

        map.put(
                ByteTypeConverter.stringToByte("stamina"),
                ByteTypeConverter.numToByte(stamina)
        );

        map.put(
                ByteTypeConverter.stringToByte("x"),
                ByteTypeConverter.numToByte(x)
        );

        map.put(
                ByteTypeConverter.stringToByte("y"),
                ByteTypeConverter.numToByte(y)
        );

        map.put(
                ByteTypeConverter.stringToByte("breedReady"),
                ByteTypeConverter.stringToByte(String.valueOf(false))
        );

        map.put(
                ByteTypeConverter.stringToByte("breedReadyTick"),
                ByteTypeConverter.numToByte(0)
        );

        map.put(
                ByteTypeConverter.stringToByte("cell"),
                ByteTypeConverter.stringToByte(cellKey)
        );

        map.put(
                ByteTypeConverter.stringToByte("targetId"),
                ByteTypeConverter.stringToByte("null")
        );
        return map;
    }
}
