package com.example.world.service;

import com.example.world.entity.*;
import com.example.world.service.inmemory.EntityManager;
import com.example.world.util.GeoUtil;
import com.example.world.util.RandUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BehaviorService {
    private final Random random = new Random();
    private final CollisionService collisionService;
    private final EntityManager entityManager;

    public void moveWithCollision(List<NextMove> nextMoves, Map<Long, List<Long>> collisionResults) {
        for (NextMove nextMove : nextMoves) {
            RedisEntity entity = nextMove.getEntity();

//            List<Long> collision =
//                    collisionResults.get(entity.getId());

            int prevX = entity.getX();
            int prevY = entity.getY();
            int nextX = nextMove.getNextX();
            int nextY = nextMove.getNextY();
            boolean blocked = !(prevX != nextX || prevY != nextY);

            if (blocked) {
                handleBlockedEntity(entity);
            } else {
                handleUnBlockedEntity(entity);
            }

            if (!entity.isDead()) {
                handleLiveEntity(entity);
            }

//            if (collision.isEmpty()) {
//                entity.setX(nextX);
//                entity.setY(nextY);
//            }

            entity.setX(nextX);
            entity.setY(nextY);
        }
    }

    public BehaviorResult moveNext(
            RedisEntity entity,
            Map<Long, RedisEntity> entityMap,
            Map<Long, List<RedisEntity>> nearEntities,
            List<RedisEntity> spawnList) {
        return switch (entity.getState()) {
            case MOVE -> moveRand(entity);
            case IDLE -> moveIDLE(entity);
            case ATTACK -> moveAttack(entity, entityMap);
            case CHASE -> moveChase(entity, entityMap);
            case REST -> moveRest(entity);
            case RUN -> moveRun(entity, entityMap);
            case FLOCK -> moveFlock(entity, entityMap);
            case SPAWN -> spawn(entity, entityMap, spawnList);
            default -> throw new IllegalStateException("Unknown state : " + entity.getState());
        };
    }

//    public List<NextMove> decideMoves(
//            List<RedisEntity> entities,
//            Map<Long, RedisEntity> entityMap,
//            Map<Long, List<RedisEntity>> nearEntities,
//            List<RedisEntity> spawnList) {
//
//        List<NextMove> nextMoves = new ArrayList<>();
//        Map<Position, Long> map = new HashMap<>();
//
//        for (RedisEntity entity : entities) {
//            BehaviorResult behavior = moveNext(entity, entityMap, nearEntities, spawnList);
//            resolveMove(entity, behavior, map, nextMoves);
//        }
//        return nextMoves;
//    }

    public List<NextMove> decideMoves(
            List<RedisEntity> entities,
            Map<Long, RedisEntity> entityMap,
            Map<Long, List<RedisEntity>> nearEntities,
            List<RedisEntity> spawnList
    ) {

        List<NextMove> result = new ArrayList<>();

        for (RedisEntity entity : entities) {
            BehaviorResult behavior = moveNext(entity, entityMap, nearEntities, spawnList);
            Position nextPosition = behavior.getNextPosition();

            NextMove prevMove = new NextMove(entity, entity.getX(), entity.getY());
            NextMove nextMove = new NextMove(entity, nextPosition.getX(), nextPosition.getY());
            if(collisionService.tryMove(nextMove)) {
                result.add(nextMove);
            } else {
                result.add(prevMove);
            }
        }

        return result;
    }

//    private void resolveMove(
//            RedisEntity entity,
//            BehaviorResult result,
//            Map<Position, Long> occupied,
//            List<NextMove> nextMoves
//    ) {
//
//        Position position = result.getNextPosition();
////        Long occupiedId = occupied.putIfAbsent(position, entity.getId());
////
////        if (occupiedId != null) {
////            if (entity.getId() == 5805) {
////                System.out.println(
////                        "5805 blocked by " + occupiedId +
////                                " at " + position
////                );
////            }
////            return;
////        }
////
////
//        boolean dup = occupied.putIfAbsent(position, entity.getId()) != null;
//        nextMoves.add(
//                new NextMove(
//                        entity,
//                        dup ? entity.getX() : position.getX(),
//                        dup ? entity.getY() : position.getY()
//                )
//        );
//    }


    public BehaviorResult moveRand(RedisEntity entity) {
        int dx = random.nextInt(3) - 1;
        int dy = random.nextInt(3) - 1;
        int nextX = GeoUtil.setCoordinate(entity.getX() + dx);
        int nextY = GeoUtil.setCoordinate(entity.getY() + dy);

        return new BehaviorResult(
                entity.getState(),
                new Position(nextX, nextY),
                null
        );
    }

    public BehaviorResult moveIDLE(RedisEntity entity) {
        return moveRest(entity);
    }

    public BehaviorResult moveRest(RedisEntity entity) {
        return new BehaviorResult(
                StateEnum.REST,
                new Position(entity.getX(), entity.getY()),
                null
        );
    }

    public BehaviorResult moveChase(RedisEntity entity, Map<Long, RedisEntity> entityMap) {
        RedisEntity target = entityMap.get(entity.getTargetId());
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

        int moveX = -dx;
        int moveY = -dy;

        if (RandUtil.percent(30)) {

            if (Math.abs(dx) >= Math.abs(dy)) {
                moveY += RandUtil.percent(50) ? 1 : -1;
            } else {
                moveX += RandUtil.percent(50) ? 1 : -1;
            }

            moveX = Math.max(-1, Math.min(1, moveX));
            moveY = Math.max(-1, Math.min(1, moveY));
        }

        int nextX = GeoUtil.setCoordinate(curX + moveX);
        int nextY = GeoUtil.setCoordinate(curY + moveY);

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
        if(target.isDead()) {
            handleSuccessHuntEntity(entity);
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
                StateEnum.ATTACK,
                new Position(nextX, nextY),
                entity.getTargetId()
        );
    }

    private BehaviorResult moveFlock(
            RedisEntity entity,
            Map<Long, RedisEntity> entityMap
    ) {
        Long targetId = entity.getTargetId();
        if(!entityMap.containsKey(targetId)) {
            System.out.printf("%d일때 %d target", entity.getId(), entity.getTargetId());
        }
        int curX = entity.getX();
        int curY = entity.getY();
        RedisEntity target = entityMap.get(entity.getTargetId());
        int targetX = target.getX();
        int targetY = target.getY();


        int dx = Integer.compare(targetX, curX);
        int dy = Integer.compare(targetY, curY);

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
        if (partner == null || entity.getId() > partner.getId()) {
            return moveRand(entity);
        }

        if (!RandUtil.percent(20)) {
            return moveRand(entity);
        }

        Position childP = collisionService.reserveEmptyPosition(entity, partner);
        if(childP == null) {
            return moveFlock(entity, entityMap);
        }

        int childX = childP.getX();
        int childY = childP.getY();

        RedisEntity child = new RedisEntity(
                null,
                0,
                entity.getType(),
                StateEnum.MOVE,
                100,
                100,
                childX,
                childY,
                false,
                0,
                null,
                null,
                false
        );

        spawnList.add(child);
        handleSuccessSpawnEntity(entity);
        handleSuccessBreedEntity(partner);

        return new BehaviorResult(
                StateEnum.SPAWN,
                new Position(entity.getX(), entity.getY()),
                null
        );
    }

    private void handleBlockedEntity(RedisEntity entity) {
        entity.increaseStamina();
        entity.setSkipGeoUpdate(true);
    }

    private void handleUnBlockedEntity(RedisEntity entity) {
        entity.decreaseStamina();
        entity.setSkipGeoUpdate(false);
    }

    private void handleDeadEntity(RedisEntity entity) {

    }

    private void handleLiveEntity(RedisEntity entity) {
        entity.increaseAge();
        entity.increaseHp();
        entity.decreaseBreedTick();
    }

    private void handleSuccessHuntEntity(RedisEntity entity) {
        entity.successHunt();
        entityManager.addHistoryEntity(entity);
        entityManager.addStreamEntity(entity);
    }

    private void handleSuccessSpawnEntity(RedisEntity entity) {
        entity.afterBreed();
        entityManager.addHistoryEntity(entity);
        entityManager.addStreamEntity(entity);
    }

    private void handleSuccessBreedEntity(RedisEntity entity) {
        entity.afterBreed();
    }
}
