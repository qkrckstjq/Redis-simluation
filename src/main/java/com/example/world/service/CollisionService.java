package com.example.world.service;

import com.example.world.entity.NextMove;
import com.example.world.entity.Position;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.TypeEnum;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.logging.SocketHandler;

@Component
public class CollisionService {

    private Map<Position, Set<Long>> board;
    private Map<Long, RedisEntity> reverseBoard;

    public CollisionService() {
        this.board = new HashMap<>();
        this.reverseBoard = new HashMap<>();
    }

    public CollisionService(List<RedisEntity> entities) {
        this.board = new HashMap<>();
        this.reverseBoard = new HashMap<>();

        for (RedisEntity entity : entities) {
            Long id = entity.getId();
            reverseBoard.put(id, entity);

            Position position = new Position(
                    entity.getX(),
                    entity.getY()
            );

            Set<Long> existEntities =
                    board.getOrDefault(
                            position,
                            new HashSet<>()
                    );

            existEntities.add(id);
            board.put(position, existEntities);
        }
    }

    public void initBoards(List<RedisEntity> entities) {
        this.board = new HashMap<>();
        this.reverseBoard = new HashMap<>();

        for (RedisEntity entity : entities) {
            Long id = entity.getId();
            reverseBoard.put(id, entity);

            Position position = new Position(
                    entity.getX(),
                    entity.getY()
            );

            Set<Long> existEntities =
                    board.getOrDefault(
                            position,
                            new HashSet<>()
                    );

            existEntities.add(id);
            board.put(position, existEntities);
        }
    }

    public boolean tryMove(NextMove nextMove) {

        RedisEntity entity = nextMove.getEntity();

        Long id = entity.getId();

        Position prevPosition = new Position(
                entity.getX(),
                entity.getY()
        );

        Position nextPosition = new Position(
                nextMove.getNextX(),
                nextMove.getNextY()
        );

        Set<Long> prevEntities = board.get(prevPosition);

        if (!board.containsKey(nextPosition)) {

            Set<Long> nextEntities = new HashSet<>();
            nextEntities.add(id);

            board.put(nextPosition, nextEntities);

            prevEntities.remove(id);

            if (prevEntities.isEmpty()) {
                board.remove(prevPosition);
            }

            return true;
        } else {
            Set<Long> collisionEntities = board.get(nextPosition);
            TypeEnum curType = entity.getType();
            for(Long collisionId : collisionEntities) {
                RedisEntity target = reverseBoard.get(collisionId);
                if(!curType.equals(target.getType())) {
                    entity.setTargetId(null);
                }
            }
        }

        return false;
    }
}
