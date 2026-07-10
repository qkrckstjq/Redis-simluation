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
    public static final String METRICS_CONSUMER_GROUP = "metrics-consumer-group";
    public static final String METRICS_CONSUMER = "metrics-consumer";
    public static final String STATISTICS_CONSUMER_GROUP = "statistics-consumer-group";
    public static final String STATISTICS_CONSUMER = "statistics-consumer";
    public static final String HISTORY_ENTITY_ = "history:entity:";
    public static final String TICK = "simulation:tick";
}
