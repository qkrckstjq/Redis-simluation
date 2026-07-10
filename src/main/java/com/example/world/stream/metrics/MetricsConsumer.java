package com.example.world.stream.metrics;

import com.example.world.constants.RedisKeys;
import com.example.world.stream.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.*;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

public class MetricsConsumer implements InitializingBean, DisposableBean, StreamListener<String, MapRecord<String, String, String>> {
    private final StreamMetric streamMetric;
    private final StringRedisTemplate redisTemplate;
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;
    private final String consumerName;

    public MetricsConsumer(
            StreamMetric streamMetric,
            StringRedisTemplate redisTemplate,
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer,
            String consumerName
    ) {
        this.streamMetric = streamMetric;
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
        this.consumerName = consumerName;
    }



    @Override
    public void afterPropertiesSet() {

        createConsumerGroup();

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .build();

        listenerContainer = StreamMessageListenerContainer.create(
                redisTemplate.getConnectionFactory(),
                options
        );

        listenerContainer.receive(
                Consumer.from(RedisKeys.METRICS_CONSUMER_GROUP, consumerName),
                StreamOffset.create(RedisKeys.SIMULATION_EVENTS_STR, ReadOffset.lastConsumed()),
                this
        );

        listenerContainer.start();
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Map<String, String> value = message.getValue();

        String state = value.get("state");
        streamMetric.increment(state);

        redisTemplate.opsForStream().acknowledge(
                RedisKeys.METRICS_CONSUMER_GROUP,
                message
        );
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
                        RedisKeys.METRICS_CONSUMER_GROUP
                );
            } else {
                redisTemplate.opsForStream().createGroup(
                        RedisKeys.SIMULATION_EVENTS_STR,
                        RedisKeys.METRICS_CONSUMER_GROUP
                );
            }

        } catch (Exception ignored) {
        }
    }

}
