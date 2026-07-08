package com.example.world.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class EntityHistoryDto {
    private int tick;
    private StateEnum state;
    private long targetId;
    private long entityId;
    private int age;

    public EntityHistoryDto(
            int tick,
            StateEnum state,
            long targetId,
            long entityId,
            int age
    ) {
        this.tick = tick;
        this.state = state;
        this.targetId = targetId;
        this.entityId = entityId;
        this.age = age;
    }
}
