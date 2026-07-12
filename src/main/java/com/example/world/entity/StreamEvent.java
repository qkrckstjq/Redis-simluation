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
            long entityId,
            long curTick,
            long targetId,
            int age
    ) {
        this.state = state;
        this.entityId = entityId;
        this.tick = curTick;
        this.targetId = targetId;
        this.age = age;
    }
}
