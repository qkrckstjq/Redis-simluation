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

        commonAiService.initTarget(entity, entityMap);

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

        if (entities == null || entities.isEmpty()) {
            return;
        }
        List<RedisEntity> targetList = new ArrayList<>();
        for (RedisEntity target : entities) {
            if(target.getType().equals(TypeEnum.SHEEP)) {
                if(targetList.isEmpty()) wolfAiService.tryAttackOrChase(entity, target);
                targetList.add(target);
            } else if(target.getType().equals(TypeEnum.WOLF)) {
                if(commonAiService.trySpawn(entity, target)) return;
            }
        }
        targetList.forEach(sheep -> {
            sheep.setTargetId(entity.getId());
        });

        if(targetList.isEmpty()) {
            entity.setTargetId(null);
        }
    }

    public void sheepAi(
            RedisEntity entity,
            List<RedisEntity> entities,
            Map<Long, RedisEntity> entityMap
    ) {
        if(sheepAiService.keepRun(entity, entityMap)) {
            return;
        }

        if(sheepAiService.keepFlock(entity, entityMap)) {
            return;
        }

        if (entities == null || entities.isEmpty()) {
            return;
        }
//        int sheepCount = 0;
        List<RedisEntity> nearSheepList = new ArrayList<>();
        for (RedisEntity target : entities) {
            if(target.getType().equals(TypeEnum.WOLF)) {
                if(sheepAiService.run(entity, target)) return;
            } else if(target.getType().equals(TypeEnum.SHEEP)) {
                if(commonAiService.trySpawn(entity, target)) return;
                nearSheepList.add(target);
//                sheepCount++;
            }
        }
        sheepAiService.moveOrFlock(entity, nearSheepList);
    }
}
