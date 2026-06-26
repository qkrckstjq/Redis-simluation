package com.example.world.event;

import com.example.world.service.EntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EntityEventScheduler {
    private final EntityService entityService;
    public static boolean RUN = false;
    public static boolean ASYNC = true;

    @Scheduled(fixedRate = 100)
    public void tick() {
        if(RUN) {
            if(ASYNC) entityService.processTickListAsync();
            else entityService.processTickListSync();
        }
    }
}
