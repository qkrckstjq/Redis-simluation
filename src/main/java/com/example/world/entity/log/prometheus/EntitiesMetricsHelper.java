package com.example.world.entity.log.prometheus;

import com.example.world.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntitiesMetricsHelper {
    private final RedisRepository redisRepository;
    private final EntitiesMetrics metrics;

    @Scheduled(fixedDelay = 5000)
    public void getEntitySize() {
        Long entitySize = redisRepository.getEntitiesSize();
        metrics.setEntitySize(entitySize);
    }
}
