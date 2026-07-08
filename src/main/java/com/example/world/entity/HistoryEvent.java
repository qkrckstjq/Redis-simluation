package com.example.world.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class SimulationEvent {
    private long tick;

    private StateEnum state;

    private long entityId;

    private long targetId;

    private int age;

    public SimulationEvent(StateEnum state, Long curTick, long entityId, long targetId, int age) {
        this.tick = curTick;
        this.state = state;
        this.entityId = entityId;
        this.targetId = targetId;
        this.age = age;
    }
}
