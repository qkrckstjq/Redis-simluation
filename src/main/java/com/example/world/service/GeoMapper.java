package com.example.world.service;

import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
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
                ByteTypeConverter.IntegerToByte(Math.toIntExact(entity.getId()))
        );

        map.put(
                ByteTypeConverter.stringToByte("age"),
                ByteTypeConverter.IntegerToByte(entity.getAge())
        );

        map.put(
                ByteTypeConverter.stringToByte("hp"),
                ByteTypeConverter.IntegerToByte(entity.getHp())
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
                ByteTypeConverter.IntegerToByte(entity.getStamina())
        );

        map.put(
                ByteTypeConverter.stringToByte("breedReady"),
                ByteTypeConverter.stringToByte(String.valueOf(entity.isBreedReady()))
        );

        map.put(
                ByteTypeConverter.stringToByte("breedReadyTick"),
                ByteTypeConverter.IntegerToByte(entity.getBreedReadyTick())
        );

        map.put(
                ByteTypeConverter.stringToByte("cell"),
                ByteTypeConverter.stringToByte(entity.getCellKey())
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
                ByteTypeConverter.IntegerToByte(id)
        );

        map.put(
                ByteTypeConverter.stringToByte("age"),
                ByteTypeConverter.IntegerToByte(0)
        );

        map.put(
                ByteTypeConverter.stringToByte("hp"),
                ByteTypeConverter.IntegerToByte(hp)
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
                ByteTypeConverter.IntegerToByte(stamina)
        );

        map.put(
                ByteTypeConverter.stringToByte("x"),
                ByteTypeConverter.IntegerToByte(x)
        );

        map.put(
                ByteTypeConverter.stringToByte("y"),
                ByteTypeConverter.IntegerToByte(y)
        );

        map.put(
                ByteTypeConverter.stringToByte("breedReady"),
                ByteTypeConverter.stringToByte(String.valueOf(false))
        );

        map.put(
                ByteTypeConverter.stringToByte("breedReadyTick"),
                ByteTypeConverter.IntegerToByte(0)
        );

        map.put(
                ByteTypeConverter.stringToByte("cell"),
                ByteTypeConverter.stringToByte(cellKey)
        );
        return map;
    }
}
