package com.example.world.service;

import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
import com.example.world.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiDecisionService {
    private final EntityMapperImpl entityMapperImpl;

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
        int x = entity.getX();
        int y = entity.getY();
        Long targetId = entity.getTargetId();

        if(keepTarget(entity, entityMap)) {
            return;
        }

        for (RedisEntity target : entities) {
            if (target != null && target.getType() == TypeEnum.SHEEP) {
                int targetX = target.getX();
                int targetY = target.getY();
                double dist = GeoUtil.getDist(x, y, targetX, targetY);
                if(dist <= 1.0) {
                    entity.setState(StateEnum.ATTACK);
                } else {
                    entity.setState(StateEnum.CHASE);
                }

                entity.setTargetId(target.getId());
                return;
            }
        }
        entity.setTargetId(null);
//        entity.setState(StateEnum.MOVE);
    }

    public void sheepAi(
            RedisEntity entity,
            List<RedisEntity> entities,
            Map<Long, RedisEntity> entityMap
    ) {
        if(keepSheepRun(entity, entityMap)) {
            return;
        }

        int sheepCount = 0;
        for (RedisEntity target : entities) {
            if(target.getType().equals(TypeEnum.SHEEP)) {
                sheepCount++;
            }
            if (target.getType().equals(TypeEnum.WOLF)) {
                entity.setState(StateEnum.RUN);
                entity.setTargetId(target.getId());
                return;
            }
        }

        if(sheepCount < 2) {
            entity.setState(StateEnum.MOVE);
            return;
        }

        entity.setState(StateEnum.FLOCK);
        entity.setTargetId(null);
//        entity.setState(StateEnum.MOVE);
    }

    private boolean keepTarget(
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

        double dist = GeoUtil.getDist(
                entity.getX(),
                entity.getY(),
                target.getX(),
                target.getY()
        );

        if (dist <= 1) {
            entity.setState(StateEnum.ATTACK);
            return true;
        }

        if (dist <= 10) {
            entity.setState(StateEnum.CHASE);
            return true;
        }

        return false;
    }

    private boolean keepSheepRun(
            RedisEntity entity,
            Map<Long, RedisEntity> entityMap
    ) {

        Long targetId = entity.getTargetId();

        if (targetId == null) {
            return false;
        }

        RedisEntity wolf = entityMap.get(targetId);

        if (wolf == null) {
            return false;
        }

        double dist = GeoUtil.getDist(
                entity.getX(),
                entity.getY(),
                wolf.getX(),
                wolf.getY()
        );

        if (dist <= 10.0) {
            entity.setState(StateEnum.RUN);
            return true;
        }

        return false;
    }

    private boolean trySpawn(
            RedisEntity entity1,
            RedisEntity entity2
    ) {
        int curHp = entity1.getHp();
        int targetHp = entity2.getHp();
        int curStamina = entity1.getStamina();
        int targetStamina = entity2.getStamina();
        double dist = GeoUtil.getDist(entity1.getX(), entity1.getY(), entity2.getX(), entity2.getY());

        if(curHp < 80 || targetHp < 80) return false;
        if(curStamina < 80 || targetStamina < 80) return false;
        if(dist > 1) return false;
        entity1.setState();
    }


}
