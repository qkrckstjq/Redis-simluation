package com.example.world.stream;

import com.example.world.constants.RedisKeys;
import com.example.world.entity.SimulationEvent;
import com.example.world.util.ByteTypeConverter;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class StreamService {

    public Consumer<RedisConnection> publish(List<SimulationEvent> events) {

        return connection -> {

            for (SimulationEvent event : events) {

                Map<byte[], byte[]> body = new HashMap<>();

                body.put(
                        ByteTypeConverter.stringToByte("state"),
                        ByteTypeConverter.stringToByte(event.getState().toString())
                );

                body.put(
                        ByteTypeConverter.stringToByte("tick"),
                        ByteTypeConverter.stringToByte(String.valueOf(event.getTick()))
                );

                body.put(
                        ByteTypeConverter.stringToByte("entityId"),
                        ByteTypeConverter.stringToByte(String.valueOf(event.getEntityId()))
                );

                event.getPayload().forEach((k, v) ->
                        body.put(
                                ByteTypeConverter.stringToByte(k),
                                ByteTypeConverter.stringToByte(v)
                        )
                );

                connection.streamCommands().xAdd(
                        RedisKeys.STREAM_BYTE,
                        body
                );
            }

        };
    }
}
