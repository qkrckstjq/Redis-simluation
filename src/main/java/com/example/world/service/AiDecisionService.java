package com.example.world.service;

import com.example.world.entity.RedisEntity;
import com.example.world.entity.StateEnum;
import com.example.world.entity.TypeEnum;
import com.example.world.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.SQLOutput;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiDecisionService {
    private final EntityMapper entityMapper;

    public void decideState(List<RedisEntity> entities, Map<Long, List<RedisEntity>> nearEntities) {
        entities.forEach(entity -> {
            decideTypeBehavior(entity, nearEntities.get(entity.getId()));
        });
    }

    public void decideTypeBehavior(RedisEntity entity, List<RedisEntity> entities) {
        switch (entity.getType()) {
            case WOLF:
                wolfAi(entity, entities);
                break;

            case SHEEP:
                sheepAi(entity, entities);
                break;
        }
    }

    public void wolfAi(RedisEntity entity, List<RedisEntity> entities) {
        int x = entity.getX();
        int y = entity.getY();
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

    public void sheepAi(RedisEntity entity, List<RedisEntity> entities) {
        for (RedisEntity target : entities) {
            if (target != null && target.getType() == TypeEnum.WOLF) {
                entity.setState(StateEnum.RUN);
                entity.setTargetId(target.getId());
                return;
            }
        }
        entity.setTargetId(null);
//        entity.setState(StateEnum.MOVE);
    }
}
