package com.example.world.service.ai;

import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.service.EntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SheepAiService {
    private final CommonAiService commonAiService;


    public boolean run(
            RedisEntity entity,
            RedisEntity target
    ) {
        entity.setState(StateEnum.RUN);
        entity.setTargetId(target.getId());
        return true;
    }

    public boolean keepRun(
            RedisEntity entity,
            Map<Long, RedisEntity> entityMap
    ) {

        Long targetId = entity.getTargetId();

        if (targetId == null) {
            return false;
        }

        RedisEntity wolf = entityMap.get(targetId);

        if (wolf == null || EntityService.isDead(wolf)) {
            entity.setTargetId(null);
            return false;
        }

        return run(entity, wolf);
    }

//    public boolean trySpawn(
//            RedisEntity entity1,
//            RedisEntity entity2
//    ) {
//        double dist = commonAiService.getDistBetEntities(entity1, entity2);
//        if(dist > 1.0) return false;
//
//        if(!EntityService.isBreedReady(entity1) || !EntityService.isBreedReady(entity2)) return false;
//
//        entity1.setState(StateEnum.SPAWN);
//        entity1.setTargetId(entity2.getId());
//        return true;
//    }

    public void moveOrFlock(
            RedisEntity entity,
            int sheepCount
    ) {
        if(sheepCount < 2) {
            entity.setState(StateEnum.MOVE);
        } else {
            entity.setState(StateEnum.FLOCK);
        }
        entity.setTargetId(null);
    }
}
