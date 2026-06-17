package com.example.world.service;

import com.example.world.entity.*;
import com.example.world.util.GeoUtil;
import com.example.world.util.RandUtil;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BehaviorService {
    private final Random random = new Random();

    public void moveWithCollision(List<NextMove> nextMoves, Map<Long, List<Long>> collisionResults) {
        for (NextMove nextMove : nextMoves) {

            RedisEntity entity = nextMove.getEntity();

            List<Long> collision =
                    collisionResults.get(entity.getId());

            int prevX = entity.getX();
            int prevY = entity.getY();
            int nextX = nextMove.getNextX();
            int nextY = nextMove.getNextY();

            if (prevX != nextX || prevY != nextY) {
                entity.decreaseStamina();
            } else {
                entity.increaseStamina();
            }

            if (!EntityService.isDead(entity)) {
                entity.increaseHp();
            }

            if (collision.isEmpty()) {
                entity.setX(nextX);
                entity.setY(nextY);
            }
        }
    }

    public BehaviorResult moveNext(
            RedisEntity entity,
            Map<Long, RedisEntity> entityMap,
            Map<Long, List<RedisEntity>> nearEntities,
            List<RedisEntity> spawnList) {
        switch (entity.getState()) {
            case MOVE:
                return moveRand(entity);

            case IDLE:
                return moveIDLE(entity);

            case ATTACK:
                return moveAttack(entity, entityMap);

            case CHASE:
                return moveChase(entity, entityMap);

            case REST:
                return moveRest(entity);

            case RUN:
                return moveRun(entity, entityMap);

            case FLOCK:
                return moveFlock(entity, nearEntities);

            case SPAWN:
                return spawn(entity, entityMap, spawnList);

            default:
                throw new IllegalStateException("Unknown state : " + entity.getState());
        }
    }

    public List<NextMove> decideMoves(
            List<RedisEntity> entities,
            Map<Long, RedisEntity> entityMap,
            Map<Long, List<RedisEntity>> nearEntities,
            List<RedisEntity> spawnList) {

        List<NextMove> nextMoves = new ArrayList<>();
        Map<Position, Long> map = new HashMap<>();

        for (RedisEntity entity : entities) {
            BehaviorResult behavior = moveNext(entity, entityMap, nearEntities, spawnList);
            resolveMove(entity, behavior, map, nextMoves);
        }
        return nextMoves;
    }

    private void resolveMove(
            RedisEntity entity,
            BehaviorResult result,
            Map<Position, Long> occupied,
            List<NextMove> nextMoves
    ) {

        Position position = result.getNextPosition();

        if (occupied.putIfAbsent(position, entity.getId()) != null) {
            return;
        }

        nextMoves.add(
                new NextMove(
                        entity,
                        position.getX(),
                        position.getY()
                )
        );
    }


    public BehaviorResult moveRand(RedisEntity entity) {
        if(entity.getStamina() < 50) {
            entity.setState(StateEnum.REST);
            return new BehaviorResult(
                    StateEnum.REST,
                    new Position(entity.getX(), entity.getY()),
                    null
            );
        }

        boolean keepMove = random.nextInt(10) > 1; // 80%
        int dx = 0;
        int dy = 0;

        if(keepMove) {
            dx = random.nextInt(3) - 1;
            dy = random.nextInt(3) - 1;
        } else {
            entity.setState(StateEnum.IDLE);
        }

        int nextX = GeoUtil.setCoordinate(entity.getX() + dx);
        int nextY = GeoUtil.setCoordinate(entity.getY() + dy);

        return new BehaviorResult(
                keepMove ? entity.getState() : StateEnum.IDLE,
                new Position(nextX, nextY),
                null
        );
    }

    public BehaviorResult moveIDLE(RedisEntity entity) {
        boolean keepIdle = random.nextInt(10) > 7; //20%
        int dx = 0;
        int dy = 0;

        if(!keepIdle) {
            dx = random.nextInt(3) - 1;
            dy = random.nextInt(3) - 1;
            entity.setState(StateEnum.MOVE);
        }

        int nextX = GeoUtil.setCoordinate(entity.getX() + dx);
        int nextY = GeoUtil.setCoordinate(entity.getY() + dy);

        return new BehaviorResult(
                keepIdle ? entity.getState() : StateEnum.MOVE,
                new Position(nextX, nextY),
                null
        );
    }

    public BehaviorResult moveRest(RedisEntity entity) {
        if(entity.getStamina() < 70) {
            return new BehaviorResult(
                    StateEnum.REST,
                    new Position(entity.getX(), entity.getY()),
                    null
            );
        } else {
            entity.setState(StateEnum.MOVE);
        }

        int dx = random.nextInt(3) - 1;
        int dy = random.nextInt(3) - 1;

        int nextX = GeoUtil.setCoordinate(entity.getX() + dx);
        int nextY = GeoUtil.setCoordinate(entity.getY() + dy);

        return new BehaviorResult(
                StateEnum.MOVE,
                new Position(nextX, nextY),
                null
        );
    }

    public BehaviorResult moveChase(RedisEntity entity, Map<Long, RedisEntity> entityMap) {
        RedisEntity target = entityMap.get(entity.getTargetId());
        if(entity.getStamina() < 50) return moveRest(entity);
        if (target == null) {
            entity.setState(StateEnum.MOVE);
            entity.setTargetId(null);

            return new BehaviorResult(
                    StateEnum.MOVE,
                    new Position(entity.getX(), entity.getY()),
                    null
            );
        }

        int curX = entity.getX();
        int curY = entity.getY();

        int targetX = target.getX();
        int targetY = target.getY();

        int dx = Integer.compare(targetX, curX);
        int dy = Integer.compare(targetY, curY);

        int nextX = GeoUtil.setCoordinate(curX + dx);
        int nextY = GeoUtil.setCoordinate(curY + dy);

        return new BehaviorResult(
                StateEnum.CHASE,
                new Position(nextX, nextY),
                entity.getTargetId()
        );
    }

    public BehaviorResult moveRun(RedisEntity entity, Map<Long, RedisEntity> entityMap) {
        RedisEntity target = entityMap.get(entity.getTargetId());
        if(entity.getStamina() <= 0) return moveRest(entity);
        if (target == null) {
            entity.setState(StateEnum.MOVE);
            entity.setTargetId(null);

            return new BehaviorResult(
                    StateEnum.MOVE,
                    new Position(entity.getX(), entity.getY()),
                    null
            );
        }

        int curX = entity.getX();
        int curY = entity.getY();

        int targetX = target.getX();
        int targetY = target.getY();

        int dx = Integer.compare(targetX, curX);
        int dy = Integer.compare(targetY, curY);

        int nextX = GeoUtil.setCoordinate(curX - dx);
        int nextY = GeoUtil.setCoordinate(curY - dy);

        return new BehaviorResult(
                StateEnum.RUN,
                new Position(nextX, nextY),
                entity.getTargetId()
        );
    }

    public BehaviorResult moveAttack(
            RedisEntity entity,
            Map<Long, RedisEntity> entityMap
    ) {
        RedisEntity target = entityMap.get(entity.getTargetId());
        if(entity.getStamina() <= 0) return moveRest(entity);

        if (target == null) {
            entity.setState(StateEnum.MOVE);
            entity.setTargetId(null);

            return new BehaviorResult(
                    StateEnum.MOVE,
                    new Position(entity.getX(), entity.getY()),
                    null
            );
        }

        target.attackedByWolf();

        int curX = entity.getX();
        int curY = entity.getY();

        int targetX = target.getX();
        int targetY = target.getY();

        int dx = Integer.compare(targetX, curX);
        int dy = Integer.compare(targetY, curY);

        int nextX = GeoUtil.setCoordinate(curX + dx);
        int nextY = GeoUtil.setCoordinate(curY + dy);

        return new BehaviorResult(
                StateEnum.ATTACK,
                new Position(nextX, nextY),
                entity.getTargetId()
        );
    }

    private BehaviorResult moveFlock(
            RedisEntity entity,
            Map<Long, List<RedisEntity>> entityMap
    ) {
        if(entity.getStamina() <= 0) return moveRest(entity);
        if(RandUtil.percent(20)){
            return moveRand(entity);
        }

        int curX = entity.getX();
        int curY = entity.getY();
        int targetX = entity.getX();
        int targetY = entity.getY();
        List<RedisEntity> sheepList = entityMap.get(entity.getId()).stream()
                .filter(e -> e.getType().equals(TypeEnum.SHEEP))
                .toList();
        int sheepCount = sheepList.size() + 1;
        for(RedisEntity e : sheepList) {
            targetX += e.getX();
            targetY += e.getY();
        }
        int avgX = targetX / sheepCount;
        int avgY = targetY / sheepCount;

        double dist = GeoUtil.getDist(
                curX,
                curY,
                avgX,
                avgY
        );

        if(dist < 2.0){
            return moveRand(entity);
        }

        int dx = Integer.compare(avgX, curX);
        int dy = Integer.compare(avgY, curY);

        int nextX = GeoUtil.setCoordinate(curX + dx);
        int nextY = GeoUtil.setCoordinate(curY + dy);

        return new BehaviorResult(
                StateEnum.FLOCK,
                new Position(nextX, nextY),
                entity.getTargetId()
        );
    }

    private BehaviorResult spawn(
            RedisEntity entity,
            Map<Long, RedisEntity> entityMap,
            List<RedisEntity> spawnList
    ) {
        RedisEntity partner = entityMap.get(entity.getTargetId());
        if (entity.getId() > partner.getId()) {
            return moveRand(entity);
        }

        if (!RandUtil.percent(50)) {
            return moveRand(entity);
        }

        if (partner == null) {
            return moveRand(entity);
        }

        int childX = GeoUtil.setCoordinate(
                entity.getX() + RandUtil.getIntRand(-1, 1)
        );

        int childY = GeoUtil.setCoordinate(
                entity.getY() + RandUtil.getIntRand(-1, 1)
        );

        RedisEntity child = new RedisEntity(
                null,
                entity.getType(),
                StateEnum.MOVE,
                100,
                100,
                childX,
                childY,
                null
        );

        spawnList.add(child);

        entity.setHp(entity.getHp() - 20);
        entity.setStamina(entity.getStamina() - 40);

        partner.setHp(partner.getHp() - 20);
        partner.setStamina(partner.getStamina() - 40);

        return new BehaviorResult(
                StateEnum.SPAWN,
                new Position(entity.getX(), entity.getY()),
                null
        );
    }
}
