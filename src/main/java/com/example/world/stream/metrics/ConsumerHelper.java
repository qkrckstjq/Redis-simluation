package com.example.world.stream.metrics;

import com.example.world.constants.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConsumerHelper {
    private final StringRedisTemplate redisTemplate;
    private final StreamMetric streamMetric;

    public void createConsumerGroup() {
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

    public void createDlqConsumerGroup() {
        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.SIMULATION_EVENTS_DLQ))) {
                redisTemplate.opsForStream().createGroup(
                        RedisKeys.SIMULATION_EVENTS_DLQ,
                        ReadOffset.latest(),
                        RedisKeys.METRICS_CONSUMER_GROUP
                );
            } else {
                redisTemplate.opsForStream().createGroup(
                        RedisKeys.SIMULATION_EVENTS_DLQ,
                        RedisKeys.METRICS_CONSUMER_GROUP
                );
            }
        } catch (Exception ignored) {
        }
    }

    public void recoveryPending(String consumerName) {
        PendingMessages pendingMessages =
                redisTemplate.opsForStream().pending(
                        RedisKeys.SIMULATION_EVENTS_STR,
                        RedisKeys.METRICS_CONSUMER_GROUP,
                        Range.unbounded(),
                        100L,
                        Duration.ofSeconds(30)
                );

        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        List<RecordId> processIds = new ArrayList<>();
        List<RecordId> dlqIds = new ArrayList<>();

        pendingMessages.forEach(p -> {
            if (p.getTotalDeliveryCount() >= 5) {
                dlqIds.add(p.getId());
            } else {
                processIds.add(p.getId());
            }
        });

        processPending(processIds, consumerName);

        processDlq(dlqIds, consumerName);
    }

    private void processPending(List<RecordId> ids, String consumerName) {

        if (ids.isEmpty()) {
            return;
        }

        List<MapRecord<String, Object, Object>> claimed = claim(ids, consumerName);

        for (MapRecord<String, Object, Object> record : claimed) {
            try {
                processAndAck(record);
            } catch (Exception e) {
                streamMetric.incrementPendingError();
            }
        }
    }

    private void processDlq(List<RecordId> ids, String consumerName) {

        if (ids.isEmpty()) {
            return;
        }

        List<MapRecord<String, Object, Object>> claimed = claim(ids, consumerName);

        for (MapRecord<String, Object, Object> record : claimed) {
            try {
                deadLetterStream(record);
            } catch (Exception e) {
                streamMetric.incrementDqlError();
            }
        }
    }

    private List<MapRecord<String, Object, Object>> claim(List<RecordId> ids, String consumerName) {
        return redisTemplate.opsForStream().claim(
                RedisKeys.SIMULATION_EVENTS_STR,
                RedisKeys.METRICS_CONSUMER_GROUP,
                consumerName,
                Duration.ofSeconds(30),
                ids.toArray(new RecordId[0])
        );
    }

    public void process(Map<String, String> value) {
        String state = value.get("state");
        streamMetric.incrementState(state);
    }

    public void ack(MapRecord<String, String, String> message) {
        redisTemplate.opsForStream().acknowledge(
                RedisKeys.METRICS_CONSUMER_GROUP,
                message
        );
    }

    private void processAndAck(MapRecord<String, Object, Object> record) {
//        if (true) {
//            throw new RuntimeException("TEST Pending Error");
//        }

        @SuppressWarnings("unchecked")
        Map<String, String> value =
                (Map<String, String>) (Map<?, ?>) record.getValue();

        process(value);

        redisTemplate.opsForStream().acknowledge(
                RedisKeys.METRICS_CONSUMER_GROUP,
                record
        );
    }

    private void deadLetterStream(MapRecord<String, Object, Object> record) {

        redisTemplate.opsForStream().add(
                RedisKeys.SIMULATION_EVENTS_DLQ,
                record.getValue()
        );

        redisTemplate.opsForStream().acknowledge(
                RedisKeys.SIMULATION_EVENTS_STR,
                RedisKeys.METRICS_CONSUMER_GROUP,
                record.getId()
        );
    }
}
