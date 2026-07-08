package com.example.world.service;

import com.example.world.entity.RedisEntity;
import com.example.world.repository.RedisRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.example.world.service.batch.BatchProcessor.BATCH_SIZE;

@Component
@RequiredArgsConstructor
public class TickManager {
    private final AtomicLong tick = new AtomicLong();
    private final RedisRepository redisRepository;

    @PostConstruct
    public void init() {
        String tickValue = redisRepository.getTick();
        long redisTick = tickValue == null ? 1L : Long.parseLong(tickValue);
        this.tick.setOpaque(redisTick);
    }

    public long nextTick() {
        return tick.incrementAndGet();
    }

    public long currentTick() {
        return tick.get();
    }
}
