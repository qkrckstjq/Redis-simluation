package com.example.world.constants;


import com.example.world.util.ByteTypeConverter;
import org.springframework.stereotype.Component;

@Component
public final class RedisKeys {
    public static final byte[] WORLD_BYTE =
            ByteTypeConverter.stringToByte("world:entities");

    public static final byte[] GEO_BYTE =
            ByteTypeConverter.stringToByte("geo:entities");

    public static final byte[] STREAM_BYTE =
            ByteTypeConverter.stringToByte("simulation-events");

    public static final byte[] ENTITY_BYTE =
            ByteTypeConverter.stringToByte("entity:id");

    public static final String WORLD_STR = "world:entities";
    public static final String GEO_STR = "geo:entities";
    public static final String SIMULATION_EVENTS_STR = "simulation-events";
    public static final String ENTITY_STR = "entity:id";
    public static final String GROUP_NAME = "entity-history-group";
    public static final String CONSUMER_NAME = "history-consumer";
    public static final String HISTORY_ENTITY_ = "history:entity:";
    public static final String TICK = "simulation:tick";
}
