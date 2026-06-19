package com.example.world.entity;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class EntitySnapshotDto {

    private Long id;
    private TypeEnum type;
    private StateEnum state;
    private int stamina;
    private int hp;
    private int x;
    private int y;
    private Long targetId;
    private List<Long> nearbyIds;

    public EntitySnapshotDto(Long id, TypeEnum type, StateEnum state, int stamina, int hp, int x, int y) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.stamina = stamina;
        this.hp = hp;
        this.x = x;
        this.y = y;
    }

    public EntitySnapshotDto(
            Long id,
            TypeEnum type,
            StateEnum state,
            int stamina,
            int hp,
            int x,
            int y,
            List<Long> nearbyIds,
            Long targetId
    ) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.stamina = stamina;
        this.hp = hp;
        this.x = x;
        this.y = y;
        this.nearbyIds = nearbyIds;
        this.targetId = targetId;
    }
}