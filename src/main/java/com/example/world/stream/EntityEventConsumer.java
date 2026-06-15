package com.example.world.stream;

import com.example.world.constants.RedisKeys;
import com.example.world.entity.SimulationEvent;
import com.example.world.entity.StateEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EntityEventConsumer implements InitializingBean, DisposableBean,
        StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate redisTemplate;
    private final HistoryService historyService;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;

    @Override
    public void afterPropertiesSet() {

        createConsumerGroup();

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .build();

        listenerContainer = StreamMessageListenerContainer.create(
                redisTemplate.getConnectionFactory(),
                options
        );

        listenerContainer.receive(
                Consumer.from(RedisKeys.GROUP_NAME, RedisKeys.CONSUMER_NAME),
                StreamOffset.create(RedisKeys.SIMULATION_EVENTS_STR, ReadOffset.lastConsumed()),
                this
        );

        listenerContainer.start();
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {

        try {

            SimulationEvent event = toSimulationEvent(message.getValue());

            historyService.save(event);

            redisTemplate.opsForStream().acknowledge(
                    RedisKeys.GROUP_NAME,
                    message
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {

        if (listenerContainer != null) {
            listenerContainer.stop();
        }
    }

    private void createConsumerGroup() {

        try {

            if (!Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.SIMULATION_EVENTS_STR))) {
                redisTemplate.opsForStream().createGroup(
                        RedisKeys.SIMULATION_EVENTS_STR,
                        ReadOffset.latest(),
                        RedisKeys.GROUP_NAME
                );
            } else {
                redisTemplate.opsForStream().createGroup(
                        RedisKeys.SIMULATION_EVENTS_STR,
                        RedisKeys.GROUP_NAME
                );
            }

        } catch (Exception ignored) {
        }
    }

    private SimulationEvent toSimulationEvent(Map<String, String> map) {

        Map<String, String> payload = new HashMap<>(map);

        payload.remove("state");
        payload.remove("tick");
        payload.remove("entityId");

        return new SimulationEvent(
                StateEnum.valueOf(map.get("state")),
                Long.parseLong(map.get("tick")),
                Long.parseLong(map.get("entityId")),
                payload
        );
    }
}
