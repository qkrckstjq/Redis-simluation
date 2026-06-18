package com.example.world.service.ai;

import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.service.EntityService;
import com.example.world.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WolfAiService {
    private final CommonAiService commonAiService;

    public boolean keepTarget(
            RedisEntity entity,
            Map<Long, RedisEntity> entityMap
    ) {
        Long targetId = entity.getTargetId();

        if (targetId == null)
            return false;

        RedisEntity target = entityMap.get(targetId);

        if (target == null)
            return false;

        if (EntityService.isDead(target))
            return false;

        return tryAttackOrChase(entity, target);
    }

    public boolean tryAttackOrChase(
            RedisEntity entity,
            RedisEntity target
    ) {
        double dist = commonAiService.getDistBetEntities(entity, target);
        if(dist <= 1.0) {
            entity.setState(StateEnum.ATTACK);
        } else {
            entity.setState(StateEnum.CHASE);
        }
        entity.setTargetId(target.getId());
        return true;
    }

//    private boolean tryBreed(
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
}
