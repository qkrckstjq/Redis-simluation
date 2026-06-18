package com.example.world.service.ai;

import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.service.EntityService;
import com.example.world.util.GeoUtil;
import org.springframework.stereotype.Service;

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

        if(!EntityService.isBreedReady(entity1) || !EntityService.isBreedReady(entity2)) return false;

        entity1.setState(StateEnum.SPAWN);
        entity2.setState(StateEnum.SPAWN);
        entity1.setTargetId(entity2.getId());
        entity2.setTargetId(entity1.getId());
        return true;
    }
}
