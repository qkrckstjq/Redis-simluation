package com.example.world.service;

import com.example.world.entity.NextMove;
import com.example.world.entity.Position;
import com.example.world.entity.RedisEntity;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CollisionService {

    private final Map<Position, Set<Long>> board;

    public CollisionService() {
        this.board = new HashMap<>();
    }

    public CollisionService(List<RedisEntity> entities) {
        this.board = new HashMap<>();

        for (RedisEntity entity : entities) {
            Long id = entity.getId();

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
        }

        return false;
    }
}
