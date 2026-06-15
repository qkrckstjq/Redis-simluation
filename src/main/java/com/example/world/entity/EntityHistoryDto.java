package com.example.world.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class EntityHistoryDto {
    private StateEnum state;
    private Long targetId;
    private Integer x;
    private Integer y;
    private Long tick;


    public EntityHistoryDto(
            StateEnum state,
            Long targetId,
            Integer x,
            Integer y,
            Long tick
    ) {
        this.state = state;
        this.targetId = targetId;
        this.x = x;
        this.y = y;
        this.tick = tick;
    }
}
