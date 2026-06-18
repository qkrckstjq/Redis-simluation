package com.example.world.service.ai;

import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
import com.example.world.service.EntityMapperImpl;
import com.example.world.service.EntityService;
import com.example.world.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiDecisionService {
    private final EntityMapperImpl entityMapperImpl;
    private final SheepAiService sheepAiService;
    private final WolfAiService wolfAiService;
    private final CommonAiService commonAiService;

    public void decideState(
            List<RedisEntity> entities,
            Map<Long, List<RedisEntity>> nearEntities,
            Map<Long, RedisEntity> entityMap
    ) {
        entities.forEach(entity -> {
            decideTypeBehavior(entity, nearEntities.get(entity.getId()), entityMap);
        });
    }

    public void decideTypeBehavior(
            RedisEntity entity,
            List<RedisEntity> entities,
            Map<Long, RedisEntity> entityMap
    ) {
        switch (entity.getType()) {
            case WOLF:
                wolfAi(entity, entities, entityMap);
                break;

            case SHEEP:
                sheepAi(entity, entities, entityMap);
                break;
        }
    }

    public void wolfAi(
            RedisEntity entity,
            List<RedisEntity> entities,
            Map<Long, RedisEntity> entityMap
    ) {
        if(wolfAiService.keepTarget(entity, entityMap)) {
            return;
        }

        for (RedisEntity target : entities) {
            if(target.getType().equals(TypeEnum.SHEEP)) {
                if(wolfAiService.tryAttackOrChase(entity, target)) return;
            } else if(target.getType().equals(TypeEnum.WOLF)) {
                if(commonAiService.trySpawn(entity, target)) return;
            }
        }
        entity.setTargetId(null);
    }

    public void sheepAi(
            RedisEntity entity,
            List<RedisEntity> entities,
            Map<Long, RedisEntity> entityMap
    ) {
        if(sheepAiService.keepRun(entity, entityMap)) {
            return;
        }

        List<RedisEntity> nearSheeps = new ArrayList<>();
        for (RedisEntity target : entities) {
            if(target.getType().equals(TypeEnum.WOLF)) {
                if(sheepAiService.run(entity, target)) return;
            } else if(target.getType().equals(TypeEnum.SHEEP)) {
                if(commonAiService.trySpawn(entity, target)) return;
                nearSheeps.add(target);
            }
        }
        sheepAiService.moveOrFlock(entity, nearSheeps);
    }
}
