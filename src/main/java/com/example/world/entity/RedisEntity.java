package com.example.world.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RedisEntity {
    Long id;
    TypeEnum type;
    StateEnum state;
    int stamina;
    int hp;
    int x;
    int y;
    Long targetId;

    public RedisEntity(Long id, TypeEnum type, StateEnum state, int stamina, int hp, int x, int y) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.stamina = stamina;
        this.hp = hp;
        this.x = x;
        this.y = y;
    }

    public void decreaseStamina() {
        stamina = Math.max(stamina - 1, 0);
    }

    public void increaseStamina() {
        stamina = Math.min(stamina + 1, 100);
    }

    public void attackedByWolf() {
        hp -= 20;
    }
}
