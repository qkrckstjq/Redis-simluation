package com.example.world.service.ai;

import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
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

        RedisEntity target = entityMap.get(targetId);

        if (target.getType().equals(TypeEnum.SHEEP)) {
//            entity.setTargetId(null);
            return false;
        }

        return run(entity, target);
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
            List<RedisEntity> nearSheepList
    ) {
        int nearSheepSize = nearSheepList.size();
        if(nearSheepSize < 2) {
            entity.setState(StateEnum.MOVE);
//            entity.setTargetId(null);
            return;
        }
        entity.setState(StateEnum.FLOCK);
        entity.setTargetId(nearSheepList.get(nearSheepSize / 2).getId());
    }
}
