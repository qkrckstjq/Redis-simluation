package com.example.world.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class SimulationEvent {

    private StateEnum state;

    private long tick;

    private long entityId;

    private Map<String, String> payload;

    public SimulationEvent(StateEnum state, Long curTick, long entityId, Map<String, String> payLoad) {
        this.state = state;
        this.tick = curTick;
        this.entityId = entityId;
        this.payload = payLoad;
    }
}
