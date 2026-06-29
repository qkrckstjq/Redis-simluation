package com.example.world.event;

import com.example.world.service.RedisEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EntityEventScheduler {
    private final RedisEntityService redisEntityService;
    public static boolean RUN = false;
    public static boolean ASYNC = true;

    @Scheduled(fixedRate = 100)
    public void tick() {
        if(RUN) {
            if(ASYNC) redisEntityService.processTickListAsync();
            else redisEntityService.processTickListSync();
        }
    }
}
