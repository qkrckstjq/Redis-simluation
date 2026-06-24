package com.example.world.service.ai;

import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
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

        if (target == null) {
            return false;
        }

        if(target.getType().equals(TypeEnum.WOLF)) {
            return commonAiService.trySpawn(entity, target);
        }

        if (target.isDead())
            return false;

        return tryAttackOrChase(entity, target);
    }

    public boolean tryAttackOrChase(
            RedisEntity entity,
            RedisEntity target
    ) {
        if(entity.getStamina() <= 0) {
            entity.setState(StateEnum.REST);
            return false;
        }
        double dist = commonAiService.getDistBetEntities(entity, target);
        if(dist <= 1.5) {
            entity.setState(StateEnum.ATTACK);
        } else {
            if(entity.getStamina() < 50 && entity.getAge() < 500) {
                entity.setState(StateEnum.REST);
            } else {
                entity.setState(StateEnum.CHASE);
            }
        }
        target.setState(StateEnum.RUN);
        entity.setTargetId(target.getId());
        return true;
    }

    public boolean followBreedableWolf(
            RedisEntity entity1,
            RedisEntity entity2
    ) {
        double dist = commonAiService.getDistBetEntities(entity1, entity2);

        boolean breedableEntity1 = entity1.isBreedReady();
        boolean breedableEntity2 = entity2.isBreedReady();

        if (dist <= 1.0) return false;
        if (!breedableEntity1 || !breedableEntity2) return false;

        entity1.setState(StateEnum.CHASE);
        entity1.setTargetId(entity2.getId());

        entity2.setState(StateEnum.CHASE);
        entity2.setTargetId(entity1.getId());
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
