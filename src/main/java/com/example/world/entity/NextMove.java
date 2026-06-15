package com.example.world.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NextMove {
    private RedisEntity entity;

    int nextX;
    int nextY;

    public NextMove(
            RedisEntity entity,
            int nextX,
            int nextY
            ) {
        this.entity = entity;
        this.nextX = nextX;
        this.nextY = nextY;
    }
}
