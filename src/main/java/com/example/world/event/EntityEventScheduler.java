package com.example.world.event;

import com.example.world.service.EntityService;
import com.example.world.service.RedisEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntityEventScheduler {
    private final EntityService entityService;
    public static boolean RUN = false;
    public static boolean ASYNC = true;

    @Scheduled(fixedRate = 100)
    public void tick() {
        if(RUN) {
            if(ASYNC) entityService.processTickListAsync();
            else entityService.processTickListSync();
//            redisEntityService.processTickListInMemoryAsync();
        }
    }
}
