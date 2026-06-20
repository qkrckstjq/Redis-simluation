package com.example.world.service.ai;

import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
import com.example.world.service.EntityService;
import com.example.world.util.GeoUtil;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CommonAiService {
    public double getDistBetEntities(
            RedisEntity entity1,
            RedisEntity entity2
    ) {
        return GeoUtil.getDist(entity1.getX(), entity1.getY(), entity2.getX(), entity2.getY());
    }

    public boolean trySpawn(
            RedisEntity entity1,
            RedisEntity entity2
    ) {
        double dist = getDistBetEntities(entity1, entity2);
        if(dist > 1.0) return false;
        boolean breedableEntity1 = EntityService.isBreedReady(entity1);
        boolean breedableEntity2 = EntityService.isBreedReady(entity2);

        if(!breedableEntity1 || !breedableEntity2) return false;
//        if(entity1.getType().equals(TypeEnum.SHEEP)) {
//            //양의 경우 두 양 모두 breedReady true여야함
//            if(!breedableEntity1 || !breedableEntity2) return false;
//        } else {
//            //늑대의 경우 두 늑대 중 한마리라도 true면 통과
//            if(!breedableEntity1 && !breedableEntity2) return false;
//        }
        entity1.setState(StateEnum.SPAWN);
        entity2.setState(StateEnum.SPAWN);
        entity1.setTargetId(entity2.getId());
        entity2.setTargetId(entity1.getId());
        return true;
    }

    public void initTarget(
            RedisEntity entity,
            Map<Long, RedisEntity> entityMap
    ) {
        Long targetId = entity.getTargetId();
        if(targetId == null) {
            entity.setTargetId(null);
            return;
        }
        RedisEntity target = entityMap.get(targetId);
        if(target == null || EntityService.isDead(target)) {
            entity.setTargetId(null);
            return;
        }
        double dist = getDistBetEntities(entity, target);
        if(dist > 10) {
            entity.setTargetId(null);
        }
    }
}
