package com.example.world.stream;

import com.example.world.constants.RedisKeys;
import com.example.world.entity.HistoryEvent;
import com.example.world.entity.StreamEvent;
import com.example.world.util.ByteTypeConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class StreamService {
    private final EventMapper eventMapper;

    public Consumer<RedisConnection> publish(List<HistoryEvent> events) {

        return connection -> {

            for (HistoryEvent event : events) {

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

                body.put(
                        ByteTypeConverter.stringToByte("targetId"),
                        ByteTypeConverter.stringToByte(String.valueOf(event.getTargetId()))
                );

                body.put(
                        ByteTypeConverter.stringToByte("age"),
                        ByteTypeConverter.stringToByte(String.valueOf(event.getAge()))
                );

                connection.streamCommands().xAdd(
                        RedisKeys.STREAM_BYTE,
                        body
                );
            }

        };
    }

    public Consumer<RedisConnection> publishStreamEvents(List<StreamEvent> streamEvents) {
        return connection -> {
            for(StreamEvent event : streamEvents) {
                Map<byte[], byte[]> body = eventMapper.streamEventToMap(event);
                connection.streamCommands().xAdd(
                        RedisKeys.STREAM_BYTE,
                        body
                );
            }
        };
    }
}
