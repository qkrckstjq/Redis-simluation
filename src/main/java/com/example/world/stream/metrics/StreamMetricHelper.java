package com.example.world.stream.metrics;

import com.example.world.constants.RedisKeys;
import com.example.world.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.connection.stream.StreamInfo.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StreamMetricHelper {
    private final RedisRepository redisRepository;
    private final StreamMetric streamMetric;

    @Scheduled(fixedDelay = 5000)
    private void getEventStreamLength() {
        Long size = redisRepository.getStreamSize(RedisKeys.SIMULATION_EVENTS_STR);
        streamMetric.setEventSize(size);
    }

    @Scheduled(fixedDelay = 5000)
    private void getPendingLength() {
        PendingMessagesSummary pending = redisRepository.getPending(
                RedisKeys.SIMULATION_EVENTS_STR,
                RedisKeys.METRICS_CONSUMER_GROUP
        );
        Long pendingSize = pending.getTotalPendingMessages();
        streamMetric.setPendingSize(pendingSize);
    }
}
