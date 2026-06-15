package com.example.world.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BehaviorResult {
    private StateEnum action;
    private Position nextPosition;
    private Long targetId;

    public BehaviorResult(StateEnum action, Position nextPosition, Long targetId) {
        this.action = action;
        this.nextPosition = nextPosition;
        this.targetId = targetId;
    }
}
