package com.example.world.stream.metrics;

import com.example.world.constants.RedisKeys;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

public class MetricsConsumer implements InitializingBean, DisposableBean, StreamListener<String, MapRecord<String, String, String>> {
    private final StreamMetric streamMetric;
    private final StringRedisTemplate redisTemplate;
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;
    private final String consumerName;
    private final ConsumerHelper consumerHelper;

    public MetricsConsumer(
            StreamMetric streamMetric,
            StringRedisTemplate redisTemplate,
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer,
            String consumerName,
            ConsumerHelper consumerHelper
    ) {
        this.streamMetric = streamMetric;
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
        this.consumerName = consumerName;
        this.consumerHelper = consumerHelper;
    }



    @Override
    public void afterPropertiesSet() {
        consumerHelper.createConsumerGroup();
        consumerHelper.createDlqConsumerGroup();

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .errorHandler(e -> System.out.println(e.getLocalizedMessage()))
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
        try {
            if(true) throw new RuntimeException("TEST Consumer Error");
            consumerHelper.process(message.getValue());
            consumerHelper.ack(message);
        } catch (Exception e) {
            streamMetric.incrementProcessError();
        }
    }

    @Override
    public void destroy() {
        if (listenerContainer != null) {
            listenerContainer.stop();
        }
    }

    @Scheduled(fixedDelay = 30_000)
    private void recoveryPending() {
        consumerHelper.recoveryPending(consumerName);
    }
}
