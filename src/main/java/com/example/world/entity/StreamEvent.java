package com.example.world.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StreamEvent {
    private long tick;

    private StateEnum state;

    private long entityId;

    private long targetId;

    private int age;

    public StreamEvent(
            StateEnum state,
            Long curTick,
            long entityId,
            long targetId,
            int age
    ) {
        this.tick = curTick;
        this.state = state;
        this.entityId = entityId;
        this.targetId = targetId;
        this.age = age;
    }
}
