package com.example.world.service.ai;

import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
import com.example.world.service.EntityService;
import com.example.world.util.RandUtil;
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

        //sheep기준 target이 sheep이면 run 중지
        if(target.getType().equals(TypeEnum.SHEEP)) {
//            entity.setTargetId(null);
            return false;
        }

        //sheep기준 target이 wolf일 경우
        return run(entity, target);
    }

    public boolean keepFlock(
            RedisEntity entity,
            Map<Long, RedisEntity> entityMap
    ) {
//        if(!entity.getType().equals(TypeEnum.SHEEP)) {
//            return false;
//        }
        Long targetId = entity.getTargetId();

        if(targetId == null)
            return false;

        RedisEntity target = entityMap.get(targetId);

        if(target == null)
            return false;

        if(RandUtil.percent(20)) {
            entity.setState(StateEnum.MOVE);
            entity.setTargetId(null);
            return true;
        }

        if(!(commonAiService.getDistBetEntities(entity, target) > 5)) {
            entity.setState(StateEnum.FLOCK);
            return true;
        }

        return false;
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
        if(entity.getStamina() < 50) {
            return;
        }
        int nearSheepSize = nearSheepList.size();
        if(RandUtil.percent(20) || nearSheepSize < 2) {
            entity.setState(StateEnum.MOVE);
            entity.setTargetId(null);
            return;
        }
        entity.setState(StateEnum.FLOCK);
        entity.setTargetId(nearSheepList.getFirst().getId());
    }
}
